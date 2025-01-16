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
        columnNames.add("Select column to process");
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
        int rows = 1;
        int cols = dataNeededForInteractor.size() + excelFileReader.getNumberOfFeatures() * 4;
        String defaultCellValue = "Select from file";
        String otherRowsValue = "value";
        String defaultColumnTitle = "Title";

        Object[][] data = new Object[rows][cols];
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] = defaultCellValue;
            }
        }
        for (int i = 1; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] = otherRowsValue;
            }
        }

        String[] columnTitles = new String[cols];
        Arrays.fill(columnTitles, defaultColumnTitle);

        TableModel tableModel = new DefaultTableModel(data, columnTitles);
        table.setModel(tableModel);

        // Add combo box only to the first row
        for (int i = 0; i < dataNeededForInteractor.size(); i++) {
            TableColumn tableColumn = table.getColumnModel().getColumn(i);
            tableColumn.setHeaderValue(dataNeededForInteractor.get(i));
            tableColumn.setPreferredWidth(150);
            JComboBox<String> comboBox = new JComboBox<>(new Vector<>(columnNames));
//            int finalI = i;
//            comboBox.addActionListener(e -> {
//                setUpPreviewRows(comboBox, finalI);
//            });
            columnsList.add(comboBox);

            tableColumn.setCellEditor(new DefaultCellEditor(comboBox) {
                @Override
                public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                    if (row == 0) {
                        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
                    }
                    return null;
                }
            });
        }

        for (int i = 0; i < excelFileReader.getNumberOfFeatures(); i++) {
            addFeatureCells(i);
        }

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
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
     * Configures table preview rows based on an Excel file and combo box selection.
     *
     * @param comboBox      the JComboBox with column names.
     * @param comboBoxIndex the index of the table column to update.
     * This method:
     * 1. Exits if no file is loaded.
     * 2. Retrieves preview data from the selected sheet.
     * 3. Maps the selected combo box item to a column index.
     * 4. Updates the table's column model with preview values.
     *
     * @throws NullPointerException if combo box or sheet selection is null.
     */
    public void setUpPreviewRows(JComboBox<String> comboBox, int comboBoxIndex) {
        if (excelFileReader.currentFilePath == null) {
            return;
        }

        TableColumnModel columnModel = table.getColumnModel();

        List<List<String>> firstLines = excelFileReader.getFileFirstLines(Objects.requireNonNull(sheets.getSelectedItem()).toString(), 5);
        String listSelection = Objects.requireNonNull(comboBox.getSelectedItem()).toString();

        getDataAndIndexes();
        int index = dataAndIndexes.get(listSelection);

        for (List<String> firstLine : firstLines) {
            String value = firstLine.get(index);
            columnModel.getColumn(comboBoxIndex);
            columnModel.getColumn(index);
        }
    }
}
