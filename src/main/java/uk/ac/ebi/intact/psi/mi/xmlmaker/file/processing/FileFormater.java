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

public class FileFormater {
    final ExcelFileReader excelFileReader;

    @Getter
    private final List<List<String>> newFormat = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(FileFormater.class.getName());

    @Setter
    private Map<String, String> interactionData = new HashMap<>();
    private String[] header = {"Interaction Number", "Participant", "Experimental role",
            "Interaction detection method", "Participant detection method", "Experimental preparation",
            "Biological role", "Participant organism", "Feature", "Feature start", "Feature end",};

    private final Map<String, Integer> participantCountMap = new HashMap<>();


    public FileFormater(ExcelFileReader excelFileReader) {
        this.excelFileReader = excelFileReader;
    }

    public void selectFileFormater(String filePath, int baitColumnIndex, int preyColumnIndex,
                                   String sheetSelected, boolean binary) {
            File file = new File(filePath);
            String fileType = FileUtils.getFileExtension(filePath);
            String fileName = file.getName();
            String modifiedFileName =  FileUtils.getFileName(filePath) + "_xmlMakerFormatted";
            String newFileName ="";

        switch (fileType) {
            case "xlsx":
                LOGGER.info("Reading xlsx file: " + fileName);
                formatExcelFile(baitColumnIndex, preyColumnIndex, sheetSelected, binary);
                Workbook workbookXlsx = new XSSFWorkbook();
                postProcessInteractions();
                writeToExcel(newFormat, modifiedFileName + ".xlsx", workbookXlsx);
                newFileName = modifiedFileName + ".xlsx";
                break;
            case "xls":
                LOGGER.info("Reading xls file: " + fileName);
                formatExcelFile(baitColumnIndex, preyColumnIndex, sheetSelected, binary);
                Workbook workbookXls = new HSSFWorkbook();
                postProcessInteractions();
                writeToExcel(newFormat, modifiedFileName + ".xls", workbookXls);
                newFileName = modifiedFileName + ".xls";
                break;
            case "csv":
                LOGGER.info("Reading csv file: " + fileName);
                formatSeparatedFormatFile(baitColumnIndex, preyColumnIndex, binary);
                postProcessInteractions();
                writeToFile(newFormat, modifiedFileName + ".csv", ',');
                newFileName = modifiedFileName + ".csv";
                break;
            case "tsv":
                LOGGER.info("Reading tsv file: " + fileName);
                formatSeparatedFormatFile(baitColumnIndex, preyColumnIndex, binary);
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

    public void formatSeparatedFormatFile(int baitColumnIndex, int preyColumnIndex, boolean binary) {
        Iterator<List<String>> iterator = excelFileReader.readFileWithSeparator();

        int interactionNumber = 0;
        String lastBait = null;

        if (binary) {
            while (iterator.hasNext()) {
                List<String> row = iterator.next();
                String bait = row.get(baitColumnIndex);
                String prey = row.get(preyColumnIndex);
                interactionNumber++;
                addNewParticipant(String.valueOf(interactionNumber), bait, "bait");
                addNewParticipant(String.valueOf(interactionNumber), prey, "prey");
            }
        }

        while (iterator.hasNext()) {
            List<String> row = iterator.next();
            String bait = row.get(baitColumnIndex);
            String prey = row.get(preyColumnIndex);
            if (lastBait == null || !lastBait.equals(bait)) {
                interactionNumber++;
                lastBait = bait;
                addNewParticipant(String.valueOf(interactionNumber), prey, "prey");
                addNewParticipant(String.valueOf(interactionNumber), bait, "bait");
            } else {
                lastBait = bait;
                addNewParticipant(String.valueOf(interactionNumber), prey, "prey");
            }
        }

        iterator.remove();
    }

    public void formatExcelFile(int baitColumnIndex, int preyColumnIndex, String sheetSelected, boolean binary) {
        Iterator<Row> iterator = excelFileReader.readWorkbookSheet(sheetSelected);

        int interactionNumber = 0;
        String lastBait = null;

        while (iterator.hasNext()) {
            Row row = iterator.next();
            String bait = FileUtils.getCellValueAsString(row.getCell(baitColumnIndex));
            String prey = FileUtils.getCellValueAsString(row.getCell(preyColumnIndex));

            if (binary) {
                interactionNumber++;
                addNewParticipant(String.valueOf(interactionNumber), bait, "bait");
                addNewParticipant(String.valueOf(interactionNumber), prey, "prey");
            } else {
                if (lastBait == null || !lastBait.equals(bait)) {
                    interactionNumber++;
                    lastBait = bait;
                    addNewParticipant(String.valueOf(interactionNumber), bait, "bait");
                }
                addNewParticipant(String.valueOf(interactionNumber), prey, "prey");
            }
        }
        iterator.remove();
    }

    public void addNewParticipant(String interactionNumber, String participant, String participantType) {
        List<String> newParticipant = new ArrayList<>();

        newParticipant.add(interactionNumber);
        newParticipant.add(participant);
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

    public void postProcessInteractions() {
        String[] extendedHeader = Arrays.copyOf(header, header.length + 1);
        extendedHeader[extendedHeader.length - 1] = "Interaction Type";
        header = extendedHeader;

        int expectedColumns = header.length - 1; // Before adding the new column

        for (List<String> row : newFormat) {
            // Ensure the row has enough columns
            while (row.size() < expectedColumns) {
                row.add(""); // Add empty values for missing columns
            }

            // Determine interaction type
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
