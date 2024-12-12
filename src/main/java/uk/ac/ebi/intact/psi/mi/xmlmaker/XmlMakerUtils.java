package uk.ac.ebi.intact.psi.mi.xmlmaker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.pride.utilities.ols.web.service.client.OLSClient;
import uk.ac.ebi.pride.utilities.ols.web.service.config.OLSWsConfig;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

//TODO: use log4j to have a report

public class XmlMakerUtils {

    private final Map<String, String> miIds = new HashMap<>();
    static final OLSClient olsClient = new OLSClient(new OLSWsConfig());

    public static void processFile(String filePath, ExcelFileReader excelFileReader) {
        excelFileReader.selectFileOpener(filePath);
    }

    public void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(new JFrame(), message, "ERROR", JOptionPane.ERROR_MESSAGE);
    }

    public void showInfoDialog(String message) {
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
                System.err.println("HTTP Error: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public HttpURLConnection createConnection(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        return connection;
    }

    public String fetchTaxIdForOrganism(String organismName) {
        String apiResponse = fetchTaxIdWithApi(organismName);
        if (apiResponse != null) {
            return extractOboId(apiResponse);
        }
        return null;
    }

    public static String encodeForURL(String input) {
        try {
            return URLEncoder.encode(input, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String fetchMiId(String input) {
        String miId = null;
        if (miIds.get(input) != null) {
            miId = miIds.get(input);
        }
        else {
            try {
                Term term = olsClient.getExactTermByName(input, "mi");
                miId = (term != null && term.getOboId() != null) ? term.getOboId().getIdentifier() : null;
                miIds.put(input, miId);
            } catch (NullPointerException e) {
                System.err.println("Failed to retrieve MI ID for " + input);
                showErrorDialog("Failed to retrieve MI ID for " + input);
            }
        }
        return miId;
    }

    public static String extractOboId(String json) {
        if (json == null || json.isEmpty()) {
            System.err.println("Invalid input JSON");
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(json);
            JsonNode docsNode = root.path("response").path("docs");

            if (docsNode.isArray() && !docsNode.isEmpty()) {
                JsonNode firstDoc = docsNode.get(0);
                String oboId = firstDoc.path("obo_id").asText();

                if (oboId.startsWith("NCBITaxon:")) {
                    return oboId.substring("NCBITaxon:".length());
                } else {
                    System.err.println("obo_id does not start with 'NCBITaxon:'");
                }
            } else {
                System.err.println("No documents found in response");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
