package uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping;

import lombok.Getter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.LoadingSpinner;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
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
    @Getter
    private final JComboBox<String> sheets = new JComboBox<>();
    private final JComboBox<String> idColumn = new JComboBox<>();
    private final JComboBox<String> organismColumn = new JComboBox<>();
    private final JComboBox<String> idDbColumn = new JComboBox<>();
    private final ExcelFileReader excelFileReader;
    private static final Logger LOGGER = Logger.getLogger(UniprotMapperGui.class.getName());
    private final LoadingSpinner loadingSpinner;

    /**
     * Constructs a new instance of the UniprotMapperGui class.
     * @param excelFileReader The ExcelFileReader instance to interact with the Excel file.
     */
    public UniprotMapperGui(ExcelFileReader excelFileReader, LoadingSpinner loadingSpinner) {
        this.excelFileReader = excelFileReader;
        this.loadingSpinner = loadingSpinner;
    }

    /**
     * Builds and returns the main panel for UniProt mapping.
     * This panel sets up the layout, combo boxes for user input, and the process button.
     *
     * @return The main JPanel for UniProt mapping.
     */
    public JPanel uniprotPanel() {
        JPanel uniprotPanel = new JPanel();
        uniprotPanel.setLayout(new GridLayout(1, 1));
        uniprotPanel.setMaximumSize(new Dimension(2000, 200));
        setupComboBoxDefaults();

        uniprotPanel.add(XmlMakerUtils.setComboBoxDimension(sheets, "Select sheet"));
        uniprotPanel.add(XmlMakerUtils.setComboBoxDimension(idColumn, "Select ID column"));
        uniprotPanel.add(XmlMakerUtils.setComboBoxDimension(idDbColumn, "Select ID database column"));
        uniprotPanel.add(XmlMakerUtils.setComboBoxDimension(organismColumn, "Select Organism column"));

        sheets.addActionListener(e -> {
            if (!isUpdatingSheets) {
                setUpColumns();
            }
        });


        JButton processFile = processFileButton();
        uniprotPanel.add(processFile);

        uniprotPanel.setVisible(true);
        return uniprotPanel;
    }

    /**
     * Sets up default items for the combo boxes.
     * This method is called in uniprotPanel() to set the default selections.
     */
    private void setupComboBoxDefaults() {
        idColumn.addItem("Select participant ID column");
        idColumn.setEnabled(true);
        idDbColumn.addItem("Select ID database column");
        idDbColumn.setEnabled(true);
        organismColumn.addItem("Select organism column");
        organismColumn.setEnabled(true);
    }

    private boolean isUpdatingSheets = false;

    /**
     * Sets up the sheet selection for the UI, updating the available options in a combo box
     * based on the sheets present in an Excel file. This method disables events while
     * updating the combo box to prevent unnecessary triggers and updates the sheet list accordingly.
     * - If the Excel file does not contain any sheets, it adds a placeholder item ("Select sheet")
     *   and disables the combo box.
     * - If the Excel file contains sheets, it populates the combo box with the available sheet names
     *   and enables the selection.
     */
    public void setUpSheets() {
        isUpdatingSheets = true; // Suppress events
        setupComboBoxDefaults();

        if (excelFileReader.sheets.isEmpty()) {
            sheets.addItem("Select sheet");
            sheets.setEnabled(false);
            sheets.setSelectedIndex(0);
            setUpColumns();
        } else {
            sheets.removeAllItems();
            sheets.setEnabled(true);
            sheets.addItem("Select sheet");
            for (String sheetName : excelFileReader.sheets) {
                sheets.addItem(sheetName);
            }
        }
        isUpdatingSheets = false;
    }

    /**
     * Populates the columns in the combo boxes based on the selected sheet.
     * This method updates the available columns for participant ID, organism, and ID database.
     */
    public void setUpColumns() {
        idColumn.removeAllItems();
        idColumn.addItem("Select participant ID column");
        idColumn.setEnabled(true);

        organismColumn.removeAllItems();
        organismColumn.addItem("Select organism column");
        organismColumn.setEnabled(true);

        idDbColumn.removeAllItems();
        idDbColumn.addItem("Select ID database column");
        idDbColumn.setEnabled(true);

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
            loadingSpinner.showSpinner();

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    String sheetSelected = (String) sheets.getSelectedItem();
                    String idColumnSelectedItem = (String) idColumn.getSelectedItem();
                    int idDbColumnIndex = idDbColumn.getSelectedIndex() - 1;
                    int organismColumnIndex = organismColumn.getSelectedIndex() - 1;

                    if (sheets.isEnabled()) {
                        if (isInvalidSelection(sheetSelected, idColumnSelectedItem)) {
                            SwingUtilities.invokeLater(() -> XmlMakerUtils.showErrorDialog("Please select valid sheet and ID column"));
                            return null;
                        }
                        processSheet(sheetSelected, idColumnSelectedItem, idDbColumnIndex, organismColumnIndex);
                    } else {
                        if (idColumnSelectedItem == null || idColumnSelectedItem.equals("Select column to process")) {
                            SwingUtilities.invokeLater(() -> XmlMakerUtils.showErrorDialog("Please select valid sheet and ID column"));
                            return null;
                        }
                        processFileWithoutSheet(idColumnSelectedItem, idDbColumnIndex, organismColumnIndex);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    loadingSpinner.hideSpinner();
                    showMoleculeSetDialog();
                }
            };

            worker.execute();
        });
        return processFile;
    }

    /**
     * Processes the selected sheet using the provided parameters for ID column, organism, and ID database.
     *
     * @param sheetSelected The name of the selected sheet.
     * @param idColumn The name of the selected ID column.
     */
    private void processSheet(String sheetSelected, String idColumn, int idDbColumnIndex, int organismColumnIndex) {
        try {
            excelFileReader.checkAndInsertUniprotResultsWorkbook(sheetSelected, idColumn, idDbColumnIndex, organismColumnIndex);
            XmlMakerUtils.showInfoDialog("Inactive Uniprot IDs: " + excelFileReader.getUniprotIdNotFound());
            XmlMakerUtils.showInfoDialog("Successfully updated the UniProt IDs");
        } catch (Exception ex) {
            handleProcessingError(ex);
        }
    }

    /**
     * Processes the file without using a sheet selection.
     * This method processes the file with participant ID, organism, and ID database columns.
     *
     * @param idColumn The name of the selected ID column.
     */
    private void processFileWithoutSheet(String idColumn, int idDbColumn, int organismColumn) {
        try {
            excelFileReader.checkAndInsertUniprotResultsSeparatedFormat(idColumn, idDbColumn, organismColumn);
            XmlMakerUtils.showInfoDialog("Inactive Uniprot IDs: " + excelFileReader.getUniprotIdNotFound());
            XmlMakerUtils.showInfoDialog("Successfully updated the UniProt IDs");
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
            JOptionPane.showMessageDialog(new JFrame(),"Those participants have been identified as part of a molecule set: " + participantsList,
                    "INFORMATION", JOptionPane.INFORMATION_MESSAGE);
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
