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
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.events.InputSelectedEvent;
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
    private final XmlMakerUtils xmlMakerutils = new XmlMakerUtils();
    private final UniprotGeneralMapper uniprotGeneralMapper = new UniprotGeneralMapper();

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
    public Map<String, String> alreadyParsed = new HashMap<>();

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

    /**
     * Reads the specified sheet from the workbook and returns an iterator over its rows.
     *
     * @param sheetSelected the name of the sheet to read.
     * @return an iterator of rows from the selected sheet. If the sheet does not exist, returns an empty iterator.
     */
    public Iterator<Row> readWorkbookSheet(String sheetSelected) {
        Sheet sheet = workbook.getSheet(sheetSelected);
        if (sheet == null) {
            LOGGER.severe("Sheet " + sheetSelected + " does not exist.");
            return Collections.emptyIterator();
        }

        Iterator<Row> iterator = sheet.iterator();

        boolean isFirstRow = true;

        if (iterator.hasNext()) {
            Row row = iterator.next();
            List<String> rowData = new ArrayList<>();
            for (Cell cell : row) {
                rowData.add(formatter.formatCellValue(cell));
            }

            if (isFirstRow) {
                fileData = rowData;
                isFirstRow = false;
            }
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

    /**
     * Registers an event listener for the InputSelectedEvent.
     *
     * @param listener the listener to be added.
     */
    public void registerInputSelectedEventHandler(InputSelectedEvent.Listener listener) {
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

    /**
     * Reads a separated file, processes it, and updates identifiers using UniProt results.
     *
     * @param idColumnName      the name of the column containing the ID.
     */
    public void checkAndInsertUniprotResultsSeparatedFormat(String idColumnName) {
        Iterator<List<String>> iterator = readFileWithSeparator();

        int idColumnIndex = fileData.indexOf(idColumnName);
        if (idColumnIndex == -1) {
            LOGGER.severe("Invalid column name: " + idColumnName);
            return;
        }

        fileData.add("Updated ids");
        fileData.add("Updated databases");

        String tmpFilePath = "tmp." + FileUtils.getFileExtension(fileName);
        try (CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(
                new FileOutputStream(tmpFilePath), StandardCharsets.UTF_8),
                separator, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, "\n")) {

            csvWriter.writeNext(fileData.toArray(new String[0]));

            while (iterator.hasNext()) {
                List<String> row = iterator.next();

                String previousId = row.get(idColumnIndex);

                String uniprotResult;
                String alreadyParsedId = alreadyParsed.get(previousId);

                if (alreadyParsedId == null) {
                    uniprotResult = getOneUniprotId(previousId);
                    alreadyParsed.put(previousId, uniprotResult);
                } else {
                    uniprotResult = alreadyParsedId;
                }

                List<String> data = new ArrayList<>(row);
                data.add(uniprotResult != null ? uniprotResult : previousId);
                data.add(uniprotResult != null ? "UniprotKB" : "");
                csvWriter.writeNext(data.toArray(new String[0]));
            }
            alreadyParsed.clear();
        } catch (IOException e) {
            xmlMakerutils.showErrorDialog("Error reading file: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error writing file", e);
        }
    }

    private String getOneUniprotId(String previousId) {
        ArrayList<UniprotResult> uniprotResults = uniprotGeneralMapper.fetchUniprotResult(previousId);

        if (uniprotResults.isEmpty()) {
            return null;
        }

        String uniprotResult = uniprotResults.get(0).getUniprotAc();

        if (uniprotResults.size() > 1) {
            UniprotGeneralMapperGui mapperGui = new UniprotGeneralMapperGui(uniprotGeneralMapper);
            mapperGui.getUniprotIdChoicePanel(uniprotGeneralMapper.getButtonGroup());

            synchronized (this) {
                while (mapperGui.getSelectedId() == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            uniprotResult = mapperGui.getSelectedId();
            System.out.println(uniprotResult);
        }

        if (moleculeSetChecker.isProteinPartOfMoleculeSet(uniprotResult)) {
            proteinsPartOfMoleculeSet.add(uniprotResult);
        }
        return uniprotResult;
    }

    /**
     * Processes a workbook and updates identifiers using UniProt results.
     *
     * @param sheetSelected     the name of the sheet to process.
     * @param idColumnName      the name of the column containing the ID.
     */
    public void checkAndInsertUniprotResultsWorkbook(String sheetSelected, String idColumnName) {
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

            while (iterator.hasNext()) {
                Row row = iterator.next();
                if (row == null) continue;

                row.shiftCellsRight(idColumnIndex + 1, row.getLastCellNum() - 1, 1);
                Cell previousIdCell = row.getCell(idColumnIndex);
                String previousId = FileUtils.getCellValueAsString(previousIdCell);
                String uniprotResult;
                String alreadyParsedId = alreadyParsed.get(previousId);

                if (alreadyParsedId == null) {
                    uniprotResult = getOneUniprotId(previousId);
                    alreadyParsed.put(previousId, uniprotResult);
                } else {
                    uniprotResult = alreadyParsedId;
                }

                Cell newColumnCell = row.getCell(idColumnIndex + 1);
                if (newColumnCell == null) {
                    newColumnCell = row.createCell(idColumnIndex + 1);
                    newColumnCell.setCellType(CellType.STRING);
                }

                newColumnCell.setCellValue(uniprotResult != null ? uniprotResult : previousId);

            }
            workbook.write(fileOut);
            workbook.close();
            alreadyParsed.clear();

        } catch (IOException e) {
            xmlMakerutils.showErrorDialog("Error reading file: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error processing workbook", e);
        }
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
            for (Cell cell : row) {
                currentLine.add(cell.getStringCellValue());
            }
            firstLines.add(currentLine);
        }
        return firstLines;
    }
}
