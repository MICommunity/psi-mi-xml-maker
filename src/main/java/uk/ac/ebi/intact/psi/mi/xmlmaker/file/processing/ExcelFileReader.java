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
        }
    }

    public void readXlsxFile(String filePath) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            workbook = new XSSFWorkbook(fileInputStream);
            getSheets();
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
                String[] tokens = line.split(separator, maxSize); // -1 keeps trailing empty strings
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

    public void checkAndInsertUniprotResultsExcel(String sheetSelected, String organismId, String selectedColumn) {
        Sheet sheet = workbook.getSheet(sheetSelected);
        if (sheet == null) {
            xmlMakerutils.showErrorDialog("Sheet not found");
            return;
        }
        int selectedColumnIndex = findColumnIndex(sheet, selectedColumn);
        if (selectedColumnIndex != -1) {
            insertColumnWithUniprotResults(sheet, selectedColumnIndex, organismId);
        } else {
            xmlMakerutils.showErrorDialog("Column not found");
        }
        writeWorkbookToFile(currentFilePath);
    }

    public ArrayList<ArrayList<String>> checkAndInsertUniprotResultsFileSeparatedFormat(String organismId, String selectedColumn) {
        int idInputColumnIndex = 0;
        ArrayList<ArrayList<String>> data = readFileWithSeparator();
        ArrayList<String> header = data.get(0);

        for (int cell = 0; cell < header.size(); cell++) {
            if (header.get(cell).equals(selectedColumn)) {
                idInputColumnIndex = cell;
                break;
            }
        }

        if (header.size() <= idInputColumnIndex + 1 || !header.get(header.size() - 1).equals("UniprotResult")) {
            header.add("Uniprot Ids");
        }

        for (int i = 1; i < data.size(); i++) {
            ArrayList<String> row = data.get(i);
            String uniprotResult = uniprotMapper.fetchUniprotResults(row.get(idInputColumnIndex), organismId);
            row.add(header.size()-1, uniprotResult.isEmpty() ? "null" : uniprotResult);
        }

        xmlMakerutils.showInfoDialog("File processed!");
        writeFileWithSeparator(data);
        return data;
    }

    private void writeWorkbookToFile(String fileUrl) {
        try {
            File inputFile = Paths.get(fileUrl).toFile();
            try (FileOutputStream fileOut = new FileOutputStream(inputFile)) {
                workbook.write(fileOut);
                xmlMakerutils.showInfoDialog("New column inserted with UniProt accession numbers in " + inputFile.getName());
            }
        } catch (Exception e) {
            xmlMakerutils.showErrorDialog("Error writing Excel file");
            throw new RuntimeException(e);
        }
    }

    public void insertColumnWithUniprotResults(Sheet sheet, int columnIndex, String organismId) {
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }
            shiftCellsToTheRight(row, columnIndex);
            Cell previousCell = row.getCell(columnIndex);
            if (previousCell != null) {
                String geneValue = formatter.formatCellValue(previousCell);
                String uniprotResult;
                if (rowIndex == 0) {
                    uniprotResult = "UniprotAc " + geneValue; // geneValue == header cell
                } else {
                    uniprotResult = uniprotMapper.fetchUniprotResults(geneValue, organismId);
                }
                if (!uniprotResult.isEmpty()) {
                    Cell newCell = row.createCell(columnIndex);
                    newCell.setCellValue(uniprotResult);
                    if (moleculeSetChecker.isProteinPartOfMoleculeSet(uniprotResult)) {
                        highlightCells(newCell);
                    }
                } else {
                    row.createCell(columnIndex);
                }
            }
        }
    }

    private void shiftCellsToTheRight(Row row, int columnIndex) {
        for (int colNum = row.getLastCellNum(); colNum > columnIndex; colNum--) {
            Cell oldCell = row.getCell(colNum - 1);
            Cell newCell = row.createCell(colNum);
            if (oldCell != null) {
                cloneCell(oldCell, newCell);
            }
        }
    }

    public static void cloneCell(Cell oldCell, Cell newCell) {
        newCell.setCellStyle(oldCell.getCellStyle());
        switch (oldCell.getCellType()) {
            case STRING:
                newCell.setCellValue(oldCell.getStringCellValue());
                break;
            case NUMERIC:
                newCell.setCellValue(oldCell.getNumericCellValue());
                break;
            case BOOLEAN:
                newCell.setCellValue(oldCell.getBooleanCellValue());
                break;
            case FORMULA:
                newCell.setCellFormula(oldCell.getCellFormula());
                break;
            case ERROR:
                newCell.setCellErrorValue(oldCell.getErrorCellValue());
                break;
            case BLANK:
            default:
                newCell.setBlank();
                break;
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
        } catch (IOException e) {
            xmlMakerutils.showErrorDialog("Error modifying to file: " + e.getMessage());
        }
    }

}
