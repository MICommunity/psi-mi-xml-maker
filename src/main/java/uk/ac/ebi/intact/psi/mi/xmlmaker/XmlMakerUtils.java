package uk.ac.ebi.intact.psi.mi.xmlmaker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.pride.utilities.ols.web.service.client.OLSClient;
import uk.ac.ebi.pride.utilities.ols.web.service.config.OLSWsConfig;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;

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

    public void showErrorDialog(String message) {
        LOGGER.severe("Error: " + message);
        JOptionPane.showMessageDialog(new JFrame(), message, "ERROR", JOptionPane.ERROR_MESSAGE);
    }

    public void showInfoDialog(String message) {
        LOGGER.info("Info: " + message);
        JOptionPane.showMessageDialog(new JFrame(), message, "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
    }

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

    public HttpURLConnection createConnection(String urlString) throws Exception {
        LOGGER.fine("Creating HTTP connection to URL: " + urlString);
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        return connection;
    }

    public String fetchTaxIdForOrganism(String organismName) {
        LOGGER.info("Fetching TaxID for organism: " + organismName);
        String apiResponse = fetchTaxIdWithApi(organismName);
        if (apiResponse != null) {
            String taxId = extractOboId(apiResponse);
            LOGGER.fine("Extracted TaxID: " + taxId + " for organism: " + organismName);
            return taxId;
        }
        LOGGER.warning("Failed to fetch TaxID for organism: " + organismName);
        return null;
    }

    public static String encodeForURL(String input) {
        try {
            LOGGER.fine("Encoding input for URL: " + input);
            return URLEncoder.encode(input, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to encode input for URL: " + input, e);
            return null;
        }
    }

    public String fetchMiId(String input) {
//        LOGGER.info("Fetching MI ID for input: " + input);
        String miId = miIds.get(input);
        if (miId != null) {
//            LOGGER.fine("Found MI ID in cache: " + miId + " for input: " + input);
        } else {
            try {
                Term term = olsClient.getExactTermByName(input, "mi");
                miId = (term != null && term.getOboId() != null) ? term.getOboId().getIdentifier() : null;
                miIds.put(input, miId);
//                LOGGER.fine("Retrieved MI ID: " + miId + " for input: " + input);
            } catch (NullPointerException e) {
                LOGGER.log(Level.WARNING, "Failed to retrieve MI ID for input: " + input, e);
                showErrorDialog("Failed to retrieve MI ID for " + input);
            }
        }
        return miId;
    }

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
                    LOGGER.fine("Extracted OBO ID: " + oboId);
                    return oboId.substring("NCBITaxon:".length());
                } else {
                    LOGGER.warning("OBO ID does not start with 'NCBITaxon:'");
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
