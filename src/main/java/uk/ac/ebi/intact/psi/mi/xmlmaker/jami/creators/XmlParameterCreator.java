package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.creators;

import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.ParameterValue;
import psidev.psi.mi.jami.xml.model.extension.xml300.XmlParameter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.GuiUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Utility class for creating {@link XmlParameter} instances from input strings.
 */
public class XmlParameterCreator {

    private static final Logger LOGGER = Logger.getLogger(XmlParameterCreator.class.getName());
    /**
     * Creates a list of {@link XmlParameter} objects from the provided parameter details.
     *
     * @param parameterType       Semicolon-separated parameter types.
     * @param parameterValue      Semicolon-separated parameter values.
     * @param parameterUncertainty Semicolon-separated uncertainties (optional).
     * @param parameterUnit       Semicolon-separated units.
     * @param parameterExponent   Semicolon-separated exponents (optional).
     * @param parameterBase       Semicolon-separated bases (optional).
     * @return A list of constructed {@link XmlParameter} instances.
     * @throws IllegalArgumentException if required, values are missing or mismatched.
     */
    public static List<XmlParameter> createParameter(
            String parameterType,
            String parameterValue,
            String parameterUncertainty,
            String parameterUnit,
            String parameterExponent,
            String parameterBase) {

        Objects.requireNonNull(parameterType, "Parameter type cannot be null");
        Objects.requireNonNull(parameterValue, "Parameter value cannot be null");

        List<XmlParameter> parameters = new ArrayList<>();

        String[] parameterValues = splitSafely(parameterValue);
        String[] parameterUncertainties = splitSafely(parameterUncertainty);
        String[] parameterUnits = splitSafely(parameterUnit);
        String[] parameterTypes = splitSafely(parameterType);
        String[] parameterExponents = splitSafely(parameterExponent);
        String[] parameterBases = splitSafely(parameterBase);

        int paramCount = parameterValues.length;
        if (parameterTypes.length != paramCount) {
            throw new IllegalArgumentException("Parameter type count doesn't match value count");
        }
        if (parameterUnits.length != paramCount) {
            throw new IllegalArgumentException("Parameter unit count doesn't match value count");
        }

        for (int i = 0; i < paramCount; i++) {
            try {
                CvTerm type = XmlMakerUtils.fetchTerm(parameterTypes[i]);
                ParameterValue value = createParameterValue(parameterValues[i]);

                if (value == null) {
                    throw new IllegalArgumentException("Invalid parameter value at index " + i);
                }

                BigDecimal uncertainty = parseBigDecimal(parameterUncertainties, i);
                Short exponent = parseShort(parameterExponents, i);
                Short base = parseShort(parameterBases, i);
                CvTerm unit = XmlMakerUtils.fetchTerm(parameterUnits[i]);

                XmlParameter xmlParameter = new XmlParameter(type, value, uncertainty, unit);
                if (exponent != null) {
                    xmlParameter.setJAXBExponent(exponent);
                }
                if (base != null) {
                    xmlParameter.setJAXBBase(base);
                }
                parameters.add(xmlParameter);
            } catch (Exception e) {
                LOGGER.warning("Failed to create parameter :" + e.getMessage());
                GuiUtils.showErrorDialog("Failed to created parameter: " + e.getMessage());
            }
        }

        return parameters;
    }

    /**
     * Splits a string by semicolon safely, returning an empty array if the input is null.
     *
     * @param input Input string to split.
     * @return An array of strings.
     */
    private static String[] splitSafely(String input) {
        return input == null ? new String[0] : input.split(";");
    }

    /**
     * Parses a {@link BigDecimal} from the provided array at the specified index.
     *
     * @param values Array of string values.
     * @param index  Index to parse from.
     * @return A {@link BigDecimal}, or null if input is missing or empty.
     * @throws IllegalArgumentException if the value is not a valid decimal.
     */
    private static BigDecimal parseBigDecimal(String[] values, int index) {
        if (values == null || index >= values.length || values[index] == null || values[index].trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(values[index].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid decimal value: " + values[index], e);
        }
    }

    /**
     * Parses a {@link Short} from the provided array at the specified index.
     *
     * @param values Array of string values.
     * @param index  Index to parse from.
     * @return A {@link Short}, or null if input is missing, empty, or not a valid short.
     */
    private static Short parseShort(String[] values, int index) {
        if (values == null || index >= values.length || values[index] == null || values[index].trim().isEmpty()) {
            return null;
        }
        try {
            return Short.parseShort(values[index].trim());
        } catch (NumberFormatException e) {
            LOGGER.warning("Failed to parse to short: " + e.getMessage());
            return null;
        }
    }

    /**
     * Converts a single string value into a {@link ParameterValue} if valid.
     *
     * @param value The string representation of a decimal value.
     * @return A {@link ParameterValue}, or null if the input is invalid.
     */
    private static ParameterValue createParameterValue(String value) {
        BigDecimal decimalValue = parseBigDecimal(new String[]{value}, 0);
        return decimalValue != null ? new ParameterValue(decimalValue) : null;
    }
}