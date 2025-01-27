package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Objects;

public class FileFormaterGui {
    final ExcelFileReader excelFileReader;
    final FileFormater fileFormater;
    private final JComboBox<String> sheets = new JComboBox<>();
    private final JComboBox<String> baitColumn = new JComboBox<>();
    private final JComboBox<String> preyColumn = new JComboBox<>();
    ParticipantAndInteractionCreatorGui participantAndInteractionCreatorGui = new ParticipantAndInteractionCreatorGui();

    public FileFormaterGui(ExcelFileReader excelFileReader) {
        this.excelFileReader = excelFileReader;
        fileFormater = new FileFormater(excelFileReader);
    }

    /**
     * Builds and returns the main panel for UniProt mapping.
     * This panel sets up the layout, combo boxes for user input, and the process button.
     *
     * @return The main JPanel for UniProt mapping.
     */
    public JPanel getFileFormaterPanel() {
        JPanel fileFormaterPanel = new JPanel();

        JCheckBox fileFormaterCheckBox = new JCheckBox();
        fileFormaterCheckBox.setText("Create binary interactions");

        fileFormaterPanel.add(setComboBoxDimension(sheets, "Select sheet"));
        fileFormaterPanel.add(setComboBoxDimension(baitColumn, "Select baits column"));
        fileFormaterPanel.add(setComboBoxDimension(preyColumn, "Select preys column"));

        sheets.addActionListener(e -> setUpColumns());

        JButton fileFormaterButton = new JButton("Format file");
        fileFormaterButton.addActionListener(e -> {
            Map<String, String> interactionData = participantAndInteractionCreatorGui.getParticipantDetails();
            formatFile(fileFormaterCheckBox.isSelected(),interactionData);

        });

        fileFormaterPanel.add(fileFormaterCheckBox);

        fileFormaterPanel.add(participantAndInteractionCreatorGui.createParticipantAndInteractionCreatorGui());

        fileFormaterPanel.add(fileFormaterButton);
        fileFormaterPanel.setVisible(true);

        return fileFormaterPanel;
    }

    /**
     * Populates the sheets combo box with available sheets from the ExcelFileReader.
     * If no sheets are available, disables the combo box.
     */
    public void setUpSheets() {
        setupComboBoxDefaults();
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
     * Sets up default items for the combo boxes.
     * This method is called in uniprotPanel() to set the default selections.
     */
    private void setupComboBoxDefaults() {
        baitColumn.addItem("Select bait column");
        baitColumn.setEnabled(false);
        preyColumn.addItem("Select prey column");
        preyColumn.setEnabled(false);

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
     * Populates the columns in the combo boxes based on the selected sheet.
     * This method updates the available columns for participant ID, organism, and ID database.
     */
    public void setUpColumns() {
        baitColumn.removeAllItems();
        baitColumn.addItem("Select baits column");
        baitColumn.setEnabled(true);

        preyColumn.removeAllItems();
        preyColumn.addItem("Select preys column");
        preyColumn.setEnabled(true);


        String selectedSheet = "";
        if (sheets.isEnabled()) {
            selectedSheet = (String) sheets.getSelectedItem();
        }
        for (String columnName : excelFileReader.getColumns(selectedSheet)) {
            baitColumn.addItem(columnName);
            preyColumn.addItem(columnName);
        }
    }

    public void formatFile(boolean binary, Map<String, String> interactionData) {
        try {
            fileFormater.setInteractionData(interactionData);
            fileFormater.selectFileFormater(excelFileReader.currentFilePath, baitColumn.getSelectedIndex()-1,
                    preyColumn.getSelectedIndex()-1, Objects.requireNonNull(sheets.getSelectedItem()).toString(),
                    binary);
        } catch (Exception e) {
            XmlMakerUtils.showErrorDialog("Error during file formatting: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
