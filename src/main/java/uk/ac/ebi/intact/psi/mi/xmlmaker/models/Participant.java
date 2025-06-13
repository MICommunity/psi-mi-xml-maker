package uk.ac.ebi.intact.psi.mi.xmlmaker.models;

import lombok.Getter;
import lombok.Setter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.Feature;

import java.util.List;

@Getter
@Setter
public class Participant {
    private String id;
    private String name;
    private String idDb;
    private String experimentalRole;
    private String biologicalRole;
    private String taxId;
    private String expressedInTaxId;
    private String experimentalPreparations;

    private String inputId;
    private String inputIdDb;
    private String inputName;

    private List<Feature> features;

}
