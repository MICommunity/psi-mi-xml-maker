package uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping;

import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MoleculeSetChecker {

    private static final String DEFAULT_MOLECULE_SET_PATH = "src/main/resources/molecule_sets.xls";
    private static final int PROTEINS_CELL_INDEX = 3;
    private static final int MOLECULE_SET_AC_COLUMN_INDEX = 0;

    private final DataFormatter formatter = new DataFormatter();
    private final Map<String, String> proteinAndMoleculeSet = new HashMap<>();

    public MoleculeSetChecker()  {
        parseMoleculeSetFile();
    }

    private Workbook readFile() throws IOException {
        File file = new File(DEFAULT_MOLECULE_SET_PATH);
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            return WorkbookFactory.create(fis);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void parseMoleculeSetFile() {
        try (Workbook workbook = readFile()) {
            if (workbook == null) return;

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return;
            }

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                String proteins = formatter.formatCellValue(row.getCell(PROTEINS_CELL_INDEX));
                String moleculeSetAc = formatter.formatCellValue(row.getCell(MOLECULE_SET_AC_COLUMN_INDEX));

                if (!proteins.isEmpty() && !moleculeSetAc.isEmpty()) {
                    for (String protein : proteins.split(",")) {
                        protein = protein.trim();
                        proteinAndMoleculeSet.putIfAbsent(protein, moleculeSetAc);
                    }
                }
            }
   } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isProteinPartOfMoleculeSet(String proteinAc) {
        return proteinAndMoleculeSet.containsKey(proteinAc);
    }
}
