package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;
import javax.swing.*;
import java.awt.*;
import java.util.List;
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
    private final JComboBox<String> baitNameColumn = new JComboBox<>();
    private final JComboBox<String> preyNameColumn = new JComboBox<>();

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
//        fileFormaterPanel.setPreferredSize(new Dimension(2_000, 500));
        fileFormaterPanel.setLayout(new BoxLayout(fileFormaterPanel, BoxLayout.X_AXIS));

        JPanel wrapperSheetSelection = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        wrapperSheetSelection.add(createSheetPanel());
        fileFormaterPanel.add(wrapperSheetSelection);

        fileFormaterPanel.add(participantAndInteractionCreatorGui.createParticipantAndInteractionCreatorGui());

        JPanel wrapperFileProcessing = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wrapperFileProcessing.add(createFileProcessingPanel());
        fileFormaterPanel.add(wrapperFileProcessing);

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
        baitNameColumn.addItem("Select bait name column");
        baitNameColumn.setEnabled(false);
        preyNameColumn.addItem("Select prey name column");
        preyNameColumn.setEnabled(false);
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

        baitNameColumn.removeAllItems();
        baitNameColumn.addItem("Select baits name");
        baitNameColumn.setEnabled(true);

        preyNameColumn.removeAllItems();
        preyNameColumn.addItem("Select preys name");
        preyNameColumn.setEnabled(true);

        String selectedSheet = "";
        if (sheets.isEnabled()) {
            selectedSheet = (String) sheets.getSelectedItem();
        }
        for (String columnName : excelFileReader.getColumns(selectedSheet)) {
            baitColumn.addItem(columnName);
            preyColumn.addItem(columnName);
            baitNameColumn.addItem(columnName);
            preyNameColumn.addItem(columnName);
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
            fileFormater.selectFileFormater(excelFileReader.currentFilePath,
                    baitColumn.getSelectedIndex()-1,
                    preyColumn.getSelectedIndex()-1,
                    baitNameColumn.getSelectedIndex() -1,
                    preyNameColumn.getSelectedIndex() -1,
                    Objects.requireNonNull(sheets.getSelectedItem()).toString(),
                    binary);

        } catch (Exception e) {
            XmlMakerUtils.showErrorDialog("Error during file formatting: " + e.getMessage());
            LOGGER.warning("Error during file formatting: " + e);
        }
    }

    public JPanel createSheetPanel() {
        JPanel sheetsPanel = new JPanel();
        sheetsPanel.setLayout(new GridLayout(5, 1));
        sheetsPanel.setPreferredSize(new Dimension(250, 300));

        sheetsPanel.setBorder(BorderFactory.createTitledBorder(" 2.1 Select in the file"));

        sheetsPanel.add(XmlMakerUtils.setComboBoxDimension(sheets, "Select sheet"));

        sheetsPanel.add(XmlMakerUtils.setComboBoxDimension(baitColumn, "Select baits column"));
        sheetsPanel.add(XmlMakerUtils.setComboBoxDimension(baitNameColumn, "Select baits name column"));

        sheetsPanel.add(XmlMakerUtils.setComboBoxDimension(preyColumn, "Select preys column"));
        sheetsPanel.add(XmlMakerUtils.setComboBoxDimension(preyNameColumn, "Select preys name column"));

        sheets.addActionListener(e -> setUpColumns());

        return sheetsPanel;
    }

    public JPanel createFileProcessingPanel() {
        JCheckBox fileFormaterCheckBox = new JCheckBox("Create binary interactions");

        JButton fileFormaterButton = new JButton("Format file");
        fileFormaterButton.addActionListener(e -> {
            Map<String, String> interactionData = participantAndInteractionCreatorGui.getParticipantDetails();
            fileFormater.setBaitFeatures(participantAndInteractionCreatorGui.getFeaturesData(true));
            fileFormater.setPreyFeatures(participantAndInteractionCreatorGui.getFeaturesData(false));
            formatFile(fileFormaterCheckBox.isSelected(), interactionData);
        });

        JPanel processPanel = new JPanel();
        processPanel.setPreferredSize(new Dimension(200, 100));
        processPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        gbc.weighty = 0.5;

        //Checkbox position
        gbc.gridy = 0; //row num
        processPanel.add(fileFormaterCheckBox, gbc);

        //Button position
        gbc.gridy = 1;
        processPanel.add(fileFormaterButton, gbc);

        return processPanel;
    }
}
