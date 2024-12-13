package uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
    final Map<String, String> alreadyParsed = new HashMap<>();
    private static final String ACCEPT_HEADER = "Accept";
    private static final String ACCEPT_JSON = "application/json";
    final XmlMakerUtils utils = new XmlMakerUtils();

    public String fetchUniprotResults(String protein, String organismId, String database) {
        String urlString = uniprotQueryConstructor(protein, organismId, database);
        if (alreadyParsed.containsKey(protein)) {
            return alreadyParsed.get(protein);
        }
        try {
            HttpURLConnection connection = createConnection(urlString);
            return parseResponse(connection, protein);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return protein;
    }

    private HttpURLConnection createConnection(String urlString) throws Exception {
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
            return extractUniprotAccession(content.toString(), protein);
        } catch (Exception ignored) {
        } finally {
            connection.disconnect();
        }
            return null;
    }

    private String extractUniprotAccession(String jsonString, String protein) {
        // TODO: should we directly use the API fetching for only accession and remove this function?
        JsonElement parsedElement = JsonParser.parseString(jsonString);
        JsonObject jsonResponse = parsedElement.getAsJsonObject();
        String uniprotAccession = getUniprotAC(jsonResponse);
        if (uniprotAccession != null) {
            alreadyParsed.put(protein, uniprotAccession);
            return uniprotAccession;
        } else {
            alreadyParsed.put(protein, " ");
        }
        return protein;
    }

    public String getUniprotAC(JsonObject results) {
        List<JsonObject> swissProtUniprotACs = new ArrayList<>();
        List<JsonObject> tremblUniprotACs = new ArrayList<>();

        if (results != null && results.has("results")) {
            JsonArray resultsAsJson = results.get("results").getAsJsonArray();
            for (int i = 0; i < resultsAsJson.size(); i++) {
                JsonObject result = resultsAsJson.get(i).getAsJsonObject();
                if (result.has("entryType")) {
                    switch (result.get("entryType").getAsString()) {
                        case "UniProtKB reviewed (Swiss-Prot)":
                            swissProtUniprotACs.add(result);
                            break;
                        case "UniProtKB unreviewed (TrEMBL)":
                            tremblUniprotACs.add(result);
                            break;
                        case "Inactive":
                            return result.get("inactiveReason").getAsJsonObject().get("mergeDemergeTo").getAsString();
                        default:
                            break;
                    }
                }
            }
        }
        return chooseUniprotAc(swissProtUniprotACs, tremblUniprotACs);
    }

    public String chooseUniprotAc(List<JsonObject> swissProtUniprotACs, List<JsonObject> tremblUniprotACs) {
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
        String uniprotApiUrl = "https://rest.uniprot.org/uniprotkb/search?query=(xref:";
        organismId = utils.fetchTaxIdForOrganism(organismId);
        database = chooseDbFromCol(query, database);
        String uniprotApiUrlPart2 = "%20AND%20organism_id:";
        String uniprotApiUrlPart3 = ")&format=json&fields=accession,organism_id";
        if (database != null) {
            return uniprotApiUrl + database  + uniprotApiUrlPart2 + organismId + uniprotApiUrlPart3;
        }
        else {
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
