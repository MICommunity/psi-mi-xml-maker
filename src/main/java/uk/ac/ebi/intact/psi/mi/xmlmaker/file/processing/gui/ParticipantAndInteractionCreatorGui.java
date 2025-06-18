package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.gui;

import lombok.Getter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.FileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.CacheUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.DataForRawFile.*;
import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils.*;
import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.GuiUtils.*;

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

    //experiment details
    private final JComboBox<String> hostOrganism = new JComboBox<>();
    private final JComboBox<String> interactionDetectionMethodCombobox = new JComboBox<>();
    private final JComboBox<String> participantDetectionMethodCombobox = new JComboBox<>();
    @Getter private final JCheckBox addVariableExperimentalCondition = new JCheckBox("Add Variable Experimental Condition");

    //interaction details
    private final JTextField interactionFigureLegend = new JTextField("Interaction Figure Legend");
    @Getter private final JCheckBox multipleInteractionParameters = new JCheckBox("Add parameters");

    //bait information
    private final JComboBox<String> baitIdDatabase = new JComboBox<>();
    private final JComboBox<String> baitBiologicalRole = new JComboBox<>();
    private final JComboBox<String> baitOrganism = new JComboBox<>();
    private final JComboBox<String> baitExpressedInOrganism = new JComboBox<>();
    private final JSpinner numberOfBaitExperimentalPrep = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
    private final List<JComboBox<String>> baitExperimentalPreparationList = new ArrayList<>();
    private final List<String> baitExperimentalPreparationNames = new ArrayList<>();
    private final JPanel baitExperimentalPreparationPanel = new JPanel(new GridLayout(2, 1));
    FeatureCreatorGui baitFeatureCreator = new FeatureCreatorGui(this);

    //prey information
    private final JComboBox<String> preyIdDatabase = new JComboBox<>();
    private final JComboBox<String> preyBiologicalRole = new JComboBox<>();
    private final JComboBox<String> preyOrganism = new JComboBox<>();
    private final JComboBox<String> preyExpressedInOrganism = new JComboBox<>();
    private final JSpinner numberOfPreyExperimentalPrep = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
    private final List<JComboBox<String>> preyExperimentalPreparationList = new ArrayList<>();
    private final List<String> preyExperimentalPreparationNames = new ArrayList<>();
    private final JPanel preyExperimentalPreparationPanel = new JPanel(new GridLayout(2, 1));
    FeatureCreatorGui preyFeatureCreator = new FeatureCreatorGui(this);

    //caches
    private final List<String> dbCache = new ArrayList<>();
    private final List<String> xrefQualifierCache = new ArrayList<>();

    //UI dimensions
    private final int HEIGHT = 300;
    private final int SCREEN_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width - 100;
    private final int THIRD_WIDTH = SCREEN_WIDTH / 3;
    private final int SIXTH_WIDTH = SCREEN_WIDTH / 6;
    private final Dimension participantPanelDimension = new Dimension(THIRD_WIDTH, HEIGHT);
    private final Dimension experimentPanelDimension = new Dimension(SIXTH_WIDTH, HEIGHT);

    private final FileReader fileReader;

    public ParticipantAndInteractionCreatorGui(FileReader fileReader) {
        this.fileReader = fileReader;
    }

    /**
     * Creates the GUI panel for selecting participant and interaction details.
     * The panel contains dropdowns for different attributes related to interaction data.
     *
     * @return The JPanel containing the GUI components.
     */
    public JPanel createParticipantAndInteractionCreatorGui() {
        JPanel participantAndInteractionCreatorPanel = new JPanel();
        participantAndInteractionCreatorPanel.setSize(new Dimension(SCREEN_WIDTH, HEIGHT));
        participantAndInteractionCreatorPanel.setPreferredSize(new Dimension(SCREEN_WIDTH, HEIGHT));
        participantAndInteractionCreatorPanel.setLayout(new BoxLayout(participantAndInteractionCreatorPanel, BoxLayout.X_AXIS));

        participantAndInteractionCreatorPanel.add(experimentInfoPanel());
        participantAndInteractionCreatorPanel.add(interactionInfoPanel());
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
        setXrefQualifiers();

        updateExperimentalPreparations((int) numberOfBaitExperimentalPrep.getValue(), baitExperimentalPreparationPanel, baitExperimentalPreparationList);
        numberOfBaitExperimentalPrep.setToolTipText("Number of bait experimental preparations");
        addSpinnerListener(numberOfBaitExperimentalPrep, baitExperimentalPreparationPanel, baitExperimentalPreparationList);

        updateExperimentalPreparations((int) numberOfPreyExperimentalPrep.getValue(), preyExperimentalPreparationPanel, preyExperimentalPreparationList);
        numberOfPreyExperimentalPrep.setToolTipText("Number of prey experimental preparations");
        addSpinnerListener(numberOfPreyExperimentalPrep, preyExperimentalPreparationPanel, preyExperimentalPreparationList);
    }

    /**
     * Populates the bait and prey biological role dropdowns with predefined roles.
     */
    public void setBiologicalRole() {
        for (String termName : CacheUtils.BIOLOGICAL_ROLES) {
            baitBiologicalRole.addItem(termName);
            preyBiologicalRole.addItem(termName);
        }
    }

    /**
     * Populates the bait and prey experimental preparation dropdowns.
     */
    public void setExperimentalPreparations() {
        for (String termName : CacheUtils.EXPERIMENTAL_PREPARATIONS) {
            preyExperimentalPreparationNames.add(termName);
            baitExperimentalPreparationNames.add(termName);
        }
    }

    /**
     * Populates the interaction detection method dropdown with predefined methods.
     */
    public void setInteractionDetectionMethod() {
        interactionDetectionMethodCombobox.setToolTipText("Interaction Detection Method");
        for (String termName : CacheUtils.INTERACTION_DETECTION_METHODS) {
            interactionDetectionMethodCombobox.addItem(termName);
        }
    }

    /**
     * Populates the participant detection method dropdown with predefined methods.
     */
    public void setParticipantDetectionMethod() {
        participantDetectionMethodCombobox.setToolTipText("Participant Identification Method");
        for (String termName : CacheUtils.PARTICIPANT_DETECTION_METHODS) {
            participantDetectionMethodCombobox.addItem(termName);
        }
    }

    /**
     * Populates the bait and prey organism dropdowns with available organisms and their taxonomic IDs.
     * These dropdowns are editable, allowing users to manually input values.
     */
    public void setOrganisms() {
        baitOrganism.setEditable(true);
        baitOrganism.setToolTipText("Bait organism ID");

        preyOrganism.setEditable(true);
        preyOrganism.setToolTipText("Prey organism ID");

        hostOrganism.setEditable(true);
        hostOrganism.setToolTipText("Host organism ID");

        preyExpressedInOrganism.setEditable(true);
        preyExpressedInOrganism.setToolTipText("Prey expressed organism ID");

        baitExpressedInOrganism.setEditable(true);
        baitExpressedInOrganism.setToolTipText("Bait expressed organism ID");

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

        participantDetails.put(BAIT_ID_DB.nameAndExperimentalRole,
                isValueNull(baitIdDatabase.getSelectedItem(), BAIT_ID_DB.name));
        participantDetails.put(PREY_ID_DB.nameAndExperimentalRole,
                isValueNull(preyIdDatabase.getSelectedItem(), PREY_ID_DB.name));
        participantDetails.put(INTERACTION_DETECTION_METHOD.nameAndExperimentalRole,
                isValueNull(interactionDetectionMethodCombobox.getSelectedItem(), INTERACTION_DETECTION_METHOD.name));
        participantDetails.put(BAIT_EXPERIMENTAL_PREPARATION.nameAndExperimentalRole,
                isValueNull(getBaitExperimentalPreparationsAsString(), BAIT_EXPERIMENTAL_PREPARATION.name));
        participantDetails.put(PREY_EXPERIMENTAL_PREPARATION.nameAndExperimentalRole,
                isValueNull(getPreyExperimentalPreparationsAsString(), PREY_EXPERIMENTAL_PREPARATION.name));
        participantDetails.put(PARTICIPANT_DETECTION_METHOD.nameAndExperimentalRole,
                isValueNull(participantDetectionMethodCombobox.getSelectedItem(), PARTICIPANT_DETECTION_METHOD.name));
        participantDetails.put(BAIT_BIOLOGICAL_ROLE.nameAndExperimentalRole,
                isValueNull(baitBiologicalRole.getSelectedItem(), BAIT_BIOLOGICAL_ROLE.name));
        participantDetails.put(PREY_BIOLOGICAL_ROLE.nameAndExperimentalRole,
                isValueNull(preyBiologicalRole.getSelectedItem(), PREY_BIOLOGICAL_ROLE.name));

        participantDetails.put(INTERACTION_FIGURE_LEGEND.nameAndExperimentalRole, interactionFigureLegend.getText());

        participantDetails.put(BAIT_ORGANISM.nameAndExperimentalRole,
                isValueNull(fetchTaxIdForOrganism(Objects.requireNonNull(baitOrganism.getSelectedItem()).toString()), BAIT_ORGANISM.name));
        participantDetails.put(PREY_ORGANISM.nameAndExperimentalRole,
                isValueNull(fetchTaxIdForOrganism(Objects.requireNonNull(preyOrganism.getSelectedItem()).toString()), PREY_ORGANISM.name));
        participantDetails.put(HOST_ORGANISM.nameAndExperimentalRole, isValueNull(fetchTaxIdForOrganism(Objects.requireNonNull(hostOrganism.getSelectedItem()).toString()), HOST_ORGANISM.name));
        participantDetails.put(PREY_EXPRESSED_IN_ORGANISM.nameAndExperimentalRole, isValueNull(fetchTaxIdForOrganism(Objects.requireNonNull(preyExpressedInOrganism.getSelectedItem()).toString()), PREY_EXPRESSED_IN_ORGANISM.name));
        participantDetails.put(BAIT_EXPRESSED_IN_ORGANISM.nameAndExperimentalRole, isValueNull(fetchTaxIdForOrganism(Objects.requireNonNull(baitExpressedInOrganism.getSelectedItem()).toString()), BAIT_EXPRESSED_IN_ORGANISM.name));

        return participantDetails;
    }

    /**
     * Adds a change listener to the number of experimental preparations spinner, which updates the
     * bait experimental preparations when the value changes.
     */
    private void addSpinnerListener(JSpinner spinner, JPanel panelToUpdate, List<JComboBox<String>> listToUpdate) {
        spinner.addChangeListener(e -> {
            int value = (int) spinner.getValue();
            SwingUtilities.invokeLater(() -> updateExperimentalPreparations(value, panelToUpdate, listToUpdate));
        });
    }

    /**
     * Updates the number of bait experimental preparations based on the given count.
     * Dynamically adds or removes combo boxes from the bait experimental panel.
     *
     * @param count The number of bait experimental preparations to display.
     */
    private void updateExperimentalPreparations(int count, JPanel experimentalPreparationsPanel, List<JComboBox<String>> listToUpdate) {
        experimentalPreparationsPanel.removeAll();  // Ensure old components are removed
        listToUpdate.clear();

        for (int i = 0; i < count; i++) {
            JComboBox<String> comboBox = new JComboBox<>();
            comboBox.setToolTipText("Experimental preparation " + i);
            comboBox.addItem("Experimental Preparation");

            for (String termName : baitExperimentalPreparationNames) {
                comboBox.addItem(termName);
            }

            listToUpdate.add(comboBox);
            experimentalPreparationsPanel.add(setComboBoxDimension(comboBox, "Experimental Preparation " + (i + 1)));
        }

        experimentalPreparationsPanel.revalidate();
        experimentalPreparationsPanel.repaint();
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
     * Retrieves the selected experimental preparations for the prey participant as a single formatted string.
     * <p>
     * The method loops through a list of combo boxes for experimental preparation, filters out null or empty
     * selections, and concatenates the valid selections with a semicolon separator.
     *
     * @return a string of selected experimental preparations for prey, separated by " ; "
     */
    public String getPreyExperimentalPreparationsAsString() {
        List<String> selectedPreparations = new ArrayList<>();

        for (JComboBox<String> comboBox : preyExperimentalPreparationList) {
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
        baitIdDatabase.addItem("UniProtKB");
        baitIdDatabase.setToolTipText("Bait ID database");

        preyIdDatabase.addItem("UniProtKB");
        preyIdDatabase.setToolTipText("Prey ID database");

        baitIdDatabase.addItem("geneid");
        preyIdDatabase.addItem("geneid");

        baitIdDatabase.addItem("gene name");
        preyIdDatabase.addItem("gene name");

        for (String termName : CacheUtils.DATABASES) {
            baitIdDatabase.addItem(termName);
            preyIdDatabase.addItem(termName);
            dbCache.add(termName);
        }
    }

    /**
     * Initializes and populates the {@code xrefQualifierCache} with controlled vocabulary terms
     * fetched from the Ontology Lookup Service (OLS) using the MI identifier for Xref qualifiers.
     * <p>
     * This method is typically used to preload qualifiers for xref (cross-reference) annotations.
     */
    private void setXrefQualifiers(){
        xrefQualifierCache.addAll(CacheUtils.XREF_QUALIFIERS);
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
        baitPanel.setPreferredSize(participantPanelDimension);

        baitPanel.setBorder(BorderFactory.createTitledBorder(" 2.3 Select baits information"));
        baitPanel.setMaximumSize(participantPanelDimension);


        baitPanel.add(setComboBoxDimension(baitIdDatabase, BAIT_ID_DB.name));
        baitPanel.add(setComboBoxDimension(baitBiologicalRole, BAIT_BIOLOGICAL_ROLE.name));
        baitBiologicalRole.setToolTipText("Bait Biological Role");
        baitPanel.add(setComboBoxDimension(baitOrganism, BAIT_ORGANISM.name));
        baitPanel.add(setComboBoxDimension(baitExpressedInOrganism, BAIT_EXPRESSED_IN_ORGANISM.name));

        numberOfBaitExperimentalPrep.setPreferredSize(new Dimension(200, 100));
        numberOfBaitExperimentalPrep.setBorder(BorderFactory.createTitledBorder("Experimental preparations"));
        numberOfBaitExperimentalPrep.add(baitExperimentalPreparationPanel);
        baitPanel.add(numberOfBaitExperimentalPrep);
        baitPanel.add(baitExperimentalPreparationPanel);

        JButton createFeatureButton = new JButton("Create feature(s)");
        createFeatureButton.addActionListener(e -> {
            JPanel featureOptionPanel = baitFeatureCreator.getFeatureMainPanel(true);
            JOptionPane.showConfirmDialog(null,
                    featureOptionPanel,
                    "Feature(s) creation",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
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
        preyPanel.setPreferredSize(participantPanelDimension);
        preyPanel.setBorder(BorderFactory.createTitledBorder(" 2.4 Select preys information"));
        preyPanel.setMaximumSize(participantPanelDimension);

        preyPanel.add(setComboBoxDimension(preyIdDatabase, PREY_ID_DB.name));
        preyPanel.add(setComboBoxDimension(preyBiologicalRole, PREY_BIOLOGICAL_ROLE.name));
        preyBiologicalRole.setToolTipText("Prey Biological Role");
        preyPanel.add(setComboBoxDimension(preyOrganism, PREY_ORGANISM.name));
        preyPanel.add(setComboBoxDimension(preyExpressedInOrganism, PREY_EXPRESSED_IN_ORGANISM.name));

        numberOfPreyExperimentalPrep.setPreferredSize(new Dimension(200, 100));
        numberOfPreyExperimentalPrep.setBorder(BorderFactory.createTitledBorder("Experimental preparations"));
        numberOfPreyExperimentalPrep.add(preyExperimentalPreparationPanel);
        preyPanel.add(numberOfPreyExperimentalPrep);
        preyPanel.add(preyExperimentalPreparationPanel);

        JButton createFeatureButton = new JButton("Create feature(s)");
        createFeatureButton.addActionListener(e -> {
            JPanel featureOptionPanel = preyFeatureCreator.getFeatureMainPanel(false);
            JOptionPane.showConfirmDialog(null,
                    featureOptionPanel,
                    "Feature(s) creation",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
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
    public JPanel experimentInfoPanel() {
        JPanel experimentInfoPanel = new JPanel();
        experimentInfoPanel.setLayout(new GridLayout(4, 1));
        experimentInfoPanel.setPreferredSize(experimentPanelDimension);
        experimentInfoPanel.setMaximumSize(experimentPanelDimension);
        experimentInfoPanel.setBorder(BorderFactory.createTitledBorder(" 2.2 Experiment details"));

        experimentInfoPanel.add(setComboBoxDimension(hostOrganism, HOST_ORGANISM.name));

        experimentInfoPanel.add(setComboBoxDimension(interactionDetectionMethodCombobox, INTERACTION_DETECTION_METHOD.name));
        experimentInfoPanel.add(setComboBoxDimension(participantDetectionMethodCombobox, PARTICIPANT_DETECTION_METHOD.name));
        experimentInfoPanel.add(addVariableExperimentalCondition);

        return experimentInfoPanel;
    }

    /**
     * Creates and returns a {@link JPanel} that contains components related to interaction details in the UI.
     * <p>
     * The panel includes:
     * <ul>
     *     <li>A checkbox or field for specifying multiple interaction parameters.</li>
     *     <li>A figure legend component with tooltip for guiding users on interaction representations.</li>
     * </ul>
     * The panel layout is a 4-row {@link GridLayout}, and its size is set using {@code experimentPanelDimension}.
     *
     * @return a configured {@link JPanel} for interaction information
     */
    public JPanel interactionInfoPanel() {
        JPanel interactionInfoPanel = new JPanel();
        interactionInfoPanel.setLayout(new GridLayout(4, 1));
        interactionInfoPanel.setPreferredSize(experimentPanelDimension);
        interactionInfoPanel.setMaximumSize(experimentPanelDimension);
        interactionInfoPanel.setBorder(BorderFactory.createTitledBorder(" 2.3 Interaction details"));

        interactionInfoPanel.add(multipleInteractionParameters);

        interactionFigureLegend.setPreferredSize(new Dimension(100, 50));
        interactionFigureLegend.setToolTipText("Interaction Figure Legend");
        interactionInfoPanel.add(interactionFigureLegend);

        return interactionInfoPanel;
    }

}