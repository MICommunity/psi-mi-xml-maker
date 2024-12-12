package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import psidev.psi.mi.jami.commons.MIWriterOptionFactory;
import psidev.psi.mi.jami.commons.PsiJami;
import psidev.psi.mi.jami.datasource.InteractionWriter;
import psidev.psi.mi.jami.factory.InteractionWriterFactory;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.xml.PsiXmlVersion;
import psidev.psi.mi.jami.xml.cache.InMemoryLightIdentityObjectCache;
import psidev.psi.mi.jami.xml.model.extension.xml300.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.XmlMakerUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;

import javax.swing.*;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.util.*;

@Component
public class PsiMiXmlMaker {
    final InteractionsCreator interactionsCreator;
    final List<XmlInteractionEvidence> xmlModelledInteractions;
    private String publicationId;
    final XmlMakerUtils utils = new XmlMakerUtils();
    private ExcelFileReader excelFileReader;

    @Autowired
    public void setExcelFileReader(ExcelFileReader excelFileReader) {
        this.excelFileReader = excelFileReader;
    }

    public PsiMiXmlMaker(InteractionsCreator interactionsCreator, ExcelFileReader excelFileReader) {
        this.interactionsCreator = interactionsCreator;
        xmlModelledInteractions = interactionsCreator.xmlModelledInteractions;
        this.excelFileReader = excelFileReader;
        publicationId = this.excelFileReader.getPublicationId();
    }

    public void interactionsWriter(String saveLocation) {
        publicationId = excelFileReader.getPublicationId();

        Publication intactPubmedRef = new BibRef(publicationId);
        String intactMiId = utils.fetchMiId("intact");

        XmlSource source = new XmlSource("IntAct", "European Bioinformatics Institute", "https://www.ebi.ac.uk",
                "European Bioinformatics Institute (EMBL-EBI), Wellcome Genome Campus, Hinxton, Cambridge, CB10 1SD, United Kingdom.", intactPubmedRef);
        source.setUrl("https://www.ebi.ac.uk");
        source.setFullName("European Bioinformatics Institute");

        if (publicationId == null) {
            utils.showErrorDialog("Please enter the publication ID!");
        }
        XmlXref primaryXref = createXref("pubmed", "primary-reference", "pubmed", publicationId);
        //TODO: check why it is not considered primary
        XmlXref secondaryXref = createXref("psi-mi", "identity", "psi-mi", intactMiId);


        CvTermXrefContainer xrefContainer = new CvTermXrefContainer();
        xrefContainer.setJAXBPrimaryRef(primaryXref);
        secondaryXref.setSecondary("secondary-reference");
        xrefContainer.getJAXBSecondaryRefs().add(secondaryXref);
        source.setJAXBXref(xrefContainer);

        GregorianCalendar calendar = new GregorianCalendar();
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

        Map<String, Object> expandedXmlWritingOptions = MIWriterOptionFactory.getInstance().getExpandedXmlOptions(
                new File(saveLocation),
                InteractionCategory.evidence,
                ComplexType.n_ary,
                new InMemoryLightIdentityObjectCache(),
                Collections.newSetFromMap(new IdentityHashMap()),
                source,
                defaultReleaseDate,
                defaultEntryAnnotations,
                false,
                true,
                PsiXmlVersion.v3_0_0
        );


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

    public XmlXref createXref(String name, String refType, String database, String id) {
        String dbMiId = utils.fetchMiId(name);
        String refTypeMiId = utils.fetchMiId(refType);
        CvTerm refTypeCv = new XmlCvTerm(refType, refTypeMiId);
        CvTerm databaseCv = new XmlCvTerm(database, dbMiId);

        return new XmlXref(databaseCv, id, refTypeCv);
    }

}