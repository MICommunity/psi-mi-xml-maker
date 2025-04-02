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
import java.net.URI;
import java.net.URISyntaxException;
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
                interactionWriterGui.getInteractionWriter());
        excelFileReader.registerInputSelectedEventHandler(event -> setUpSheets());
    }

    /**
     * Initializes and sets up the main user interface (UI) frame and its components.
     * The method performs the following tasks:
     * <ul>
     *     <li>Creates and configures the main frame window using {@link #createMainFrame()}.</li>
     *     <li>Sets up the layout for the content panel with a vertical {@link BoxLayout}.</li>
     *     <li>Creates a restart button, positions it centrally, and adds it to the content panel.</li>
     *     <li>Adds various UI panels to the content panel for handling file selection, formatting, UniProt mapping, and file processing.</li>
     *     <li>Creates and positions a save button with a spinner at the bottom of the content panel.</li>
     *     <li>Registers an event handler for input selection, triggering the setup of sheets.</li>
     *     <li>Configures a scroll pane to contain the content panel and adds vertical and horizontal scrollbars as needed.</li>
     *     <li>Sets up drag-and-drop functionality for the main frame.</li>
     *     <li>Sets a loading glass pane over the frame to indicate processing.</li>
     *     <li>Maximizes the window size and makes it visible.</li>
     * </ul>
     *
     * @see #createMainFrame()
     * @see #createRestartButton(JFrame)
     * @see #createFileLabel()
     * @see #createFileFetcherPanel()
     * @see #createFileFormaterPanel()
     * @see #createUniprotMapperPanel()
     * @see #createFileProcessingPanel()
     * @see #createSaveOptionsPanel()
     * @see #createSaveButtonWithSpinner()
     * @see #setUpSheets()
     * @see #makeFrameDragAndDrop(JFrame)
     */
    public void initialize() {
        JFrame frame = createMainFrame();

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JButton restartButton = createRestartButton(frame);
        restartButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(restartButton);

        contentPanel.add(createFileLabel());
        contentPanel.add(createFileFetcherPanel());
        contentPanel.add(createFileFormaterPanel());
        contentPanel.add(createUniprotMapperPanel());
        contentPanel.add(createFileProcessingPanel());
        contentPanel.add(createSaveOptionsPanel());

        JButton saveButton = createSaveButtonWithSpinner();
        saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(saveButton);

        excelFileReader.registerInputSelectedEventHandler(event -> setUpSheets());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        frame.setContentPane(scrollPane);

        makeFrameDragAndDrop(frame);
        frame.setGlassPane(loadingSpinner.createLoadingGlassPane(frame));
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
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
    private void makeFrameDragAndDrop(JFrame frame) {
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
        JMenuItem userGuide = new JMenuItem("How to use");
        userGuide.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/MICommunity/psi-mi-xml-maker"));
            } catch (IOException | URISyntaxException ex) {
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
        pubmedInputPanel.setBorder(BorderFactory.createTitledBorder("1.2 Enter the publication ID and database"));


        JTextField publicationDatabase = new JTextField("Publication database");
        publicationDatabase.setEditable(true);


        JTextField publicationTitleField = new JTextField("Publication ID");
        publicationTitleField.setEditable(true);
        JButton textValidationButton = new JButton("Submit");
        textValidationButton.addActionListener(e -> {
            excelFileReader.setPublicationId(publicationTitleField.getText());
            excelFileReader.setPublicationDb(publicationDatabase.getText());
        });
        pubmedInputPanel.add(publicationTitleField);
        pubmedInputPanel.add(publicationDatabase);
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
            if (excelFileReader.getPublicationId() == null) {
                XmlMakerUtils.showErrorDialog("Please provide a valid publication ID.");
                return;
            }

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
                                XmlMakerUtils.showErrorDialog("An error occurred while saving the file." +
                                        " Please verify that all columns are associated correctly. " + ex)
                        );
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

    /**
     * Creates and returns a panel for file formatting.
     * This panel is retrieved from the `fileFormaterGui` instance, with additional configurations applied:
     * - The panel is set to allow auto-scrolling.
     * - A border with the title "2. Format raw file" is added to the panel for better visual separation and description.
     *
     * @return The formatted JPanel containing file formatting UI elements.
     */
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

    /**
     * Sets up the sheets for the UI by clearing the current list of sheets and retrieving
     * the updated sheet list from the Excel file. It then triggers the setup of sheets in
     * different components of the application, ensuring that all relevant UI elements are
     * updated to reflect the available sheets.
     * This method performs the following steps:
     * - Clears the existing sheet list in the `excelFileReader`.
     * - Retrieves the updated list of sheets from the Excel file using the `getSheets()` method.
     * - Calls the `setUpSheets()` method in the `fileFormaterGui`, `uniprotMapperGui`, and
     *   `interactionsCreatorGui` components to ensure that all related parts of the UI are updated.
     */
    private void setUpSheets() {
        excelFileReader.sheets.clear();
        excelFileReader.getSheets();
        fileFormaterGui.setUpSheets();
        uniprotMapperGui.setUpSheets();
        interactionsCreatorGui.setUpSheets();
    }
}
