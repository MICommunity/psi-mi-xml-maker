package uk.ac.ebi.intact.psi.mi.xmlmaker.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Experiment {
    private String hostOrganismTaxId;
    private String interactionsDetectionMethod;
    private String participantsIdentificationMethod;
    List<Interaction> interactions;
}
