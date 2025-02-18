package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import lombok.Getter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml.DataTypeAndColumn;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

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

    private final JComboBox<String> hostOrganism = new JComboBox<>();

    private final List<JComboBox<String>> baitExperimentalPreparationList = new ArrayList<>();
    private final List<String> baitExperimentalPreparationNames = new ArrayList<>();
    private final JComboBox<String> baitBiologicalRole = new JComboBox<>();
    private final JComboBox<String> baitOrganism = new JComboBox<>();
    private final JComboBox<String> baitIdDatabase = new JComboBox<>();

    private final JComboBox<String> preyExperimentalPreparation = new JComboBox<>();
    private final JComboBox<String> preyBiologicalRole = new JComboBox<>();
    private final JComboBox<String> preyOrganism = new JComboBox<>();
    private final JComboBox<String> preyIdDatabase = new JComboBox<>();

    private final JComboBox<String> preyExpressedInOrganism = new JComboBox<>();
    private final JComboBox<String> baitExpressedInOrganism = new JComboBox<>();

    @Getter
    private final JSpinner numberOfExperimentalPrep = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));

    private final JPanel baitExperimentalPanel = new JPanel(new GridLayout(2, 1));

    private final int HEIGHT = 300;
    private final Dimension panelDimension = new Dimension(500, HEIGHT);

    private final List<String> featureRangeTypeCache = new ArrayList<>();
    private final List<String> featureTypeCache = new ArrayList<>();
    private final List<String> dbCache = new ArrayList<>();

    private final List<Map<String, JComboBox<String>>> preyFeaturesComboBoxes = new ArrayList<>();
    private final List<Map<String, JComboBox<String>>> baitFeaturesComboBoxes = new ArrayList<>();

    private final List<JComboBox<String>> baitFeatureXrefs = new ArrayList<>();
    private final List<JComboBox<String>> preyFeatureXrefs = new ArrayList<>();
    private final List<JComboBox<String>> baitFeatureXrefDb = new ArrayList<>();
    private final List<JComboBox<String>> preyFeatureXrefDb = new ArrayList<>();

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
        for (String termName : XmlMakerUtils.getTermsFromOls(DataAndMiID.BIOLOGICAL_ROLE.miId)) {
            baitBiologicalRole.addItem(termName);
            preyBiologicalRole.addItem(termName);
        }
    }

    /**
     * Populates the bait and prey experimental preparation dropdowns.
     */
    public void setExperimentalPreparations() {
        for (String termName : XmlMakerUtils.getTermsFromOls(DataAndMiID.EXPERIMENTAL_PREPARATION.miId)) {
            preyExperimentalPreparation.addItem(termName);
            baitExperimentalPreparationNames.add(termName);
        }
    }

    /**
     * Populates the interaction detection method dropdown with predefined methods.
     */
    public void setInteractionDetectionMethod() {
        for (String termName : XmlMakerUtils.getTermsFromOls(DataAndMiID.INTERACTION_DETECTION_METHOD.miId)) {
            interactionDetectionMethodCombobox.addItem(termName);
        }
    }

    /**
     * Populates the participant detection method dropdown with predefined methods.
     */
    public void setParticipantDetectionMethod() {
        for (String termName : XmlMakerUtils.getTermsFromOls(DataAndMiID.PARTICIPANT_DETECTION_METHOD.miId)) {
            participantDetectionMethodCombobox.addItem(termName);
        }
    }

    /**
     * Populates the bait and prey organism dropdowns with available organisms and their taxonomic IDs.
     * These dropdowns are editable, allowing users to manually input values.
     */
    public void setOrganisms() {
        baitOrganism.setEditable(true);
        preyOrganism.setEditable(true);
        hostOrganism.setEditable(true);
        preyExpressedInOrganism.setEditable(true);
        baitExpressedInOrganism.setEditable(true);
        for (ParticipantOrganism participantOrganism : ParticipantOrganism.values()) {
            preyOrganism.addItem(participantOrganism.name + " (" + participantOrganism.taxId + ")");
            baitOrganism.addItem(participantOrganism.name + " (" + participantOrganism.taxId + ")");
            hostOrganism.addItem(participantOrganism.name + " (" + participantOrganism.taxId + ")");
            preyExpressedInOrganism.addItem(participantOrganism.name + " (" + participantOrganism.taxId + ")");
            baitExpressedInOrganism.addItem(participantOrganism.name + " (" + participantOrganism.taxId + ")");
        }
    }

    /**
     * Retrieves the selected participant and interaction details from the GUI.
     *
     * @return A map containing the selected details, mapped by their respective data keys.
     */
    public Map<String, String> getParticipantDetails() {
        Map<String, String> participantDetails = new HashMap<>();

        participantDetails.put(DataForRawFile.BAIT_ID_DB.name,
                isValueNull(baitIdDatabase.getSelectedItem(), DataForRawFile.BAIT_ID_DB.name));
        participantDetails.put(DataForRawFile.PREY_ID_DB.name,
                isValueNull(preyIdDatabase.getSelectedItem(), DataForRawFile.PREY_ID_DB.name));
        participantDetails.put(DataForRawFile.INTERACTION_DETECTION_METHOD.name,
                isValueNull(interactionDetectionMethodCombobox.getSelectedItem(), DataForRawFile.INTERACTION_DETECTION_METHOD.name));
        participantDetails.put(DataForRawFile.BAIT_EXPERIMENTAL_PREPARATION.name,
                isValueNull(getBaitExperimentalPreparationsAsString(), DataForRawFile.BAIT_EXPERIMENTAL_PREPARATION.name));
        participantDetails.put(DataForRawFile.PREY_EXPERIMENTAL_PREPARATION.name,
                isValueNull(preyExperimentalPreparation.getSelectedItem(), DataForRawFile.PREY_EXPERIMENTAL_PREPARATION.name));
        participantDetails.put(DataForRawFile.PARTICIPANT_DETECTION_METHOD.name,
                isValueNull(participantDetectionMethodCombobox.getSelectedItem(), DataForRawFile.PARTICIPANT_DETECTION_METHOD.name));
        participantDetails.put(DataForRawFile.BAIT_BIOLOGICAL_ROLE.name,
                isValueNull(baitBiologicalRole.getSelectedItem(), DataForRawFile.BAIT_BIOLOGICAL_ROLE.name));
        participantDetails.put(DataForRawFile.PREY_BIOLOGICAL_ROLE.name,
                isValueNull(preyBiologicalRole.getSelectedItem(), DataForRawFile.PREY_BIOLOGICAL_ROLE.name));

        participantDetails.put(DataForRawFile.BAIT_ORGANISM.name,
                isValueNull(XmlMakerUtils.fetchTaxIdForOrganism(Objects.requireNonNull(baitOrganism.getSelectedItem()).toString()), DataForRawFile.BAIT_ORGANISM.name));
        participantDetails.put(DataForRawFile.PREY_ORGANISM.name,
                isValueNull(XmlMakerUtils.fetchTaxIdForOrganism(Objects.requireNonNull(preyOrganism.getSelectedItem()).toString()), DataForRawFile.PREY_ORGANISM.name));
        participantDetails.put(DataForRawFile.HOST_ORGANISM.name, isValueNull(XmlMakerUtils.fetchTaxIdForOrganism(Objects.requireNonNull(hostOrganism.getSelectedItem()).toString()), DataForRawFile.HOST_ORGANISM.name));
        participantDetails.put(DataForRawFile.PREY_EXPRESSED_IN_ORGANISM.name, isValueNull(XmlMakerUtils.fetchTaxIdForOrganism(Objects.requireNonNull(preyExpressedInOrganism.getSelectedItem()).toString()), DataForRawFile.PREY_EXPRESSED_IN_ORGANISM.name));
        participantDetails.put(DataForRawFile.BAIT_EXPRESSED_IN_ORGANISM.name, isValueNull(XmlMakerUtils.fetchTaxIdForOrganism(Objects.requireNonNull(baitExpressedInOrganism.getSelectedItem()).toString()), DataForRawFile.BAIT_EXPRESSED_IN_ORGANISM.name));


        return participantDetails;
    }

    /**
     * Checks if the given value is null or matches the specified keyName.
     * If the value is null or equal to the keyName, an empty string is returned.
     * Otherwise, the string representation of the value is returned.
     *
     * @param value The object whose value is to be checked. Can be any object or null.
     * @param keyName The key name to compare the string value against.
     *
     * @return An empty string if the value is null or equals the keyName, otherwise the string representation of the value.
     */
    private String isValueNull(Object value, String keyName) {
        if (value == null) {
            return "";
        }
        String stringValue = value.toString();
        return stringValue.equals(keyName) ? "" : stringValue;
    }

    /**
     * Sets up dropdowns for bait feature start and end locations.
     * Users can select predefined values or enter custom ones.
     */
    private void setFeatureLocation(JComboBox<String> featureLocation) {
        featureLocation.setEditable(true);
        featureLocation.addItem("c-term");
        featureLocation.addItem("n-term");
        featureLocation.addItem("undetermined");
    }

    /**
     * Sets the feature type options in the given combo box based on terms fetched from the OLS.
     *
     * @param featureTypeComboBox The combo box to populate with feature type options.
     */
    private void setFeatureType(JComboBox<String> featureTypeComboBox) {
        if (featureTypeCache.isEmpty()) {
            featureTypeCache.addAll(XmlMakerUtils.getTermsFromOls(DataAndMiID.FEATUTRE_TYPE.miId));
        }
        for (String termName : featureTypeCache) {
            featureTypeComboBox.addItem(termName);
        }
    }

    /**
     * Sets the feature range type options in the given combo box based on terms fetched from the OLS.
     *
     * @param featureRangeTypeComboBox The combo box to populate with feature range type options.
     */
    private void setFeatureRangeType(JComboBox<String> featureRangeTypeComboBox) {
        if (featureRangeTypeCache.isEmpty()) {
            featureRangeTypeCache.addAll(XmlMakerUtils.getTermsFromOls(DataAndMiID.FEATURE_RANGE_TYPE.miId));
        }

        for (String termName : featureRangeTypeCache) {
            featureRangeTypeComboBox.addItem(termName);
        }
    }

    /**
     * Adds a change listener to the number of experimental preparations spinner, which updates the
     * bait experimental preparations when the value changes.
     */
    private void addSpinnerListener() {
        numberOfExperimentalPrep.addChangeListener(e -> {
            int value = (int) numberOfExperimentalPrep.getValue();
            SwingUtilities.invokeLater(() -> updateBaitExperimentalPreparations(value));
        });
    }

    /**
     * Updates the number of bait experimental preparations based on the given count.
     * Dynamically adds or removes combo boxes from the bait experimental panel.
     *
     * @param count The number of bait experimental preparations to display.
     */
    private void updateBaitExperimentalPreparations(int count) {
        int currentCount = baitExperimentalPanel.getComponentCount();

        for (int i = currentCount; i < count; i++) {
            JComboBox<String> comboBox = new JComboBox<>();
            comboBox.addItem("Experimental Preparation");

            for (String termName : baitExperimentalPreparationNames) {
                comboBox.addItem(termName);
            }

            baitExperimentalPreparationList.add(comboBox);
            baitExperimentalPanel.add(XmlMakerUtils.setComboBoxDimension(comboBox, "Bait Experimental Preparation " + (i + 1)));
        }

        for (int i = currentCount - 1; i >= count; i--) {
            baitExperimentalPanel.remove(i);
            baitExperimentalPreparationList.remove(i);
        }

        baitExperimentalPanel.revalidate();
        baitExperimentalPanel.repaint();
    }

    /**
     * Retrieves the selected bait experimental preparations as a semicolon-separated string.
     *
     * @return A string representing the selected bait experimental preparations.
     */
    public String getBaitExperimentalPreparationsAsString() {
        List<String> selectedPreparations = new ArrayList<>();

        for (JComboBox<String> comboBox : baitExperimentalPreparationList) {
            String selectedValue = isValueNull(comboBox.getSelectedItem(), DataTypeAndColumn.EXPERIMENTAL_PREPARATION.name);
            if (selectedValue != null && !selectedValue.isEmpty()) {
                selectedPreparations.add(selectedValue);
            }
        }

        return String.join(" ; ", selectedPreparations);
    }

    /**
     * Sets the available databases for bait and prey ID combo boxes.
     * Adds "uniprotKB" and "geneid" as options, followed by other terms fetched from the OLS.
     */
    private void setDatabases() {
        baitIdDatabase.addItem("uniprotKB");
        preyIdDatabase.addItem("uniprotKB");

        baitIdDatabase.addItem("geneid");
        preyIdDatabase.addItem("geneid");

        for (String termName : XmlMakerUtils.getTermsFromOls(DataAndMiID.DATABASES.miId)) {
            baitIdDatabase.addItem(termName);
            preyIdDatabase.addItem(termName);
            dbCache.add(termName);
        }
    }

    /**
     * Sets the available feature database options in the given combo box.
     * Adds "uniprotKB" and "geneid" as options, followed by the terms in the cache.
     *
     * @param featureDbComboBox The combo box to populate with database options.
     */
    private void setFeatureDb(JComboBox<String> featureDbComboBox) {
        featureDbComboBox.addItem("uniprotKB");
        featureDbComboBox.addItem("geneid");

        for (String termName : dbCache) {
            featureDbComboBox.addItem(termName);
        }
    }

    //PANELS

    /**
     * Creates and returns a panel containing bait-related information including combo boxes for
     * bait ID database, biological role, organism, and experimental preparations.
     *
     * @return A panel with bait information.
     */
    public JPanel createBaitPanel() {
        JPanel baitPanel = new JPanel();
        baitPanel.setLayout(new GridLayout(4, 1));
        baitPanel.setPreferredSize(panelDimension);

        baitPanel.setBorder(BorderFactory.createTitledBorder(" 2.3 Select baits information"));
        baitPanel.setMaximumSize(panelDimension);


        baitPanel.add(XmlMakerUtils.setComboBoxDimension(baitIdDatabase, DataForRawFile.BAIT_ID_DB.name));
        baitPanel.add(XmlMakerUtils.setComboBoxDimension(baitBiologicalRole, DataForRawFile.BAIT_BIOLOGICAL_ROLE.name));
        baitPanel.add(XmlMakerUtils.setComboBoxDimension(baitOrganism, DataForRawFile.BAIT_ORGANISM.name));
        baitPanel.add(XmlMakerUtils.setComboBoxDimension(baitExpressedInOrganism, DataForRawFile.BAIT_EXPRESSED_IN_ORGANISM.name));

        numberOfExperimentalPrep.setPreferredSize(new Dimension(200, 100));
        numberOfExperimentalPrep.setBorder(BorderFactory.createTitledBorder("Select number of experimental preparations"));
        numberOfExperimentalPrep.add(baitExperimentalPanel);
        baitPanel.add(numberOfExperimentalPrep);
        baitPanel.add(baitExperimentalPanel);

        JButton createFeatureButton = new JButton("Create feature(s)");
        createFeatureButton.addActionListener(e -> {
            JPanel featureOptionPanel = createFeatureOptionPanel(true);
            JOptionPane.showConfirmDialog(null, featureOptionPanel,"Feature Selection", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        });
        baitPanel.add(createFeatureButton);

        return baitPanel;
    }

    /**
     * Creates and returns a panel containing prey-related information including combo boxes for
     * prey ID database, experimental preparation, biological role, organism, and experimental preparations.
     *
     * @return A panel with prey information.
     */
    public JPanel createPreyPanel() {
        JPanel preyPanel = new JPanel();
        preyPanel.setLayout(new GridLayout(4, 1));
        preyPanel.setPreferredSize(panelDimension);
        preyPanel.setBorder(BorderFactory.createTitledBorder(" 2.4 Select preys information"));
        preyPanel.setMaximumSize(panelDimension);

        preyPanel.add(XmlMakerUtils.setComboBoxDimension(preyIdDatabase, DataForRawFile.PREY_ID_DB.name));
        preyPanel.add(XmlMakerUtils.setComboBoxDimension(preyExperimentalPreparation, DataForRawFile.PREY_EXPERIMENTAL_PREPARATION.name));
        preyPanel.add(XmlMakerUtils.setComboBoxDimension(preyBiologicalRole, DataForRawFile.PREY_BIOLOGICAL_ROLE.name));
        preyPanel.add(XmlMakerUtils.setComboBoxDimension(preyOrganism, DataForRawFile.PREY_ORGANISM.name));
        preyPanel.add(XmlMakerUtils.setComboBoxDimension(preyExpressedInOrganism, DataForRawFile.PREY_EXPRESSED_IN_ORGANISM.name));
        preyPanel.add(XmlMakerUtils.setComboBoxDimension(preyExperimentalPreparation, DataForRawFile.PREY_EXPERIMENTAL_PREPARATION.name));

        JButton createFeatureButton = new JButton("Create feature(s)");
        createFeatureButton.addActionListener(e -> {
            JPanel featureOptionPanel = createFeatureOptionPanel(false);
            JOptionPane.showConfirmDialog(null, featureOptionPanel,
                    "Feature Selection", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        });

        preyPanel.add(createFeatureButton);

        return preyPanel;
    }

    /**
     * Creates and returns a panel for general information with combo boxes for participant detection
     * method, interaction detection method, and host organism.
     *
     * @return A panel with general information.
     */
    public JPanel createGeneralInformationPanel() {
        JPanel generalInformationPanel = new JPanel();
        generalInformationPanel.setLayout(new GridLayout(3, 1));
        generalInformationPanel.setPreferredSize(new Dimension(200, HEIGHT));
        generalInformationPanel.setBorder(BorderFactory.createTitledBorder(" 2.2 Select general information"));

        generalInformationPanel.add(XmlMakerUtils.setComboBoxDimension(participantDetectionMethodCombobox, DataForRawFile.PARTICIPANT_DETECTION_METHOD.name));
        generalInformationPanel.add(XmlMakerUtils.setComboBoxDimension(interactionDetectionMethodCombobox, DataForRawFile.INTERACTION_DETECTION_METHOD.name));
        generalInformationPanel.add(XmlMakerUtils.setComboBoxDimension(hostOrganism, DataForRawFile.HOST_ORGANISM.name));

        return generalInformationPanel;
    }

    // MULTIPLE FEATURES CREATION

    /**
     * Creates a panel allowing the user to select the number of features to create, with additional
     * options for each feature such as feature start and end locations, type, and range type.
     * Also adds listeners to update the feature panel dynamically as the number of features changes.
     *
     * @param bait A boolean indicating whether the feature panel is for bait (true) or prey (false).
     * @return A panel for selecting features.
     */
    private JPanel createFeatureOptionPanel(boolean bait) {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(1500, 500));

        JSpinner numberOfFeature = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        numberOfFeature.setBorder(BorderFactory.createTitledBorder(" Select number of features"));
        JPanel featureContainerPanel = new JPanel();
        featureContainerPanel.setLayout(new BoxLayout(featureContainerPanel, BoxLayout.Y_AXIS));

        numberOfFeature.addChangeListener(e -> {
            int value = (int) numberOfFeature.getValue();
            SwingUtilities.invokeLater(() -> {
                if (bait){
                    baitFeaturesComboBoxes.clear();
                } else {
                    preyFeaturesComboBoxes.clear();
                }
                updateFeaturePanel(featureContainerPanel, value, bait);
            });
        });

        updateFeaturePanel(featureContainerPanel, (int) numberOfFeature.getValue(), bait);

        mainPanel.add(numberOfFeature, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(featureContainerPanel), BorderLayout.CENTER);

        return mainPanel;
    }

    /**
     * Updates the feature panel to show the correct number of feature panels based on the given count.
     * Each feature panel contains combo boxes for feature attributes.
     *
     * @param featureContainerPanel The panel containing the feature combo boxes.
     * @param count The number of feature panels to display.
     * @param bait A boolean indicating whether the features are for bait (true) or prey (false).
     */
    private void updateFeaturePanel(JPanel featureContainerPanel, int count, boolean bait) {
        int currentCount = featureContainerPanel.getComponentCount();
        for (int i = currentCount; i < count; i++) {
            featureContainerPanel.add(createFeaturePanel(i, bait));
        }
        for (int i = currentCount - 1; i >= count; i--) {
            featureContainerPanel.remove(i);
        }

        featureContainerPanel.revalidate();
        featureContainerPanel.repaint();
    }

    /**
     * Creates a single feature panel that contains combo boxes for feature attributes (start location,
     * end location, type, and range type) and allows the user to add feature xrefs.
     *
     * @param featureNumber The index of the feature being created.
     * @param bait A boolean indicating whether the feature is for bait (true) or prey (false).
     * @return A panel representing one feature.
     */
    private JPanel createFeaturePanel(int featureNumber, boolean bait) {
        Map<String, JComboBox<String>> comboBoxMap = new HashMap<>();

        JPanel featurePanel = new JPanel();
        featurePanel.setBorder(BorderFactory.createTitledBorder("Create feature " + (featureNumber + 1)));

        List<DataForRawFile> featureAttributes = Arrays.asList(
                DataForRawFile.FEATURE_START_LOCATION,
                DataForRawFile.FEATURE_END_LOCATION,
                DataForRawFile.FEATURE_TYPE,
                DataForRawFile.FEATURE_RANGE_TYPE
        );

        for (DataForRawFile attribute : featureAttributes) {
            JComboBox<String> comboBox = new JComboBox<>();
            comboBoxMap.put(attribute.name, comboBox);
            XmlMakerUtils.setComboBoxDimension(comboBox, attribute.name);
            featurePanel.add(comboBox);
        }

        featurePanel.add(createFeatureXrefPanel(bait));

        setFeatureType(comboBoxMap.get(DataForRawFile.FEATURE_TYPE.name));
        setFeatureRangeType(comboBoxMap.get(DataForRawFile.FEATURE_RANGE_TYPE.name));
        setFeatureLocation(comboBoxMap.get(DataForRawFile.FEATURE_START_LOCATION.name));
        setFeatureLocation(comboBoxMap.get(DataForRawFile.FEATURE_END_LOCATION.name));

        if (bait) {
            baitFeaturesComboBoxes.add(comboBoxMap);
        } else {
            preyFeaturesComboBoxes.add(comboBoxMap);
        }

        return featurePanel;
    }

    /**
     * Retrieves the feature data from the feature combo boxes for either bait or prey, depending on the given parameter.
     *
     * @param bait A boolean indicating whether the feature data is for bait (true) or prey (false).
     * @return A list of maps containing the feature data for each feature.
     */
    public List<Map<String, String>> getFeaturesData(boolean bait){
        List<Map<String, String>> featuresData;
        if (bait) {
            featuresData = getFeaturesDataFromCombobox(baitFeaturesComboBoxes, true);
        } else {
            featuresData = getFeaturesDataFromCombobox(preyFeaturesComboBoxes, false);
        }

        return featuresData;
    }

    /**
     * Retrieves the feature data from the provided combo boxes, returning it as a list of maps where each map contains
     * key-value pairs representing feature attributes (start location, end location, feature type, range type, and xref data).
     *
     * @param comboBoxes A list of maps, where each map holds a set of combo boxes corresponding to feature attributes.
     * @param bait A boolean flag indicating whether the feature data corresponds to a bait (true) or a prey (false).
     *
     * @return A list of maps, where each map contains feature attributes as keys (start location, end location, feature type,
     *         range type, xref) and the corresponding selected values from the combo boxes.
     *
     * @throws NullPointerException if any of the combo boxes contain null values for the selected items.
     */
    private List<Map<String, String>> getFeaturesDataFromCombobox(List<Map<String, JComboBox<String>>> comboBoxes, boolean bait) {
        List<Map<String, String>> featuresData = new ArrayList<>();

        for (Map<String, JComboBox<String>> comboBoxMap : comboBoxes) {
            Map<String, String> featureData = new HashMap<>();
            featureData.put(DataForRawFile.FEATURE_START_LOCATION.name,
                    Objects.requireNonNull(comboBoxMap.get(DataForRawFile.FEATURE_START_LOCATION.name).
                            getSelectedItem()).toString());

            featureData.put(DataForRawFile.FEATURE_END_LOCATION.name,
                    Objects.requireNonNull(comboBoxMap.get(DataForRawFile.FEATURE_END_LOCATION.name).
                            getSelectedItem()).toString());

            featureData.put(DataForRawFile.FEATURE_TYPE.name,
                    Objects.requireNonNull(comboBoxMap.get(DataForRawFile.FEATURE_TYPE.name).
                            getSelectedItem()).toString());

            featureData.put(DataForRawFile.FEATURE_RANGE_TYPE.name,
                    Objects.requireNonNull(comboBoxMap.get(DataForRawFile.FEATURE_RANGE_TYPE.name).
                            getSelectedItem()).toString());

            featureData.put(DataForRawFile.FEATURE_XREF.name, getFeatureXrefAsString(bait));

            featureData.put(DataForRawFile.FEATURE_XREF_DB.name, getFeatureXrefDbAsString(bait));

            featuresData.add(featureData);
        }
        return featuresData;
    }

    /**
     * Creates a panel for selecting feature xrefs, allowing the user to add multiple xrefs for each feature.
     * The number of xrefs can be adjusted with a spinner.
     *
     * @param bait A boolean indicating whether the xrefs are for bait (true) or prey (false).
     * @return A panel for selecting feature xrefs.
     */
    private JPanel createFeatureXrefPanel(boolean bait) {
        JPanel featureXrefPanel = new JPanel();
        featureXrefPanel.setLayout(new BoxLayout(featureXrefPanel, BoxLayout.Y_AXIS));
        featureXrefPanel.setBorder(BorderFactory.createTitledBorder("Feature Xref"));
        JSpinner numberOfXref = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        numberOfXref.setPreferredSize(new Dimension(200, 50));
        numberOfXref.setMaximumSize(new Dimension(200, 50));
        numberOfXref.setBorder(BorderFactory.createTitledBorder("Select number of xref"));
        featureXrefPanel.add(numberOfXref);

        JPanel xrefContainerPanel = new JPanel();
        xrefContainerPanel.setLayout(new BoxLayout(xrefContainerPanel, BoxLayout.Y_AXIS));
        xrefContainerPanel.add(createOneFeatureXrefPanel(bait));

        featureXrefPanel.add(xrefContainerPanel);

        numberOfXref.addChangeListener(e -> updateFeatureXrefPanel(xrefContainerPanel,
                (int) numberOfXref.getValue(), bait));

        return featureXrefPanel;
    }

    /**
     * Creates a single xref panel for selecting a feature xref and its associated database.
     *
     * @param bait A boolean indicating whether the xrefs are for bait (true) or prey (false).
     * @return A panel representing one feature xref.
     */
    private JPanel createOneFeatureXrefPanel(boolean bait) {
        JPanel xrefPanel = new JPanel();
        JComboBox<String> xrefComboBox = new JComboBox<>();
        xrefComboBox.setEditable(true);
        XmlMakerUtils.setComboBoxDimension(xrefComboBox, DataForRawFile.FEATURE_XREF.name);
        xrefPanel.add(xrefComboBox);

        JComboBox<String> xrefDbComboBox = new JComboBox<>();
        xrefDbComboBox.setEditable(true);
        XmlMakerUtils.setComboBoxDimension(xrefDbComboBox, DataForRawFile.FEATURE_XREF_DB.name);
        setFeatureDb(xrefDbComboBox);
        xrefPanel.add(xrefDbComboBox);

        if (bait) {
            baitFeatureXrefs.add(xrefComboBox);
            baitFeatureXrefDb.add(xrefDbComboBox);
        } else {
            preyFeatureXrefs.add(xrefComboBox);
            preyFeatureXrefDb.add(xrefDbComboBox);
        }
        return xrefPanel;
    }

    /**
     * Updates the xref panel to show the correct number of xrefs based on the given count.
     *
     * @param xrefPanel The panel containing the xrefs.
     * @param numberOfXref The number of xrefs to display.
     * @param bait A boolean indicating whether the xrefs are for bait (true) or prey (false).
     */
    private void updateFeatureXrefPanel(JPanel xrefPanel, int numberOfXref, boolean bait) {
        int currentCount = xrefPanel.getComponentCount();
        for (int i = currentCount; i < numberOfXref; i++) {
            xrefPanel.add(createOneFeatureXrefPanel(bait));
        }
        for (int i = currentCount - 1; i >= numberOfXref; i--) {
            xrefPanel.remove(i);
        }

        xrefPanel.revalidate();
        xrefPanel.repaint();
    }

    /**
     * Retrieves the selected feature xrefs as a string, concatenated with semicolons.
     *
     * @param bait A boolean indicating whether the xrefs are for bait (true) or prey (false).
     * @return A string representing the selected feature xrefs.
     */
    private String getFeatureXrefAsString(boolean bait) {
        StringBuilder xrefBuilder = new StringBuilder();
        if (bait) {
            for (JComboBox<String> comboBox : baitFeatureXrefs) {
                xrefBuilder.append(comboBox.getSelectedItem()).append(";");
            }
        } else {
            for (JComboBox<String> comboBox : preyFeatureXrefs) {
                xrefBuilder.append(comboBox.getSelectedItem()).append(";");
            }
        }
        return xrefBuilder.toString();
    }

    /**
     * Retrieves the selected feature xref databases as a string, concatenated with semicolons.
     *
     * @param bait A boolean indicating whether the xrefs are for bait (true) or prey (false).
     * @return A string representing the selected feature xref databases.
     */
    private String getFeatureXrefDbAsString(boolean bait) {
        StringBuilder xrefBuilder = new StringBuilder();
        if (bait) {
            for (JComboBox<String> comboBox : baitFeatureXrefDb) {
                xrefBuilder.append(comboBox.getSelectedItem()).append(";");
            }
        } else {
            for (JComboBox<String> comboBox : preyFeatureXrefDb) {
                xrefBuilder.append(comboBox.getSelectedItem()).append(";");
            }
        }
        return xrefBuilder.toString();
    }
}