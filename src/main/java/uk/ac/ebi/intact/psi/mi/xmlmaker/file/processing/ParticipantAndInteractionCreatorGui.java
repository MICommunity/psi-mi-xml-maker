package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import lombok.Getter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.FileUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
public class ParticipantAndInteractionCreatorGui {
    private final JComboBox<String> interactionDetectionMethodCombobox = new JComboBox<>();
    private final JComboBox<String> baitExperimentalPreparation = new JComboBox<>();
    private final JComboBox<String> preyExperimentalPreparation = new JComboBox<>();
    private final JComboBox<String> participantDetectionMethodCombobox = new JComboBox<>();
    private final JComboBox<String> baitBiologicalRole = new JComboBox<>();
    private final JComboBox<String> preyBiologicalRole = new JComboBox<>();

    private final JComboBox<String> baitOrganism = new JComboBox<>();
    private final JComboBox<String> preyOrganism = new JComboBox<>();


    public JPanel createParticipantAndInteractionCreatorGui() {
        JPanel participantAndInteractionCreatorPanel = new JPanel();
        participantAndInteractionCreatorPanel.setMaximumSize(new Dimension(2000, 400));
        participantAndInteractionCreatorPanel.setLayout(new GridLayout(2, 1));

        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(participantDetectionMethodCombobox, DataForRawFile.PARTICIPANT_DETECTION_METHOD.name));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(interactionDetectionMethodCombobox, DataForRawFile.INTERACTION_DETECTION_METHOD.name));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(baitExperimentalPreparation, DataForRawFile.BAIT_EXPERIMENTAL_PREPARATION.name));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(preyExperimentalPreparation, DataForRawFile.PREY_EXPERIMENTAL_PREPARATION.name));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(baitBiologicalRole, DataForRawFile.BAIT_BIOLOGICAL_ROLE.name));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(preyBiologicalRole, DataForRawFile.PREY_BIOLOGICAL_ROLE.name));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(baitOrganism, DataForRawFile.BAIT_ORGANISM.name));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(preyOrganism, DataForRawFile.PREY_ORGANISM.name));

        setUp();
        return participantAndInteractionCreatorPanel;
    }

    public void setUp(){
        setParticipantDetectionMethod();
        setInteractionDetectionMethod();
        setExperimentalPreparations();
        setBiologicalRole();
        setOrganisms();
    }

    public void setBiologicalRole() {
        for (BiologicalRole biologicalRole : BiologicalRole.values()) {
            baitBiologicalRole.addItem(biologicalRole.name);
            preyBiologicalRole.addItem(biologicalRole.name);
        }
    }

    public void setExperimentalPreparations(){
        for (ExperimentalPreparation experimentalPreparation : ExperimentalPreparation.values()) {
            baitExperimentalPreparation.addItem(experimentalPreparation.description);
            preyExperimentalPreparation.addItem(experimentalPreparation.description);
        }
    }

    public void setInteractionDetectionMethod() {
        for (InteractionDetectionMethod interactionDetectionMethod : InteractionDetectionMethod.values()) {
            interactionDetectionMethodCombobox.addItem(interactionDetectionMethod.name);
        }
    }

    public void setParticipantDetectionMethod(){
        for (ParticipantDetectionMethod participantDetectionMethod : ParticipantDetectionMethod.values()) {
            participantDetectionMethodCombobox.addItem(participantDetectionMethod.name);
        }
    }

    public void setOrganisms(){
        baitOrganism.setEditable(true);
        preyOrganism.setEditable(true); //todo: see for the size
//        ((JTextField) baitOrganism.getEditor().getEditorComponent()).setMaximumSize(new Dimension(100, 10));
//        ((JTextField) preyOrganism.getEditor().getEditorComponent()).setMaximumSize(new Dimension(100, 10));
        for (ParticipantOrganism participantOrganism : ParticipantOrganism.values()) {
            preyOrganism.addItem(participantOrganism.name + " (" + participantOrganism.taxId + ")");
            baitOrganism.addItem(participantOrganism.name + " (" + participantOrganism.taxId + ")");
        }
    }

    public Map<String, String> getParticipantDetails(){
        Map<String, String> participantDetails = new HashMap<>();
        participantDetails.put(DataForRawFile.INTERACTION_DETECTION_METHOD.name, Objects.requireNonNull(interactionDetectionMethodCombobox.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.BAIT_EXPERIMENTAL_PREPARATION.name, Objects.requireNonNull(baitExperimentalPreparation.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.PREY_EXPERIMENTAL_PREPARATION.name, Objects.requireNonNull(preyExperimentalPreparation.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.PARTICIPANT_DETECTION_METHOD.name, Objects.requireNonNull(participantDetectionMethodCombobox.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.BAIT_BIOLOGICAL_ROLE.name, Objects.requireNonNull(baitBiologicalRole.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.PREY_BIOLOGICAL_ROLE.name, Objects.requireNonNull(preyBiologicalRole.getSelectedItem()).toString());

        participantDetails.put(DataForRawFile.BAIT_ORGANISM.name, XmlMakerUtils.fetchTaxIdForOrganism(Objects.requireNonNull(baitOrganism.getSelectedItem()).toString()));
        participantDetails.put(DataForRawFile.PREY_ORGANISM.name, XmlMakerUtils.fetchTaxIdForOrganism(Objects.requireNonNull(preyOrganism.getSelectedItem()).toString()));

        return participantDetails;
    }

}
