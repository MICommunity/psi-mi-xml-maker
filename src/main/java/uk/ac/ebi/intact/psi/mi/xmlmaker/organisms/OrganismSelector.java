package uk.ac.ebi.intact.psi.mi.xmlmaker.organisms;

import javax.swing.*;
import java.awt.*;

public class OrganismSelector {
    public final SuggestedOrganisms suggestedOrganisms = new SuggestedOrganisms();
    public final JComboBox<String> suggestedOrganismsIds = new JComboBox<>();
    public String organismSelected = "-1";

    public OrganismSelector() {
        setUpSuggestedOrganismsIds();
        addComboBoxListener();
    }

    public JPanel organismSelectionPanel() {
        JPanel organismSelectionPanel = new JPanel();
        organismSelectionPanel.setBounds(10, 70, 400, 400);

        suggestedOrganismsIds.setModel(new DefaultComboBoxModel<>(suggestedOrganisms.getOrganismDisplayNames()));
        suggestedOrganismsIds.setEditable(true);

        JButton organismValidation = new JButton("Validate");
        organismValidation.addActionListener(e -> {
            organismSelected = getSelectedOrganism();
            JOptionPane.showMessageDialog(null, "Organism ID validated: " + organismSelected);
        });

        organismSelectionPanel.add(suggestedOrganismsIds).setPreferredSize(new Dimension(400, 20));
        organismSelectionPanel.add(organismValidation);
        return organismSelectionPanel;
    }

    public void setUpSuggestedOrganismsIds() {
        suggestedOrganismsIds.setModel(new DefaultComboBoxModel<>(suggestedOrganisms.getOrganismDisplayNames()));
    }

    public String getSelectedOrganism() {
        String selectedDisplayName = (String) suggestedOrganismsIds.getSelectedItem();
        if (selectedDisplayName == null || selectedDisplayName.trim().isEmpty()) {
            return "-1";
        }
        return suggestedOrganisms.getOrganismId(selectedDisplayName);
    }

    private void addComboBoxListener() {
        suggestedOrganismsIds.addActionListener(e -> {
            organismSelected = getSelectedOrganism();
        });
    }
}
