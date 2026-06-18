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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    private static final int BATCH_SIZE = 25;

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

    public ArrayList<UniprotResult> fetchUniprotResultWithoutFilters(String protein, String previousDb) {
        String fallbackDb = "gene name".equalsIgnoreCase(normalizeDb(previousDb)) ? "gene name" : "";
        return fetchUniprotResult(protein, fallbackDb, "");
    }

    public boolean hasSearchFilters(String previousDb, String organism) {
        String normalizedDb = normalizeDb(previousDb);
        String normalizedOrganism = normalizeValue(organism);
        return (!normalizedDb.isEmpty() && !"uniprotkb".equals(normalizedDb)) || !normalizedOrganism.isEmpty();
    }

    public Map<String, ArrayList<UniprotResult>> fetchUniprotResultsBatch(Collection<UniprotRequest> requests) {
        Map<String, ArrayList<UniprotResult>> resultsByRequest = new HashMap<>();
        if (requests == null || requests.isEmpty()) {
            return resultsByRequest;
        }

        Map<String, List<UniprotRequest>> groupedRequests = new LinkedHashMap<>();
        for (UniprotRequest request : requests) {
            resultsByRequest.putIfAbsent(request.getLookupKey(), new ArrayList<>());
            groupedRequests.computeIfAbsent(buildGroupKey(request), ignored -> new ArrayList<>()).add(request);
        }

        for (List<UniprotRequest> group : groupedRequests.values()) {
            for (int start = 0; start < group.size(); start += BATCH_SIZE) {
                List<UniprotRequest> batch = group.subList(start, Math.min(start + BATCH_SIZE, group.size()));
                JsonObject response = getBatchUniprotResponse(batch);
                ArrayList<UniprotResult> batchResults = getUniprotIds(response);

                for (UniprotResult result : batchResults) {
                    for (UniprotRequest request : batch) {
                        if (matchesRequest(result, request)) {
                            resultsByRequest.computeIfAbsent(request.getLookupKey(), ignored -> new ArrayList<>()).add(result);
                        }
                    }
                }
            }
        }

        return resultsByRequest;
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
        return executeRequest(urlString);
    }

    private JsonObject getBatchUniprotResponse(List<UniprotRequest> requests) {
        String urlString = buildBatchUrl(requests);
        return executeRequest(urlString);
    }

    private JsonObject executeRequest(String urlString) {
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

    private String buildBatchUrl(List<UniprotRequest> requests) {
        String query = buildBatchQuery(requests);
        return "https://rest.uniprot.org/uniprotkb/search?size=500&query=" + encode(query);
    }

    private String buildBatchQuery(List<UniprotRequest> requests) {
        if (requests.isEmpty()) {
            return "";
        }

        UniprotRequest first = requests.get(0);
        String previousDb = normalizeDb(first.getPreviousDb());
        String organism = normalizeValue(first.getOrganism());

        List<String> clauses = new ArrayList<>();
        for (UniprotRequest request : requests) {
            clauses.add(buildClause(request.getProtein(), previousDb));
        }

        String query = "(" + String.join(" OR ", clauses) + ")";
        if (!organism.isEmpty() && !"uniprotkb".equals(previousDb)) {
            query = query + " AND organism_id:" + organism;
        }
        return query;
    }

    private String buildClause(String identifier, String previousDb) {
        String sanitizedIdentifier = escapeQueryValue(identifier);
        if ("gene name".equals(previousDb)) {
            return "gene_exact:" + sanitizedIdentifier;
        }
        if (previousDb.isEmpty() || "uniprotkb".equals(previousDb)) {
            return "(accession:" + sanitizedIdentifier + " OR id:" + sanitizedIdentifier + ")";
        }
        return "(xref:" + sanitizedIdentifier + " AND database:" + escapeQueryValue(previousDb) + ")";
    }

    /**
     * Extracts UniProt results from the JSON response and returns a list of {@link UniprotResult} objects.
     *
     * @param results The JSON response from the UniProt API.
     * @return A list of {@link UniprotResult} objects representing the UniProt entries.
     */
    public ArrayList<UniprotResult> getUniprotIds(JsonObject results) {
        ArrayList<UniprotResult> uniprotResults = new ArrayList<>();

        if (results == null || results.isJsonNull() || !results.has("results")){
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
        String name = extractParticipantName(result);
        String uniprotId = result.get("uniProtkbId").getAsString();
        String organism = result.get("organism").getAsJsonObject().get("taxonId").getAsString();
        int sequenceSize = result.get("sequence").getAsJsonObject().get("length").getAsInt();
        String uniprotLink = "https://www.uniprot.org/uniprotkb/" + uniprotAc;
        String entryType = result.get("entryType").getAsString();

        UniprotResult uniprotResult = new UniprotResult(uniprotAc, name, organism,
                entryType, uniprotLink, "UniProtKB", sequenceSize, "protein");
        uniprotResult.setUniprotId(uniprotId);
        uniprotResult.setSecondaryAccessions(extractSecondaryAccessions(result));
        uniprotResult.setGeneNames(extractGeneNames(result));
        uniprotResult.setMatchingCrossReferences(extractCrossReferences(result));
        return uniprotResult;
    }

    private String extractParticipantName(JsonObject result) {
        String geneName = extractPrimaryGeneName(result);
        if (geneName != null) {
            return geneName;
        }

        JsonObject proteinDescription = result.getAsJsonObject("proteinDescription");
        if (proteinDescription != null) {
            String recommendedName = getFullName(proteinDescription.getAsJsonObject("recommendedName"));
            if (recommendedName != null) {
                return recommendedName;
            }

            JsonArray submissionNames = proteinDescription.getAsJsonArray("submissionNames");
            if (submissionNames != null && !submissionNames.isEmpty()) {
                String submissionName = getFullName(submissionNames.get(0).getAsJsonObject());
                if (submissionName != null) {
                    return submissionName;
                }
            }

            JsonArray alternativeNames = proteinDescription.getAsJsonArray("alternativeNames");
            if (alternativeNames != null && !alternativeNames.isEmpty()) {
                String alternativeName = getFullName(alternativeNames.get(0).getAsJsonObject());
                if (alternativeName != null) {
                    return alternativeName;
                }
            }
        }

        return result.get("uniProtkbId").getAsString();
    }

    private String extractPrimaryGeneName(JsonObject result) {
        if (!result.has("genes")) {
            return null;
        }

        JsonArray genes = result.getAsJsonArray("genes");
        if (genes.isEmpty()) {
            return null;
        }

        JsonObject firstGene = genes.get(0).getAsJsonObject();
        JsonObject geneName = firstGene.getAsJsonObject("geneName");
        if (geneName != null && geneName.has("value")) {
            return geneName.get("value").getAsString();
        }

        JsonArray synonyms = firstGene.getAsJsonArray("synonyms");
        if (synonyms != null && !synonyms.isEmpty()) {
            JsonObject synonym = synonyms.get(0).getAsJsonObject();
            if (synonym.has("value")) {
                return synonym.get("value").getAsString();
            }
        }

        return null;
    }

    private String getFullName(JsonObject nameObject) {
        if (nameObject == null) {
            return null;
        }
        JsonObject fullName = nameObject.getAsJsonObject("fullName");
        if (fullName != null && fullName.has("value")) {
            return fullName.get("value").getAsString();
        }
        return null;
    }

    private List<String> extractSecondaryAccessions(JsonObject result) {
        List<String> secondaryAccessions = new ArrayList<>();
        if (result.has("secondaryAccessions")) {
            JsonArray secondaryArray = result.getAsJsonArray("secondaryAccessions");
            for (JsonElement secondaryAccession : secondaryArray) {
                secondaryAccessions.add(secondaryAccession.getAsString());
            }
        }
        return secondaryAccessions;
    }

    private List<String> extractGeneNames(JsonObject result) {
        List<String> geneNames = new ArrayList<>();
        if (result.has("genes")) {
            JsonArray genes = result.getAsJsonArray("genes");
            for (JsonElement geneElement : genes) {
                JsonObject gene = geneElement.getAsJsonObject();
                JsonObject geneName = gene.getAsJsonObject("geneName");
                if (geneName != null && geneName.has("value")) {
                    geneNames.add(geneName.get("value").getAsString());
                }
                JsonArray synonyms = gene.getAsJsonArray("synonyms");
                if (synonyms != null) {
                    for (JsonElement synonym : synonyms) {
                        JsonObject synonymObject = synonym.getAsJsonObject();
                        if (synonymObject.has("value")) {
                            geneNames.add(synonymObject.get("value").getAsString());
                        }
                    }
                }
            }
        }
        return geneNames;
    }

    private Map<String, Set<String>> extractCrossReferences(JsonObject result) {
        Map<String, Set<String>> crossReferences = new HashMap<>();
        if (!result.has("uniProtKBCrossReferences")) {
            return crossReferences;
        }

        JsonArray xrefs = result.getAsJsonArray("uniProtKBCrossReferences");
        for (JsonElement xrefElement : xrefs) {
            JsonObject xref = xrefElement.getAsJsonObject();
            String database = normalizeValue(xref.get("database").getAsString());
            Set<String> values = crossReferences.computeIfAbsent(database, ignored -> new HashSet<>());
            if (xref.has("id")) {
                values.add(normalizeValue(xref.get("id").getAsString()));
            }
            JsonArray properties = xref.getAsJsonArray("properties");
            if (properties != null) {
                for (JsonElement propertyElement : properties) {
                    JsonObject property = propertyElement.getAsJsonObject();
                    if (property.has("value")) {
                        values.add(normalizeValue(property.get("value").getAsString()));
                    }
                }
            }
        }
        return crossReferences;
    }

    private boolean matchesRequest(UniprotResult result, UniprotRequest request) {
        if (result == null) {
            return false;
        }
        if (!normalizeValue(request.getOrganism()).isEmpty()
                && result.getOrganism() != null
                && !normalizeValue(request.getOrganism()).equals(normalizeValue(result.getOrganism()))) {
            return false;
        }

        String previousDb = normalizeDb(request.getPreviousDb());
        String identifier = normalizeValue(request.getProtein());

        if (previousDb.isEmpty() || "uniprotkb".equals(previousDb)) {
            return matchesUniprotIdentifier(result, identifier);
        }
        if ("gene name".equals(previousDb)) {
            return matchesGeneName(result, identifier);
        }
        return matchesCrossReference(request.getProtein(), previousDb, result.getMatchingCrossReferences());
    }

    private boolean matchesUniprotIdentifier(UniprotResult result, String identifier) {
        if (identifier.equals(normalizeValue(result.getUniprotAc()))
                || identifier.equals(normalizeValue(result.getUniprotId()))) {
            return true;
        }
        for (String secondaryAccession : result.getSecondaryAccessions()) {
            if (identifier.equals(normalizeValue(secondaryAccession))) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesGeneName(UniprotResult result, String identifier) {
        for (String geneName : result.getGeneNames()) {
            if (identifier.equals(normalizeValue(geneName))) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesCrossReference(String identifier, String previousDb, Map<String, Set<String>> crossReferences) {
        if (crossReferences == null || crossReferences.isEmpty()) {
            return false;
        }
        Set<String> values = crossReferences.get(normalizeValue(previousDb));
        return values != null && values.contains(normalizeValue(identifier));
    }

    private String buildGroupKey(UniprotRequest request) {
        return normalizeDb(request.getPreviousDb()) + "|" + normalizeValue(request.getOrganism());
    }

    private String normalizeDb(String previousDb) {
        return normalizeValue(previousDb);
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String escapeQueryValue(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.contains(" ") || trimmed.contains(":") || trimmed.contains("(") || trimmed.contains(")")) {
            return "\"" + trimmed.replace("\"", "\\\"") + "\"";
        }
        return trimmed;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Getter
    public static class UniprotRequest {
        private final String protein;
        private final String previousDb;
        private final String organism;
        private final String lookupKey;

        public UniprotRequest(String protein, String previousDb, String organism, String lookupKey) {
            this.protein = protein;
            this.previousDb = previousDb;
            this.organism = organism;
            this.lookupKey = lookupKey;
        }
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
