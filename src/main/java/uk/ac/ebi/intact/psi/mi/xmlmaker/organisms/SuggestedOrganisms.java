package uk.ac.ebi.intact.psi.mi.xmlmaker.organisms;

import java.util.LinkedHashMap;
import java.util.Map;

public class SuggestedOrganisms {
    private final Map<String, String> organismMap = new LinkedHashMap<>();

    public SuggestedOrganisms() {
        initializeOrganismMap();
    }

    private void initializeOrganismMap() {
        organismMap.put("-1", "Select or type organism ID");
        organismMap.put("9606", "Homo sapiens (9606)");
        organismMap.put("10090", "Mus musculus (10090)");
        organismMap.put("10116", "Rattus norvegicus (10116)");
        organismMap.put("7227", "Drosophila melanogaster (7227)");
        organismMap.put("559292", "Saccharomyces cerevisiae (559292)");
        organismMap.put("83333", "Escherichia coli (83333)");
        organismMap.put("3702", "Arabidopsis thaliana (3702)");
        organismMap.put("6239", "Caenorhabditis elegans (6239)");
        organismMap.put("284812", "Schizosaccharomyces pombe (284812)");
        organismMap.put("9031", "Gallus gallus (9031)");
        organismMap.put("9913", "Bos taurus (9913)");
        organismMap.put("2697049", "Severe acute respiratory syndrome coronavirus 2 (2697049)");
        organismMap.put("8732", "Crotalus durissus terrificus (8732)");
        organismMap.put("694009", "Severe acute respiratory syndrome coronavirus (694009)");
        organismMap.put("1263720", "Middle East respiratory syndrome-related coronavirus (1263720)");
        organismMap.put("7955", "Danio rerio (7955)");
        organismMap.put("9615", "Canis lupus familiaris (9615)");
        organismMap.put("1235996", "Human betacoronavirus 2c EMC/2012 (1235996)");
        organismMap.put("8355", "Xenopus laevis (8355)");
        organismMap.put("9986", "Oryctolagus cuniculus (9986)");
        organismMap.put("9823", "Sus scrofa (9823)");
        organismMap.put("562", "Escherichia coli (562)");
        organismMap.put("6523", "Lymnaea stagnalis (6523)");
        organismMap.put("9940", "Ovis aries (9940)");
        organismMap.put("208964", "Pseudomonas aeruginosa (208964)");
        organismMap.put("7787", "Tetronarce californica (7787)");
        organismMap.put("243277", "Vibrio cholerae serotype O1 (243277)");
    }

    public String[] getOrganismDisplayNames() {
        return organismMap.values().toArray(new String[0]);
    }

    public String getOrganismId(String displayName) {
        for (Map.Entry<String, String> entry : organismMap.entrySet()) {
            if (entry.getValue().equals(displayName)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
