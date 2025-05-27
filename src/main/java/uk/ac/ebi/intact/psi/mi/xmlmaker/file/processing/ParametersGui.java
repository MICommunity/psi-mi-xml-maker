package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.DataAndMiID;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.Parameter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils.*;

public class ParametersGui {
    List<String> parametersTypeCache = new ArrayList<>();
    List<String> parametersUnitCache = new ArrayList<>();
    List<Parameter> parameters = new ArrayList<>();
    ExcelFileReader excelFileReader;

    public ParametersGui(ExcelFileReader excelFileReader) {
        setupParametersTypeCache();
        setupUnitCache();
        this.excelFileReader = excelFileReader;
    }

    private JPanel createParameterPanel() {
        JPanel parametersPanel = new JPanel();
        parametersPanel.setLayout(new GridLayout(2,3));
        parametersPanel.setBorder(BorderFactory.createTitledBorder("Parameter"));

        Parameter parameter = new Parameter();
        parameters.add(parameter);

        parametersPanel.setBorder(BorderFactory.createCompoundBorder(
                parametersPanel.getBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        parametersPanel.add(getParametersTypeComboBox(parameter));

        JComboBox<String> value = new JComboBox<>(new Vector<>(getValueColumn()));
        XmlMakerUtils.setComboBoxDimension(value, "Value");
        value.addActionListener(e-> parameter.setValueColumn(value.getSelectedItem().toString()));
        parametersPanel.add(value);

        parametersPanel.add(getUnitComboBox(parameter));

        JTextField base = new JTextField();
        XmlMakerUtils.setTextFieldDimension(base, "Base");
        base.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { parameter.setBase(base.getText()); }
            public void removeUpdate(DocumentEvent e) { parameter.setBase(base.getText()); }
            public void changedUpdate(DocumentEvent e) { parameter.setBase(base.getText()); }
        });
        parametersPanel.add(base);

        JTextField exponent = new JTextField();
        XmlMakerUtils.setTextFieldDimension(exponent, "Exponent");
        exponent.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { parameter.setExponent(exponent.getText()); }
            public void removeUpdate(DocumentEvent e) { parameter.setExponent(exponent.getText()); }
            public void changedUpdate(DocumentEvent e) { parameter.setExponent(exponent.getText()); }
        });
        parametersPanel.add(exponent);


        JComboBox<String> uncertainty = new JComboBox<>(new Vector<>(getValueColumn()));
        XmlMakerUtils.setComboBoxDimension(uncertainty, "Uncertainty");
        parametersPanel.add(uncertainty);
        uncertainty.addActionListener(e -> parameter.setUncertaintyColumn(uncertainty.getSelectedItem().toString()));
        return parametersPanel;
    }

    private JComboBox<String> getUnitComboBox(Parameter parameter) {
        JComboBox<String> unitCombobox = new JComboBox<>();
        setComboBoxDimension(unitCombobox, "Unit");
        for (String unit : parametersUnitCache) {
            unitCombobox.addItem(unit);
        }
        unitCombobox.addActionListener(e-> parameter.setUnit(unitCombobox.getSelectedItem().toString()));
        return unitCombobox;
    }

    private JComboBox<String> getParametersTypeComboBox(Parameter parameter) {
        JComboBox<String> parametersTypeComboBox = new JComboBox<>();
        setComboBoxDimension(parametersTypeComboBox, "Parameter type");
        for (String type : parametersTypeCache) {
            parametersTypeComboBox.addItem(type);
        }
        parametersTypeComboBox.addActionListener(e-> parameter.setType(parametersTypeComboBox.getSelectedItem().toString()));
        return parametersTypeComboBox;
    }

    private void setupParametersTypeCache() {
        parametersTypeCache.addAll(getTermsFromOls(DataAndMiID.XREF_QUALIFIER.miId));
    }

    private void setupUnitCache() {
        parametersUnitCache.addAll(getTermsFromOls(DataAndMiID.UNIT.miId));
    }

    public JPanel parametersContainer() {
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
        dynamicContentPanel.add(createParameterPanel());

        int[] previousValue = {1};

        numberOfParameters.addChangeListener(e -> {
            int newValue = (int) numberOfParameters.getValue();

            if (newValue > previousValue[0]) {
                for (int i = previousValue[0]; i < newValue; i++) {
                    dynamicContentPanel.add(createParameterPanel());
                }
            } else if (newValue < previousValue[0]) {
                for (int i = previousValue[0]; i > newValue; i--) {
                    int componentCount = dynamicContentPanel.getComponentCount();
                    if (componentCount > 0) {
                        dynamicContentPanel.remove(componentCount - 1);
                        parameters.remove(parameters.size() - 1);
                    }
                }
            }

            previousValue[0] = newValue;

            dynamicContentPanel.revalidate();
            dynamicContentPanel.repaint();
        });

        return parametersContainer;
    }

    private List<String> getValueColumn(){
        return excelFileReader.getColumns(excelFileReader.sheetSelectedUpdate);
    }
}
