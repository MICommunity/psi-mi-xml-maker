package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import lombok.Getter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.DataAndMiID;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils.*;
import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils.getTermsFromOls;

public class FeatureCreatorGui {
    JPanel featureMainPanel = new JPanel();

    private final List<String> locationOptions = new ArrayList<>();
    private final List<String> typeOptions = new ArrayList<>();
    private final List<String> rangeTypeOptions = new ArrayList<>();

    @Getter
    private final List<Feature> baitFeatures = new ArrayList<>();
    @Getter
    private final List<Feature> preyFeatures = new ArrayList<>();

    private boolean bait;

    private final ParticipantAndInteractionCreatorGui participantAndInteractionCreatorGui;

    public FeatureCreatorGui(ParticipantAndInteractionCreatorGui participantAndInteractionCreatorGui) {
        this.participantAndInteractionCreatorGui = participantAndInteractionCreatorGui;
        loadOptions();
    }

    private void loadOptions() {
        setLocationOptions();
        setTypeOptions();
        setRangeTypeOptions();
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
            typeOptions.addAll(getTermsFromOls(DataAndMiID.FEATUTRE_TYPE.miId));
        }
    }

    private void setRangeTypeOptions() {
        if (rangeTypeOptions.isEmpty()) {
            rangeTypeOptions.addAll(getTermsFromOls(DataAndMiID.FEATURE_RANGE_TYPE.miId));
        }
    }

    public JPanel getFeatureMainPanel(boolean isBait) {
        bait = isBait;
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
        featurePanel.setLayout(new GridLayout(3,1));

        JComboBox<String> startLocationComboBox = createComboBox(locationOptions, "Start location");
        startLocationComboBox.addActionListener(e -> feature.setStartLocation(startLocationComboBox.getSelectedItem().toString()));

        JComboBox<String> endLocationComboBox = createComboBox(locationOptions, "End location");
        endLocationComboBox.addActionListener(e -> feature.setEndLocation(endLocationComboBox.getSelectedItem().toString()));

        JComboBox<String> rangeTypeComboBox = createComboBox(rangeTypeOptions, "Range type");
        rangeTypeComboBox.addActionListener(e -> feature.setRangeType(rangeTypeComboBox.getSelectedItem().toString()));

        JComboBox<String> typeComboBox = createComboBox(typeOptions, "Feature type");
        typeComboBox.addActionListener(e -> feature.setType(typeComboBox.getSelectedItem().toString()));

        JTextField originalSequenceTextField = new JTextField();
        setTextFieldDimension(originalSequenceTextField, "Original sequence");
        originalSequenceTextField.addActionListener(e -> feature.setOriginalSequence(originalSequenceTextField.getText()));

        JTextField newSequenceTextField = new JTextField();
        setTextFieldDimension(newSequenceTextField, "New sequence");
        newSequenceTextField.addActionListener(e -> feature.setNewSequence(newSequenceTextField.getText()));

        JTextField shortLabelTextField = new JTextField();
        setTextFieldDimension(shortLabelTextField, "Short label");
        shortLabelTextField.addActionListener(e -> feature.setShortName(shortLabelTextField.getText()));

        if (bait) {
            feature.setNumber(baitFeatures.size());
            baitFeatures.add(feature);
        } else {
            feature.setNumber(preyFeatures.size());
            preyFeatures.add(feature);
        }

        featurePanel.add(startLocationComboBox);
        featurePanel.add(endLocationComboBox);
        featurePanel.add(rangeTypeComboBox);
        featurePanel.add(typeComboBox);
        featurePanel.add(shortLabelTextField);
        featurePanel.add(originalSequenceTextField);
        featurePanel.add(newSequenceTextField);
        featurePanel.add(createFeatureXrefContainerPanel(feature.getNumber()));

        return featurePanel;
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

        JTextField xrefTextField = new JTextField(currentFeature.getXref().get(xrefIndex), 15);
        setTextFieldDimension(xrefTextField, "Cross-reference");
        xrefTextField.addActionListener(e -> currentFeature.getXref().set(xrefIndex, xrefTextField.getText()));

        JComboBox xrefDbComboBox = new JComboBox<>(participantAndInteractionCreatorGui.getDbCache().toArray());
        setComboBoxDimension(xrefDbComboBox, "Database");
        xrefDbComboBox.setSelectedItem(currentFeature.getXrefDb().get(xrefIndex));
        xrefDbComboBox.addActionListener(e -> currentFeature.getXrefDb().set(xrefIndex, (String) xrefDbComboBox.getSelectedItem()));

        JComboBox xrefQualifierComboBox = new JComboBox<>(participantAndInteractionCreatorGui.getXrefQualifierCache().toArray());
        setComboBoxDimension(xrefQualifierComboBox, "Qualifier");
        xrefQualifierComboBox.setSelectedItem(currentFeature.getXrefQualifier().get(xrefIndex));
        xrefQualifierComboBox.addActionListener(e -> currentFeature.getXrefQualifier().set(xrefIndex, (String) xrefQualifierComboBox.getSelectedItem()));

        JCheckBox addParametersToInteractionButton = new JCheckBox("Add parameters to feature");
        addParametersToInteractionButton.addActionListener(e -> {
            currentFeature.setInduceInteractionParameters(addParametersToInteractionButton.isSelected());
            JPanel parametersPanel = parametersGui.parametersContainer();
            JOptionPane.showConfirmDialog(null, parametersPanel,"Add parameters", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            currentFeature.setParameters(parametersGui.parameters);
            currentFeature.setParametersAsString();
        });

        xrefPanel.add(xrefTextField, BorderLayout.NORTH);
        xrefPanel.add(xrefDbComboBox, BorderLayout.LINE_START);
        xrefPanel.add(xrefQualifierComboBox, BorderLayout.LINE_END);
        xrefPanel.add(addParametersToInteractionButton, BorderLayout.SOUTH);

        return xrefPanel;
    }
}
