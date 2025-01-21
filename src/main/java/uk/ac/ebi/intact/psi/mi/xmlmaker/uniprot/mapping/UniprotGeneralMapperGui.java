package uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping;

import lombok.Getter;
import javax.swing.*;
import java.util.Enumeration;

public class UniprotGeneralMapperGui {
    UniprotGeneralMapper generalMapper;
    @Getter
    String selectedId;

    public UniprotGeneralMapperGui(UniprotGeneralMapper generalMapper) {
        this.generalMapper = generalMapper;
    }

    public void getUniprotIdChoicePanel(ButtonGroup uniprotIdsGroup) {
        JPanel buttonContainer = new JPanel();
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.Y_AXIS));
        Enumeration<AbstractButton> buttons = uniprotIdsGroup.getElements();

        while (buttons.hasMoreElements()) {
            AbstractButton button = buttons.nextElement();
            buttonContainer.add(button);
        }

        int result = JOptionPane.showConfirmDialog(null, buttonContainer, "Choose a Uniprot ID",
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            buttons = uniprotIdsGroup.getElements();

            while (buttons.hasMoreElements()) {
                AbstractButton button = buttons.nextElement();
                if (button.isSelected()) {
                    selectedId = button.getName();
                    break;
                }
            }
        } else {
            selectedId = null;
        }

//        return buttonContainer;
    }
}
