package uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping;

import lombok.Getter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.LoadingSpinner;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.FileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.FileWriter;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.logging.Logger;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.GuiUtils.*;

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

    private final FileReader fileReader;
    private final FileWriter fileWriter;

    private final LoadingSpinner loadingSpinner;
    private boolean isUpdatingSheets = false;

    private static final Logger LOGGER = Logger.getLogger(UniprotMapperGui.class.getName());

    /**
     * Constructs a new instance of the UniprotMapperGui class.
     * @param fileReader The FileReader instance to interact with the Excel file.
     * @param loadingSpinner The loading spinner for display
     */
    public UniprotMapperGui(FileReader fileReader, LoadingSpinner loadingSpinner, FileWriter fileWriter) {
        this.fileReader = fileReader;
        this.loadingSpinner = loadingSpinner;
        this.fileWriter = fileWriter;
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
        uniprotPanel.setMaximumSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width - 50, 200));
        setupComboBoxDefaults();

        uniprotPanel.add(setComboBoxDimension(sheets, "Select sheet"));
        sheets.setToolTipText("Select sheet");
        uniprotPanel.add(setComboBoxDimension(idColumn, "Select ID column"));
        idColumn.setToolTipText("Select ID column");
        uniprotPanel.add(setComboBoxDimension(idDbColumn, "Select ID database column"));
        idDbColumn.setToolTipText("Select ID database column");
        uniprotPanel.add(setComboBoxDimension(organismColumn, "Select Organism column"));
        organismColumn.setToolTipText("Select Organism column");

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

        if (fileReader.sheets.isEmpty()) {
            sheets.addItem("Select sheet");
            sheets.setEnabled(false);
            sheets.setSelectedIndex(0);
            setUpColumns();
        } else {
            sheets.removeAllItems();
            sheets.setEnabled(true);
            sheets.addItem("Select sheet");
            for (String sheetName : fileReader.sheets) {
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
        for (String columnName : fileReader.getColumns(selectedSheet)) {
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
                    int idColumnIndex = idColumn.getSelectedIndex() - 1;
                    int idDbColumnIndex = idDbColumn.getSelectedIndex() - 1;
                    int organismColumnIndex = organismColumn.getSelectedIndex() - 1;

                    if (sheets.isEnabled()) {
                        processSheet(sheetSelected, idColumnIndex, idDbColumnIndex, organismColumnIndex);
                    } else {
                        processFileWithoutSheet(idColumnIndex, idDbColumnIndex, organismColumnIndex);
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
     * @param idColumnIndex Index of id column
     * @param idDbColumnIndex Index of id database column
     * @param organismColumnIndex Index of organism column
     */
    private void processSheet(String sheetSelected, int idColumnIndex, int idDbColumnIndex, int organismColumnIndex) {
        try {
            fileWriter.checkAndInsertUniprotResultsWorkbook(sheetSelected, idColumnIndex, idDbColumnIndex, organismColumnIndex);
            if (!fileWriter.getUniprotIdNotFound().isEmpty()) {
                showInfoDialog("Inactive Uniprot IDs: " + fileWriter.getUniprotIdNotFound());
            }
            showInfoDialog("UniProt IDs successfully updated");
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
    private void processFileWithoutSheet(int idColumn, int idDbColumn, int organismColumn) {
        try {
            fileWriter.checkAndInsertUniprotResultsSeparatedFormat(idColumn, idDbColumn, organismColumn);
            List<String> uniprotIdNotFound = fileWriter.getUniprotIdNotFound();
            if (!uniprotIdNotFound.isEmpty()) {
                showInfoDialog("Inactive Uniprot IDs: " + uniprotIdNotFound);
            }
            showInfoDialog("UniProt IDs successfully updated");
        } catch (Exception ex) {
            handleProcessingError(ex);
        }
    }

    /**
     * Displays a dialog with participants that have been identified as part of a molecule set.
     * If any proteins are part of a molecule set, they are shown in a comma-separated list.
     */
    private void showMoleculeSetDialog() {
        if (!fileWriter.getProteinsPartOfMoleculeSet().isEmpty()) {
            String participantsList = String.join(", ", fileWriter.getProteinsPartOfMoleculeSet());
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
}
