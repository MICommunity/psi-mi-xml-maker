package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.gui;

import lombok.Getter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.FileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.VariableExperimentalCondition;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.CacheUtils;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class VariableExperimentalConditionGui {
    private final List<String> experimentalConditionUnitCache = new ArrayList<>();
    @Getter
    private final List<VariableExperimentalCondition> experimentalConditions = new ArrayList<>();

    final FileReader fileReader;
    public VariableExperimentalConditionGui(FileReader fileReader) {
        this.fileReader = fileReader;
        setupUnitCache();

    }

    private void setupUnitCache() {
        experimentalConditionUnitCache.addAll(CacheUtils.UNITS);
    }

    public JPanel getVariableExperimentalConditionPanel() {
        JPanel parametersContainer = new JPanel();
        parametersContainer.setLayout(new BoxLayout(parametersContainer, BoxLayout.Y_AXIS));
        parametersContainer.setPreferredSize(new Dimension(1000, 500));

        JSpinner numberOfParameters = new JSpinner();
        numberOfParameters.setModel(new SpinnerNumberModel(1, 1, 15, 1));

        JPanel dynamicContentPanel = new JPanel();
        dynamicContentPanel.setLayout(new BoxLayout(dynamicContentPanel, BoxLayout.Y_AXIS));
        dynamicContentPanel.setAutoscrolls(true);

        JScrollPane scrollPane = new JScrollPane(dynamicContentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(900, 900));

        parametersContainer.add(numberOfParameters);
        parametersContainer.add(scrollPane);
        dynamicContentPanel.add(getOneVariableExperimentalConditionPanel());

        int[] previousValue = {1};

        numberOfParameters.addChangeListener(e -> {
            int newValue = (int) numberOfParameters.getValue();

            if (newValue > previousValue[0]) {
                for (int i = previousValue[0]; i < newValue; i++) {
                    dynamicContentPanel.add(getOneVariableExperimentalConditionPanel());
                }
            } else if (newValue < previousValue[0]) {
                for (int i = previousValue[0]; i > newValue; i--) {
                    int componentCount = dynamicContentPanel.getComponentCount();
                    if (componentCount > 0) {
                        dynamicContentPanel.remove(componentCount - 1);
                        experimentalConditions.remove(experimentalConditions.size() - 1);
                    }
                }
            }

            previousValue[0] = newValue;

            dynamicContentPanel.revalidate();
            dynamicContentPanel.repaint();
        });

        return parametersContainer;
    }

    public JPanel getOneVariableExperimentalConditionPanel() {
        JPanel variableConditionPanel = new JPanel();
        variableConditionPanel.setLayout(new GridLayout(0,3));
        variableConditionPanel.setBorder(BorderFactory.createTitledBorder("Experimental variable Condition"));

        VariableExperimentalCondition variableExperimentalCondition = new VariableExperimentalCondition();
        variableConditionPanel.add(getDescriptionComboBox(variableExperimentalCondition));
        variableConditionPanel.add(getValueColumnComboBox(variableExperimentalCondition));
        variableConditionPanel.add(getUnitComboBox(variableExperimentalCondition));

        experimentalConditions.add(variableExperimentalCondition);
        return variableConditionPanel;
    }

    private JComboBox<String> getUnitComboBox(VariableExperimentalCondition variableExperimentalCondition) {
        JComboBox<String> unitComboBox = new JComboBox<>();
        XmlMakerUtils.setComboBoxDimension(unitComboBox, "Unit");
        unitComboBox.setEditable(true);
        for (String unit: experimentalConditionUnitCache){
            unitComboBox.addItem(unit);
        }
        unitComboBox.addActionListener(e-> variableExperimentalCondition.setUnit(unitComboBox.getSelectedItem().toString()));
        return unitComboBox;
    }

    private JComboBox<String> getDescriptionComboBox(VariableExperimentalCondition variableExperimentalCondition) {
        JComboBox<String> descriptionComboBox = new JComboBox<>();
        XmlMakerUtils.setComboBoxDimension(descriptionComboBox, "Description");
        descriptionComboBox.setEditable(true);
        descriptionComboBox.addActionListener(e-> variableExperimentalCondition.setDescription(descriptionComboBox.getSelectedItem().toString()));
        return descriptionComboBox;
    }

    private JComboBox<String> getValueColumnComboBox(VariableExperimentalCondition variableExperimentalCondition) {
        JComboBox<String> valueColumnComboBox = new JComboBox<>();
        XmlMakerUtils.setComboBoxDimension(valueColumnComboBox, "Value");
        valueColumnComboBox.setEditable(true);
        for (String valueColumn: fileReader.getColumns(fileReader.getSheetSelectedUpdate())){
            valueColumnComboBox.addItem(valueColumn);
        }
        valueColumnComboBox.addActionListener(e -> variableExperimentalCondition.setValueColumn(valueColumnComboBox.getSelectedItem().toString()));
        return valueColumnComboBox;
    }
}
