package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.creators;

import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.ParameterValue;
import psidev.psi.mi.jami.xml.model.extension.xml300.XmlParameter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class XmlParameterCreator {

    public static List<XmlParameter> createParameter(String parameterType, String parameterValue, String parameterUncertainty, String parameterUnit) {
        List<XmlParameter> parameters = new ArrayList<>();

        String[] parameterValues = parameterValue.split(";");
        String[] parameterUncertainties = parameterUncertainty.split(";");
        String[] parameterUnits = parameterUnit.split(";");
        String[] parameterTypes = parameterType.split(";");

        for (int i = 0; i < parameterValues.length; i++) {
            CvTerm type = XmlMakerUtils.fetchTerm(parameterTypes[i]);
            ParameterValue value = createParameterValue(parameterValues[i]);
            BigDecimal uncertainty = null;
            if (parameterUncertainties.length > 0) {
                uncertainty = getCorrectBigDecimal(parameterUncertainties[i]);
            }
            CvTerm unit = XmlMakerUtils.fetchTerm(parameterUnits[i]);
            parameters.add(new XmlParameter(type, value, uncertainty, unit));
        }

        return parameters;
    }

    private static BigDecimal getCorrectBigDecimal(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(trimmedValue);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static ParameterValue createParameterValue(String value) {
        if (getCorrectBigDecimal(value) != null) {
            return new ParameterValue(getCorrectBigDecimal(value));
        }
        return null;
    }
}
