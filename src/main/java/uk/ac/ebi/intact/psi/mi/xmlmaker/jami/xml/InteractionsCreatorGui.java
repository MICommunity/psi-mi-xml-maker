package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import lombok.Getter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapperGui;

import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.poi.ss.usermodel.CellType.STRING;

/**
 * This class provides a graphical user interface for creating interaction participants
 * from an Excel file using the Apache POI library. It integrates with {@link ExcelFileReader}
 * to read data from Excel sheets and supports the selection of columns to process
 * for interaction creation.
 * The class extends {@link JPanel} and includes a table for displaying and configuring
 * data columns. It supports interaction with Uniprot mapping via {@link UniprotMapperGui}.
 */
public class InteractionsCreatorGui extends JPanel {
    private final JComboBox<String> sheets = new JComboBox<>();
    private final List<JComboBox<String>> columnsList = new ArrayList<>();
    private final List<String> columnNames = new ArrayList<>();
    private final JTable table = new JTable();

    private final ExcelFileReader excelFileReader;
    public final InteractionsCreator interactionsCreator;
    private List<List<String>> firstLines = new ArrayList<>();


    @Getter
    public JPanel participantCreatorPanel ;
    public final Map<String, Integer> dataAndIndexes = new HashMap<>();
    final ArrayList<String> dataNeededForInteractor = new ArrayList<>(
            Arrays.stream(DataTypeAndColumn.values())
                    .filter(dataType -> dataType.initial)
                    .map(dataType -> dataType.name)
                    .collect(Collectors.toList())
    );


    /**
     * Constructs an instance of {@code InteractionsCreatorGui}.
     *
     * @param excelFileReader  The Excel file reader used to read data.
     * @param writer Interaction writer
     * @param uniprotMapperGui The Uniprot mapper GUI for integrating with Uniprot.
     */
    public InteractionsCreatorGui(ExcelFileReader excelFileReader, InteractionWriter writer, UniprotMapperGui uniprotMapperGui) {
        this.excelFileReader = excelFileReader;
        this.participantCreatorPanel = new JPanel(new BorderLayout());
        this.interactionsCreator = new InteractionsCreator(excelFileReader, writer, uniprotMapperGui, getDataAndIndexes());
    }

    /**
     * Creates and returns the panel for configuring and creating interaction participants.
     *
     * @return A {@link JPanel} containing the participant creator interface.
     */
    public JPanel participantCreatorPanel() {
        JPanel sheetSelectorPanel = new JPanel();
        sheetSelectorPanel.setLayout(new BoxLayout(sheetSelectorPanel, BoxLayout.Y_AXIS));

        sheetSelectorPanel.add(sheets);

        sheets.addActionListener(e -> {
            interactionsCreator.sheetSelected = Objects.requireNonNull(sheets.getSelectedItem()).toString();
            setUpColumns();
            createInteractionDataTable();
        });

        JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(scrollPane.getPreferredSize().width, 200)); // Set the height to 200 pixels
        sheetSelectorPanel.add(scrollPane);

        participantCreatorPanel.add(sheetSelectorPanel, BorderLayout.NORTH);
        return participantCreatorPanel;
    }

    /**
     * Sets up the available sheets in the ComboBox for selection.
     */
    public void setUpSheets() {
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

    }

    /**
     * Sets up the columns for the currently selected sheet.
     */
    private void setUpColumns() {
        columnsList.clear();
        columnNames.clear();
        if (sheets.isEnabled()) {
            String selectedSheet = (String) sheets.getSelectedItem();
            if (selectedSheet != null && !selectedSheet.equals("Select sheet")) {
                columnNames.addAll(excelFileReader.getColumns(selectedSheet));
            }
        } else {
            columnNames.addAll(excelFileReader.getColumns(""));
        }
    }

    /**
     * Creates the data table for configuring interaction participant columns.
     */
    private void createInteractionDataTable() {
        int rows = 5;
        int cols = dataNeededForInteractor.size() + excelFileReader.getNumberOfFeatures() * 4;
        String defaultCellValue = "Select from file";
        String otherRowsValue = "N/A";
        String defaultColumnTitle = "Title";

        String sheetName = Objects.requireNonNull(sheets.getSelectedItem(), "Sheet selection is null").toString();
        firstLines = excelFileReader.getFileFirstLines(sheetName, rows);

        Object[][] data = new Object[rows][cols];
        for (int i = 0; i < rows; i++) {
            Arrays.fill(data[i], (i == 0) ? defaultCellValue : otherRowsValue);
        }

        String[] columnTitles = new String[cols];
        Arrays.fill(columnTitles, defaultColumnTitle);

        TableModel tableModel = new DefaultTableModel(data, columnTitles);
        table.setModel(tableModel);

        for (int i = 0; i < dataNeededForInteractor.size(); i++) {
            configureColumn(i, dataNeededForInteractor.get(i), data, tableModel);
        }

        for (int i = 0; i < excelFileReader.getNumberOfFeatures(); i++) {
            addFeatureCells(i);
        }

        configureTableAppearance();
    }

    /**
     * Configures a table column with the specified index and header value.
     *
     * @param columnIndex The index of the column to configure.
     * @param headerValue The header value for the column.
     * @param data        The table data array.
     * @param tableModel  The table model to update.
     */
    private void configureColumn(int columnIndex, String headerValue, Object[][] data, TableModel tableModel) {
        TableColumn tableColumn = table.getColumnModel().getColumn(columnIndex);
        tableColumn.setHeaderValue(headerValue);
        tableColumn.setPreferredWidth(150);

        JComboBox<String> comboBox = new JComboBox<>(new Vector<>(columnNames));
        String defaultValue = interactionsCreator.mostSimilarColumn(columnNames, headerValue);

        if (columnNames.contains(defaultValue)) {
            comboBox.setSelectedItem(defaultValue);
            data[0][columnIndex] = defaultValue;
            tableModel.setValueAt(defaultValue, 0, columnIndex);
        }

        comboBox.addActionListener(e -> setUpPreviewRows(columnIndex, headerValue));
        columnsList.add(comboBox);

        tableColumn.setCellEditor(new DefaultCellEditor(comboBox) {
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                if (row == 0) {
                    JComboBox<String> editorComboBox = (JComboBox<String>) super.getTableCellEditorComponent(table, value, isSelected, row, column);
                    editorComboBox.setSelectedItem(value);
                    return editorComboBox;
                }
                return null;
            }
        });

        tableColumn.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(value != null ? value.toString() : "Select from file");
                setUpPreviewRows(columnIndex, headerValue);
            }
        });
    }

    /**
     * Configures the overall appearance of the table, including header and resizing behaviour.
     */
    private void configureTableAppearance() {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                component.setBackground(Color.decode("#68297c"));
                component.setForeground(Color.WHITE);
                return component;
            }
        });

        table.repaint();
        table.revalidate();
    }

    /**
     * Retrieves the mapping of data types to column indices.
     *
     * @return A {@link Map} linking data types to column indices.
     */
    public Map<String, Integer> getDataAndIndexes() {
        List<String> tableColumnsNames = getTableColumnNames();
        if (excelFileReader.workbook == null) {
            for (int i = 0; i < table.getColumnCount(); i++) {
                int index = excelFileReader.fileData.indexOf(table.getValueAt(0, i).toString());
                dataAndIndexes.put(tableColumnsNames.get(i), index);
            }
        } else {
            Sheet sheet = excelFileReader.workbook.getSheetAt(sheets.getSelectedIndex() - 1);
            for (int i = 0; i < table.getColumnCount(); i++) {
                Row row = sheet.getRow(0); // get the header
                for (Cell cell : row) {
                    if (cell.getCellType() == STRING && cell.getStringCellValue()
                            .equals(table.getValueAt(0, i).toString())) {
                        dataAndIndexes.put(tableColumnsNames.get(i), cell.getColumnIndex());
                    }
                }
            }
        }
        return dataAndIndexes;
    }

    /**
     * Adds feature-related cells to the data table.
     *
     * @param featureIndex The index of the feature being added.
     */
    public void addFeatureCells(int featureIndex) {
        ArrayList<String> featureCells = new ArrayList<>();
        featureCells.add(DataTypeAndColumn.FEATURE_SHORT_LABEL.name + "_" + featureIndex);
        featureCells.add(DataTypeAndColumn.FEATURE_TYPE.name + "_" + featureIndex);
        featureCells.add(DataTypeAndColumn.FEATURE_START_STATUS.name + "_" + featureIndex);
        featureCells.add(DataTypeAndColumn.FEATURE_END_STATUS.name + "_" + featureIndex);

        TableColumnModel columnModel = table.getColumnModel();

        for (int i = 0; i < featureCells.size(); i++) {
            String columnName = featureCells.get(i);
            TableColumn newColumn = columnModel.getColumn(dataNeededForInteractor.size() + featureIndex * 4 + i);
            newColumn.setHeaderValue(columnName);
            newColumn.setPreferredWidth(150);
            JComboBox<String> comboBox = new JComboBox<>(new Vector<>(columnNames));
            columnsList.add(comboBox);
            newColumn.setCellEditor(new DefaultCellEditor(comboBox));
        }
    }

    /**
     * Retrieves the names of the columns in the data table.
     *
     * @return A {@link List} of column names.
     */
    public List<String> getTableColumnNames() {
        List<String> tableColumnNames = new ArrayList<>();
        for (int i = 0; i < table.getColumnCount(); i++) {
            String columnName = table.getColumnModel().getColumn(i).getHeaderValue().toString();
            tableColumnNames.add(columnName);
        }
        return tableColumnNames;
    }

    /**
     * Configures preview rows in the table based on the selected column and combo box index.
     *
     * @param comboBoxIndex The index of the combo box column in the table where data should be set.
     * @param columnName    The name of the column from which data will be fetched.
     *
     * @throws IndexOutOfBoundsException If the comboBoxIndex is out of bounds for the table.
     */
    public void setUpPreviewRows(int comboBoxIndex, String columnName) {
        if (excelFileReader.currentFilePath == null) {
            System.err.println("No file is loaded. Cannot configure preview rows.");
            return;
        }

        getDataAndIndexes();
        Integer index = dataAndIndexes.get(columnName);
        if (index == null || index < 0) {
            System.err.println("Invalid column index mapping for selection: " + columnName);
            return;
        }

        TableModel tableModel = table.getModel();
        int columnCount = tableModel.getColumnCount();
        int rowCount = tableModel.getRowCount();

        if (comboBoxIndex < 0 || comboBoxIndex >= columnCount) {
            throw new IndexOutOfBoundsException("ComboBox index " + comboBoxIndex + " is out of bounds for the table.");
        }

        int firstLinesCount = Math.min(firstLines.size(), rowCount);

        for (int rowIndex = 1; rowIndex < firstLinesCount; rowIndex++) {
            List<String> rowData = firstLines.get(rowIndex);
            String value = (index < rowData.size()) ? rowData.get(index) : "N/A";
            tableModel.setValueAt(value, rowIndex, comboBoxIndex);
        }
    }
}
