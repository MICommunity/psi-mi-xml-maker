package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import javax.swing.*;

public class PsiMiXmlMakerGui {
    PsiMiXmlMaker maker;

    public JPanel PsiMiXmlMakerPanel() {
        JPanel xmlPanel = new JPanel();
        xmlPanel.setBounds(10, 70, 400, 400);

        JButton processButton = new JButton("Create XML file");
        processButton.addActionListener(e -> {

        });

        JPanel panel = publicationInformationPanel();
        xmlPanel.add(panel);
        xmlPanel.add(processButton);

        return xmlPanel;
    }

    public JPanel publicationInformationPanel() {
        JPanel publicationInformationPanel = new JPanel();
        publicationInformationPanel.setLayout(new BoxLayout(publicationInformationPanel, BoxLayout.Y_AXIS));

        JTextField publicationTitleField = new JTextField("Publication pubmed ID");
        publicationTitleField.setEditable(true);

        publicationInformationPanel.add(publicationTitleField);
        return publicationInformationPanel;
    }

}
