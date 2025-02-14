package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import psidev.psi.mi.jami.exception.IllegalRangeException;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.xml.model.extension.xml300.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.FileUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import psidev.psi.mi.jami.utils.PositionUtils;
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

    @Setter
    private int numberOfFeature = 1;

    private static final Logger LOGGER = Logger.getLogger(InteractionsCreator.class.getName());

    /**
     * Constructs an InteractionsCreator with the specified Excel reader, Uniprot mapper GUI, and column-to-index mapping.
     *
     * @param reader           the Excel file reader for fetching data from Excel files.
     * @param writer           the InteractionWriter
     * @param columnAndIndex   the mapping of column names to their corresponding indices in the dataset.
     */
    public InteractionsCreator(ExcelFileReader reader, InteractionWriter writer, Map<String, Integer> columnAndIndex) {
        this.excelFileReader = reader;
        this.interactionWriter = writer;
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
        DataTypeAndColumn[] required = {PARTICIPANT_NAME, PARTICIPANT_ID, PARTICIPANT_ID_DB, PARTICIPANT_ORGANISM};

        for (DataTypeAndColumn requiredColumn : required) {
            if (data.get(requiredColumn.name) == null || data.get(requiredColumn.name).isBlank()) {
                LOGGER.warning(requiredColumn.name + " is required but missing or empty.");
                return null;
            }
        }

        Map<DataTypeAndColumn, CvTerm> terms = fetchCvTermsFromData(data);
        List<CvTerm> experimentalPreparations = getExperimentalPreparations(data.get(EXPERIMENTAL_PREPARATION.name));
        CvTerm participantIdDb = terms.get(PARTICIPANT_ID_DB);
        CvTerm experimentalRole = terms.get(EXPERIMENTAL_ROLE);
        CvTerm xref= terms.get(PARTICIPANT_XREF);
        CvTerm xrefDb = terms.get(PARTICIPANT_XREF_DB);
        CvTerm participantIdentificationMethod = terms.get(PARTICIPANT_IDENTIFICATION_METHOD);

        String name = Objects.requireNonNull(data.get(PARTICIPANT_NAME.name), "The participant name cannot be null");
        String participantId = data.get(PARTICIPANT_ID.name);
        String participantOrganism = data.get(PARTICIPANT_ORGANISM.name);
        Xref uniqueId = new XmlXref(participantIdDb, participantId);

        Xref authorName = new XmlXref(participantIdDb, name, utils.fetchTerm("inferred by author"));

        if (participantOrganism == null || participantOrganism.isEmpty()) {
            LOGGER.warning("Missing or invalid participant organism for participant: " + name);
            return null;
        }

        Organism organism = createOrganism(participantOrganism);

        Interactor participant = createParticipantByType(data.get(PARTICIPANT_TYPE.name), name, organism, uniqueId);
        XmlParticipantEvidence participantEvidence = new XmlParticipantEvidence(participant);

        String participantExpressedInOrganism = data.get(PARTICIPANT_EXPRESSED_IN_ORGANISM.name);
        setParticipantExpressedInOrganism(participantEvidence, participantExpressedInOrganism);

        if (experimentalRole != null) {
            participantEvidence.setExperimentalRole(experimentalRole);
        }

        addFeatures(participantEvidence, data);


        //TODO: see the commented part for psi-mi verification:
//        Xref inputParticipantName = new XmlXref(participantIdDb, name, utils.fetchTerm("inferred by author"));
//        participantEvidence.getXrefs().add(inputParticipantName);
//        addXrefs(participantEvidence, xref, xrefDb, authorName);

        addExperimentalPreparations(participantEvidence, experimentalPreparations);

        if (participantIdentificationMethod != null) {
            participantEvidence.getIdentificationMethods().add(participantIdentificationMethod);
        }

        return participantEvidence;
    }

    private void addExperimentalPreparations(XmlParticipantEvidence participantEvidence, List<CvTerm> experimentalPreparations) {
        for (CvTerm experimentalPreparation : experimentalPreparations) {
            if (experimentalPreparation != null) {
                participantEvidence.getExperimentalPreparations().add(experimentalPreparation);
            }
        }
    }

    private void addXrefs(ParticipantEvidence participantEvidence, CvTerm xref, CvTerm xrefDb, Xref authorName) {
        if (xref != null && xref.getShortName() != null && xrefDb != null) {
            Xref xmlXref = new XmlXref(xref, xref.getShortName());
            xmlXref.getDatabase().setFullName(xrefDb.getShortName());
            xmlXref.getDatabase().setMIIdentifier(xrefDb.getMIIdentifier());
            participantEvidence.getXrefs().add(xmlXref);
            participantEvidence.getXrefs().add(authorName);
        }
    }

    private void addFeatures(XmlParticipantEvidence participantEvidence, Map<String, String> data) {
        if (numberOfFeature > 0) {
            for (int i = 0; i < numberOfFeature; i++) {
                XmlFeatureEvidence feature = createFeature(i, data);
                if (feature != null) {
                    participantEvidence.addFeature(feature);
                }
            }
        }
    }

    private void setParticipantExpressedInOrganism(ParticipantEvidence participantEvidence, String participantExpressedInOrganism) {
        if (!participantExpressedInOrganism.trim().isEmpty()) {
            Organism participantExpressedIn = createOrganism(participantExpressedInOrganism);
            participantEvidence.setExpressedInOrganism(participantExpressedIn);
        }
    }

    private Interactor createParticipantByType(String participantType, String name, Organism organism, Xref uniqueId) {
        Interactor participant = new XmlPolymer();

        switch (participantType.toLowerCase().trim()){
            case "protein":
                participant = new XmlProtein(name, organism, uniqueId);
                break;
            case "nucleic acid":
                participant = new XmlNucleicAcid(name, organism, uniqueId);
                break;
            case "molecule":
                participant = new XmlMolecule(name, organism, uniqueId);
                break;
            case "gene":
                participant = new XmlGene(name, organism, uniqueId);
                break;
            default:
                break;
        }

        return participant;
    }

    private List<CvTerm> getExperimentalPreparations(String experimentalPreparation){
        List<CvTerm> experimentalPreparations = new ArrayList<>();
        if (experimentalPreparation != null) {
            if (!experimentalPreparation.contains(";")) {
                experimentalPreparations.add(utils.fetchTerm(experimentalPreparation));
            } else {
                String[] preparations = experimentalPreparation.split(";");
                for (String preparation : preparations) {
                    experimentalPreparations.add(utils.fetchTerm(preparation.trim()));
                }
            }
        }
        return experimentalPreparations;
    }

    private Map<DataTypeAndColumn, CvTerm> fetchCvTermsFromData(Map<String, String> data) {
        Map<DataTypeAndColumn, CompletableFuture<CvTerm>> futureTerms = Stream.of(PARTICIPANT_TYPE, PARTICIPANT_ID_DB,
                        EXPERIMENTAL_ROLE, EXPERIMENTAL_PREPARATION, PARTICIPANT_IDENTIFICATION_METHOD,
                        PARTICIPANT_XREF, PARTICIPANT_XREF_DB)
                .map(type -> Map.entry(type, data.get(type.name)))
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> CompletableFuture.supplyAsync(() -> utils.fetchTerm(e.getValue()))
                ));

        return futureTerms.entrySet().stream()
                .filter(e -> e.getValue().join() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().join()));
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
        String currentInteractionNumber = "0";

        if (data.hasNext()) {
            List<String> firstRow = data.next();
            currentInteractionNumber = firstRow.get(interactionNumberColumn);
            Iterator<List<String>> finalData = data;
            data = new Iterator<>() {
                boolean firstRowProcessed = false;

                @Override
                public boolean hasNext() {
                    return !firstRowProcessed || finalData.hasNext();
                }

                @Override
                public List<String> next() {
                    if (!firstRowProcessed) {
                        firstRowProcessed = true;
                        return firstRow;
                    } else {
                        return finalData.next();
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

            currentInteractionNumber = getInteractionNumber(columnAndIndex, interactionNumberColumn, currentInteractionNumber, datum);
        }

        isFileFinished = true;
        createInteractions();
        dataList.clear();
    }

    /**
     * Processes data from the workbook, organizes it into a structured format, and manages interactions.
     *
     * @param columnAndIndex a map containing column names and their corresponding indices.
     */
    public void fetchDataWithWorkbook(Map<String, Integer> columnAndIndex) {
        Iterator<Row> data = excelFileReader.readWorkbookSheet(sheetSelected);
        int expectedNumberOfColumns = excelFileReader.fileData.size();
        int interactionNumberColumn = columnAndIndex.get(INTERACTION_NUMBER.name);
        String currentInteractionNumber = "0";

        if (data.hasNext()) {
            Row firstRow = data.next();
            List<String> firstRowData = new ArrayList<>();
            int firstCellNum = firstRow.getFirstCellNum();
            int lastCellNum = firstRow.getLastCellNum();

            for (int cellNum = firstCellNum; cellNum < lastCellNum; cellNum++) {
                Cell cell = firstRow.getCell(cellNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (cell == null) {
                    firstRowData.add("");
                } else {
                    firstRowData.add(FileUtils.getCellValueAsString(cell));
                }
            }

            currentInteractionNumber = firstRowData.get(interactionNumberColumn);

            Iterator<Row> finalData = data;
            data = new Iterator<>() {
                boolean firstRowProcessed = false;

                @Override
                public boolean hasNext() {
                    return !firstRowProcessed || finalData.hasNext();
                }

                @Override
                public Row next() {
                    if (!firstRowProcessed) {
                        firstRowProcessed = true;
                        return firstRow;
                    } else {
                        return finalData.next();
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
                    datum.add("");
                } else {
                    datum.add(FileUtils.getCellValueAsString(cell));
                }
            }

            if (datum.size() < expectedNumberOfColumns) {
                LOGGER.warning("Row has fewer cells than expected. Skipping row: " + datum);
                continue;
            }

            currentInteractionNumber = getInteractionNumber(columnAndIndex, interactionNumberColumn, currentInteractionNumber, datum);
        }

        isFileFinished = true;
        createInteractions();
        dataList.clear();
    }

    /**
     * Retrieves the current interaction number from the provided data list and updates the interaction set.
     * If the interaction number changes, it triggers the creation of a new interaction
     * and clears the previous data list.
     *
     * @param columnAndIndex A map associating column names with their respective indices.
     * @param interactionNumberColumn The index of the column containing interaction numbers.
     * @param currentInteractionNumber The interaction number from the previous iteration.
     * @param datum A list containing the current row of data.
     * @return The updated interaction number.
     */
    private String getInteractionNumber(Map<String, Integer> columnAndIndex, int interactionNumberColumn,
                                        String currentInteractionNumber, List<String> datum) {
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
        return currentInteractionNumber;
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

    /**
     * Creates an {@link Organism} object for the given host organism name.
     *
     * @param hostOrganism the name of the host organism. If null, creation is skipped.
     * @return an {@link Organism} object, or {@code null} if:
     *         <ul>
     *           <li>The host organism is null.</li>
     *           <li>No Tax ID is found.</li>
     *           <li>The Tax ID is not a valid integer.</li>
     *         </ul>
     *
     * <p>Logs warnings for null input, missing Tax ID, or invalid Tax ID format.</p>
     */
    private Organism createOrganism(String hostOrganism) {
        if (hostOrganism == null || hostOrganism.contains("organism") || hostOrganism.isEmpty()) {
            LOGGER.warning("Host organism is null. Skipping organism creation.");
            return null;
        }

        String hostOrganismTaxId = XmlMakerUtils.fetchTaxIdForOrganism(hostOrganism);

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

        String featureType = data.get(FEATURE_TYPE.name + featureIndexString);
        String featureStart = data.get(FEATURE_START.name + featureIndexString);
        String featureRangeType = data.get(FEATURE_RANGE_TYPE.name + featureIndexString);
        String featureXref = data.get(FEATURE_XREF.name + featureIndexString);
        String featureXrefDb = data.get(FEATURE_XREF_DB.name + featureIndexString);

        XmlFeatureEvidence featureEvidence = getFeatureEvidence(featureType);

        if (featureEvidence.getType().getMIIdentifier() == null || featureEvidence.getType().getMIIdentifier().isEmpty()) {
            return null;
        }


        Position position = getRangePosition(featureStart, featureRangeType);
        XmlRange featureRange = getFeatureRange(position, position);

        FeatureXrefContainer featureXrefContainer = getFeatureXrefContainer(featureXref, featureXrefDb);

        featureEvidence.setJAXBXref(featureXrefContainer);
        featureEvidence.setJAXBRangeWrapper(new AbstractXmlFeature.JAXBRangeWrapper());
        featureEvidence.getRanges().add(featureRange);

        return featureEvidence;
    }

    private FeatureXrefContainer getFeatureXrefContainer(String featureXref, String featureXrefDb) {
        FeatureXrefContainer featureXrefContainer = new FeatureXrefContainer();

        if (featureXref != null && featureXrefDb != null) {
            List<String> featuresXrefs = getFeatureXrefs(featureXref);
            List<CvTerm> featuresXrefsDb = getFeatureXrefsDb(featureXrefDb);

            // Ensure both lists are the same size before proceeding
            int size = Math.min(featuresXrefs.size(), featuresXrefsDb.size());

            for (int i = 0; i < size; i++) {
                String xref = featuresXrefs.get(i);
                CvTerm xrefDb = featuresXrefsDb.get(i);
                if (xref != null && xrefDb != null) {
                    Xref featureXrefXml = new XmlXref(xrefDb, xref);
                    featureXrefContainer.getXrefs().add(featureXrefXml);
                }
            }
        }

        return featureXrefContainer;
    }

    private List<CvTerm> getFeatureXrefsDb(String featureXrefDb) {
        if (featureXrefDb == null || featureXrefDb.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(featureXrefDb.split(";"))
                .map(String::trim)
                .map(utils::fetchTerm)
                .collect(Collectors.toList());
    }

    private List<String> getFeatureXrefs(String featureXref) {
        if (featureXref == null || featureXref.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(featureXref.split(";"))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private XmlFeatureEvidence getFeatureEvidence(String featureType) {
        String featureTypeMiId = utils.fetchMiId(featureType);
        CvTerm featureTypeCv = new XmlCvTerm(featureType, featureTypeMiId);
        return new XmlFeatureEvidence(featureTypeCv);
    }

    private XmlRange getFeatureRange(Position startPosition, Position endPosition) {
        XmlRange featureRange = new XmlRange();
        featureRange.setPositions(startPosition, endPosition);
        return featureRange;
    }

    /**
     * Writes the current batch of interactions to a file if certain conditions are met.
     *
     * <p>The interactions are written if either:</p>
     * <ul>
     *   <li>The number of interactions reaches or exceeds the maximum allowed per file.</li>
     *   <li>The file has been marked as finished.</li>
     * </ul>
     *
     * <p>Once written, the interactions list is cleared.</p>
     *
     * <p>Logs the number of interactions being processed before writing.</p>
     */
    private void launchWriting() {
        if (xmlModelledInteractions.size() >= MAX_INTERACTIONS_PER_FILE || isFileFinished) {
            LOGGER.info("Processing " + xmlModelledInteractions.size() + " interactions.");
            interactionWriter.writeInteractions(xmlModelledInteractions);
            xmlModelledInteractions.clear();
        }
    }

    /**
     * Finds the most similar column name from a list of column names based on the
     * similarity to a given GUI column name. The similarity is calculated using a
     * similarity scoring function, which returns a percentage indicating how similar
     * the column names are.
     *
     * @param columns a list of column names to compare against the given GUI column name
     * @param guiColumnName the column name from the GUI to which similarity is calculated
     * @return the column name from the list that is most similar to the GUI column name
     */
    public String getMostSimilarColumn(List<String> columns, String guiColumnName) {
        String mostSimilarColumn = null;
        double mostSimilarColumnScore = 0;
        for (String column : columns) {
            if (utils.calculateSimilarity(column, guiColumnName) > mostSimilarColumnScore) {
                mostSimilarColumn = column;
                mostSimilarColumnScore = utils.calculateSimilarity(column, guiColumnName);
            }
        }
        return mostSimilarColumn;
    }

    /**
     * Determines the position type based on the given range and feature range type.
     * If a feature range type is provided, it creates a position using the feature type
     * and its corresponding MI identifier. Otherwise, it interprets the range string
     * to determine whether it represents a terminal position (N-terminal or C-terminal),
     * a numeric position, or an undetermined position.
     *
     * @param range The range value, which may be a numeric position or a terminal designation (e.g., "c-term").
     * @param featureRangeType The specific feature range type, if available.
     * @return A {@code Position} object representing the determined range type.
     * @throws RuntimeException If an invalid numeric range is encountered.
     */
    public Position getRangePosition(String range, String featureRangeType) {
        if (featureRangeType.contains("c-term")) return PositionUtils.createCTerminalRangePosition();

        if (featureRangeType.contains("n-term")) return PositionUtils.createNTerminalRangePosition();

        if (range.matches("\\d+")) {
            try {
                return PositionUtils.createPositionFromString(range);
            } catch (IllegalRangeException e) {
                throw new RuntimeException(e);
            }
        }

        return PositionUtils.createUndeterminedPosition();
    }

}