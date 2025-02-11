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
import java.util.List;
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
    private boolean isUpdatingSheets = false;

    private final JSpinner numberOfFeatures = new JSpinner(new SpinnerNumberModel(1, 0, 10, 1));


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
     */
    public InteractionsCreatorGui(ExcelFileReader excelFileReader, InteractionWriter writer) {
        this.excelFileReader = excelFileReader;
        this.participantCreatorPanel = new JPanel(new BorderLayout());
        this.interactionsCreator = new InteractionsCreator(excelFileReader, writer, getDataAndIndexes());
    }

    /**
     * Creates and returns the panel for configuring and creating interaction participants.
     *
     * @return A {@link JPanel} containing the participant creator interface.
     */
    public JPanel participantCreatorPanel() {
        JPanel sheetSelectorPanel = new JPanel();
        sheetSelectorPanel.setLayout(new BoxLayout(sheetSelectorPanel, BoxLayout.Y_AXIS));

        JPanel sheetWrapperPanel = new JPanel();
        sheetWrapperPanel.setLayout(new GridLayout(1,1));

        sheets.addItem("Select sheet");
        sheetWrapperPanel.add(sheets);

        numberOfFeatures.setBorder(BorderFactory.createTitledBorder("Select number of features"));
        sheetWrapperPanel.add(numberOfFeatures);

        sheetSelectorPanel.add(sheetWrapperPanel);

        sheets.addActionListener(e -> {
            if (!isUpdatingSheets) {
                interactionsCreator.sheetSelected = Objects.requireNonNull(sheets.getSelectedItem()).toString();
                setUpColumns();
            }
        });


        JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(scrollPane.getPreferredSize().width, 200)); // Set the height to 200 pixels
        sheetSelectorPanel.add(scrollPane);

        participantCreatorPanel.add(sheetSelectorPanel, BorderLayout.NORTH);
        return participantCreatorPanel;
    }

    /**
     * Populates the sheets combo box with available sheet names from the {@code ExcelFileReader}.
     * If no sheets are found, the combo box is disabled and a default placeholder is set.
     * Otherwise, the combo box is enabled and updated with the available sheets.
     * Events are temporarily suppressed using {@code isUpdatingSheets} to prevent unintended triggers.
     */
    public void setUpSheets() {
        isUpdatingSheets = true; // Suppress events

        if (excelFileReader.sheets.isEmpty()) {
            sheets.setEnabled(false);
            sheets.setSelectedIndex(0);
            sheets.addItem("Select sheet");
            setUpColumns();
        } else {
            sheets.removeAllItems();
            sheets.setEnabled(true);
            sheets.addItem("Select sheet");
            for (String sheetName : excelFileReader.sheets) {
                sheets.addItem(sheetName);
            }
//            setUpColumns();
        }
        isUpdatingSheets = false;
        addSpinnerListener();
    }

    /**
     * Sets up the columns for the currently selected sheet.
     */
    public void setUpColumns() {
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
        createInteractionDataTable();
    }

    /**
     * Creates the data table for configuring interaction participant columns.
     */
    private void createInteractionDataTable() {
        int rows = 5;
        int cols = dataNeededForInteractor.size() + (int) numberOfFeatures.getValue() * 6;
        String defaultCellValue = "Select from file";
        String otherRowsValue = "N/A";
        String defaultColumnTitle = "Title";

        if(excelFileReader.currentFilePath != null) {
            String sheetName = Objects.requireNonNull(sheets.getSelectedItem(), "Sheet selection is null").toString();
            firstLines = excelFileReader.getFileFirstLines(sheetName, rows);
        }

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

        for (int i = 0; i < (int) numberOfFeatures.getValue(); i++) {
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
                    //                    editorComboBox.setSelectedItem(value);
                    return super.getTableCellEditorComponent(table, value, isSelected, row, column);
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
            Sheet sheet = excelFileReader.workbook.getSheetAt(sheets.getSelectedIndex()-1);
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
     * Adds feature-related cells to the data table and configures them like other columns.
     *
     * @param featureIndex The index of the feature being added.
     */
    public void addFeatureCells(int featureIndex) {
        List<String> featureCells = List.of(
                DataTypeAndColumn.FEATURE_TYPE.name + "_" + featureIndex,
                DataTypeAndColumn.FEATURE_START.name + "_" + featureIndex,
                DataTypeAndColumn.FEATURE_END.name + "_" + featureIndex,
                DataTypeAndColumn.FEATURE_RANGE_TYPE.name + "_" + featureIndex,
                DataTypeAndColumn.FEATURE_XREF.name + "_" + featureIndex,
                DataTypeAndColumn.FEATURE_XREF_DB.name + "_" + featureIndex
        );

        TableModel tableModel = table.getModel();
        int baseColumnIndex = dataNeededForInteractor.size() + featureIndex * featureCells.size();

        for (int i = 0; i < featureCells.size(); i++) {
            String columnName = featureCells.get(i);
            int columnIndex = baseColumnIndex + i;

            if (columnIndex >= table.getColumnCount()) {
                continue;
            }

            TableColumn tableColumn = table.getColumnModel().getColumn(columnIndex);
            tableColumn.setHeaderValue(columnName);
            tableColumn.setPreferredWidth(150);

            JComboBox<String> comboBox = new JComboBox<>(new Vector<>(columnNames));
            String defaultValue = interactionsCreator.mostSimilarColumn(columnNames, columnName);

            if (columnNames.contains(defaultValue)) {
                comboBox.setSelectedItem(defaultValue);
                tableModel.setValueAt(defaultValue, 0, columnIndex);
            }

            comboBox.addActionListener(e -> setUpPreviewRows(columnIndex, columnName));
            columnsList.add(comboBox);

            tableColumn.setCellEditor(new DefaultCellEditor(comboBox) {
                @Override
                public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                    return row == 0 ? super.getTableCellEditorComponent(table, value, isSelected, row, column) : null;
                }
            });

            tableColumn.setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                protected void setValue(Object value) {
                    setText(value != null ? value.toString() : "Select from file");
                    setUpPreviewRows(columnIndex, columnName);
                }
            });
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

    private void addSpinnerListener() {
        numberOfFeatures.addChangeListener(e -> {
            int value = (int) numberOfFeatures.getValue();
            SwingUtilities.invokeLater(() -> {
                createInteractionDataTable();
                addFeatureCells(value);
            });
            interactionsCreator.setNumberOfFeature(value);
        });
    }
}
