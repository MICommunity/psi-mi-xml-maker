package uk.ac.ebi.intact.psi.mi.xmlmaker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ebi.pride.utilities.ols.web.service.client.OLSClient;
import uk.ac.ebi.pride.utilities.ols.web.service.config.OLSWsConfig;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;

/**
 * Utility class for XML creation and management tasks in the PSI-MI context.
 * This class includes methods to interact with external APIs, encode URLs,
 * retrieve ontology terms, and handle user interactions with dialog messages.
 * It also manages logging for debugging and troubleshooting purposes.
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
     *
     * @param message the error message to display
     */
    public void showErrorDialog(String message) {
        LOGGER.severe("Error: " + message);
        JOptionPane.showMessageDialog(new JFrame(), message, "ERROR", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Displays an informational message in a dialog box.
     *
     * @param message the informational message to display
     */
    public void showInfoDialog(String message) {
        JOptionPane.showMessageDialog(new JFrame(), message, "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Fetches the Taxonomy ID for a given organism name using the OLS API.
     *
     * @param organismName the name of the organism
     * @return the raw API response as a string, or null if the request fails
     */
    public String fetchTaxIdWithApi(String organismName) {
        String urlString = "https://www.ebi.ac.uk/ols4/api/search?q=" + encodeForURL(organismName) + "&ontology=ncbitaxon";
        try {
            HttpURLConnection connection = createConnection(urlString);
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            } else {
                LOGGER.warning("HTTP Error: " + responseCode + " while fetching TaxID for organism: " + organismName);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while fetching TaxID for organism: " + organismName, e);
        }
        return null;
    }

    /**
     * Creates an HTTP connection for a given URL.
     *
     * @param urlString the URL to connect to
     * @return a HttpURLConnection object
     * @throws Exception if an error occurs while creating the connection
     */
    public HttpURLConnection createConnection(String urlString) throws Exception {
        LOGGER.fine("Creating HTTP connection to URL: " + urlString);
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        return connection;
    }

    /**
     * Fetches the Taxonomy ID for a given organism name by processing the API response.
     *
     * @param organismName the name of the organism
     * @return the extracted Taxonomy ID as a string, or null if extraction fails
     */
    public String fetchTaxIdForOrganism(String organismName) {
        String apiResponse = fetchTaxIdWithApi(organismName);
        if (apiResponse != null) {
            return extractOboId(apiResponse);
        }
        LOGGER.warning("Failed to fetch TaxID for organism: " + organismName);
        return null;
    }

    /**
     * Encodes a string for safe use in a URL.
     *
     * @param input the string to encode
     * @return the URL-encoded string
     */
    public static String encodeForURL(String input) {
        try {
            return URLEncoder.encode(input, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to encode input for URL: " + input, e);
            return null;
        }
    }

    /**
     * Fetches the MI (Molecular Interaction) ID for a given term using the OLS client.
     *
     * @param input the term for which to fetch the MI ID
     * @return the MI ID as a string, or null if the term is not found
     */
    public String fetchMiId(String input) {
        String miId = miIds.get(input);
        try {
            Term term = olsClient.getExactTermByName(input, "mi");
            miId = (term != null && term.getOboId() != null) ? term.getOboId().getIdentifier() : null;
            miIds.put(input, miId);
        } catch (NullPointerException e) {
            LOGGER.log(Level.WARNING, "Failed to retrieve MI ID for input: " + input, e);
            showErrorDialog("Failed to retrieve MI ID for " + input);
        }
        return miId;
    }

    /**
     * Extracts the OBO (Open Biomedical Ontologies) ID from a JSON response.
     *
     * @param json the JSON response string
     * @return the extracted OBO ID as a string, or null if extraction fails
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
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while extracting OBO ID from JSON", e);
        }
        return null;
    }
}
