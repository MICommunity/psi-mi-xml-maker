package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import psidev.psi.mi.jami.commons.MIWriterOptionFactory;
import psidev.psi.mi.jami.commons.PsiJami;
import psidev.psi.mi.jami.datasource.InteractionWriter;
import psidev.psi.mi.jami.factory.InteractionWriterFactory;
import psidev.psi.mi.jami.model.InteractionEvidence;
import psidev.psi.mi.jami.xml.PsiXmlVersion;
import psidev.psi.mi.jami.xml.model.extension.factory.options.PsiXmlWriterOptions;
import psidev.psi.mi.jami.xml.model.extension.xml300.XmlInteractionEvidence;
import psidev.psi.mi.jami.xml.model.extension.xml300.XmlModelledInteraction;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class PsiMiXmlMaker {
    InteractionsCreator interactionsCreator;
    ArrayList<XmlInteractionEvidence> xmlModelledInteractions;

    public PsiMiXmlMaker(InteractionsCreator interactionsCreator) {
        this.interactionsCreator = interactionsCreator;
        xmlModelledInteractions = interactionsCreator.xmlModelledInteractions;
    }

    public void interactionsWriter() {
        PsiJami.initialiseAllFactories();
        MIWriterOptionFactory optionwriterFactory = MIWriterOptionFactory.getInstance();

        String xmlFileName = "test.xml";
        Map<String, Object> xmlWritingOptions = optionwriterFactory.getDefaultXmlOptions(new File(xmlFileName));

        xmlWritingOptions.put(PsiXmlWriterOptions.XML_VERSION_OPTION, PsiXmlVersion.v3_0_0);

        InteractionWriterFactory writerFactory = InteractionWriterFactory.getInstance();
        InteractionWriter xmlInteractionWriter = null;

        try {
            xmlInteractionWriter = writerFactory.getInteractionWriterWith(xmlWritingOptions);
            xmlInteractionWriter.start();

//            Iterator<XmlModelledInteraction> interactionsIterator = xmlModelledInteractions.iterator();
//            while (interactionsIterator.hasNext()) {
//                xmlInteractionWriter.write(interactionsIterator.next());
//                if (interactionsIterator.hasNext() instanceof XmlInteractionEvidence) {
//                    InteractionEvidence interactionEvidence = (InteractionEvidence) interactionsIterator.next();
//                }
//            }
            xmlInteractionWriter.write(xmlModelledInteractions);
            xmlInteractionWriter.end();
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null, "Writing completed successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE)
            );

        } catch (Exception e) {
            System.err.println("Error during PSI-XML writing: " + e.getMessage());
            e.printStackTrace();
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null, "An error occurred: " +
                            e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
            );

        } finally {
            if (xmlInteractionWriter != null) {
                try {
                    xmlInteractionWriter.close();
                } catch (Exception e) {
                    System.err.println("Error while closing the PSI-XML writer: " + e.getMessage());
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(null, "Error closing writer: " +
                                    e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
                    );
                }
            }
        }
    }
}