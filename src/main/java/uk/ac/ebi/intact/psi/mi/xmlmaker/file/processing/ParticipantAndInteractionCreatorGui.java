package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import lombok.Getter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
public class ParticipantAndInteractionCreatorGui {
    private final JComboBox<String> interactionTypeComboBox = new JComboBox<>();
    private final JComboBox<String> interactionDetectionMethodCombobox = new JComboBox<>();
    private final JComboBox<String> experimentalPreparation = new JComboBox<>();
    private final JComboBox<String> participantDetectionMethodCombobox = new JComboBox<>();
    private final JComboBox<String> baitBiologicalRole = new JComboBox<>();
    private final JComboBox<String> baitExperimentalRole = new JComboBox<>();
    private final JComboBox<String> preyBiologicalRole = new JComboBox<>();
    private final JComboBox<String> preyExperimentalRole = new JComboBox<>();

    public JPanel createParticipantAndInteractionCreatorGui() {
        JPanel participantAndInteractionCreatorPanel = new JPanel();
        participantAndInteractionCreatorPanel.setMaximumSize(new Dimension(2000, 200));
        participantAndInteractionCreatorPanel.setLayout(new GridLayout(2, 1));

        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(participantDetectionMethodCombobox, "Participant Detection Method"));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(interactionDetectionMethodCombobox, "Interaction Detection Method"));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(experimentalPreparation, "Experimental Preparation"));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(interactionTypeComboBox, "Interaction Type"));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(participantDetectionMethodCombobox,  "Participant Detection Method"));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(baitBiologicalRole, "Bait Biological Role"));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(preyBiologicalRole, "Prey Biological Role"));

        setUp();
        return participantAndInteractionCreatorPanel;
    }

    public void setUp(){
        setBiologicalRole();
        setExperimentalRole();
        setInteractionType();
        setParticipantDetectionMethod();
        setInteractionDetectionMethod();
    }

    public void setExperimentalRole() {
        for (ExperimentalRole experimentalRole : ExperimentalRole.values()) {
            baitExperimentalRole.addItem(experimentalRole.name);
            preyExperimentalRole.addItem(experimentalRole.name);
        }
    }

    public void setBiologicalRole() {
        for (BiologicalRole biologicalRole : BiologicalRole.values()) {
            baitBiologicalRole.addItem(biologicalRole.name);
            preyBiologicalRole.addItem(biologicalRole.name);
        }
    }

    public void setInteractionType() {
        for (InteractionType interactionType : InteractionType.values()) {
            interactionTypeComboBox.addItem(interactionType.name);
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

    public Map<String, String> getParticipantDetails(){
        Map<String, String> participantDetails = new HashMap<>();
        participantDetails.put(DataForRawFile.INTERACTION_DETECTION_METHOD.name, Objects.requireNonNull(interactionDetectionMethodCombobox.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.PARTICIPANT_DETECTION_METHOD.name, Objects.requireNonNull(participantDetectionMethodCombobox.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.EXPERIMENTAL_PREPARATION.name, Objects.requireNonNull(experimentalPreparation.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.BAIT_BIOLOGICAL_ROLE.name, Objects.requireNonNull(baitBiologicalRole.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.PREY_BIOLOGICAL_ROLE.name, Objects.requireNonNull(preyBiologicalRole.getSelectedItem()).toString());
        return participantDetails;
    }

}
