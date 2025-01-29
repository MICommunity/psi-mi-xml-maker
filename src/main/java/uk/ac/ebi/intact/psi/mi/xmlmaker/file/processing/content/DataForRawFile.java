package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content;

public enum DataForRawFile {
    INTERACTION_DETECTION_METHOD("Interaction Detection Method", false, true),
    PARTICIPANT_DETECTION_METHOD("Participant Detection Method", false, true),
    BAIT_EXPERIMENTAL_PREPARATION("Bait Experimental Preparation", true, false),
    PREY_EXPERIMENTAL_PREPARATION("Prey Experimental Preparation", false, false),
    BAIT_BIOLOGICAL_ROLE("Bait Biological Role", true, false),
    PREY_BIOLOGICAL_ROLE("Prey Biological Role", false, false),

    BAIT_ORGANISM("Bait Organism", true, false),
    PREY_ORGANISM("Prey Organism", false, false),
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
