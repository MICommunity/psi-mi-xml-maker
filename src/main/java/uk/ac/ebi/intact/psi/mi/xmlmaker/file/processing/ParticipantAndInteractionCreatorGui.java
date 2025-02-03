package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import lombok.Getter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The {@code ParticipantAndInteractionCreatorGui} class provides a graphical user interface
 * for selecting participant and interaction details in a molecular interaction dataset.
 * Users can specify detection methods, biological roles, organisms, and experimental
 * preparations using dropdowns.
 *
 * <p>Features include:</p>
 * <ul>
 *   <li>Selection of interaction and participant detection methods</li>
 *   <li>Specification of biological roles for bait and prey</li>
 *   <li>Editable organism selection with taxonomic ID lookup</li>
 *   <li>Feature selection for bait proteins</li>
 * </ul>
 *
 * This class is used to retrieve participant details for interaction formatting.
 *
 */
@Getter
public class ParticipantAndInteractionCreatorGui {
    private final JComboBox<String> interactionDetectionMethodCombobox = new JComboBox<>();
    private final JComboBox<String> participantDetectionMethodCombobox = new JComboBox<>();

    private final List<JComboBox<String>> baitExperimentalPreparationList = new ArrayList<>();
    private final JComboBox<String> preyExperimentalPreparation = new JComboBox<>();

    private final JComboBox<String> baitBiologicalRole = new JComboBox<>();
    private final JComboBox<String> preyBiologicalRole = new JComboBox<>();

    private final JComboBox<String> baitOrganism = new JComboBox<>();
    private final JComboBox<String> preyOrganism = new JComboBox<>();

    private final JComboBox<String> baitFeatureStartLocation = new JComboBox<>();
    private final JComboBox<String> baitFeatureEndLocation = new JComboBox<>();
    private final JComboBox<String> baitFeatureType = new JComboBox<>();
    private final JComboBox<String> featureRangeType = new JComboBox<>();

    private final JComboBox<String> baitIdDatabase = new JComboBox<>();
    private final JComboBox<String> preyIdDatabase = new JComboBox<>();

    @Getter
    private final JSpinner numberOfExperimentalPrep = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
    private final JPanel baitExperimentalPanel = new JPanel(new GridLayout(0, 1));



    /**
     * Creates the GUI panel for selecting participant and interaction details.
     * The panel contains dropdowns for different attributes related to interaction data.
     *
     * @return The JPanel containing the GUI components.
     */
    public JPanel createParticipantAndInteractionCreatorGui() {
        JPanel participantAndInteractionCreatorPanel = new JPanel();
        participantAndInteractionCreatorPanel.setMaximumSize(new Dimension(1000, 400));
        participantAndInteractionCreatorPanel.setLayout(new GridLayout(4, 1));

        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(baitIdDatabase, DataForRawFile.BAIT_ID_DB.name));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(preyIdDatabase, DataForRawFile.PREY_ID_DB.name));

        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(participantDetectionMethodCombobox, DataForRawFile.PARTICIPANT_DETECTION_METHOD.name));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(interactionDetectionMethodCombobox, DataForRawFile.INTERACTION_DETECTION_METHOD.name));

        participantAndInteractionCreatorPanel.add(numberOfExperimentalPrep);
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(preyExperimentalPreparation, DataForRawFile.PREY_EXPERIMENTAL_PREPARATION.name));

        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(baitBiologicalRole, DataForRawFile.BAIT_BIOLOGICAL_ROLE.name));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(preyBiologicalRole, DataForRawFile.PREY_BIOLOGICAL_ROLE.name));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(baitOrganism, DataForRawFile.BAIT_ORGANISM.name));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(preyOrganism, DataForRawFile.PREY_ORGANISM.name));

        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(baitFeatureType, DataForRawFile.BAIT_FEATURE_TYPE.name));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(baitFeatureStartLocation, DataForRawFile.BAIT_FEATURE_START_LOCATION.name));
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(baitFeatureEndLocation, DataForRawFile.BAIT_FEATURE_END_LOCATION.name));

        participantAndInteractionCreatorPanel.add(numberOfExperimentalPrep);

        participantAndInteractionCreatorPanel.add(baitExperimentalPanel);
        participantAndInteractionCreatorPanel.add(XmlMakerUtils.setComboBoxDimension(preyExperimentalPreparation, DataForRawFile.PREY_EXPERIMENTAL_PREPARATION.name));

        setUp();
        return participantAndInteractionCreatorPanel;
    }

    /**
     * Populates the dropdowns with relevant data for each category.
     * This method is called during initialization to set up all the required selections.
     */
    private void setUp() {
        setParticipantDetectionMethod();
        setInteractionDetectionMethod();
        setExperimentalPreparations();
        setFeatureType();
        setFeatureLocation();
        setOrganisms();
        setBiologicalRole();
        setDatabases();
        updateBaitExperimentalPreparations((int) numberOfExperimentalPrep.getValue());

        addSpinnerListener();
    }

    /**
     * Populates the bait and prey biological role dropdowns with predefined roles.
     */
    public void setBiologicalRole() {
        for (BiologicalRole biologicalRole : BiologicalRole.values()) {
            baitBiologicalRole.addItem(biologicalRole.name);
            preyBiologicalRole.addItem(biologicalRole.name);
        }
    }

    /**
     * Populates the bait and prey experimental preparation dropdowns.
     */
    public void setExperimentalPreparations(){
        for (ExperimentalPreparation experimentalPreparation : ExperimentalPreparation.values()) {
//            baitExperimentalPreparation.addItem(experimentalPreparation.description);
            preyExperimentalPreparation.addItem(experimentalPreparation.description);
        }
    }

    /**
     * Populates the interaction detection method dropdown with predefined methods.
     */
    public void setInteractionDetectionMethod() {
        for (InteractionDetectionMethod interactionDetectionMethod : InteractionDetectionMethod.values()) {
            interactionDetectionMethodCombobox.addItem(interactionDetectionMethod.name);
        }
    }

    /**
     * Populates the participant detection method dropdown with predefined methods.
     */
    public void setParticipantDetectionMethod(){
        for (ParticipantDetectionMethod participantDetectionMethod : ParticipantDetectionMethod.values()) {
            participantDetectionMethodCombobox.addItem(participantDetectionMethod.name);
        }
    }

    /**
     * Populates the bait and prey organism dropdowns with available organisms and their taxonomic IDs.
     * These dropdowns are editable, allowing users to manually input values.
     */
    public void setOrganisms(){
        baitOrganism.setEditable(true);
        preyOrganism.setEditable(true);
        for (ParticipantOrganism participantOrganism : ParticipantOrganism.values()) {
            preyOrganism.addItem(participantOrganism.name + " (" + participantOrganism.taxId + ")");
            baitOrganism.addItem(participantOrganism.name + " (" + participantOrganism.taxId + ")");
        }
    }

    /**
     * Retrieves the selected participant and interaction details from the GUI.
     *
     * @return A map containing the selected details, mapped by their respective data keys.
     */
    public Map<String, String> getParticipantDetails(){
        Map<String, String> participantDetails = new HashMap<>();

        participantDetails.put(DataForRawFile.BAIT_ID_DB.name, Objects.requireNonNull(baitIdDatabase.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.PREY_ID_DB.name, Objects.requireNonNull(preyIdDatabase.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.INTERACTION_DETECTION_METHOD.name, Objects.requireNonNull(interactionDetectionMethodCombobox.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.BAIT_EXPERIMENTAL_PREPARATION.name, Objects.requireNonNull(getBaitExperimentalPreparationsAsString()));
        participantDetails.put(DataForRawFile.PREY_EXPERIMENTAL_PREPARATION.name, Objects.requireNonNull(preyExperimentalPreparation.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.PARTICIPANT_DETECTION_METHOD.name, Objects.requireNonNull(participantDetectionMethodCombobox.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.BAIT_BIOLOGICAL_ROLE.name, Objects.requireNonNull(baitBiologicalRole.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.PREY_BIOLOGICAL_ROLE.name, Objects.requireNonNull(preyBiologicalRole.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.BAIT_ORGANISM.name, XmlMakerUtils.fetchTaxIdForOrganism(Objects.requireNonNull(baitOrganism.getSelectedItem()).toString()));
        participantDetails.put(DataForRawFile.PREY_ORGANISM.name, XmlMakerUtils.fetchTaxIdForOrganism(Objects.requireNonNull(preyOrganism.getSelectedItem()).toString()));

        participantDetails.put(DataForRawFile.BAIT_FEATURE_TYPE.name, Objects.requireNonNull(baitFeatureType.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.BAIT_FEATURE_START_LOCATION.name, Objects.requireNonNull(baitFeatureStartLocation.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.BAIT_FEATURE_END_LOCATION.name, Objects.requireNonNull(baitFeatureEndLocation.getSelectedItem()).toString());

        return participantDetails;
    }

    /**
     * Sets up dropdowns for bait feature start and end locations.
     * Users can select predefined values or enter custom ones.
     */
    private void setFeatureLocation(){
        baitFeatureStartLocation.setEditable(true);
        baitFeatureStartLocation.addItem("c-term");
        baitFeatureStartLocation.addItem("n-term");
        baitFeatureStartLocation.addItem("undetermined");

        baitFeatureEndLocation.setEditable(true);
        baitFeatureEndLocation.addItem("c-term");
        baitFeatureEndLocation.addItem("n-term");
        baitFeatureEndLocation.addItem("undetermined");
    }

    private void setFeatureType(){
        for (FeatureType featureType : FeatureType.values()) {
            baitFeatureType.addItem(featureType.name);
        }
    }


    private void addSpinnerListener() {
        numberOfExperimentalPrep.addChangeListener(e -> {
            int value = (int) numberOfExperimentalPrep.getValue();
            SwingUtilities.invokeLater(() -> updateBaitExperimentalPreparations(value));
        });
    }


    private void updateBaitExperimentalPreparations(int count) {
        baitExperimentalPanel.removeAll();
        baitExperimentalPreparationList.clear();

        for (int i = 0; i < count; i++) {
            JComboBox<String> comboBox = new JComboBox<>();
            for (ExperimentalPreparation experimentalPreparation : ExperimentalPreparation.values()) {
                comboBox.addItem(experimentalPreparation.description);
            }
            baitExperimentalPreparationList.add(comboBox);
            baitExperimentalPanel.add(XmlMakerUtils.setComboBoxDimension(comboBox, "Bait Experimental Preparation " + (i + 1)));
        }

        baitExperimentalPanel.revalidate();
        baitExperimentalPanel.repaint();
    }

    public String getBaitExperimentalPreparationsAsString() {
        List<String> selectedPreparations = new ArrayList<>();

        for (JComboBox<String> comboBox : baitExperimentalPreparationList) {
            String selectedValue = (String) comboBox.getSelectedItem();
            if (selectedValue != null) {
                selectedPreparations.add(selectedValue);
            }
        }

        return String.join(" ; ", selectedPreparations);
    }

    private void setDatabases(){
        for (Databases databases : Databases.values()) {
            baitIdDatabase.addItem(databases.abbrev);
            preyIdDatabase.addItem(databases.abbrev);
        }
    }
}
