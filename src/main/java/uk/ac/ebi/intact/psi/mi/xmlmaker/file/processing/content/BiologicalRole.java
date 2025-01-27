package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content;

public enum BiologicalRole {
    ACCEPTOR("Acceptor", "EBI-1813002"),
    ALLOSTERIC_EFFECTOR("Allosteric Effector", "EBI-6350202"),
    ALLOSTERIC_MOLECULE("Allosteric Molecule", "EBI-6350206"),
    ANCILLARY("Ancillary", "EBI-5528713"),
    COFACTOR("Cofactor", "EBI-967823"),
    COMPETITOR("Competitor", "EBI-2474720"),
    DONOR("Donor", "EBI-1813088"),
    ELECTRON_ACCEPTOR("Electron Acceptor", "EBI-704943"),
    ELECTRON_DONOR("Electron Donor", "EBI-704954"),
    ELECTRON_DONOR_ACCEPTOR("Electron Donor/Acceptor", "EBI-8698018"),
    ENZYME("Enzyme", "EBI-46"),
    ENZYME_REGULATOR("Enzyme Regulator", "EBI-9663006"),
    ENZYME_TARGET("Enzyme Target", "EBI-64"),
    INHIBITOR("Inhibitor", "EBI-704931"),
    PHOSPHATE_ACCEPTOR("Phosphate Acceptor", "EBI-1218289"),
    PHOSPHATE_DONOR("Phosphate Donor", "EBI-1218298"),
    PHOTON_ACCEPTOR("Photon Acceptor", "EBI-5528722"),
    PHOTON_DONOR("Photon Donor", "EBI-5528726"),
    PROTON_ACCEPTOR("Proton Acceptor", "EBI-8698158"),
    PROTON_DONOR("Proton Donor", "EBI-8698167"),
    PUTATIVE_SELF("Putative Self", "EBI-1813295"),
    REGULATOR("Regulator", "EBI-16372468"),
    REGULATOR_TARGET("Regulator Target", "EBI-16372493"),
    RIBOZYME("Ribozyme", "EBI-30876060"),
    SELF("Self", "EBI-61"),
    STIMULATOR("Stimulator", "EBI-1218302"),
    SULFATE_ACCEPTOR("Sulfate Acceptor", "EBI-9095157"),
    SULFATE_DONOR("Sulfate Donor", "EBI-9095153"),
    TO_SET("To Set", "EBI-21455119"),
    UNSPECIFIED_ROLE("Unspecified Role", "EBI-77781");

    public final String name;
    public final String ebiId;

    BiologicalRole(String name, String ebiId){
        this.name = name;
        this.ebiId = ebiId;
    }
}
