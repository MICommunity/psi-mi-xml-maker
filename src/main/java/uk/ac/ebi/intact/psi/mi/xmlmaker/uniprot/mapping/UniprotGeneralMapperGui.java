package uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping;

import lombok.Getter;
import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;

public class UniprotGeneralMapperGui {
    final UniprotGeneralMapper generalMapper;
    @Getter
    String selectedId;
    @Getter
    String selectedIdDb;

    @Getter
    private String selectedParticipantType;

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

    public void getParticipantChoicePanel(String previousId) {
        JPanel participantPanel = new JPanel();
        participantPanel.setBorder(BorderFactory.createTitledBorder("No UniProt ID found for: " + previousId));
        participantPanel.setLayout(new BoxLayout(participantPanel, BoxLayout.Y_AXIS));
        participantPanel.setPreferredSize(new Dimension(300, 150));

        JLabel typeLabel = new JLabel("Select the participant type:");
        participantPanel.add(typeLabel);

        ButtonGroup typeGroup = new ButtonGroup();
        String[] buttons = {"Gene", "Molecule", "Nucleic Acid", "Protein"};

        for (String button : buttons) {
            JRadioButton radioButton = new JRadioButton(button);
            typeGroup.add(radioButton);
            participantPanel.add(radioButton);
        }

        int result = JOptionPane.showConfirmDialog(null, participantPanel, "Choose Participant Type",
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            Enumeration<AbstractButton> elements = typeGroup.getElements();
            while (elements.hasMoreElements()) {
                AbstractButton button = elements.nextElement();
                if (button.isSelected()) {
                    selectedParticipantType = button.getText();
                    break;
                }
            }
        } else {
            selectedParticipantType = null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Participant Choice");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            UniprotGeneralMapper generalMapper = null; // Replace with a valid instance if needed
            UniprotGeneralMapperGui gui = new UniprotGeneralMapperGui(generalMapper);

            gui.getParticipantChoicePanel("P12345");

            frame.pack();
            frame.setVisible(true);
        });
    }
}
