package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content;

import java.util.Objects;

public enum DataForRawFile {
    INTERACTION_DETECTION_METHOD("Interaction Detection Method", false, true),
    PARTICIPANT_DETECTION_METHOD("Participant Detection Method", false, true),
    EXPERIMENTAL_PREPARATION("Experimental Preparation", false, true),
    BAIT_BIOLOGICAL_ROLE("Bait Biological Role", true, false),
//    BAIT_EXPERIMENTAL_ROLE("Bait Experimental Role", true, false),
    PREY_BIOLOGICAL_ROLE("Prey Biological Role", false, false),
//    PREY_EXPERIMENTAL_ROLE("Prey Experimental Role", false, false);
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
