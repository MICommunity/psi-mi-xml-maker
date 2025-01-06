package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import uk.ac.ebi.intact.psi.mi.xmlmaker.XmlMakerUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.MoleculeSetChecker;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapper;

import javax.swing.*;
import java.awt.Font;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The {@code ExcelFileReader} class provides functionality for reading, processing,
 * and modifying Excel (XLS, XLSX) and delimited text files (CSV, TSV).
 * It integrates with UniprotMapper and MoleculeSetChecker to validate and update data.
 */
public class ExcelFileReader {

    private static final Logger LOGGER = Logger.getLogger(ExcelFileReader.class.getName());
    public final int CHUNK_SIZE = 100; // Default chunk size for processing large files

    private final MoleculeSetChecker moleculeSetChecker = new MoleculeSetChecker();
    private final DataFormatter formatter = new DataFormatter();
    private final UniprotMapper uniprotMapper = new UniprotMapper();
    private final XmlMakerUtils xmlMakerutils = new XmlMakerUtils();

    public Workbook workbook;
    public List<String> fileData;

    @Getter
    @Setter
    public String publicationId;
    public String currentFilePath;
    private String fileName;
    private String fileType;
    private char separator;
    public String outputDirName;

    private final JLabel currentFileLabel;
    public final List<String> sheets = new ArrayList<>();
    private final List<String> columns = new ArrayList<>();
    public final List<String> proteinsPartOfMoleculeSet = new ArrayList<>();
    private int lastChunkIndex = 0;

    /**
     * Default constructor initializes the reader with no file selected.
     */
    public ExcelFileReader() {
        this.fileName = null;
        this.currentFileLabel = new JLabel("No file selected");
    }

    // FILE READING

    /**
     * Opens the specified file and reads its contents based on file type.
     *
     * @param filePath The path to the file to be read.
     */
    public void selectFileOpener(String filePath) {
        fileName = new File(filePath).getName();
        fileType = getFileExtension(filePath);
        currentFileLabel.setText(getFileName());
        currentFilePath = filePath;

        try {
            switch (fileType) {
                case "xlsx":
                    LOGGER.info("Reading xlsx file: " + fileName);
                    readXlsxFile(filePath);
                    break;
                case "xls":
                    LOGGER.info("Reading xls file: " + fileName);
                    readXlsFile(filePath);
                    break;
                case "csv":
                    LOGGER.info("Reading csv file: " + fileName);
                    separator = ',';
                    readFileWithSeparator();
                    break;
                case "tsv":
                    LOGGER.info("Reading tsv file: " + fileName);
                    separator = '\t';
                    readFileWithSeparator();
                    break;
                default:
                    LOGGER.warning("Unsupported file format: " + fileType);
                    xmlMakerutils.showErrorDialog("Unsupported file format! Supported formats: .csv, .tsv, .xls, .xlsx");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading file: " + fileName, e);
            xmlMakerutils.showErrorDialog("Unable to read file: " + e.getMessage());
        }
    }

    /**
     * Reads an XLS file using Apache POI.
     *
     * @param filePath The path to the XLS file.
     * @throws IOException If the file cannot be read.
     */
    public void readXlsFile(String filePath) throws IOException {
        try (POIFSFileSystem poifsFileSystem = new POIFSFileSystem(new File(filePath))) {
            workbook = new HSSFWorkbook(poifsFileSystem);
            getSheets();
        }
    }

    /**
     * Reads an XLSX file using Apache POI.
     *
     * @param filePath The path to the XLSX file.
     * @throws IOException If the file cannot be read.
     */
    public void readXlsxFile(String filePath) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            workbook = new XSSFWorkbook(fileInputStream);
            getSheets();
        }
    }

    /**
     * Reads a specific chunk of rows from a CSV or TSV file and stores its contents.
     *
     */
    public void readFileWithSeparator() {
        List<String> header = null;

        try (InputStream fileStream = new FileInputStream(new File(currentFilePath));
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileStream));
             CSVReader csvReader = new CSVReaderBuilder(bufferedReader)
                     .withCSVParser(new com.opencsv.CSVParserBuilder()
                             .withSeparator(separator)
                             .withIgnoreQuotations(false)
                             .build())
                     .build()) {

            List<List<String>> chunk = new ArrayList<>();
            String[] nextLine;
            int rowIndex = 0;

            if ((nextLine = csvReader.readNext()) != null) {
                header = Arrays.stream(nextLine)
                        .map(cell -> cell == null ? "" : cell.trim())
                        .collect(Collectors.toList());
            }

            while ((nextLine = csvReader.readNext()) != null) {
                chunk.add(Arrays.stream(nextLine)
                        .map(cell -> cell == null ? "" : cell.trim())
                        .collect(Collectors.toList()));
                rowIndex++;

                if (rowIndex % CHUNK_SIZE == 0) {
                    createSubFilesWithSeparator(chunk);
                    chunk.clear();
                }
            }

            if (!chunk.isEmpty()) {
                createSubFilesWithSeparator(chunk);
            }

        } catch (IOException | CsvValidationException e) {
            LOGGER.log(Level.SEVERE, "Unable to read file with separator", e);
            xmlMakerutils.showErrorDialog("Error reading file: " + e.getMessage());
        }

        if (header != null) {
            fileData = header;
        }
    }

    // GUI

    /**
     * Gets the list of sheet names in the current workbook.
     */
    public void getSheets() {
        if (workbook != null) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheets.add(workbook.getSheetName(i));
            }
        }
    }

    /**
     * Gets the column headers from the selected sheet or file.
     *
     * @param sheetSelected The name of the sheet or file.
     * @return A list of column names.
     */
    public List<String> getColumns(String sheetSelected) {
        columns.clear();
        if (fileType.equals("xlsx") || fileType.equals("xls")) {
            if (workbook == null) {
                LOGGER.warning("No file data loaded.");
                return columns;
            }
            Sheet sheet = workbook.getSheet(sheetSelected);
            if (sheet != null) {
                Row headerRow = sheet.getRow(0);
                if (headerRow != null) {
                    for (Cell cell : headerRow) {
                        columns.add(formatter.formatCellValue(cell));
                    }
                }
            }
        } else if (fileType.equals("csv") || fileType.equals("tsv")) {
            if (fileData == null || fileData.isEmpty()) {
                LOGGER.warning("No file data loaded.");
                return columns;
            }
            columns.addAll(fileData);
        }

        return columns;
    }

    /**
     * Returns the file label for the GUI.
     *
     * @return A JLabel with the file name or a default message.
     */
    public JLabel getFileLabel() {
        currentFileLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        currentFileLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return currentFileLabel;
    }

    /**
     * Gets the current file name.
     *
     * @return The name of the currently opened file, or a default message.
     */
    public String getFileName() {
        return fileName != null ? "Current file: " + fileName : "No file selected";
    }

    /**
     * Extracts the file extension from a file name.
     *
     * @param fileName The file name.
     * @return The file extension.
     */
    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    //MODIFY AND WRITE FILES

    /**
     * Checks the selected Excel sheet and inserts Uniprot results into the specified column.
     * If the column or sheet is not found, displays an error dialog.
     *
     * @param sheetSelected      The name of the selected sheet.
     * @param selectedColumn     The column name to find and update.
     * @param organismColumnIndex The index of the organism column.
     * @param idDbColumnIndex    The index of the database ID column.
     */
    public void checkAndInsertUniprotResultsExcel(String sheetSelected, String selectedColumn,
                                                  int organismColumnIndex, int idDbColumnIndex) {
        Sheet sheet = workbook.getSheet(sheetSelected);
        if (sheet == null) {
            xmlMakerutils.showErrorDialog("Sheet not found");
            LOGGER.warning("Sheet not found: " + sheetSelected);
            return;
        }
        int idColumnIndex = findColumnIndex(sheet, selectedColumn);
        if (idColumnIndex != -1) {
            insertColumnWithUniprotResults(sheet, idColumnIndex, organismColumnIndex, idDbColumnIndex);
        } else {
            xmlMakerutils.showErrorDialog("Column not found");
            LOGGER.warning("Column not found at" + idColumnIndex);
        }
        writeWorkbookToFile();
    }
    /**
     * Processes a file with a separated format to insert Uniprot results into the specified column.
     * Updates the ID column with Uniprot results and highlights proteins part of the molecule set.
     *
     * @param idColumnName       The name of the column containing IDs.
     * @param organismColumnIndex The index of the organism column.
     * @param idDbColumnIndex    The index of the database ID column.
     */
    public void checkAndInsertUniprotResultsFileSeparatedFormat(String idColumnName, int organismColumnIndex,
                                                                int idDbColumnIndex) {
        try {
            List<List<String>> data = readSubFile(currentFilePath);
            if (data.isEmpty()) {
                LOGGER.warning("No data to process in the file.");
                xmlMakerutils.showErrorDialog("The file contains no data to process.");
                return;
            }
            int idColumnIndex = -1;
            for (int i = 0; i < fileData.size(); i++) {
                if (fileData.get(i).equals(idColumnName)) {
                    idColumnIndex = i;
                    break;
                }
            }

            if (idColumnIndex == -1) {
                LOGGER.warning("Specified ID column not found in the header: " + idColumnName);
                xmlMakerutils.showErrorDialog("Specified ID column not found in the file.");
                return;
            }

            for (int rowIndex = 1; rowIndex < data.size(); rowIndex++) { // Skip header row
                List<String> row = data.get(rowIndex);
                String idValue = row.get(idColumnIndex).trim();
                String organism = row.get(organismColumnIndex).trim();
                String idDb = row.get(idDbColumnIndex).trim();

                if (!idValue.isEmpty()) {
                    String uniprotResult = uniprotMapper.fetchUniprotResults(idValue, organism, idDb);
                    if (uniprotResult != null && !uniprotResult.isEmpty()) {
                        row.set(idColumnIndex, uniprotResult); // Update ID column with Uniprot result
                        if (moleculeSetChecker.isProteinPartOfMoleculeSet(uniprotResult)) {
                            proteinsPartOfMoleculeSet.add(uniprotResult);
                        }
                    }
                }
            }
            writeFileWithSeparator(data);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing file: " + currentFilePath, e);
            xmlMakerutils.showErrorDialog("Error processing file: " + e.getMessage());
        }
        uniprotMapper.clearAlreadyParsed();
    }
    /**
     * Writes the current workbook to a file. If an error occurs, displays an error dialog.
     */
    private void writeWorkbookToFile() {
        try {
            File inputFile = Paths.get(currentFilePath).toFile();
            try (FileOutputStream fileOut = new FileOutputStream(inputFile)) {
                workbook.write(fileOut);
                xmlMakerutils.showInfoDialog("File modified successfully.");
                LOGGER.log(Level.INFO, "File modified successfully.");
                selectFileOpener(currentFilePath);
            }
        } catch (Exception e) {
            xmlMakerutils.showErrorDialog("Error writing Excel file");
            LOGGER.log(Level.SEVERE, "Error writing Excel file", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Inserts a new column with Uniprot results into the given Excel sheet.
     * Highlights cells if the result is part of the molecule set.
     *
     * @param sheet              The Excel sheet to process.
     * @param idColumnIndex      The index of the ID column.
     * @param organismColumnIndex The index of the organism column.
     * @param idDbColumnIndex    The index of the database ID column.
     */
    public void insertColumnWithUniprotResults(Sheet sheet, int idColumnIndex,
                                               int organismColumnIndex, int idDbColumnIndex) {
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }

            Cell previousCell = row.getCell(idColumnIndex);
            Cell previousDbCell = row.getCell(idDbColumnIndex);

            if (previousCell == null) {
                previousCell = row.createCell(idColumnIndex);
            }
            if (previousDbCell == null) {
                previousDbCell = row.createCell(idDbColumnIndex);
            }

            String organismValue = row.getCell(organismColumnIndex) != null ?
                    row.getCell(organismColumnIndex).getStringCellValue() : "";
            int organismId = Integer.parseInt(xmlMakerutils.fetchTaxIdForOrganism(organismValue));
            String organism = String.valueOf(organismId);
            String idDb = previousDbCell.getStringCellValue();

            String geneValue = formatter.formatCellValue(previousCell);
            String uniprotResult = "";

            if (rowIndex == 0) {
                uniprotResult = "Updated " + geneValue; // geneValue == header cell
            } else if (!geneValue.isEmpty()) {
                uniprotResult = uniprotMapper.fetchUniprotResults(geneValue, organism, idDb);
            }

            if (uniprotResult != null && !uniprotResult.isEmpty()) {
                previousCell.setCellValue(uniprotResult);
                if (moleculeSetChecker.isProteinPartOfMoleculeSet(uniprotResult)) {
                    highlightCells(previousCell);
                    proteinsPartOfMoleculeSet.add(uniprotResult);
                }
                previousDbCell.setCellValue("UniprotKB");
            } else {
                previousCell.setCellValue(geneValue);
            }
        }
        uniprotMapper.clearAlreadyParsed(); // Save space
    }

    /**
     * Highlights a cell by setting its background color to red if it contains
     * a protein that is part of the molecule set.
     *
     * @param cell The cell to highlight.
     */
    public void highlightCells(Cell cell) {
        if (moleculeSetChecker.isProteinPartOfMoleculeSet(cell.getStringCellValue())) {
            CellStyle cellStyle = cell.getSheet().getWorkbook().createCellStyle();
            cellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(cellStyle);
        }
    }

    /**
     * Finds the index of a column in the Excel sheet based on the given column name.
     *
     * @param sheet          The sheet to search for the column.
     * @param selectedColumn The name of the column to find.
     * @return The column index if found; -1 otherwise.
     */
    private int findColumnIndex(Sheet sheet, String selectedColumn) {
        Row headerRow = sheet.getRow(0);
        for (Cell cell : headerRow) {
            if (formatter.formatCellValue(cell).contains(selectedColumn)) {
                return cell.getColumnIndex();
            }
        }
        return -1;
    }

    /**
     * Writes the modified data to a file with a specific separator format.
     * If an error occurs, displays an error dialog.
     *
     * @param data The modified data to write to the file.
     */
    private void writeFileWithSeparator(List<List<String>> data) {
        try (CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(
                new FileOutputStream(currentFilePath), StandardCharsets.UTF_8),
                separator, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, "\n")) {

            for (List<String> row : data) {
                csvWriter.writeNext(row.stream()
                        .map(value -> value == null ? "" : value.trim()).toArray(String[]::new));
            }

            xmlMakerutils.showInfoDialog("File modified successfully.");
            LOGGER.log(Level.INFO, "File modified successfully.");
            selectFileOpener(currentFilePath);
        } catch (IOException e) {
            xmlMakerutils.showErrorDialog("Error modifying file: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error modifying file", e.getMessage());
        }
    }

    /**
     * Retrieves the number of features based on columns containing "FeatureShortLabel".
     *
     * @return The number of features found.
     */
    public int getNumberOfFeatures(){
        int numberOfFeatures = 0;
        for (String column: columns){
            if (column.toLowerCase().contains("featureshortlabel")){
                numberOfFeatures++;
            }
        }
        return numberOfFeatures;
    }

    /**
     * Creates sub-files from chunks of data.
     *
     * @param data The chunked data to be written.
     */
    public void createSubFilesWithSeparator(List<List<String>> data) {
        String fileExtension = fileType.equals("csv") ? ".csv" : ".tsv";
        outputDirName = currentFilePath.replace(fileExtension, "_chunks"); // Directory name for chunks
        File outputDir = new File(outputDirName);

        if (!outputDir.exists()) {
            if (outputDir.mkdirs()) {
                LOGGER.log(Level.INFO, "Output directory created: " + outputDirName);
            } else {
                LOGGER.log(Level.SEVERE, "Failed to create output directory: " + outputDirName);
                xmlMakerutils.showErrorDialog("Error creating output directory: " + outputDirName);
                return;
            }
        }

        String chunkFileName = new File(outputDir, "chunk_" + lastChunkIndex + fileExtension).getPath();
        lastChunkIndex++;

        try (CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(
                new FileOutputStream(chunkFileName), StandardCharsets.UTF_8),
                separator, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, "\n")) {

            for (List<String> row : data) {
                csvWriter.writeNext(row.toArray(new String[0]));
            }

            LOGGER.log(Level.INFO, "Chunk file created: " + chunkFileName);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error writing chunk file", e);
            xmlMakerutils.showErrorDialog("Error writing chunk file: " + e.getMessage());
        }
    }

    /**
     * Reads a CSV or TSV file and stores its contents.
     *
     * @return A list of rows, where each row is a list of cell values.
     */
    public List<List<String>> readSubFile(String filePath) {
        List<List<String>> data = new ArrayList<>();
        try (CSVReader csvReader = new CSVReaderBuilder(new FileReader(filePath))
                .withCSVParser(new com.opencsv.CSVParserBuilder()
                        .withSeparator(separator)
                        .withIgnoreQuotations(false)
                        .build())
                .build()) {

            String[] nextLine;
            while ((nextLine = csvReader.readNext()) != null) {
                List<String> lineCells = new ArrayList<>();
                for (String cell : nextLine) {
                    lineCells.add(cell == null ? "" : cell.trim());
                }
                data.add(lineCells);
            }
            return data;
        } catch (IOException | CsvValidationException e) {
            LOGGER.log(Level.SEVERE, "Unable to read file with separator", e);
            xmlMakerutils.showErrorDialog("Error reading file: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
