package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import com.opencsv.CSVWriter;

import lombok.Getter;
import lombok.Setter;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.DataForRawFile;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.Parameter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.FileUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml.DataTypeAndColumn.*;

/**
 * The {@code FileFormater} class processes interaction data from CSV, TSV, XLS, and XLSX files,
 * formatting it for XML generation. It extracts bait and prey interactions, supports binary
 * and non-binary formatting, and outputs the processed data in the same format as the input.
 *
 * <p>Supported formats: CSV, TSV, XLS, XLSX.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 *     FileFormater fileFormater = new FileFormater(excelFileReader);
 *     fileFormater.selectFileFormater("data.xlsx", 0, 1, "Sheet1", true);
 * </pre>
 */

public class FileFormater {
    final ExcelFileReader excelFileReader;

    private static final Logger LOGGER = Logger.getLogger(FileFormater.class.getName());

    @Getter
    private final List<Map<String, String>> participants = new ArrayList<>();
    @Setter
    private Map<String, String> interactionData = new HashMap<>();
    @Setter
    private List<Feature> baitFeatures = new ArrayList<>();
    @Setter
    private List<Feature> preyFeatures = new ArrayList<>();
    @Setter
    private boolean addParameters;

    ParametersGui parametersGui;

    String[] header = Arrays.stream(values())
            .filter(col -> col.initial)
            .map(col -> col.name)
            .toArray(String[]::new);

    private final Map<String, Integer> participantCountMap = new HashMap<>();

    /**
     * Constructs a FileFormater object with an ExcelFileReader.
     *
     * @param excelFileReader The Excel file reader used to read input files.
     */
    public FileFormater(ExcelFileReader excelFileReader) {
        this.excelFileReader = excelFileReader;
        this.parametersGui  = new ParametersGui(excelFileReader);
    }

    /**
     * Selects the appropriate file formatting method based on the file type and processes the file.
     *
     * @param filePath        The path of the file to be formatted.
     * @param baitColumnIndex The column index for the bait.
     * @param preyColumnIndex The column index for the prey.
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
                getParametersPanel();
                formatExcelFile(baitColumnIndex, preyColumnIndex, sheetSelected, binary, baitNameColumnIndex, preyNameColumnIndex);
                Workbook workbookXlsx = new XSSFWorkbook();
                addInteractionType();
                addAllFeatures();
                addInteractionParameters(sheetSelected);
                writeToExcel(participants, modifiedFileName + ".xlsx", header, workbookXlsx);
                newFileName = modifiedFileName + ".xlsx";
                break;
            case "xls":
                LOGGER.info("Reading xls file: " + fileName);
                getParametersPanel();
                formatExcelFile(baitColumnIndex, preyColumnIndex, sheetSelected, binary, baitNameColumnIndex, preyNameColumnIndex);
                Workbook workbookXls = new HSSFWorkbook();
                addInteractionType();
                addAllFeatures();
                addInteractionParameters(sheetSelected);
                writeToExcel(participants, modifiedFileName + ".xls", header, workbookXls);
                newFileName = modifiedFileName + ".xls";
                break;
            case "csv":
                LOGGER.info("Reading csv file: " + fileName);
                getParametersPanel();
                formatSeparatedFormatFile(baitColumnIndex, preyColumnIndex, binary, baitNameColumnIndex, preyNameColumnIndex);
                addInteractionType();
                addAllFeatures();
                addInteractionParameters(sheetSelected);
                writeToFile(participants, modifiedFileName + ".csv", ',', header);
                newFileName = modifiedFileName + ".csv";
                break;
            case "tsv":
                LOGGER.info("Reading tsv file: " + fileName);
                formatSeparatedFormatFile(baitColumnIndex, preyColumnIndex, binary, baitNameColumnIndex, preyNameColumnIndex);
                getParametersPanel();
                addInteractionType();
                addAllFeatures();
                writeToFile(participants, modifiedFileName + ".tsv", '\t', header);
                newFileName = modifiedFileName + ".tsv";
                break;
            default:
                LOGGER.warning("Unsupported file format: " + fileType);
                XmlMakerUtils.showErrorDialog("Unsupported file format! Supported formats: .csv, .tsv, .xls, .xlsx");
        }
        participants.clear();
        XmlMakerUtils.showInfoDialog("File modified: " + modifiedFileName);
        excelFileReader.selectFileOpener(newFileName);
    }

    public <T> void formatFile(Iterator<T> iterator,
                               Function<T, String> getBait,
                               Function<T, String> getPrey,
                               Function<T, String> getBaitName,
                               Function<T, String> getPreyName,
                               Function<T, String> getParamValue,
                               Function<T, String> getParamUncertainty,
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

            String interactionParamValue = getParamValue.apply(row);
            String interactionParamUncertainty = getParamUncertainty.apply(row);

            if (binary) {
                interactionNumber++;
                addNewParticipant(String.valueOf(interactionNumber), bait, baitName, "bait", interactionParamValue, interactionParamUncertainty, rowIndex);
                addNewParticipant(String.valueOf(interactionNumber), prey, preyName, "prey", interactionParamValue, interactionParamUncertainty, rowIndex);
            } else {
                if (lastBait == null || !lastBait.equals(bait)) {
                    interactionNumber++;
                    lastBait = bait;
                    addNewParticipant(String.valueOf(interactionNumber), bait, baitName, "bait", interactionParamValue, interactionParamUncertainty, rowIndex);
                }
                addNewParticipant(String.valueOf(interactionNumber), prey, preyName, "prey", interactionParamValue, interactionParamUncertainty, rowIndex);
            }
        }

        try {
            iterator.remove();
        } catch (UnsupportedOperationException ignored) {
        }
    }

    public void formatSeparatedFormatFile(int baitColumnIndex, int preyColumnIndex,
                                          boolean binary, int baitNameColumnIndex,
                                          int preyNameColumnIndex) {

        Iterator<List<String>> iterator = excelFileReader.readFileWithSeparator();
        Map<String, Object> indexes = getIndexFromHeader(excelFileReader.getColumns(excelFileReader.sheetSelectedUpdate));

        List<Integer> paramValueIdx = (List<Integer>) indexes.get(INTERACTION_PARAM_VALUE.name);
        List<Integer> paramUncertaintyIdx = (List<Integer>) indexes.get(INTERACTION_PARAM_UNCERTAINTY.name);

        formatFile(iterator,
                row -> row.get(baitColumnIndex),
                row -> row.get(preyColumnIndex),
                row -> baitNameColumnIndex == -1 ? "" : row.get(baitNameColumnIndex),
                row -> preyNameColumnIndex == -1 ? "" : row.get(preyNameColumnIndex),
                row -> { StringBuilder paramValues = new StringBuilder();
                    for (int index : paramValueIdx) {
                        String currentValue = row.get(index) + ";";
                        paramValues.append(currentValue);
                    }
                    return paramValues.toString();
                },
                row -> { StringBuilder paramUncertainties = new StringBuilder();
                    for (int index : paramUncertaintyIdx) {
                        String currentValue = row.get(index) + ";";
                        paramUncertainties.append(currentValue);
                    }
                    return paramUncertainties.toString();
                },
                binary
        );
    }

    public void formatExcelFile(int baitColumnIndex, int preyColumnIndex,
                                String sheetSelected, boolean binary,
                                int baitNameColumnIndex, int preyNameColumnIndex) {

        Iterator<Row> iterator = excelFileReader.readWorkbookSheet(sheetSelected);
        Map<String, Object> indexes = getIndexFromHeader(excelFileReader.getColumns(sheetSelected));

        List<Integer> paramValueIdx = (List<Integer>) indexes.get(INTERACTION_PARAM_VALUE.name);
        List<Integer> paramUncertaintyIdx = (List<Integer>) indexes.get(INTERACTION_PARAM_UNCERTAINTY.name);

        formatFile(iterator,
                row -> FileUtils.getCellValueAsString(row.getCell(baitColumnIndex)),
                row -> FileUtils.getCellValueAsString(row.getCell(preyColumnIndex)),
                row -> baitNameColumnIndex == -1 ? "" : FileUtils.getCellValueAsString(row.getCell(baitNameColumnIndex)),
                row -> preyNameColumnIndex == -1 ? "" : FileUtils.getCellValueAsString(row.getCell(preyNameColumnIndex)),
                row -> { StringBuilder paramValues = new StringBuilder();
                                if (paramValueIdx != null) {
                                    for (int index : paramValueIdx) {
                                        String currentValue = FileUtils.getCellValueAsString(row.getCell(index)) + ";";
                                        paramValues.append(currentValue);
                                    }
                                    return paramValues.toString();
                                }
                                else return "";
                            },
                row -> { StringBuilder paramUncertainties = new StringBuilder();
                                if (paramUncertaintyIdx != null) {
                                    for (int index : paramUncertaintyIdx) {
                                        String currentValue = FileUtils.getCellValueAsString(row.getCell(index)) + ";";
                                        paramUncertainties.append(currentValue);
                                    }
                                    return paramUncertainties.toString();
                                }
                                else return "";
                            },
                binary
        );
    }

    /**
     * Adds a new participantId to the formatted data structure.
     *
     * @param interactionNumber The interaction number associated with the participantId.
     * @param participantId       The participantId's identifier.
     * @param experimentalRole   The type of participantId (e.g., "bait" or "prey").
     */
    public void addNewParticipant(String interactionNumber,
                                  String participantId,
                                  String participantName,
                                  String experimentalRole,
                                  String interactionParamValue,
                                  String interactionParamUncertainty,
                                  int rowIndex) {
        Map<String, String> oneParticipant = new HashMap<>();
        oneParticipant.put(INTERACTION_NUMBER.name, interactionNumber);
        oneParticipant.put(PARTICIPANT_ID.name, participantId);
        oneParticipant.put(PARTICIPANT_NAME.name, participantName);
        oneParticipant.put(EXPERIMENTAL_ROLE.name, experimentalRole);
        oneParticipant.put(INTERACTION_PARAM_VALUE.name, interactionParamValue);
        oneParticipant.put(INTERACTION_PARAM_UNCERTAINTY.name, interactionParamUncertainty);
        oneParticipant.put(PARTICIPANT_ROW_INDEX.name, Objects.toString(rowIndex));

        participantCountMap.put(interactionNumber, participantCountMap.getOrDefault(interactionNumber, 0) + 1);

        for (DataForRawFile field : DataForRawFile.values()) {
            if (field.isCommon && !field.isFeature) {
                oneParticipant.put(field.name, interactionData.get(field.name + field.isBait));
            } else {
                if ("bait".equalsIgnoreCase(experimentalRole) && field.isBait && !field.isFeature) {
                    oneParticipant.put(field.name, interactionData.get(field.name + true));
                } else if ("prey".equalsIgnoreCase(experimentalRole) && !field.isBait && !field.isFeature) {
                    oneParticipant.put(field.name, interactionData.get(field.name + false));
                }
            }
        }

        participants.add(oneParticipant);
    }

    public void writeToFile(List<Map<String, String>> data, String filePath, char delimiter, String[] header) {
        try (CSVWriter writer = new CSVWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8),
                delimiter,
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {

            writer.writeNext(header);

            for (Map<String, String> row : data) {
                String[] line = Arrays.stream(header)
                        .map(h -> row.getOrDefault(h, ""))
                        .toArray(String[]::new);
                writer.writeNext(line);
            }

        } catch (IOException e) {
            LOGGER.warning("Error writing to file: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void writeToExcel(List<Map<String, String>> data, String filePath, String[] header, Workbook workbook) {
        Sheet sheet = workbook.createSheet("Formatted data");

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < header.length; i++) {
            headerRow.createCell(i).setCellValue(header[i]);
        }

        for (int i = 0; i < data.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Map<String, String> rowData = data.get(i);
            for (int j = 0; j < header.length; j++) {
                String value = rowData.getOrDefault(header[j], "");
                row.createCell(j).setCellValue(value);
            }
        }

        try (workbook; FileOutputStream fos = new FileOutputStream(filePath)) {
            try {
                workbook.write(fos);
            } catch (IOException e) {
                LOGGER.warning("Error writing Excel file: " + e.getMessage());
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            LOGGER.warning("Error closing workbook: " + e.getMessage());
        }
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

    private int numberOfFeatureCells(){
        int total = 0;
        for (DataForRawFile data : DataForRawFile.values()) {
            if (data.isFeature) {
                total++;
            }
        }
        return total;
    }

    /**
     * Adds feature information to a specific row based on the provided list of features.
     *
     * @param participant The row to which feature information will be added.
     * @param features The list of feature data to be added to the row.
     */
    private void addOneFeature(Map<String, String> participant, List<Feature> features) {
        List<DataForRawFile> featureFields = Arrays.stream(DataForRawFile.values())
                .filter(DataForRawFile::isFeature)
                .collect(Collectors.toList());

        if (features.isEmpty()) {
            for (DataForRawFile featureField : featureFields) {
                participant.put(featureField.name(), "");
            }
        } else {
            for (int i = 0; i < features.size(); i++) {
                String adding = "_" + i;
                Feature feature = features.get(i);
                participant.put(DataForRawFile.FEATURE_SHORT_NAME.name + adding, feature.getShortName());
                participant.put(DataForRawFile.FEATURE_TYPE.name + adding, feature.getType());
                participant.put(DataForRawFile.FEATURE_START_LOCATION.name + adding, feature.getStartLocation());
                participant.put(DataForRawFile.FEATURE_END_LOCATION.name + adding, feature.getEndLocation());
                participant.put(DataForRawFile.FEATURE_XREF.name + adding, feature.getListAsString(feature.getXref()));
                participant.put(DataForRawFile.FEATURE_XREF_DB.name + adding, feature.getListAsString(feature.getXrefDb()));
                participant.put(DataForRawFile.FEATURE_XREF_QUALIFIER.name + adding, feature.getListAsString(feature.getXrefQualifier()));

                participant.put(DataForRawFile.FEATURE_PARAM_TYPE.name + adding, feature.getParameterTypes());
                participant.put(DataForRawFile.FEATURE_PARAM_VALUE.name + adding, feature.getParameterValues());
                participant.put(DataForRawFile.FEATURE_PARAM_VALUE.name + adding, getValueFromFeatureParameter((DataForRawFile.FEATURE_PARAM_VALUE.name + adding), participant));
                participant.put(DataForRawFile.FEATURE_PARAM_UNIT.name + adding, feature.getParameterUnits());
                participant.put(DataForRawFile.FEATURE_PARAM_BASE.name + adding, feature.getParameterBases());
                participant.put(DataForRawFile.FEATURE_PARAM_EXPONENT.name + adding, feature.getParameterExponents());
                participant.put(DataForRawFile.FEATURE_PARAM_UNCERTAINTY.name + adding, feature.getParameterUncertainties());
                participant.put(DataForRawFile.FEATURE_PARAM_UNCERTAINTY.name + adding, getValueFromFeatureParameter((DataForRawFile.FEATURE_PARAM_UNCERTAINTY.name + adding), participant));
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
            for (DataForRawFile field : DataForRawFile.values()) {
                if (field.isFeature){
                    header.add(field.name + "_" + i);
                }
            }
        }
        return header;
    }

    private void getParametersPanel() {
        if (addParameters) {
            JPanel parametersPanel = parametersGui.parametersContainer();
            JOptionPane.showConfirmDialog(null, parametersPanel,"Add parameters", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        }
    }

    private void addInteractionParameters(String sheetSelected) {
        if (parametersGui == null || parametersGui.parameters == null) {
            LOGGER.severe("Parameters GUI or parameters list is null");
            return;
        }

        List<Parameter> parameters = parametersGui.parameters;

        if (excelFileReader == null || excelFileReader.workbook == null) {
            LOGGER.severe("Participants or Excel file reader is not initialized");
            return;
        }

        Sheet sheet = excelFileReader.workbook.getSheet(sheetSelected);
        if (sheet == null) {
            LOGGER.severe("Sheet not found: " + sheetSelected);
            return;
        }

        for (Map<String, String> participant : participants) {
            if (participant == null) continue;
            for (Parameter parameter : parameters) {
                if (parameter == null) continue;
                checkAndAddToCurrentString(INTERACTION_PARAM_UNIT.name, safe(parameter.getUnit()), participant);
                checkAndAddToCurrentString(INTERACTION_PARAM_EXPONENT.name, safe(parameter.getExponent()), participant);
                checkAndAddToCurrentString(INTERACTION_PARAM_BASE.name, safe(parameter.getBase()), participant);
                checkAndAddToCurrentString(INTERACTION_PARAM_TYPE.name, safe(parameter.getType()), participant);
            }
        }
    }

    private String safe(String value) {
        return value == null ? " ; " : value + ";";
    }

    private void checkAndAddToCurrentString(String key, String value, Map<String, String> participant) {
        if (participant.get(key) == null) {
            participant.put(key, value);
        } else {
            String previousValue = participant.get(key);
            String newValue = previousValue + value;
            participant.put(key, newValue);
        }

    }

    private Map<String, Object> getIndexFromHeader(List<String> headerRow) {
        Map<String, Object> indexMap = new HashMap<>();

        List<Integer> paramValueIndex = new ArrayList<>();
        List<Integer> paramUncertaintyIndex = new ArrayList<>();

        for (int i = 0; i < headerRow.size(); i++) {
            String cellValue = headerRow.get(i);
            for (Parameter parameter : parametersGui.parameters){
                if (cellValue != null) {
                    if (cellValue.equals(parameter.getValueColumn())){
                        paramValueIndex.add(i);
                        indexMap.put(INTERACTION_PARAM_VALUE.name, paramValueIndex);
                    } else if (cellValue.equals(parameter.getUncertaintyColumn())){
                        paramUncertaintyIndex.add(i);
                        indexMap.put(INTERACTION_PARAM_UNCERTAINTY.name, paramUncertaintyIndex);
                    }
                }
            }
        }
        return indexMap;
    }

    private String getValueFromFeatureParameter(String getFromParticipant, Map<String, String> participant) {
        StringBuilder value = new StringBuilder();
        String featureValueColumns = participant.get(getFromParticipant);
        if (featureValueColumns != null) {
            String[] valuesColumns = featureValueColumns.split(";");
            for (String valueColumn : valuesColumns) {
                value.append(getDataFromRow(getColumnIndex(valueColumn), Integer.parseInt(participant.get(PARTICIPANT_ROW_INDEX.name)))).append(";");
            }
        }
        return value.toString();
    }

    private int getColumnIndex(String columnName){
        List<String> columns = excelFileReader.getColumns(excelFileReader.sheetSelectedUpdate);
        return columns.indexOf(columnName);
    }

    private String getDataFromRow(int columnIndex, int rowIndex) {
        Sheet sheet = excelFileReader.workbook.getSheet(excelFileReader.sheetSelectedUpdate);
        Row row = sheet.getRow(rowIndex);
        if (row == null || columnIndex < 0) {
            return "";
        }
        Cell cell = row.getCell(columnIndex);
        if (cell != null) {
            return FileUtils.getCellValueAsString(cell);
        } else {
            return "";
        }
    }
}
