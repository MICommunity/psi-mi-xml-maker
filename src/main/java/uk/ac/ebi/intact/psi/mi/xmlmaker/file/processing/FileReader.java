package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import uk.ac.ebi.intact.psi.mi.xmlmaker.events.InputSelectedEvent.Listener;
import uk.ac.ebi.intact.psi.mi.xmlmaker.events.InputSelectedEvent;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.FileUtils;

import javax.swing.*;
import java.awt.Font;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.GuiUtils.*;

/**
 * The {@code FileReader} class provides functionality for reading, processing,
 * and modifying Excel (XLS, XLSX) and delimited text files (CSV, TSV).
 * It integrates with UniprotMapper and MoleculeSetChecker to validate and update data.
 */
public class FileReader {

    private static final Logger LOGGER = Logger.getLogger(FileReader.class.getName());

    // Dependencies
    private final DataFormatter formatter = new DataFormatter();

    // File data

    private final List<Listener> listeners = new ArrayList<>();
    private String fileName;
    private String fileType;

    @Getter
    private char separator;
    private final JLabel currentFileLabel;
    private final List<String> columns = new ArrayList<>();

    @Setter @Getter public String publicationId;

    @Setter @Getter public String publicationDb;

    @Getter private String currentFilePath;

    @Getter @Setter String sheetSelectedUpdate;

    @Getter private Workbook workbook;

    public List<String> fileData;
    public final List<String> sheets = new ArrayList<>();

    public FileReader() {
        this.fileName = null;
        this.currentFileLabel = new JLabel("No file selected");
    }

    // FILE READING

    /**
     * Opens the specified file and reads its contents based on the file type.
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
            if (!handleFileByType(fileType, filePath)) {
                LOGGER.warning("Unsupported file format: " + fileType);
                showErrorDialog("Unsupported file format! Supported formats: .csv, .tsv, .xls, .xlsx");
                return;
            }
            fireInputSelectedEvent(new InputSelectedEvent(file));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading file: " + fileName, e);
            showErrorDialog("Unable to read file: " + e);
        }
    }

    /**
     * Processes the file based on its extension.
     *
     * @param fileType the file extension (e.g. "csv", "xlsx")
     * @param filePath the full path to the file
     * @return {@code true} if the file type is supported and processed, {@code false} otherwise
     * @throws IOException if an error occurs while reading the file
     */
    private boolean handleFileByType(String fileType, String filePath) throws IOException {
        LOGGER.info("Reading " + fileType + " file: " + fileName);
        switch (fileType) {
            case "xlsx":
                readXlsxFile(filePath);
                return true;
            case "xls":
                readXlsFile(filePath);
                return true;
            case "csv":
                separator = ',';
                readFileWithSeparator();
                return true;
            case "tsv":
                separator = '\t';
                readFileWithSeparator();
                return true;
            default:
                return false;
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
     * Reads the specified sheet from the workbook and returns an iterator over its rows.
     *
     * @param sheetSelected the name of the sheet to read.
     * @return an iterator of rows from the selected sheet. If the sheet does not exist, returns an empty iterator.
     */
    public Iterator<Row> readWorkbookSheet(String sheetSelected) {
        Sheet sheet = workbook.getSheet(sheetSelected);
        if (sheet == null) {
            LOGGER.severe("Sheet '" + sheetSelected + "' does not exist.");
            return Collections.emptyIterator();
        }

        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) {
            Row headerRow = rowIterator.next();
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(formatter.formatCellValue(cell));
            }
            this.fileData = headers;
        }

        return rowIterator;
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
            showErrorDialog("Error reading file: " + e.getMessage());
        }
        return iterator;
    }

    // GUI data

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
        sheetSelectedUpdate = sheetSelected;
        columns.clear();

        if (fileType == null || fileType.isEmpty()) {
            LOGGER.warning("File type is not specified.");
            return columns;
        }

        if (fileType.equalsIgnoreCase("xlsx") || fileType.equalsIgnoreCase("xls")) {
            if (workbook == null) {
                LOGGER.warning("No Excel workbook data loaded.");
                return columns;
            }
            Sheet sheet = workbook.getSheet(sheetSelected);
            if (sheet == null) {
                LOGGER.warning("Sheet not found: " + sheetSelected);
                return columns;
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                LOGGER.warning("Header row is missing in the sheet: " + sheetSelected);
                return columns;
            }
            for (Cell cell : headerRow) {
                columns.add(formatter.formatCellValue(cell));
            }

        } else if (fileType.equalsIgnoreCase("csv") || fileType.equalsIgnoreCase("tsv")) {
            if (fileData == null || fileData.isEmpty()) {
                LOGGER.warning("No CSV/TSV file data loaded.");
                return columns;
            }
            columns.addAll(fileData);

        } else {
            LOGGER.warning("Unsupported file type: " + fileType);
            return columns;
        }

        if (columns.isEmpty()) {
            columns.add("No data");
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
     * Retrieves the first few lines from the specified sheet or separated file.
     *
     * @param sheetSelected the name of the sheet (if applicable).
     * @param numberOfRows  the number of rows to retrieve.
     * @return a list of rows, where each row is a list of strings.
     */
    public List<List<String>> getFileFirstLines(String sheetSelected, int numberOfRows) {
        List<List<String>> firstLines = new ArrayList<>();
        int i = 0;

        if (workbook == null){
            Iterator<List<String>> iteratorSeparated = readFileWithSeparator();
            while (iteratorSeparated.hasNext() && i < numberOfRows) {
                firstLines.add(iteratorSeparated.next());
                i++;
            }
            return firstLines;
        }
        Iterator<Row> iteratorWorkbook = readWorkbookSheet(sheetSelected);
        while (iteratorWorkbook.hasNext() && i < numberOfRows) {
            Row row = iteratorWorkbook.next();
            List<String> currentLine = new ArrayList<>();
            for (int j = 0; j < row.getLastCellNum(); j++) {
                currentLine.add(FileUtils.getCellValueAsString(row.getCell(j)));
            }
            firstLines.add(currentLine);
        }
        return firstLines;
    }

    /**
     * Registers an event listener for the InputSelectedEvent.
     *
     * @param listener the listener to be added.
     */
    public void registerInputSelectedEventHandler(Listener listener) {
        listeners.add(listener);
    }

    /**
     * Triggers the InputSelectedEvent for all registered listeners.
     *
     * @param event the event to be fired.
     */
    private void fireInputSelectedEvent(InputSelectedEvent event) {
        listeners.forEach(listener -> listener.handle(event));
    }
}
