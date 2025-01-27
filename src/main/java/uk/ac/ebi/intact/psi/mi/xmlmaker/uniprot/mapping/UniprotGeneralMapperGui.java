package uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping;

import lombok.Getter;
import javax.swing.*;
import java.util.Enumeration;

public class UniprotGeneralMapperGui {
    final UniprotGeneralMapper generalMapper;
    @Getter
    String selectedId;
    @Getter
    String selectedIdDb;

    public UniprotGeneralMapperGui(UniprotGeneralMapper generalMapper) {
        this.generalMapper = generalMapper;
    }

    public void getUniprotIdChoicePanel(ButtonGroup uniprotIdsGroup, String previousId) {
        JPanel buttonContainer = new JPanel();
        JTextField otherIdField = new JTextField("Other id");
        JTextField otherIdDbField = new JTextField("Other id database");
        buttonContainer.add(otherIdField);
        buttonContainer.add(otherIdDbField);
        buttonContainer.setBorder(BorderFactory.createTitledBorder("Select Uniprot ID for: " + previousId));
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.Y_AXIS));
        Enumeration<AbstractButton> buttons = uniprotIdsGroup.getElements();

        while (buttons.hasMoreElements()) {
            AbstractButton button = buttons.nextElement();
            buttonContainer.add(button);
        }

        int result = JOptionPane.showConfirmDialog(null, buttonContainer, "Choose a Uniprot ID",
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            if (!"Other id".equals(otherIdField.getText().trim())
                    && !otherIdField.getText().trim().isEmpty()) {
                selectedId = otherIdField.getText().trim();

                if (otherIdDbField.getText().trim().isEmpty()) {
                    selectedIdDb = "CustomDatabase";
                } else {
                    selectedIdDb = otherIdDbField.getText().trim();
                }
                return;
            }

            buttons = uniprotIdsGroup.getElements();
            while (buttons.hasMoreElements()) {
                AbstractButton button = buttons.nextElement();
                if (button.isSelected()) {
                    selectedId = button.getName();
                    selectedIdDb = "UniprotKB";
                    break;
                }
            }
        } else {
            selectedId = null;
        }
    }
}
