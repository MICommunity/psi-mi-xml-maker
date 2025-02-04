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
    private final List<String> baitExperimentalPreparationNames = new ArrayList<>();
    private final JComboBox<String> preyExperimentalPreparation = new JComboBox<>();

    private final JComboBox<String> baitBiologicalRole = new JComboBox<>();
    private final JComboBox<String> preyBiologicalRole = new JComboBox<>();

    private final JComboBox<String> baitOrganism = new JComboBox<>();
    private final JComboBox<String> preyOrganism = new JComboBox<>();

    private final JComboBox<String> baitFeatureStartLocation = new JComboBox<>();
    private final JComboBox<String> baitFeatureEndLocation = new JComboBox<>();
    private final JComboBox<String> baitFeatureType = new JComboBox<>();
    private final JComboBox<String> baitFeatureRangeType = new JComboBox<>();
    private final JComboBox<String> baitFeatureXrefDb = new JComboBox<>();
    private final JComboBox<String> baitFeatureXref = new JComboBox<>();


    private final JComboBox<String> baitIdDatabase = new JComboBox<>();
    private final JComboBox<String> preyIdDatabase = new JComboBox<>();

    @Getter
    private final JSpinner numberOfExperimentalPrep = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));

    private final JPanel baitExperimentalPanel = new JPanel(new GridLayout(2, 1));

    private final Dimension panelDimension = new Dimension(100, 400);

    /**
     * Creates the GUI panel for selecting participant and interaction details.
     * The panel contains dropdowns for different attributes related to interaction data.
     *
     * @return The JPanel containing the GUI components.
     */
    public JPanel createParticipantAndInteractionCreatorGui() {
        JPanel participantAndInteractionCreatorPanel = new JPanel();

        participantAndInteractionCreatorPanel.add(createGeneralInformationPanel());
        participantAndInteractionCreatorPanel.add(createBaitPanel());
        participantAndInteractionCreatorPanel.add(createPreyPanel());

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

        setBaitFeatureRangeType();
        baitFeatureXref.setEditable(true);

        updateBaitExperimentalPreparations((int) numberOfExperimentalPrep.getValue());

        addSpinnerListener();
    }

    /**
     * Populates the bait and prey biological role dropdowns with predefined roles.
     */
    public void setBiologicalRole() {
        for (String termName: XmlMakerUtils.getTermsFromOls(DataAndMiID.BIOLOGICAL_ROLE.miId)){
            baitBiologicalRole.addItem(termName);
            preyBiologicalRole.addItem(termName);
        }
    }

    /**
     * Populates the bait and prey experimental preparation dropdowns.
     */
    public void setExperimentalPreparations(){
        for (String termName: XmlMakerUtils.getTermsFromOls(DataAndMiID.EXPERIMENTAL_PREPARATION.miId)){
            preyExperimentalPreparation.addItem(termName);
            baitExperimentalPreparationNames.add(termName);
        }
    }

    /**
     * Populates the interaction detection method dropdown with predefined methods.
     */
    public void setInteractionDetectionMethod() {
        for (String termName: XmlMakerUtils.getTermsFromOls(DataAndMiID.INTERACTION_DETECTION_METHOD.miId)){
            interactionDetectionMethodCombobox.addItem(termName);
        }
    }

    /**
     * Populates the participant detection method dropdown with predefined methods.
     */
    public void setParticipantDetectionMethod(){
        for (String termName: XmlMakerUtils.getTermsFromOls(DataAndMiID.PARTICIPANT_DETECTION_METHOD.miId)){
            participantDetectionMethodCombobox.addItem(termName);
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
        participantDetails.put(DataForRawFile.BAIT_FEATURE_RANGE_TYPE.name, Objects.requireNonNull(baitFeatureRangeType.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.BAIT_FEATURE_XREF.name, Objects.requireNonNull(baitFeatureXref.getSelectedItem()).toString());
        participantDetails.put(DataForRawFile.BAIT_FEATURE_XREF_DB.name, Objects.requireNonNull(baitFeatureXrefDb.getSelectedItem()).toString());

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
        for (String termName: XmlMakerUtils.getTermsFromOls(DataAndMiID.FEATUTRE_TYPE.miId)){
            baitFeatureType.addItem(termName);
        }
    }

    private void setBaitFeatureRangeType(){
        for (String termName: XmlMakerUtils.getTermsFromOls(DataAndMiID.FEATURE_RANGE_TYPE.miId)){
            baitFeatureRangeType.addItem(termName);
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
            comboBox.addItem("Experimental Preparation");

            for (String termName: baitExperimentalPreparationNames){
                comboBox.addItem(termName);
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
        baitIdDatabase.addItem("uniprotKB");
        preyIdDatabase.addItem("uniprotKB");
        baitFeatureXrefDb.addItem("uniprotKB");

        baitIdDatabase.addItem("geneid");
        preyIdDatabase.addItem("geneid");
        baitFeatureXrefDb.addItem("geneid");

        for (String termName: XmlMakerUtils.getTermsFromOls(DataAndMiID.DATABASES.miId)){
            baitIdDatabase.addItem(termName);
            preyIdDatabase.addItem(termName);
            baitFeatureXrefDb.addItem(termName);
        }

    }

    public JPanel createBaitPanel() {
        JPanel baitPanel = new JPanel();
        baitPanel.setLayout(new GridLayout(6, 1));
        baitPanel.setPreferredSize(new Dimension(600, 300));

        baitPanel.setBorder(BorderFactory.createTitledBorder(" 2.3 Select baits information"));
        baitPanel.setMaximumSize(panelDimension);


        baitPanel.add(XmlMakerUtils.setComboBoxDimension(baitIdDatabase, DataForRawFile.BAIT_ID_DB.name));
        baitPanel.add(XmlMakerUtils.setComboBoxDimension(baitBiologicalRole, DataForRawFile.BAIT_BIOLOGICAL_ROLE.name));
        baitPanel.add(XmlMakerUtils.setComboBoxDimension(baitOrganism, DataForRawFile.BAIT_ORGANISM.name));

        baitPanel.add(XmlMakerUtils.setComboBoxDimension(baitFeatureType, DataForRawFile.BAIT_FEATURE_TYPE.name));
        baitPanel.add(XmlMakerUtils.setComboBoxDimension(baitFeatureStartLocation, DataForRawFile.BAIT_FEATURE_START_LOCATION.name));
        baitPanel.add(XmlMakerUtils.setComboBoxDimension(baitFeatureEndLocation, DataForRawFile.BAIT_FEATURE_END_LOCATION.name));
        baitPanel.add(XmlMakerUtils.setComboBoxDimension(baitFeatureRangeType, DataForRawFile.BAIT_FEATURE_RANGE_TYPE.name));
        baitPanel.add(XmlMakerUtils.setComboBoxDimension(baitFeatureXref, DataForRawFile.BAIT_FEATURE_XREF.name));
        baitPanel.add(XmlMakerUtils.setComboBoxDimension(baitFeatureXrefDb, DataForRawFile.BAIT_FEATURE_XREF_DB.name));


        numberOfExperimentalPrep.setPreferredSize(new Dimension(200, 100));
        numberOfExperimentalPrep.setBorder(BorderFactory.createTitledBorder("Select number of experimental preparations"));
        baitPanel.add(numberOfExperimentalPrep);
        baitPanel.add(baitExperimentalPanel);

        return baitPanel;
    }

    public JPanel createPreyPanel() {
        JPanel preyPanel = new JPanel();
        preyPanel.setLayout(new GridLayout(4, 1));
        preyPanel.setPreferredSize(new Dimension(300, 300));
        preyPanel.setBorder(BorderFactory.createTitledBorder(" 2.4 Select preys information"));
        preyPanel.setMaximumSize(panelDimension);

        preyPanel.add(XmlMakerUtils.setComboBoxDimension(preyIdDatabase, DataForRawFile.PREY_ID_DB.name));
        preyPanel.add(XmlMakerUtils.setComboBoxDimension(preyExperimentalPreparation, DataForRawFile.PREY_EXPERIMENTAL_PREPARATION.name));
        preyPanel.add(XmlMakerUtils.setComboBoxDimension(preyBiologicalRole, DataForRawFile.PREY_BIOLOGICAL_ROLE.name));
        preyPanel.add(XmlMakerUtils.setComboBoxDimension(preyOrganism, DataForRawFile.PREY_ORGANISM.name));
        preyPanel.add(XmlMakerUtils.setComboBoxDimension(preyExperimentalPreparation, DataForRawFile.PREY_EXPERIMENTAL_PREPARATION.name));

        return preyPanel;
    }

    public JPanel createGeneralInformationPanel() {
        JPanel generalInformationPanel = new JPanel();
        generalInformationPanel.setLayout(new GridLayout(3, 1));
        generalInformationPanel.setPreferredSize(new Dimension(300, 300));
        generalInformationPanel.setBorder(BorderFactory.createTitledBorder(" 2.2 Select general information"));

        generalInformationPanel.add(XmlMakerUtils.setComboBoxDimension(participantDetectionMethodCombobox, DataForRawFile.PARTICIPANT_DETECTION_METHOD.name));
        generalInformationPanel.add(XmlMakerUtils.setComboBoxDimension(interactionDetectionMethodCombobox, DataForRawFile.INTERACTION_DETECTION_METHOD.name));

        return generalInformationPanel;
    }

}
