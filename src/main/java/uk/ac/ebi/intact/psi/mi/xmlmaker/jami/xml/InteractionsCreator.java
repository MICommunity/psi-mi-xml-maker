package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.xml.model.extension.xml300.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapperGui;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml.DataTypeAndColumn.*;

/**
 * The InteractionsCreator class is responsible for processing Excel files or directory-based input files
 * to create biological interaction evidence in the XML format using the JAMI library.
 * It processes participants, interactions, and related data, and generates XML objects for interaction modeling.
 */
public class InteractionsCreator {
    @Setter
    private static int MAX_INTERACTIONS_PER_FILE = 1_000;
    final ExcelFileReader excelFileReader;
    final InteractionWriter interactionWriter;
    final UniprotMapperGui uniprotMapperGui;
    final XmlMakerUtils utils = new XmlMakerUtils();
    public final List<XmlInteractionEvidence> xmlModelledInteractions = new ArrayList<>();
    public final List<Map<String, String>> dataList = new ArrayList<>();

    @Setter
    Map<String, Integer> columnAndIndex;
    public String sheetSelected;

    private boolean isFileFinished;

    @Setter
    @Getter
    public String publicationId;
    public int numberOfFeature = 0;

    private static final Logger LOGGER = Logger.getLogger(InteractionsCreator.class.getName());

    /**
     * Constructs an InteractionsCreator with the specified Excel reader, Uniprot mapper GUI, and column-to-index mapping.
     *
     * @param reader           the Excel file reader for fetching data from Excel files.
     * @param writer           the InteractionWriter
     * @param uniprotMapperGui the Uniprot mapper GUI for mapping protein data.
     * @param columnAndIndex   the mapping of column names to their corresponding indices in the dataset.
     */
    public InteractionsCreator(ExcelFileReader reader, InteractionWriter writer, UniprotMapperGui uniprotMapperGui, Map<String, Integer> columnAndIndex) {
        this.excelFileReader = reader;
        this.interactionWriter = writer;
        this.uniprotMapperGui = uniprotMapperGui;
        this.columnAndIndex = columnAndIndex;
        this.publicationId = excelFileReader.publicationId;
    }

    /**
     * Creates participants and interactions based on the provided file format.
     * Clears existing participants and interactions and repopulates them using the data read from files or the workbook.
     **/
    public void createParticipantsWithFileFormat() {
        xmlModelledInteractions.clear();
        dataList.clear();
        numberOfFeature = excelFileReader.getNumberOfFeatures();
        if (excelFileReader.workbook == null) {
            fetchDataFileWithSeparator(columnAndIndex);
        } else {
            fetchDataWithWorkbook(columnAndIndex);
        }
    }

    /**
     * Creates an {@link XmlParticipantEvidence} instance from the provided data.
     *
     * <p>Extracts and validates participant attributes, resolves MI IDs asynchronously,
     * and constructs the participant evidence object with the given details. Returns
     * {@code null} if required fields are missing or invalid.
     *
     * @param data a {@link Map} containing participant attributes, with keys defined
     *             in {@link DataTypeAndColumn}.
     * @return an {@link XmlParticipantEvidence} instance, or {@code null} if validation fails.
     *
     * <p>Required fields:
     * <ul>
     *   <li>{@code PARTICIPANT_NAME}: Non-null, non-empty participant name.</li>
     *   <li>{@code PARTICIPANT_ID} and {@code PARTICIPANT_ID_DB}: Valid unique ID and database.</li>
     *   <li>{@code PARTICIPANT_ORGANISM}: Resolved to a valid taxonomy ID.</li>
     * </ul>
     *
     * <p>Optional attributes include type, experimental role, preparations, cross-references,
     * and identification methods, which are resolved if provided. Features can also be added
     * via {@link #createFeature(int, Map)}.
     * @throws NumberFormatException if the organism's taxonomy ID is invalid.
     */
    public XmlParticipantEvidence createParticipant(Map<String, String> data) {
        DataTypeAndColumn[] required = {PARTICIPANT_NAME, PARTICIPANT_ID, PARTICIPANT_ID_DB, PARTICIPANT_TYPE, PARTICIPANT_ORGANISM};

        for (DataTypeAndColumn requiredColumn : required) {
            if (data.get(requiredColumn.name) == null || data.get(requiredColumn.name).isBlank()) {
                LOGGER.warning(requiredColumn.name + " is required but missing or empty.");
                return null;
            }
        }

        Map<DataTypeAndColumn, CompletableFuture<CvTerm>> futureTerms = Stream.of(PARTICIPANT_TYPE, PARTICIPANT_ID_DB,
                        EXPERIMENTAL_ROLE, EXPERIMENTAL_PREPARATION, PARTICIPANT_IDENTIFICATION_METHOD) //TODO: ADD XREF
                .map(type -> Map.entry(type, data.get(type.name)))
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> CompletableFuture.supplyAsync(() -> utils.fetchTerm(e.getValue()))
                ));

        Map<DataTypeAndColumn, CvTerm> terms = futureTerms
                .entrySet()
                .stream()
                .filter(e -> e.getValue().join() != null)
                .collect(Collectors
                        .toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().join()
                        ));

        CvTerm participantType = terms.get(PARTICIPANT_TYPE);
        CvTerm participantIdDb = terms.get(PARTICIPANT_ID_DB);
        CvTerm experimentalRole = terms.get(EXPERIMENTAL_ROLE);
        CvTerm experimentalPreparation = terms.get(EXPERIMENTAL_PREPARATION);
//        CvTerm xref= terms.get(PARTICIPANT_XREF);
        CvTerm participantIdentificationMethod = terms.get(PARTICIPANT_IDENTIFICATION_METHOD);

        String name = getNonNullValue(data);
        String participantId = data.get(PARTICIPANT_ID.name);
        String participantOrganism = data.get(PARTICIPANT_ORGANISM.name);
        Xref uniqueId = new XmlXref(participantIdDb, participantId);

        if (participantOrganism == null || participantOrganism.isEmpty()) {
            LOGGER.warning("Missing or invalid participant organism for participant: " + name);
            return null;
        }
        String taxId = utils.fetchTaxIdForOrganism(participantOrganism);
        Organism organism = new XmlOrganism(Integer.parseInt(Objects.requireNonNull(taxId)));

        Interactor participant = new XmlPolymer(name, participantType, organism, uniqueId);
        XmlParticipantEvidence participantEvidence = new XmlParticipantEvidence(participant);

        if (experimentalRole != null) {
            participantEvidence.setExperimentalRole(experimentalRole);
        }

        if (numberOfFeature > 0) {
            for (int i = 0; i < numberOfFeature; i++) {
                XmlFeatureEvidence feature = createFeature(i, data);
                participantEvidence.addFeature(feature);
            }
        }

        if (experimentalPreparation != null) {
            participantEvidence.getExperimentalPreparations().add(experimentalPreparation);
        }

//        if (xref != null) {
//            Xref xmlXref = new XmlXref(xref, xref.getShortName()); // TODO check normality of using name as identifier for xref
//            participantEvidence.getXrefs().add(xmlXref);
//        }

        if (participantIdentificationMethod != null) {
            participantEvidence.getIdentificationMethods().add(participantIdentificationMethod);
        }

        return participantEvidence;
    }

    // TODO Either rename or thow a runtime error if null

    /**
     * Retrieves the value from the provided data map for the given column name, ensuring the value is neither null nor empty.
     * If the value is null or empty (after trimming whitespace), the method returns null.
     *
     * @param data A map containing column names as keys and their corresponding values.
     * @return The value from the map if it is non-null and non-empty, otherwise returns null.
     */
    private String getNonNullValue(Map<String, String> data) {
        String value = data.get(DataTypeAndColumn.PARTICIPANT_NAME.name);
        return (value == null || value.trim().isEmpty()) ? null : value;
    }

    /**
     * Fetches data from a file using a separator (e.g., CSV or TSV) and stores it in a list of maps for processing.
     *
     * @param columnAndIndex the mapping of column names to their corresponding indices in the dataset.
     */
    public void fetchDataFileWithSeparator(Map<String, Integer> columnAndIndex) {

        Iterator<List<String>> data = excelFileReader.readFileWithSeparator();
        int expectedNumberOfColumns = excelFileReader.fileData.size();
        int interactionNumberColumn = columnAndIndex.get(INTERACTION_NUMBER.name);
        String currentInteractionNumber = "0"; // Will be set based on the first row

        // Check if there's data available first
        if (data.hasNext()) {
            List<String> firstRow = data.next();
            // Initialize currentInteractionNumber based on the first row
            currentInteractionNumber = firstRow.get(interactionNumberColumn);
            // Put the first row back into the iterator to process in the loop
            Iterator<List<String>> finalData = data;
            data = new Iterator<List<String>>() {
                boolean firstRowProcessed = false;

                @Override
                public boolean hasNext() {
                    return !firstRowProcessed || finalData.hasNext();
                }

                @Override
                public List<String> next() {
                    if (!firstRowProcessed) {
                        firstRowProcessed = true;
                        return firstRow; // Return the first row initially
                    } else {
                        return finalData.next(); // Continue with the iterator
                    }
                }
            };
        }

        while (data.hasNext()) {
            isFileFinished = false;
            List<String> datum = data.next();

            if (datum.size() < expectedNumberOfColumns) {
                LOGGER.warning("Row has fewer cells than expected. Skipping row: " + datum);
                continue;
            }

            if (!currentInteractionNumber.equals(datum.get(interactionNumberColumn))) {
                createInteractions();
                dataList.clear();
                currentInteractionNumber = datum.get(interactionNumberColumn);
            }

            Map<String, String> dataMap = new HashMap<>();
            for (DataTypeAndColumn column : DataTypeAndColumn.values()) {
                if (column.initial) {
                    dataMap.put(column.name, datum.get(columnAndIndex.get(column.name)));
                }
                for (int i = 0; i < numberOfFeature; i++) {
                    if (!column.initial) {
                        String key = column.name + "_" + i;
                        Integer index = columnAndIndex.get(key);
                        if (index != null && index < datum.size()) {
                            dataMap.put(key, datum.get(index));
                        } else {
                            LOGGER.warning("Index out of bounds for key: " + key);
                        }
                    }
                }
            }
            dataList.add(dataMap);
        }

        isFileFinished = true;
        createInteractions();
        dataList.clear();
    }

    public void fetchDataWithWorkbook(Map<String, Integer> columnAndIndex) {
        Iterator<Row> data = excelFileReader.readWorkbookSheet(sheetSelected);
        int expectedNumberOfColumns = excelFileReader.fileData.size();
        int interactionNumberColumn = columnAndIndex.get(INTERACTION_NUMBER.name);
        String currentInteractionNumber = "0"; // Will be set based on the first row

        // Check if there's data available first
        if (data.hasNext()) {
            Row firstRow = data.next();
            List<String> firstRowData = new ArrayList<>();
            int firstCellNum = firstRow.getFirstCellNum();
            int lastCellNum = firstRow.getLastCellNum();

            for (int cellNum = firstCellNum; cellNum < lastCellNum; cellNum++) {
                Cell cell = firstRow.getCell(cellNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (cell == null) {
                    firstRowData.add(""); // Add an empty string for blank or missing cells
                } else {
                    firstRowData.add(cell.toString());
                }
            }

            // Initialize currentInteractionNumber based on the first row
            currentInteractionNumber = firstRowData.get(interactionNumberColumn);

            // Put the first row back into the iterator to process in the loop
            Iterator<Row> finalData = data;
            data = new Iterator<Row>() {
                boolean firstRowProcessed = false;

                @Override
                public boolean hasNext() {
                    return !firstRowProcessed || finalData.hasNext();
                }

                @Override
                public Row next() {
                    if (!firstRowProcessed) {
                        firstRowProcessed = true;
                        return firstRow; // Return the first row initially
                    } else {
                        return finalData.next(); // Continue with the iterator
                    }
                }
            };
        }

        while (data.hasNext()) {
            isFileFinished = false;
            Row row = data.next();
            List<String> datum = new ArrayList<>();
            int firstCellNum = row.getFirstCellNum();
            int lastCellNum = row.getLastCellNum();

            for (int cellNum = firstCellNum; cellNum < lastCellNum; cellNum++) {
                Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (cell == null) {
                    datum.add(""); // Add an empty string for blank or missing cells
                } else {
                    datum.add(cell.toString());
                }
            }

            if (datum.size() < expectedNumberOfColumns) {
                LOGGER.warning("Row has fewer cells than expected. Skipping row: " + datum);
                continue; // Skip this row if it has fewer cells than expected
            }

            if (!currentInteractionNumber.equals(datum.get(interactionNumberColumn))) {
                createInteractions();
                dataList.clear();
                currentInteractionNumber = datum.get(interactionNumberColumn);
            }

            Map<String, String> dataMap = new HashMap<>();
            for (DataTypeAndColumn column : DataTypeAndColumn.values()) {
                if (column.initial) {
                    dataMap.put(column.name, datum.get(columnAndIndex.get(column.name)));
                }
                for (int i = 0; i < numberOfFeature; i++) {
                    if (!column.initial) {
                        String key = column.name + "_" + i;
                        Integer index = columnAndIndex.get(key);
                        if (index != null && index < datum.size()) {
                            dataMap.put(key, datum.get(index));
                        } else {
                            LOGGER.warning("Index out of bounds for key: " + key);
                        }
                    }
                }
            }
            dataList.add(dataMap);
        }

        isFileFinished = true;
        createInteractions();
        dataList.clear();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "N/A";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "N/A";
        }
    }

    /**
     * Creates and sets the details of an interaction based on the provided parameters, including interaction type,
     * detection method, participant identification method, and host organism. This method also links the interaction
     * to an experiment and a publication if relevant details are provided.
     *
     * @param interaction                     The interaction object to be populated with details.
     * @param interactionDetectionMethod      The method used to detect the interaction, used to create a CvTerm for detection method.
     * @param participantIdentificationMethod The method used to identify the participant, used to create a CvTerm for identification method.
     * @param hostOrganism                    The host organism associated with the interaction, used to fetch the organism's tax ID.
     * @param interactionType                 The type of the interaction, used to create a CvTerm for interaction type.
     */
    private void processInteractionCreation(XmlInteractionEvidence interaction, String interactionDetectionMethod,
                                            String participantIdentificationMethod,
                                            String hostOrganism, String interactionType) {
        if (interactionType != null) {
            String interactionTypeMiId = utils.fetchMiId(interactionType);
            CvTerm interactionTypeCv = new XmlCvTerm(interactionType, interactionTypeMiId);
            interaction.setInteractionType(interactionTypeCv);
        }

        if (interactionDetectionMethod != null) {
            Organism organism = createOrganism(hostOrganism);

            CvTerm detectionMethod = utils.fetchTerm(interactionDetectionMethod);
            Publication publication = new BibRef(excelFileReader.getPublicationId());
            XmlExperiment experiment;

            if (organism == null || hostOrganism.isBlank()) {
                experiment = new XmlExperiment(publication, detectionMethod);
            } else {
                experiment = new XmlExperiment(publication, detectionMethod, organism);
            }
            if (participantIdentificationMethod != null) {
                experiment.setParticipantIdentificationMethod(utils.fetchTerm(participantIdentificationMethod));
            }

            interaction.setExperiment(experiment);
        }

        xmlModelledInteractions.add(interaction);
        launchWriting();
    }

    private Organism createOrganism(String hostOrganism) {
        if (hostOrganism == null) {
            LOGGER.warning("Host organism is null. Skipping organism creation.");
            return null;
        }

        String hostOrganismTaxId = utils.fetchTaxIdForOrganism(hostOrganism);

        if (hostOrganismTaxId == null) {
            LOGGER.warning("No Tax ID found for host organism: " + hostOrganism);
            return null;
        }

        Organism organism = null;

        try {
            int hostOrganismInt = Integer.parseInt(hostOrganismTaxId);
            organism = new XmlOrganism(hostOrganismInt);
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid host organism tax ID: " + hostOrganism);
        }

        return organism;
    }

    /**
     * Creates interactions by processing grouped participant data and linking them to experiments and publications.
     */
    public void createInteractions() {
        if (dataList.isEmpty() && isFileFinished) {
            launchWriting();
            return;
        }

        XmlInteractionEvidence interaction = new XmlInteractionEvidence();

        String interactionDetectionMethod = null;
        String participantIdentificationMethod = null;
        String hostOrganism = null;
        String interactionType = null;

        for (Map<String, String> participant : dataList) {
            XmlParticipantEvidence newParticipant = createParticipant(participant);
            interaction.addParticipant(newParticipant);

            interactionDetectionMethod = participant.get(DataTypeAndColumn.INTERACTION_DETECTION_METHOD.name);
            participantIdentificationMethod = participant.get(DataTypeAndColumn.PARTICIPANT_IDENTIFICATION_METHOD.name);
            hostOrganism = participant.get(DataTypeAndColumn.HOST_ORGANISM.name);
            interactionType = participant.get(DataTypeAndColumn.INTERACTION_TYPE.name);
        }
        processInteractionCreation(interaction, interactionDetectionMethod, participantIdentificationMethod, hostOrganism, interactionType);
    }

    /**
     * Creates a feature evidence object for a participant based on feature data.
     *
     * @param featureIndex the index of the feature.
     * @param data         the map containing feature data.
     * @return the created XmlFeatureEvidence object.
     */
    private XmlFeatureEvidence createFeature(int featureIndex, Map<String, String> data) {
        String featureIndexString = "_" + featureIndex;

        String featureShortLabel = data.get(DataTypeAndColumn.FEATURE_TYPE.name + featureIndexString);
        String featureType = data.get(DataTypeAndColumn.FEATURE_TYPE.name + featureIndexString);
        String featureTypeMiId = utils.fetchMiId(featureType);
        CvTerm featureTypeCv = new XmlCvTerm(featureType, featureTypeMiId);
        XmlFeatureEvidence featureEvidence = new XmlFeatureEvidence(featureTypeCv);
        featureEvidence.setShortName(featureShortLabel);

        XmlRange featureRange = new XmlRange();
        String featureStartRange = data.get(DataTypeAndColumn.FEATURE_START_STATUS.name + featureIndexString);
        String featureStartRangeMiId = utils.fetchMiId(featureStartRange);
        XmlCvTerm featureStartRangeCv = new XmlCvTerm(featureStartRange, featureStartRangeMiId);
        featureRange.setJAXBStartStatus(featureStartRangeCv);

        String featureEndRange = data.get(DataTypeAndColumn.FEATURE_END_STATUS.name + featureIndexString);
        String featureEndRangeMiId = utils.fetchMiId(featureEndRange);
        XmlCvTerm featureEndRangeCv = new XmlCvTerm(featureEndRange, featureEndRangeMiId);
        featureRange.setJAXBEndStatus(featureEndRangeCv);

        featureEvidence.setJAXBRangeWrapper(new AbstractXmlFeature.JAXBRangeWrapper());
        featureEvidence.getRanges().add(featureRange);

        return featureEvidence;
    }

    private void launchWriting() {
        if (xmlModelledInteractions.size() >= MAX_INTERACTIONS_PER_FILE || isFileFinished) {
            LOGGER.info("Processing " + xmlModelledInteractions.size() + " interactions.");
            interactionWriter.writeInteractions(xmlModelledInteractions);
            xmlModelledInteractions.clear();
        }
    }
}