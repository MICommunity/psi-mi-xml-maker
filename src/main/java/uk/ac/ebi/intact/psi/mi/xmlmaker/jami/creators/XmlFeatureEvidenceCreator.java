package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.creators;

import psidev.psi.mi.jami.exception.IllegalRangeException;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.Position;
import psidev.psi.mi.jami.model.ResultingSequence;
import psidev.psi.mi.jami.model.Xref;
import psidev.psi.mi.jami.utils.PositionUtils;
import psidev.psi.mi.jami.xml.model.extension.xml300.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.InputData.*;

/**
 * Utility class responsible for constructing {@link XmlFeatureEvidence} objects from structured input data.
 * <p>
 * This class interprets a {@code Map<String, String>} where each key corresponds to a feature property,
 * typically loaded from structured flat-file data (e.g., tab-delimited format), and assembles the appropriate
 * PSI-MI JAMI XML model components.
 * </p>
 */
public class XmlFeatureEvidenceCreator {

    /**
     * Creates an XmlFeatureEvidence object from the provided data map using the specified feature index.
     *
     * @param featureIndex The index of the feature to be created
     * @param data The map containing all feature data with keys following the pattern "prefix_index"
     * @return The created XmlFeatureEvidence object, or null if the feature couldn't be created
     */
    public static XmlFeatureEvidence createFeature(int featureIndex, Map<String, String> data) {
        String featureIndexString = "_" + featureIndex;

        FeatureData featureData = extractFeatureData(data, featureIndexString);
        if (featureData.getFeatureShortName() == null || featureData.getFeatureShortName().isEmpty()) {
            return null;
        }


        XmlFeatureEvidence featureEvidence = getFeatureEvidence(featureData.getFeatureType(), featureData.getFeatureShortName());

        processFeatureProperties(featureEvidence, featureData);
        setFeatureRangeAndSequence(featureEvidence, featureData);
        setFeatureXrefs(featureEvidence, featureData);

        return featureEvidence;
    }

    /**
     * Extracts all feature-related data from the input map for a specific feature index.
     *
     * @param data The map containing all feature data
     * @param featureIndexString The feature index suffix (e.g. "_1")
     * @return FeatureData object containing all extracted data
     */
    private static FeatureData extractFeatureData(Map<String, String> data, String featureIndexString) {
        FeatureData featureData = new FeatureData();
        featureData.setFeatureShortName(data.get(FEATURE_SHORT_NAME.name + featureIndexString));
        featureData.setFeatureType(data.get(FEATURE_TYPE.name + featureIndexString));
        featureData.setFeatureRole(data.get(FEATURE_ROLE.name + featureIndexString));

        featureData.setFeatureStart(data.get(FEATURE_START_LOCATION.name + featureIndexString));
        featureData.setFeatureEnd(data.get(FEATURE_END_LOCATION.name + featureIndexString));
        featureData.setFeatureRangeType(data.get(FEATURE_RANGE_TYPE.name + featureIndexString));

        featureData.setFeatureXref(data.get(FEATURE_XREF.name + featureIndexString));
        featureData.setFeatureXrefDb(data.get(FEATURE_XREF_DB.name + featureIndexString));
        featureData.setFeatureXrefQualifier(data.get(FEATURE_XREF_QUALIFIER.name + featureIndexString));

        featureData.setFeatureOriginalSequence(data.get(FEATURE_ORIGINAL_SEQUENCE.name + featureIndexString));
        featureData.setFeatureNewSequence(data.get(FEATURE_NEW_SEQUENCE.name + featureIndexString));

        featureData.setParameterType(data.get(FEATURE_PARAM_TYPE.name + featureIndexString));
        featureData.setParameterValue(data.get(FEATURE_PARAM_VALUE.name + featureIndexString));
        featureData.setParameterUncertainty(data.get(FEATURE_PARAM_UNCERTAINTY.name + featureIndexString));
        featureData.setParameterUnit(data.get(FEATURE_PARAM_UNIT.name + featureIndexString));
        featureData.setParameterExponent(data.get(FEATURE_PARAM_EXPONENT.name + featureIndexString));
        featureData.setParameterBase(data.get(FEATURE_PARAM_BASE.name + featureIndexString));

        return featureData;
    }

    /**
     * Processes additional feature properties including qualifier, role, and parameters.
     *
     * @param featureEvidence The feature to modify
     * @param featureData The data containing the properties to process
     */
    private static void processFeatureProperties(XmlFeatureEvidence featureEvidence, FeatureData featureData) {
        if (featureData.getFeatureRole() != null && !featureData.getFeatureRole().isEmpty()) {
            CvTerm featureRoleCv = XmlMakerUtils.fetchTerm(featureData.getFeatureRole());
            if (featureRoleCv != null) {
                featureEvidence.setRole(featureRoleCv);
            }
        }

        if (featureData.getParameterType() != null && !featureData.getParameterType().isEmpty()) {
            featureData.setParameterType(featureData.getParameterType());
            addParametersToFeature(featureEvidence, featureData);
        }
    }

    /**
     * Adds parameters to the feature evidence from the provided feature data.
     *
     * @param featureEvidence The feature to add parameters to
     * @param featureData The data containing the parameters
     */
    private static void addParametersToFeature(XmlFeatureEvidence featureEvidence, FeatureData featureData) {
        List<XmlParameter> featureParameters = XmlParameterCreator.createParameter(
                featureData.getParameterType(),
                featureData.getParameterValue(),
                featureData.getParameterUncertainty(),
                featureData.getParameterUnit(),
                featureData.getParameterExponent(),
                featureData.getParameterBase()
        );

        for (XmlParameter interactionParameter : featureParameters) {
            if (!featureEvidence.getParameters().contains(interactionParameter)) {
                featureEvidence.getParameters().add(interactionParameter);
            }
        }
    }

    /**
     * Sets the range and resulting sequence for the feature evidence.
     *
     * @param featureEvidence The feature to modify
     * @param featureData The data containing range and sequence information
     */
    private static void setFeatureRangeAndSequence(XmlFeatureEvidence featureEvidence, FeatureData featureData) {
        Position positionStart = getRangePosition(featureData.getFeatureStart(), featureData.getFeatureRangeType());
        Position positionEnd = getRangePosition(featureData.getFeatureEnd(), featureData.getFeatureRangeType());
        XmlRange featureRange = getFeatureRange(positionStart, positionEnd);

        ResultingSequence resultingSequence = new XmlResultingSequence(
                featureData.getFeatureOriginalSequence(),
                featureData.getFeatureNewSequence()
        );
        featureRange.setResultingSequence(resultingSequence);

        featureEvidence.setJAXBRangeWrapper(new AbstractXmlFeature.JAXBRangeWrapper());
        featureEvidence.getRanges().add(featureRange);
    }

    /**
     * Constructs an {@link XmlRange} from start and end {@link Position} objects.
     *
     * @param startPosition The start position of the feature.
     * @param endPosition The end position of the feature.
     * @return An {@link XmlRange} representing the positional span of the feature.
     */
    private static XmlRange getFeatureRange(Position startPosition, Position endPosition) {
        XmlRange featureRange = new XmlRange();
        featureRange.setPositions(startPosition, endPosition);
        return featureRange;
    }

    /**
     * Determines the {@link Position} of a feature range from the string value and range type.
     * This handles numeric ranges, undetermined ranges, and special terminal positions (N-term/C-term).
     *
     * @param range The position as a string (e.g., "42").
     * @param featureRangeType A description of the type (e.g., "n-term", "c-term").
     * @return A {@link Position} object representing the resolved range.
     */
    public static Position getRangePosition(String range, String featureRangeType) {
        if (range == null) {
            return PositionUtils.createUndeterminedPosition();
        }

        range = range.trim().replaceAll(";$", "");

        if (featureRangeType != null) {
            String lowerFeatureRangeType = featureRangeType.toLowerCase();
            if (lowerFeatureRangeType.contains("c-term")) {
                return PositionUtils.createCTerminalRangePosition();
            }
            if (lowerFeatureRangeType.contains("n-term")) {
                return PositionUtils.createNTerminalRangePosition();
            }
        }

        if (range.matches("\\d+")) {
            try {
                return PositionUtils.createPositionFromString(range);
            } catch (IllegalRangeException e) {
                throw new RuntimeException("Invalid range: " + range, e);
            }
        }

        return PositionUtils.createUndeterminedPosition();
    }

    /**
     * Sets the xrefs for the feature evidence.
     *
     * @param featureEvidence The feature to modify
     * @param featureData The data containing xref information
     */
    private static void setFeatureXrefs(XmlFeatureEvidence featureEvidence, FeatureData featureData) {
        FeatureXrefContainer featureXrefContainer = getFeatureXrefContainer(
                featureData.getFeatureXref(),
                featureData.getFeatureXrefDb(),
                featureData.getFeatureXrefQualifier()
        );
        featureEvidence.setJAXBXref(featureXrefContainer);
    }

    /**
     * Constructs a {@link FeatureXrefContainer} from provided xref and database name lists.
     *
     * @param featureXref A semicolon-separated list of feature xrefs (e.g., "P12345;Q67890").
     * @param featureXrefDb A semicolon-separated list of corresponding database names (e.g., "UniProtKB;RefSeq").
     * @return A {@link FeatureXrefContainer} containing all valid {@link Xref}s.
     */
    private static FeatureXrefContainer getFeatureXrefContainer(String featureXref, String featureXrefDb, String featureXrefType) {
        FeatureXrefContainer featureXrefContainer = new FeatureXrefContainer();

        if (featureXref != null && featureXrefDb != null) {
            List<String> featuresXrefs = getFeatureXrefs(featureXref);
            List<CvTerm> featuresXrefsDb = getFeatureXrefsDb(featureXrefDb);
            List<CvTerm> featureXrefsQualifiers = featureXrefType != null ?
                    getFeatureXrefsType(featureXrefType) : Collections.emptyList();

            int size = Math.min(featuresXrefs.size(), featuresXrefsDb.size());
            if (!featureXrefsQualifiers.isEmpty()) {
                size = Math.min(size, featureXrefsQualifiers.size());
            }

            for (int i = 0; i < size; i++) {
                String xref = featuresXrefs.get(i);
                CvTerm xrefDb = featuresXrefsDb.get(i);
                CvTerm xrefQualifier = !featureXrefsQualifiers.isEmpty() ?
                        featureXrefsQualifiers.get(i) : null;

                if (xref != null && xrefDb != null) {
                    Xref featureXrefXml = (xrefQualifier != null) ?
                            new XmlXref(xrefDb, xref, xrefQualifier) :
                            new XmlXref(xrefDb, xref);
                    featureXrefContainer.getXrefs().add(featureXrefXml);
                }
            }
        }

        return featureXrefContainer;
    }

    /**
     * Converts a semicolon-separated list of database names into a list of {@link CvTerm}s.
     *
     * @param featureXrefDb A semicolon-separated string of database names.
     * @return A list of {@link CvTerm} objects representing the databases.
     */
    private static List<CvTerm> getFeatureXrefsDb(String featureXrefDb) {
        if (featureXrefDb == null || featureXrefDb.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(featureXrefDb.split(";"))
                .map(String::trim)
                .map(XmlMakerUtils::fetchTerm)
                .collect(Collectors.toList());
    }

    /**
     * Converts a semicolon-separated list of database names into a list of {@link CvTerm}s.
     *
     * @param featureXrefType A semicolon-separated string of database names.
     * @return A list of {@link CvTerm} objects representing the databases.
     */
    private static List<CvTerm> getFeatureXrefsType(String featureXrefType) {
        if (featureXrefType == null || featureXrefType.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(featureXrefType.split(";"))
                .map(String::trim)
                .map(XmlMakerUtils::fetchTerm)
                .collect(Collectors.toList());
    }

    /**
     * Converts a semicolon-separated list of xref identifiers into a list of strings.
     *
     * @param featureXref A semicolon-separated string of xref identifiers.
     * @return A list of xref strings.
     */
    private static List<String> getFeatureXrefs(String featureXref) {
        if (featureXref == null || featureXref.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(featureXref.split(";"))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * Constructs a basic {@link XmlFeatureEvidence} object given a feature type and optional short label.
     *
     * @param featureType The descriptive name of the feature type (e.g., "phosphorylation site").
     * @param featureShortLabel A short label for the feature (maybe {@code null}).
     * @return An initialized {@link XmlFeatureEvidence} object, or {@code null} if MI identifier could not be resolved.
     */
    private static XmlFeatureEvidence getFeatureEvidence(String featureType, String featureShortLabel) {
        XmlFeatureEvidence featureEvidence = new XmlFeatureEvidence();

        CvTerm featureTypeCv = XmlMakerUtils.fetchTerm(featureType);
        if (featureTypeCv != null) {
            featureEvidence.setType(featureTypeCv);
        }

        featureEvidence.setShortName(featureShortLabel);
        return featureEvidence;
    }
}
