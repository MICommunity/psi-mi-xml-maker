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

import static uk.ac.ebi.intact.psi.mi.xmlmaker.jami.DataTypeAndColumn.*;

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
     * Creates an {@link XmlFeatureEvidence} object based on a feature index and the input data map.
     *
     * @param featureIndex The index of the feature in the dataset. This is used to resolve suffixes for column names.
     * @param data A map of column name to value for all parsed data entries.
     * @return A populated {@link XmlFeatureEvidence} object, or {@code null} if essential data is missing or invalid.
     */
    public static XmlFeatureEvidence createFeature(int featureIndex, Map<String, String> data) {
        String featureIndexString = "_" + featureIndex;

        String featureShortName = data.get(FEATURE_SHORT_NAME.name + featureIndexString);
        String featureType = data.get(FEATURE_TYPE.name + featureIndexString);
        String featureStart = data.get(FEATURE_START.name + featureIndexString);
        String featureEnd = data.get(FEATURE_END.name + featureIndexString);
        String featureRangeType = data.get(FEATURE_RANGE_TYPE.name + featureIndexString);
        String featureXref = data.get(FEATURE_XREF.name + featureIndexString);
        String featureXrefDb = data.get(FEATURE_XREF_DB.name + featureIndexString);
        String featureRole = data.get(FEATURE_ROLE.name + featureIndexString);

        String featureOriginalSequence = data.get(FEATURE_ORIGINAL_SEQUENCE.name + featureIndexString);
        String featureNewSequence = data.get(FEATURE_NEW_SEQUENCE.name + featureIndexString);

        Position positionStart = getRangePosition(featureStart, featureRangeType);
        Position positionEnd = getRangePosition(featureEnd, featureRangeType);
        XmlRange featureRange = getFeatureRange(positionStart, positionEnd);

        String parameterType = data.get(FEATURE_PARAM_TYPE.name + featureIndexString);
        String parameterValue = data.get(FEATURE_PARAM_VALUE.name + featureIndexString);
        String parameterUncertainty = data.get(FEATURE_PARAM_UNCERTAINTY.name + featureIndexString);
        String parameterUnit = data.get(FEATURE_PARAM_UNIT.name + featureIndexString);
        String parameterExponent = data.get(FEATURE_PARAM_EXPONENT.name + featureIndexString);
        String parameterBase = data.get(FEATURE_PARAM_BASE.name + featureIndexString);

        XmlFeatureEvidence featureEvidence = getFeatureEvidence(featureType, featureShortName);
        if (featureEvidence == null) {
            return null;
        }

        if (featureEvidence.getType().getMIIdentifier() == null || featureEvidence.getType().getMIIdentifier().isEmpty()) {
            return null;
        }

        if (featureRole != null && !featureRole.isEmpty()) {
            CvTerm featureRoleCv = XmlMakerUtils.fetchTerm(featureRole);
            if (featureRoleCv != null) {
                featureEvidence.setRole(featureRoleCv);
            }
        }

        if (parameterType != null && !parameterType.isEmpty()) {
            if (parameterType.endsWith(";")){
                parameterType = parameterType.substring(0, parameterType.length() - 1);
            }

            List<XmlParameter> featureParameters = XmlParameterCreator.createParameter(parameterType, parameterValue, parameterUncertainty, parameterUnit,  parameterExponent, parameterBase);

            for (XmlParameter interactionParameter : featureParameters) {
                if (!featureEvidence.getParameters().contains(interactionParameter)) {
                    featureEvidence.getParameters().add(interactionParameter);
                }
            }
        }

        ResultingSequence resultingSequence = new XmlResultingSequence(featureOriginalSequence, featureNewSequence);
        featureRange.setResultingSequence(resultingSequence);

        FeatureXrefContainer featureXrefContainer = getFeatureXrefContainer(featureXref, featureXrefDb);

        featureEvidence.setJAXBXref(featureXrefContainer);
        featureEvidence.setJAXBRangeWrapper(new AbstractXmlFeature.JAXBRangeWrapper());
        featureEvidence.getRanges().add(featureRange);

        return featureEvidence;
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
     * Constructs a {@link FeatureXrefContainer} from provided xref and database name lists.
     *
     * @param featureXref A semicolon-separated list of feature xrefs (e.g., "P12345;Q67890").
     * @param featureXrefDb A semicolon-separated list of corresponding database names (e.g., "UniProtKB;RefSeq").
     * @return A {@link FeatureXrefContainer} containing all valid {@link Xref}s.
     */
    private static FeatureXrefContainer getFeatureXrefContainer(String featureXref, String featureXrefDb) {
        FeatureXrefContainer featureXrefContainer = new FeatureXrefContainer();

        if (featureXref != null && featureXrefDb != null) {
            List<String> featuresXrefs = getFeatureXrefs(featureXref);
            List<CvTerm> featuresXrefsDb = getFeatureXrefsDb(featureXrefDb);

            // Ensure both lists are the same size before proceeding
            int size = Math.min(featuresXrefs.size(), featuresXrefsDb.size());

            for (int i = 0; i < size; i++) {
                String xref = featuresXrefs.get(i);
                CvTerm xrefDb = featuresXrefsDb.get(i);
                if (xref != null && xrefDb != null) {
                    Xref featureXrefXml = new XmlXref(xrefDb, xref);
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
     * @param featureShortLabel A short label for the feature (may be {@code null}).
     * @return An initialized {@link XmlFeatureEvidence} object, or {@code null} if MI identifier could not be resolved.
     */
    private static XmlFeatureEvidence getFeatureEvidence(String featureType, String featureShortLabel) {
        String featureTypeMiId = XmlMakerUtils.fetchMiId(featureType);
        if (featureTypeMiId == null || featureTypeMiId.trim().isEmpty()) {
            return null;
        }
        CvTerm featureTypeCv = new XmlCvTerm(featureType, featureTypeMiId);

        XmlFeatureEvidence featureEvidence = new XmlFeatureEvidence(featureTypeCv);
        featureEvidence.setShortName(featureShortLabel);
        return featureEvidence;
    }

}
