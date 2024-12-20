package uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.ebi.intact.psi.mi.xmlmaker.XmlMakerUtils;

public class UniprotMapper {
    private static final Logger LOGGER = LogManager.getLogger(UniprotMapper.class);

    private final Map<String, String> alreadyParsed = new HashMap<>();
    private static final String ACCEPT_HEADER = "Accept";
    private static final String ACCEPT_JSON = "application/json";
    private final XmlMakerUtils utils = new XmlMakerUtils();

    public String fetchUniprotResults(String protein, String organismId, String database) {
        LOGGER.info("Fetching UniProt results for protein: {}, organismId: {}, database: {}", protein, organismId, database);

        String urlString = uniprotQueryConstructor(protein, organismId, database);
        if (alreadyParsed.containsKey(protein)) {
            LOGGER.debug("Protein '{}' already parsed. Returning cached result.", protein);
            return alreadyParsed.get(protein);
        }

        try {
            HttpURLConnection connection = createConnection(urlString);
            return parseResponse(connection, protein);
        } catch (Exception e) {
            LOGGER.error("Error fetching UniProt results for protein '{}': {}", protein, e.getMessage(), e);
        }
        return protein;
    }

    private HttpURLConnection createConnection(String urlString) throws Exception {
        LOGGER.debug("Creating connection to UniProt API with URL: {}", urlString);

        URL uniprotURL = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) uniprotURL.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty(ACCEPT_HEADER, ACCEPT_JSON);
        return connection;
    }

    private String parseResponse(HttpURLConnection connection, String protein) {
        StringBuilder content = new StringBuilder();

        try (BufferedReader queryResults = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = queryResults.readLine()) != null) {
                content.append(inputLine);
            }
            LOGGER.debug("Response received for protein '{}': {}", protein, content);
            return extractUniprotAccession(content.toString(), protein);
        } catch (Exception e) {
            LOGGER.error("Error parsing response for protein '{}': {}", protein, e.getMessage(), e);
        } finally {
            connection.disconnect();
        }
        return null;
    }

    private String extractUniprotAccession(String jsonString, String protein) {
        LOGGER.debug("Extracting UniProt accession from JSON response for protein '{}'", protein);

        try {
            JsonElement parsedElement = JsonParser.parseString(jsonString);
            JsonObject jsonResponse = parsedElement.getAsJsonObject();
            String uniprotAccession = getUniprotAC(jsonResponse);

            if (uniprotAccession != null) {
                LOGGER.info("UniProt accession '{}' found for protein '{}'.", uniprotAccession, protein);
                alreadyParsed.put(protein, uniprotAccession);
                return uniprotAccession;
            } else {
                LOGGER.warn("No UniProt accession found for protein '{}'.", protein);
                alreadyParsed.put(protein, " ");
            }
        } catch (Exception e) {
            LOGGER.error("Error extracting UniProt accession for protein '{}': {}", protein, e.getMessage(), e);
        }
        return protein;
    }

    public String getUniprotAC(JsonObject results) {
        List<JsonObject> swissProtUniprotACs = new ArrayList<>();
        List<JsonObject> tremblUniprotACs = new ArrayList<>();

        if (results != null && results.has("results")) {
            JsonArray resultsAsJson = results.get("results").getAsJsonArray();
            for (JsonElement element : resultsAsJson) {
                JsonObject result = element.getAsJsonObject();
                if (result.has("entryType")) {
                    switch (result.get("entryType").getAsString()) {
                        case "UniProtKB reviewed (Swiss-Prot)":
                            swissProtUniprotACs.add(result);
                            break;
                        case "UniProtKB unreviewed (TrEMBL)":
                            tremblUniprotACs.add(result);
                            break;
                        case "Inactive":
                            LOGGER.warn("Protein entry is inactive. Returning merge/demerge information.");
                            return result.get("inactiveReason").getAsJsonObject().get("mergeDemergeTo").getAsString();
                        default:
                            LOGGER.debug("Unknown entry type in results: {}", result.get("entryType").getAsString());
                            break;
                    }
                }
            }
        }
        return chooseUniprotAc(swissProtUniprotACs, tremblUniprotACs);
    }

    public String chooseUniprotAc(List<JsonObject> swissProtUniprotACs, List<JsonObject> tremblUniprotACs) {
        LOGGER.debug("Choosing best UniProt accession based on sequence length.");

        sortArrayBySequenceLength(swissProtUniprotACs);
        sortArrayBySequenceLength(tremblUniprotACs);

        if (!swissProtUniprotACs.isEmpty()) {
            return swissProtUniprotACs.get(0).get("primaryAccession").getAsString();
        } else if (!tremblUniprotACs.isEmpty()) {
            return tremblUniprotACs.get(0).get("primaryAccession").getAsString();
        }
        return null;
    }

    public void sortArrayBySequenceLength(List<JsonObject> arrayList) {
        arrayList.sort((result1, result2) -> Integer.compare(getSequenceLength(result2), getSequenceLength(result1)));
    }

    private int getSequenceLength(JsonObject result) {
        return result.has("sequence") && result.getAsJsonObject("sequence").has("value")
                ? result.getAsJsonObject("sequence").get("value").getAsString().length()
                : 0;
    }

    private String uniprotQueryConstructor(String query, String organismId, String database) {
        LOGGER.debug("Constructing UniProt query for query: {}, organismId: {}, database: {}", query, organismId, database);

        String uniprotApiUrl = "https://rest.uniprot.org/uniprotkb/search?query=(xref:";
        organismId = utils.fetchTaxIdForOrganism(organismId);
        database = chooseDbFromCol(query, database);
        String uniprotApiUrlPart2 = "%20AND%20organism_id:";
        String uniprotApiUrlPart3 = ")&format=json&fields=accession,organism_id";

        if (database != null) {
            return uniprotApiUrl + database + uniprotApiUrlPart2 + organismId + uniprotApiUrlPart3;
        } else {
            return "https://rest.uniprot.org/uniprotkb/search?query=accession:" + query;
        }
    }

    public String chooseDbFromCol(String query, String db) {
        if (db.toLowerCase().contains("refseq")) {
            return "RefSeq-" + query;
        }
        if (db.toLowerCase().contains("geneid")) {
            return "GeneID-" + query;
        }
        if (db.toLowerCase().contains("ensembl")) {
            return "ensembl-" + query;
        }
        return null;
    }
}
