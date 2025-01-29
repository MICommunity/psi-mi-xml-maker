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

    public ArrayList<UniprotResult> fetchUniprotResult(String protein, String previousDb, String organism){
        try {
            return getUniprotIds(getUniprotResponse(protein, previousDb, organism));
        } catch (Exception e) {
            XmlMakerUtils.showErrorDialog("Error fetching UniProt results, please check your internet connection");
            LOGGER.error("Error fetching UniProt results for protein '{}': {}", protein, e.getMessage(), e);
        }
        return null;
    }

    public JsonObject getUniprotResponse(String protein, String previousDb, String organism){
        String urlString = "https://rest.uniprot.org/uniprotkb/search?query=" + protein;
        if (previousDb != null) {
            urlString += "&db=" + previousDb;
        }
        if (organism != null) {
            urlString = "https://rest.uniprot.org/uniprotkb/search?query=(xref:" + protein + "%20AND%20organism_id:" + organism + ")";
//            urlString = XmlMakerUtils.encodeForURL(urlString);
        }
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

    public ArrayList<UniprotResult> getUniprotIds(JsonObject results) {
        if (results == null) return null;
        JsonArray resultsAsJson = results.get("results").getAsJsonArray();
        ArrayList<UniprotResult> uniprotResults = new ArrayList<>();

        for (JsonElement element : resultsAsJson) {
            JsonObject result = element.getAsJsonObject();

            String uniprotAc;
            String name;
            String entryType = result.get("entryType").getAsString();
            String uniprotLink;
            String organism;

            if (!Objects.equals(entryType, "Inactive")){
                uniprotAc = result.get("primaryAccession").getAsString();
                name = result.get("uniProtkbId").getAsString();
                organism = result.get("organism").getAsJsonObject().get("taxonId").getAsString();

            } else {
                uniprotAc = result.get("inactiveReason").getAsJsonObject().get("mergeDemergeTo").getAsString();
                name = uniprotAc;
                organism = "";
            }
            uniprotLink = "https://www.uniprot.org/uniprotkb/" + uniprotAc;

            UniprotResult oneResult = new UniprotResult(uniprotAc, name, organism, entryType, uniprotLink, "UniprotKB");
            uniprotResults.add(oneResult);
        }

        setButtonGroup(uniprotResults);
        return uniprotResults;
    }

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
