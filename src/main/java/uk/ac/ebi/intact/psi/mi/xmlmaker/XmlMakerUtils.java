package uk.ac.ebi.intact.psi.mi.xmlmaker;

import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;

import javax.swing.*;

public class XmlMakerUtils {
    public static void processFile(String filePath){
        ExcelFileReader excelFileReader = new ExcelFileReader();
        excelFileReader.selectFileOpener(filePath);
    }

    public void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(new JFrame(), message, "ERROR", JOptionPane.ERROR_MESSAGE);
    }

    public void showInfoDialog(String message) {
        JOptionPane.showMessageDialog(new JFrame(), message, "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
    }
}
