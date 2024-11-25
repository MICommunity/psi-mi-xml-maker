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
        xmlPanel.add(processButton);

        return xmlPanel;
    }

}
