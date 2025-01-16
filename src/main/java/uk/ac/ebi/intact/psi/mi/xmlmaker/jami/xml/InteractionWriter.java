package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import psidev.psi.mi.jami.commons.MIWriterOptionFactory;
import psidev.psi.mi.jami.commons.PsiJami;
import psidev.psi.mi.jami.factory.InteractionWriterFactory;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.xml.PsiXmlVersion;
import psidev.psi.mi.jami.xml.cache.InMemoryLightIdentityObjectCache;
import psidev.psi.mi.jami.xml.model.extension.xml300.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.FileUtils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * InteractionWriter is a Spring component responsible for creating PSI-MI XML files
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
public class InteractionWriter {

    private static final Logger LOGGER = LogManager.getLogger(InteractionWriter.class);

    public String publicationId;
    final XmlMakerUtils utils = new XmlMakerUtils();
    private final ExcelFileReader excelFileReader;

    private int fileCounter = 0;
    @Getter @Setter
    private String saveLocation;
    @Getter @Setter
    private String name;

    /**
     * Constructs a InteractionWriter instance with the given dependencies.
     *
     * @param excelFileReader an instance of ExcelFileReader
     */
    public InteractionWriter(ExcelFileReader excelFileReader) {
        this.excelFileReader = excelFileReader;
        publicationId = this.excelFileReader.getPublicationId();
    }

    /**
     * Writes interaction data to a PSI-MI XML file at the specified location.
     */
    public void writeInteractions(List<XmlInteractionEvidence> interactions) {
        if (!validateInputs(interactions)) return;

        String filePath = constructFilePath();
        XmlSource source = initializeSource();
        XMLGregorianCalendar defaultReleaseDate = createDefaultReleaseDate();
        Collection<Annotation> defaultEntryAnnotations = initializeDefaultAnnotations();
        Map<String, Object> writingOptions = prepareWritingOptions(filePath, source, defaultReleaseDate, defaultEntryAnnotations);

        writeInteractionsToFile(interactions, writingOptions);
    }

    /**
     * Validates the input interactions and ensures that the publication ID is set.
     *
     * @param interactions the list of XmlInteractionEvidence to be validated.
     * @return {@code true} if inputs are valid, {@code false} otherwise.
     *         If invalid, error dialogs are shown and relevant errors are logged.
     */
    private boolean validateInputs(List<XmlInteractionEvidence> interactions) {
        if (interactions == null || interactions.isEmpty()) {
            utils.showErrorDialog("No interactions provided for writing.");
            LOGGER.error("No interactions provided for writing.");
            return false;
        }

        publicationId = excelFileReader.getPublicationId();
        if (publicationId == null) {
            utils.showErrorDialog("Publication ID is null. Please enter the publication ID.");
            LOGGER.error("Impossible to find publication ID.");
            return false;
        }
        LOGGER.info("Starting interaction writer with publication ID: {}", publicationId);
        return true;
    }

    /**
     * Constructs the file path for saving the XML file, based on the save location and file naming pattern.
     *
     * @return the constructed file path as a string.
     */
    private String constructFilePath() {
        String directory = this.saveLocation + "/" + FileUtils.getFileName(name) + "/";
        return directory + FileUtils.getFileName(name)  + "_" + fileCounter++ + ".xml";
    }

    /**
     * Initializes the XmlSource object with relevant details including publication reference and xrefs.
     *
     * @return a fully initialized {@link XmlSource} object.
     */
    private XmlSource initializeSource() {
        Publication intactPubmedRef = new BibRef(publicationId);
        String intactMiId = utils.fetchMiId("intact");

        XmlSource source = new XmlSource("IntAct", "European Bioinformatics Institute", "https://www.ebi.ac.uk",
                "European Bioinformatics Institute (EMBL-EBI), Wellcome Genome Campus, Hinxton, Cambridge, CB10 1SD, United Kingdom.", intactPubmedRef);

        XmlXref primaryXref = createXref("pubmed", "primary-reference", "pubmed", publicationId);
        XmlXref secondaryXref = createXref("psi-mi", "identity", "psi-mi", intactMiId);
        secondaryXref.setSecondary("secondary-reference");

        CvTermXrefContainer xrefContainer = new CvTermXrefContainer();
        xrefContainer.setJAXBPrimaryRef(primaryXref);
        xrefContainer.getJAXBSecondaryRefs().add(secondaryXref);
        source.setJAXBXref(xrefContainer);

        return source;
    }

    /**
     * Creates a default release date in the form of an XMLGregorianCalendar with the current date and time.
     *
     * @return the default release date as an {@link XMLGregorianCalendar}.
     * @throws RuntimeException if an error occurs while creating the release date.
     */
    private XMLGregorianCalendar createDefaultReleaseDate() {
        try {
            GregorianCalendar calendar = new GregorianCalendar();
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
        } catch (DatatypeConfigurationException e) {
            LOGGER.error("Error while creating default release date", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Initializes and returns a collection of default annotations for entries.
     *
     * @return a collection of {@link Annotation} objects, containing a default entry annotation.
     */
    private Collection<Annotation> initializeDefaultAnnotations() {
        Collection<Annotation> defaultEntryAnnotations = new ArrayList<>();
        XmlAnnotation defaultEntryAnnotation = new XmlAnnotation();
        defaultEntryAnnotations.add(defaultEntryAnnotation);
        return defaultEntryAnnotations;
    }

    /**
     * Prepares the writing options for the interaction file.
     *
     * @param filePath the path of the file to be written.
     * @param source the source to be used in the file.
     * @param defaultReleaseDate the default release date.
     * @param defaultEntryAnnotations the default annotations for the entries.
     * @return a map containing the writing options for the interaction file.
     */
    private Map<String, Object> prepareWritingOptions(String filePath, XmlSource source, XMLGregorianCalendar defaultReleaseDate, Collection<Annotation> defaultEntryAnnotations) {
        PsiJami.initialiseAllFactories();

        return MIWriterOptionFactory.getInstance().getExpandedXmlOptions(
                new File(filePath),
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
    }

    /**
     * Writes a list of interactions to a file using the provided writing options.
     *
     * @param interactions the list of {@link XmlInteractionEvidence} to be written.
     * @param writingOptions the map of options for writing the interactions.
     *
     * <p>Logs success and error messages during the writing process.
     * Ensures the directory exists before writing.</p>
     */
    private void writeInteractionsToFile(List<XmlInteractionEvidence> interactions, Map<String, Object> writingOptions) {
        InteractionWriterFactory writerFactory = InteractionWriterFactory.getInstance();
        psidev.psi.mi.jami.datasource.InteractionWriter<XmlInteractionEvidence> xmlInteractionWriter = null;

        try {
            createDirectoryIfNotExists();

            xmlInteractionWriter = writerFactory.getInteractionWriterWith(writingOptions);
            xmlInteractionWriter.start();
            xmlInteractionWriter.write(interactions);
            xmlInteractionWriter.end();

            LOGGER.info("PSI-XML writing completed successfully. File saved at: {}", saveLocation);

        } catch (Exception e) {
            utils.showErrorDialog("Error during PSI-XML writing: " + e.getMessage());
            LOGGER.error("Error during PSI-XML writing", e);
        } finally {
            closeWriter(xmlInteractionWriter);
            utils.showInfoDialog("PSI-XML writing completed successfully. File saved at: " + saveLocation);
        }
    }

    /**
     * Creates the directory for the output file if it does not already exist.
     *
     * @throws IOException if an error occurs while creating the directory.
     */
    private void createDirectoryIfNotExists() throws IOException {
        Path directory = Path.of(this.saveLocation + "/" + FileUtils.getFileName(name) + "/");
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }

    /**
     * Closes the provided PSI-XML writer, handling any potential exceptions.
     *
     * @param writer the {@link psidev.psi.mi.jami.datasource.InteractionWriter} to be closed.
     *               If the writer is {@code null}, no action is taken.
     *
     * <p>Logs any errors encountered while closing the writer.</p>
     */
    private void closeWriter(psidev.psi.mi.jami.datasource.InteractionWriter<XmlInteractionEvidence> writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (Exception e) {
                LOGGER.error("Error while closing the PSI-XML writer", e);
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
