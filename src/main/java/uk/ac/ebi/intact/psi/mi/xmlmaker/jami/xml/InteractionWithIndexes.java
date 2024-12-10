package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

public class InteractionWithIndexes {
    final String interactionNumber;
    final int startIndex;
    final int endIndex;

    InteractionWithIndexes(String interactionNumber, int startIndex, int endIndex) {
        this.interactionNumber = interactionNumber;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }
}
