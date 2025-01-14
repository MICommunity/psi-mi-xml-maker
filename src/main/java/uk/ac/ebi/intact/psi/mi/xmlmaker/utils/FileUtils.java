package uk.ac.ebi.intact.psi.mi.xmlmaker.utils;

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
}
