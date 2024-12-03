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
    HOST_ORGANISM("Host organism"),;
    
    public final String value;
    dataTypeAndColumn(String value) {
        this.value = value;
    }
}
