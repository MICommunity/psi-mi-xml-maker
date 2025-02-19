package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.Cell;

import java.util.function.Function;

public enum DataTypeAndColumn {
    INTERACTION_NUMBER("Interaction number", cell -> String.valueOf(cell.getNumericCellValue())),
    INTERACTION_TYPE("Interaction type"),
    INTERACTION_DETECTION_METHOD("Interaction detection method"),
    INTERACTION_FIGURE_LEGEND("Interaction feature legend"),
    HOST_ORGANISM("Host organism"),
    EXPERIMENTAL_PREPARATION("Experimental Preparation"),

    PARTICIPANT_NAME("Participant name"),
    PARTICIPANT_ID("Participant ID"),
    PARTICIPANT_ID_DB("Participant ID database"),
    PARTICIPANT_TYPE("Participant type"),
    PARTICIPANT_ORGANISM("Participant organism"),
    PARTICIPANT_EXPRESSED_IN_ORGANISM("Participant expressed in organism"),
    EXPERIMENTAL_ROLE("Experimental role"),
    PARTICIPANT_IDENTIFICATION_METHOD("Participant identification method"),

    PARTICIPANT_XREF("Participant xref"),
    PARTICIPANT_XREF_DB("Participant xref database"),

    FEATURE_TYPE("Feature type", false),
    FEATURE_START("Feature start", false),
    FEATURE_END("Feature end", false),
    FEATURE_RANGE_TYPE("Feature range type", false),
    FEATURE_XREF("Feature xref", false),
    FEATURE_XREF_DB("Feature xref database", false);

    public final String name;
    public final Function<Cell, String> extractString;
    public final boolean initial;
    @Getter @Setter
    private int index;

    DataTypeAndColumn(String name) {
        this(name, Cell::getStringCellValue);
    }

    DataTypeAndColumn(String name, boolean initial) {
        this(name, Cell::getStringCellValue, initial, false);
    }

    DataTypeAndColumn(String name, Function<Cell, String> extractString) {
        this(name, extractString, true, false);
    }

    DataTypeAndColumn(String name, Function<Cell, String> extractString, boolean initial, boolean required) {
        this.name = name;
        this.extractString = extractString;
        this.initial = initial;
    }

}
