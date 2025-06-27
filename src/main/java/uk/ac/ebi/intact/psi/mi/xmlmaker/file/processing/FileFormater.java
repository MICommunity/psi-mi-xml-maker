package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import lombok.Getter;
import lombok.Setter;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.InputData.*;
import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.GuiUtils.*;
import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.FileUtils.*;

import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.InputData;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.gui.ParametersGui;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.gui.VariableExperimentalConditionGui;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.Feature;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.Parameter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.VariableExperimentalCondition;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.FileUtils;


import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The {@code FileFormater} class processes interaction data from CSV, TSV, XLS, and XLSX files,
 * formatting it for XML generation. It extracts bait and prey interactions, supports binary
 * and non-binary formatting, and outputs the processed data in the same format as the input.
 *
 * <p>Supported formats: CSV, TSV, XLS, XLSX.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 *     FileFormater fileFormater = new FileFormater(fileReader);
 *     fileFormater.selectFileFormater("data.xlsx", 0, 1, "Sheet1", true);
 * </pre>
 */
@Getter
@Setter
public class FileFormater {
    final FileReader fileReader;

    private static final Logger LOGGER = Logger.getLogger(FileFormater.class.getName());

    @Getter
    private final List<Map<String, String>> participants = new ArrayList<>();
    private Map<String, String> interactionData = new HashMap<>();
    private List<Feature> baitFeatures = new ArrayList<>();
    private List<Feature> preyFeatures = new ArrayList<>();
    private boolean addParameters;
    private boolean addVariableExperimentalConditions;

    FileWriter fileWriter;
    ParametersGui parametersGui;
    VariableExperimentalConditionGui variableExperimentalConditionGui;

    String[] header = Arrays.stream(values())
            .filter(col -> col.initial)
            .map(col -> col.name)
            .toArray(String[]::new);

    private final Map<String, Integer> participantCountMap = new HashMap<>();

    /**
     * Constructs a FileFormater object with a FileReader.
     *
     * @param fileReader The Excel file reader used to read input files.
     */
    public FileFormater(FileReader fileReader) {
        this.fileReader = fileReader;
        this.fileWriter = new FileWriter(fileReader);
        this.parametersGui  = new ParametersGui(fileReader);
        this.variableExperimentalConditionGui = new VariableExperimentalConditionGui(fileReader);
        setFileReader(fileReader);
    }

    /**
     * Selects the appropriate file formatting method based on the file type and processes the file.
     *
     * @param filePath        The path of the file to be formatted.
     * @param baitColumnIndex The column index for the bait.
     * @param preyColumnIndex The column index for the prey.
     * @param baitNameColumnIndex The column index for the bait name.
     * @param preyNameColumnIndex The column index for the prey name.
     * @param sheetSelected   The sheet name (for Excel files).
     * @param binary          Indicates whether the interactions should be formatted in binary mode.
     */
    public void selectFileFormater(String filePath,
                                   int baitColumnIndex,
                                   int preyColumnIndex,
                                   int baitNameColumnIndex,
                                   int preyNameColumnIndex,
                                   String sheetSelected,
                                   boolean binary) {
            File file = new File(filePath);
            String fileType = FileUtils.getFileExtension(filePath);
            String fileName = file.getName();
            String modifiedFileName =  FileUtils.getFileName(filePath) + "_xmlMakerFormatted";
            String newFileName ="";

        switch (fileType) {
            case "xlsx":
                LOGGER.info("Reading xlsx file: " + fileName);
                displayDataPanels();
                formatExcelFile(baitColumnIndex, preyColumnIndex, sheetSelected, binary, baitNameColumnIndex, preyNameColumnIndex);
                Workbook workbookXlsx = new XSSFWorkbook();
                addDataToParticipants();
                fileWriter.writeToExcel(participants, modifiedFileName + ".xlsx", header, workbookXlsx);
                newFileName = modifiedFileName + ".xlsx";
                break;
            case "xls":
                LOGGER.info("Reading xls file: " + fileName);
                displayDataPanels();
                formatExcelFile(baitColumnIndex, preyColumnIndex, sheetSelected, binary, baitNameColumnIndex, preyNameColumnIndex);
                Workbook workbookXls = new HSSFWorkbook();
                addDataToParticipants();
                fileWriter.writeToExcel(participants, modifiedFileName + ".xls", header, workbookXls);
                newFileName = modifiedFileName + ".xls";
                break;
            case "csv":
                LOGGER.info("Reading csv file: " + fileName);
                displayDataPanels();
                formatSeparatedFormatFile(baitColumnIndex, preyColumnIndex, binary, baitNameColumnIndex, preyNameColumnIndex);
                addDataToParticipants();
                fileWriter.writeToFile(participants, modifiedFileName + ".csv", ',', header);
                newFileName = modifiedFileName + ".csv";
                break;
            case "tsv":
                LOGGER.info("Reading tsv file: " + fileName);
                displayDataPanels();
                formatSeparatedFormatFile(baitColumnIndex, preyColumnIndex, binary, baitNameColumnIndex, preyNameColumnIndex);
                addDataToParticipants();
                fileWriter.writeToFile(participants, modifiedFileName + ".tsv", '\t', header);
                newFileName = modifiedFileName + ".tsv";
                break;
            default:
                LOGGER.warning("Unsupported file format: " + fileType);
                showErrorDialog("Unsupported file format! Supported formats: .csv, .tsv, .xls, .xlsx");
        }
        participants.clear();
        showInfoDialog("File modified: " + modifiedFileName);
        fileReader.selectFileOpener(newFileName);
    }

    private void displayDataPanels(){
        getVariableExperimentalConditionsPanel();
        getParametersPanel();
    }

    private void addDataToParticipants(){
        addInteractionType();
        addAllFeatures();
        addInteractionParameters();
        addVariableConditions();
    }

    /**
     * Generic formatter for both Excel and separated files.
     * It reads interaction rows and adds bait and prey participants to the list.
     *
     * @param iterator                 Iterator over input data rows (either Excel rows or list of strings).
     * @param getBait                  Function to extract bait ID from a row.
     * @param getPrey                  Function to extract prey ID from a row.
     * @param getBaitName              Function to extract bait display name.
     * @param getPreyName              Function to extract prey display name.
     * @param binary                   Whether to treat the data as binary (bait-prey pairs) or grouped.
     * @param <T>                      Type of row (e.g., Excel Row or list).
     */
    public <T> void formatFile(Iterator<T> iterator,
                               Function<T, String> getBait,
                               Function<T, String> getPrey,
                               Function<T, String> getBaitName,
                               Function<T, String> getPreyName,
                               boolean binary) {

        int interactionNumber = 0;
        String lastBait = null;
        int rowIndex = 0;

        while (iterator.hasNext()) {
            T row = iterator.next();
            rowIndex++;

            String bait = getBait.apply(row);
            String prey = getPrey.apply(row);

            String baitName = getBaitName.apply(row);
            String preyName = getPreyName.apply(row);

            if (bait.isEmpty() || prey.isEmpty()) {
                continue;
            }

            if (binary) {
                interactionNumber++;
                addNewParticipant(String.valueOf(interactionNumber), bait, baitName, "bait", rowIndex);
                addNewParticipant(String.valueOf(interactionNumber), prey, preyName, "prey", rowIndex);
            } else {
                if (lastBait == null || !lastBait.equals(bait)) {
                    interactionNumber++;
                    lastBait = bait;
                    addNewParticipant(String.valueOf(interactionNumber), bait, baitName, "bait", rowIndex);
                }
                addNewParticipant(String.valueOf(interactionNumber), prey, preyName, "prey", rowIndex);
            }
        }
        iterator.remove();
    }

    /**
     * Formats interaction data from separated files (CSV or TSV).
     *
     * @param baitColumnIndex        Index of the bait identifier column.
     * @param preyColumnIndex        Index of the prey identifier column.
     * @param binary                 Whether the format is binary (each line is a bait-prey pair).
     * @param baitNameColumnIndex    Optional index for bait display names (-1 if not used).
     * @param preyNameColumnIndex    Optional index for prey display names (-1 if not used).
     */
    public void formatSeparatedFormatFile(int baitColumnIndex, int preyColumnIndex,
                                          boolean binary, int baitNameColumnIndex,
                                          int preyNameColumnIndex) {

        Iterator<List<String>> iterator = fileReader.readFileWithSeparator();

        formatFile(iterator,
                row -> row.get(baitColumnIndex),
                row -> row.get(preyColumnIndex),
                row -> baitNameColumnIndex == -1 ? "" : row.get(baitNameColumnIndex),
                row -> preyNameColumnIndex == -1 ? "" : row.get(preyNameColumnIndex),
                binary
        );
    }

    /**
     * Formats interaction data from an Excel sheet.
     *
     * @param baitColumnIndex        Index of the bait identifier column.
     * @param preyColumnIndex        Index of the prey identifier column.
     * @param sheetSelected          Name of the Excel sheet to read from.
     * @param binary                 Whether the format is binary (each line is a bait-prey pair).
     * @param baitNameColumnIndex    Optional index for bait display names (-1 if not used).
     * @param preyNameColumnIndex    Optional index for prey display names (-1 if not used).
     */
    public void formatExcelFile(int baitColumnIndex, int preyColumnIndex,
                                String sheetSelected, boolean binary,
                                int baitNameColumnIndex, int preyNameColumnIndex) {

        Iterator<Row> iterator = fileReader.readWorkbookSheet(sheetSelected);

        formatFile(iterator,
                row -> FileUtils.getCellValueAsString(row.getCell(baitColumnIndex)),
                row -> FileUtils.getCellValueAsString(row.getCell(preyColumnIndex)),
                row -> baitNameColumnIndex == -1 ? "" : FileUtils.getCellValueAsString(row.getCell(baitNameColumnIndex)),
                row -> preyNameColumnIndex == -1 ? "" : FileUtils.getCellValueAsString(row.getCell(preyNameColumnIndex)),
                binary
        );
    }

    /**
     * Adds a new participantId to the formatted data structure.
     *
     * @param interactionNumber The interaction number associated with the participantId.
     * @param participantId       The participant's identifier.
     * @param participantName       The participant name
     * @param experimentalRole   The type of participantId (e.g., "bait" or "prey").
     * @param rowIndex   Participant row index.
     */
    public void addNewParticipant(String interactionNumber,
                                  String participantId,
                                  String participantName,
                                  String experimentalRole,
                                  int rowIndex) {
        Map<String, String> oneParticipant = new HashMap<>();

        participantCountMap.put(interactionNumber, participantCountMap.getOrDefault(interactionNumber, 0) + 1);

        for (InputData field : InputData.values()) {
            if (field.initial) {
                if (field.experimentalRoleDependent) {
                    if ("bait".equalsIgnoreCase(experimentalRole)) {
                        oneParticipant.put(field.name, interactionData.get(field.name + BAIT.name));
                    } else if ("prey".equalsIgnoreCase(experimentalRole)) {
                        oneParticipant.put(field.name, interactionData.get(field.name + PREY.name));
                    }
                } else {
                    oneParticipant.put(field.name, interactionData.get(field.name));
                }
            }
        }

        oneParticipant.put(INTERACTION_NUMBER.name, interactionNumber);
        oneParticipant.put(PARTICIPANT_ID.name, participantId);
        oneParticipant.put(PARTICIPANT_NAME.name, participantName);
        oneParticipant.put(EXPERIMENTAL_ROLE.name, experimentalRole);
        oneParticipant.put(PARTICIPANT_ROW_INDEX.name, Objects.toString(rowIndex));

        participants.add(oneParticipant);
    }

    /**
     * Post-processes the formatted interaction data by adding an "Interaction Type" column.
     * If an interaction involves more than two participants, it is classified as an "association";
     * otherwise, it is classified as a "physical interaction."
     */
    public void addInteractionType() {
        for (Map<String, String> participant : participants) {
            String interactionNumber = participant.get(INTERACTION_NUMBER.name);
            int count = participantCountMap.getOrDefault(interactionNumber, 0);
            if (count > 2) {
                participant.put(INTERACTION_TYPE.name, "association");
            } else {
                participant.put(INTERACTION_TYPE.name, "physical association");
            }
        }
    }

    /**
     * Adds feature-related information to the formatted data.
     * This method includes adding feature columns for each participant (bait or prey) based on available features.
     */
    private void addAllFeatures() {
        int numberOfColumnsToAdd = getNumberOfFeaturesColumns() * numberOfFeatureCells();
        ArrayList<String> featuresHeader = getFeaturesHeader();
        String[] extendedHeader = Arrays.copyOf(header, header.length + numberOfColumnsToAdd);

        for (int i = 0; i < featuresHeader.size(); i++) {
            extendedHeader[header.length + i] = featuresHeader.get(i);
        }

        header = extendedHeader;

        for (Map<String, String> participant : participants) {
            String participantExperimentalRole = participant.get(EXPERIMENTAL_ROLE.name);
            if (participantExperimentalRole.trim().equals("prey")) {
                addOneFeature(participant, preyFeatures);
            } else if (participantExperimentalRole.trim().equals("bait")) {
                addOneFeature(participant, baitFeatures);
            }
        }
    }

    /**
     * Counts the number of feature cells defined in {@link InputData}.
     *
     * @return The total number of feature cells.
     */
    private int numberOfFeatureCells(){
        return (int) Arrays.stream(InputData.values()).filter(data -> !data.initial).count();
    }

    /**
     * Adds feature information to a specific row based on the provided list of features.
     *
     * @param participant The row to which feature information will be added.
     * @param features The list of feature data to be added to the row.
     */
    private void addOneFeature(Map<String, String> participant, List<Feature> features) {
        List<InputData> featureFields = Arrays.stream(InputData.values())
                .filter(field -> !field.initial)
                .collect(Collectors.toList());

        if (features.isEmpty()) {
            for (InputData featureField : featureFields) {
                participant.put(featureField.name(), "");
            }
        } else {
            for (int i = 0; i < features.size(); i++) {
                String adding = "_" + i;
                Feature feature = features.get(i);
                participant.put(FEATURE_SHORT_NAME.name + adding, feature.getShortName());
                participant.put(FEATURE_TYPE.name + adding, feature.getType());
                participant.put(FEATURE_START_LOCATION.name + adding, feature.getStartLocation());
                participant.put(FEATURE_END_LOCATION.name + adding, feature.getEndLocation());
                participant.put(FEATURE_RANGE_TYPE.name + adding, feature.getRangeType());

                participant.put(FEATURE_XREF.name + adding, feature.getListAsString(feature.getXref()));
                participant.put(FEATURE_XREF_DB.name + adding, feature.getListAsString(feature.getXrefDb()));
                participant.put(FEATURE_XREF_QUALIFIER.name + adding, feature.getListAsString(feature.getXrefQualifier()));

                participant.put(FEATURE_PARAM_TYPE.name + adding, feature.getParameterTypes());
                participant.put(FEATURE_PARAM_VALUE.name + adding, feature.getParameterValues());
                participant.put(FEATURE_PARAM_VALUE.name + adding, getValueFromFile((FEATURE_PARAM_VALUE.name + adding), participant));

                participant.put(FEATURE_PARAM_UNIT.name + adding, feature.getParameterUnits());
                participant.put(FEATURE_PARAM_BASE.name + adding, feature.getParameterBases());
                participant.put(FEATURE_PARAM_EXPONENT.name + adding, feature.getParameterExponents());

                participant.put(FEATURE_PARAM_UNCERTAINTY.name + adding, feature.getParameterUncertainties());
                participant.put(FEATURE_PARAM_UNCERTAINTY.name + adding, getValueFromFile((FEATURE_PARAM_UNCERTAINTY.name + adding), participant));

                participant.put(FEATURE_ROLE.name + adding, feature.getRole());

                participant.put(FEATURE_ORIGINAL_SEQUENCE.name + adding,
                        "Original Sequence".equalsIgnoreCase(feature.getOriginalSequence()) ? "" : feature.getOriginalSequence());

                participant.put(FEATURE_NEW_SEQUENCE.name + adding,
                        "New Sequence".equalsIgnoreCase(feature.getNewSequence()) ? "" : feature.getNewSequence());

                if (feature.isFetchFromFile()){
                    participant.put(FEATURE_SHORT_NAME.name + adding, getValueFromFile((FEATURE_SHORT_NAME.name + adding), participant));
                    participant.put(FEATURE_TYPE.name + adding, getValueFromFile((FEATURE_TYPE.name + adding), participant));
                    participant.put(FEATURE_START_LOCATION.name + adding, getValueFromFile((FEATURE_START_LOCATION.name + adding), participant));
                    participant.put(FEATURE_END_LOCATION.name + adding, getValueFromFile((FEATURE_END_LOCATION.name + adding), participant));
                    participant.put(FEATURE_RANGE_TYPE.name + adding, getValueFromFile((FEATURE_RANGE_TYPE.name + adding), participant));
                    participant.put(FEATURE_ORIGINAL_SEQUENCE.name + adding, getValueFromFile((FEATURE_ORIGINAL_SEQUENCE.name + adding), participant));
                    participant.put(FEATURE_NEW_SEQUENCE.name + adding, getValueFromFile((FEATURE_NEW_SEQUENCE.name + adding), participant));
                    participant.put(FEATURE_ROLE.name + adding, getValueFromFile((FEATURE_ROLE.name + adding), participant));

                    participant.put(FEATURE_XREF.name + adding, getValueFromFile((FEATURE_XREF.name + adding), participant));
                    participant.put(FEATURE_XREF_DB.name + adding, getValueFromFile((FEATURE_XREF_DB.name + adding), participant));
                    participant.put(FEATURE_XREF_QUALIFIER.name + adding, getValueFromFile((FEATURE_XREF_QUALIFIER.name + adding), participant));
                }
            }
        }
    }

    /**
     * Retrieves the number of columns needed for features based on the maximum number of bait or prey features.
     *
     * @return The number of columns required for features.
     */
    private int getNumberOfFeaturesColumns() {
        int numberOfBaitFeatures = baitFeatures.size();
        int numberOfPreyFeatures = preyFeatures.size();
        return Math.max(numberOfBaitFeatures, numberOfPreyFeatures);
    }

    /**
     * Retrieves the header names for feature columns.
     *
     * @return A list of feature column headers.
     */
    private ArrayList<String> getFeaturesHeader(){
        ArrayList<String> header = new ArrayList<>();
        for (int i = 0; i < getNumberOfFeaturesColumns(); i++) {
            for (InputData field : InputData.values()) {
                if (!field.initial){
                    header.add(field.name + "_" + i);
                }
            }
        }
        return header;
    }

    /**
     * Displays a dialog box for the user to enter interaction parameter definitions.
     * This is triggered if the `addParameters` flag is true.
     */
    private void getParametersPanel() {
        if (addParameters) {
            JPanel parametersPanel = parametersGui.parametersContainer();
            JOptionPane.showConfirmDialog(null, parametersPanel,"Add parameters", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        }
    }

    /**
     * Displays a dialog box for the user to enter interaction variable experimental conditions definitions.
     * This is triggered if the `addVariableExperimentalConditions` flag is true.
     */
    private void getVariableExperimentalConditionsPanel() {
        if (addVariableExperimentalConditions) {
            JPanel variableExperimentalConditionsPanel = variableExperimentalConditionGui.getVariableExperimentalConditionPanel();
            JOptionPane.showConfirmDialog(null, variableExperimentalConditionsPanel,"Add experimental variable conditions", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        }
    }

    /**
     * Adds parameter-related metadata (unit, base, exponent, type, uncertainty)
     * to each participant in the interaction dataset.
     * <p>
     * Values are extracted directly from the participant map using column names
     * defined in {@link Parameter} objects loaded via the GUI.
     * </p>
     */
    private void addInteractionParameters() {
        if (parametersGui == null || parametersGui.getParameters().isEmpty()) {
            LOGGER.severe("Parameters GUI or parameters list is null");
            return;
        }

        if (participants.isEmpty()) {
            LOGGER.severe("Participants list is null or empty");
            return;
        }

        List<Parameter> parameters = parametersGui.getParameters();

        for (Map<String, String> participant : participants) {
            if (participant == null) continue;

            for (Parameter parameter : parameters) {
                if (parameter == null) continue;

                checkAndAddToCurrentString(INTERACTION_PARAM_VALUE.name, parameter.getValueColumn(), participant);
                checkAndAddToCurrentString(INTERACTION_PARAM_UNIT.name, parameter.getUnit(), participant);
                checkAndAddToCurrentString(INTERACTION_PARAM_EXPONENT.name, parameter.getExponent(), participant);
                checkAndAddToCurrentString(INTERACTION_PARAM_BASE.name, parameter.getBase(), participant);
                checkAndAddToCurrentString(INTERACTION_PARAM_TYPE.name, parameter.getType(), participant);
                checkAndAddToCurrentString(INTERACTION_PARAM_UNCERTAINTY.name, parameter.getUncertaintyColumn(), participant);
            }
            participant.put(INTERACTION_PARAM_VALUE.name, getValueFromFile(INTERACTION_PARAM_VALUE.name, participant));
            participant.put(INTERACTION_PARAM_UNCERTAINTY.name, getValueFromFile(INTERACTION_PARAM_UNCERTAINTY.name, participant));
        }
    }

    /**
     * Adds variableExperimentalCondition-related metadata
     * to each participant in the interaction dataset.
     */
    private void addVariableConditions(){
        if (variableExperimentalConditionGui == null || variableExperimentalConditionGui.getExperimentalConditions().isEmpty()) {
            LOGGER.severe("Parameters GUI or parameters list is null");
            return;
        }
        if (participants.isEmpty()) {
            LOGGER.severe("Participants list is null or empty");
            return;
        }

        List<VariableExperimentalCondition> variableExperimentalConditions = variableExperimentalConditionGui.getExperimentalConditions();

        for (Map<String, String> participant : participants) {
            for  (VariableExperimentalCondition variableExperimentalCondition : variableExperimentalConditions) {
                checkAndAddToCurrentString(EXPERIMENTAL_VARIABLE_CONDITION_DESCRIPTION.name, variableExperimentalCondition.getDescription(), participant);
                checkAndAddToCurrentString(EXPERIMENTAL_VARIABLE_CONDITION_VALUE.name,  variableExperimentalCondition.getValueColumn(), participant);
                checkAndAddToCurrentString(EXPERIMENTAL_VARIABLE_CONDITION_UNIT.name,  variableExperimentalCondition.getUnit(), participant);
            }
            participant.put(EXPERIMENTAL_VARIABLE_CONDITION_VALUE.name, getValueFromFile(EXPERIMENTAL_VARIABLE_CONDITION_VALUE.name, participant));
        }

    }

    /**
     * Returns a sanitized string; replaces null or empty values with an empty string,
     * otherwise appends a semicolon.
     *
     * @param value The input string to sanitize.
     * @return A cleaned string ending with a semicolon or an empty string.
     */
    private String safe(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return "";
        }
        return value + ";";
    }

    /**
     * Adds a value to the participant map under the given key, merging if the key already exists.
     *
     * @param key         The key to add or update in the map.
     * @param value       The value to add.
     * @param participant The map to update.
     */
    private void checkAndAddToCurrentString(String key, String value, Map<String, String> participant) {
        if (value == null || value.trim().equals(";") || value.trim().isEmpty()) return;

        if (!participant.containsKey(key)) {
            participant.put(key, value);
        } else {
            participant.merge(key, value, (previous, current) -> safe(previous) + safe(current));
        }
    }
}
