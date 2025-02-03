package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;
import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * The {@code FileFormaterGui} class provides a graphical user interface
 * for formatting interaction data files. It allows users to select an
 * Excel sheet, specify bait and prey columns, and format the file for
 * XML generation with optional binary interactions.
 *
 * <p>Features include:</p>
 * <ul>
 *   <li>Dropdowns for sheet and column selection</li>
 *   <li>Checkbox for binary interaction formatting</li>
 *   <li>Button to trigger file formatting</li>
 * </ul>
 *
 * This class interacts with {@code FileFormater} to process the selected file
 * and {@code ExcelFileReader} to retrieve sheet and column information.
 *
 */
public class FileFormaterGui {
    final ExcelFileReader excelFileReader;
    final FileFormater fileFormater;
    private final JComboBox<String> sheets = new JComboBox<>();
    private final JComboBox<String> baitColumn = new JComboBox<>();
    private final JComboBox<String> preyColumn = new JComboBox<>();

    final ParticipantAndInteractionCreatorGui participantAndInteractionCreatorGui = new ParticipantAndInteractionCreatorGui();

    private static final Logger LOGGER = Logger.getLogger(FileFormaterGui.class.getName());


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
        JPanel sheetsPanel = new JPanel();
        sheetsPanel.setLayout(new GridLayout(3, 1));

        JCheckBox fileFormaterCheckBox = new JCheckBox();
        fileFormaterCheckBox.setText("Create binary interactions");

        sheetsPanel.add(setComboBoxDimension(sheets, "Select sheet"));
        sheetsPanel.add(setComboBoxDimension(baitColumn, "Select baits column"));
        sheetsPanel.add(setComboBoxDimension(preyColumn, "Select preys column"));

        sheets.addActionListener(e -> setUpColumns());

        JButton fileFormaterButton = new JButton("Format file");
        fileFormaterButton.addActionListener(e -> {
            Map<String, String> interactionData = participantAndInteractionCreatorGui.getParticipantDetails();
            formatFile(fileFormaterCheckBox.isSelected(),interactionData);
        });

        fileFormaterPanel.add(sheetsPanel);
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
        return XmlMakerUtils.setComboBoxDimension(comboBox, defaultItem);
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

    /**
     * Formats the selected file based on the given interaction data and binary mode.
     * This method sets the interaction data, selects the appropriate file format,
     * and processes the file accordingly.
     *
     * @param binary          {@code true} if the interactions should be formatted in binary mode, {@code false} otherwise.
     * @param interactionData A map containing interaction-related data.
     */
    public void formatFile(boolean binary, Map<String, String> interactionData) {
        try {
            fileFormater.setInteractionData(interactionData);
            fileFormater.selectFileFormater(excelFileReader.currentFilePath, baitColumn.getSelectedIndex()-1,
                    preyColumn.getSelectedIndex()-1, Objects.requireNonNull(sheets.getSelectedItem()).toString(),
                    binary);
        } catch (Exception e) {
            XmlMakerUtils.showErrorDialog("Error during file formatting: " + e.getMessage());
            LOGGER.warning("Error during file formatting: " + e);
        }
    }

}
