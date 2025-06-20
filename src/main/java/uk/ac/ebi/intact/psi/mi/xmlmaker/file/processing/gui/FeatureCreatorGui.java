package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.gui;

import lombok.Getter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.FileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.Feature;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.CacheUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.GuiUtils.*;

/**
 * A GUI component for creating and managing biological feature definitions.
 * This class provides a user interface for defining features (such as protein domains or modifications)
 * that can be associated with bait or prey molecules in protein interaction experiments.
 *
 * <p>The GUI allows users to:
 * <ul>
 *   <li>Define multiple features with various attributes</li>
 *   <li>Specify feature locations, types, and roles</li>
 *   <li>Toggle between manual entry and file-based data fetching</li>
 *   <li>Manage cross-references for features</li>
 * </ul>
 */
public class FeatureCreatorGui {
    private static final int MAX_FEATURES = 10;
    private static final double SCREEN_SIZE_RATIO = 0.9;

    final JPanel featureMainPanel = new JPanel();

    private final List<String> locationOptions = List.of("c-term", "n-term", "undetermined");
    private final List<String> typeOptions = new ArrayList<>(CacheUtils.FEATURE_TYPES);
    private final List<String> rangeTypeOptions = new ArrayList<>(CacheUtils.FEATURE_RANGE_TYPES);
    private final List<String> roleOptions = new ArrayList<>(CacheUtils.FEATURE_ROLES);

    @Getter
    private final List<Feature> baitFeatures = new ArrayList<>();
    @Getter
    private final List<Feature> preyFeatures = new ArrayList<>();

    @Getter
    private boolean bait;

    private final ParticipantAndInteractionCreatorGui participantAndInteractionCreatorGui;

    @Getter
    private List<String> fileColumns = new ArrayList<>();

    /**
     * Constructs a new FeatureCreatorGui with reference to its parent component.
     *
     * @param participantAndInteractionCreatorGui the parent GUI component
     */
    public FeatureCreatorGui(ParticipantAndInteractionCreatorGui participantAndInteractionCreatorGui) {
        this.participantAndInteractionCreatorGui = participantAndInteractionCreatorGui;
    }

    /**
     * Returns the main panel configured for either bait or prey features.
     *
     * @param isBait true if configuring bait features, false for prey features
     * @return the fully configured main panel
     */
    public JPanel getFeatureMainPanel(boolean isBait) {
        this.bait = isBait;
        FileReader fileReader = participantAndInteractionCreatorGui.getFileReader();
        this.fileColumns = fileReader.getColumns(fileReader.getSheetSelectedUpdate());
        createFeaturesContainerPanel();
        return featureMainPanel;
    }

    /**
     * Creates the container panel that holds all feature panels.
     */
    private void createFeaturesContainerPanel() {
        JPanel featureContainerPanel = new JPanel();
        featureContainerPanel.setLayout(new BoxLayout(featureContainerPanel, BoxLayout.Y_AXIS));
        featureMainPanel.setLayout(new BorderLayout(5, 5));

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenSize.width = (int) (screenSize.width * SCREEN_SIZE_RATIO);
        screenSize.height = (int) (screenSize.height * SCREEN_SIZE_RATIO);

        featureMainPanel.setPreferredSize(screenSize);
        featureMainPanel.add(createFeatureCountSpinner(featureContainerPanel), BorderLayout.NORTH);
        featureMainPanel.add(new JScrollPane(featureContainerPanel), BorderLayout.CENTER);
    }

    /**
     * Creates and configures the spinner control for selecting number of features.
     *
     * @param panelToUpdate the panel that will be updated when spinner value changes
     * @return the configured JSpinner instance
     */
    private JSpinner createFeatureCountSpinner(JPanel panelToUpdate) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 1, MAX_FEATURES, 1));
        spinner.setBorder(BorderFactory.createTitledBorder("Select number of features"));
        spinner.addChangeListener(e -> updateFeaturePanel(panelToUpdate, (int) spinner.getValue()));
        updateFeaturePanel(panelToUpdate, (int) spinner.getValue());
        return spinner;
    }

    /**
     * Updates the feature panels based on the requested count.
     *
     * @param container the container panel holding feature panels
     * @param count the desired number of feature panels
     */
    private void updateFeaturePanel(JPanel container, int count) {
        int currentCount = container.getComponentCount();
        List<Feature> features = bait ? baitFeatures : preyFeatures;

        for (int i = currentCount; i < count; i++) {
            container.add(createFeaturePanel());
        }

        for (int i = currentCount - 1; i >= count; i--) {
            container.remove(i);
            features.remove(i);
        }

        container.revalidate();
        container.repaint();
    }

    /**
     * Creates an individual feature definition panel.
     *
     * @return the configured feature panel
     */
    private JPanel createFeaturePanel() {
        Feature feature = createNewFeature();
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4,0));
        panel.setBorder(BorderFactory.createTitledBorder("Feature"));

        AtomicBoolean fetchFromFile = new AtomicBoolean(false);
        panel.add(createFetchFromFileToggle(fetchFromFile));

        Map<String, JComboBox<String>> comboBoxes = createComboBoxes(feature);
        addComboBoxesToPanel(panel, comboBoxes);
        setupFetchFromFileListener(fetchFromFile, comboBoxes, feature);

        panel.add(new FeatureXrefGui(participantAndInteractionCreatorGui, this)
                .createFeatureXrefContainerPanel(feature.getNumber()));

        return panel;
    }

    /**
     * Creates a new feature instance and adds it to the appropriate list.
     *
     * @return the newly created Feature instance
     */
    private Feature createNewFeature() {
        Feature feature = new Feature();
        List<Feature> targetList = bait ? baitFeatures : preyFeatures;
        feature.setNumber(targetList.size());
        targetList.add(feature);
        return feature;
    }

    /**
     * Creates the "Fetch from file" toggle checkbox.
     *
     * @param fetchFlag the atomic boolean to bind to the checkbox state
     * @return the configured JCheckBox
     */
    private JCheckBox createFetchFromFileToggle(AtomicBoolean fetchFlag) {
        JCheckBox checkBox = new JCheckBox("Fetch from file");
        checkBox.setSelected(fetchFlag.get());
        return checkBox;
    }

    /**
     * Creates all combo boxes needed for feature definition.
     *
     * @param feature the feature instance to bind to the combo boxes
     * @return a map of combo boxes keyed by their purpose
     */
    private Map<String, JComboBox<String>> createComboBoxes(Feature feature) {
        Map<String, JComboBox<String>> comboBoxes = new LinkedHashMap<>();

        comboBoxes.put("shortLabel", createComboBox(typeOptions, "* Feature short label"));
        comboBoxes.put("type", createComboBox(typeOptions, "* Feature type"));
        comboBoxes.put("startLocation", createComboBox(locationOptions, "Start location"));
        comboBoxes.put("endLocation", createComboBox(locationOptions, "End location"));
        comboBoxes.put("rangeType", createComboBox(rangeTypeOptions, "Range type"));
        comboBoxes.put("originalSequence", createEditableComboBox(new ArrayList<>(), "Original sequence"));
        comboBoxes.put("newSequence", createEditableComboBox(new ArrayList<>(), "New sequence"));
        comboBoxes.put("role", createComboBox(roleOptions, "Feature role"));

        ActionListener updateListener = e -> updateFeatureProperties(feature, comboBoxes);
        comboBoxes.values().forEach(cb -> cb.addActionListener(updateListener));

        return comboBoxes;
    }

    /**
     * Adds all combo boxes to the feature panel in the correct order.
     *
     * @param panel the target panel to receive combo boxes
     * @param comboBoxes the map of combo boxes to add
     */
    private void addComboBoxesToPanel(JPanel panel, Map<String, JComboBox<String>> comboBoxes) {
        panel.add(comboBoxes.get("shortLabel"));
        panel.add(comboBoxes.get("type"));
        panel.add(comboBoxes.get("startLocation"));
        panel.add(comboBoxes.get("endLocation"));
        panel.add(comboBoxes.get("rangeType"));
        panel.add(comboBoxes.get("originalSequence"));
        panel.add(comboBoxes.get("newSequence"));
        panel.add(comboBoxes.get("role"));
    }

    /**
     * Sets up the action listener for the "Fetch from file" toggle.
     *
     * @param fetchFlag the atomic boolean tracking the toggle state
     * @param comboBoxes the map of combo boxes to update
     * @param feature the feature instance being configured
     */
    private void setupFetchFromFileListener(AtomicBoolean fetchFlag,
                                            Map<String, JComboBox<String>> comboBoxes,
                                            Feature feature) {
        JCheckBox toggle = (JCheckBox) (comboBoxes.get("shortLabel").getParent()).getComponent(0);

        toggle.addActionListener(e -> {
            boolean fetch = toggle.isSelected();
            fetchFlag.set(fetch);
            feature.setFetchFromFile(fetch);

            comboBoxes.forEach((key, cb) -> {
                String tooltip = cb.getToolTipText();
                List<String> options;

                if (fetch) {
                    options = new ArrayList<>(fileColumns);
                } else {
                    options = new ArrayList<>(getDefaultOptions(key));
                }

                options.add(0, tooltip);

                DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(options.toArray(new String[0]));
                cb.setModel(model);

                cb.setSelectedIndex(0);
            });
        });
    }

    /**
     * Gets the default options for a given combo box type.
     *
     * @param key the identifier for the combo box type
     * @return the list of default options for the specified combo box
     */
    private List<String> getDefaultOptions(String key) {
        switch (key) {
            case "startLocation":
            case "endLocation":
                return locationOptions;
            case "rangeType":
                return rangeTypeOptions;
            case "type":
                return typeOptions;
            case "role":
                return roleOptions;
            default:
                return new ArrayList<>();
        }
    }

    /**
     * Updates feature properties based on current combo box selections.
     *
     * @param feature the feature instance to update
     * @param comboBoxes the map of combo boxes containing current values
     */
    private void updateFeatureProperties(Feature feature, Map<String, JComboBox<String>> comboBoxes) {
        feature.setStartLocation(getValidComboBoxValue(comboBoxes.get("startLocation")));
        feature.setEndLocation(getValidComboBoxValue(comboBoxes.get("endLocation")));
        feature.setRangeType(getValidComboBoxValue(comboBoxes.get("rangeType")));
        feature.setType(getValidComboBoxValue(comboBoxes.get("type")));
        feature.setOriginalSequence(getValidComboBoxValue(comboBoxes.get("originalSequence")));
        feature.setNewSequence(getValidComboBoxValue(comboBoxes.get("newSequence")));
        feature.setShortName(getValidComboBoxValue(comboBoxes.get("shortLabel")));
        feature.setRole(getValidComboBoxValue(comboBoxes.get("role")));
    }

    /**
     * Extracts a valid value from a combo box, considering both editable and non-editable states.
     *
     * @param comboBox the combo box to extract value from
     * @return the current valid value as a String
     */
    private String getValidComboBoxValue(JComboBox<String> comboBox) {
        Object value = comboBox.isEditable() ? comboBox.getEditor().getItem() : comboBox.getSelectedItem();
        String tooltip = comboBox.getToolTipText();
        return (value != null && !value.toString().equals(tooltip)) ? value.toString() : "";
    }

    /**
     * Creates a combo box with the specified options and tooltip, where the tooltip text
     * is also set as the first selectable item in the combo box.
     *
     * @param options the list of options to populate (tooltip will be prepended)
     * @param tooltip the tooltip text that will also appear as first item
     * @return the configured JComboBox
     */
    private JComboBox<String> createComboBox(List<String> options, String tooltip) {
        JComboBox<String> comboBox = new JComboBox<>();

        List<String> combinedOptions = new ArrayList<>();
        combinedOptions.add(tooltip);
        combinedOptions.addAll(options);

        comboBox.setModel(new DefaultComboBoxModel<>(combinedOptions.toArray(new String[0])));

        setComboBoxDimension(comboBox, tooltip);
        comboBox.setEditable(true);
        comboBox.setToolTipText(tooltip);

        comboBox.setSelectedIndex(0);

        return comboBox;
    }

    /**
     * Creates an editable combo box with the specified options and tooltip,
     * where the tooltip text is the first selectable item.
     *
     * @param options the list of options to populate (tooltip will be prepended)
     * @param tooltip the tooltip text that will also appear as first item
     * @return the configured editable JComboBox
     */
    private JComboBox<String> createEditableComboBox(List<String> options, String tooltip) {
        JComboBox<String> comboBox = createComboBox(options, tooltip);
        comboBox.setEditable(true);
        return comboBox;
    }
}