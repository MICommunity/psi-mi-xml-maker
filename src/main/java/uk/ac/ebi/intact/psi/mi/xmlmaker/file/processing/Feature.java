package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import lombok.Getter;
import lombok.Setter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.Parameter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Feature {
    private String shortName;
    private String type;
    private String startLocation;
    private String endLocation;
    private String rangeType;
    private String resultingSequence;

    private List<String> xref = new ArrayList<>();
    private List<String> xrefDb = new ArrayList<>();
    private List<String> xrefQualifier  = new ArrayList<>();

    private boolean induceInteractionParameters;
    private int number;

    private List<Parameter> parameters = new ArrayList<>();

    private String parameterTypes = "";
    private String parameterValues = "";
    private String parameterUnits = "";
    private String parameterBases = "";
    private String parameterExponents = "";
    private String parameterUncertainties = "";

    public String getListAsString(List<String> list) {
        return String.join(";", list);
    }

    public void setParametersAsString(){
        for (Parameter parameter : parameters) {
            parameterTypes += parameter.getType() + ";";
            parameterValues += parameter.getValueColumn() + ";";
            parameterUnits += parameter.getUnit() + ";";
            parameterBases += parameter.getBase() + ";";
            parameterExponents += parameter.getExponent() + ";";
            parameterUncertainties += parameter.getUncertaintyColumn() + ";";
        }
    }

    public void printInformation(){
        System.out.println("ShortName: " + shortName);
        System.out.println("Type: " + type);
        System.out.println("StartLocation: " + startLocation);
        System.out.println("EndLocation: " + endLocation);
        System.out.println("RangeType: " + rangeType);
        System.out.println("Xref: " + xref);
        System.out.println("XrefDb: " + xrefDb);
        System.out.println("XrefQualifier: " + xrefQualifier);
        System.out.println("InduceInteractionParameters: " + induceInteractionParameters);
        System.out.println("Number: " + number);
        System.out.println("Parameters: " + parameters);
    }
}
