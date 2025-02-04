package uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class is responsible for fetching and processing UniProt results based on a given protein, previous database,
 * and organism. It interacts with the UniProt API to retrieve data and manages the display of results using a set of
 * radio buttons for each UniProt entry.
 */
@Getter
public class UniprotGeneralMapper {
    private static final Logger LOGGER = LogManager.getLogger(UniprotGeneralMapper.class);

    @Setter
    private Map<String, String> alreadyParsed = new HashMap<>();
    private static final String ACCEPT_HEADER = "Accept";
    private static final String ACCEPT_JSON = "application/json";

    @Setter
    private UniprotResult selectedUniprot;
    private ButtonGroup buttonGroup  = new ButtonGroup();

    /**
     * Fetches UniProt results for the given protein, previous database, and organism.
     *
     * @param protein The protein to search for.
     * @param previousDb The previous database to search in (can be {@code null}).
     * @param organism The organism's taxon ID to filter by (can be {@code null}).
     * @return A list of {@link UniprotResult} objects containing the search results.
     */
    public ArrayList<UniprotResult> fetchUniprotResult(String protein, String previousDb, String organism){
        try {
            return getUniprotIds(getUniprotResponse(protein, previousDb, organism), protein, previousDb, organism);
        } catch (Exception e) {
            XmlMakerUtils.showErrorDialog("Error fetching UniProt results, please check your internet connection");
            LOGGER.error("Error fetching UniProt results for protein '{}': {}", protein, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Sends a GET request to the UniProt API and retrieves the raw response.
     *
     * @param protein The protein to search for.
     * @param previousDb The previous database to search in (can be {@code null}).
     * @param organism The organism's taxon ID to filter by (can be {@code null}).
     * @return A {@link JsonObject} representing the response from the UniProt API.
     */
    public JsonObject getUniprotResponse(String protein, String previousDb, String organism){
        StringBuilder test = new StringBuilder("https://rest.uniprot.org/uniprotkb/search?query=(xref:");
        test.append(protein);
        if (organism != null) {
            test.append("%20AND%20organism_id:").append(organism);
        }
        if (previousDb != null) {
            test.append("%20AND%20database:").append(previousDb);
        }
        test.append(")");

        String urlString = test.toString();

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            StringBuilder content = new StringBuilder();

            connection.setRequestMethod("GET");
            connection.setRequestProperty(ACCEPT_HEADER, ACCEPT_JSON);

            try (BufferedReader queryResults = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                while ((inputLine = queryResults.readLine()) != null) {
                    content.append(inputLine);
                }
                return JsonParser.parseString(content.toString()).getAsJsonObject();
            } catch (Exception e) {
                LOGGER.error("Error fetching Uniprot response: {}", e.getMessage(), e);
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            LOGGER.error("Error fetching Uniprot response: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * Extracts UniProt results from the JSON response and returns a list of {@link UniprotResult} objects.
     *
     * @param results The JSON response from the UniProt API.
     * @return A list of {@link UniprotResult} objects representing the UniProt entries.
     */
    public ArrayList<UniprotResult> getUniprotIds(JsonObject results, String protein, String previousDb, String inputOrganism) {
        ArrayList<UniprotResult> uniprotResults = new ArrayList<>();

        if (results == null){
            uniprotResults.add(new UniprotResult(protein, protein, inputOrganism, null,
                    null, "UniprotKB", 0));
            return uniprotResults;
        }

        JsonArray resultsAsJson = results.get("results").getAsJsonArray();

        for (JsonElement element : resultsAsJson) {
            JsonObject result = element.getAsJsonObject();

            String uniprotAc;
            String name;
            String entryType = result.get("entryType").getAsString();
            String uniprotLink;
            String organism;
            int sequenceSize;

            if (!Objects.equals(entryType, "Inactive")){
                uniprotAc = result.get("primaryAccession").getAsString();
                name = result.get("uniProtkbId").getAsString();
                organism = result.get("organism").getAsJsonObject().get("taxonId").getAsString();
                sequenceSize = result.get("sequence").getAsJsonObject().get("length").getAsInt();

            } else {
                uniprotAc = result.get("inactiveReason").getAsJsonObject().get("mergeDemergeTo").getAsString();
                name = uniprotAc;
                organism = "";
                sequenceSize = 0;
            }
            uniprotLink = "https://www.uniprot.org/uniprotkb/" + uniprotAc;

            UniprotResult oneResult = new UniprotResult(uniprotAc, name, organism, entryType, uniprotLink, "UniprotKB", sequenceSize); //todo: keep the previous id if not found
            uniprotResults.add(oneResult);
        }

        setButtonGroup(uniprotResults);
        return uniprotResults;
    }

    /**
     * Sets the {@link ButtonGroup} for displaying the UniProt results as radio buttons.
     *
     * @param results A list of {@link UniprotResult} objects to be displayed.
     */
    private void setButtonGroup(ArrayList<UniprotResult> results){
        buttonGroup = new ButtonGroup();
        for (UniprotResult result : results){
            JRadioButton button = new JRadioButton();
            String buttonText = "<html><b>" + result.getUniprotAc() + "</b> (Tax ID:" + result.getOrganism() + ") - " +
                    result.getEntryType() + " - <a href=\"" + result.getUniprotLink() + "\">" + result.getUniprotLink() + "</a></html>";
            button.setText(buttonText);
            button.setName(result.getUniprotAc());
            buttonGroup.add(button);
        }
    }
}
