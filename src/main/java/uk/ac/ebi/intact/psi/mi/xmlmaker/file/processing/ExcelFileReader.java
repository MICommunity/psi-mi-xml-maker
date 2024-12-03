package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import uk.ac.ebi.intact.psi.mi.xmlmaker.XmlMakerUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.MoleculeSetChecker;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapper;

import javax.swing.*;
import java.awt.*;
import java.awt.Font;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
    public String separator = null;

    public JLabel currentFileLabel;

    public ArrayList<String> sheets = new ArrayList<>();
    public ArrayList<String> columns = new ArrayList<>();
    public Workbook workbook;
    public ArrayList<ArrayList<String>> fileData;

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
                separator = null;
                break;
            case "xls":
                LOGGER.info("Reading xls file: " + fileName);
                try {
                    readXlsFile(filePath);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Unable to read xls file: " + e.getMessage(), e);
                }
                separator = null;
                break;
            case "csv":
                LOGGER.info("Reading csv file: " + fileName);
                separator = ",";
                readFileWithSeparator();
                break;
            case "tsv":
                LOGGER.info("Reading tsv file: " + fileName);
                separator = "\t";
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
            fileData.clear();
        }
    }

    public ArrayList<ArrayList<String>> readFileWithSeparator() {
        ArrayList<ArrayList<String>> data = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(currentFilePath))) {
            String line;
            boolean firstLine = true;
            int maxSize = 0;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    String[] tokens = line.split(separator, -1);
                    maxSize = tokens.length; //TODO: check other ways to do that because if they are commas in the values it causes issues
                    firstLine = false;
                }
                String[] tokens = line.split(separator, maxSize);
                ArrayList<String> lineCells = new ArrayList<>(Arrays.asList(tokens));
                if (tokens.length < maxSize) {
                    for (int i = tokens.length; i < maxSize; i++) {
                        lineCells.add(" ");
                    }
                }
                data.add(lineCells);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to read file with separator: " + e.getMessage(), e);
        }
        workbook = null;
        fileData = data;
        return data;
    }

//    GETTERS FOR THE GUI

    public ArrayList<String> getSheets() {
        sheets.clear();
        if (workbook != null) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheets.add(workbook.getSheetName(i));
            }
        }
        return sheets;
    }

    public ArrayList<String> getColumns(String sheetName) {
        columns.clear();

        if (fileType.equals("xlsx") || fileType.equals("xls")) {
            if (workbook == null) {
                LOGGER.warning("No workbook loaded.");
                return columns;
            }
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                LOGGER.warning("Sheet not found: " + sheetName);
                return columns;
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                for (Cell cell : headerRow) {
                    columns.add(cell.getStringCellValue());
                }
            }
        } else if (fileType.equals("csv") || fileType.equals("tsv")) {
            String separator = fileType.equals("csv") ? "," : "\t";
            try (BufferedReader reader = new BufferedReader(new FileReader(currentFilePath))) {
                String headerLine = reader.readLine();
                if (headerLine != null) {
                    String[] headers = headerLine.split(separator);
                    for (String header : headers) {
                        columns.add(header.trim());
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Unable to read file with separator: " + e.getMessage(), e);
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

    public void checkAndInsertUniprotResultsFileSeparatedFormat(
            String idColumnIndex, int organismColumnIndex, int idDbColumnIndex) {
        int idInputColumnIndex = 0;
        ArrayList<ArrayList<String>> data = readFileWithSeparator();
        ArrayList<String> header = data.get(0);

        for (int cell = 0; cell < header.size(); cell++) {
            if (header.get(cell).equals(idColumnIndex)) {
                idInputColumnIndex = cell;
                break;
            }
        }

        for (int i = 1; i < data.size(); i++) {
            ArrayList<String> row = data.get(i);
            String currentValue = row.get(idInputColumnIndex);
            String uniprotResult = uniprotMapper.fetchUniprotResults(
                    currentValue,
                    row.get(organismColumnIndex),
                    row.get(idDbColumnIndex)
            );

            if (uniprotResult != null && !uniprotResult.isEmpty()) {
                row.set(idInputColumnIndex, uniprotResult);
            }
            else {
                continue;
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

            String organism = organismTaxIdFormatter(row.getCell(organismColumnIndex).getStringCellValue()); //TODO: use taxID
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
                    }
                } else {
                    continue;
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

    private void writeFileWithSeparator(ArrayList<ArrayList<String>> data){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFilePath))) {
            for (ArrayList<String> row : data) {
                writer.write(String.join(separator, row));
                writer.newLine();
            }
            xmlMakerutils.showInfoDialog("File modified successfully.");
            selectFileOpener(currentFilePath);
        } catch (IOException e) {
            xmlMakerutils.showErrorDialog("Error modifying to file: " + e.getMessage());
        }
    }

    public String organismTaxIdFormatter(String organism) {
        String[] parts = organism.split("/");
        if (parts.length > 1) {
            return parts[1].trim();
        }
        return "null";
    }
}
