package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

public enum dataTypeAndColumn {
    INTERACTION_NUMBER("Interaction number"),
    INTERACTION_DETECTION_METHOD("Interaction detection method"),
    PARTICIPANT_NAME("Participant name"),
    PARTICIPANT_TYPE("Participant type"),
    PARTICIPANT_ID("Participant ID"),
    PARTICIPANT_DB("Participant DB"),
    PARTICIPANT_ID_DB("Participant ID database"),
    PARTICIPANT_ORGANISM("Participant taxID"),
    EXPERIMENTAL_ROLE("Experimental role"),
    PARTICIPANT_IDENTIFICATION_METHOD("Participant identification method"),
    HOST_ORGANISM("Host organism"),
    INTERACTION_TYPE("Interaction type"),
    FEATURE_SHORT_LABEL("Feature short label"),
    FEATURE_TYPE("Feature type"),
    FEATURE_START_STATUS("Feature start status"),
    FEATURE_END_STATUS("Feature end status"),
    EXPERIMENTAL_PREPARATION("Experimental preparation"),;
    
    public final String value;
    dataTypeAndColumn(String value) {
        this.value = value;
    }
}
