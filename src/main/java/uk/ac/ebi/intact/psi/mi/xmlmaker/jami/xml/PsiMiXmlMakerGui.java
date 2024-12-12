package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class PsiMiXmlMakerGui {
    private final PsiMiXmlMaker xmlMaker;
    private JTextField saveLocationField;

    public PsiMiXmlMakerGui(InteractionsCreator interactionsCreator, ExcelFileReader excelFileReader) {
        this.xmlMaker = new PsiMiXmlMaker(interactionsCreator, excelFileReader);
    }

    public JPanel createPsiMiXmlMakerPanel() {
        JPanel xmlPanel = new JPanel();
        xmlPanel.setLayout(new BoxLayout(xmlPanel, BoxLayout.Y_AXIS));

        JPanel datePanel = createDateSelectionPanel();
        xmlPanel.add(datePanel);

        JPanel saveLocationPanel = createSaveLocationPanel();
        xmlPanel.add(saveLocationPanel);

        JButton processButton = new JButton("Create XML File");
        processButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        processButton.setToolTipText("Click to generate the XML file based on input data.");

        processButton.addActionListener(e -> {
                String saveLocation = getSaveLocation();
                try {
                    xmlMaker.interactionsWriter(saveLocation);
                    JOptionPane.showMessageDialog(null, "XML file created successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Error creating XML file: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

        xmlPanel.add(processButton);
        return xmlPanel;
    }

    private JPanel createDateSelectionPanel() {
        JPanel dateSelectionPanel = new JPanel();
        dateSelectionPanel.setLayout(new FlowLayout());
        dateSelectionPanel.setBorder(BorderFactory.createTitledBorder("Select Date"));

        JLabel dateLabel = new JLabel("Date:");
        SpinnerDateModel dateModel = new SpinnerDateModel();
        JSpinner dateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy");
        dateSpinner.setEditor(editor);

        dateSelectionPanel.add(dateLabel);
        dateSelectionPanel.add(dateSpinner);
        return dateSelectionPanel;
    }

    private JPanel createSaveLocationPanel() {
        JPanel saveLocationPanel = new JPanel();
        saveLocationPanel.setLayout(new FlowLayout());
        saveLocationPanel.setBorder(BorderFactory.createTitledBorder("Save Location"));

        JLabel saveLocationLabel = new JLabel("Save to:");
        saveLocationField = new JTextField(20);
        JButton browseButton = getJButton();

        saveLocationPanel.add(saveLocationLabel);
        saveLocationPanel.add(saveLocationField);
        saveLocationPanel.add(browseButton);

        return saveLocationPanel;
    }

    private JButton getJButton() {
        JButton browseButton = new JButton("Browse...");

        browseButton.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnValue = fileChooser.showSaveDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    saveLocationField.setText(selectedFile.getAbsolutePath());
                }
        });
        return browseButton;
    }

    public String getSaveLocation() {
        return saveLocationField.getText();
    }
}
