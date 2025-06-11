package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content;

public enum DataAndMiID {

    PARTICIPANT_DETECTION_METHOD("MI:0002"),
    INTERACTION_DETECTION_METHOD("MI:0001"),
    FEATUTRE_TYPE("MI:0116"),
    FEATURE_RANGE_TYPE("MI:0333"),
    EXPERIMENTAL_PREPARATION("MI:0346"),
    DATABASES("MI:0473"),
    BIOLOGICAL_ROLE("MI:0500"),
    XREF_QUALIFIER("MI:0353"),
    UNIT("MI:0647"),
    PARAMETER_TYPE("MI:0640")
    ;



    public final String miId;

    DataAndMiID(String miId) {
        this.miId = miId;
    }
}
