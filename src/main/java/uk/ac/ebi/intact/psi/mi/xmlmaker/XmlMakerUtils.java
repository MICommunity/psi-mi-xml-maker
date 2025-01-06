package uk.ac.ebi.intact.psi.mi.xmlmaker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ebi.pride.utilities.ols.web.service.client.OLSClient;
import uk.ac.ebi.pride.utilities.ols.web.service.config.OLSWsConfig;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.*;

/**
 * Utility class for XML creation and management tasks in the PSI-MI context.
 * Includes methods for interacting with external APIs, encoding URLs,
 * retrieving ontology terms, and handling user interactions with dialogs.
 */
public class XmlMakerUtils {

    private static final Logger LOGGER = Logger.getLogger(XmlMakerUtils.class.getName());
    private final Map<String, String> miIds = new HashMap<>();
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
            System.err.println("Failed to set up logger handlers: " + e.getMessage());
        }
    }

    /**
     * Displays an error message in a dialog box.
     */
    public void showErrorDialog(String message) {
        LOGGER.severe("Error: " + message);
        JOptionPane.showMessageDialog(new JFrame(), message, "ERROR", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Displays an informational message in a dialog box.
     */
    public void showInfoDialog(String message) {
        JOptionPane.showMessageDialog(new JFrame(), message, "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Fetches the Taxonomy ID for a given organism name using the OLS API.
     */
    public String fetchTaxIdWithApi(String organismName) {
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
     * Creates an HTTP connection for a given URL.
     */
    public HttpURLConnection createConnection(String urlString) throws IOException {
        LOGGER.fine("Creating HTTP connection to URL: " + urlString);
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        return connection;
    }

    /**
     * Fetches the Taxonomy ID for a given organism name by processing the API response.
     */
    public String fetchTaxIdForOrganism(String organismName) {
        if (organismName.matches("\\d+")) {
            return organismName; //already a taxid
        } else {
            String apiResponse = fetchTaxIdWithApi(organismName);
            return apiResponse != null ? extractOboId(apiResponse) : null;
        }
    }

    /**
     * Encodes a string for safe use in a URL.
     */
    public static String encodeForURL(String input) {
        return URLEncoder.encode(input, StandardCharsets.UTF_8);
    }

    /**
     * Fetches the MI (Molecular Interaction) ID for a given term using the OLS client.
     */
    public String fetchMiId(String input) {
        String miId = miIds.get(input);
        if (miId == null) {
            try {
                Term term = olsClient.getExactTermByName(input, "mi");
                miId = (term != null && term.getOboId() != null) ? term.getOboId().getIdentifier() : null;
                miIds.put(input, miId);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to retrieve MI ID for input: " + input, e);
                showErrorDialog("Failed to retrieve MI ID for " + input);
            }
        }
        return miId;
    }

    /**
     * Extracts the OBO (Open Biomedical Ontologies) ID from a JSON response.
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
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while extracting OBO ID from JSON", e);
        }
        return null;
    }
}
