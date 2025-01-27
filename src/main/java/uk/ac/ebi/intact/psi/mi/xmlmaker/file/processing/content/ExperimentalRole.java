package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content;

public enum ExperimentalRole {
    ANCILLARY("ancillary", "EBI-967819"),
    BAIT("bait", "EBI-49"),
    CHEMILUMINISCENCE_DONOR("chemiluminiscence donor", "EBI-21916045"),
    ENHANCED("enhanced", "EBI-25613861"),
    ENHANCER("enhancer", "EBI-25613856"),
    EPISTATIC("epistatic", "EBI-25613846"),
    FLUORESCENCE_ACCEPT("fluorescence accept", "EBI-704935"),
    FLUORESCENCE_DONOR("fluorescence donor", "EBI-704939"),
    FRET_PAIR("freet pair", "EBI-1372455"),
    HYPOSTATIC("hypostatic", "EBI-25613851"),
    LUMINESCENCE_ACCEPT("luminescence accept", "EBI-21916039"),
    LUMINESCENCE_ACCEPT_DONOR("luminescence acceptor donor pair", "EBI-21916030"),
    LUMINESCENCE_DONOR("luminescence donor", "EBI-21916035"),
    LUMINESCENCE_TRANSMITTER("luminescence transmitter", "EBI-21941773"),
    NEUTRAL_COMPONENT("neutral component", "EBI-55"),
    PREY("prey", "EBI-58"),
    PUTATIVE_SELF("putative self", "EBI-1812614"),
    SELF("self", "EBI-1383082"),
    UNSPECIFIED_ROLE("unspecified role", "EBI-1383079"),
    ;


    public final String name;
    public final String ebiId;

    ExperimentalRole(String name, String ebiId){
        this.name = name;
        this.ebiId = ebiId;
    }
}
