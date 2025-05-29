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
import static uk.ac.ebi.intact.psi.mi.xmlmaker.jami.DataTypeAndColumn.FEATURE_END;
import static uk.ac.ebi.intact.psi.mi.xmlmaker.jami.DataTypeAndColumn.FEATURE_RANGE_TYPE;
import static uk.ac.ebi.intact.psi.mi.xmlmaker.jami.DataTypeAndColumn.FEATURE_RESULTING_SEQUENCE;
import static uk.ac.ebi.intact.psi.mi.xmlmaker.jami.DataTypeAndColumn.FEATURE_XREF;
import static uk.ac.ebi.intact.psi.mi.xmlmaker.jami.DataTypeAndColumn.FEATURE_XREF_DB;

public class XmlFeatureEvidenceCreator {

    public static XmlFeatureEvidence createFeature(int featureIndex, Map<String, String> data) {
        String featureIndexString = "_" + featureIndex;

        String featureShortName = data.get(FEATURE_SHORT_NAME.name + featureIndexString);
        String featureType = data.get(FEATURE_TYPE.name + featureIndexString);
        String featureStart = data.get(FEATURE_START.name + featureIndexString);
        String featureEnd = data.get(FEATURE_END.name + featureIndexString);
        String featureRangeType = data.get(FEATURE_RANGE_TYPE.name + featureIndexString);
        String featureXref = data.get(FEATURE_XREF.name + featureIndexString);
        String featureXrefDb = data.get(FEATURE_XREF_DB.name + featureIndexString);

        String featureResultingSequence = data.get(FEATURE_RESULTING_SEQUENCE.name + featureIndexString);

        XmlFeatureEvidence featureEvidence = getFeatureEvidence(featureType, featureShortName);

        if (featureEvidence.getType().getMIIdentifier() == null || featureEvidence.getType().getMIIdentifier().isEmpty()) {
            return null;
        }

        Position positionStart = getRangePosition(featureStart, featureRangeType);
        Position positionEnd = getRangePosition(featureEnd, featureRangeType);
        XmlRange featureRange = getFeatureRange(positionStart, positionEnd);


        String parameterType = data.get(FEATURE_PARAM_TYPE.name + featureIndexString);
        String parameterValue = data.get(FEATURE_PARAM_VALUE.name + featureIndexString);
        String parameterUncertainty = data.get(FEATURE_PARAM_UNCERTAINTY.name + featureIndexString);
        String parameterUnit = data.get(FEATURE_PARAM_UNIT.name + featureIndexString);

        List<XmlParameter> featureParameters = XmlParameterCreator.createParameter(parameterType, parameterValue, parameterUncertainty, parameterUnit);

        for (XmlParameter interactionParameter : featureParameters) {
            if (!featureEvidence.getParameters().contains(interactionParameter)) {
                featureEvidence.getParameters().add(interactionParameter);
            }
        }

        ResultingSequence resultingSequence = new XmlResultingSequence();
        resultingSequence.setNewSequence("test");
        featureRange.setResultingSequence(resultingSequence);

        FeatureXrefContainer featureXrefContainer = getFeatureXrefContainer(featureXref, featureXrefDb);

        featureEvidence.setJAXBXref(featureXrefContainer);
        featureEvidence.setJAXBRangeWrapper(new AbstractXmlFeature.JAXBRangeWrapper());
        featureEvidence.getRanges().add(featureRange);

        return featureEvidence;
    }

    private static XmlRange getFeatureRange(Position startPosition, Position endPosition) {
        XmlRange featureRange = new XmlRange();
        featureRange.setPositions(startPosition, endPosition);
        return featureRange;
    }

    public static Position getRangePosition(String range, String featureRangeType) {
        if (featureRangeType.contains("c-term")) return PositionUtils.createCTerminalRangePosition();

        if (featureRangeType.contains("n-term")) return PositionUtils.createNTerminalRangePosition();

        if (range.matches("\\d+")) {
            try {
                return PositionUtils.createPositionFromString(range);
            } catch (IllegalRangeException e) {
                throw new RuntimeException(e);
            }
        }

        return PositionUtils.createUndeterminedPosition();
    }

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

    private static List<CvTerm> getFeatureXrefsDb(String featureXrefDb) {
        if (featureXrefDb == null || featureXrefDb.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(featureXrefDb.split(";"))
                .map(String::trim)
                .map(XmlMakerUtils::fetchTerm)
                .collect(Collectors.toList());
    }

    private static List<String> getFeatureXrefs(String featureXref) {
        if (featureXref == null || featureXref.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(featureXref.split(";"))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private static XmlFeatureEvidence getFeatureEvidence(String featureType, String featureShortLabel) {
        String featureTypeMiId = XmlMakerUtils.fetchMiId(featureType);
        CvTerm featureTypeCv = new XmlCvTerm(featureType, featureTypeMiId);

        XmlFeatureEvidence featureEvidence = new XmlFeatureEvidence(featureTypeCv);
        featureEvidence.setShortName(featureShortLabel);
        return featureEvidence;
    }

}
