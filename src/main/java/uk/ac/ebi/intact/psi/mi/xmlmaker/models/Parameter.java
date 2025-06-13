package uk.ac.ebi.intact.psi.mi.xmlmaker.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Parameter {
    String type;
    String valueColumn;
    String unit;
    String base;
    String exponent;
    String uncertaintyColumn;

    public Parameter() {}
}
