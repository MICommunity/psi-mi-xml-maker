package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content;

import lombok.Getter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml.DataTypeAndColumn;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml.DataTypeAndColumn.*;

public enum DataForRawFile {
    BAIT_ID_DB(PARTICIPANT_ID_DB.name, true, false, false),
    PREY_ID_DB(PARTICIPANT_ID_DB.name, false, false, false),

    INTERACTION_DETECTION_METHOD(DataTypeAndColumn.INTERACTION_DETECTION_METHOD.name, false, true, false),
    PARTICIPANT_DETECTION_METHOD(PARTICIPANT_IDENTIFICATION_METHOD.name, false, true, false),

    INTERACTION_FIGURE_LEGEND(DataTypeAndColumn.INTERACTION_FIGURE_LEGEND.name, false, true, false),

    HOST_ORGANISM(DataTypeAndColumn.HOST_ORGANISM.name, false, true, false),

    BAIT_EXPERIMENTAL_PREPARATION(EXPERIMENTAL_PREPARATION.name, true, false, false),
    PREY_EXPERIMENTAL_PREPARATION(EXPERIMENTAL_PREPARATION.name, false, false, false),

    BAIT_BIOLOGICAL_ROLE("Bait Biological Role", true, false, false),
    PREY_BIOLOGICAL_ROLE("Prey Biological Role", false, false, false),

    BAIT_ORGANISM(PARTICIPANT_ORGANISM.name, true, false, false),
    PREY_ORGANISM(PARTICIPANT_ORGANISM.name, false, false, false),
    PREY_EXPRESSED_IN_ORGANISM(PARTICIPANT_EXPRESSED_IN_ORGANISM.name, false, false, false),
    BAIT_EXPRESSED_IN_ORGANISM(PARTICIPANT_EXPRESSED_IN_ORGANISM.name, true, false, false),

    FEATURE_SHORT_NAME(DataTypeAndColumn.FEATURE_SHORT_NAME.name, false, false, true),
    FEATURE_TYPE(DataTypeAndColumn.FEATURE_TYPE.name, false, false, true),
    FEATURE_START_LOCATION(FEATURE_START.name, false, false, true),
    FEATURE_END_LOCATION(FEATURE_END.name, false, false, true),
    FEATURE_RANGE_TYPE(DataTypeAndColumn.FEATURE_RANGE_TYPE.name, false, false, true),
    FEATURE_XREF(DataTypeAndColumn.FEATURE_XREF.name, false, false, true),
    FEATURE_XREF_DB(DataTypeAndColumn.FEATURE_XREF_DB.name, false, false, true),
    FEATURE_XREF_QUALIFIER(DataTypeAndColumn.FEATURE_XREF_QUALIFIER.name, false, false, true),
    FEATURE_PARAM_TYPE(DataTypeAndColumn.FEATURE_PARAM_TYPE.name, false, false, true),
    FEATURE_PARAM_VALUE(DataTypeAndColumn.FEATURE_PARAM_VALUE.name, false, false, true),
    FEATURE_PARAM_UNIT(DataTypeAndColumn.FEATURE_PARAM_UNIT.name, false, false, true),
    FEATURE_PARAM_BASE(DataTypeAndColumn.FEATURE_PARAM_BASE.name, false, false, true),
    FEATURE_PARAM_EXPONENT(DataTypeAndColumn.FEATURE_PARAM_EXPONENT.name, false, false, true),
    FEATURE_PARAM_UNCERTAINTY(DataTypeAndColumn.FEATURE_PARAM_UNCERTAINTY.name, false, false, true);

    public final String name;
    public final boolean isBait;
    public final boolean isCommon;
    @Getter
    public final boolean isFeature;
    public final String nameAndExperimentalRole;

    DataForRawFile(String name, boolean isBait, boolean isCommon, boolean isFeature) {
        this.name = name;
        this.isBait = isBait;
        this.isCommon = isCommon;
        this.isFeature = isFeature;
        this.nameAndExperimentalRole = name + isBait;
    }
}
