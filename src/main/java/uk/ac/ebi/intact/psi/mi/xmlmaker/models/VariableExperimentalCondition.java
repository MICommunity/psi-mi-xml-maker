package uk.ac.ebi.intact.psi.mi.xmlmaker.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VariableExperimentalCondition {
    String description;
    String unit;
    String valueColumn;

    public VariableExperimentalCondition() {}
}
