package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import lombok.Getter;
import lombok.Setter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.Parameter;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class Feature {
    private String shortName;
    private String type;
    private String startLocation;
    private String endLocation;
    private String rangeType;
    private String originalSequence;
    private String newSequence;
    private boolean fetchFromFile;
    private String role;

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

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public String getListAsString(List<String> list) {
        return String.join(",", list);
    }
    public void setParametersAsString() {
        StringBuilder types = new StringBuilder();
        StringBuilder values = new StringBuilder();
        StringBuilder units = new StringBuilder();
        StringBuilder bases = new StringBuilder();
        StringBuilder exponents = new StringBuilder();
        StringBuilder uncertainties = new StringBuilder();

        for (Parameter parameter : parameters) {
            types.append(Objects.toString(parameter.getType(), "")).append(";");
            values.append(Objects.toString(parameter.getValueColumn(), "")).append(";");
            units.append(Objects.toString(parameter.getUnit(), "")).append(";");
            bases.append(Objects.toString(parameter.getBase(), "")).append(";");
            exponents.append(Objects.toString(parameter.getExponent(), "")).append(";");
            uncertainties.append(Objects.toString(parameter.getUncertaintyColumn(), "")).append(";");
        }

        parameterTypes = types.toString();
        parameterValues = values.toString();
        parameterUnits = units.toString();
        parameterBases = bases.toString();
        parameterExponents = exponents.toString();
        parameterUncertainties = uncertainties.toString();
    }

    public void setFetchFromFile(boolean fetchFromFile) {
        boolean oldValue = this.fetchFromFile;
        this.fetchFromFile = fetchFromFile;
        pcs.firePropertyChange("fetchFromFile", oldValue, fetchFromFile);
    }


    public void addFetchFromFileListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener("fetchFromFile", listener);
    }

}