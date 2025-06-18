package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.gui;

import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.FileFormater;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.FileReader;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static java.awt.Toolkit.*;
import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.GuiUtils.*;

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
 * and {@code FileReader} to retrieve sheet and column information.
 *
 */
public class FileFormaterGui {
    final FileReader fileReader;
    final FileFormater fileFormater;
    private final JComboBox<String> sheets = new JComboBox<>();
    private final JComboBox<String> baitColumn = new JComboBox<>();
    private final JComboBox<String> preyColumn = new JComboBox<>();
    private final JComboBox<String> baitNameColumn = new JComboBox<>();
    private final JComboBox<String> preyNameColumn = new JComboBox<>();

    private final int WIDTH = getDefaultToolkit().getScreenSize().width - 100;

    final ParticipantAndInteractionCreatorGui participantAndInteractionCreatorGui;

    private static final Logger LOGGER = Logger.getLogger(FileFormaterGui.class.getName());

    public FileFormaterGui(FileReader fileReader) {
        this.fileReader = fileReader;
        fileFormater = new FileFormater(fileReader);
        this.participantAndInteractionCreatorGui = new ParticipantAndInteractionCreatorGui(fileReader);
    }

    /**
     * Builds and returns the main panel for UniProt mapping.
     * This panel sets up the layout, combo boxes for user input, and the process button.
     *
     * @return The main JPanel for UniProt mapping.
     */
    public JPanel getFileFormaterPanel() {
        JPanel fileFormaterPanel = new JPanel();
        fileFormaterPanel.setLayout(new BoxLayout(fileFormaterPanel, BoxLayout.Y_AXIS));

        JPanel wrapperSheetSelection = new JPanel(new FlowLayout(FlowLayout.CENTER));
        wrapperSheetSelection.add(createSheetPanel());
        fileFormaterPanel.add(wrapperSheetSelection);

        fileFormaterPanel.add(participantAndInteractionCreatorGui.createParticipantAndInteractionCreatorGui());

        JPanel wrapperFileProcessing = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wrapperFileProcessing.add(createFileProcessingPanel());
        fileFormaterPanel.add(wrapperFileProcessing);

        fileFormaterPanel.setVisible(true);
        return fileFormaterPanel;
    }

    private boolean isUpdatingSheets = false;

    /**
     * Populates the sheets combo box with available sheets from the FileReader.
     * If no sheets are available, disables the combo box.
     */
    public void setUpSheets() {
        isUpdatingSheets = true; // Suppress events
        setupComboBoxDefaults();

        if (fileReader.sheets.isEmpty()) {
            sheets.addItem("* Select sheet");
            sheets.setToolTipText("* Select sheet");
            sheets.setEnabled(false);
            sheets.setSelectedIndex(0);
            setUpColumns();
        } else {
            sheets.removeAllItems();
            sheets.setEnabled(true);
            sheets.addItem("* Select sheet");
            sheets.setToolTipText("* Select sheet");
            for (String sheetName : fileReader.sheets) {
                sheets.addItem(sheetName);
            }
        }
        isUpdatingSheets = false;
    }

    /**
     * Sets up default items for the combo boxes.
     * This method is called in uniprotPanel() to set the default selections.
     */
    private void setupComboBoxDefaults() {
        baitColumn.setEnabled(false);
        preyColumn.setEnabled(false);
        baitNameColumn.setEnabled(false);
        preyNameColumn.setEnabled(false);
    }

    /**
     * Populates the columns in the combo boxes based on the selected sheet.
     * This method updates the available columns for participant ID, organism, and ID database.
     */
    public void setUpColumns() {
        baitColumn.removeAllItems();
        baitColumn.setToolTipText("* Select baits id column");
        baitColumn.addItem("* Select baits id column");
        baitColumn.setEnabled(true);

        preyColumn.removeAllItems();
        preyColumn.setToolTipText("* Select preys id column");
        preyColumn.addItem("* Select preys id column");
        preyColumn.setEnabled(true);

        baitNameColumn.removeAllItems();
        baitNameColumn.setToolTipText("Select baits name column");
        baitNameColumn.addItem("Select baits name");
        baitNameColumn.setEnabled(true);

        preyNameColumn.removeAllItems();
        preyNameColumn.setToolTipText("Select preys name column");
        preyNameColumn.addItem("Select preys name");
        preyNameColumn.setEnabled(true);

        String selectedSheet = "";
        if (sheets.isEnabled()) {
            selectedSheet = (String) sheets.getSelectedItem();
        }
        for (String columnName : fileReader.getColumns(selectedSheet)) {
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
            fileFormater.selectFileFormater(fileReader.getCurrentFilePath(),
                    baitColumn.getSelectedIndex()-1,
                    preyColumn.getSelectedIndex()-1,
                    baitNameColumn.getSelectedIndex() -1,
                    preyNameColumn.getSelectedIndex() -1,
                    Objects.requireNonNull(sheets.getSelectedItem()).toString(),
                    binary);

        } catch (Exception e) {
            showErrorDialog("Error during file formatting, please check that the mandatory columns are correctly selected.");
            LOGGER.warning("Error during file formatting: " + e);
        }
    }

    /**
     * Creates a panel for selecting various options in the file, such as selecting the sheet, bait column,
     * bait name column, prey column, and prey name column.
     * The panel also contains dropdowns for each selection, making it user-friendly for selecting
     * relevant columns for file processing.
     *
     * @return A {@link JPanel} containing combo boxes for sheet and column selections.
     */
    public JPanel createSheetPanel() {
        JPanel sheetsPanel = new JPanel();
        sheetsPanel.setLayout(new GridLayout(1, 5));
        sheetsPanel.setPreferredSize(new Dimension(WIDTH, 100));

        sheetsPanel.setBorder(BorderFactory.createTitledBorder(" 2.1 Select in the file"));

        sheetsPanel.add(setComboBoxDimension(sheets, "* Select sheet"));

        sheetsPanel.add(setComboBoxDimension(baitColumn, "* Select baits id column"));
        sheetsPanel.add(setComboBoxDimension(baitNameColumn, "Select baits name column"));

        sheetsPanel.add(setComboBoxDimension(preyColumn, "* Select preys id column"));
        sheetsPanel.add(setComboBoxDimension(preyNameColumn, "Select preys name column"));

        sheets.addActionListener(e -> {
            if (!isUpdatingSheets) {
                setUpColumns();
            }
        });

        return sheetsPanel;
    }

    /**
     * Creates a panel for file processing which includes a checkbox for creating binary interactions
     * and a button to trigger the file formatting process. When the button is clicked, it uses the
     * user-selected features and interaction data to format the file accordingly.
     *
     * @return A {@link JPanel} containing a checkbox and button for file processing.
     */
    public JPanel createFileProcessingPanel() {
        JCheckBox fileFormaterCheckBox = new JCheckBox("Create binary interactions");

        JButton fileFormaterButton = getFileFormaterButton(fileFormaterCheckBox);

        JPanel processPanel = new JPanel();
        processPanel.setPreferredSize(new Dimension(WIDTH, 100));
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

    /**
     * Creates and returns a {@link JButton} labeled "Format file" with an attached action listener.
     * <p>
     * When clicked, the button:
     * <ul>
     *     <li>Retrieves participant interaction data from the GUI.</li>
     *     <li>Configures the {@code fileFormater} instance with parameters and feature data from the GUI.</li>
     *     <li>Triggers the file formatting process based on whether the associated checkbox is selected.</li>
     * </ul>
     *
     * @param fileFormaterCheckBox a checkbox indicating whether the formatting should be applied
     * @return a configured {@link JButton} that initiates file formatting when clicked
     */
    private JButton getFileFormaterButton(JCheckBox fileFormaterCheckBox) {
        JButton fileFormaterButton = new JButton("Format file");
        fileFormaterButton.addActionListener(e -> {
            Map<String, String> interactionData = participantAndInteractionCreatorGui.getParticipantDetails();
            fileFormater.setAddVariableExperimentalConditions(participantAndInteractionCreatorGui.getAddVariableExperimentalCondition().isSelected());
            fileFormater.setAddParameters(participantAndInteractionCreatorGui.getMultipleInteractionParameters().isSelected());
            fileFormater.setBaitFeatures(participantAndInteractionCreatorGui.getBaitFeatureCreator().getBaitFeatures());
            fileFormater.setPreyFeatures(participantAndInteractionCreatorGui.getPreyFeatureCreator().getPreyFeatures());
            formatFile(fileFormaterCheckBox.isSelected(), interactionData);
        });
        return fileFormaterButton;
    }
}
