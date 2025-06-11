package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import lombok.Getter;
import lombok.Setter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.Parameter;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
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
    private String originalSequence;
    private String newSequence;
    private boolean fetchFromFile;

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

    public void setFetchFromFile(boolean fetchFromFile) {
        boolean oldValue = this.fetchFromFile;
        this.fetchFromFile = fetchFromFile;
        pcs.firePropertyChange("fetchFromFile", oldValue, fetchFromFile);
    }


    public void addFetchFromFileListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener("fetchFromFile", listener);
    }

}