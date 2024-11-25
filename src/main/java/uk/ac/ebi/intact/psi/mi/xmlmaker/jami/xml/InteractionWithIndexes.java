package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

public class InteractionWithIndexes {
    String interactionNumber;
    int startIndex;
    int endIndex;

    InteractionWithIndexes(String interactionNumber, int startIndex, int endIndex) {
        this.interactionNumber = interactionNumber;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }
}
