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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExcelFileReader {

    private static final Logger LOGGER = Logger.getLogger(ExcelFileReader.class.getName());
    private final MoleculeSetChecker moleculeSetChecker = new MoleculeSetChecker();
    private final DataFormatter formatter = new DataFormatter();
    private final UniprotMapper uniprotMapper = new UniprotMapper();
    private final XmlMakerUtils xmlMakerutils = new XmlMakerUtils();

    public String currentFilePath = null;
    public String fileName;
    public String fileType;
    public char separator;
    @Getter
    @Setter
    public String publicationId;

    public final JLabel currentFileLabel;

    public final List<String> sheets = new ArrayList<>();
    public final List<String> columns = new ArrayList<>();
    public Workbook workbook;
    public List<List<String>> fileData;
    public final List<String> proteinsPartOfMoleculeSet = new ArrayList<>();

    public ExcelFileReader() {
        this.fileName = null;
        this.currentFileLabel = new JLabel("No file selected");
    }

//    OPEN AND READ THE FILES

    public void selectFileOpener(String filePath) {
        fileName = new File(filePath).getName();
        fileType = getFileExtension(filePath);
        currentFileLabel.setText(getFileName());
        currentFilePath = filePath;

        switch (fileType) {
            case "xlsx":
                LOGGER.info("Reading xlsx file: " + fileName);
                try {
                    readXlsxFile(filePath);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Unable to read xlsx file: " + e.getMessage(), e);
                }
                break;
            case "xls":
                LOGGER.info("Reading xls file: " + fileName);
                try {
                    readXlsFile(filePath);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Unable to read xls file: " + e.getMessage(), e);
                }
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
                JOptionPane.showMessageDialog(null, "Unsupported file format! Please provide a valid file format: .csv, .tsv, .xls, .xlsx", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    public void readXlsFile(String filePath) throws IOException {
        try (POIFSFileSystem poifsFileSystem = new POIFSFileSystem(new File(filePath))) {
            workbook = new HSSFWorkbook(poifsFileSystem);
            getSheets();
            fileData.clear();
        }
    }

    public void readXlsxFile(String filePath) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            workbook = new XSSFWorkbook(fileInputStream);
            getSheets();
            fileData = null;
        }
    }

    public List<List<String>> readFileWithSeparator() {
        List<List<String>> data = new ArrayList<>();

        try (CSVReader csvReader = new CSVReaderBuilder(new FileReader(currentFilePath))
                .withCSVParser(new com.opencsv.CSVParserBuilder()
                        .withSeparator(separator)
                        .withIgnoreQuotations(false)
                        .build())
                .build()) {

            String[] nextLine;
            while (true) {
                try {
                    if ((nextLine = csvReader.readNext()) == null) break;
                } catch (CsvValidationException e) {
                    throw new RuntimeException(e);
                }
                List<String> lineCells = new ArrayList<>();
                for (String cell : nextLine) {
                    lineCells.add(cell == null ? "" : cell);
                }
                data.add(lineCells);
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to read file with separator: " + e.getMessage(), e);
        }

        fileData = data;
        return data;
    }

//    GETTERS FOR THE GUI

    public List<String> getSheets() {
        sheets.clear();
        if (workbook != null) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheets.add(workbook.getSheetName(i));
            }
        }
        return sheets;
    }

    public List<String> getColumns() {
        columns.clear();

        if (fileType.equals("xlsx") || fileType.equals("xls")) {
            if (fileData == null || fileData.isEmpty()) {
                LOGGER.warning("No file data loaded.");
                return columns;
            }List<String> headerRow = fileData.get(0);
            if (headerRow != null) {
                columns.addAll(headerRow);
            }
        } else if (fileType.equals("csv") || fileType.equals("tsv")) {
            if (fileData == null || fileData.isEmpty()) {
                LOGGER.warning("No file data loaded.");
                return columns;
            }
            List<String> headerRow = fileData.get(0);
            if (headerRow != null) {
                columns.addAll(headerRow);
            }
        }

        return columns;
    }

    public JLabel getFileLabel() {
        currentFileLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        currentFileLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return currentFileLabel;
    }

    public String getFileName() {
        return fileName != null ? "Current file: " + fileName : "No file selected";
    }

    //    MODIFY AND WRITE FILES

//    TODO: cleanup and extract those function in the ExcelFileWriter.java

    public void checkAndInsertUniprotResultsExcel(String sheetSelected, String selectedColumn,
                                                  int organismColumnIndex, int idDbColumnIndex) {
        Sheet sheet = workbook.getSheet(sheetSelected);
        if (sheet == null) {
            xmlMakerutils.showErrorDialog("Sheet not found");
            return;
        }
        int idColumnIndex = findColumnIndex(sheet, selectedColumn);
        if (idColumnIndex != -1) {
            insertColumnWithUniprotResults(sheet, idColumnIndex, organismColumnIndex, idDbColumnIndex);
        } else {
            xmlMakerutils.showErrorDialog("Column not found");
        }
        writeWorkbookToFile();
    }

    public void checkAndInsertUniprotResultsFileSeparatedFormat(String idColumnIndex, int organismColumnIndex,
                                                                int idDbColumnIndex) {
        //TODO: check indexes with the new library
        int idInputColumnIndex = 0;
        List<List<String>> data = readFileWithSeparator();
        List<String> header = data.get(0);

        for (int cell = 0; cell < header.size(); cell++) {
            if (header.get(cell).equals(idColumnIndex)) {
                idInputColumnIndex = cell;
                break;
            }
        }

        for (List<String> row : data) {
            String currentValue = row.get(idInputColumnIndex);
            String uniprotResult = uniprotMapper.fetchUniprotResults(
                    currentValue,
                    row.get(organismColumnIndex),
                    row.get(idDbColumnIndex)
            );

            if (uniprotResult != null && !uniprotResult.isEmpty()) {
                row.set(idInputColumnIndex, uniprotResult);
                if (moleculeSetChecker.isProteinPartOfMoleculeSet(uniprotResult)) {
                    proteinsPartOfMoleculeSet.add(uniprotResult);
                }
            }
        }

        writeFileWithSeparator(data);
    }

    private void writeWorkbookToFile() {
        try {
            File inputFile = Paths.get(currentFilePath).toFile();
            try (FileOutputStream fileOut = new FileOutputStream(inputFile)) {
                workbook.write(fileOut);
                xmlMakerutils.showInfoDialog("File modified successfully.");
                selectFileOpener(currentFilePath);
            }
        } catch (Exception e) {
            xmlMakerutils.showErrorDialog("Error writing Excel file");
            throw new RuntimeException(e);
        }
    }

    public void insertColumnWithUniprotResults(Sheet sheet, int idColumnIndex,
                                               int organismColumnIndex, int idDbColumnIndex) {
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }
            Cell previousCell = row.getCell(idColumnIndex);

            int organismId = Integer.parseInt(xmlMakerutils.fetchTaxIdForOrganism(row.getCell(organismColumnIndex).getStringCellValue()));
            String organism = String.valueOf(organismId);
            String idDb = row.getCell(idDbColumnIndex).getStringCellValue();

            if (previousCell != null) {
                String geneValue = formatter.formatCellValue(previousCell);
                String uniprotResult;
                if (rowIndex == 0) {
                        uniprotResult = "Updated " + geneValue; // geneValue == header cell
                } else {
                    uniprotResult = uniprotMapper.fetchUniprotResults(geneValue, organism, idDb);
                }
                if (uniprotResult != null && !uniprotResult.isEmpty()) {
                    previousCell.setCellValue(uniprotResult);
                    if (moleculeSetChecker.isProteinPartOfMoleculeSet(uniprotResult)) {
                        highlightCells(previousCell);
                        proteinsPartOfMoleculeSet.add(uniprotResult);
                    }
                }
            }
        }
    }

    public void highlightCells(Cell cell) {
        if (moleculeSetChecker.isProteinPartOfMoleculeSet(cell.getStringCellValue())) {
            CellStyle cellStyle = cell.getSheet().getWorkbook().createCellStyle();
            cellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(cellStyle);
        }
    }

    private int findColumnIndex(Sheet sheet, String selectedColumn) {
        Row headerRow = sheet.getRow(0);
        for (Cell cell : headerRow) {
            if (formatter.formatCellValue(cell).contains(selectedColumn)) {
                return cell.getColumnIndex();
            }
        }
        return -1;
    }

    private void writeFileWithSeparator(List<List<String>> data) {
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(currentFilePath), separator, '"',
                '"', "\n")) {
            for (List<String> row : data) {
                String[] rowArray = row.toArray(new String[0]);
                csvWriter.writeNext(rowArray);
            }

            xmlMakerutils.showInfoDialog("File modified successfully.");
            selectFileOpener(currentFilePath);

        } catch (IOException e) {
            xmlMakerutils.showErrorDialog("Error modifying file: " + e.getMessage());
        }
    }

}
