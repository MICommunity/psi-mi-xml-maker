package uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UniprotResult {
    private String accession;
    private String uniprotAc;
    private String organism;
    private String entryType;
    private String uniprotLink;

    public UniprotResult(String uniprotAc, String organism, String entryType, String uniprotLink) {
        this.uniprotAc = uniprotAc;
        this.organism = organism;
        this.entryType = entryType;
        this.uniprotLink = uniprotLink;
    }
}
