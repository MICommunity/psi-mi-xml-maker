package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content;

public enum DataForRawFile {
    BAIT_ID_DB("Bait ID DB", true, false),
    PREY_ID_DB("Prey ID DB", false, false),

    INTERACTION_DETECTION_METHOD("Interaction Detection Method", false, true),
    PARTICIPANT_DETECTION_METHOD("Participant Detection Method", false, true),

    BAIT_EXPERIMENTAL_PREPARATION("Bait Experimental Preparation", true, false),
    PREY_EXPERIMENTAL_PREPARATION("Prey Experimental Preparation", false, false),

    BAIT_BIOLOGICAL_ROLE("Bait Biological Role", true, false),
    PREY_BIOLOGICAL_ROLE("Prey Biological Role", false, false),

    BAIT_ORGANISM("Bait Organism", true, false),
    PREY_ORGANISM("Prey Organism", false, false),

    BAIT_FEATURE_TYPE("Bait Feature type", true, false),
    BAIT_FEATURE_START_LOCATION("Bait Feature Start Location", true, false),
    BAIT_FEATURE_END_LOCATION("Bait Feature End Location", true, false),
    BAIT_FEATURE_RANGE_TYPE("Bait Feature Range Type", true, false),
    ;

    public final String name;
    public final boolean isBait;
    public final boolean isCommon;

    DataForRawFile(String name, boolean isBait, boolean isCommon) {
        this.name = name;
        this.isBait = isBait;
        this.isCommon = isCommon;
    }
}
