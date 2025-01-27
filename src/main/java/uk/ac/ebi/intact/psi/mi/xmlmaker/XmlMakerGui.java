package uk.ac.ebi.intact.psi.mi.xmlmaker;

import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.FileFormaterGui;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml.InteractionWriterGui;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml.InteractionsCreatorGui;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapperGui;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Main GUI class for the PSI-MI XML Maker application.
 * <p>
 * Provides a graphical user interface for processing files to create PSI-MI XML files,
 * update Uniprot IDs, and manage interaction participants.
 * </p>
 */
public class XmlMakerGui {

    private static final int FRAME_WIDTH = 2000;
    private static final int FRAME_HEIGHT = 2000;
    private static final Logger LOGGER = Logger.getLogger(XmlMakerGui.class.getName());
    private final ExcelFileReader excelFileReader;
    private final UniprotMapperGui uniprotMapperGui;
    private final InteractionsCreatorGui interactionsCreatorGui;
    private final InteractionWriterGui interactionWriterGui;
    private final LoadingSpinner loadingSpinner;
    private final FileFormaterGui fileFormaterGui;

    /**
     * Constructs the XmlMakerGui instance, initializing all dependent components.
     */
    public XmlMakerGui() {
        this.loadingSpinner = new LoadingSpinner();
        this.excelFileReader = new ExcelFileReader();
        this.uniprotMapperGui = new UniprotMapperGui(excelFileReader, loadingSpinner);
        this.fileFormaterGui = new FileFormaterGui(excelFileReader);
        this.interactionWriterGui = new InteractionWriterGui(excelFileReader);
        this.interactionsCreatorGui = new InteractionsCreatorGui(excelFileReader,
                interactionWriterGui.getInteractionWriter(), uniprotMapperGui);
        excelFileReader.registerInputSelectedEventHandler(event -> setUpSheets());
    }

    public void initialize() {
        JFrame frame = createMainFrame();

        JButton restartButton = createRestartButton(frame);
        restartButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        frame.add(restartButton);

        frame.add(createFileLabel());

        frame.add(createFileFetcherPanel());
        frame.add(createFileFormaterPanel());
        frame.add(createUniprotMapperPanel());
        frame.add(createFileProcessingPanel());
        frame.add(createSaveOptionsPanel());
        JButton saveButton = createSaveButtonWithSpinner();
        frame.add(saveButton);

        excelFileReader.registerInputSelectedEventHandler(event -> setUpSheets());

        makeFrameDnD(frame);
        frame.setGlassPane(loadingSpinner.createLoadingGlassPane(frame));
        frame.setVisible(true);
    }

    /**
     * Creates the main application frame.
     *
     * @return The main application JFrame.
     */
    private JFrame createMainFrame() {
        JFrame frame = new JFrame("PSI-MI XML Maker");
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        frame.setJMenuBar(createMenuBar());
        frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return frame;
    }

    /**
     * Creates a JLabel to display the selected file.
     *
     * @return The JLabel showing the selected file.
     */
    private JLabel createFileLabel() {
        return excelFileReader.getFileLabel();
    }

    /**
     * Creates the panel for fetching files.
     *
     * @return The JPanel for file fetching.
     */
    private JPanel createFileFetcherPanel() {
        JPanel fileFetcherPanel = createFileFetcher();
        fileFetcherPanel.setBorder(new TitledBorder("1. Fetch file"));
        fileFetcherPanel.setMaximumSize(new Dimension(FRAME_WIDTH, 200));
        return fileFetcherPanel;
    }

    /**
     * Creates the panel for mapping Uniprot IDs.
     *
     * @return The JPanel for Uniprot mapping.
     */
    private JPanel createUniprotMapperPanel() {
        JPanel uniprotMapperPanel = uniprotMapperGui.uniprotPanel();
        uniprotMapperPanel.setBorder(new TitledBorder("3. Update the Uniprot ids"));
        return uniprotMapperPanel;
    }

    /**
     * Creates the panel for processing interaction participants.
     *
     * @return The JPanel for interaction participant creation.
     */
    private JPanel createFileProcessingPanel() {
        JPanel fileProcessingPanel = interactionsCreatorGui.participantCreatorPanel();
        fileProcessingPanel.setMaximumSize(new Dimension(FRAME_WIDTH, 300));
        fileProcessingPanel.setAutoscrolls(true);
        fileProcessingPanel.setBorder(new TitledBorder("4. Create the interaction participants"));
        return fileProcessingPanel;
    }

    /**
     * Creates the panel for generating PSI-MI XML files.
     *
     * @return The JPanel for PSI-MI XML file creation.
     */
    private JPanel createSaveOptionsPanel() {
        JPanel psiXmlMakerPanel = interactionWriterGui.createPsiMiXmlMakerPanel();
        psiXmlMakerPanel.setMaximumSize(new Dimension(FRAME_WIDTH, 200));
        psiXmlMakerPanel.setBorder(new TitledBorder("5. Save options"));
        return psiXmlMakerPanel;
    }

    /**
     * Enables drag-and-drop file support for the given JFrame.
     *
     * @param frame The JFrame to enable drag-and-drop on.
     */
    private void makeFrameDnD(JFrame frame) {
        frame.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                return handleFileImport(support);
            }
        });
    }

    /**
     * Handles file import through drag-and-drop.
     *
     * @param support The TransferSupport containing file information.
     * @return True if the file import is successful; false otherwise.
     */
    private boolean handleFileImport(TransferHandler.TransferSupport support) {
        try {
            Transferable transferable = support.getTransferable();
            List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
            if (!files.isEmpty()) {
                File file = files.get(0);
                processFile(file);
            }
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to import file", e);
            XmlMakerUtils.showErrorDialog("Failed to import file. Ensure it is a valid format.");
        }
        return false;
    }

    /**
     * Processes the given file by validating its type and setting up the appropriate panels.
     *
     * @param file The file to process.
     */
    public void processFile(File file) {
        String filePath = file.getAbsolutePath();
        if (!isValidFileType(filePath)) {
            XmlMakerUtils.showErrorDialog("Unsupported file type. Please provide a valid file (.xls, .xlsx, .csv, or .tsv).");
            return;
        }
        excelFileReader.selectFileOpener(filePath);
        setUpSheets();
    }

    /**
     * Validates the file type based on its extension.
     *
     * @param filePath The path of the file to validate.
     * @return True if the file type is valid; false otherwise.
     */
    private boolean isValidFileType(String filePath) {
        String fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        return fileExtension.equals("xls") || fileExtension.equals("xlsx") || fileExtension.equals("csv") || fileExtension.equals("tsv");
    }

    /**
     * Creates the menu bar for the application.
     *
     * @return The JMenuBar with menu options.
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menu = new JMenu("File");
        JMenuItem importFile = new JMenuItem("Import file");
        importFile.addActionListener(e -> fetchFile());
        menu.add(importFile);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem documentation = new JMenuItem("Documentation");
        documentation.addActionListener(e -> {
            File htmlFile = new File("target/reports/apidocs/index.html");
            try {
                Desktop.getDesktop().browse(htmlFile.toURI());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        helpMenu.add(documentation);

        JMenuItem userGuide = new JMenuItem("How to use");
        userGuide.addActionListener(e -> {
            File htmlFile = new File("target/reports/apidocs/index.html");
            try {
                Desktop.getDesktop().browse(htmlFile.toURI());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        helpMenu.add(userGuide);

        menuBar.add(menu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    /**
     * Creates the panel for file fetching.
     *
     * @return The JPanel for file fetching operations.
     */
    public JPanel createFileFetcher() {
        JPanel fileFetcherPanel = new JPanel();
        fileFetcherPanel.setLayout(new BoxLayout(fileFetcherPanel, BoxLayout.Y_AXIS));

        JPanel fileSelectionPanel = new JPanel();
        fileSelectionPanel.setLayout(new FlowLayout());
        fileSelectionPanel.setBorder(BorderFactory.createTitledBorder("1.1 Input the file to process"));
        JLabel fetchFileLabel = new JLabel("Drag or fetch the file to process");
        fetchFileLabel.setHorizontalAlignment(JLabel.CENTER);
        JButton fetchingButton = new JButton("Fetch file");
        fetchingButton.addActionListener(e -> fetchFile());
        fileSelectionPanel.add(fetchFileLabel);
        fileSelectionPanel.add(fetchingButton);

        JPanel pubmedInputPanel = new JPanel();
        pubmedInputPanel.setLayout(new FlowLayout());
        pubmedInputPanel.setBorder(BorderFactory.createTitledBorder("1.2 Enter the PubMed ID"));

        JTextField publicationTitleField = new JTextField("Publication PubMed ID");
        publicationTitleField.setEditable(true);
        JButton textValidationButton = new JButton("Submit");
        textValidationButton.addActionListener(e -> excelFileReader.setPublicationId(publicationTitleField.getText()));
        pubmedInputPanel.add(publicationTitleField);
        pubmedInputPanel.add(textValidationButton);

        fileFetcherPanel.add(fileSelectionPanel);
        fileFetcherPanel.add(pubmedInputPanel);
        return fileFetcherPanel;
    }

    /**
     * Opens a file chooser to fetch a file for processing.
     */
    private void fetchFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) return;
        File selectedFile = chooser.getSelectedFile();
        if (selectedFile == null) {
            XmlMakerUtils.showErrorDialog("No file was selected.");
            return;
        }
        processFile(selectedFile);
    }

    /**
     * Creates a save button with a loading spinner.
     *
     * @return A JButton configured to start the save operation with a loading spinner.
     */
    private JButton createSaveButtonWithSpinner() {
        JButton saveButton = new JButton("Create XML file(s)");

        saveButton.addActionListener(e -> {
            loadingSpinner.showSpinner();

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    try {
                        interactionsCreatorGui.interactionsCreator.setColumnAndIndex(interactionsCreatorGui.getDataAndIndexes());
                        interactionsCreatorGui.interactionsCreator.createParticipantsWithFileFormat();
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, "Error during save operation", ex);
                        SwingUtilities.invokeLater(() ->
                                XmlMakerUtils.showErrorDialog("An error occurred while saving the file.")
                        );
                        ex.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void done() {
                    loadingSpinner.hideSpinner();
                }
            };
            worker.execute();
        });

        return saveButton;
    }

    /**
     * Creates a restart button that restarts the application.
     *
     * @param currentFrame The current application frame to close upon restart.
     * @return The JButton configured to restart the application.
     */
    private JButton createRestartButton(JFrame currentFrame) {
        JButton restartButton = new JButton("Restart");

        restartButton.addActionListener(e -> {
            int confirmation = JOptionPane.showConfirmDialog(
                    currentFrame,
                    "Are you sure you want to restart the application?",
                    "Restart Confirmation",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirmation == JOptionPane.YES_OPTION) {
                currentFrame.dispose();

                SwingUtilities.invokeLater(() -> new XmlMakerGui().initialize());
            }
        });

        return restartButton;
    }

    private JPanel createFileFormaterPanel(){
        JPanel fileFormaterPanel = fileFormaterGui.getFileFormaterPanel();
        fileFormaterPanel.setAutoscrolls(true);
        fileFormaterPanel.setBorder(new TitledBorder("2. Format raw file"));
        return fileFormaterPanel;
    }

    /**
     * The main method to start the PSI-MI XML Maker application.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new XmlMakerGui().initialize());
    }

    private void setUpSheets() {
        excelFileReader.sheets.clear();
        excelFileReader.getSheets();
        fileFormaterGui.setUpSheets();
        uniprotMapperGui.setUpSheets();
        interactionsCreatorGui.setUpSheets();
        interactionsCreatorGui.setUpColumns();

    }
}
