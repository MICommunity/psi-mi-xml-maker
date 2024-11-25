package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import javax.swing.*;
import java.awt.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.XmlMakerUtils;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileFetcherGui {

    private static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 15);
    private static final Logger LOGGER = Logger.getLogger(FileFetcherGui.class.getName());

    private final ExcelFileReader excelFileReader;

    public FileFetcherGui(ExcelFileReader excelFileReader) {
        this.excelFileReader = excelFileReader;
    }

    public JPanel createFileFetcher() {
        JPanel fileFetcherPanel = new JPanel();
        JLabel fetchFileLabel = new JLabel("Drag or fetch the file to process");
        fetchFileLabel.setFont(LABEL_FONT);
        fetchFileLabel.setHorizontalAlignment(JLabel.CENTER);

        JButton fetchingButton = new JButton("Fetch file");
        fetchingButton.addActionListener(e -> fetchFile());

        fileFetcherPanel.add(fetchFileLabel);
        fileFetcherPanel.add(fetchingButton);
        return fileFetcherPanel;
    }

    private void fetchFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            try {
                File selectedFile = chooser.getSelectedFile();
                XmlMakerUtils.processFile(selectedFile.getAbsolutePath());
                excelFileReader.selectFileOpener(selectedFile.getAbsolutePath());
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error processing file", ex);
                JOptionPane.showMessageDialog(null, "Failed to process file. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
