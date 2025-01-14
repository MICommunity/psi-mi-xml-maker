package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.events.InputSelectedEvent;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.MoleculeSetChecker;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapper;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.FileUtils;

import javax.swing.*;
import java.awt.Font;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * The {@code ExcelFileReader} class provides functionality for reading, processing,
 * and modifying Excel (XLS, XLSX) and delimited text files (CSV, TSV).
 * It integrates with UniprotMapper and MoleculeSetChecker to validate and update data.
 */
public class ExcelFileReader  {

    private static final Logger LOGGER = Logger.getLogger(ExcelFileReader.class.getName());
    private final MoleculeSetChecker moleculeSetChecker = new MoleculeSetChecker();
    private final DataFormatter formatter = new DataFormatter();
    private final UniprotMapper uniprotMapper = new UniprotMapper();
    private final XmlMakerUtils xmlMakerutils = new XmlMakerUtils();

    public Workbook workbook;
    public List<String> fileData;

    private final List<InputSelectedEvent.Listener> listeners = new ArrayList<>();

    @Getter
    @Setter
    public String publicationId;
    public String currentFilePath;
    private String fileName;
    private String fileType;
    private char separator;
    private final JLabel currentFileLabel;
    public final List<String> sheets = new ArrayList<>();
    private final List<String> columns = new ArrayList<>();
    public final List<String> proteinsPartOfMoleculeSet = new ArrayList<>();

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
        File file = new File(filePath);
        fileName = file.getName();
        fileType = FileUtils.getFileExtension(filePath);
        currentFileLabel.setText(getFileName());
        currentFilePath = filePath;
        fileData = new ArrayList<>();
        sheets.clear();
        columns.clear();

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
            fireInputSelectedEvent(new InputSelectedEvent(file));
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

    public Iterator<Row> readWorkbookSheet(String sheetSelected) {
        Iterator<Row> iterator = null;
        Sheet sheet = workbook.getSheet(sheetSelected);
        iterator = StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(sheet.iterator(), Spliterator.ORDERED), false)
                .collect(Collectors.toList()).iterator();

        if (iterator.hasNext()) {
            Row row = iterator.next();
            List<String> rowData = new ArrayList<>();
            for (Cell cell : row) {
                rowData.add(cell.toString());
            }
            fileData = rowData;
        }

        return iterator;
    }

        /**
         * Reads the CSV/TSV file and store its content.
         *
         * @return the file data
         */
    public Iterator<List<String>> readFileWithSeparator() {
        Iterator<List<String>> iterator = null;

        try (InputStream fileStream = new FileInputStream(currentFilePath);
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileStream));
             CSVReader csvReader = new CSVReaderBuilder(bufferedReader)
                     .withCSVParser(new CSVParserBuilder()
                             .withSeparator(separator)
                             .withIgnoreQuotations(false)
                             .build())
                     .build()) {

            iterator = StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(csvReader.iterator(), Spliterator.ORDERED), false)
                    .map(row -> Arrays.stream(row).map(String::trim).collect(Collectors.toList()))
                    .collect(Collectors.toList()).iterator();

            if (iterator.hasNext()) {
                fileData = iterator.next();
            }
            return iterator;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to read file with separator", e);
            xmlMakerutils.showErrorDialog("Error reading file: " + e.getMessage());
        }
        return iterator;
    }

    // GUI

    /**
     * Gets the list of sheet names in the current workbook.
     */
    public void getSheets() {
        sheets.clear();
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

    //MODIFY AND WRITE FILES

    /**
     * Retrieves the number of features based on columns containing "FeatureShortLabel".
     *
     * @return The number of features found.
     */
    public int getNumberOfFeatures() {
        int numberOfFeatures = 0;
        for (String column : columns) {
            if (column.toLowerCase().contains("featureshortlabel")) {
                numberOfFeatures++;
            }
        }
        return numberOfFeatures;
    }

    public void registerInputSelectedEventHandler(InputSelectedEvent.Listener listener) {
        listeners.add(listener);
    }

    private void fireInputSelectedEvent(InputSelectedEvent event) {
        listeners.forEach(listener -> listener.handle(event));
    }

    public void checkAndInsertUniprotResultsSeparatedFormat(String idColumnName, int organismColumnIndex,
                                                            int idDbColumnIndex) {
        Iterator<List<String>> iterator = readFileWithSeparator();

        int idColumnIndex = fileData.indexOf(idColumnName);
        if (idColumnIndex == -1) {
            LOGGER.severe("Invalid column name: " + idColumnName);
            return;
        }

        String tmpFilePath = "tmp." + FileUtils.getFileExtension(fileName);
        try (CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(
                new FileOutputStream(tmpFilePath), StandardCharsets.UTF_8),
                separator, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, "\n")) {

            csvWriter.writeNext(fileData.toArray(new String[0]));

            while (iterator.hasNext()) {
                List<String> row = iterator.next();

                String previousId = row.get(idColumnIndex);
                String organismValue = row.get(organismColumnIndex);
                String previousIdDbValue = row.get(idDbColumnIndex);

                String uniprotResult = uniprotMapper.fetchUniprotResults(previousId, organismValue, previousIdDbValue);

                if (moleculeSetChecker.isProteinPartOfMoleculeSet(uniprotResult)) {
                    proteinsPartOfMoleculeSet.add(uniprotResult);
                }

                List<String> data = new ArrayList<>();
                for (int cell = 0; cell < row.size(); cell++) {
                    boolean arePreviousAndFetchedIdEquals = !Objects.equals(uniprotResult, previousId) && !uniprotResult.isEmpty();
                    if (cell == idColumnIndex) {
                        data.add(arePreviousAndFetchedIdEquals ? uniprotResult : row.get(cell));
                    } else if (cell == idDbColumnIndex) {
                        data.add(arePreviousAndFetchedIdEquals ? "UniprotKB" : row.get(cell));
                    } else {
                        data.add(row.get(cell));
                    }
                }

                csvWriter.writeNext(data.toArray(new String[0]));
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error writing file", e);
        }
    }
    public void checkAndInsertUniprotResultsIterator(String sheetSelected, String idColumnName,
                                                     int organismColumnIndex, int idDbColumnIndex) {
        try (FileOutputStream fileOut = new FileOutputStream("tmp." + FileUtils.getFileExtension(fileName))) {
            Iterator<Row> iterator = readWorkbookSheet(sheetSelected);

            if (fileData == null || fileData.isEmpty()) {
                LOGGER.severe("Header row is missing or invalid.");
                return;
            }

            int idColumnIndex = fileData.indexOf(idColumnName);
            if (idColumnIndex == -1) {
                LOGGER.severe("Invalid column name: " + idColumnName);
                return;
            }

            Workbook workbook = WorkbookFactory.create(new FileInputStream(currentFilePath));

            while (iterator.hasNext()) {
                Row row = iterator.next();
                if (row == null) continue;

                Cell previousIdCell = row.getCell(idColumnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                Cell organismCell = row.getCell(organismColumnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                Cell previousIdDbCell = row.getCell(idDbColumnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

                String previousId = previousIdCell.getStringCellValue();
                String organismValue = organismCell.getStringCellValue();
                String previousIdDbValue = previousIdDbCell.getStringCellValue();

                String uniprotResult = uniprotMapper.fetchUniprotResults(previousId, organismValue, previousIdDbValue);

                if (moleculeSetChecker.isProteinPartOfMoleculeSet(uniprotResult)) {
                    proteinsPartOfMoleculeSet.add(uniprotResult);
                }

                if (!uniprotResult.isEmpty() && !uniprotResult.equals(previousId)) {
                    previousIdCell.setCellValue(uniprotResult);
                    previousIdDbCell.setCellValue("UniprotKB");
                }
            }

            workbook.write(fileOut);
            workbook.close();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error processing workbook", e);
        }
    }

}
