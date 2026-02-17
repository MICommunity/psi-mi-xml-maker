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
        List<String> types = new ArrayList<>();
        List<String> values = new ArrayList<>();
        List<String> units = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> exponents = new ArrayList<>();
        List<String> uncertainties = new ArrayList<>();

        for (Parameter parameter : parameters) {
            types.add(Objects.toString(parameter.getType(), ""));
            values.add(Objects.toString(parameter.getValueColumn(), ""));
            units.add(Objects.toString(parameter.getUnit(), ""));
            bases.add(Objects.toString(parameter.getBase(), ""));
            exponents.add(Objects.toString(parameter.getExponent(), ""));
            uncertainties.add(Objects.toString(parameter.getUncertaintyColumn(), ""));
        }

        parameterTypes = String.join(";", types);
        parameterValues = String.join(";", values);
        parameterUnits = String.join(";", units);
        parameterBases = String.join(";", bases);
        parameterExponents = String.join(";", exponents);
        parameterUncertainties = String.join(";", uncertainties);
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