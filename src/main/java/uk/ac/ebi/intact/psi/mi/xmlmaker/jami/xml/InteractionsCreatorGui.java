package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import lombok.Getter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.SuggestedOrganisms;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static org.apache.poi.ss.usermodel.CellType.STRING;

public class InteractionsCreatorGui extends JPanel {
    private final JComboBox<String> sheets = new JComboBox<>();
    private final JComboBox<String> columns = new JComboBox<>();
    private final ExcelFileReader excelFileReader;
    @Getter
    public JPanel participantCreatorPanel;
    public Map<String, Integer> dataTypeAndColumn = new HashMap<>();
    private JTable table = new JTable();
    private InteractionsCreator interactionsCreator;
    private PsiMiXmlMaker xmlMaker;
    SuggestedOrganisms suggestedOrganisms; //TODO: Add this column instead of the "participant taxID"

    String[] dataNeededForInteractor = {
            "Interaction number",
            "Participant name",
            "Participant type",
            "Participant taxID",
            "Participant ID",
            "Participant ID database",
            "Experimental role"
    };


    public InteractionsCreatorGui(ExcelFileReader excelFileReader) {
        this.excelFileReader = excelFileReader;
        setUpSheets();
        this.participantCreatorPanel = new JPanel(new BorderLayout());
        this.interactionsCreator = new InteractionsCreator(excelFileReader);
    }

    public JPanel participantCreatorPanel() {
        JPanel sheetSelectorPanel = new JPanel();
        sheetSelectorPanel.setLayout(new BoxLayout(sheetSelectorPanel, BoxLayout.Y_AXIS));

        sheets.addItem("Select sheet");
        sheets.addActionListener(e -> setUpColumns());
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

    public void setUpSheets() {
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
                    interactionsCreator.createParticipantsWithFileFormat(getDataTypeAndColumn(), sheets.getSelectedIndex());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "An error occurred during file processing sheets: " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                if (columnSelected == null || columnSelected.equals("Select column to process")) {
                    JOptionPane.showMessageDialog(null, "Please select a valid column for processing!", "ERROR", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    interactionsCreator.createParticipantsWithFileFormat(getDataTypeAndColumn(), 0);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "An error occurred during file processing: " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return processFileButton;
    }

    private JTable createInteractionDataTable() {
        JTable table = new JTable();
        TableModel tableModel = new DefaultTableModel(
                new Object[][]{{"Select from file", "Select from file", "Select from file", "Select from file",
                        "Select from file", "Select from file", "Select from file"},},
                new String[]{"Title 1", "Title 2", "Title 3", "Title 4", "Title 5", "Title 6", "Title 7"}
        );

        table.setModel(tableModel);

        for (int i = 0; i < dataNeededForInteractor.length; i++) {
            table.getColumnModel().getColumn(i).setHeaderValue(dataNeededForInteractor[i]);
            table.getColumnModel().getColumn(i).setCellEditor(new DefaultCellEditor(columns));
        }

        return table;
    }

    public Map<String, Integer> getDataTypeAndColumn() {
        if (excelFileReader.workbook == null) {
            for (int i = 0; i < table.getColumnCount(); i++) {
                int index = excelFileReader.readFileWithSeparator().get(0).indexOf(table.getValueAt(0, i).toString());
                dataTypeAndColumn.put(dataNeededForInteractor[i], index);
            }
        } else {
            Sheet sheet = excelFileReader.workbook.getSheetAt(sheets.getSelectedIndex()-1);
            for (int i = 0; i < table.getColumnCount(); i++) {
                Row row = sheet.getRow(0);
                for (Cell cell : row) {
                    if (cell.getCellType() == STRING && cell.getStringCellValue()
                            .equals(table.getValueAt(0, i).toString())){
                        dataTypeAndColumn.put(dataNeededForInteractor[i], cell.getColumnIndex());
                    }
                }
            }
        }
        return dataTypeAndColumn;
    }


}
