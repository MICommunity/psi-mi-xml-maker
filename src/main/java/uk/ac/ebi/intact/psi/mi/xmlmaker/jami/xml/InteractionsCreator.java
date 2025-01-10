package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.*;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.xml.model.extension.xml300.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapperGui;
import java.util.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.XmlMakerUtils;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The InteractionsCreator class is responsible for processing Excel files or directory-based input files
 * to create biological interaction evidence in the XML format using the JAMI library.
 * It processes participants, interactions, and related data, and generates XML objects for interaction modeling.
 */
public class InteractionsCreator {
    final ExcelFileReader excelFileReader;
    final UniprotMapperGui uniprotMapperGui;
    final XmlMakerUtils utils = new XmlMakerUtils();
    public final List<XmlInteractionEvidence> xmlModelledInteractions = new ArrayList<>();
    public final List<Map<String, String>> dataList = new ArrayList<>();
    final Map<String, Integer> columnAndIndex;

    public String sheetSelected;

    @Setter @Getter
    public String publicationId;
    public int numberOfFeature = 0;

    private static final Logger LOGGER = Logger.getLogger(InteractionsCreator.class.getName());

    /**
     * Constructs an InteractionsCreator with the specified Excel reader, Uniprot mapper GUI, and column-to-index mapping.
     *
     * @param reader          the Excel file reader for fetching data from Excel files.
     * @param uniprotMapperGui the Uniprot mapper GUI for mapping protein data.
     * @param columnAndIndex   the mapping of column names to their corresponding indices in the dataset.
     */
    public InteractionsCreator(ExcelFileReader reader, UniprotMapperGui uniprotMapperGui, Map<String, Integer> columnAndIndex) {
        this.excelFileReader = reader;
        this.uniprotMapperGui = uniprotMapperGui;
        this.columnAndIndex = columnAndIndex;
        this.publicationId = excelFileReader.publicationId;
    }

    /**
     * Creates participants and interactions based on the provided file format.
     * Clears existing participants and interactions and repopulates them using the data read from files or the workbook.
     *
     * @param columnAndIndex the mapping of column names to their corresponding indexes in the dataset.
     */
    public void createParticipantsWithFileFormat(Map<String, Integer> columnAndIndex) {
        xmlModelledInteractions.clear();
        dataList.clear();
        numberOfFeature = excelFileReader.getNumberOfFeatures();
        if (excelFileReader.workbook == null) {
            fetchDataFileWithSeparator(columnAndIndex);
        } else {
            fetchDataWithWorkbook(columnAndIndex);
        }
        createInteractions();
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
     *
     * @throws NumberFormatException if the organism's taxonomy ID is invalid.
     */
    public XmlParticipantEvidence createParticipant(Map<String, String> data) {
        String name = getNonNullValue(data, DataTypeAndColumn.PARTICIPANT_NAME);

        if (name == null) {
            LOGGER.warning("Participant name is required but missing or empty.");
            return null;
        }

        String participantType = getNonNullValue(data, DataTypeAndColumn.PARTICIPANT_TYPE);
        String participantOrganism = getNonNullValue(data, DataTypeAndColumn.PARTICIPANT_ORGANISM);
        String participantId = getNonNullValue(data, DataTypeAndColumn.PARTICIPANT_ID);
        String participantIdDb = getNonNullValue(data, DataTypeAndColumn.PARTICIPANT_ID_DB);
        String experimentalRole = getNonNullValue(data, DataTypeAndColumn.EXPERIMENTAL_ROLE);
        String experimentalPreparation = getNonNullValue(data, DataTypeAndColumn.EXPERIMENTAL_PREPARATION);
        String xref = getNonNullValue(data, DataTypeAndColumn.PARTICIPANT_XREF);
        String participantIdentificationMethod = getNonNullValue(data, DataTypeAndColumn.PARTICIPANT_IDENTIFICATION_METHOD);

        CompletableFuture<String> participantTypeMiIdFuture = CompletableFuture.supplyAsync(() -> utils.fetchMiId(participantType));
        CompletableFuture<String> participantIdDbMiIdFuture = CompletableFuture.supplyAsync(() -> utils.fetchMiId(participantIdDb));
        CompletableFuture<String> experimentalRoleMiIdFuture = CompletableFuture.supplyAsync(() -> utils.fetchMiId(experimentalRole));
        CompletableFuture<String> experimentalPreparationMiIdFuture = CompletableFuture.supplyAsync(() -> utils.fetchMiId(experimentalPreparation));
        CompletableFuture<String> xrefMiIdFuture = CompletableFuture.supplyAsync(() -> utils.fetchMiId(xref));
        CompletableFuture<String> participantIdentificationMethodMiIdFuture = CompletableFuture.supplyAsync(() -> utils.fetchMiId(participantIdentificationMethod));

        CompletableFuture.allOf(participantTypeMiIdFuture, participantIdDbMiIdFuture, experimentalRoleMiIdFuture, experimentalPreparationMiIdFuture, xrefMiIdFuture, participantIdentificationMethodMiIdFuture).join();

        String participantTypeMiId = participantTypeMiIdFuture.join();
        String participantIdDbMiId = participantIdDbMiIdFuture.join();
        String experimentalRoleMiId = experimentalRoleMiIdFuture.join();
        String experimentalPreparationMiId = experimentalPreparationMiIdFuture.join();
        String xrefMiId = xrefMiIdFuture.join();
        String participantIdentificationMethodMiId = participantIdentificationMethodMiIdFuture.join();

        if (participantTypeMiId == null || participantTypeMiId.trim().isEmpty()) {
            LOGGER.warning("Missing or invalid MI ID for participant: " + name);
            return null;
        }
        assert participantType != null;
        CvTerm type = new XmlCvTerm(participantType, participantTypeMiId);

        if (participantIdDbMiId == null || participantId == null) {
            LOGGER.warning("Missing or invalid participant ID or ID DB for participant: " + name);
            return null;
        }
        assert participantIdDb != null;
        Xref uniqueId = new XmlXref(new XmlCvTerm(participantIdDb, participantIdDbMiId), participantId);

        if (participantOrganism == null || participantOrganism.isEmpty()) {
            LOGGER.warning("Missing or invalid participant organism for participant: " + name);
            return null;
        }
        String taxId = utils.fetchTaxIdForOrganism(Objects.requireNonNull(participantOrganism));
        Organism organism = new XmlOrganism(Integer.parseInt(taxId));

        Interactor participant = new XmlPolymer(name, type, organism, uniqueId);
        XmlParticipantEvidence participantEvidence = new XmlParticipantEvidence(participant);

        if (experimentalRole != null) {
            CvTerm experimentalRoleCv = new XmlCvTerm(experimentalRole, experimentalRoleMiId);
            participantEvidence.setExperimentalRole(experimentalRoleCv);
        }

        if (numberOfFeature > 0) {
            for (int i = 0; i < numberOfFeature; i++) {
                XmlFeatureEvidence feature = createFeature(i, data);
                participantEvidence.addFeature(feature);
            }
        }

        if (experimentalPreparation != null) {
            CvTerm experimentalPreparationCv = new XmlCvTerm(experimentalPreparation, experimentalPreparationMiId);
            participantEvidence.getExperimentalPreparations().add(experimentalPreparationCv);
        }

        if (xref != null) {
            CvTerm xrefCv = new XmlCvTerm(xref, xrefMiId);
            Xref xmlXref = new XmlXref(xrefCv, xref);
            participantEvidence.getXrefs().add(xmlXref);
        }

        if (participantIdentificationMethod != null) {
            CvTerm participantIdentificationMethodCv = new XmlCvTerm(participantIdentificationMethod, participantIdentificationMethodMiId);
            participantEvidence.getIdentificationMethods().add(participantIdentificationMethodCv);
        }

        return participantEvidence;
    }

    /**
     * Retrieves the value from the provided data map for the given column name, ensuring the value is neither null nor empty.
     * If the value is null or empty (after trimming whitespace), the method returns null.
     *
     * @param data A map containing column names as keys and their corresponding values.
     * @param column The column whose value is to be fetched from the map.
     * @return The value from the map if it is non-null and non-empty, otherwise returns null.
     */
    private String getNonNullValue(Map<String, String> data, DataTypeAndColumn column) {
        String value = data.get(column.name);
        return (value == null || value.trim().isEmpty()) ? null : value;
    }

    /**
     * Fetches data from a file using a separator (e.g., CSV or TSV) and stores it in a list of maps for processing.
     *
     * @param columnAndIndex the mapping of column names to their corresponding indices in the dataset.
     */
    public void fetchDataFileWithSeparator(Map<String, Integer> columnAndIndex) {
        List<List<String>> data = excelFileReader.readFileWithSeparator();

        int expectedNumberOfColumns = data.get(0).size(); // Ensure uniform row size
        int totalRows = data.size();
        int currentRow = 0;

        for (int rowIndex = currentRow; rowIndex < totalRows; rowIndex++) {
            List<String> datum = data.get(rowIndex);
            if (datum.size() < expectedNumberOfColumns) {
                LOGGER.warning("Row has fewer cells than expected. Skipping row: " + datum);
                continue;
            }

            Map<String, String> dataMap = new HashMap<>();
            for (DataTypeAndColumn column : DataTypeAndColumn.values()) {
                if (column.initial) {
                    dataMap.put(column.name, datum.get(columnAndIndex.get(column.name)));
                }
                if (numberOfFeature > 0) {
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
            }
            dataList.add(dataMap);
        }
    }

    /**
     * Fetches data from a workbook and stores it in a list of maps for further processing.
     *
     * @param columnAndIndex the mapping of column names to their corresponding indices in the dataset.
     */
    public void fetchDataWithWorkbook(Map<String, Integer> columnAndIndex) {
        Workbook workbook = excelFileReader.workbook;

        if (workbook == null) {
            throw new IllegalArgumentException("Workbook is null. Cannot fetch data.");
        }

        Sheet sheet = workbook.getSheet(sheetSelected);
        if (sheet == null) {
            sheet = workbook.getSheetAt(0);
            LOGGER.warning("Invalid sheetSelected: " + sheetSelected + ". Defaulting to the first sheet: " + sheet.getSheetName());
        }

        int totalRows = sheet.getLastRowNum();
        int currentRow = 1; // Skip header row

        Map<String, List<Map<String, String>>> interactionChunks = new HashMap<>();

            for (int i = currentRow; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, String> dataMap = new HashMap<>();
                for (DataTypeAndColumn column : DataTypeAndColumn.values()) {
                    String columnKey = column.name;
                    Integer colIndex = columnAndIndex.get(columnKey);
                    if (colIndex != null) {
                        Cell cell = row.getCell(colIndex);
                        dataMap.put(columnKey, getCellValueAsString(cell));
                    } else {
                        dataMap.put(columnKey, "N/A");
                    }

                    if (!column.initial && numberOfFeature > 0) {
                        for (int j = 0; j < numberOfFeature; j++) {
                            String featureKey = columnKey + "_" + j;
                            Integer featureIndex = columnAndIndex.get(featureKey);
                            if (featureIndex != null) {
                                Cell featureCell = row.getCell(featureIndex);
                                dataMap.put(featureKey, getCellValueAsString(featureCell));
                            } else {
                                dataMap.put(featureKey, "N/A");
                            }
                        }
                    }
                }

                String interactionNumber = dataMap.get(DataTypeAndColumn.INTERACTION_NUMBER.name());
                if (interactionNumber != null) {
                    interactionChunks.computeIfAbsent(interactionNumber, k -> new ArrayList<>()).add(dataMap);
                }
                dataList.add(dataMap);
            }
        }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "N/A";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "N/A";
        }
    }

    /**
     * Creates and sets the details of an interaction based on the provided parameters, including interaction type,
     * detection method, participant identification method, and host organism. This method also links the interaction
     * to an experiment and a publication if relevant details are provided.
     *
     * @param interaction The interaction object to be populated with details.
     * @param interactionDetectionMethod The method used to detect the interaction, used to create a CvTerm for detection method.
     * @param participantIdentificationMethod The method used to identify the participant, used to create a CvTerm for identification method.
     * @param hostOrganism The host organism associated with the interaction, used to fetch the organism's tax ID.
     * @param interactionType The type of the interaction, used to create a CvTerm for interaction type.
     */
    private void interactionCreator(XmlInteractionEvidence interaction, String interactionDetectionMethod,
                                    String participantIdentificationMethod,
                                    String hostOrganism, String interactionType) {
        if (interactionType != null) {
            String interactionTypeMiId = utils.fetchMiId(interactionType);
            CvTerm interactionTypeCv = new XmlCvTerm(interactionType, interactionTypeMiId);
            interaction.setInteractionType(interactionTypeCv);
        }

        Organism organism = null;
        if (hostOrganism != null) {
            String hostOrganismTaxId = utils.fetchTaxIdForOrganism(hostOrganism);
            if (hostOrganismTaxId != null) {
                try {
                    int hostOrganismInt = Integer.parseInt(hostOrganismTaxId);
                    organism = new XmlOrganism(hostOrganismInt);
                } catch (NumberFormatException e) {
                    LOGGER.warning("Invalid host organism tax ID: " + hostOrganism);
                }
            } else {
                LOGGER.warning("No Tax ID found for host organism: " + hostOrganism);
            }
        } else {
            LOGGER.warning("Host organism is null. Skipping organism creation.");
        }

        if (interactionDetectionMethod != null) {
            String interactionDetectionMiId = utils.fetchMiId(interactionDetectionMethod);
            CvTerm detectionMethod = new XmlCvTerm(interactionDetectionMethod, interactionDetectionMiId);
            Publication publication = new BibRef(excelFileReader.getPublicationId());
            XmlExperiment experiment = new XmlExperiment(publication, detectionMethod, organism);

            if (participantIdentificationMethod != null) {
                String identificationMethodMiId = utils.fetchMiId(participantIdentificationMethod);
                CvTerm identificationMethodCv = new XmlCvTerm(participantIdentificationMethod, identificationMethodMiId);
                experiment.setParticipantIdentificationMethod(identificationMethodCv);
            }

            interaction.setExperiment(experiment);
        }

        xmlModelledInteractions.add(interaction);
    }

    /**
     * Groups the dataset into interactions by grouping participant data based on the interaction number.
     *
     * @return a map where keys are interaction numbers, and values are lists of participant data maps.
     */
    public Map<String, List<Map<String, String>>> createGroups(){
        return dataList.stream().collect(Collectors.groupingBy(participant ->
                participant.get(DataTypeAndColumn.INTERACTION_NUMBER.name)));
    }

    /**
     * Creates interactions by processing grouped participant data and linking them to experiments and publications.
     */
    public void createInteractions(){
        Map<String, List<Map<String, String>>> groups = createGroups();

        for (Map.Entry<String, List<Map<String, String>>> group : groups.entrySet()) {
            XmlInteractionEvidence interaction = new XmlInteractionEvidence();

            String interactionDetectionMethod = "detectionMethod";
            String participantIdentificationMethod = "participantIdentificationMethod";
            String hostOrganism = "hostOrganism";
            String interactionType = "interactionType";

            for (Map<String, String> participant : group.getValue()) {
                XmlParticipantEvidence newParticipant = createParticipant(participant);
                interaction.addParticipant(newParticipant);

                interactionDetectionMethod = participant.get(DataTypeAndColumn.INTERACTION_DETECTION_METHOD.name);
                participantIdentificationMethod = participant.get(DataTypeAndColumn.PARTICIPANT_IDENTIFICATION_METHOD.name);
                hostOrganism = participant.get(DataTypeAndColumn.HOST_ORGANISM.name);
                interactionType = participant.get(DataTypeAndColumn.INTERACTION_TYPE.name);

            }

            interactionCreator(interaction, interactionDetectionMethod, participantIdentificationMethod, hostOrganism, interactionType);
        }
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
}