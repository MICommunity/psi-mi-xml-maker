package uk.ac.ebi.intact.psi.mi.xmlmaker.utils;

import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.DataAndMiID;

import java.util.List;

/**
 * Utility class to fetch all the data needed at application opening and only once
 */
public class CacheUtils {
    public static final List<String> UNITS;
    public static final List<String> XREF_QUALIFIERS;
    public static final List<String> FEATURE_TYPES;
    public static final List<String> FEATURE_RANGE_TYPES;
    public static final List<String> FEATURE_ROLES;
    public static final List<String> DATABASES;
    public static final List<String> PARTICIPANT_DETECTION_METHODS;
    public static final List<String> INTERACTION_DETECTION_METHODS;
    public static final List<String> EXPERIMENTAL_PREPARATIONS;
    public static final List<String> BIOLOGICAL_ROLES;
    public static final List<String> PARAMETER_TYPES;

    static {
        UNITS = XmlMakerUtils.getTermsFromOls(DataAndMiID.UNIT.miId);
        XREF_QUALIFIERS = XmlMakerUtils.getTermsFromOls(DataAndMiID.XREF_QUALIFIER.miId);
        FEATURE_TYPES = XmlMakerUtils.getTermsFromOls(DataAndMiID.FEATURE_TYPE.miId);
        FEATURE_RANGE_TYPES = XmlMakerUtils.getTermsFromOls(DataAndMiID.FEATURE_RANGE_TYPE.miId);
        FEATURE_ROLES = XmlMakerUtils.getTermsFromOls(DataAndMiID.FEATURE_ROLE.miId);
        DATABASES = XmlMakerUtils.getTermsFromOls(DataAndMiID.DATABASES.miId);
        PARTICIPANT_DETECTION_METHODS = XmlMakerUtils.getTermsFromOls(DataAndMiID.PARTICIPANT_DETECTION_METHOD.miId);
        INTERACTION_DETECTION_METHODS = XmlMakerUtils.getTermsFromOls(DataAndMiID.INTERACTION_DETECTION_METHOD.miId);
        EXPERIMENTAL_PREPARATIONS = XmlMakerUtils.getTermsFromOls(DataAndMiID.EXPERIMENTAL_PREPARATION.miId);
        BIOLOGICAL_ROLES = XmlMakerUtils.getTermsFromOls(DataAndMiID.BIOLOGICAL_ROLE.miId);
        PARAMETER_TYPES = XmlMakerUtils.getTermsFromOls(DataAndMiID.PARAMETER_TYPE.miId);
    }
}
