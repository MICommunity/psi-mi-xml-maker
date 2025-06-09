package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.gui;

import lombok.Getter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.InteractionWriter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.creators.InteractionsCreator;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.FileUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.GUIUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;

import static java.awt.Toolkit.*;

/**
 * InteractionWriterGui provides a graphical user interface (GUI) for generating
 * PSI-MI XML files using the InteractionWriter class. The GUI allows users to
 * specify a save location, select a date, and initiate the XML generation process.
 * Dependencies:
 * - InteractionWriter for handling the XML creation logic.
 * - InteractionsCreator for creating interaction data.
 * - ExcelFileReader for reading publication-related data.
 * Usage:
 * Create an instance of this class with the required dependencies and integrate
 * the panel returned by `createPsiMiXmlMakerPanel` into a JFrame or other container.
 */
public class InteractionWriterGui {
    @Getter
    private final InteractionWriter interactionWriter;
    private JTextField filenameField;
    private JTextField saveLocationField;
    private JTextField numberOfInteractionsField;
    private final ExcelFileReader excelFileReader;
    private JFileChooser directoryChooser;

    /**
     * Constructs a InteractionWriterGui instance with the given dependencies.
     *
     * @param excelFileReader an instance of ExcelFileReader for reading publication-related data
     */
    public InteractionWriterGui(ExcelFileReader excelFileReader) {
        this.interactionWriter = new InteractionWriter(excelFileReader);
        this.excelFileReader = excelFileReader;
    }

    /**
     * Creates the main panel containing the GUI for generating PSI-MI XML files.
     *
     * @return a JPanel with the PSI-MI XML maker controls
     */
    public JPanel createPsiMiXmlMakerPanel() {
        JPanel xmlPanel = new JPanel();
        xmlPanel.setLayout(new BoxLayout(xmlPanel, BoxLayout.Y_AXIS));

        JPanel datePanel = createDateSelectionPanel();
        datePanel.setMaximumSize(new Dimension(getDefaultToolkit().getScreenSize().width - 100, 100));
        xmlPanel.add(datePanel);

        JPanel saveLocationPanel = createSaveLocationPanel();
        saveLocationPanel.setMaximumSize(new Dimension(getDefaultToolkit().getScreenSize().width - 100, 100));
        xmlPanel.add(saveLocationPanel);

        return xmlPanel;
    }

    /**
     * Creates a panel for selecting a date.
     *
     * @return a JPanel containing date selection controls
     */
    private JPanel createDateSelectionPanel() {
        JPanel dateSelectionPanel = new JPanel();
        dateSelectionPanel.setLayout(new FlowLayout());
        dateSelectionPanel.setBorder(BorderFactory.createTitledBorder("5.1 Select the publication Date"));

        SpinnerDateModel dateModel = new SpinnerDateModel();
        JSpinner dateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy");
        dateSpinner.setEditor(editor);

        dateSpinner.setBorder(new TitledBorder("Publication release date"));
        dateSpinner.setPreferredSize(new Dimension(200, 50));
        dateSelectionPanel.add(dateSpinner);
        return dateSelectionPanel;
    }

    /**
     * Creates a panel for specifying the save location.
     *
     * @return a JPanel containing save location controls
     */
    private JPanel createSaveLocationPanel() {
        JPanel saveLocationPanel = new JPanel();
        saveLocationPanel.setLayout(new FlowLayout());
        saveLocationPanel.setBorder(BorderFactory.createTitledBorder("5.2 Select the saving location"));


        JLabel nameLabel = new JLabel("Name: ");
        filenameField = new JTextField(20);
        JLabel saveLocationLabel = new JLabel("Directory:");
        saveLocationField = new JTextField(20);
        GUIUtils.addChangeListener(saveLocationField, change -> interactionWriter.setSaveLocation(saveLocationField.getText()));
        GUIUtils.addChangeListener(filenameField, change -> interactionWriter.setName(filenameField.getText()));
        JButton browseButton = getBrowseButton();

        excelFileReader.registerInputSelectedEventHandler(event -> {
            filenameField.setText(FileUtils.getFileName(event.getSelectedFile().getName()));
            saveLocationField.setText(event.getSelectedFile().getParent());
            directoryChooser.setSelectedFile(event.getSelectedFile().getParentFile());
        });

        JLabel numberOfInteractionsLabel = new JLabel("Number of Interactions per file:");
        numberOfInteractionsField = new JTextField("1000",20);
        GUIUtils.addChangeListener(numberOfInteractionsField, change ->
                InteractionsCreator.setMAX_INTERACTIONS_PER_FILE(Integer.parseInt(numberOfInteractionsField.getText())));

        saveLocationPanel.add(numberOfInteractionsLabel);
        saveLocationPanel.add(numberOfInteractionsField);
        saveLocationPanel.add(nameLabel);
        saveLocationPanel.add(filenameField);
        saveLocationPanel.add(saveLocationLabel);
        saveLocationPanel.add(saveLocationField);
        saveLocationPanel.add(browseButton);

        return saveLocationPanel;
    }

    /**
     * Creates a button for browsing and selecting a directory.
     *
     * @return a JButton for opening a file chooser dialog
     */
    private JButton getBrowseButton() {
        JButton browseButton = new JButton("Browse...");
        directoryChooser = new JFileChooser();
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        browseButton.addActionListener(e -> {

            int returnValue = directoryChooser.showSaveDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = directoryChooser.getSelectedFile();
                saveLocationField.setText(selectedFile.getAbsolutePath());
            }
        });
        return browseButton;
    }

}
