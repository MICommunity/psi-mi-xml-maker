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

    public Parameter(String type, String valueColumn, String unit, String base, String exponent, String uncertaintyColumn) {
        this.type = type;
        this.valueColumn = valueColumn;
        this.unit = unit;
        this.base = base;
        this.exponent = exponent;
        this.uncertaintyColumn = uncertaintyColumn;
    }
}
