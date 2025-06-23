package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.gui;

import uk.ac.ebi.intact.psi.mi.xmlmaker.models.Feature;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.GuiUtils.setComboBoxDimension;

/**
 * A GUI component for managing cross-references associated with biological features.
 *
 * <p>This class provides functionality to:
 * <ul>
 *   <li>Create and manage multiple cross-references for features</li>
 *   <li>Configure database sources, qualifiers, and labels for cross-references</li>
 *   <li>Toggle between manual entry and file-based data fetching</li>
 *   <li>Add parameters to features through a dialog interface</li>
 * </ul>
 *
 * <p>The component maintains synchronization between the UI elements and the underlying
 * {@link Feature} model objects, ensuring consistent data representation.
 */
 public class FeatureXrefGui {
    private static final int DEFAULT_XREF_COUNT = 1;
    private static final int MAX_XREF_COUNT = 10;
    private static final String XREF_TITLE = "Cross-reference";
    private static final String DB_TOOLTIP = "Database";
    private static final String QUALIFIER_TOOLTIP = "Qualifier";
    private static final String XREF_LABEL_TOOLTIP = "Cross-reference label";

    private final ParticipantAndInteractionCreatorGui participantAndInteractionCreatorGui;
    private final FeatureCreatorGui featureCreatorGui;
    private final List<String> fileColumns;
    private final boolean bait;

    /**
     * Constructs a new FeatureXrefGui with references to parent components.
     *
     * @param participantAndInteractionCreatorGui the parent interaction creator GUI
     * @param featureCreatorGui the parent feature creator GUI
     */
    public FeatureXrefGui(ParticipantAndInteractionCreatorGui participantAndInteractionCreatorGui,
                          FeatureCreatorGui featureCreatorGui) {
        this.participantAndInteractionCreatorGui = participantAndInteractionCreatorGui;
        this.featureCreatorGui = featureCreatorGui;
        this.fileColumns = featureCreatorGui.getFileColumns();
        this.bait = featureCreatorGui.isBait();
    }

    /**
     * Creates the main container panel for feature cross-references.
     *
     * @param featureIndex the index of the feature in the parent's list
     * @return the configured container panel
     */
    public JPanel createFeatureXrefContainerPanel(int featureIndex) {
        JPanel containerPanel = new JPanel();
        containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));

        containerPanel.setBorder(BorderFactory.createTitledBorder("Cross-references"));

        JPanel xrefPanelListContainer = new JPanel();
        xrefPanelListContainer.setLayout(new BoxLayout(xrefPanelListContainer, BoxLayout.Y_AXIS));

        JSpinner xrefCountSpinner = createXrefCountSpinner(xrefPanelListContainer, featureIndex);

        containerPanel.add(xrefCountSpinner, BorderLayout.NORTH);
        containerPanel.add(new JScrollPane(xrefPanelListContainer), BorderLayout.CENTER);

        updateFeatureXrefPanel(xrefPanelListContainer, DEFAULT_XREF_COUNT, featureIndex);

        return containerPanel;
    }

    /**
     * Creates the spinner control for selecting number of cross-references.
     *
     * @param xrefPanelListContainer the panel to update when spinner changes
     * @param featureIndex the index of the current feature
     * @return the configured spinner control
     */
    private JSpinner createXrefCountSpinner(JPanel xrefPanelListContainer, int featureIndex) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(DEFAULT_XREF_COUNT, 1, MAX_XREF_COUNT, 1));
        spinner.setPreferredSize(new Dimension(20, 50));
        spinner.setBorder(BorderFactory.createTitledBorder("Select number of cross-references"));
        spinner.addChangeListener(e ->
                updateFeatureXrefPanel(xrefPanelListContainer, (int) spinner.getValue(), featureIndex)
        );
        return spinner;
    }

    /**
     * Updates the cross-reference panels based on the requested count.
     *
     * @param xrefPanelListContainer the container panel for cross-references
     * @param numberOfXref the desired number of cross-references
     * @param featureIndex the index of the current feature
     */
    private void updateFeatureXrefPanel(JPanel xrefPanelListContainer, int numberOfXref, int featureIndex) {
        Feature currentFeature = getCurrentFeature(featureIndex);

        initializeFeatureXrefLists(currentFeature);
        adjustXrefListsSize(currentFeature, numberOfXref);
        rebuildXrefPanels(xrefPanelListContainer, numberOfXref, featureIndex);
    }

    /**
     * Initializes the cross-reference lists in a feature if they are null.
     *
     * @param feature the feature to initialize
     */
    private void initializeFeatureXrefLists(Feature feature) {
        if (feature.getXref() == null) feature.setXref(new ArrayList<>());
        if (feature.getXrefDb() == null) feature.setXrefDb(new ArrayList<>());
        if (feature.getXrefQualifier() == null) feature.setXrefQualifier(new ArrayList<>());
    }

    /**
     * Adjusts the size of cross-reference lists to match the target size.
     *
     * @param feature the feature to modify
     * @param targetSize the desired size of the lists
     */
    private void adjustXrefListsSize(Feature feature, int targetSize) {
        while (feature.getXref().size() < targetSize) {
            feature.addXref("");
            feature.addXrefDb("");
            feature.addXrefQualifier("");
        }

        while (feature.getXref().size() > targetSize) {
            int last = feature.getXref().size() - 1;
            feature.removeXref(last);
            feature.removeXrefDb(last);
            feature.removeXrefQualifier(last);
        }
    }

    /**
     * Rebuilds all cross-reference panels in the container.
     *
     * @param container the parent container panel
     * @param count the number of panels to create
     * @param featureIndex the index of the current feature
     */
    private void rebuildXrefPanels(JPanel container, int count, int featureIndex) {
        container.removeAll();
        for (int i = 0; i < count; i++) {
            container.add(createFeatureXrefPanel(featureIndex, i));
        }
        container.revalidate();
        container.repaint();
    }

    /**
     * Creates an individual cross-reference configuration panel.
     *
     * @param featureIndex the index of the parent feature
     * @param xrefIndex the index of this cross-reference
     * @return the configured panel
     */
    private JPanel createFeatureXrefPanel(int featureIndex, int xrefIndex) {
        JPanel xrefPanel = new JPanel(new BorderLayout(5, 5));
        xrefPanel.setBorder(BorderFactory.createTitledBorder(XREF_TITLE));

        Feature currentFeature = getCurrentFeature(featureIndex);
        ParametersGui parametersGui = new ParametersGui(participantAndInteractionCreatorGui.getFileReader());

        JComboBox<String> xrefTextField = getXrefTextField(currentFeature, xrefIndex);
        JComboBox<String> xrefDbComboBox = getXrefDbComboBox(currentFeature, xrefIndex);
        JComboBox<String> xrefQualifierComboBox = getXrefDQualifierCombobox(currentFeature, xrefIndex);

        xrefPanel.add(xrefTextField, BorderLayout.NORTH);
        xrefPanel.add(xrefDbComboBox, BorderLayout.LINE_START);
        xrefPanel.add(xrefQualifierComboBox, BorderLayout.LINE_END);
        xrefPanel.add(createParametersCheckbox(currentFeature, parametersGui), BorderLayout.SOUTH);

        return xrefPanel;
    }

    /**
     * Creates a database selection combo box for a cross-reference.
     *
     * @param currentFeature the feature being configured
     * @param xrefIndex the index of the cross-reference
     * @return the configured combo box
     */
    private JComboBox<String> getXrefDbComboBox(Feature currentFeature, int xrefIndex) {
        JComboBox<String> comboBox = createXrefComboBox(
                DB_TOOLTIP,
                participantAndInteractionCreatorGui.getDbCache(),
                currentFeature.getXrefDb().get(xrefIndex),
                selected -> currentFeature.getXrefDb().set(xrefIndex, selected)
        );
        configureFetchFromFile(comboBox, DB_TOOLTIP, participantAndInteractionCreatorGui.getDbCache(), currentFeature);
        return comboBox;
    }

    /**
     * Creates a qualifier selection combo box for a cross-reference.
     *
     * @param currentFeature the feature being configured
     * @param xrefIndex the index of the cross-reference
     * @return the configured combo box
     */
    private JComboBox<String> getXrefDQualifierCombobox(Feature currentFeature, int xrefIndex) {
        JComboBox<String> comboBox = createXrefComboBox(
                QUALIFIER_TOOLTIP,
                participantAndInteractionCreatorGui.getXrefQualifierCache(),
                currentFeature.getXrefQualifier().get(xrefIndex),
                selected -> currentFeature.getXrefQualifier().set(xrefIndex, selected)
        );
        configureFetchFromFile(comboBox, QUALIFIER_TOOLTIP,
                participantAndInteractionCreatorGui.getXrefQualifierCache(), currentFeature);
        return comboBox;
    }

    /**
     * Creates a label entry field for a cross-reference.
     *
     * @param currentFeature the feature being configured
     * @param xrefIndex the index of the cross-reference
     * @return the configured combo box
     */
    private JComboBox<String> getXrefTextField(Feature currentFeature, int xrefIndex) {
        JComboBox<String> comboBox = createXrefComboBox(
                XREF_LABEL_TOOLTIP,
                new ArrayList<>(),
                currentFeature.getXref().get(xrefIndex),
                selected -> currentFeature.getXref().set(xrefIndex, selected)
        );

        currentFeature.addFetchFromFileListener(e -> {
            comboBox.removeAllItems();
            comboBox.addItem(XREF_LABEL_TOOLTIP);
            if (currentFeature.isFetchFromFile()) {
                fileColumns.forEach(comboBox::addItem);
            }
            comboBox.setSelectedIndex(0);
        });

        return comboBox;
    }

    /**
     * Creates a standardized combo box for cross-reference fields.
     *
     * @param tooltip the tooltip and default item text
     * @param defaultItems the default items to populate
     * @param currentValue the current value to display
     * @param updateAction the action to perform when selection changes
     * @return the configured combo box
     */
    private JComboBox<String> createXrefComboBox(String tooltip, List<String> defaultItems,
                                                 String currentValue, Consumer<String> updateAction) {
        JComboBox<String> comboBox = new JComboBox<>();
        comboBox.addItem(tooltip);
        defaultItems.forEach(comboBox::addItem);

        setComboBoxDimension(comboBox, tooltip);
        comboBox.setToolTipText(tooltip);
        comboBox.setEditable(true);
        comboBox.setSelectedItem(currentValue.isEmpty() ? tooltip : currentValue);

        comboBox.addActionListener(e -> {
            String selected = (String) comboBox.getSelectedItem();
            updateAction.accept(tooltip.equals(selected) ? "" : selected);
        });

        return comboBox;
    }

    /**
     * Configures a combo box to respond to fetch-from-file mode changes.
     *
     * @param comboBox the combo box to configure
     * @param tooltip the tooltip text to use as default item
     * @param defaultItems the default items to show in manual mode
     * @param feature the feature being configured
     */
    private void configureFetchFromFile(JComboBox<String> comboBox, String tooltip,
                                        List<String> defaultItems, Feature feature) {
        feature.addFetchFromFileListener(e -> {
            comboBox.removeAllItems();
            comboBox.addItem(tooltip);

            List<String> items = feature.isFetchFromFile() ? fileColumns : defaultItems;
            items.forEach(comboBox::addItem);

            comboBox.setSelectedIndex(0);
        });
    }

    /**
     * Creates a checkbox for adding parameters to a feature.
     *
     * @param currentFeature the feature being configured
     * @param parametersGui the parameter GUI component
     * @return the configured checkbox
     */
    private JCheckBox createParametersCheckbox(Feature currentFeature, ParametersGui parametersGui) {
        JCheckBox checkbox = new JCheckBox("Add parameters to feature");
        checkbox.addActionListener(e -> {
            currentFeature.setInduceInteractionParameters(checkbox.isSelected());
            JPanel parametersPanel = parametersGui.parametersContainer();
            JOptionPane.showConfirmDialog(
                    null,
                    parametersPanel,
                    "Add parameters",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            currentFeature.setParameters(parametersGui.getParameters());
            currentFeature.setParametersAsString();
        });
        return checkbox;
    }

    /**
     * Gets the current feature being edited based on bait/prey status.
     *
     * @param featureIndex the index of the feature
     * @return the requested feature
     */
    private Feature getCurrentFeature(int featureIndex) {
        return bait ?
                featureCreatorGui.getBaitFeatures().get(featureIndex) :
                featureCreatorGui.getPreyFeatures().get(featureIndex);
    }
}