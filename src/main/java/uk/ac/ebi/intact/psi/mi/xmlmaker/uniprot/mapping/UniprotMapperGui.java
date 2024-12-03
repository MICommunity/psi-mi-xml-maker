package uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping;

import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;

import javax.swing.*;
import java.awt.*;

public class UniprotMapperGui extends JPanel {
    private final JComboBox<String> sheets = new JComboBox<>();
    private final JComboBox<String> idColumn = new JComboBox<>();
    private final JComboBox<String> organismColumn = new JComboBox<>();
    private final JComboBox<String> idDbColumn = new JComboBox<>();

    private final ExcelFileReader excelFileReader;

    public UniprotMapperGui(ExcelFileReader excelFileReader) {
        this.excelFileReader = excelFileReader;
        setUpSheets();
    }

    public JPanel uniprotPanel() {
        JPanel uniprotPanel = new JPanel();
        uniprotPanel.setBounds(10, 70, 400, 400);

        JPanel fileProcessingPanel = new JPanel();
        fileProcessingPanel.setLayout(new BoxLayout(fileProcessingPanel, BoxLayout.Y_AXIS));

        fileProcessingPanel.add(sheets);
        sheets.addItem("Select sheet");

        fileProcessingPanel.add(idColumn).setPreferredSize(new Dimension(400, 50));
        idColumn.addItem("Select ID column");
        sheets.addActionListener(e -> setUpColumns());

        fileProcessingPanel.add(idDbColumn).setPreferredSize(new Dimension(400, 50));
        idDbColumn.addItem("Select ID database column");
        sheets.addActionListener(e -> setUpColumns());

        fileProcessingPanel.add(organismColumn).setPreferredSize(new Dimension(400, 50));
        organismColumn.addItem("Select organism column");
        sheets.addActionListener(e -> setUpColumns());

        JButton processFile = processFileButton();
        uniprotPanel.add(fileProcessingPanel);
        uniprotPanel.add(processFile);
        uniprotPanel.setVisible(true);
        return uniprotPanel;
    }

    public void setUpSheets() {
        sheets.removeAllItems();
        sheets.addItem("Select sheet");
        if (excelFileReader.getSheets().isEmpty()) {
            sheets.setEnabled(false);
            sheets.setSelectedIndex(0);
        } else {
            sheets.setEnabled(true);
            for (String sheetName : excelFileReader.getSheets()) {
                sheets.addItem(sheetName);
            }
        }
    }

    public void setUpColumns() {
        idColumn.removeAllItems();
        idColumn.addItem("Select participant id column to process");

        organismColumn.removeAllItems();
        organismColumn.addItem("Select organism column");

        idDbColumn.removeAllItems();
        idDbColumn.addItem("Select id database column");

        if (sheets.isEnabled()) {
            String selectedSheet = (String) sheets.getSelectedItem();
            if (selectedSheet != null && !selectedSheet.equals("Select sheet")) {
                for (String columnName : excelFileReader.getColumns(selectedSheet)) {
                    idColumn.addItem(columnName);
                    organismColumn.addItem(columnName);
                    idDbColumn.addItem(columnName);
                }
            }
        } else {
            for (String columnName : excelFileReader.getColumns("")) {
                idColumn.addItem(columnName);
                organismColumn.addItem(columnName);
                idDbColumn.addItem(columnName);
            }
        }
    }

    public JButton processFileButton() {
        JButton processFile = new JButton("Update the Uniprot ids");
        processFile.addActionListener(e -> {

            String sheetSelected = (String) sheets.getSelectedItem();
            String idColumnIndex = (String) idColumn.getSelectedItem();
            int idDbColumnIndex = idDbColumn.getSelectedIndex() - 1;
            int organismColumnIndex = organismColumn.getSelectedIndex() - 1;
            if (sheets.isEnabled()) {
                if (sheetSelected == null || sheetSelected.equals("Select sheet") ||
                        idColumnIndex == null || idColumnIndex.equals("Select column to process")) {
                    JOptionPane.showMessageDialog(null, "Please select a valid sheet and column!", "ERROR", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    excelFileReader.checkAndInsertUniprotResultsExcel(sheetSelected, idColumnIndex, organismColumnIndex, idDbColumnIndex);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "An error occurred during file processing: " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            } else {
                if (idColumnIndex == null || idColumnIndex.equals("Select column to process")) {
                    JOptionPane.showMessageDialog(null, "Please select a valid column for processing!", "ERROR", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    excelFileReader.checkAndInsertUniprotResultsFileSeparatedFormat(idColumnIndex, organismColumnIndex, idDbColumnIndex);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "An error occurred during file processing: " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });

        return processFile;
    }
}
