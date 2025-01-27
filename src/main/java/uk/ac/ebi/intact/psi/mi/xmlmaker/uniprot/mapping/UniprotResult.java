package uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UniprotResult {
    private String name;
    private String uniprotAc;
    private String organism;
    private String entryType;
    private String uniprotLink;
    private String idDb;

    public UniprotResult(String uniprotAc, String name, String organism, String entryType, String uniprotLink, String idDb) {
        this.uniprotAc = uniprotAc;
        this.name = name;
        this.organism = organism;
        this.entryType = entryType;
        this.uniprotLink = uniprotLink;
        this.idDb = idDb;
    }
}
