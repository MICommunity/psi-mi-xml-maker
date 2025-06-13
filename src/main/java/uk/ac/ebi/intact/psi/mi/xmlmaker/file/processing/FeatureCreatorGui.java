package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import lombok.Getter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.DataAndMiID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils.*;

public class FeatureCreatorGui {
    final JPanel featureMainPanel = new JPanel();

    private final List<String> locationOptions = new ArrayList<>();
    private final List<String> typeOptions = new ArrayList<>();
    private final List<String> rangeTypeOptions = new ArrayList<>();
    private final List<String> roleOptions = new ArrayList<>();

    @Getter
    private final List<Feature> baitFeatures = new ArrayList<>();
    @Getter
    private final List<Feature> preyFeatures = new ArrayList<>();

    private boolean bait;

    private final ParticipantAndInteractionCreatorGui participantAndInteractionCreatorGui;
    private List<String> fileColumns = new ArrayList<>();

    public FeatureCreatorGui(ParticipantAndInteractionCreatorGui participantAndInteractionCreatorGui) {
        this.participantAndInteractionCreatorGui = participantAndInteractionCreatorGui;
        loadOptions();
    }

    private void loadOptions() {
        setLocationOptions();
        setTypeOptions();
        setRangeTypeOptions();
        setRoleOptions();
    }

    private void setLocationOptions() {
        if (locationOptions.isEmpty()){
            locationOptions.add("c-term");
            locationOptions.add("n-term");
            locationOptions.add("undetermined");
        }
    }

    private void setTypeOptions() {
        if (typeOptions.isEmpty()) {
            typeOptions.addAll(getTermsFromOls(DataAndMiID.FEATURE_TYPE.miId));
        }
    }

    private void setRangeTypeOptions() {
        if (rangeTypeOptions.isEmpty()) {
            rangeTypeOptions.addAll(getTermsFromOls(DataAndMiID.FEATURE_RANGE_TYPE.miId));
        }
    }

    private void setRoleOptions() {
        if (roleOptions.isEmpty()) {
            roleOptions.addAll(getTermsFromOls(DataAndMiID.FEATURE_ROLE.miId));
        }
    }

    public JPanel getFeatureMainPanel(boolean isBait) {
        this.bait = isBait;
        ExcelFileReader excelFileReader = participantAndInteractionCreatorGui.getExcelFileReader();
        this.fileColumns = excelFileReader.getColumns(excelFileReader.sheetSelectedUpdate);
        createFeaturesContainerPanel();
        return featureMainPanel;
    }

    private void createFeaturesContainerPanel() {
        JPanel featureContainerPanel = new JPanel();
        featureContainerPanel.setLayout(new BoxLayout(featureContainerPanel, BoxLayout.Y_AXIS));
        featureMainPanel.setLayout(new BorderLayout(5, 5));
        featureMainPanel.add(getNumberOfFeaturesSpinner(featureContainerPanel), BorderLayout.NORTH);
        featureMainPanel.add(new JScrollPane(featureContainerPanel), BorderLayout.CENTER);
    }

    private JSpinner getNumberOfFeaturesSpinner(JPanel panelToUpdate) {
        JSpinner featureCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));

        featureCountSpinner.setBorder(BorderFactory.createTitledBorder("Select number of features"));
        featureCountSpinner.addChangeListener(e -> {
            int value = (int) featureCountSpinner.getValue();
            SwingUtilities.invokeLater(() -> updateFeaturePanel(panelToUpdate, value));
        });

        updateFeaturePanel(panelToUpdate, (int) featureCountSpinner.getValue());
        return featureCountSpinner;
    }

    private void updateFeaturePanel(JPanel featureContainerPanel, int count) {
        int currentCount = featureContainerPanel.getComponentCount();
        for (int i = currentCount; i < count; i++) {
            featureContainerPanel.add(createFeaturePanel());
        }

        for (int i = currentCount - 1; i >= count; i--) {
            featureContainerPanel.remove(i);
            if (bait){
                baitFeatures.remove(i);
            } else {
                preyFeatures.remove(i);
            }
        }

        featureContainerPanel.revalidate();
        featureContainerPanel.repaint();
    }

    private JPanel createFeaturePanel() {
        Feature feature = new Feature();
        JPanel featurePanel = new JPanel();
        featurePanel.setBorder(BorderFactory.createTitledBorder("Feature"));
        featurePanel.setLayout(new GridLayout(0, 3));
        AtomicBoolean fetchFromFile = new AtomicBoolean(false);

        JCheckBox fetchFromFileToggle = new JCheckBox("Fetch from file");
        fetchFromFileToggle.setSelected(fetchFromFile.get());
        featurePanel.add(fetchFromFileToggle);

        JComboBox<String> startLocationComboBox = createComboBox(locationOptions, "Start location");
        JComboBox<String> endLocationComboBox = createComboBox(locationOptions, "End location");
        JComboBox<String> rangeTypeComboBox = createComboBox(rangeTypeOptions, "Range type");
        JComboBox<String> typeComboBox = createComboBox(typeOptions, "* Feature type");
        JComboBox<String> originalSequenceComboBox = createComboBox(new ArrayList<>(), "Original sequence");
        originalSequenceComboBox.setEditable(true);
        JComboBox<String> newSequenceComboBox = createComboBox(new ArrayList<>(), "New sequence");
        newSequenceComboBox.setEditable(true);
        JComboBox<String> shortLabelTextField = createComboBox(typeOptions, "* Feature short label");
        JComboBox<String> roleCombobox = createComboBox(roleOptions, "Feature role");

        ActionListener updateListeners = e -> {
            Object start = getComboBoxValue(startLocationComboBox);
            String startTooltip = startLocationComboBox.getToolTipText();
            feature.setStartLocation(start != null && !start.toString().equals(startTooltip) ? start.toString() : "");

            Object end = getComboBoxValue(endLocationComboBox);
            String endTooltip = endLocationComboBox.getToolTipText();
            feature.setEndLocation(end != null && !end.toString().equals(endTooltip) ? end.toString() : "");

            Object range = getComboBoxValue(rangeTypeComboBox);
            String rangeTooltip = rangeTypeComboBox.getToolTipText();
            feature.setRangeType(range != null && !range.toString().equals(rangeTooltip) ? range.toString() : "");

            Object type = getComboBoxValue(typeComboBox);
            String typeTooltip = typeComboBox.getToolTipText();
            feature.setType(type != null && !type.toString().equals(typeTooltip) ? type.toString() : "");

            Object original = getComboBoxValue(originalSequenceComboBox);
            String originalTooltip = originalSequenceComboBox.getToolTipText();
            feature.setOriginalSequence(original != null && !original.toString().equals(originalTooltip) ? original.toString() : "");

            Object newSequence = getComboBoxValue(newSequenceComboBox);
            String newSequenceTooltip = newSequenceComboBox.getToolTipText();
            feature.setNewSequence(newSequence != null && !newSequence.toString().equals(newSequenceTooltip) ? newSequence.toString() : "");

            Object shortLabel = getComboBoxValue(shortLabelTextField);
            String shortLabelTooltip = shortLabelTextField.getToolTipText();
            feature.setShortName(shortLabel != null && !shortLabel.toString().equals(shortLabelTooltip) ? shortLabel.toString() : "");

            Object role = getComboBoxValue(roleCombobox);
            String roleToolTip = roleCombobox.getToolTipText();
            feature.setRole(role != null && !role.toString().equals(roleToolTip) ? role.toString() : "");
        };

        startLocationComboBox.addActionListener(updateListeners);
        endLocationComboBox.addActionListener(updateListeners);
        rangeTypeComboBox.addActionListener(updateListeners);
        typeComboBox.addActionListener(updateListeners);
        originalSequenceComboBox.addActionListener(updateListeners);
        newSequenceComboBox.addActionListener(updateListeners);
        shortLabelTextField.addActionListener(updateListeners);
        roleCombobox.addActionListener(updateListeners);

        if (bait) {
            feature.setNumber(baitFeatures.size());
            baitFeatures.add(feature);
        } else {
            feature.setNumber(preyFeatures.size());
            preyFeatures.add(feature);
        }

        featurePanel.add(shortLabelTextField);
        featurePanel.add(typeComboBox);

        featurePanel.add(startLocationComboBox);
        featurePanel.add(endLocationComboBox);
        featurePanel.add(rangeTypeComboBox);

        featurePanel.add(originalSequenceComboBox);
        featurePanel.add(newSequenceComboBox);
        featurePanel.add(roleCombobox);

        featurePanel.add(createFeatureXrefContainerPanel(feature.getNumber()));

        fetchFromFileToggle.addActionListener(e -> {
            boolean fetch = fetchFromFileToggle.isSelected();
            fetchFromFile.set(fetch);
            feature.setFetchFromFile(fetch);

            updateComboBoxData(typeComboBox, fetch ? fileColumns : typeOptions);
            updateComboBoxData(startLocationComboBox, fetch ? fileColumns : locationOptions);
            updateComboBoxData(endLocationComboBox, fetch ? fileColumns : locationOptions);
            updateComboBoxData(rangeTypeComboBox, fetch ? fileColumns : rangeTypeOptions);
            updateComboBoxData(originalSequenceComboBox, fetch ? fileColumns : new ArrayList<>());
            updateComboBoxData(newSequenceComboBox, fetch ? fileColumns : new ArrayList<>());
            updateComboBoxData(roleCombobox, fetch ? fileColumns : roleOptions);
            updateComboBoxData(shortLabelTextField, fetch ? fileColumns : new ArrayList<>());
        });

        return featurePanel;
    }

    private Object getComboBoxValue(JComboBox<String> comboBox) {
        return comboBox.isEditable() ? comboBox.getEditor().getItem() : comboBox.getSelectedItem();
    }

    private void updateComboBoxData(JComboBox<String> comboBox, List<String> items) {
        String tooltip = comboBox.getToolTipText();
        comboBox.removeAllItems();

        if (tooltip != null) {
            comboBox.addItem(tooltip);
        }

        for (String item : items) {
            if (!item.equals(tooltip)) {
                comboBox.addItem(item);
            }
        }
    }

    private JComboBox<String> createComboBox(List<String> options, String tooltip) {
        JComboBox<String> comboBox = new JComboBox<>();
        setComboBoxDimension(comboBox, tooltip);
        for (String option : options) {
            comboBox.addItem(option);
        }
        comboBox.setEditable(true);
        comboBox.setToolTipText(tooltip);

        return comboBox;
    }

    private JPanel createFeatureXrefContainerPanel(int featureIndex) {
        JPanel containerPanel = new JPanel();
        containerPanel.setLayout(new BorderLayout(5, 5));
        containerPanel.setBorder(BorderFactory.createTitledBorder("Cross-references"));

        JPanel xrefPanelListContainer = new JPanel();
        xrefPanelListContainer.setLayout(new BoxLayout(xrefPanelListContainer, BoxLayout.Y_AXIS));

        JSpinner xrefCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        xrefCountSpinner.setSize(new Dimension(20,20));
        xrefCountSpinner.setMaximumSize(new Dimension(20,20));
        xrefCountSpinner.setBorder(BorderFactory.createTitledBorder("Select number of cross-references"));
        xrefCountSpinner.addChangeListener(e -> {
            int count = (int) xrefCountSpinner.getValue();
            updateFeatureXrefPanel(xrefPanelListContainer, count, featureIndex);
        });

        containerPanel.add(xrefCountSpinner, BorderLayout.NORTH);
        containerPanel.add(xrefPanelListContainer, BorderLayout.CENTER);

        updateFeatureXrefPanel(xrefPanelListContainer, 1, featureIndex);

        return containerPanel;
    }

    private void updateFeatureXrefPanel(JPanel xrefPanelListContainer, int numberOfXref, int featureIndex) {
        Feature currentFeature = bait ? baitFeatures.get(featureIndex) : preyFeatures.get(featureIndex);

        while (currentFeature.getXref().size() < numberOfXref) {
            currentFeature.getXref().add("");
            currentFeature.getXrefDb().add("");
            currentFeature.getXrefQualifier().add("");
        }
        while (currentFeature.getXref().size() > numberOfXref) {
            int last = currentFeature.getXref().size() - 1;
            currentFeature.getXref().remove(last);
            currentFeature.getXrefDb().remove(last);
            currentFeature.getXrefQualifier().remove(last);
        }

        xrefPanelListContainer.removeAll();
        for (int i = 0; i < numberOfXref; i++) {
            xrefPanelListContainer.add(createFeatureXrefPanel(featureIndex, i));
        }

        xrefPanelListContainer.revalidate();
        xrefPanelListContainer.repaint();
    }

    private JPanel createFeatureXrefPanel(int featureIndex, int xrefIndex) {
        JPanel xrefPanel = new JPanel();
        xrefPanel.setLayout(new BorderLayout(5, 5));
        xrefPanel.setBorder(BorderFactory.createTitledBorder("Cross-reference"));

        ExcelFileReader excelFileReader = participantAndInteractionCreatorGui.getExcelFileReader();
        ParametersGui parametersGui = new ParametersGui(excelFileReader);

        Feature currentFeature = bait ? baitFeatures.get(featureIndex) : preyFeatures.get(featureIndex);

        JComboBox<String> xrefTextField = getXrefTextField(currentFeature, xrefIndex);
        JComboBox<String> xrefDbComboBox = getXrefDbComboBox(currentFeature, xrefIndex);
        JComboBox<String> xrefQualifierComboBox = getXrefDQualifierCombobox(currentFeature, xrefIndex);

        JCheckBox addParametersToInteractionButton = getJCheckBox(currentFeature, parametersGui);

        xrefPanel.add(xrefTextField, BorderLayout.NORTH);
        xrefPanel.add(xrefDbComboBox, BorderLayout.LINE_START);
        xrefPanel.add(xrefQualifierComboBox, BorderLayout.LINE_END);
        xrefPanel.add(addParametersToInteractionButton, BorderLayout.SOUTH);

        return xrefPanel;
    }

    private static JCheckBox getJCheckBox(Feature currentFeature, ParametersGui parametersGui) {
        JCheckBox addParametersToInteractionButton = new JCheckBox("Add parameters to feature");
        addParametersToInteractionButton.addActionListener(e -> {
            currentFeature.setInduceInteractionParameters(addParametersToInteractionButton.isSelected());
            JPanel parametersPanel = parametersGui.parametersContainer();
            JOptionPane.showConfirmDialog(null, parametersPanel,"Add parameters", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            currentFeature.setParameters(parametersGui.parameters);
            currentFeature.setParametersAsString();
        });
        return addParametersToInteractionButton;
    }

    private JComboBox<String> getXrefDbComboBox(Feature currentFeature, int xrefIndex) {
        JComboBox<String> xrefDbComboBox = new JComboBox<>();
        final String tooltipText = "Database"; // Store default text

        SwingUtilities.invokeLater(() -> {
            xrefDbComboBox.setSelectedIndex(0);
            for (String db: participantAndInteractionCreatorGui.getDbCache()) {
                xrefDbComboBox.addItem(db);
            }
            xrefDbComboBox.setSelectedItem(currentFeature.getXrefDb().get(xrefIndex));
        });

        currentFeature.addFetchFromFileListener(e -> {
            xrefDbComboBox.removeAllItems();
            xrefDbComboBox.addItem(tooltipText);
            xrefDbComboBox.setSelectedIndex(0);
            if (currentFeature.isFetchFromFile()) {
                for (String column : fileColumns) {
                    xrefDbComboBox.addItem(column);
                }
            } else {
                for (String db: participantAndInteractionCreatorGui.getDbCache()) {
                    xrefDbComboBox.addItem(db);
                }
            }
        });

        setComboBoxDimension(xrefDbComboBox, tooltipText);
        xrefDbComboBox.setToolTipText(tooltipText);
        xrefDbComboBox.setEditable(true);
        xrefDbComboBox.setSelectedItem(currentFeature.getXrefDb().get(xrefIndex));

        xrefDbComboBox.addActionListener(e -> {
            String selected = (String) xrefDbComboBox.getSelectedItem();
            currentFeature.getXrefDb().set(xrefIndex,
                    tooltipText.equals(selected) ? "" : selected);
        });

        xrefDbComboBox.revalidate();
        xrefDbComboBox.repaint();
        return xrefDbComboBox;
    }

    private JComboBox<String> getXrefDQualifierCombobox(Feature currentFeature, int xrefIndex) {
        JComboBox<String> xrefQualifierCombobox = new JComboBox<>();
        final String tooltipText = "Qualifier"; // Store default text

        SwingUtilities.invokeLater(() -> {
            xrefQualifierCombobox.setSelectedIndex(0);
            for (String qualifier: participantAndInteractionCreatorGui.getXrefQualifierCache()) {
                xrefQualifierCombobox.addItem(qualifier);
            }
            xrefQualifierCombobox.setSelectedItem(currentFeature.getXrefQualifier().get(xrefIndex));
        });

        currentFeature.addFetchFromFileListener(e -> {
            xrefQualifierCombobox.removeAllItems();
            xrefQualifierCombobox.addItem(tooltipText);
            xrefQualifierCombobox.setSelectedIndex(0);
            if (currentFeature.isFetchFromFile()) {
                for (String column : fileColumns) {
                    xrefQualifierCombobox.addItem(column);
                }
            } else {
                for (String qualifier: participantAndInteractionCreatorGui.getXrefQualifierCache()) {
                    xrefQualifierCombobox.addItem(qualifier);
                }
            }
        });

        setComboBoxDimension(xrefQualifierCombobox, tooltipText);
        xrefQualifierCombobox.setToolTipText(tooltipText);
        xrefQualifierCombobox.setEditable(true);
        xrefQualifierCombobox.setSelectedItem(currentFeature.getXrefQualifier().get(xrefIndex));

        xrefQualifierCombobox.addActionListener(e -> {
            String selected = (String) xrefQualifierCombobox.getSelectedItem();
            currentFeature.getXrefQualifier().set(xrefIndex,
                    tooltipText.equals(selected) ? "" : selected);
        });

        xrefQualifierCombobox.revalidate();
        xrefQualifierCombobox.repaint();
        return xrefQualifierCombobox;
    }

    private JComboBox<String> getXrefTextField(Feature currentFeature, int xrefIndex) {
        JComboBox<String> xrefText = new JComboBox<>();
        final String tooltipText = "Cross-reference label";

        SwingUtilities.invokeLater(() -> {
            xrefText.setSelectedIndex(0);
            xrefText.setSelectedItem(currentFeature.getXref().get(xrefIndex));
        });

        currentFeature.addFetchFromFileListener(e -> {
            xrefText.removeAllItems();
            xrefText.addItem(tooltipText);
            xrefText.setSelectedIndex(0);
            if (currentFeature.isFetchFromFile()) {
                for (String column : fileColumns) {
                    xrefText.addItem(column);
                }
            }
        });

        setComboBoxDimension(xrefText, tooltipText);
        xrefText.setToolTipText(tooltipText);
        xrefText.setEditable(true);
        xrefText.setSelectedItem(currentFeature.getXrefDb().get(xrefIndex));

        xrefText.addActionListener(e -> {
            String selected = (String) xrefText.getSelectedItem();
            currentFeature.getXref().set(xrefIndex,
                    tooltipText.equals(selected) ? "" : selected);
        });

        return xrefText;
    }
}
