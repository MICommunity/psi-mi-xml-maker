//package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;
//
//import org.apache.poi.ss.usermodel.*;
//import uk.ac.ebi.intact.psi.mi.xmlmaker.XmlMakerUtils;
//import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.MoleculeSetChecker;
//import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapper;
//import java.io.*;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.logging.Logger;
//
//public class ExcelFileWriter {
//
//    private static final Logger LOGGER = Logger.getLogger(ExcelFileWriter.class.getName());
//    private final Workbook workbook;
//    private final XmlMakerUtils xmlMakerUtils = new XmlMakerUtils();
//    private final UniprotMapper uniprotMapper;
//    private final MoleculeSetChecker moleculeSetChecker;
//    private final DataFormatter formatter = new DataFormatter();
//    private final String separator;
//    private final String currentFilePath;
//
//    public ExcelFileWriter(Workbook workbook, UniprotMapper uniprotMapper, MoleculeSetChecker moleculeSetChecker, String separator, String currentFilePath) {
//        this.workbook = workbook;
//        this.uniprotMapper = uniprotMapper;
//        this.moleculeSetChecker = moleculeSetChecker;
//        this.separator = separator;
//        this.currentFilePath = currentFilePath;
//    }
//
//    public void checkAndInsertUniprotResultsExcel(String sheetSelected, String organismId, String selectedColumn, String currentFilePath) {
//        Sheet sheet = workbook.getSheet(sheetSelected);
//        if (sheet == null) {
//            xmlMakerUtils.showErrorDialog("Sheet not found");
//            return;
//        }
//        int selectedColumnIndex = findColumnIndex(sheet, selectedColumn);
//        if (selectedColumnIndex != -1) {
//            insertColumnWithUniprotResults(sheet, selectedColumnIndex, organismId);
//        } else {
//            xmlMakerUtils.showErrorDialog("Column not found");
//        }
//        writeWorkbookToFile(currentFilePath);
//    }
//
//    public ArrayList<ArrayList<String>> checkAndInsertUniprotResultsFileSeparatedFormat(String organismId, String selectedColumn, ArrayList<ArrayList<String>> data){
//        int idInputColumnIndex = 0;
//        ArrayList<String> header = data.get(0);
//
//        for (int cell = 0; cell < header.size(); cell++) {
//            if (header.get(cell).equals(selectedColumn)) {
//                idInputColumnIndex = cell;
//                break;
//            }
//        }
//
//        if (header.size() <= idInputColumnIndex + 1 || !header.get(header.size() - 1).equals("UniprotResult")) {
//            header.add("Uniprot Ids");
//        }
//
//        for (int i = 1; i < data.size(); i++) {
//            ArrayList<String> row = data.get(i);
//            String uniprotResult = uniprotMapper.fetchUniprotResults(row.get(idInputColumnIndex), organismId);
//            row.add(header.size() - 1, uniprotResult.isEmpty() ? "null" : uniprotResult);
//        }
//
//        xmlMakerUtils.showInfoDialog("File processed!");
//        writeFileWithSeparator(data);
//        return data;
//    }
//
//    private void insertColumnWithUniprotResults(Sheet sheet, int columnIndex, String organismId) {
//        System.out.println("insert column with uniprot results");
//        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
//            Row row = sheet.getRow(rowIndex);
//            if (row == null) {
//                row = sheet.createRow(rowIndex);
//            }
//            shiftCellsToTheRight(row, columnIndex);
//            Cell previousCell = row.getCell(columnIndex);
//            if (previousCell != null) {
//                String geneValue = formatter.formatCellValue(previousCell);
//                String uniprotResult;
//                if (rowIndex == 0) {
//                    uniprotResult = "UniprotAc " + geneValue;
//                } else {
//                    uniprotResult = uniprotMapper.fetchUniprotResults(geneValue, organismId);
//                }
//                if (!uniprotResult.isEmpty()) {
//                    Cell newCell = row.createCell(columnIndex);
//                    newCell.setCellValue(uniprotResult);
//                    if (moleculeSetChecker.isProteinPartOfMoleculeSet(uniprotResult)) {
//                        highlightCells(newCell);
//                    }
//                } else {
//                    row.createCell(columnIndex);
//                }
//            }
//        }
//    }
//
//    private void shiftCellsToTheRight(Row row, int columnIndex) {
//        for (int colNum = row.getLastCellNum(); colNum > columnIndex; colNum--) {
//            Cell oldCell = row.getCell(colNum - 1);
//            Cell newCell = row.createCell(colNum);
//            if (oldCell != null) {
//                cloneCell(oldCell, newCell);
//            }
//        }
//    }
//
//    public static void cloneCell(Cell oldCell, Cell newCell) {
//        newCell.setCellStyle(oldCell.getCellStyle());
//        switch (oldCell.getCellType()) {
//            case STRING:
//                newCell.setCellValue(oldCell.getStringCellValue());
//                break;
//            case NUMERIC:
//                newCell.setCellValue(oldCell.getNumericCellValue());
//                break;
//            case BOOLEAN:
//                newCell.setCellValue(oldCell.getBooleanCellValue());
//                break;
//            case FORMULA:
//                newCell.setCellFormula(oldCell.getCellFormula());
//                break;
//            case ERROR:
//                newCell.setCellErrorValue(oldCell.getErrorCellValue());
//                break;
//            case BLANK:
//            default:
//                newCell.setBlank();
//                break;
//        }
//    }
//
//    private void highlightCells(Cell cell) {
//        if (moleculeSetChecker.isProteinPartOfMoleculeSet(cell.getStringCellValue())) {
//            CellStyle cellStyle = cell.getSheet().getWorkbook().createCellStyle();
//            cellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
//            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//            cell.setCellStyle(cellStyle);
//        }
//    }
//
//    private int findColumnIndex(Sheet sheet, String selectedColumn) {
//        Row headerRow = sheet.getRow(0);
//        for (Cell cell : headerRow) {
//            if (formatter.formatCellValue(cell).contains(selectedColumn)) {
//                return cell.getColumnIndex();
//            }
//        }
//        return -1;
//    }
//
//    private void writeWorkbookToFile(String fileUrl) {
//        try {
//            File inputFile = Paths.get(fileUrl).toFile();
//            try (FileOutputStream fileOut = new FileOutputStream(inputFile)) {
//                workbook.write(fileOut);
//                xmlMakerUtils.showInfoDialog("New column inserted with UniProt accession numbers in " + inputFile.getName());
//            }
//        } catch (Exception e) {
//            xmlMakerUtils.showErrorDialog("Error writing Excel file");
//            throw new RuntimeException(e);
//        }
//    }
//
//    private void writeFileWithSeparator(ArrayList<ArrayList<String>> data) {
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFilePath))) {
//            for (ArrayList<String> row : data) {
//                writer.write(String.join(separator, row));
//                writer.newLine();
//            }
//            xmlMakerUtils.showInfoDialog("File modified successfully.");
//        } catch (IOException e) {
//            xmlMakerUtils.showErrorDialog("Error modifying file: " + e.getMessage());
//        }
//    }
//}
