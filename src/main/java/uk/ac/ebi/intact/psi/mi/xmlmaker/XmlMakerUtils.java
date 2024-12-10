package uk.ac.ebi.intact.psi.mi.xmlmaker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.similarity.LevenshteinDistance;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.organisms.SuggestedOrganisms;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class XmlMakerUtils {

    SuggestedOrganisms suggestedOrganisms = new SuggestedOrganisms();

    public static void processFile(String filePath, ExcelFileReader excelFileReader) {
        excelFileReader.selectFileOpener(filePath);
    }

    public void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(new JFrame(), message, "ERROR", JOptionPane.ERROR_MESSAGE);
    }

    public void showInfoDialog(String message) {
        JOptionPane.showMessageDialog(new JFrame(), message, "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
    }

    public int findMostSimilarOrganism(String input) {
        LevenshteinDistance levenshtein = new LevenshteinDistance();

        String mostSimilarKey = null;
        int minDistance = Integer.MAX_VALUE;

        for (Map.Entry<String, String> entry : suggestedOrganisms.organismMap.entrySet()) {
            int distance = levenshtein.apply(input, entry.getValue());
            if (distance < minDistance) {
                minDistance = distance;
                mostSimilarKey = entry.getKey();
            }
        }
        assert mostSimilarKey != null;
        return Integer.parseInt(mostSimilarKey);
    }

    public String fetchTaxIdWithApi(String organismName) {
        String urlString = "https://www.ebi.ac.uk/ena/taxonomy/rest/scientific-name/" + organismName;
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

    public String getTaxId(String json) {
        if (json == null || json.isEmpty()) {
            System.err.println("Invalid JSON input: " + json);
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(json);

            if (rootNode.isArray() && rootNode.size() > 0) {
                JsonNode firstElement = rootNode.get(0);
                if (firstElement.has("taxId")) {
                    return firstElement.get("taxId").asText();
                } else {
                    System.err.println("Key 'taxId' not found in JSON: " + firstElement);
                }
            } else {
                System.err.println("Unexpected JSON structure: " + rootNode);
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
        organismName = encodeForURL(organismName);
        String apiResponse = fetchTaxIdWithApi(organismName);
        if (apiResponse != null) {
            return getTaxId(apiResponse);
        }
        return null;
    }

    public static String encodeForURL(String input) {
        try {
            return URLEncoder.encode(input, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void showActionDialog(String[] options) {
        JDialog dialog = new JDialog((JFrame) null, "Choose Action", true);
        dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));

        ButtonGroup group = new ButtonGroup();
        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));

        for (String option : options) {
            JRadioButton radioButton = new JRadioButton(option);
            group.add(radioButton);
            radioPanel.add(radioButton);
        }

        JButton validateButton = new JButton("Validate");
        validateButton.addActionListener(e -> {
            ButtonModel selectedModel = group.getSelection();
            if (selectedModel != null) {
                //TODO: made actions for the molecularSets
                String selectedOption = selectedModel.getActionCommand();
                showInfoDialog("You selected: " + selectedOption);
                dialog.dispose();
            } else {
                showErrorDialog("Please select an option.");
            }
        });

        dialog.add(radioPanel);
        dialog.add(validateButton);
        dialog.pack();

        dialog.setSize(300, 100);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

}
