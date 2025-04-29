package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing;

import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.DataAndMiID;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils.*;

public class Parameters {
    JPanel parametersPanel;
    List<String> parametersTypeCache = new ArrayList<>();
    List<String> parametersUnitCache = new ArrayList<>();

    public Parameters() {
        parametersPanel = new JPanel();
        setupParametersTypeCache();
        setupUnitCache();
    }

    public JPanel getParametersPanel() {
        parametersPanel.setLayout(new BoxLayout(parametersPanel, BoxLayout.PAGE_AXIS));
        parametersPanel.setSize(new Dimension(200, 200));
        parametersPanel.setBorder(BorderFactory.createTitledBorder("Parameter"));

        parametersPanel.add(getParametersTypeComboBox());

        JTextField value = new JTextField("Value");
        parametersPanel.add(value);

        parametersPanel.add(getUnitComboBox());

        JTextField base = new JTextField("Base");
        parametersPanel.add(base);

        JTextField exponent = new JTextField("Exponent");
        parametersPanel.add(exponent);

        JTextField uncertainty = new JTextField("Uncertainty");
        parametersPanel.add(uncertainty);

        return parametersPanel;
    }

    private JComboBox<String> getUnitComboBox() {
        JComboBox<String> unitCombobox = new JComboBox<>();
        setComboBoxDimension(unitCombobox, "Unit");
        for (String unit : parametersUnitCache) {
            unitCombobox.addItem(unit);
        }
        return unitCombobox;
    }

    private JComboBox<String> getParametersTypeComboBox() {
        JComboBox<String> parametersTypeComboBox = new JComboBox<>();
        setComboBoxDimension(parametersTypeComboBox, "Parameter type");
        for (String type : parametersTypeCache) {
            parametersTypeComboBox.addItem(type);
        }
        return parametersTypeComboBox;
    }

    private void setupParametersTypeCache() {
        parametersTypeCache.addAll(getTermsFromOls(DataAndMiID.XREF_QUALIFIER.miId));
    }

    private void setupUnitCache() {
        parametersTypeCache.addAll(getTermsFromOls(DataAndMiID.UNIT.miId));
    }
}
