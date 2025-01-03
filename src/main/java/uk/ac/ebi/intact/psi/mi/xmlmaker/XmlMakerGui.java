package uk.ac.ebi.intact.psi.mi.xmlmaker;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml.InteractionsCreatorGui;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml.PsiMiXmlMakerGui;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapperGui;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;

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
    private final PsiMiXmlMakerGui psiMiXmlMakerGui;


    /**
     * Constructs the XmlMakerGui instance, initializing all dependent components.
     */
    public XmlMakerGui() {
        this.excelFileReader = new ExcelFileReader();
        this.uniprotMapperGui = new UniprotMapperGui(excelFileReader);
        this.interactionsCreatorGui =  new InteractionsCreatorGui(excelFileReader, uniprotMapperGui);
        this.psiMiXmlMakerGui = new PsiMiXmlMakerGui(interactionsCreatorGui.interactionsCreator, excelFileReader);
    }

    /**
     * Initializes the main GUI components and displays the application frame.
     */
    public void initialize() {
        JFrame frame = createMainFrame();
        frame.add(createFileLabel());
        frame.add(createFileFetcherPanel());
        frame.add(createUniprotMapperPanel());
        frame.add(createFileProcessingPanel());
        frame.add(createPsiMiXmlMakerPanel());
        makeFrameDnD(frame);
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
        return fileFetcherPanel;
    }

    /**
     * Creates the panel for mapping Uniprot IDs.
     *
     * @return The JPanel for Uniprot mapping.
     */
    private JPanel createUniprotMapperPanel() {
        JPanel uniprotMapperPanel = uniprotMapperGui.uniprotPanel();
        uniprotMapperPanel.setBorder(new TitledBorder("2. Update the Uniprot ids"));
        return uniprotMapperPanel;
    }

    /**
     * Creates the panel for processing interaction participants.
     *
     * @return The JPanel for interaction participant creation.
     */
    private JPanel createFileProcessingPanel() {
        JPanel fileProcessingPanel = interactionsCreatorGui.participantCreatorPanel();
        fileProcessingPanel.setAutoscrolls(true);
        fileProcessingPanel.setBorder(new TitledBorder("3. Create the interaction participants"));
        return fileProcessingPanel;
    }

    /**
     * Creates the panel for generating PSI-MI XML files.
     *
     * @return The JPanel for PSI-MI XML file creation.
     */
    private JPanel createPsiMiXmlMakerPanel() {
        JPanel psiXmlMakerPanel = psiMiXmlMakerGui.createPsiMiXmlMakerPanel();
        psiXmlMakerPanel.setBorder(new TitledBorder("4. Create the PSI-MI.xml file"));
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
            JOptionPane.showMessageDialog(null, "Failed to import file. Ensure it is a valid format.", "Error", JOptionPane.ERROR_MESSAGE);
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
        if (isValidFileType(filePath)) {
            excelFileReader.selectFileOpener(filePath);
            uniprotMapperGui.setUpSheets();
            interactionsCreatorGui.setUpSheets();
        } else {
            JOptionPane.showMessageDialog(null, "Unsupported file type. Please provide a valid file (.xls, .xlsx, .csv, or .tsv).", "Invalid File", JOptionPane.WARNING_MESSAGE);
        }
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
        menuBar.add(menu);
        return menuBar;
    }

    /**
     * Creates the panel for file fetching.
     *
     * @return The JPanel for file fetching operations.
     */
    public JPanel createFileFetcher() {
        JPanel fileFetcherPanel = new JPanel();
        JLabel fetchFileLabel = new JLabel("Drag or fetch the file to process");
        fetchFileLabel.setHorizontalAlignment(JLabel.CENTER);

        JButton fetchingButton = new JButton("Fetch file");
        fetchingButton.addActionListener(e -> fetchFile());

        JTextField publicationTitleField = new JTextField("Publication pubmed ID");
        publicationTitleField.setEditable(true);
        JButton textValidationButton = new JButton("Submit");
        textValidationButton.addActionListener(e -> excelFileReader.setPublicationId(publicationTitleField.getText()));

        fileFetcherPanel.add(fetchFileLabel);
        fileFetcherPanel.add(fetchingButton);
        fileFetcherPanel.add(publicationTitleField);
        fileFetcherPanel.add(textValidationButton);
        return fileFetcherPanel;
    }

    /**
     * Opens a file chooser to fetch a file for processing.
     */
    private void fetchFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            if (selectedFile != null) {
                try {
                    processFile(selectedFile);
                } catch (Exception e) {
                    System.err.println("Error processing file: " + e.getMessage());
                    LOGGER.log(Level.SEVERE, "Error processing file: " + selectedFile.getAbsolutePath(), e);
                }
            } else {
                System.err.println("No file was selected.");
            }
        }
    }

    /**
     * The main method to start the PSI-MI XML Maker application.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new XmlMakerGui().initialize());
    }
}
