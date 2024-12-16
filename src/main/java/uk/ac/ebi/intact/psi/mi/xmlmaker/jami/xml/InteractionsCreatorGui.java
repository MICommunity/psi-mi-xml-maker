package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import lombok.Getter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapperGui;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.apache.poi.ss.usermodel.CellType.STRING;

public class InteractionsCreatorGui extends JPanel {
    private final JComboBox<String> sheets = new JComboBox<>();
    private final JComboBox<String> columns = new JComboBox<>();
    private JTable table = new JTable();

    private final ExcelFileReader excelFileReader;
    public final InteractionsCreator interactionsCreator;

    @Getter
    public JPanel participantCreatorPanel;
    public final Map<String, Integer> dataAndIndexes = new HashMap<>();

    final ArrayList<String> dataNeededForInteractor = new ArrayList<>(
            Arrays.stream(DataTypeAndColumn.values())
                    .map(dataType -> dataType.name)
                    .collect(Collectors.toList())
    );

    public InteractionsCreatorGui(ExcelFileReader excelFileReader, UniprotMapperGui uniprotMapperGui) {
        this.excelFileReader = excelFileReader;
        this.participantCreatorPanel = new JPanel(new BorderLayout());
        this.interactionsCreator = new InteractionsCreator(excelFileReader, uniprotMapperGui, dataAndIndexes, sheets.getSelectedIndex());
    }

    public JPanel participantCreatorPanel() {
        JPanel sheetSelectorPanel = new JPanel();
        sheetSelectorPanel.setLayout(new BoxLayout(sheetSelectorPanel, BoxLayout.Y_AXIS));

        sheets.addActionListener(e -> {
            if (!isSettingUpSheets) {
                setUpColumns();
            }
        });
        sheetSelectorPanel.add(sheets);

        table = createInteractionDataTable();
        JScrollPane scrollPane = new JScrollPane(table);
        sheetSelectorPanel.add(scrollPane);

        JButton processFileButton = createProcessFileButton();
        participantCreatorPanel.add(sheetSelectorPanel, BorderLayout.NORTH);
        participantCreatorPanel.add(processFileButton, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(participantCreatorPanel, BorderLayout.CENTER);

        return participantCreatorPanel;
    }

    private boolean isSettingUpSheets = false;

    public void setUpSheets() {
        isSettingUpSheets = true; // Suppress events during setup
        try {
            sheets.removeAllItems();
            sheets.addItem("Select sheet");
            if (excelFileReader.sheets.isEmpty()) {
                sheets.setEnabled(false);
                sheets.setSelectedIndex(0);
            } else {
                sheets.setEnabled(true);
                for (String sheetName : excelFileReader.sheets) {
                    sheets.addItem(sheetName);
                }
            }
        } finally {
            isSettingUpSheets = false;
        }
        setUpColumns();
    }

    private void setUpColumns() {
        columns.removeAllItems();
        columns.addItem("Select column to process");

        if (sheets.isEnabled()) {
            String selectedSheet = (String) sheets.getSelectedItem();
            if (selectedSheet != null && !selectedSheet.equals("Select sheet")) {
                for (String columnName : excelFileReader.getColumns(selectedSheet)) {
                    columns.addItem(columnName);
                }
            }
        } else {
            for (String columnName : excelFileReader.columns) {
                columns.addItem(columnName);
            }
        }
        columns.revalidate();
        columns.repaint();
        for (int i = 0; i < excelFileReader.getNumberOfFeatures(); i++) {
            addFeatureCells(i);
        }
    }

    private JButton createProcessFileButton() {
        JButton processFileButton = new JButton("Create participants");
        processFileButton.addActionListener(e -> {

            String sheetSelected = (String) sheets.getSelectedItem();
            String columnSelected = (String) columns.getSelectedItem();

            if (sheets.isEnabled()) {
                if (sheetSelected == null || sheetSelected.equals("Select sheet") ||
                        columnSelected == null || columnSelected.equals("Select column to process")) {
                    JOptionPane.showMessageDialog(null, "Please select a valid sheet and column!", "ERROR", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    interactionsCreator.createParticipantsWithFileFormat(getDataAndIndexes());
                    JOptionPane.showMessageDialog(null, "Participants created successfully", "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                            "An error occurred during file processing sheets: " +
                                    ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                if (columnSelected == null || columnSelected.equals("Select column to process")) {
                    JOptionPane.showMessageDialog(null, "Please select a valid column for processing!", "ERROR", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    interactionsCreator.createParticipantsWithFileFormat(getDataAndIndexes());
                    JOptionPane.showMessageDialog(null, "Participants created successfully", "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "An error occurred during file processing: " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return processFileButton;
    }

    private JTable createInteractionDataTable() {
        JTable table = new JTable();
        int rows = 1;
        int cols = dataNeededForInteractor.size();
        String defaultCellValue = "Select from file";
        String defaultColumnTitle = "Title";

        Object[][] data = new Object[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] = defaultCellValue;
            }
        }

        String[] columnTitles = new String[cols];
        Arrays.fill(columnTitles, defaultColumnTitle);

        TableModel tableModel = new DefaultTableModel(data, columnTitles);
        table.setModel(tableModel);

        for (int i = 0; i < dataNeededForInteractor.size(); i++) {
            table.getColumnModel().getColumn(i).setHeaderValue(dataNeededForInteractor.get(i));
            table.getColumnModel().getColumn(i).setCellEditor(new DefaultCellEditor(columns));
            table.getColumnModel().getColumn(i).setPreferredWidth(200);
            int finalI = i;
            table.getTableHeader().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent me) {
//                    System.out.println(dataNeededForInteractor.get(finalI));
                }
            });
        }

        if (excelFileReader.getNumberOfFeatures() > 0) {
            addFeatureCells(excelFileReader.getNumberOfFeatures());
        }
        return table;
    }

    public Map<String, Integer> getDataAndIndexes() {
        if (excelFileReader.workbook == null) {
            for (int i = 0; i < table.getColumnCount(); i++) {
                int index = excelFileReader.readFileWithSeparator().get(0).indexOf(table.getValueAt(0, i).toString());
                dataAndIndexes.put(dataNeededForInteractor.get(i), index);
            }
        } else {
            Sheet sheet = excelFileReader.workbook.getSheetAt(sheets.getSelectedIndex() - 1);
            for (int i = 0; i < table.getColumnCount(); i++) {
                Row row = sheet.getRow(0);
                for (Cell cell : row) {
                    if (cell.getCellType() == STRING && cell.getStringCellValue()
                            .equals(table.getValueAt(0, i).toString())) {
                        dataAndIndexes.put(dataNeededForInteractor.get(i), cell.getColumnIndex());
                    }
                }
            }
        }
        return dataAndIndexes;
    }

    public void addFeatureCells(int featureIndex) {
        ArrayList<String> featureCells = new ArrayList<>();
        featureCells.add(DataTypeAndColumn.FEATURE_SHORT_LABEL.name);
        featureCells.add(DataTypeAndColumn.FEATURE_TYPE.name);
        featureCells.add(DataTypeAndColumn.FEATURE_START_STATUS.name);
        featureCells.add(DataTypeAndColumn.FEATURE_END_STATUS.name);

        TableColumnModel columnModel = table.getColumnModel();

        for (String columnName : featureCells) {
            TableColumn newColumn = new TableColumn();
            newColumn.setPreferredWidth(200);
            newColumn.setHeaderValue(columnName);
            newColumn.setCellEditor(new DefaultCellEditor(columns));
            columnModel.addColumn(newColumn);
            dataNeededForInteractor.add(columnName + "_" + featureIndex);
        }
    }

}
