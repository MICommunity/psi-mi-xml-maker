package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.gui;

import lombok.Getter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.FileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.XmlFileWriter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.creators.XmlInteractionsCreator;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.FileUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.GuiUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;

import static java.awt.Toolkit.*;

/**
 * SavingOptionsGui provides a graphical user interface (GUI) for generating
 * PSI-MI XML files using the XmlFileWriter class. The GUI allows users to
 * specify a save location, select a date, and initiate the XML generation process.
 * Dependencies:
 * - XmlFileWriter for handling the XML creation logic.
 * - XmlInteractionsCreator for creating interaction data.
 * - FileReader for reading publication-related data.
 * Usage:
 * Create an instance of this class with the required dependencies and integrate
 * the panel returned by `createPsiMiXmlMakerPanel` into a JFrame or the other container.
 */
public class SavingOptionsGui {
    @Getter
    private final XmlFileWriter xmlFileWriter;
    private JTextField filenameField;
    private JTextField saveLocationField;
    private JTextField numberOfInteractionsField;
    private final FileReader fileReader;
    private JFileChooser directoryChooser;

    /**
     * Constructs an SavingOptionsGui instance with the given dependencies.
     *
     * @param fileReader an instance of FileReader for reading publication-related data
     */
    public SavingOptionsGui(FileReader fileReader) {
        this.xmlFileWriter = new XmlFileWriter(fileReader);
        this.fileReader = fileReader;
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
        GuiUtils.addChangeListener(saveLocationField, change -> xmlFileWriter.setSaveLocation(saveLocationField.getText()));
        GuiUtils.addChangeListener(filenameField, change -> xmlFileWriter.setName(filenameField.getText()));
        JButton browseButton = getBrowseButton();

        fileReader.registerInputSelectedEventHandler(event -> {
            filenameField.setText(FileUtils.getFileName(event.getSelectedFile().getName()));
            saveLocationField.setText(event.getSelectedFile().getParent());
            directoryChooser.setSelectedFile(event.getSelectedFile().getParentFile());
        });

        JLabel numberOfInteractionsLabel = new JLabel("Number of Interactions per file:");
        numberOfInteractionsField = new JTextField("1000",20);
        GuiUtils.addChangeListener(numberOfInteractionsField, change ->
                XmlInteractionsCreator.setMAX_INTERACTIONS_PER_FILE(Integer.parseInt(numberOfInteractionsField.getText())));

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
