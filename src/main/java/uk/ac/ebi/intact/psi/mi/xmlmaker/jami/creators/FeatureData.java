package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.creators;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeatureData {
    private String featureShortName;
    private String featureType;
    private String featureStart;
    private String featureEnd;
    private String featureRangeType;
    private String featureXref;
    private String featureXrefDb;
    private String featureXrefQualifier;
    private String featureRole;
    private String featureOriginalSequence;
    private String featureNewSequence;

    private String parameterType;
    private String parameterValue;
    private String parameterUncertainty;
    private String parameterUnit;
    private String parameterExponent;
    private String parameterBase;
}
