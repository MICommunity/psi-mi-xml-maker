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
import uk.ac.ebi.intact.psi.mi.xmlmaker.organisms.OrganismSelector;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapperGui;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.FileFetcherGui;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;

public class XmlMakerGui {

    private static final int FRAME_WIDTH = 2000;
    private static final int FRAME_HEIGHT = 2000;
    private static final Logger LOGGER = Logger.getLogger(XmlMakerGui.class.getName());
    private final ExcelFileReader excelFileReader;
    private final UniprotMapperGui uniprotMapperGui;
    private final InteractionsCreatorGui interactionsCreatorGui;
    private final PsiMiXmlMakerGui psiMiXmlMakerGui;
    private final OrganismSelector organismSelector;

    public XmlMakerGui() {
        this.excelFileReader = new ExcelFileReader();
        this.organismSelector = new OrganismSelector();
        this.uniprotMapperGui = new UniprotMapperGui(excelFileReader);
        this.interactionsCreatorGui =  new InteractionsCreatorGui(excelFileReader, uniprotMapperGui, organismSelector);
        this.psiMiXmlMakerGui = new PsiMiXmlMakerGui(interactionsCreatorGui.interactionsCreator);
    }

    public void initialize() {
        JFrame frame = createMainFrame();
        frame.add(createExcelFileLabel());
        frame.add(createFileFetcherPanel());
//        frame.add(createOrganismSelectorPanel());
        frame.add(createUniprotMapperPanel());
        frame.add(createFileProcessingPanel());
        frame.add(createPsiMiXmlMakerPanel());
        makeFrameDnD(frame);
        frame.setVisible(true);
    }

    private JFrame createMainFrame() {
        JFrame frame = new JFrame("PSI-MI XML Maker");
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        frame.setJMenuBar(createMenuBar());
        frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return frame;
    }

    private JLabel createExcelFileLabel() {
        return excelFileReader.getFileLabel();
    }

    private JPanel createFileFetcherPanel() {
        FileFetcherGui fileFetcherGui = new FileFetcherGui(excelFileReader);
        JPanel fileFetcherPanel = fileFetcherGui.createFileFetcher();
        fileFetcherPanel.setBorder(new TitledBorder("1. Fetch file"));
        return fileFetcherPanel;
    }

    private JPanel createUniprotMapperPanel() {
        JPanel uniprotMapperPanel = uniprotMapperGui.uniprotPanel();
        uniprotMapperPanel.setBorder(new TitledBorder("2. Update the Uniprot ids"));
        return uniprotMapperPanel;
    }

    private JPanel createFileProcessingPanel() {
        JPanel fileProcessingPanel = interactionsCreatorGui.participantCreatorPanel();
        fileProcessingPanel.setBorder(new TitledBorder("3. Create the interaction participants"));
        return fileProcessingPanel;
    }

    private JPanel createPsiMiXmlMakerPanel() {
        JPanel psiXmlMakerPanel = psiMiXmlMakerGui.PsiMiXmlMakerPanel();
        psiXmlMakerPanel.setBorder(new TitledBorder("4. Create the PSI-MI.xml file"));
        return psiXmlMakerPanel;
    }

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

    private void processFile(File file) {
        String filePath = file.getAbsolutePath();
        if (isValidFileType(filePath)) {
            XmlMakerUtils.processFile(filePath);
            excelFileReader.selectFileOpener(filePath);
            uniprotMapperGui.setUpSheets();
            interactionsCreatorGui.setUpSheets();
        } else {
            JOptionPane.showMessageDialog(null, "Unsupported file type. Please provide a valid file (.xls, .xlsx, .csv, or .tsv).", "Invalid File", JOptionPane.WARNING_MESSAGE);
        }
    }

    private boolean isValidFileType(String filePath) {
        String fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        return fileExtension.equals("xls") || fileExtension.equals("xlsx") || fileExtension.equals("csv") || fileExtension.equals("tsv");
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");

        JMenuItem importFile = new JMenuItem("Import file");
        importFile.addActionListener(e -> openFileChooser());

        menu.add(importFile);
        menuBar.add(menu);
        return menuBar;
    }

    private void openFileChooser() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            if (selectedFile != null) {
                processFile(selectedFile);
            }
        }
    }

    private JPanel createOrganismSelectorPanel(){
        JPanel organismSelectorPanel = organismSelector.organismSelectionPanel();
        organismSelectorPanel.setBorder(new TitledBorder("2. Select the organism"));
        return organismSelectorPanel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new XmlMakerGui().initialize());
    }
}
