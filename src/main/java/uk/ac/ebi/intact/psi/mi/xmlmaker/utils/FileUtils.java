package uk.ac.ebi.intact.psi.mi.xmlmaker.utils;

import org.apache.poi.ss.usermodel.Cell;

public class FileUtils {
    /**
     * Extracts the file extension from a file name.
     *
     * @param fileName The file name.
     * @return The file extension.
     */
    public static String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

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
}
