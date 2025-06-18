package uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.UniprotResult;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.GuiUtils.*;

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
    @Getter
    private final List<String> uniprotIdNotFound = new ArrayList<>();

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
            return getUniprotIds(getUniprotResponse(protein, previousDb, organism));
        } catch (Exception e) {
            showErrorDialog("Error fetching UniProt results, please check your internet connection");
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
        String urlString = buildUrl(protein, previousDb, organism);

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
                return null;
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            LOGGER.error("Error fetching Uniprot response: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Builds the UniProt API search URL based on the provided participant ID, previous database, and organism.
     *
     * @param previousParticipantId The identifier to search for (e.g., UniProt accession or gene name).
     * @param previousDb The name of the previous database (can be {@code null} or empty).
     * @param organism The taxon ID of the organism to filter by (can be {@code null} or empty).
     * @return A string representing the full search URL for the UniProt API.
     */
    private String buildUrl(String previousParticipantId, String previousDb, String organism) {
        String baseUrl = "https://rest.uniprot.org/uniprotkb/search?query=";

        if (previousDb != null && previousDb.equals("gene name")) {
            return buildUrlForGene(previousParticipantId, organism);
        }

        if (organism == null
            || previousDb == null
            || organism.trim().isEmpty()
            || previousDb.trim().isEmpty()
            || organism.equals("-2")
            || organism.equals("-1")
            || previousDb.trim().equalsIgnoreCase("uniprotkb")) {
            return baseUrl + previousParticipantId;
        }

        return baseUrl + "(xref:" + previousParticipantId +
                "%20AND%20organism_id:" + organism +
                "%20AND%20database:" + previousDb +
                ")";
    }

    /**
     * Builds the UniProt API search URL specifically for gene name queries.
     *
     * @param previousParticipantId The gene name to search for.
     * @param organism The taxon ID of the organism to filter by (can be {@code null} or empty).
     * @return A string representing the full search URL for the UniProt API.
     */
    private String buildUrlForGene(String previousParticipantId, String organism){
        String baseUrl = "https://rest.uniprot.org/uniprotkb/search?query=gene:" + previousParticipantId;

        if (organism != null
            && !organism.trim().isEmpty()
            && !organism.equals("-2")
            && !organism.equals("-1")) {
            return baseUrl + "%20AND%20organism_id:" + organism;
        }

        return baseUrl;
    }

    /**
     * Extracts UniProt results from the JSON response and returns a list of {@link UniprotResult} objects.
     *
     * @param results The JSON response from the UniProt API.
     * @return A list of {@link UniprotResult} objects representing the UniProt entries.
     */
    public ArrayList<UniprotResult> getUniprotIds(JsonObject results) {
        ArrayList<UniprotResult> uniprotResults = new ArrayList<>();

        if (results.isJsonNull()){
            return uniprotResults;
        }

        JsonArray resultsAsJson = results.get("results").getAsJsonArray();

        for (JsonElement element : resultsAsJson) {
            JsonObject result = element.getAsJsonObject();
            String entryType = result.get("entryType").getAsString();
            UniprotResult oneResult;

            if (!Objects.equals(entryType, "Inactive")){
                oneResult = getUniprotResultFromActiveID(result);
            } else {
                oneResult = getUniprotResultFromInactiveID(result);
            }

            if (oneResult != null) {
                uniprotResults.add(oneResult);
            }
        }

        setButtonGroup(uniprotResults);
        return uniprotResults;
    }

    /**
     * Constructs a {@link UniprotResult} object from a JSON object representing an active UniProt entry.
     *
     * @param result The JSON object containing the UniProt entry data.
     * @return A {@code UniprotResult} object populated with entry details.
     */
    private UniprotResult getUniprotResultFromInactiveID(JsonObject result) {
        JsonObject inactiveResult = result.get("inactiveReason").getAsJsonObject();
        if (inactiveResult.get("inactiveReasonType").getAsString().equals("DELETED")) {
            uniprotIdNotFound.add(result.get("primaryAccession").getAsString());
            return null;
        } else if (inactiveResult.get("inactiveReasonType").getAsString().equals("MERGED")) {
            if (inactiveResult.get("mergeDemergeTo").getAsString() != null) {
                String name = inactiveResult.get("mergeDemergeTo").getAsString();
                String organism = "";
                JsonObject newResult = getUniprotResponse(name, "UniProtKB", organism);
                JsonArray resultsAsJson = newResult.get("results").getAsJsonArray();
                return getUniprotResultFromActiveID(resultsAsJson.get(0).getAsJsonObject());
            }
        }
        return null;
    }

    /**
     * Processes a JSON object representing an inactive UniProt entry and retrieves the merged active entry if available.
     *
     * @param result The JSON object containing the inactive UniProt entry data.
     * @return A {@code UniprotResult} for the merged entry, or {@code null} if deleted or unresolvable.
     */
    private UniprotResult getUniprotResultFromActiveID(JsonObject result) {
        String uniprotAc = result.get("primaryAccession").getAsString();
        String name = result.get("uniProtkbId").getAsString();
        String organism = result.get("organism").getAsJsonObject().get("taxonId").getAsString();
        int sequenceSize = result.get("sequence").getAsJsonObject().get("length").getAsInt();
        String uniprotLink = "https://www.uniprot.org/uniprotkb/" + uniprotAc;
        String entryType = result.get("entryType").getAsString();

        return new UniprotResult(uniprotAc, name, organism,
                entryType, uniprotLink, "UniProtKB", sequenceSize, "protein");
    }

    /**
     * Sets the {@link ButtonGroup} for displaying the UniProt results as radio buttons.
     *
     * @param results A list of {@link UniprotResult} objects to be displayed.
     */
    private void setButtonGroup(ArrayList<UniprotResult> results){
        buttonGroup = new ButtonGroup();
        for (UniprotResult result : results) {
            if ("UniProtKB reviewed (Swiss-Prot)".equals(result.getEntryType())) {
                JRadioButton button = new JRadioButton();
                String buttonText = "<html><b>" + result.getUniprotAc() + "</b> (Tax ID:" + result.getOrganism() + ") - " +
                        result.getEntryType() + " - <a href=\"" + result.getUniprotLink() + "\">" + result.getUniprotLink() + "</a></html>";
                button.setText(buttonText);
                button.setName(result.getUniprotAc());
                buttonGroup.add(button);
            }
        }
    }
}
