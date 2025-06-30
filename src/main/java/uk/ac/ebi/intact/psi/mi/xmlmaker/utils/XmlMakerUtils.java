package uk.ac.ebi.intact.psi.mi.xmlmaker.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.xml.model.extension.xml300.XmlCvTerm;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.InputData;
import uk.ac.ebi.pride.utilities.ols.web.service.client.OLSClient;
import uk.ac.ebi.pride.utilities.ols.web.service.config.OLSWsConfig;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Identifier;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;
import java.util.stream.Collectors;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.InputData.EXPERIMENTAL_ROLE;

/**
 * Utility class for XML creation and management tasks in the PSI-MI context.
 * Includes methods for interacting with external APIs, encoding URLs,
 * retrieving ontology terms, and handling user interactions with dialogs.
 */
public class XmlMakerUtils {

    private static final Logger LOGGER = Logger.getLogger(XmlMakerUtils.class.getName());
    private static final Map<String, CvTerm> nameToCvTerm = new ConcurrentHashMap<>();
    private static final Map<String, String> nameToTaxIdCache = new ConcurrentHashMap<>();
    static final OLSClient olsClient = new OLSClient(new OLSWsConfig());
    static {
        try {
            FileHandler fileHandler = new FileHandler("xmlmakerutils.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(consoleHandler);
            LOGGER.setLevel(Level.FINE);
        } catch (IOException e) {
            LOGGER.warning("Failed to set up logger handlers: " + e.getMessage());
        }
    }

    /**
     * Creates an HTTP connection for a given URL.
     * @param urlString url to connect
     * @throws IOException if error
     * @return URLConnection
     */
    public static HttpURLConnection createConnection(String urlString) throws IOException {
        LOGGER.fine("Creating HTTP connection to URL: " + urlString);
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        return connection;
    }

    /**
     * Encodes a string for safe use in a URL.
     * @param input to convert in URL format.
     * @return string compatible with URL.
     */
    public static String encodeForURL(String input) {
        return URLEncoder.encode(input, StandardCharsets.UTF_8);
    }

    /**
     * Fetches the Taxonomy ID for a given organism name using the OLS API.
     * @param organismName organism to fetch
     * @return organism tax id
     */
    public static String fetchTaxIdWithApi(String organismName) {
        String urlString = "https://www.ebi.ac.uk/ols4/api/search?q=" + encodeForURL(organismName) + "&ontology=ncbitaxon";
        try {
            HttpURLConnection connection = createConnection(urlString);
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                }
            } else {
                LOGGER.warning("HTTP Error: " + responseCode + " while fetching TaxID for organism: " + organismName);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while fetching TaxID for organism: " + organismName, e);
        }
        return null;
    }

    /**
     * Fetches the Taxonomy ID for a given organism name by processing the API response.
     * @param organismName organism to fetch
     * @return organism tax id
     */
    public static String fetchTaxIdForOrganism(String organismName) {
        String taxId = nameToTaxIdCache.get(organismName);
        if (taxId != null) return taxId;
        if (organismName.matches("\\d+") || organismName.equals("-1") || organismName.equals("-2")) return organismName;
        // already a taxId or an in-vitro/chemical synthesis
        if (organismName.toLowerCase().contains("organism") || organismName.isEmpty()) {return null;}

        String apiResponse = fetchTaxIdWithApi(organismName);
        String oboId = apiResponse != null ? extractOboId(apiResponse) : null;
        taxId = oboId != null ? oboId : organismName;

        if (oboId == null) {
            String userInput = JOptionPane.showInputDialog(null,
                    "No TaxId found for organism: " + organismName + "\nPlease enter a custom TaxId:",
                    "Custom TaxId Input",
                    JOptionPane.QUESTION_MESSAGE);

            while (userInput != null && !userInput.matches("\\d+")) {
                JOptionPane.showMessageDialog(null, "Please enter a valid numeric TaxId.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
                userInput = JOptionPane.showInputDialog(null,
                        "No TaxId found for organism: " + organismName + "\nPlease enter a valid numeric TaxId:",
                        "Custom TaxId Input",
                        JOptionPane.QUESTION_MESSAGE);
            }

            if (userInput != null && !userInput.trim().isEmpty()) {
                taxId = userInput.trim(); // Update taxId with user input
            }
        }

        nameToTaxIdCache.put(organismName, taxId);
        return taxId;
    }

    /**
     * Retrieves a {@link CvTerm} for the given input using the PSI-MI ontology.
     * Trims input, removes trailing semicolons, checks cache, and falls back to OLS lookup.
     * Returns {@code null} for blank or invalid input. Caches and returns a placeholder if not found.
     *
     * @param input the term name to look up
     * @return the corresponding {@link CvTerm}, a placeholder if not found, or {@code null} if input is invalid
     */
    public static CvTerm fetchTerm(String input) {
        if (input == null) return null;
        input = input.trim();
        if (input.endsWith(";")){
            input = input.substring(0, input.length() - 1);
        }

        if (input.isBlank() || input.contains("null")) return null;
        CvTerm term = nameToCvTerm.get(input);
        if (term != null) return term;

        try {
            Term complexTerm = olsClient.getExactTermByName(input, "mi");
            if (complexTerm != null) {
                term = new XmlCvTerm(complexTerm.getLabel(), complexTerm.getOboId().getIdentifier());
                nameToCvTerm.put(input, term);
            } else {
                term = new XmlCvTerm(input, "N/A");
                nameToCvTerm.put(input, term);
            }
        } catch (Exception e) {
            LOGGER.warning("Error while fetching term: " + input + e.getMessage());
        }
        return term;
    }

    /**
     * Fetches the MI (Molecular Interaction) ID for a given term using the OLS client
     * @param input string to look for in OLS.
     * @return MI identifier from OLS
     */
    public static String fetchMiId(String input) {
        CvTerm cvTerm = fetchTerm(input);
        return cvTerm == null ? null : cvTerm.getMIIdentifier();
    }

    /**
     * Retrieves a list of term names from the OLS (Ontology Lookup Service) for a given MI (Molecular Interaction) ID.
     * The method uses the provided MI ID to query the OLS and fetch the child terms associated with it.
     * Only terms that do not have children are added to the result list.
     *
     * @param miId The MI identifier (in OBO format) used to retrieve the child terms from OLS.
     * @return A sorted list of term names that are child terms of the provided MI ID, which do not have further children.
     */
    public static List<String> fetchTermsFromOls(String miId){
        List<String> termsNames = new ArrayList<>();

        Identifier identifier = new Identifier(miId, Identifier.IdentifierType.OBO);
        List<Term> terms = olsClient.getTermChildren(identifier, "mi", 9999);

        for (Term term : terms) {
            XmlCvTerm xmlTerm = new XmlCvTerm(term.getLabel(), term.getOboId().getIdentifier());
            nameToCvTerm.put(term.getName(), xmlTerm);
            termsNames.add(term.getName());
        }

        termsNames = termsNames.stream().distinct().collect(Collectors.toList());
        termsNames.sort(String::compareTo);

        return termsNames;
    }

    /**
     * Extracts the OBO (Open Biomedical Ontologies) ID from a JSON response.
     * @param json OLS response
     * @return OLS MI identifier.
     */
    public static String extractOboId(String json) {
        if (json == null || json.isEmpty()) {
            LOGGER.warning("Invalid input JSON for extracting OBO ID");
            return null;
        }
        try {
            LOGGER.fine("Parsing JSON to extract OBO ID");
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(json);
            JsonNode docsNode = root.path("response").path("docs");

            if (docsNode.isArray() && !docsNode.isEmpty()) {
                JsonNode firstDoc = docsNode.get(0);
                String oboId = firstDoc.path("obo_id").asText();

                if (oboId.startsWith("NCBITaxon:")) {
                    return oboId.substring("NCBITaxon:".length());
                }
            } else {
                LOGGER.warning("No documents found in JSON response");
                return null;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while extracting OBO ID from JSON", e);
            return null;
        }
        return null;
    }

    /**
     * Calculates the Levenshtein Distance between two strings. The Levenshtein
     * Distance is a measure of the number of single-character edits (insertions,
     * deletions, or substitutions) required to change one string into the other.
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return the Levenshtein Distance between the two strings
     */
    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j] + 1,
                                    dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1)
                    );
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    /**
     * Calculates the similarity between two strings as a percentage. The similarity
     * is computed based on the Levenshtein Distance between the strings and their
     * maximum possible length.
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return the similarity percentage between the two strings, where 100.0 means
     *         the strings are identical and 0.0 means they are completely dissimilar
     */
    public double calculateSimilarity(String s1, String s2) {
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 100.0;

        int distance = levenshteinDistance(s1, s2);
        return ((maxLength - distance) / (double) maxLength) * 100;
    }

    /**
     * Checks if the given value is null or matches the specified keyName.
     * If the value is null or equal to the keyName, an empty string is returned.
     * Otherwise, the string representation of the value is returned.
     *
     * @param value The object whose value is to be checked. Can be any object or null.
     * @param keyName The key name to compare the string value against.
     *
     * @return An empty string if the value is null or equals the keyName, otherwise the string representation of the value.
     */
    public static String isValueNull(Object value, String keyName) {
        if (value == null) {
            return "";
        }
        String stringValue = value.toString();
        return stringValue.equals(keyName) ? "" : stringValue;
    }

    public static String getDataKey(InputData column, Map<String, String> data) {
        if (column.experimentalRoleDependent) {
            String experimentalRole = data.get(EXPERIMENTAL_ROLE.name);
            if (experimentalRole != null && !experimentalRole.trim().isEmpty()) {
                return column.name + experimentalRole;
            }
        }
        return column.name;
    }

    public static String getValueFromCombobox(JComboBox<String> comboBox) {
        String comboboxToolTip =  comboBox.getToolTipText();
        return comboBox.getSelectedItem() != null
                && !comboBox.getSelectedItem().equals(comboboxToolTip)
                ? comboBox.getSelectedItem().toString() : "";
    }
}
