package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.util.*;

/**
 * PsiMiXmlMaker is a Spring component responsible for creating PSI-MI XML files
 * based on interaction data. It utilizes the JAMI library for creating XML files
 * and provides functionality to write interaction data to XML files.
 * Dependencies:
 * - InteractionsCreator for creating and handling interaction objects.
 * - ExcelFileReader for reading the publication ID from an external file.
 * - XmlMakerUtils for utility functions.
 * Usage:
 * This class is designed to be used as a Spring-managed bean.
 * Use the `interactionsWriter` method to generate and save a PSI-MI XML file.
 */
public class PsiMiXmlMaker {

    private static final Logger logger = LogManager.getLogger(PsiMiXmlMaker.class);

    final InteractionsCreator interactionsCreator;
    final List<XmlInteractionEvidence> xmlModelledInteractions;
    private String publicationId;
    final XmlMakerUtils utils = new XmlMakerUtils();
    private final ExcelFileReader excelFileReader;

    /**
     * Constructs a PsiMiXmlMaker instance with the given dependencies.
     *
     * @param interactionsCreator an instance of InteractionsCreator
     * @param excelFileReader     an instance of ExcelFileReader
     */
    public PsiMiXmlMaker(InteractionsCreator interactionsCreator, ExcelFileReader excelFileReader) {
        this.interactionsCreator = interactionsCreator;
        xmlModelledInteractions = interactionsCreator.xmlModelledInteractions;
        this.excelFileReader = excelFileReader;
        publicationId = this.excelFileReader.getPublicationId();
    }

    /**
     * Writes interaction data to a PSI-MI XML file at the specified location.
     *
     * @param saveLocation the directory path where the XML file will be saved
     */
    public void interactionsWriter(String saveLocation) {
        publicationId = excelFileReader.getPublicationId();
        logger.info("Starting interaction writer with publication ID: {}", publicationId);

        if (publicationId == null) {
            logger.error("Publication ID is null. Please enter the publication ID.");
            return;
        }

        Publication intactPubmedRef = new BibRef(publicationId);
        String intactMiId = utils.fetchMiId("intact");

        XmlSource source = new XmlSource("IntAct", "European Bioinformatics Institute", "https://www.ebi.ac.uk",
                "European Bioinformatics Institute (EMBL-EBI), Wellcome Genome Campus, Hinxton, Cambridge, CB10 1SD, United Kingdom.", intactPubmedRef);
        source.setUrl("https://www.ebi.ac.uk");
        source.setFullName("European Bioinformatics Institute");

        XmlXref primaryXref = createXref("pubmed", "primary-reference", "pubmed", publicationId);
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
            logger.error("Error while creating default release date", e);
            throw new RuntimeException(e);
        }

        Collection<Annotation> defaultEntryAnnotations = new ArrayList<>();
        XmlAnnotation defaultEntryAnnotation = new XmlAnnotation();
        defaultEntryAnnotations.add(defaultEntryAnnotation);

        PsiJami.initialiseAllFactories();

        Map<String, Object> expandedXmlWritingOptions = MIWriterOptionFactory.getInstance().getExpandedXmlOptions(
                new File(saveLocation + ".xml"),
                InteractionCategory.evidence,
                ComplexType.n_ary,
                new InMemoryLightIdentityObjectCache(),
                Collections.newSetFromMap(new IdentityHashMap<>()),
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
            logger.info("PSI-XML writing completed successfully. File saved at: {}", saveLocation);

        } catch (Exception e) {
            logger.error("Error during PSI-XML writing", e);

        } finally {
            if (xmlInteractionWriter != null) {
                try {
                    xmlInteractionWriter.close();
                } catch (Exception e) {
                    logger.error("Error while closing the PSI-XML writer", e);
                }
            }
        }
    }

    /**
     * Creates an XmlXref object for the given parameters.
     *
     * @param name     the name of the database
     * @param refType  the reference type
     * @param database the database name
     * @param id       the identifier
     * @return an instance of XmlXref
     */
    public XmlXref createXref(String name, String refType, String database, String id) {
        String dbMiId = utils.fetchMiId(name);
        String refTypeMiId = utils.fetchMiId(refType);
        CvTerm refTypeCv = new XmlCvTerm(refType, refTypeMiId);
        CvTerm databaseCv = new XmlCvTerm(database, dbMiId);

        return new XmlXref(databaseCv, id, refTypeCv);
    }
}
