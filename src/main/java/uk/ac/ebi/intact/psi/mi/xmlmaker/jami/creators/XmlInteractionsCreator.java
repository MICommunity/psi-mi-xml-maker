package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.creators;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.xml.model.extension.xml300.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.InputData;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.XmlFileWriter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.FileUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.FileReader;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.InputData.*;
import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils.getDataKey;

/**
 * The XmlInteractionsCreator class is responsible for processing Excel files or directory-based input files
 * to create biological interaction evidence in the XML format using the JAMI library.
 * It processes participants, interactions, and related data, and generates XML objects for interaction modeling.
 */
public class XmlInteractionsCreator {
    // Constants
    private static final Logger LOGGER = Logger.getLogger(XmlInteractionsCreator.class.getName());
    private static final int DEFAULT_MAX_INTERACTIONS_PER_FILE = 1_000;

    // Configurable properties with default values
    @Setter
    private static int MAX_INTERACTIONS_PER_FILE = DEFAULT_MAX_INTERACTIONS_PER_FILE;

    @Setter
    private int numberOfFeature = 1;

    @Setter
    private Map<String, Integer> columnAndIndex;

    @Getter @Setter
    private String publicationId;

    @Getter @Setter
    private String sheetSelected;

    // State variables
    private boolean isFileFinished;
    private final List<XmlInteractionEvidence> xmlModelledInteractions = new ArrayList<>();
    private final List<Map<String, String>> dataList = new ArrayList<>();

    // Dependencies
    private final FileReader fileReader;
    private final XmlFileWriter xmlFileWriter;
    private final XmlMakerUtils utils = new XmlMakerUtils();

    /**
     * Constructs an XmlInteractionsCreator with the specified Excel reader, Uniprot mapper GUI, and column-to-index mapping.
     *
     * @param reader           the Excel file reader for fetching data from Excel files.
     * @param writer           the XmlFileWriter
     * @param columnAndIndex   the mapping of column names to their corresponding indices in the dataset.
     */
    public XmlInteractionsCreator(FileReader reader, XmlFileWriter writer, Map<String, Integer> columnAndIndex) {
        this.fileReader = reader;
        this.xmlFileWriter = writer;
        this.columnAndIndex = columnAndIndex;
        this.publicationId = fileReader.publicationId;
    }

    /**
     * Creates participants and interactions based on the provided file format.
     * Clears existing participants and interactions and repopulates them using the data read from files or the workbook.
     **/
    public void createParticipantsWithFileFormat() {
        xmlModelledInteractions.clear();
        dataList.clear();

        if (fileReader.getWorkbook() == null) {
            fetchDataFileWithSeparator(columnAndIndex);
        } else {
            fetchDataWithWorkbook(columnAndIndex);
        }
    }

    /**
     * Creates an {@link XmlParticipantEvidence} object based on provided participant data.
     *
     * @param data A map containing participant-related data fields.
     * @return The constructed {@link XmlParticipantEvidence}, or null if required fields are missing.
     */
    public XmlParticipantEvidence createParticipant(Map<String, String> data) {
        String participantExperimentalRole = data.get(EXPERIMENTAL_ROLE.name);

        InputData[] required = {PARTICIPANT_NAME, PARTICIPANT_ID, PARTICIPANT_ID_DB, PARTICIPANT_ORGANISM};

        for (InputData requiredColumn : required) {
            String key = getDataKey(requiredColumn, data);
            if (data.get(key) == null || data.get(key).trim().isBlank()) {
                LOGGER.warning(requiredColumn.name + " is required but missing or empty.");

                if (data.get(PARTICIPANT_NAME.name) != null && !data.get(PARTICIPANT_NAME.name).trim().isBlank()) {
                    xmlFileWriter.skippedParticipants.add(data.get(PARTICIPANT_NAME.name));
                }
                else if (data.get(PARTICIPANT_ID.name) != null && !data.get(PARTICIPANT_ID.name).trim().isBlank()) {
                    xmlFileWriter.skippedParticipants.add(data.get(PARTICIPANT_ID.name));
                }
                return null;
            }
        }

        // Fetch CV terms using the updated key logic
        Map<InputData, CvTerm> terms = fetchCvTermsFromData(data);

        // Use getDataKey for experimental preparations
        List<CvTerm> experimentalPreparations = getExperimentalPreparations(data.get(getDataKey(EXPERIMENTAL_PREPARATION, data)));

        // Use getDataKey for other fields
        CvTerm participantIdDb = terms.get(PARTICIPANT_ID_DB);
        CvTerm experimentalRole = terms.get(EXPERIMENTAL_ROLE);
        CvTerm xref = terms.get(PARTICIPANT_XREF);
        CvTerm xrefDb = terms.get(PARTICIPANT_XREF_DB);
        CvTerm participantIdentificationMethod = terms.get(PARTICIPANT_IDENTIFICATION_METHOD);
        String name = Objects.requireNonNull(data.get(getDataKey(PARTICIPANT_NAME, data)), "The participant name cannot be null");
        String participantId = data.get(getDataKey(PARTICIPANT_ID, data));
        String participantOrganism = data.get(getDataKey(PARTICIPANT_ORGANISM, data));

        Xref uniqueId = new XmlXref(participantIdDb, participantId);

        if (participantOrganism == null || participantOrganism.isEmpty()) {
            LOGGER.warning("Missing or invalid participant organism for participant: " + name);
            return null;
        }

        Organism organism = createOrganism(participantOrganism);

        Interactor participant = createParticipantByType(data.get(PARTICIPANT_TYPE.name), name, organism);
        participant.setShortName(name);
        participant.getIdentifiers().add(uniqueId);

        XmlParticipantEvidence participantEvidence = new XmlParticipantEvidence(participant);

        String participantExpressedInOrganism = data.get(PARTICIPANT_EXPRESSED_IN_ORGANISM.name + participantExperimentalRole);
        setParticipantExpressedInOrganism(participantEvidence, participantExpressedInOrganism);

        if (experimentalRole != null) {
            participantEvidence.setExperimentalRole(experimentalRole);
        }

        addFeatures(participantEvidence, data);

        addXrefs(participantEvidence, xref, xrefDb);

        addExperimentalPreparations(participantEvidence, experimentalPreparations);

        if (participantIdentificationMethod != null) {
            participantEvidence.getIdentificationMethods().add(participantIdentificationMethod);
        }

        return participantEvidence;
    }

    /**
     * Adds experimental preparations to the given participant evidence.
     *
     * @param participantEvidence       The participant to update.
     * @param experimentalPreparations List of experimental preparation terms.
     */
    private void addExperimentalPreparations(XmlParticipantEvidence participantEvidence, List<CvTerm> experimentalPreparations) {
        for (CvTerm experimentalPreparation : experimentalPreparations) {
            if (experimentalPreparation != null) {
                participantEvidence.getExperimentalPreparations().add(experimentalPreparation);
            }
        }
    }

    /**
     * Adds cross-references (xrefs) to the participant evidence.
     *
     * @param participantEvidence The participant to update.
     * @param xref                The xref term (identifier).
     * @param xrefDb              The database term for the xref.
     */
    private void addXrefs(ParticipantEvidence participantEvidence, CvTerm xref, CvTerm xrefDb) {
        if (xref != null && xref.getShortName() != null && xrefDb != null) {
            Xref xmlXref = new XmlXref(xrefDb, xref.getShortName());
            xmlXref.getDatabase().setFullName(xrefDb.getShortName());
            xmlXref.getDatabase().setMIIdentifier(xrefDb.getMIIdentifier());
            participantEvidence.getXrefs().add(xmlXref);
        }
    }

    /**
     * Adds features to the given participant evidence based on input data.
     *
     * @param participantEvidence The participant to update.
     * @param data                The input data containing feature information.
     */
    private void addFeatures(XmlParticipantEvidence participantEvidence, Map<String, String> data) {
        if (numberOfFeature > 0) {
            for (int i = 0; i < numberOfFeature; i++) {
                XmlFeatureEvidence feature = XmlFeatureEvidenceCreator.createFeature(i, data);
                if (feature != null) {
                    participantEvidence.addFeature(feature);
                }
            }
        }
    }

    /**
     * Sets the expressed-in organism for the participant, if available.
     *
     * @param participantEvidence              The participant to update.
     * @param participantExpressedInOrganism  The Organism string to use.
     */
    private void setParticipantExpressedInOrganism(ParticipantEvidence participantEvidence, String participantExpressedInOrganism) {
        if (!participantExpressedInOrganism.trim().isEmpty()) {
            Organism participantExpressedIn = createOrganism(participantExpressedInOrganism);
            participantEvidence.setExpressedInOrganism(participantExpressedIn);
        }
    }

    /**
     * Creates an interactor instance based on type, name, and organism.
     *
     * @param participantType The type of participant (e.g., protein, gene).
     * @param name            The name of the participant.
     * @param organism        The organism associated with the participant.
     * @return The created {@link Interactor} instance.
     */
    private Interactor createParticipantByType(String participantType, String name, Organism organism) {
        Interactor participant = new XmlPolymer();

        switch (participantType.toLowerCase().trim()){
            case "protein":
                participant = new XmlProtein(name, organism);
                break;
            case "nucleic acid":
                participant = new XmlNucleicAcid(name, organism);
                CvTerm nucleicAcidType = XmlMakerUtils.fetchTerm("nucleic acid");
                participant.setInteractorType(nucleicAcidType); // needed as the type is not set automatically by jami here
                break;
            case "molecule":
                participant = new XmlMolecule(name, organism);
                CvTerm moleculeType = XmlMakerUtils.fetchTerm("small molecule");
                participant.setInteractorType(moleculeType); // needed as the type is not set automatically by jami here
                break;
            case "gene":
                participant = new XmlGene(name, organism);
                break;
            default:
                break;
        }
        return participant;
    }

    /**
     * Parses a semicolon-separated string of experimental preparations into CvTerms.
     *
     * @param experimentalPreparation The input string.
     * @return A list of corresponding {@link CvTerm} objects.
     */
    private List<CvTerm> getExperimentalPreparations(String experimentalPreparation){
        List<CvTerm> experimentalPreparations = new ArrayList<>();
        if (experimentalPreparation != null) {
            if (!experimentalPreparation.contains(";")) {
                experimentalPreparations.add(XmlMakerUtils.fetchTerm(experimentalPreparation));
            } else {
                String[] preparations = experimentalPreparation.split(";");
                for (String preparation : preparations) {
                    experimentalPreparations.add(XmlMakerUtils.fetchTerm(preparation.trim()));
                }
            }
        }
        return experimentalPreparations;
    }

    /**
     * Asynchronously fetches controlled vocabulary terms from participant data.
     *
     * @param data The participant data map.
     * @return A map of {@link InputData} to resolved {@link CvTerm}s.
     */
    private Map<InputData, CvTerm> fetchCvTermsFromData(Map<String, String> data) {
        return Stream.of(PARTICIPANT_TYPE, PARTICIPANT_ID_DB, EXPERIMENTAL_ROLE, EXPERIMENTAL_PREPARATION, PARTICIPANT_IDENTIFICATION_METHOD, PARTICIPANT_XREF, PARTICIPANT_XREF_DB)
                .map(type -> Map.entry(type, data.get(getDataKey(type, data))))
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> CompletableFuture.supplyAsync(() -> XmlMakerUtils.fetchTerm(e.getValue()))
                ))
                .entrySet().stream()
                .filter(e -> e.getValue().join() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().join()
                ));
    }

    /**
     * Fetches data from a file using a separator (e.g., CSV or TSV) and stores it in a list of maps for processing.
     *
     * @param columnAndIndex the mapping of column names to their corresponding indices in the dataset.
     */
    public void fetchDataFileWithSeparator(Map<String, Integer> columnAndIndex) {
        Iterator<List<String>> data = fileReader.readFileWithSeparator();
        int expectedNumberOfColumns = fileReader.fileData.size();
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
                LOGGER.warning("Row has fewer cells than expected. Skipping row: " + datum + "\n Size expected: " + expectedNumberOfColumns + "Row size: " + datum.size());
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
        Iterator<Row> data = fileReader.readWorkbookSheet(sheetSelected);
        int expectedNumberOfColumns = fileReader.fileData.size();
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
                LOGGER.warning("Row has fewer cells than expected. Skipping row: " + datum + "\n Size expected: " + expectedNumberOfColumns + "Row size: " + datum.size());
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
        for (InputData column : InputData.values()) {
            if (column.initial) {
                String key = column.name;
                if (column.experimentalRoleDependent) {
                    String experimentalRole = datum.get(columnAndIndex.get(EXPERIMENTAL_ROLE.name));
                    if (experimentalRole != null && !experimentalRole.trim().isEmpty()) {
                        key = column.name + experimentalRole;
                    }
                }
                if (columnAndIndex.get(column.name) < datum.size()) {
                    dataMap.put(key, datum.get(columnAndIndex.get(column.name)));
                } else {
                    dataMap.put(key, "");
                }
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
                                            String hostOrganism, String interactionType,
                                            String variableParameterDescs,
                                            String variableParameterValues,
                                            String variableParameterUnits) {
        if (interactionType != null) {
            String interactionTypeMiId = XmlMakerUtils.fetchMiId(interactionType);
            CvTerm interactionTypeCv = new XmlCvTerm(interactionType, interactionTypeMiId);
            interaction.setInteractionType(interactionTypeCv);
        }

        if (interactionDetectionMethod != null) {
            Organism organism = createOrganism(hostOrganism);

            CvTerm detectionMethod = XmlMakerUtils.fetchTerm(interactionDetectionMethod);
            Publication publication = new BibRef(fileReader.getPublicationId());
            XmlExperiment experiment;

            if (organism == null || hostOrganism.isBlank()) {
                experiment = new XmlExperiment(publication, detectionMethod);
            } else {
                experiment = new XmlExperiment(publication, detectionMethod, organism);
                for (VariableParameter parameter : getVariableParameters(variableParameterDescs, variableParameterValues, variableParameterUnits)) {
                    experiment.addVariableParameter(parameter);
                }
            }
            if (participantIdentificationMethod != null) {
                experiment.setParticipantIdentificationMethod(XmlMakerUtils.fetchTerm(participantIdentificationMethod));
            }

            interaction.setExperiment(experiment);
        }
        xmlModelledInteractions.add(interaction);
        launchWriting();
    }

    private List<VariableParameter> getVariableParameters(String descriptions, String values, String units){
        List<VariableParameter> variableParameters = new ArrayList<>();

        if (descriptions == null || values == null || units == null) {
            return variableParameters;
        }

        String[] valuesArray = values.split(";");
        String[] unitsArray = units.split(";");
        String[] descriptionsArray = descriptions.split(";");

        for (int i = 0; i < descriptionsArray.length; i++) {
            VariableParameter variableParameter = new XmlVariableParameter();
            variableParameter.setDescription(descriptionsArray[i]);
            variableParameter.setUnit(XmlMakerUtils.fetchTerm(unitsArray[i]));
            VariableParameterValue variableParameterValue = new XmlVariableParameterValue(valuesArray[i], variableParameter);
            variableParameter.getVariableValues().add(variableParameterValue);
            variableParameters.add(variableParameter);
        }

        return variableParameters;
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
        String interactionFigureLegend = null;
        String experimentalVariableConditionDescs = null;
        String experimentalVariableConditionValues = null;
        String experimentalVariableConditionUnits = null;

        for (Map<String, String> participant : dataList) {
            XmlParticipantEvidence newParticipant = createParticipant(participant);
            interaction.addParticipant(newParticipant);

            interactionDetectionMethod = participant.get(INTERACTION_DETECTION_METHOD.name);
            participantIdentificationMethod = participant.get(PARTICIPANT_IDENTIFICATION_METHOD.name);
            hostOrganism = participant.get(HOST_ORGANISM.name);
            interactionType = participant.get(INTERACTION_TYPE.name);
            interactionFigureLegend = participant.get(INTERACTION_FIGURE_LEGEND.name);

            String parameterType = participant.get(INTERACTION_PARAM_TYPE.name);
            String parameterValue = participant.get(INTERACTION_PARAM_VALUE.name);
            String parameterUncertainty = participant.get(INTERACTION_PARAM_UNCERTAINTY.name);
            String parameterUnit = participant.get(INTERACTION_PARAM_UNIT.name);
            String parameterExponent = participant.get(INTERACTION_PARAM_EXPONENT.name);
            String parameterBase = participant.get(INTERACTION_PARAM_BASE.name);

            experimentalVariableConditionDescs = participant.get(EXPERIMENTAL_VARIABLE_CONDITION_DESCRIPTION.name);
            experimentalVariableConditionValues = participant.get(EXPERIMENTAL_VARIABLE_CONDITION_VALUE.name);
            experimentalVariableConditionUnits = participant.get(EXPERIMENTAL_VARIABLE_CONDITION_UNIT.name);

            if (parameterType != null && !parameterType.isEmpty()) {
                List<XmlParameter> interactionParameters = XmlParameterCreator.createParameter(parameterType, parameterValue, parameterUncertainty, parameterUnit, parameterExponent, parameterBase);
                for (XmlParameter interactionParameter : interactionParameters) {
                    if (!interaction.getParameters().contains(interactionParameter)) {
                        interaction.getParameters().add(interactionParameter);
                    }
                }
            }
        }
        if (interactionFigureLegend != null) {
            CvTerm annotationType = XmlMakerUtils.fetchTerm("figure legend");
            Annotation annotation = null;
            if (annotationType != null) {
                annotation = new XmlAnnotation(annotationType, interactionFigureLegend);
            }
            interaction.getAnnotations().add(annotation);
        }


        processInteractionCreation(interaction,
                interactionDetectionMethod,
                participantIdentificationMethod,
                hostOrganism,
                interactionType,
                experimentalVariableConditionDescs,
                experimentalVariableConditionValues,
                experimentalVariableConditionUnits
                );
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
     * <p>Once written, the interaction list is cleared.</p>
     *
     * <p>Logs the number of interactions being processed before writing.</p>
     */
    private void launchWriting() {
        if (xmlModelledInteractions.size() >= MAX_INTERACTIONS_PER_FILE || isFileFinished) {
            LOGGER.info("Processing " + xmlModelledInteractions.size() + " interactions.");
            xmlFileWriter.writeInteractions(xmlModelledInteractions);
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
}