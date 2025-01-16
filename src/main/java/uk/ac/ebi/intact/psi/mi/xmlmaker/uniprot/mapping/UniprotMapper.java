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
import java.util.*;

import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;

/**
 * This class provides functionality to fetch UniProt results for a given protein from the UniProt database.
 * It caches previously fetched results to avoid redundant requests.
 */
public class UniprotMapper {
    private static final Logger LOGGER = LogManager.getLogger(UniprotMapper.class);

    private final Map<String, String> alreadyParsed = new HashMap<>();
    private static final String ACCEPT_HEADER = "Accept";
    private static final String ACCEPT_JSON = "application/json";
    private final XmlMakerUtils utils = new XmlMakerUtils();

    /**
     * Fetches the UniProt results for a given protein, organism, and database.
     *
     * @param protein the protein identifier (e.g., accession or other unique identifier).
     * @param organismId the organism identifier (e.g., taxonomic ID).
     * @param database the name of the database to search in (e.g., RefSeq, GeneID, Ensembl).
     * @return the UniProt accession for the protein, or the input protein if no accession is found.
     */
    public String fetchUniprotResults(String protein, String organismId, String database) {
        LOGGER.info("Fetching UniProt results for protein: {}, organismId: {}, database: {}", protein, organismId, database);

        String urlString = uniprotQueryConstructor(protein, organismId, database);
        if (alreadyParsed.containsKey(protein)) {
            LOGGER.debug("Protein '{}' already parsed. Returning cached result.", protein);
            return alreadyParsed.get(protein);
        }

        try {
            HttpURLConnection connection = createConnection(urlString);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
                return protein;
            }
            return parseResponse(connection, protein);
        } catch (Exception e) {
            utils.showErrorDialog("Error fetching UniProt results, please check your internet connection");
            LOGGER.error("Error fetching UniProt results for protein '{}': {}", protein, e.getMessage(), e);
            return protein;
        }
    }

    /**
     * Creates an HTTP connection to the UniProt API using the provided URL string.
     *
     * @param urlString the URL to connect to.
     * @return an {@link HttpURLConnection} object.
     * @throws Exception if an error occurs while creating the connection.
     */
    private HttpURLConnection createConnection(String urlString) throws Exception {
        LOGGER.debug("Creating connection to UniProt API with URL: {}", urlString);

        URL uniprotURL = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) uniprotURL.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty(ACCEPT_HEADER, ACCEPT_JSON);
        return connection;
    }

    /**
     * Parses the response from the UniProt API to extract the UniProt accession.
     *
     * @param connection the HTTP connection that returned the response.
     * @param protein the protein identifier for logging purposes.
     * @return the UniProt accession or the input protein if no accession is found.
     */
    private String parseResponse(HttpURLConnection connection, String protein) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader queryResults = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = queryResults.readLine()) != null) {
                content.append(inputLine);
            }
            return extractUniprotAccession(content.toString(), protein);
        } catch (Exception e) {
            LOGGER.error("Error parsing response for protein '{}': {}", protein, e.getMessage(), e);
            return protein;
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Extracts the UniProt accession from the JSON response.
     *
     * @param jsonString the JSON response as a string.
     * @param protein the protein identifier for logging purposes.
     * @return the UniProt accession or the input protein if no accession is found.
     */
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
                alreadyParsed.put(protein, protein);
                return protein;
            }
        } catch (Exception e) {
            LOGGER.error("Error extracting UniProt accession for protein '{}': {}", protein, e.getMessage(), e);
            return protein;
        }
    }

    /**
     * Retrieves the UniProt accession from the JSON response object.
     *
     * @param results the JSON object containing the results from the UniProt API.
     * @return the UniProt accession, or {@code null} if not found.
     */
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

    /**
     * Chooses the best UniProt accession based on sequence length.
     *
     * @param swissProtUniprotACs list of Swiss-Prot UniProt entries.
     * @param tremblUniprotACs list of TrEMBL UniProt entries.
     * @return the chosen UniProt accession.
     */
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

    /**
     * Sorts the list of UniProt entries by sequence length in descending order.
     *
     * @param arrayList the list of UniProt entries to sort.
     */
    public void sortArrayBySequenceLength(List<JsonObject> arrayList) {
        arrayList.sort((result1, result2) -> Integer.compare(getSequenceLength(result2), getSequenceLength(result1)));
    }

    /**
     * Retrieves the sequence length of a UniProt entry.
     *
     * @param result the UniProt entry to extract the sequence length from.
     * @return the length of the sequence.
     */
    private int getSequenceLength(JsonObject result) {
        return result.has("sequence") && result.getAsJsonObject("sequence").has("value")
                ? result.getAsJsonObject("sequence").get("value").getAsString().length()
                : 0;
    }

    /**
     * Constructs a UniProt API query URL based on the protein, organism, and database.
     *
     * @param query the protein identifier.
     * @param organismId the organism ID.
     * @param database the database to query (e.g., RefSeq, GeneID).
     * @return the constructed UniProt API query URL.
     */
    private String uniprotQueryConstructor(String query, String organismId, String database) {
        LOGGER.debug("Constructing UniProt query for query: {}, organismId: {}, database: {}", query, organismId, database);

        String uniprotApiUrl = "https://rest.uniprot.org/uniprotkb/search?query=(xref:";
        organismId = utils.fetchTaxIdForOrganism(organismId);
        database = chooseDbFromCol(query, database);
        String uniprotApiUrlPart2 = "%20AND%20organism_id:";
        String uniprotApiUrlPart3 = "&format=json&fields=accession,organism_id";

        if (!Objects.equals(database, "null") && !Objects.equals(organismId, "null")) {
            return uniprotApiUrl + database + uniprotApiUrlPart2 + organismId + uniprotApiUrlPart3;
        } else {
            return "https://rest.uniprot.org/uniprotkb/search?query=accession:" + query;
        }
    }

    /**
     * Chooses the appropriate database prefix for the query.
     *
     * @param query the protein query.
     * @param db the database name.
     * @return the appropriate database prefix.
     */
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
        return "null";
    }

    /**
     * Clears the cached parsed results.
     */
    public void clearAlreadyParsed(){
        alreadyParsed.clear();
    }
}
