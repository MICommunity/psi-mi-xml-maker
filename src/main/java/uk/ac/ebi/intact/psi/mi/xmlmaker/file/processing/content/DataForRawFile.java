package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content;

public enum DataForRawFile {
    BAIT_ID_DB("Bait ID DB", true, false, false),
    PREY_ID_DB("Prey ID DB", false, false, false),

    INTERACTION_DETECTION_METHOD("Interaction Detection Method", false, true, false),
    PARTICIPANT_DETECTION_METHOD("Participant Detection Method", false, true, false),

    INTERACTION_FIGURE_LEGEND("Interaction Feature Legend", false, true, false),

    HOST_ORGANISM("Host Organism", false, true, false),

    BAIT_EXPERIMENTAL_PREPARATION("Bait Experimental Preparation", true, false, false),
    PREY_EXPERIMENTAL_PREPARATION("Prey Experimental Preparation", false, false, false),

    BAIT_BIOLOGICAL_ROLE("Bait Biological Role", true, false, false),
    PREY_BIOLOGICAL_ROLE("Prey Biological Role", false, false, false),

    BAIT_ORGANISM("Bait Organism", true, false, false),
    PREY_ORGANISM("Prey Organism", false, false, false),
    PREY_EXPRESSED_IN_ORGANISM("Prey Expressed In Organism", false, false, false),
    BAIT_EXPRESSED_IN_ORGANISM("Bait Expressed In Organism", true, false, false),

    FEATURE_TYPE("Feature type", false, false, true),
    FEATURE_START_LOCATION("Feature Start Location", false, false, true),
    FEATURE_END_LOCATION("Feature End Location", false, false, true),
    FEATURE_RANGE_TYPE("Feature Range Type", false, false, true),
    FEATURE_XREF("Feature xref", false, false, true),
    FEATURE_XREF_DB("Feature XRef database", false, false, true),
    ;

    public final String name;
    public final boolean isBait;
    public final boolean isCommon;
    public final boolean isFeature;

    DataForRawFile(String name, boolean isBait, boolean isCommon, boolean isFeature) {
        this.name = name;
        this.isBait = isBait;
        this.isCommon = isCommon;
        this.isFeature = isFeature;
    }
}
