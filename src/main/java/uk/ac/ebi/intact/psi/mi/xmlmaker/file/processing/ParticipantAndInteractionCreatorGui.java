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

    private final List<JComboBox<String>> baitExperimentalPreparationList = new ArrayList<>();
    private final List<String> baitExperimentalPreparationNames = new ArrayList<>();
    private final JComboBox<String> baitBiologicalRole = new JComboBox<>();
    private final JComboBox<String> baitOrganism = new JComboBox<>();
    private final JComboBox<String> baitIdDatabase = new JComboBox<>();

    private final JComboBox<String> preyExperimentalPreparation = new JComboBox<>();
    private final JComboBox<String> preyBiologicalRole = new JComboBox<>();
    private final JComboBox<String> preyOrganism = new JComboBox<>();
    private final JComboBox<String> preyIdDatabase = new JComboBox<>();

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

//    private final Map<String, Map<String, JComboBox<String>>> featureAndXrefs = new HashMap<>();

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
    public Map<String, String> getParticipantDetails() {
        Map<String, String> participantDetails = new HashMap<>();

        participantDetails.put(DataForRawFile.BAIT_ID_DB.name,
                checkValue(baitIdDatabase.getSelectedItem(), DataForRawFile.BAIT_ID_DB.name));
        participantDetails.put(DataForRawFile.PREY_ID_DB.name,
                checkValue(preyIdDatabase.getSelectedItem(), DataForRawFile.PREY_ID_DB.name));
        participantDetails.put(DataForRawFile.INTERACTION_DETECTION_METHOD.name,
                checkValue(interactionDetectionMethodCombobox.getSelectedItem(), DataForRawFile.INTERACTION_DETECTION_METHOD.name));
        participantDetails.put(DataForRawFile.BAIT_EXPERIMENTAL_PREPARATION.name,
                checkValue(getBaitExperimentalPreparationsAsString(), DataForRawFile.BAIT_EXPERIMENTAL_PREPARATION.name));
        participantDetails.put(DataForRawFile.PREY_EXPERIMENTAL_PREPARATION.name,
                checkValue(preyExperimentalPreparation.getSelectedItem(), DataForRawFile.PREY_EXPERIMENTAL_PREPARATION.name));
        participantDetails.put(DataForRawFile.PARTICIPANT_DETECTION_METHOD.name,
                checkValue(participantDetectionMethodCombobox.getSelectedItem(), DataForRawFile.PARTICIPANT_DETECTION_METHOD.name));
        participantDetails.put(DataForRawFile.BAIT_BIOLOGICAL_ROLE.name,
                checkValue(baitBiologicalRole.getSelectedItem(), DataForRawFile.BAIT_BIOLOGICAL_ROLE.name));
        participantDetails.put(DataForRawFile.PREY_BIOLOGICAL_ROLE.name,
                checkValue(preyBiologicalRole.getSelectedItem(), DataForRawFile.PREY_BIOLOGICAL_ROLE.name));

        participantDetails.put(DataForRawFile.BAIT_ORGANISM.name,
                checkValue(XmlMakerUtils.fetchTaxIdForOrganism(Objects.requireNonNull(baitOrganism.getSelectedItem()).toString()), DataForRawFile.BAIT_ORGANISM.name));
        participantDetails.put(DataForRawFile.PREY_ORGANISM.name,
                checkValue(XmlMakerUtils.fetchTaxIdForOrganism(Objects.requireNonNull(preyOrganism.getSelectedItem()).toString()), DataForRawFile.PREY_ORGANISM.name));

        return participantDetails;
    }

    private String checkValue(Object value, String keyName) {
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

    private void setFeatureType(JComboBox<String> featureTypeComboBox) {
        if (featureTypeCache.isEmpty()) {
            featureTypeCache.addAll(XmlMakerUtils.getTermsFromOls(DataAndMiID.FEATUTRE_TYPE.miId));
        }
        for (String termName : featureTypeCache) {
            featureTypeComboBox.addItem(termName);
        }
    }

    private void setFeatureRangeType(JComboBox<String> featureRangeTypeComboBox) {
        if (featureRangeTypeCache.isEmpty()) {
            featureRangeTypeCache.addAll(XmlMakerUtils.getTermsFromOls(DataAndMiID.FEATURE_RANGE_TYPE.miId));
        }

        for (String termName : featureRangeTypeCache) {
            featureRangeTypeComboBox.addItem(termName);
        }
    }

    private void addSpinnerListener() {
        numberOfExperimentalPrep.addChangeListener(e -> {
            int value = (int) numberOfExperimentalPrep.getValue();
            SwingUtilities.invokeLater(() -> updateBaitExperimentalPreparations(value));
        });
    }

    private void updateBaitExperimentalPreparations(int count) {
//        baitExperimentalPanel.removeAll(); //todo: check for decreasing number
        int numberExperiments = baitExperimentalPanel.getComponents().length;
//        baitExperimentalPreparationList.clear();

        for (int i = numberExperiments; i < count; i++) {
            JComboBox<String> comboBox = new JComboBox<>();
            comboBox.addItem("Experimental Preparation");

            for (String termName : baitExperimentalPreparationNames) {
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
            String selectedValue = checkValue(comboBox.getSelectedItem(), DataTypeAndColumn.EXPERIMENTAL_PREPARATION.name);
            if (selectedValue != null && !selectedValue.isEmpty()) {
                selectedPreparations.add(selectedValue);
            }
        }

        return String.join(" ; ", selectedPreparations);
    }

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

    private void setFeatureDb(JComboBox<String> featureDbComboBox) {
        featureDbComboBox.addItem("uniprotKB");
        featureDbComboBox.addItem("geneid");

        for (String termName : dbCache) {
            featureDbComboBox.addItem(termName);
        }
    }

    //PANELS

    public JPanel createBaitPanel() {
        JPanel baitPanel = new JPanel();
        baitPanel.setLayout(new GridLayout(6, 1));
        baitPanel.setPreferredSize(panelDimension);

        baitPanel.setBorder(BorderFactory.createTitledBorder(" 2.3 Select baits information"));
        baitPanel.setMaximumSize(panelDimension);


        baitPanel.add(XmlMakerUtils.setComboBoxDimension(baitIdDatabase, DataForRawFile.BAIT_ID_DB.name));
        baitPanel.add(XmlMakerUtils.setComboBoxDimension(baitBiologicalRole, DataForRawFile.BAIT_BIOLOGICAL_ROLE.name));
        baitPanel.add(XmlMakerUtils.setComboBoxDimension(baitOrganism, DataForRawFile.BAIT_ORGANISM.name));

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

    public JPanel createPreyPanel() {
        JPanel preyPanel = new JPanel();
        preyPanel.setLayout(new GridLayout(5, 1));
        preyPanel.setPreferredSize(panelDimension);
        preyPanel.setBorder(BorderFactory.createTitledBorder(" 2.4 Select preys information"));
        preyPanel.setMaximumSize(panelDimension);

        preyPanel.add(XmlMakerUtils.setComboBoxDimension(preyIdDatabase, DataForRawFile.PREY_ID_DB.name));
        preyPanel.add(XmlMakerUtils.setComboBoxDimension(preyExperimentalPreparation, DataForRawFile.PREY_EXPERIMENTAL_PREPARATION.name));
        preyPanel.add(XmlMakerUtils.setComboBoxDimension(preyBiologicalRole, DataForRawFile.PREY_BIOLOGICAL_ROLE.name));
        preyPanel.add(XmlMakerUtils.setComboBoxDimension(preyOrganism, DataForRawFile.PREY_ORGANISM.name));
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

    public JPanel createGeneralInformationPanel() {
        JPanel generalInformationPanel = new JPanel();
        generalInformationPanel.setLayout(new GridLayout(3, 1));
        generalInformationPanel.setPreferredSize(new Dimension(200, HEIGHT));
        generalInformationPanel.setBorder(BorderFactory.createTitledBorder(" 2.2 Select general information"));

        generalInformationPanel.add(XmlMakerUtils.setComboBoxDimension(participantDetectionMethodCombobox, DataForRawFile.PARTICIPANT_DETECTION_METHOD.name));
        generalInformationPanel.add(XmlMakerUtils.setComboBoxDimension(interactionDetectionMethodCombobox, DataForRawFile.INTERACTION_DETECTION_METHOD.name));

        return generalInformationPanel;
    }

    // MULTIPLE FEATURES CREATION

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
                updateFeaturePanels(featureContainerPanel, value, bait);
            });
        });

        updateFeaturePanels(featureContainerPanel, (int) numberOfFeature.getValue(), bait);

        mainPanel.add(numberOfFeature, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(featureContainerPanel), BorderLayout.CENTER);

        return mainPanel;
    }

    private void updateFeaturePanels(JPanel featureContainerPanel, int count, boolean bait) {
//        featureContainerPanel.removeAll();
        //todo: check for decreasing number
        int numberOfFeatures = featureContainerPanel.getComponents().length;
        for (int i = numberOfFeatures; i < count; i++) {
            featureContainerPanel.add(createFeaturePanel(i, bait));
        }

        featureContainerPanel.revalidate();
        featureContainerPanel.repaint();
    }

    private JPanel createFeaturePanel(int featureNumber, boolean bait) {
        Map<String, JComboBox<String>> comboBoxMap = new HashMap<>();

        JPanel featurePanel = new JPanel(new GridLayout(2, 1));
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
            featurePanel.add(XmlMakerUtils.setComboBoxDimension(comboBox, attribute.name));
        }

        featurePanel.add(featureXrefPanel(bait));

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

    public List<Map<String, String>> getFeaturesData(boolean bait){
        List<Map<String, String>> featuresData;
        if (bait) {
            featuresData = getFeaturesDataFromCombobox(baitFeaturesComboBoxes, true);
        } else {
            featuresData = getFeaturesDataFromCombobox(preyFeaturesComboBoxes, false);
        }

        return featuresData;
    }

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

            featureData.put(DataForRawFile.FEATURE_XREF.name, featureXrefAsString(bait));

            featureData.put(DataForRawFile.FEATURE_XREF_DB.name, featureXrefDbAsString(bait));

            featuresData.add(featureData);
        }
        return featuresData;
    }

    private JPanel featureXrefPanel(boolean bait) {
        JPanel featureXrefPanel = new JPanel();
        featureXrefPanel.setBorder(BorderFactory.createTitledBorder("Feature Xref"));
        JSpinner numberOfXref = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        numberOfXref.setBorder(BorderFactory.createTitledBorder("Select number of xref"));
        featureXrefPanel.add(numberOfXref);

        JPanel xrefContainerPanel = new JPanel();
        xrefContainerPanel.setLayout(new BoxLayout(xrefContainerPanel, BoxLayout.Y_AXIS));
        xrefContainerPanel.add(oneFeatureXrefPanel(bait));

        featureXrefPanel.add(xrefContainerPanel);

        numberOfXref.addChangeListener(e -> updateXrefPanel(xrefContainerPanel,
                (int) numberOfXref.getValue(), bait));

        return featureXrefPanel;
    }

    private JPanel oneFeatureXrefPanel(boolean bait) {
        JPanel xrefPanel = new JPanel();

        JComboBox<String> xrefComboBox = new JComboBox<>();
        xrefComboBox.setEditable(true);
        xrefComboBox.addItem(DataForRawFile.FEATURE_XREF.name);
        xrefPanel.add(xrefComboBox);

        JComboBox<String> xrefDbComboBox = new JComboBox<>();
        xrefDbComboBox.setEditable(true);
        xrefDbComboBox.addItem(DataForRawFile.FEATURE_XREF_DB.name);
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

    private void updateXrefPanel(JPanel xrefPanel, int numberOfXref, boolean bait) {
//        xrefPanel.removeAll(); //TODO: CHECK HOW TO NOT REMOVE EVERYTHING
        //todo: check for decreasing number
        int length = xrefPanel.getComponents().length;
        for (int i = length; i < numberOfXref; i++) {
            xrefPanel.add(oneFeatureXrefPanel(bait));
        }
        xrefPanel.revalidate();
        xrefPanel.repaint();
    }

    private String featureXrefAsString(boolean bait) {
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

    private String featureXrefDbAsString(boolean bait) {
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