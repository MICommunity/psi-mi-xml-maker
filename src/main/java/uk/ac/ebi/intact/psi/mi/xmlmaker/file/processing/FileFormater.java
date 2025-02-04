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
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.FileUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

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

    @Getter
    private final List<List<String>> newFormat = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(FileFormater.class.getName());

    @Setter
    private Map<String, String> interactionData = new HashMap<>();
    private String[] header = {"Interaction Number", "Input Participant ID", "Input Participant Name",
            "Experimental role", "Input Participant ID database",
            "Interaction detection method", "Participant identification method", "Experimental preparation",
            "Biological role", "Participant organism", "Feature type", "Feature start", "Feature end",};

    private final Map<String, Integer> participantCountMap = new HashMap<>();

    /**
     * Constructs a FileFormater object with an ExcelFileReader.
     *
     * @param excelFileReader The Excel file reader used to read input files.
     */
    public FileFormater(ExcelFileReader excelFileReader) {
        this.excelFileReader = excelFileReader;
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
                formatExcelFile(baitColumnIndex, preyColumnIndex, sheetSelected, binary, baitNameColumnIndex, preyNameColumnIndex);
                Workbook workbookXlsx = new XSSFWorkbook();
                postProcessInteractions();
                writeToExcel(newFormat, modifiedFileName + ".xlsx", workbookXlsx);
                newFileName = modifiedFileName + ".xlsx";
                break;
            case "xls":
                LOGGER.info("Reading xls file: " + fileName);
                formatExcelFile(baitColumnIndex, preyColumnIndex, sheetSelected, binary, baitNameColumnIndex, preyNameColumnIndex);
                Workbook workbookXls = new HSSFWorkbook();
                postProcessInteractions();
                writeToExcel(newFormat, modifiedFileName + ".xls", workbookXls);
                newFileName = modifiedFileName + ".xls";
                break;
            case "csv":
                LOGGER.info("Reading csv file: " + fileName);
                formatSeparatedFormatFile(baitColumnIndex, preyColumnIndex, binary, baitNameColumnIndex, preyNameColumnIndex);
                postProcessInteractions();
                writeToFile(newFormat, modifiedFileName + ".csv", ',');
                newFileName = modifiedFileName + ".csv";
                break;
            case "tsv":
                LOGGER.info("Reading tsv file: " + fileName);
                formatSeparatedFormatFile(baitColumnIndex, preyColumnIndex, binary, baitNameColumnIndex, preyNameColumnIndex);
                postProcessInteractions();
                writeToFile(newFormat, modifiedFileName + ".tsv", '\t');
                newFileName = modifiedFileName + ".tsv";
                break;
            default:
                LOGGER.warning("Unsupported file format: " + fileType);
                XmlMakerUtils.showErrorDialog("Unsupported file format! Supported formats: .csv, .tsv, .xls, .xlsx");
        }
        newFormat.clear();
        XmlMakerUtils.showInfoDialog("File modified: " + modifiedFileName);
        excelFileReader.selectFileOpener(newFileName);
    }

    /**
     * Reads and formats a CSV or TSV file by extracting bait and prey interactions.
     *
     * @param baitColumnIndex The column index for the bait.
     * @param preyColumnIndex The column index for the prey.
     * @param binary          Indicates whether the interactions should be formatted in binary mode.
     */
    public void formatSeparatedFormatFile(int baitColumnIndex,
                                          int preyColumnIndex,
                                          boolean binary,
                                          int baitNameColumnIndex,
                                          int preyNameColumnIndex) {
        Iterator<List<String>> iterator = excelFileReader.readFileWithSeparator();

        int interactionNumber = 0;
        String lastBait = null;

        if (binary) {
            while (iterator.hasNext()) {
                List<String> row = iterator.next();
                String bait = row.get(baitColumnIndex);
                String baitName = row.get(baitNameColumnIndex);
                String prey = row.get(preyColumnIndex);
                String preyName = row.get(preyNameColumnIndex);
                interactionNumber++;
                addNewParticipant(String.valueOf(interactionNumber), bait, baitName, "bait");
                addNewParticipant(String.valueOf(interactionNumber), prey, preyName, "prey");
            }
        }

        while (iterator.hasNext()) {
            List<String> row = iterator.next();
            String bait = row.get(baitColumnIndex);
            String baitName = row.get(baitNameColumnIndex);
            String prey = row.get(preyColumnIndex);
            String preyName = row.get(preyNameColumnIndex);
            if (lastBait == null || !lastBait.equals(bait)) {
                interactionNumber++;
                lastBait = bait;
                addNewParticipant(String.valueOf(interactionNumber), prey, preyName, "prey");
                addNewParticipant(String.valueOf(interactionNumber), bait, baitName,"bait");
            } else {
                lastBait = bait;
                addNewParticipant(String.valueOf(interactionNumber), prey, preyName, "prey");
            }
        }

        iterator.remove();
    }

    /**
     * Reads and formats an Excel file by extracting bait and prey interactions.
     *
     * @param baitColumnIndex The column index for the bait.
     * @param preyColumnIndex The column index for the prey.
     * @param sheetSelected   The name of the sheet to be processed.
     * @param binary          Indicates whether the interactions should be formatted in binary mode.
     */
    public void formatExcelFile(int baitColumnIndex,
                                int preyColumnIndex,
                                String sheetSelected,
                                boolean binary,
                                int baitNameColumnIndex,
                                int preyNameColumnIndex) {
        Iterator<Row> iterator = excelFileReader.readWorkbookSheet(sheetSelected);

        int interactionNumber = 0;
        String lastBait = null;

        while (iterator.hasNext()) {
            Row row = iterator.next();
            String bait = FileUtils.getCellValueAsString(row.getCell(baitColumnIndex));
            String baitName = FileUtils.getCellValueAsString(row.getCell(baitNameColumnIndex));
            String prey = FileUtils.getCellValueAsString(row.getCell(preyColumnIndex));
            String preyName = FileUtils.getCellValueAsString(row.getCell(preyNameColumnIndex));


            if (binary) {
                interactionNumber++;
                addNewParticipant(String.valueOf(interactionNumber), bait, baitName, "bait");
                addNewParticipant(String.valueOf(interactionNumber), prey, preyName, "prey");
            } else {
                if (lastBait == null || !lastBait.equals(bait)) {
                    interactionNumber++;
                    lastBait = bait;
                    addNewParticipant(String.valueOf(interactionNumber), bait, baitName, "bait");
                }
                addNewParticipant(String.valueOf(interactionNumber), prey, preyName, "prey");
            }
        }
        iterator.remove();
    }

    /**
     * Adds a new participantId to the formatted data structure.
     *
     * @param interactionNumber The interaction number associated with the participantId.
     * @param participantId       The participantId's identifier.
     * @param participantType   The type of participantId (e.g., "bait" or "prey").
     */
    public void addNewParticipant(String interactionNumber, String participantId, String participantName, String participantType) {
        List<String> newParticipant = new ArrayList<>();

        newParticipant.add(interactionNumber);
        newParticipant.add(participantId);
        newParticipant.add(participantName);
        newParticipant.add(participantType);

        participantCountMap.put(interactionNumber, participantCountMap.getOrDefault(interactionNumber, 0) + 1);

        for (DataForRawFile data : DataForRawFile.values()) {
            if (data.isCommon) {
                newParticipant.add(interactionData.get(data.name));
            } else {
                if ("bait".equalsIgnoreCase(participantType) && data.isBait) {
                    newParticipant.add(interactionData.get(data.name));
                } else if ("prey".equalsIgnoreCase(participantType) && !data.isBait) {
                    newParticipant.add(interactionData.get(data.name));
                }
            }
        }
        newFormat.add(newParticipant);
    }

    /**
     * Writes formatted interaction data to a CSV or TSV file.
     *
     * @param data      The formatted data to be written.
     * @param filePath  The path of the output file.
     * @param delimiter The character used to separate values (e.g., ',' for CSV or '\t' for TSV).
     */
    public void writeToFile(List<List<String>> data, String filePath, char delimiter) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath),
                delimiter,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {
            writer.writeNext(header);
            for (List<String> row : data) {
                writer.writeNext(row.toArray(new String[0]));
            }
        } catch (IOException e) {
            LOGGER.warning("Error writing to file: " + e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes formatted interaction data to an Excel file.
     *
     * @param data     The formatted data to be written.
     * @param filePath The path of the output file.
     * @param workbook The workbook to which data should be written.
     */
    public void writeToExcel(List<List<String>> data, String filePath, Workbook workbook) {
        Sheet sheet = workbook.createSheet("Formatted data");

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < header.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(header[i]);
        }

        for (int i = 0; i < data.size(); i++) {
            Row row = sheet.createRow(i + 1);
            List<String> rowData = data.get(i);

            for (int j = 0; j < rowData.size(); j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(rowData.get(j));
            }
        }
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        } catch (IOException e) {
            LOGGER.warning("Error writing to file: " + e);
            throw new RuntimeException(e);
        }
        try {
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Post-processes the formatted interaction data by adding an "Interaction Type" column.
     * If an interaction involves more than two participants, it is classified as an "association";
     * otherwise, it is classified as a "physical interaction."
     */
    public void postProcessInteractions() {
        String[] extendedHeader = Arrays.copyOf(header, header.length + 1);
        extendedHeader[extendedHeader.length - 1] = "Interaction Type";
        header = extendedHeader;

        int expectedColumns = header.length - 1;

        for (List<String> row : newFormat) {
            while (row.size() < expectedColumns) {
                row.add("");
            }

            String interactionNumber = row.get(0);
            int count = participantCountMap.getOrDefault(interactionNumber, 0);
            if (count > 2) {
                row.add("association");
            } else {
                row.add("physical interaction");
            }
        }
    }
}
