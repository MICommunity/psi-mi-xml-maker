package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;


public class PsiMiXmlMakerGui {
    PsiMiXmlMaker xmlMaker;
    InteractionsCreator interactionsCreator;

    public PsiMiXmlMakerGui(InteractionsCreator interactionsCreator) {
        this.interactionsCreator = interactionsCreator;
        xmlMaker = new PsiMiXmlMaker(interactionsCreator);
    }
    public JPanel PsiMiXmlMakerPanel() {
        JPanel xmlPanel = new JPanel();
        xmlPanel.setBounds(10, 70, 400, 400);

        JButton processButton = new JButton("Create XML file");
        processButton.addActionListener(e -> {
            xmlMaker.interactionsWriter();
        });

        JPanel panel = publicationInformationPanel();
        xmlPanel.add(panel);
        xmlPanel.add(dateSelectionPanel());
        xmlPanel.add(processButton);
        return xmlPanel;
    }

    public JPanel publicationInformationPanel() {
        JPanel publicationInformationPanel = new JPanel();
        publicationInformationPanel.setLayout(new BoxLayout(publicationInformationPanel, BoxLayout.Y_AXIS));

        JTextField publicationTitleField = new JTextField("Publication pubmed ID");
        publicationTitleField.setEditable(true);
        JButton textValidationButton = new JButton("Submit");
        textValidationButton.addActionListener(e -> {
            System.out.println(publicationTitleField.getText());
            xmlMaker.setPublicationId(publicationTitleField.getText());
        });

        publicationInformationPanel.add(publicationTitleField);
//        publicationInformationPanel.add(textValidationButton);
        return publicationInformationPanel;
    }

    public JPanel dateSelectionPanel() {
        JPanel dateSelectionPanel = new JPanel();
        dateSelectionPanel.setLayout(new BoxLayout(dateSelectionPanel, BoxLayout.Y_AXIS));

        SpinnerDateModel dateModel = new SpinnerDateModel();
        JSpinner dateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy");
        dateSpinner.setEditor(editor);
//        JButton dateSelectionButton = new JButton("Select publication release date");
//        dateSelectionButton.addActionListener(e -> {
////            dateSelectionPanel();
//        });

        dateSelectionPanel.add(dateSpinner);
//        dateSelectionPanel.add(dateSelectionButton);
        return dateSelectionPanel;
    }
}
