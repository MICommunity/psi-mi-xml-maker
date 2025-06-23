package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import com.opencsv.CSVWriter;
import lombok.Getter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.UniprotResult;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.MoleculeSetChecker;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotGeneralMapper;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotGeneralMapperGui;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.FileUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.GuiUtils.showErrorDialog;

public class FileWriter {
    private final FileReader fileReader;
    private final UniprotGeneralMapper uniprotGeneralMapper = new UniprotGeneralMapper();
    private final MoleculeSetChecker moleculeSetChecker = new MoleculeSetChecker();

    private final Map<String, UniprotResult> alreadyParsed = new HashMap<>();

    @Getter
    private final List<String> proteinsPartOfMoleculeSet = new ArrayList<>();
    @Getter
    private List<String> uniprotIdNotFound = new ArrayList<>();

    private final Logger LOGGER = Logger.getLogger(FileWriter.class.getName());

    public FileWriter(FileReader fileReader) {
        this.fileReader = fileReader;
    }

    /**
     * Writes interaction data to a delimited file (CSV or TSV).
     *
     * @param data       List of participant maps to write.
     * @param filePath   Output file path.
     * @param delimiter  Field delimiter (e.g., ',' for CSV or '\t' for TSV).
     * @param header     Array of column headers to include.
     */
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

    /**
     * Writes interaction data to an Excel workbook (XLS or XLSX).
     *
     * @param data       List of participant maps to write.
     * @param filePath   Output file path (should end in .xls or .xlsx).
     * @param header     Array of column headers to include.
     * @param workbook   An Apache POI Workbook instance to write into.
     */
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
     * Reads a separated file, processes it, and updates identifiers using UniProt results.
     *
     * @param idColumnIndex      the index of the column containing the ID.
     * @param previousIdDbColumnIndex      the column index of the previous ID db.
     * @param organismColumnIndex      the organism column index.
     */
    public void checkAndInsertUniprotResultsSeparatedFormat(int idColumnIndex, int previousIdDbColumnIndex, int organismColumnIndex) {
        List<String> fileData = fileReader.fileData;
        char separator = fileReader.getSeparator();
        String currentFilePath = fileReader.getCurrentFilePath();

        Iterator<List<String>> iterator = fileReader.readFileWithSeparator();
        String tmpFilePath = fileReader.getCurrentFilePath();

        if (idColumnIndex == -1) {
            showErrorDialog("ID column not found: " + idColumnIndex);
            LOGGER.severe("ID column not found: " + idColumnIndex);
            return;
        }

        int originalColumnCount = fileData.size();

        //to avoid duplicate in the header
        fileData.set(idColumnIndex, "Input Participant ID");
        fileData.set(previousIdDbColumnIndex, "Input Participant ID database");
        fileData.set(organismColumnIndex, "Input Organism");
        fileData.set(fileData.indexOf("Participant name"), "Input Participant Name");
        fileData.set(fileData.indexOf("Participant type"), "Input Participant Type");

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

        fileReader.selectFileOpener(currentFilePath);
    }

    /**
     * Processes a workbook and updates identifiers using UniProt results.
     *
     * @param sheetSelected     the name of the sheet to process.
     * @param idColumnIndex      the name of the column containing the ID.
     * @param idDbColumnIndex      column index of the id database.
     * @param organismColumnIndex      the organism column index.
     */
    public void checkAndInsertUniprotResultsWorkbook(String sheetSelected, int idColumnIndex, int idDbColumnIndex, int organismColumnIndex) {
        Workbook workbook = fileReader.getWorkbook();
        String currentFilePath = fileReader.getCurrentFilePath();

        FileOutputStream fileOut = null;
        try {
            Iterator<Row> iterator = fileReader.readWorkbookSheet(sheetSelected);
            Sheet sheet = fileReader.getWorkbook().getSheet(sheetSelected);
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

            //to avoid duplicates
            headerRow.getCell(idColumnIndex).setCellValue("Input participant ID");
            headerRow.getCell(idDbColumnIndex).setCellValue("Input participant ID database");
            headerRow.getCell(organismColumnIndex).setCellValue("Input participant organism");
            for (int i = 1; i < lastCellIndex + 1; i++) {
                if (headerRow.getCell(i).getStringCellValue().equals("Participant name")) {
                    headerRow.getCell(i).setCellValue("Input Participant name");
                } else  if (headerRow.getCell(i).getStringCellValue().equals("Participant type")) {
                    headerRow.getCell(i).setCellValue("Input Participant type");
                }
            }

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

        fileReader.selectFileOpener(currentFilePath);
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
        System.out.println(idColumnIndex + " " + idDbColumnIndex + " " + organismColumnIndex);

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
     * Retrieves a single UniProt ID based on a given identifier, database, and organism.
     * The method gives priority reviewed (Swiss-Prot) entries over unreviewed (TrEMBL) entries.
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

}
