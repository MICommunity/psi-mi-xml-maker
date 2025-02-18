package uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping;

import lombok.Getter;
import psidev.psi.mi.jami.model.CvTerm;

import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Objects;

/**
 * This class provides a graphical user interface (GUI) for selecting Uniprot ID
 * and participant type in a mapping process. It utilizes Swing components to
 * display choices for Uniprot IDs and participant types.
 */
@Getter
public class UniprotGeneralMapperGui {
    String selectedId;
    String selectedIdDb;
    private String selectedParticipantType;

    public UniprotGeneralMapperGui() {
    }

    /**
     * Displays a panel to the user allowing them to select a UniProt ID from a list
     * of options or enter a custom ID. This method uses a {@link ButtonGroup}
     * to display the options and allows users to specify the ID and database.
     * The method updates the {@code selectedId} and {@code selectedIdDb} fields.
     *
     * @param uniprotIdsGroup The {@link ButtonGroup} containing the possible UniProt IDs.
     * @param previousId The previous UniProt ID, used in the title of the panel.
     */
    public void getUniprotIdChoicePanel(ButtonGroup uniprotIdsGroup, String previousId) {
        JPanel buttonContainer = new JPanel();
        buttonContainer.setPreferredSize(new Dimension(1000, 700));

        JTextField otherIdField = new JTextField("Other id");
        JTextField otherIdDbField = new JTextField("Other id database");
        buttonContainer.add(otherIdField);
        buttonContainer.add(otherIdDbField);
        JComboBox<String> otherParticipantTypeComboBox = createParticipantTypeComboBox();
        buttonContainer.add(otherParticipantTypeComboBox);

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
//            if (!"Other id".equals(otherIdField.getText().trim())
//                    && !otherIdField.getText().trim().isEmpty()) {

                selectedId = otherIdField.getText().trim();


                if (otherIdDbField.getText().trim().isEmpty()) {
                    selectedIdDb = "CustomDatabase";
                } else {
                    selectedIdDb = otherIdDbField.getText().trim();
                }
                if (!Objects.requireNonNull(otherParticipantTypeComboBox.getSelectedItem()).toString().equals("Participant Type")) {
                    selectedParticipantType = otherParticipantTypeComboBox.getSelectedItem().toString();
                }
//                return;
//            }

            buttons = uniprotIdsGroup.getElements();
            while (buttons.hasMoreElements()) {
                AbstractButton button = buttons.nextElement();
                if (button.isSelected()) {
                    selectedId = button.getName();
                    selectedIdDb = "UniprotKB";
                    break;
                }
            }
        }

        System.out.println("selectedId: " + selectedId);
        System.out.println("selectedDb: " + selectedIdDb);
        System.out.println("participantType: " + selectedParticipantType);

//        else {
//            selectedId = null;
//        }
    }

    private JComboBox<String> createParticipantTypeComboBox() {
        JComboBox<String> comboBox = new JComboBox<>();
        String[] buttons = {"Participant type", "Gene", "Molecule", "Nucleic Acid", "Protein"};
        for (String button : buttons) {
            comboBox.addItem(button);
        }
        return comboBox;
    }

    /**
     * Displays a panel to the user allowing them to select the participant type
     * (e.g., Gene, Molecule, Nucleic Acid, or Protein). The method updates the
     * {@code selectedParticipantType} field based on the user's selection.
     *
     * @param previousId The previous ID, used in the title of the panel.
     */
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
        }
    }
}
