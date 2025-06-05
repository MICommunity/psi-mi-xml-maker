package uk.ac.ebi.intact.psi.mi.xmlmaker.jami;

import lombok.Getter;
import lombok.Setter;

import org.apache.poi.ss.usermodel.Cell;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public enum DataTypeAndColumn {
    INTERACTION_NUMBER("Interaction number", cell -> String.valueOf(cell.getNumericCellValue())),
    INTERACTION_TYPE("Interaction type"),
    INTERACTION_DETECTION_METHOD("Interaction detection method"),
    INTERACTION_FIGURE_LEGEND("Interaction feature legend"),

    INTERACTION_PARAM_TYPE("Interaction parameter type"),
    INTERACTION_PARAM_VALUE("Interaction parameter value"),
    INTERACTION_PARAM_UNIT("Interaction parameter unit"),
    INTERACTION_PARAM_BASE("Interaction parameter base"),
    INTERACTION_PARAM_EXPONENT("Interaction parameter exponent"),
    INTERACTION_PARAM_UNCERTAINTY("Interaction parameter uncertainty"),

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
    PARTICIPANT_ROW_INDEX("Participant row index"),

    PARTICIPANT_XREF("Participant xref"),
    PARTICIPANT_XREF_DB("Participant xref database"),

    FEATURE_SHORT_NAME("Feature short name", false),
    FEATURE_TYPE("Feature type", false),
    FEATURE_START("Feature start", false),
    FEATURE_END("Feature end", false),
    FEATURE_RANGE_TYPE("Feature range type", false),
    FEATURE_XREF("Feature xref", false),
    FEATURE_XREF_DB("Feature xref database", false),
    FEATURE_XREF_QUALIFIER("Feature xref qualifier", false),
    FEATURE_PARAM_TYPE("Feature parameter type", false),
    FEATURE_PARAM_VALUE("Feature parameter value", false),
    FEATURE_PARAM_UNIT("Feature parameter unit", false),
    FEATURE_PARAM_BASE("Feature parameter base", false),
    FEATURE_PARAM_EXPONENT("Feature parameter exponent", false),
    FEATURE_PARAM_UNCERTAINTY("Feature parameter uncertainty", false),
    FEATURE_ORIGINAL_SEQUENCE("Feature original sequence", false),
    FEATURE_NEW_SEQUENCE("Feature resulting sequence", false),
    ;

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


    public static List<String> getNotInitialData(){
        final List<String> notInitialData = new ArrayList<>();
        for (DataTypeAndColumn dataType : DataTypeAndColumn.values()) {
            if (!dataType.initial) {
                notInitialData.add(dataType.name);
            }
        }
        return notInitialData;
    }
}
