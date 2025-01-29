package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content;

public enum ExperimentalPreparation {
    CA_PO_NUC_TRANSFECT("ca po nuc transfect", "EBI-1537749"),
    CDNA_LIBRARY("cdna library", "EBI-1537819"),
    CELL_LYSATE("cell lysate", "EBI-1537757"),
    CONDITIONED_MEDIUM("conditioned medium", "EBI-9095187"),
    CONFORMATION("conformation", "EBI-1812998"),
    DENATURED("denatured", "EBI-1812907"),
    ELECTROPORATION("electroporation", "EBI-1537807"),
    ENGINEERED("engineered", "EBI-1537789"),
    EXPRESSION_MODIF("expression modif", "EBI-1537815"),
    FIXED_CELL("fixed cell", "EBI-1537839"),
    HOMOGENEOUS("homogeneous", "EBI-1537687"),
    HUGE("huge", "EBI-15500394"),
    IN_VITRO_TRANSLATED("in vitro translated", "EBI-1537803"),
    INFECTION("infection", "EBI-1537827"),
    LIVING_CELL("living cell", "EBI-1537765"),
    MICROINJECTION("microinjection", "EBI-1537744"),
    MOLECULAR_SOURCE("molecular source", "EBI-1537835"),
    N_TRANSFECTION_TREAT("n transfection treat", "EBI-1537781"),
    N_TRANSFORMAT_CATION("n transformat cation", "EBI-1537679"),
    NATIVE("native", "EBI-1812743"),
    NATURALLY_OCCURRING("naturally occurring", "EBI-1537843"),
    NUCL_CONJUGATION("nucl conjugation", "EBI-1537852"),
    NUCL_DELIVERY("nucl delivery", "EBI-1537860"),
    NUCL_ELECTROPORATION("nucl electroporation", "EBI-1537728"),
    NUCL_INFECTION("nucl infection", "EBI-1537732"),
    NUCL_LIPOTRANSFECT("nucl lipotransfect", "EBI-1537720"),
    NUCL_MICROINJECTION("nucl microinjection", "EBI-1537692"),
    NUCL_PASSIVE_UPTAKE("nucl passive uptake", "EBI-1537753"),
    NUCL_TRANSDUCTION("nucl transduction", "EBI-1537872"),
    NUCL_TRANSFECTION("nucl transfection", "EBI-1537704"),
    NUCL_TRANSFORMATION("nucl transformation", "EBI-1537712"),
    OVER_EXPRESSED("over-expressed", "EBI-1537736"),
    PARTIALLY_PURIFIED("partially purified", "EBI-1537799"),
    PASSIVE_UPTAKE("passive uptake", "EBI-1537683"),
    PHAGE_LIBRARY("phage library", "EBI-21916084"),
    PHYSIOLOGICAL_LEVEL("physiological level", "EBI-1537793"),
    PROT_CATIONIC_LIPID("prot cationic lipid", "EBI-1537864"),
    PROT_ELECTROPORATION("prot electroporation", "EBI-1537675"),
    PROT_INFECTION("prot infection", "EBI-1537823"),
    PROT_MICROINJECTION("prot microinjection", "EBI-1537700"),
    PROT_PASSIVE_UPTAKE("prot passive uptake", "EBI-1537785"),
    PROTEIN_DELIVERY("protein delivery", "EBI-1537856"),
    PURIFIED("purified", "EBI-1537811"),
    SUBCELLULAR_PREP("subcellular prep", "EBI-1537724"),
    UNDER_EXPRESSED("under-expressed", "EBI-1537761");

    public final String description;
    public final String value;

    ExperimentalPreparation(String description, String value) {
        this.description = description;
        this.value = value;
    }
}
