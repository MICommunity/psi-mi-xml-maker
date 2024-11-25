package uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping;

import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;

import javax.swing.*;
import java.awt.*;

public class UniprotMapperGui extends JPanel {

    private final SuggestedOrganisms suggestedOrganisms = new SuggestedOrganisms();
    private final JComboBox<String> sheets = new JComboBox<>();
    private final JComboBox<String> columns = new JComboBox<>();
    private final JComboBox<String> suggestedOrganismsIds = new JComboBox<>();
    private final ExcelFileReader excelFileReader;

    public UniprotMapperGui(ExcelFileReader excelFileReader) {
        this.excelFileReader = excelFileReader;
        setUpSheets();
        setUpSuggestedOrganismsIds();
    }

    public JPanel uniprotPanel() {
        JPanel uniprotPanel = new JPanel();
        uniprotPanel.setBounds(10, 70, 400, 400);

        JPanel fileProcessingPanel = new JPanel();
        fileProcessingPanel.setLayout(new BoxLayout(fileProcessingPanel, BoxLayout.Y_AXIS));

        suggestedOrganismsIds.setModel(new DefaultComboBoxModel<>(suggestedOrganisms.getOrganismDisplayNames()));
        suggestedOrganismsIds.setEditable(true);

        fileProcessingPanel.add(sheets);
        sheets.addItem("Select sheet");

        fileProcessingPanel.add(columns).setPreferredSize(new Dimension(400, 50));
        columns.addItem("Select column");
        sheets.addActionListener(e -> setUpColumns());
        fileProcessingPanel.add(suggestedOrganismsIds).setPreferredSize(new Dimension(400, 20));

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
        columns.removeAllItems();
        columns.addItem("Select column to process");

        if (sheets.isEnabled()) {
            String selectedSheet = (String) sheets.getSelectedItem();
            if (selectedSheet != null && !selectedSheet.equals("Select sheet")) {
                for (String columnName : excelFileReader.getColumns(selectedSheet)) {
                    columns.addItem(columnName);
                }
            }
        } else {
            for (String columnName : excelFileReader.getColumns("")) {
                columns.addItem(columnName);
            }
        }
    }

    private void setUpSuggestedOrganismsIds() {
        suggestedOrganismsIds.setModel(new DefaultComboBoxModel<>(suggestedOrganisms.getOrganismDisplayNames()));
    }

    public JButton processFileButton() {
        JButton processFile = new JButton("Process file");
        processFile.addActionListener(e -> {

            String selectedDisplayName = (String) suggestedOrganismsIds.getSelectedItem();
            String organismId = suggestedOrganisms.getOrganismId(selectedDisplayName);

            if (organismId == null || organismId.isEmpty() || organismId.equals("null")) {
                organismId = selectedDisplayName != null ? selectedDisplayName.trim() : "";
            }

            if (organismId.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please enter a valid organism ID!", "ERROR", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String sheetSelected = (String) sheets.getSelectedItem();
            String columnSelected = (String) columns.getSelectedItem();
            if (sheets.isEnabled()) {
                if (sheetSelected == null || sheetSelected.equals("Select sheet") ||
                        columnSelected == null || columnSelected.equals("Select column to process")) {
                    JOptionPane.showMessageDialog(null, "Please select a valid sheet and column!", "ERROR", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    excelFileReader.checkAndInsertUniprotResultsExcel(sheetSelected, organismId, columnSelected);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "An error occurred during file processing: " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                if (columnSelected == null || columnSelected.equals("Select column to process")) {
                    JOptionPane.showMessageDialog(null, "Please select a valid column for processing!", "ERROR", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    excelFileReader.checkAndInsertUniprotResultsFileSeparatedFormat(organismId, columnSelected);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "An error occurred during file processing: " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
                    System.out.println(ex.getMessage());
                }
            }
        });
        return processFile;
    }
}
