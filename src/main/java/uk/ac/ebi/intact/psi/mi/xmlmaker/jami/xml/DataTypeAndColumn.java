package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import org.apache.poi.ss.usermodel.Cell;

import java.util.function.Function;

public enum DataTypeAndColumn {
    INTERACTION_NUMBER("Interaction number", cell -> String.valueOf(cell.getNumericCellValue())),
    INTERACTION_TYPE("Interaction type"),
    INTERACTION_DETECTION_METHOD("Interaction detection method"),
    HOST_ORGANISM("Host organism"),
    EXPERIMENTAL_PREPARATION("Experimental preparation"),

    PARTICIPANT_NAME("Participant name"),
    PARTICIPANT_ID("Participant ID"),
    PARTICIPANT_ID_DB("Participant ID database"),
    PARTICIPANT_TYPE("Participant type"),
    PARTICIPANT_ORGANISM("Participant taxID"),
    EXPERIMENTAL_ROLE("Experimental role"),
    PARTICIPANT_IDENTIFICATION_METHOD("Participant identification method"),
    PARTICIPANT_XREF("Participant xref"),

    FEATURE_SHORT_LABEL("Feature short label", false),
    FEATURE_TYPE("Feature type", false),
    FEATURE_START_STATUS("Feature start status", false),
    FEATURE_END_STATUS("Feature end status", false),
    ;

    public final String name;
    public final Function<Cell, String> extractString;
    public final boolean initial;

    DataTypeAndColumn(String name) {
        this(name, Cell::getStringCellValue);
    }

    DataTypeAndColumn(String name, boolean initial) {
        this(name, Cell::getStringCellValue, initial);
    }

    DataTypeAndColumn(String name, Function<Cell, String> extractString) {
        this(name, extractString, true);
    }
    DataTypeAndColumn(String name, Function<Cell, String> extractString, boolean initial) {
        this.name = name;
        this.extractString = extractString;
        this.initial = initial;
    }

}
