package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import org.apache.poi.ss.usermodel.Cell;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public enum DataTypeAndColumn {
    INTERACTION_NUMBER("Interaction number", cell -> String.valueOf(cell.getNumericCellValue())),
    INTERACTION_DETECTION_METHOD("Interaction detection method"),
    PARTICIPANT_NAME("Participant name"),
    PARTICIPANT_TYPE("Participant type"),
    PARTICIPANT_ID("Participant ID"),
    PARTICIPANT_ID_DB("Participant ID database"),
    PARTICIPANT_ORGANISM("Participant taxID"),
    EXPERIMENTAL_ROLE("Experimental role"),
    PARTICIPANT_IDENTIFICATION_METHOD("Participant identification method"),
    PARTICIPANT_XREF("Participant xref"),
    HOST_ORGANISM("Host organism"),
    INTERACTION_TYPE("Interaction type"),
    FEATURE_SHORT_LABEL("Feature short label"),
    FEATURE_TYPE("Feature type"),
    FEATURE_START_STATUS("Feature start status"),
    FEATURE_END_STATUS("Feature end status"),
    EXPERIMENTAL_PREPARATION("Experimental preparation"),;
    
    public final String name;
    public final Function<Cell, String> extractString;
//    public static final Map<String, DataTypeAndColumn> map = new HashMap<>();

    DataTypeAndColumn(String name) {
        this(name, Cell::getStringCellValue);
    }

    DataTypeAndColumn(String name, Function<Cell, String> extractString) {
        this.name = name;
        this.extractString = extractString;
    }

}
