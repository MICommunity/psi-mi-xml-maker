package uk.ac.ebi.intact.psi.mi.xmlmaker.models;

import lombok.Getter;
import lombok.Setter;

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

    private List<String> xref;
    private List<String> xrefDb;
    private List<String> xrefQualifier;

    private boolean induceInteractionParameters;
    private int number;

    private List<Parameter> parameters;

    private String parameterTypes = "";
    private String parameterValues = "";
    private String parameterUnits = "";
    private String parameterBases = "";
    private String parameterExponents = "";
    private String parameterUncertainties = "";

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public Feature() {
        this.xref = new ArrayList<>();
        this.xrefDb = new ArrayList<>();
        this.xrefQualifier = new ArrayList<>();
        this.parameters = new ArrayList<>();
    }

    // Safe setters for lists
    public void setXref(List<String> xref) {
        this.xref = new ArrayList<>(xref);
    }

    public void setXrefDb(List<String> xrefDb) {
        this.xrefDb = new ArrayList<>(xrefDb);
    }

    public void setXrefQualifier(List<String> xrefQualifier) {
        this.xrefQualifier = new ArrayList<>(xrefQualifier);
    }

    public void addXref(String xref) {
        this.xref.add(xref);
    }

    public void removeXref(int index) {
        this.xref.remove(index);
    }

    public void addXrefDb(String xrefDb) {
        this.xrefDb.add(xrefDb);
    }

    public void removeXrefDb(int index) {
        this.xrefDb.remove(index);
    }

    public void addXrefQualifier(String qualifier) {
        this.xrefQualifier.add(qualifier);
    }

    public void removeXrefQualifier(int qualifier) {
        this.xref.remove(qualifier);
    }


    public String getListAsString(List<String> list) {
        return String.join(";", list);
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