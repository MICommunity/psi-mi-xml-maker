package uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping;

import uk.ac.ebi.intact.psi.mi.xmlmaker.XmlMakerUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml.InteractionsCreatorGui;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * The UniprotMapperGui class provides a graphical user interface (GUI) for interacting with an Excel file
 * to map UniProt IDs based on the selected sheet and column configuration.
 * It allows users to select a sheet and specify columns for processing, and then execute the process
 * to update UniProt IDs.
 */
public class UniprotMapperGui extends JPanel {
    private final JComboBox<String> sheets = new JComboBox<>();
    private final JComboBox<String> idColumn = new JComboBox<>();
    private final JComboBox<String> organismColumn = new JComboBox<>();
    private final JComboBox<String> idDbColumn = new JComboBox<>();
    private final XmlMakerUtils utils = new XmlMakerUtils();
    private final ExcelFileReader excelFileReader;
    private static final Logger LOGGER = Logger.getLogger(UniprotMapperGui.class.getName());

    /**
     * Constructs a new instance of the UniprotMapperGui class.
     * @param excelFileReader The ExcelFileReader instance to interact with the Excel file.
     */
    public UniprotMapperGui(ExcelFileReader excelFileReader) {
        this.excelFileReader = excelFileReader;
    }

    /**
     * Builds and returns the main panel for UniProt mapping.
     * This panel sets up the layout, combo boxes for user input, and the process button.
     *
     * @return The main JPanel for UniProt mapping.
     */
    public JPanel uniprotPanel() {
        JPanel uniprotPanel = new JPanel();
        uniprotPanel.setBounds(10, 70, 400, 400);

        JPanel fileProcessingPanel = new JPanel();
        fileProcessingPanel.setLayout(new BoxLayout(fileProcessingPanel, BoxLayout.Y_AXIS));

        setupComboBoxDefaults();

        fileProcessingPanel.add(sheets);
        fileProcessingPanel.add(setComboBoxDimension(idColumn, "Select ID column"));
        fileProcessingPanel.add(setComboBoxDimension(idDbColumn, "Select ID database column"));
        fileProcessingPanel.add(setComboBoxDimension(organismColumn, "Select organism column"));

        sheets.addActionListener(e -> setUpColumns());

        JButton processFile = processFileButton();
        uniprotPanel.add(fileProcessingPanel);
        uniprotPanel.add(processFile);

        uniprotPanel.setVisible(true);
        return uniprotPanel;
    }

    /**
     * Sets up default items for the combo boxes.
     * This method is called in uniprotPanel() to set the default selections.
     */
    private void setupComboBoxDefaults() {
        idColumn.addItem("Select ID column");
        idDbColumn.addItem("Select ID database column");
        organismColumn.addItem("Select organism column");
        sheets.addItem("Select sheet");
    }

    /**
     * Configures combo box dimensions and adds the default item to the combo box.
     *
     * @param comboBox The combo box to be configured.
     * @param defaultItem The default item to add to the combo box.
     * @return The configured combo box.
     */
    private JComboBox<String> setComboBoxDimension(JComboBox<String> comboBox, String defaultItem) {
        comboBox.addItem(defaultItem);
        comboBox.setPreferredSize(new Dimension(400, 50));
        return comboBox;
    }

    /**
     * Populates the sheets combo box with available sheets from the ExcelFileReader.
     * If no sheets are available, disables the combo box.
     */
    public void setUpSheets() {
        if (excelFileReader.sheets.isEmpty()) {
            sheets.setEnabled(false);
            sheets.setSelectedIndex(0);
        } else {
            sheets.setEnabled(true);
            for (String sheetName : excelFileReader.sheets) {
                sheets.addItem(sheetName);
            }
        }
    }

    /**
     * Populates the columns in the combo boxes based on the selected sheet.
     * This method updates the available columns for participant ID, organism, and ID database.
     */
    public void setUpColumns() {
        idColumn.removeAllItems();
        idColumn.addItem("Select participant ID column to process");

        organismColumn.removeAllItems();
        organismColumn.addItem("Select organism column");

        idDbColumn.removeAllItems();
        idDbColumn.addItem("Select ID database column");

        String selectedSheet = "";
        if (sheets.isEnabled()) {
            selectedSheet = (String) sheets.getSelectedItem();
        }
        for (String columnName : excelFileReader.getColumns(selectedSheet)) {
            idColumn.addItem(columnName);
            organismColumn.addItem(columnName);
            idDbColumn.addItem(columnName);
        }
    }

    /**
     * Creates and returns a button to process the Excel file for UniProt mapping.
     * When clicked, the button validates user inputs and processes the selected sheet or file.
     *
     * @return The JButton that processes the file.
     */
    public JButton processFileButton() {
        JButton processFile = new JButton("Update the UniProt IDs");
        processFile.addActionListener(e -> {
            // Validate inputs
            String sheetSelected = (String) sheets.getSelectedItem();
            String idColumn = (String) this.idColumn.getSelectedItem();
            int idDbColumnIndex = idDbColumn.getSelectedIndex() - 1;
            int organismColumnIndex = organismColumn.getSelectedIndex() - 1;

            // Ensure a valid sheet and column are selected
            if (sheets.isEnabled()) {
                if (isInvalidSelection(sheetSelected, idColumn)) {
                    JOptionPane.showMessageDialog(null, "Please select a valid sheet and column!", "ERROR", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                processSheet(sheetSelected, idColumn, organismColumnIndex, idDbColumnIndex);
            } else {
                if (idColumn == null || idColumn.equals("Select column to process")) {
                    JOptionPane.showMessageDialog(null, "Please select a valid column for processing!", "ERROR", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                processFileWithoutSheet(idColumn, organismColumnIndex, idDbColumnIndex);
            }
        });
        return processFile;
    }

    /**
     * Processes the selected sheet using the provided parameters for ID column, organism, and ID database.
     *
     * @param sheetSelected The name of the selected sheet.
     * @param idColumn The name of the selected ID column.
     * @param organismColumnIndex The index of the selected organism column.
     * @param idDbColumnIndex The index of the selected ID database column.
     */
    private void processSheet(String sheetSelected, String idColumn, int organismColumnIndex, int idDbColumnIndex) {
        try {
            excelFileReader.checkAndInsertUniprotResultsExcel(sheetSelected, idColumn, organismColumnIndex, idDbColumnIndex);
            showMoleculeSetDialog();
        } catch (Exception ex) {
            handleProcessingError(ex);
        }
    }

    /**
     * Processes the file without using a sheet selection.
     * This method processes the file with participant ID, organism, and ID database columns.
     *
     * @param idColumn The name of the selected ID column.
     * @param organismColumnIndex The index of the selected organism column.
     * @param idDbColumnIndex The index of the selected ID database column.
     */
    private void processFileWithoutSheet(String idColumn, int organismColumnIndex, int idDbColumnIndex) {
        try {
            excelFileReader.checkAndInsertUniprotResultsFileSeparatedFormat(idColumn, organismColumnIndex, idDbColumnIndex);
            showMoleculeSetDialog();
        } catch (Exception ex) {
            handleProcessingError(ex);
        }
    }

    /**
     * Displays a dialog with participants that have been identified as part of a molecule set.
     * If any proteins are part of a molecule set, they are shown in a comma-separated list.
     */
    private void showMoleculeSetDialog() {
        if (!excelFileReader.proteinsPartOfMoleculeSet.isEmpty()) {
            String participantsList = String.join(", ", excelFileReader.proteinsPartOfMoleculeSet);
            utils.showInfoDialog("Those participants have been identified as part of a molecule set: " + participantsList);
        }
    }

    /**
     * Handles errors that occur during file processing.
     * This method displays an error message dialog and prints the exception stack trace.
     *
     * @param ex The exception that occurred during file processing.
     */
    private void handleProcessingError(Exception ex) {
        JOptionPane.showMessageDialog(null, "An error occurred during file processing: " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
        LOGGER.warning(ex.getMessage());
    }

    /**
     * Checks if the selected sheet or column is invalid.
     *
     * @param sheetSelected The name of the selected sheet.
     * @param idColumn The name of the selected ID column.
     * @return Returns true if the sheet or column is invalid, otherwise false.
     */
    private boolean isInvalidSelection(String sheetSelected, String idColumn) {
        return sheetSelected == null || sheetSelected.equals("Select sheet") || idColumn == null || idColumn.equals("Select column to process");
    }
}
