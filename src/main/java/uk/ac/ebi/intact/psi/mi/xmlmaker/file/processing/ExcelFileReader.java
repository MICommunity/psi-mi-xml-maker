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
    private final UniprotGeneralMapper uniprotGeneralMapper = new UniprotGeneralMapper();

    public Workbook workbook;
    public List<String> fileData;

    private final List<InputSelectedEvent.Listener> listeners = new ArrayList<>();

    @Getter
    @Setter
    public String publicationId;
    @Setter
    @Getter
    public String publicationDb;
    public String currentFilePath;
    private String fileName;
    private String fileType;
    private char separator;
    private final JLabel currentFileLabel;
    public final List<String> sheets = new ArrayList<>();
    private final List<String> columns = new ArrayList<>();
    public final List<String> proteinsPartOfMoleculeSet = new ArrayList<>();
    public final Map<String, UniprotResult> alreadyParsed = new HashMap<>();

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
                    XmlMakerUtils.showErrorDialog("Unsupported file format! Supported formats: .csv, .tsv, .xls, .xlsx");
            }
            fireInputSelectedEvent(new InputSelectedEvent(file));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading file: " + fileName, e);
            XmlMakerUtils.showErrorDialog("Unable to read file: " + e.getMessage());
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

        if (iterator.hasNext()) {
            Row row = iterator.next();
            List<String> rowData = new ArrayList<>();
            for (Cell cell : row) {
                rowData.add(formatter.formatCellValue(cell));
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
            XmlMakerUtils.showErrorDialog("Error reading file: " + e.getMessage());
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
    public void checkAndInsertUniprotResultsSeparatedFormat(String idColumnName, int previousIdDbColumnIndex, int organismColumnIndex) {
        Iterator<List<String>> iterator = readFileWithSeparator();
        String tmpFilePath = currentFilePath;

        if (fileData == null || fileData.isEmpty()) {
            LOGGER.severe("Header row is missing or invalid.");
            XmlMakerUtils.showErrorDialog("Header row is missing or invalid.");
            return;
        }

        int idColumnIndex = fileData.indexOf(idColumnName);
        if (idColumnIndex == -1) {
            LOGGER.severe("Invalid column name: " + idColumnName);
            XmlMakerUtils.showErrorDialog("Invalid column name: " + idColumnName);
            return;
        }

        fileData.add("Participant input ID");
        fileData.add("Participant input ID database");
        fileData.add("Participant organism");
        fileData.add("Participant input name");

        try (CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(
                new FileOutputStream(tmpFilePath),
                StandardCharsets.UTF_8),
                separator,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                "\n")) {

            csvWriter.writeNext(fileData.toArray(new String[0]));

            while (iterator.hasNext()) {
                List<String> row = iterator.next();

                if (row == null || row.size() <= idColumnIndex) {
                    LOGGER.warning("Skipping null or incomplete row: " + row);
                    continue;
                }

                String previousId = row.get(idColumnIndex);
                String previousDb = null;

                String updatedOrganism = null;
                String participantName = null;
                String uniprotResult = null;
                String uniprotResultDb = null;

                if (previousId == null || previousId.isEmpty()) {
                    LOGGER.warning("Skipping row with null or empty ID: " + row);
                    continue;
                }

                if (previousIdDbColumnIndex != -1){
                    previousDb = row.get(previousIdDbColumnIndex);
                }

                if (organismColumnIndex != -1){
                    if (!row.get(organismColumnIndex).toLowerCase().contains("organism") || !row.get(organismColumnIndex).isEmpty()) {
                        updatedOrganism = row.get(organismColumnIndex);
                    }
                }

                UniprotResult alreadyParsedParticipant = alreadyParsed.get(previousId);

                if (alreadyParsedParticipant == null) {
                    UniprotResult result = getOneUniprotId(previousId, previousDb, updatedOrganism);
                    if (result == null) {
                        LOGGER.warning("No UniProt results for ID: " + previousId);
                        uniprotResult = previousId;
                        uniprotResultDb = previousDb; //todo: add previous organism and name

                    } else {
                        uniprotResult = result.getUniprotAc();
                        uniprotResultDb = result.getIdDb();
                        updatedOrganism = result.getOrganism();
                        participantName = result.getName();

                        alreadyParsed.put(previousId, result);
                    }
                } else {
                    uniprotResult = alreadyParsedParticipant.getUniprotAc();
                    uniprotResultDb = alreadyParsedParticipant.getIdDb();
                    participantName = alreadyParsedParticipant.getName();
                    updatedOrganism = alreadyParsedParticipant.getOrganism();
                }

                List<String> data = new ArrayList<>(row);
                data.add(uniprotResult != null ? uniprotResult : previousId);
                data.add(uniprotResultDb != null ? uniprotResultDb : previousDb);
                data.add(updatedOrganism != null ? updatedOrganism : "");
                data.add(participantName != null ? participantName : previousId);

                csvWriter.writeNext(data.toArray(new String[0]));
            }
            alreadyParsed.clear();
        } catch (IOException e) {
            XmlMakerUtils.showErrorDialog("Error reading file: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error writing file", e);
        }
        selectFileOpener(currentFilePath);
    }

    /**
     * Processes a workbook and updates identifiers using UniProt results.
     *
     * @param sheetSelected     the name of the sheet to process.
     * @param idColumnName      the name of the column containing the ID.
     */
    public void checkAndInsertUniprotResultsWorkbook(String sheetSelected, String idColumnName, int idDbColumnIndex, int organismColumnIndex) {
        try (FileOutputStream fileOut = new FileOutputStream(currentFilePath)) {
            Iterator<Row> iterator = readWorkbookSheet(sheetSelected);

            if (fileData == null || fileData.isEmpty()) {
                LOGGER.severe("Header row is missing or invalid.");
                XmlMakerUtils.showErrorDialog("Header row is missing or invalid.");
                return;
            }

            int idColumnIndex = fileData.indexOf(idColumnName);
            if (idColumnIndex == -1) {
                LOGGER.severe("Invalid column name: " + idColumnName);
                XmlMakerUtils.showErrorDialog("Invalid column name: " + idColumnName);
                return;
            }

            Sheet sheet = workbook.getSheet(sheetSelected);
            if (sheet == null) {
                LOGGER.severe("Sheet not found: " + sheetSelected);
                XmlMakerUtils.showErrorDialog("Sheet not found: " + sheetSelected);
                return;
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                LOGGER.severe("Header row is missing.");
                XmlMakerUtils.showErrorDialog("Header row is missing.");
                return;
            }

            headerRow.createCell(headerRow.getLastCellNum()).setCellValue("Participant ID");
            headerRow.createCell(headerRow.getLastCellNum()).setCellValue("Participant ID database");
            headerRow.createCell(headerRow.getLastCellNum()).setCellValue("Participant organism");
            headerRow.createCell(headerRow.getLastCellNum()).setCellValue("Participant name");

            while (iterator.hasNext()) {
                Row row = iterator.next();
                if (row == null || row.getLastCellNum() <= idColumnIndex) {
                    LOGGER.warning("Skipping null or incomplete row: " + row);
                    continue;
                }

                String previousId = FileUtils.getCellValueAsString(row.getCell(idColumnIndex));
                String previousDb = idDbColumnIndex != -1 ? FileUtils.getCellValueAsString(row.getCell(idDbColumnIndex)) : null;

                String organism = null;

                if (organismColumnIndex != -1) {
                    String organismTmp = FileUtils.getCellValueAsString(row.getCell(organismColumnIndex)).trim();
                    if (!organismTmp.toLowerCase().contains("organism") && !organismTmp.isEmpty()) {
                        organism = organismTmp;
                    }
                }

                if (previousId == null || previousId.isEmpty()) {
                    LOGGER.warning("Skipping row with null or empty ID: " + row);
                    continue;
                }

                UniprotResult alreadyParsedParticipant = alreadyParsed.get(previousId);

                String uniprotResult = previousId;
                String uniprotResultDb = previousDb;
                String updatedOrganism = organism;
                String participantName = previousId;

                if (alreadyParsedParticipant == null) {
                    UniprotResult result = getOneUniprotId(previousId, previousDb, organism);
                    if (result == null) {
                        LOGGER.warning("No UniProt results for ID: " + previousId);
                    } else {
                        uniprotResult = result.getUniprotAc();
                        uniprotResultDb = result.getIdDb();
                        updatedOrganism = result.getOrganism();
                        participantName = result.getName();

                        alreadyParsed.put(previousId, result);
                    }
                } else {
                    uniprotResult = alreadyParsedParticipant.getUniprotAc();
                    uniprotResultDb = alreadyParsedParticipant.getIdDb();
                    updatedOrganism = alreadyParsedParticipant.getOrganism();
                    participantName = alreadyParsedParticipant.getName();
                }

                row.createCell(row.getLastCellNum(), CellType.STRING).setCellValue(uniprotResult != null ? uniprotResult : previousId);
                row.createCell(row.getLastCellNum(), CellType.STRING).setCellValue(uniprotResultDb != null ? uniprotResultDb : "");
                row.createCell(row.getLastCellNum(), CellType.STRING).setCellValue(updatedOrganism != null ? updatedOrganism : "");
                row.createCell(row.getLastCellNum(), CellType.STRING).setCellValue(participantName != null ? participantName : "");
            }

            workbook.write(fileOut);
            workbook.close();
            alreadyParsed.clear();
        } catch (IOException e) {
            XmlMakerUtils.showErrorDialog("Error processing workbook: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error processing workbook", e);
        }
        selectFileOpener(currentFilePath);
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
        UniprotGeneralMapperGui mapperGui = new UniprotGeneralMapperGui(uniprotGeneralMapper);
        ArrayList<UniprotResult> uniprotResults = uniprotGeneralMapper.fetchUniprotResult(previousId, previousIdDb, organism);
        UniprotResult oneUniprotId = null;
        List<UniprotResult> swissProtEntries = new ArrayList<>();
        List<UniprotResult> tremblEntries = new ArrayList<>();
        List<UniprotResult> noEntryTypes = new ArrayList<>();

        if (uniprotResults.isEmpty()) {
            mapperGui.getParticipantChoicePanel(previousId);
            oneUniprotId = new UniprotResult(previousId, previousId, organism, null, null,
                    previousIdDb, -1, mapperGui.getSelectedParticipantType());
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
            mapperGui.getUniprotIdChoicePanel(uniprotGeneralMapper.getButtonGroup(), previousId);
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
            for (Cell cell : row) {
                currentLine.add(cell.getStringCellValue());
            }
            firstLines.add(currentLine);
        }
        return firstLines;
    }
}
