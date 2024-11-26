package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import lombok.Setter;
import psidev.psi.mi.jami.commons.MIWriterOptionFactory;
import psidev.psi.mi.jami.commons.PsiJami;
import psidev.psi.mi.jami.datasource.InteractionWriter;
import psidev.psi.mi.jami.factory.InteractionWriterFactory;
import psidev.psi.mi.jami.model.Annotation;
import psidev.psi.mi.jami.model.ComplexType;
import psidev.psi.mi.jami.model.InteractionCategory;
import psidev.psi.mi.jami.model.Publication;
import psidev.psi.mi.jami.xml.PsiXmlVersion;
import psidev.psi.mi.jami.xml.model.extension.factory.options.PsiXmlWriterOptions;
import psidev.psi.mi.jami.xml.model.extension.xml300.*;
import uk.ac.ebi.pride.utilities.ols.web.service.client.OLSClient;
import uk.ac.ebi.pride.utilities.ols.web.service.config.OLSWsConfig;

import javax.swing.*;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.util.*;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml.InteractionsCreator.olsClient;

public class PsiMiXmlMaker {
    InteractionsCreator interactionsCreator;
    ArrayList<XmlInteractionEvidence> xmlModelledInteractions;
    private static OLSClient olsClient = new OLSClient(new OLSWsConfig());
    @Setter
    String publicationId;

    public PsiMiXmlMaker(InteractionsCreator interactionsCreator) {
        this.interactionsCreator = interactionsCreator;
        xmlModelledInteractions = interactionsCreator.xmlModelledInteractions;
    }

    public void interactionsWriter() {
        String xmlFileName = "test.xml";

        Publication intactPubmedRef = new BibRef(publicationId);
        XmlSource source = new XmlSource("IntAct", "European Bioinformatics Institute", "http://www.ebi.ac.uk",
                "European Bioinformatics Institute (EMBL-EBI), Wellcome Genome Campus, Hinxton, Cambridge, CB10 1SD, United Kingdom.", intactPubmedRef);
        source.setUrl("http://www.ebi.ac.uk");
        source.setFullName("European Bioinformatics Institute");

        String dbMiId = olsClient.getExactTermByName("pubmed", "mi").getOboId().
                getIdentifier();
        XmlCvTerm dataBase = new XmlCvTerm("pubmed", dbMiId);
        XmlXref primaryXref = new XmlXref(dataBase, publicationId);

        CvTermXrefContainer xrefContainer = new CvTermXrefContainer();
        xrefContainer.setJAXBPrimaryRef(primaryXref);
        source.setJAXBXref(xrefContainer);

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.set(2024, Calendar.DECEMBER, 1);
        XMLGregorianCalendar defaultReleaseDate;
        try {
            defaultReleaseDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
        Collection<Annotation> defaultEntryAnnotations = new ArrayList<>();
        XmlAnnotation defaultEntryAnnotation = new XmlAnnotation();
        defaultEntryAnnotations.add(defaultEntryAnnotation);

        PsiJami.initialiseAllFactories();
        MIWriterOptionFactory optionwriterFactory = MIWriterOptionFactory.getInstance();

        Map<String, Object> expandedXmlWritingOptions = optionwriterFactory.getExpandedXmlOptions(new File(xmlFileName),
                InteractionCategory.evidence, ComplexType.n_ary, source, defaultReleaseDate, defaultEntryAnnotations,
                PsiXmlVersion.v3_0_0);


        InteractionWriterFactory writerFactory = InteractionWriterFactory.getInstance();
        InteractionWriter xmlInteractionWriter = null;

        try {
            xmlInteractionWriter = writerFactory.getInteractionWriterWith(expandedXmlWritingOptions);
            xmlInteractionWriter.start();
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