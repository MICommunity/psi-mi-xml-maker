package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content;

import lombok.Getter;
import lombok.Setter;

import org.apache.poi.ss.usermodel.Cell;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The {@code InputData} enum is representing all the columns added to the formatted file.
 */
public enum InputData {
    //Experiment
    EXPERIMENTAL_PREPARATION("Experimental Preparation", true, true),
    EXPERIMENTAL_VARIABLE_CONDITION_DESCRIPTION("Experimental variable condition description", false, true),
    EXPERIMENTAL_VARIABLE_CONDITION_VALUE("Experimental variable condition value", false, true),
    EXPERIMENTAL_VARIABLE_CONDITION_UNIT("Experimental variable condition unit", false, true),
    HOST_ORGANISM("Host organism", false, true),

    //Interaction
    INTERACTION_NUMBER("Interaction number", cell -> String.valueOf(cell.getNumericCellValue()), false, true),
    INTERACTION_TYPE("Interaction type", false, true),
    INTERACTION_DETECTION_METHOD("Interaction detection method", false, true),
    INTERACTION_FIGURE_LEGEND("Interaction figure legend", false, true),

    INTERACTION_PARAM_TYPE("Interaction parameter type", false, true),
    INTERACTION_PARAM_VALUE("Interaction parameter value", false, true),
    INTERACTION_PARAM_UNIT("Interaction parameter unit", false, true),
    INTERACTION_PARAM_BASE("Interaction parameter base", false, true),
    INTERACTION_PARAM_EXPONENT("Interaction parameter exponent", false, true),
    INTERACTION_PARAM_UNCERTAINTY("Interaction parameter uncertainty", false, true),

    //Participant
    PARTICIPANT_NAME("Participant name", false, true),
    PARTICIPANT_ID("Participant ID", false, true),
    PARTICIPANT_ID_DB("Participant ID database", true, true),
    PARTICIPANT_TYPE("Participant type", false, true),
    PARTICIPANT_ORGANISM("Participant organism", true, true),
    PARTICIPANT_EXPRESSED_IN_ORGANISM("Participant expressed in organism", true, true),
    EXPERIMENTAL_ROLE("Experimental role", false, true),
    PARTICIPANT_IDENTIFICATION_METHOD("Participant identification method", false, true),
    PARTICIPANT_ROW_INDEX("Participant row index", false, true),
    PARTICIPANT_XREF("Participant xref", true, true),
    PARTICIPANT_XREF_DB("Participant xref database", true, true),
    PARTICIPANT_BIOLOGICAL_ROLE("Participant biological role", true, true),

    //Feature metadata
    FEATURE_SHORT_NAME("Feature short name", true, false),
    FEATURE_TYPE("Feature type", true, false),
    FEATURE_ROLE("Feature role", true, false),

    FEATURE_ORIGINAL_SEQUENCE("Feature original sequence", true, false),
    FEATURE_NEW_SEQUENCE("Feature resulting sequence", true, false),

    //Feature range
    FEATURE_START_LOCATION("Feature start", true, false),
    FEATURE_END_LOCATION("Feature end", true, false),
    FEATURE_RANGE_TYPE("Feature range type", true, false),

    //Feature xref
    FEATURE_XREF("Feature xref", true, false),
    FEATURE_XREF_DB("Feature xref database", true, false),
    FEATURE_XREF_QUALIFIER("Feature xref qualifier", true, false),

    //Feature parameter
    FEATURE_PARAM_TYPE("Feature parameter type", true, false),
    FEATURE_PARAM_VALUE("Feature parameter value", true, false),
    FEATURE_PARAM_UNIT("Feature parameter unit", true, false),
    FEATURE_PARAM_BASE("Feature parameter base", true, false),
    FEATURE_PARAM_EXPONENT("Feature parameter exponent", true, false),
    FEATURE_PARAM_UNCERTAINTY("Feature parameter uncertainty", true, false),

    BAIT("Bait", false, false),
    PREY("Prey", false, false),
    ;

    public final String name;
    public final boolean experimentalRoleDependent;
    public final Function<Cell, String> extractString;
    public final boolean initial;
    @Getter @Setter
    private int index;

    InputData(String name, boolean experimentalRoleDependent, boolean initial) {
        this(name, Cell::getStringCellValue, experimentalRoleDependent, initial);
    }

    InputData(String name, Function<Cell, String> extractString, boolean experimentalRoleDependent, boolean initial) {
        this.name = name;
        this.experimentalRoleDependent = experimentalRoleDependent;
        this.extractString = extractString;
        this.initial = initial;
    }

    public static List<String> getNotInitialData() {
        final List<String> notInitialData = new ArrayList<>();
        for (InputData dataType : InputData.values()) {
            if (!dataType.initial) {
                notInitialData.add(dataType.name);
            }
        }
        return notInitialData;
    }

    public static List<String> getInitialData() {
        final List<String> initialData = new ArrayList<>();
        for (InputData dataType : InputData.values()) {
            if (dataType.initial) {
                initialData.add(dataType.name);
            }
        }
        return initialData;
    }
}