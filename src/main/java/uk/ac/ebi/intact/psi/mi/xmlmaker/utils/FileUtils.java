package uk.ac.ebi.intact.psi.mi.xmlmaker.utils;

import lombok.Setter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.FileReader;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.DataTypeAndColumn.PARTICIPANT_ROW_INDEX;

public class FileUtils {

    @Setter
    static FileReader fileReader;

    /**
     * Extracts the file extension from a file name.
     *
     * @param fileName The file name.
     * @return The file extension.
     */
    public static String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Extracts the file name (without extension) from a given file name with extension.
     * If the input file name contains an extension (denoted by a period), the method returns
     * the part of the string before the last period. If there is no extension, the method returns
     * the original file name.
     *
     * @param fileNameWithExtension The full file name, including its extension.
     * @return The file name without the extension, or the original file name if no extension is found.
     */
    public static String getFileName(String fileNameWithExtension) {
        if (fileNameWithExtension != null && fileNameWithExtension.contains(".")) {
            return fileNameWithExtension.substring(0, fileNameWithExtension.lastIndexOf("."));
        } else {
            return fileNameWithExtension;
        }
    }

    /**
     * Converts a cell's value to a string representation based on its type.
     *
     * @param cell the cell to process.
     * @return the string representation of the cell's value.
     */
    public static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double numericValue = cell.getNumericCellValue();
                if (numericValue == (long) numericValue) {
                    return String.valueOf((long) numericValue);
                }
                return String.valueOf(numericValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * Retrieves cell values from Excel based on column names stored in the participant map.
     * Useful for feature-related data stored as column references (e.g., "columnA;columnB").
     * Returns the column name in case of column index not found because it is a user input
     *
     * @param key  Key to retrieve the semicolon-separated column references.
     * @param participant         The participant map with metadata.
     * @return                    Concatenated values resolved from the Excel sheet.
     */
    public static String getValueFromFile(String key, Map<String, String> participant) {
        String columns = participant.get(key);

        if (columns == null || columns.trim().isEmpty()) return "";

        String[] columnArray = columns.split(";");
        if (columnArray.length == 0) return "";

        if (columnArray.length == 1) {
            String colName = columnArray[0].trim();
            if (colName.isEmpty()) return "";
            int colIndex = getColumnIndex(colName);
            return colIndex == -1 ? colName : getDataFromRow(colIndex,
                    Integer.parseInt(participant.get(PARTICIPANT_ROW_INDEX.name)));
        }

        return Arrays.stream(columnArray)
                .map(String::trim)
                .filter(c -> !c.isEmpty())
                .map(colName -> {
                    int colIndex = getColumnIndex(colName);
                    if (colIndex == -1) {
                        return colName;
                    }
                    return getDataFromRow(colIndex,
                            Integer.parseInt(participant.get(PARTICIPANT_ROW_INDEX.name)));
                })
                .collect(Collectors.joining(";")) + ";";
    }

    /**
     * Retrieves the index of a column by its name from the selected Excel sheet.
     *
     * @param columnName The name of the column.
     * @return The index of the column, or -1 if not found.
     */
    private static int getColumnIndex(String columnName) {
        List<String> columns = fileReader.getColumns(fileReader.getSheetSelectedUpdate());
        return columns.indexOf(columnName);
    }

    /**
     * Gets the cell value from a specific column and row, supporting both Excel and delimited files.
     *
     * @param columnIndex The index of the column.
     * @param rowIndex    The index of the row.
     * @return The cell value as a string, or an empty string if not found.
     */
    private static String getDataFromRow(int columnIndex, int rowIndex) {
        if (fileReader.getWorkbook() != null) {
            Sheet sheet = fileReader.getWorkbook().getSheet(fileReader.getSheetSelectedUpdate());
            Row row = sheet.getRow(rowIndex);
            if (row == null || columnIndex < 0) {
                return "";
            }
            Cell cell = row.getCell(columnIndex);
            if (cell != null) {
                return FileUtils.getCellValueAsString(cell);
            } else {
                return "";
            }
        } else if (fileReader.readFileWithSeparator() != null) {
            Iterator<List<String>> iterator = fileReader.readFileWithSeparator();
            int currentRow = 0;
            while (iterator.hasNext()) {
                List<String> row = iterator.next();
                if (currentRow == rowIndex - 1) { // -1 because the indexing works differently for files with separator
                    if (columnIndex >= 0 && columnIndex < row.size()) {
                        return row.get(columnIndex);
                    } else {
                        return "";
                    }
                }
                currentRow++;
            }
        }
        return "";
    }
}
