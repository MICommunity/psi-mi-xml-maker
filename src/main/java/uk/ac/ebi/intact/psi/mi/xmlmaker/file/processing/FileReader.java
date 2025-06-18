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
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.UniprotResult;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.*;
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

import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.GuiUtils.*;

/**
 * The {@code FileReader} class provides functionality for reading, processing,
 * and modifying Excel (XLS, XLSX) and delimited text files (CSV, TSV).
 * It integrates with UniprotMapper and MoleculeSetChecker to validate and update data.
 */
public class FileReader {

    private static final Logger LOGGER = Logger.getLogger(FileReader.class.getName());

    // Dependencies

    private final MoleculeSetChecker moleculeSetChecker = new MoleculeSetChecker();
    private final DataFormatter formatter = new DataFormatter();
    private final UniprotGeneralMapper uniprotGeneralMapper = new UniprotGeneralMapper();

    // File data
    private final Map<String, UniprotResult> alreadyParsed = new HashMap<>();
    private final List<InputSelectedEvent.Listener> listeners = new ArrayList<>();
    private String fileName;
    private String fileType;
    private char separator;
    private final JLabel currentFileLabel;
    private final List<String> columns = new ArrayList<>();

    @Setter @Getter public String publicationId;

    @Setter @Getter public String publicationDb;

    @Getter private String currentFilePath;

    @Getter private List<String> uniprotIdNotFound = new ArrayList<>();

    @Getter @Setter String sheetSelectedUpdate;

    @Getter private Workbook workbook;

    @Getter private final List<String> proteinsPartOfMoleculeSet = new ArrayList<>();

    public List<String> fileData;
    public final List<String> sheets = new ArrayList<>();

    public FileReader() {
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

    //MODIFY AND WRITE FILES

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
     * @param previousIdDbColumnIndex      the column index of the previous ID db.
     * @param organismColumnIndex      the organism column index.
     */
    public void checkAndInsertUniprotResultsSeparatedFormat(String idColumnName, int previousIdDbColumnIndex, int organismColumnIndex) {
        Iterator<List<String>> iterator = readFileWithSeparator();
        String tmpFilePath = currentFilePath;
        int idColumnIndex = fileData.indexOf(idColumnName);

        if (idColumnIndex == -1) {
            showErrorDialog("ID column not found: " + idColumnName);
            LOGGER.severe("ID column not found: " + idColumnName);
            return;
        }

        int originalColumnCount = fileData.size();

        fileData.addAll(Arrays.asList("Participant ID", "Participant ID database", "Participant organism", "Participant name", "Participant type"));
        try (CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(new FileOutputStream(tmpFilePath), StandardCharsets.UTF_8),
                separator, CSVWriter.DEFAULT_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END)) {

            csvWriter.writeNext(fileData.toArray(new String[0]));

            while (iterator.hasNext()) {
                List<String> row = iterator.next();

                if (row == null || row.isEmpty() || row.stream().allMatch(String::isEmpty)) {
                    continue;
                }

                while (row.size() < originalColumnCount) {
                    row.add("");
                }
                processRow(row, idColumnIndex, previousIdDbColumnIndex, organismColumnIndex, csvWriter);
            }

            alreadyParsed.clear();
        } catch (IOException e) {
            showErrorDialog("Error writing file: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error writing file", e);
        }

        selectFileOpener(currentFilePath);
    }

    /**
     * Processes a workbook and updates identifiers using UniProt results.
     *
     * @param sheetSelected     the name of the sheet to process.
     * @param idColumnName      the name of the column containing the ID.
     * @param idDbColumnIndex      column index of the id database.
     * @param organismColumnIndex      the organism column index.
     */
    public void checkAndInsertUniprotResultsWorkbook(String sheetSelected, String idColumnName, int idDbColumnIndex, int organismColumnIndex) {
        FileOutputStream fileOut = null;
        try {
            Iterator<Row> iterator = readWorkbookSheet(sheetSelected);
            int idColumnIndex = fileData.indexOf(idColumnName);

            Sheet sheet = workbook.getSheet(sheetSelected);
            if (sheet == null) {
                LOGGER.severe("Sheet not found: " + sheetSelected);
                showErrorDialog("Sheet not found: " + sheetSelected);
                return;
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                LOGGER.severe("Header row is missing.");
                showErrorDialog("Header row is missing.");
                return;
            }

            int lastCellIndex = headerRow.getLastCellNum();
            headerRow.createCell(lastCellIndex).setCellValue("Participant ID");
            headerRow.createCell(lastCellIndex + 1).setCellValue("Participant ID database");
            headerRow.createCell(lastCellIndex + 2).setCellValue("Participant organism");
            headerRow.createCell(lastCellIndex + 3).setCellValue("Participant name");
            headerRow.createCell(lastCellIndex + 4).setCellValue("Participant type");

            while (iterator.hasNext()) {
                processWorkbookRow(iterator.next(), idColumnIndex, idDbColumnIndex, organismColumnIndex, lastCellIndex);
            }

            // Ensure writing happens only after processing
            fileOut = new FileOutputStream(currentFilePath);
            workbook.write(fileOut);

        } catch (IOException e) {
            showErrorDialog("Error processing workbook: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error processing workbook", e);
        } finally {
            try {
                if (fileOut != null) {
                    fileOut.close();
                }
                if (workbook != null) {
                    workbook.close();
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error closing workbook", e);
            }
            alreadyParsed.clear();
        }

        selectFileOpener(currentFilePath);
    }

    /**
     * Retrieves updated UniProt data based on a previous identifier, database, and organism.
     * Uses a cache to avoid redundant lookups.
     *
     * @param previousId       The previous identifier to update.
     * @param previousDb       The database from which the identifier originates.
     * @param updatedOrganism  The organism to be used for querying.
     * @return                 A {@link UniprotResult} containing updated data, or null if not found.
     */
    private UniprotResult getUpdatedUniprotData(String previousId, String previousDb, String updatedOrganism) {
        if (previousId == null || previousId.isEmpty()) {
            return null;
        }

        UniprotResult result = alreadyParsed.get(previousId);
        if (result == null) {
            result = getOneUniprotId(previousId, previousDb, updatedOrganism);
            if (result != null) {
                alreadyParsed.put(previousId, result);
            }
        }
        return result;
    }

    /**
     * Processes a single CSV row, updating UniProt information and appending it to the output CSV.
     *
     * @param row                      The input row as a list of string values.
     * @param idColumnIndex           Index of the ID column in the row.
     * @param previousIdDbColumnIndex Index of the ID database column in the row.
     * @param organismColumnIndex     Index of the organism column in the row.
     * @param csvWriter               The {@link CSVWriter} used to write the updated row.
     */
    private void processRow(List<String> row, int idColumnIndex, int previousIdDbColumnIndex, int organismColumnIndex, CSVWriter csvWriter) {
        String previousId = row.get(idColumnIndex).trim();
        String previousDb = (previousIdDbColumnIndex >= 0 && previousIdDbColumnIndex < row.size()) ? row.get(previousIdDbColumnIndex).trim() : "";
        String updatedOrganism = (organismColumnIndex >= 0 && organismColumnIndex < row.size()) ? row.get(organismColumnIndex).trim() : "";

        if (previousId.isEmpty()) {
            LOGGER.warning("Skipping row with null or empty ID: " + row);
            return;
        }

        UniprotResult result = getUpdatedUniprotData(previousId, previousDb, updatedOrganism);

        String uniprotResult = (result != null && result.getUniprotAc() != null) ? result.getUniprotAc() : previousId;
        String uniprotResultDb = (result != null && result.getIdDb() != null) ? result.getIdDb() : previousDb;
        updatedOrganism = (result != null && result.getOrganism() != null) ? result.getOrganism() : updatedOrganism;
        String participantName = (result != null && result.getName() != null) ? result.getName() : previousId;
        String participantType = (result != null && result.getParticipantType() != null) ? result.getParticipantType() : "Unknown";

        List<String> data = new ArrayList<>(row);
        data.add(uniprotResult);
        data.add(uniprotResultDb);
        data.add(updatedOrganism);
        data.add(participantName);
        data.add(participantType);

        csvWriter.writeNext(data.toArray(new String[0]));
    }

    /**
     * Processes a single Excel workbook row, updating UniProt data and writing the results
     * into new cells in the row.
     *
     * @param row                 The Excel {@link Row} to process.
     * @param idColumnIndex      Index of the ID column.
     * @param idDbColumnIndex    Index of the ID database column.
     * @param organismColumnIndex Index of the organism column.
     * @param lastCellIndex      Index at which to start writing new UniProt-related cells.
     */
    private void processWorkbookRow(Row row, int idColumnIndex, int idDbColumnIndex, int organismColumnIndex, int lastCellIndex) {
        if (row == null || row.getLastCellNum() <= idColumnIndex) {
            LOGGER.warning("Skipping null or incomplete row: " + row);
            return;
        }

        String previousId = FileUtils.getCellValueAsString(row.getCell(idColumnIndex));
        String previousDb = (idDbColumnIndex != -1) ? FileUtils.getCellValueAsString(row.getCell(idDbColumnIndex)) : null;
        String organism = (organismColumnIndex != -1) ? FileUtils.getCellValueAsString(row.getCell(organismColumnIndex)).trim() : null;

        if (previousId == null || previousId.isEmpty()) {
            LOGGER.warning("Skipping row with null or empty ID: " + row);
            return;
        }

        UniprotResult result = getUpdatedUniprotData(previousId, previousDb, organism);

        String uniprotResult = (result != null) ? result.getUniprotAc() : previousId;
        String uniprotResultDb = (result != null) ? result.getIdDb() : "";
        String updatedOrganism = (result != null) ? result.getOrganism() : (organism != null ? organism : "");
        String participantName = (result != null) ? result.getName() : previousId;
        String participantType = (result != null) ? result.getParticipantType() : "";

        row.createCell(lastCellIndex).setCellValue(uniprotResult);
        row.createCell(lastCellIndex + 1).setCellValue(uniprotResultDb);
        row.createCell(lastCellIndex + 2).setCellValue(updatedOrganism);
        row.createCell(lastCellIndex + 3).setCellValue(participantName);
        row.createCell(lastCellIndex + 4).setCellValue(participantType);
    }

    /**
     * Retrieves a single UniProt ID based on a given identifier, database, and organism.
     * The method prioritises reviewed (Swiss-Prot) entries over unreviewed (TrEMBL) entries.
     *
     * <p>Selection logic:
     * <ul>
     *     <li>If there is exactly one Swiss-Prot entry, it is returned.</li>
     *     <li>If multiple Swiss-Prot entries exist, a selection panel is displayed for user input.</li>
     *     <li>If no Swiss-Prot entries exist, the TrEMBL entries are sorted by sequence size (largest first),
     *         and the first one is returned.</li>
     * </ul>
     * </p>
     *
     * <p>If the selected UniProt ID belongs to a molecule set, it is added to the relevant set.</p>
     *
     * @param previousId    The identifier used to fetch UniProt entries.
     * @param previousIdDb  The database associated with the identifier.
     * @param organism      The organism to which the UniProt entry belongs.
     * @return A {@link UniprotResult} object representing the selected UniProt ID, or {@code null} if no entries exist.
     */
    private UniprotResult getOneUniprotId(String previousId, String previousIdDb, String organism) {
        if (previousId != null &&
                previousId.contains("PRO_") &&
                previousIdDb.equalsIgnoreCase("uniprotkb")) {
            UniprotResult uniprotResult = new UniprotResult(previousId, previousId,
                    organism, null, null, previousIdDb,
                    -1, "protein");
            alreadyParsed.put(previousId, uniprotResult);
            return uniprotResult;
        }

        UniprotGeneralMapperGui mapperGui = new UniprotGeneralMapperGui();
        ArrayList<UniprotResult> uniprotResults = uniprotGeneralMapper.fetchUniprotResult(previousId, previousIdDb, organism);
        uniprotIdNotFound = uniprotGeneralMapper.getUniprotIdNotFound();

        UniprotResult oneUniprotId = null;

        List<UniprotResult> swissProtEntries = new ArrayList<>();
        List<UniprotResult> tremblEntries = new ArrayList<>();
        List<UniprotResult> noEntryTypes = new ArrayList<>();

        if (uniprotResults == null || uniprotResults.isEmpty()) {
            mapperGui.getUniprotIdChoicePanel(uniprotGeneralMapper.getButtonGroup(), previousId, previousIdDb);
            synchronized (this) {
                while (mapperGui.getSelectedId() == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        LOGGER.warning("Error fetching uniprot ID: " + e);
                    }
                }
            }
            oneUniprotId = new UniprotResult(mapperGui.getSelectedId(), mapperGui.getSelectedId(),
                    organism, null, null, mapperGui.getSelectedIdDb(),
                    -1, mapperGui.getSelectedParticipantType());

            return oneUniprotId;
        }

        for (UniprotResult result : uniprotResults) {
            if ("UniProtKB reviewed (Swiss-Prot)".equals(result.getEntryType())) {
                swissProtEntries.add(result);
            } else if ("UniProtKB unreviewed (TrEMBL)".equals(result.getEntryType())) {
                tremblEntries.add(result);
            } else if (result.getEntryType() == null){
                noEntryTypes.add(result);
            }
        }

        if (swissProtEntries.size() == 1) {
            return swissProtEntries.get(0);
        } else if (swissProtEntries.size() > 1) {
            mapperGui.getUniprotIdChoicePanel(uniprotGeneralMapper.getButtonGroup(), previousId, previousIdDb);
            synchronized (this) {
                while (mapperGui.getSelectedId() == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        LOGGER.warning("Error fetching uniprot ID: " + e);
                    }
                }
            }

            for (UniprotResult uniprotResult : swissProtEntries) {
                if (uniprotResult.getUniprotAc().equals(mapperGui.getSelectedId())) {
                    oneUniprotId = uniprotResult;
                    return oneUniprotId;
                }
            }
            oneUniprotId = new UniprotResult(mapperGui.getSelectedId(), mapperGui.getSelectedId(),
                    organism, null, null, mapperGui.getSelectedIdDb(),
                    -1, mapperGui.getSelectedParticipantType());

        } else if (!tremblEntries.isEmpty()) {
            tremblEntries.sort(Comparator.comparingInt(UniprotResult::getSequenceSize).reversed());
            oneUniprotId = tremblEntries.get(0);
        } else if (!noEntryTypes.isEmpty()) {
            oneUniprotId = noEntryTypes.get(0);
        }

        if (oneUniprotId != null && moleculeSetChecker.isProteinPartOfMoleculeSet(oneUniprotId.getUniprotAc())) {
            proteinsPartOfMoleculeSet.add(oneUniprotId.getUniprotAc());
        }
        return oneUniprotId;
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
}
