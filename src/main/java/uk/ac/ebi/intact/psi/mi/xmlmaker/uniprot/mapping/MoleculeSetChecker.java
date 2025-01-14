package uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MoleculeSetChecker {

    private static final Logger LOGGER = LogManager.getLogger(MoleculeSetChecker.class);
    private static final String DEFAULT_MOLECULE_SET_PATH = "src/main/resources/molecule_sets.xls";
    private static final int PROTEINS_CELL_INDEX = 3;
    private static final int MOLECULE_SET_AC_COLUMN_INDEX = 0;

    private final DataFormatter formatter = new DataFormatter();
    private final Map<String, String> proteinAndMoleculeSet = new HashMap<>();

    public MoleculeSetChecker() {
        LOGGER.info("Initializing MoleculeSetChecker.");
        parseMoleculeSetFile();
    }

    private Workbook readFile() throws IOException {
        File file = new File(DEFAULT_MOLECULE_SET_PATH);
        if (!file.exists() || !file.isFile()) {
            LOGGER.warn("Molecule set file does not exist at path: {}", DEFAULT_MOLECULE_SET_PATH);
            return null;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            LOGGER.info("Reading molecule set file from: {}", DEFAULT_MOLECULE_SET_PATH);
            return WorkbookFactory.create(fis);
        } catch (IOException e) {
            LOGGER.error("Error reading molecule set file: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void parseMoleculeSetFile() {
        try (Workbook workbook = readFile()) {
            if (workbook == null) {
                LOGGER.warn("No workbook found. Skipping parsing of molecule set file.");
                return;
            }

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                LOGGER.warn("No sheet found in the molecule set file.");
                return;
            }

            LOGGER.info("Parsing molecule set file.");
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row

                String proteins = formatter.formatCellValue(row.getCell(PROTEINS_CELL_INDEX));
                String moleculeSetAc = formatter.formatCellValue(row.getCell(MOLECULE_SET_AC_COLUMN_INDEX));

                if (proteins.isEmpty() || moleculeSetAc.isEmpty()) continue;

                for (String protein : proteins.split(",")) {
                    protein = protein.trim();
                    proteinAndMoleculeSet.putIfAbsent(protein, moleculeSetAc);
                    LOGGER.debug("Mapped protein '{}' to molecule set '{}'.", protein, moleculeSetAc);
                }
            }
            LOGGER.info("Finished parsing molecule set file.");
        } catch (IOException e) {
            LOGGER.error("Error while parsing molecule set file: {}", e.getMessage(), e);
        }
    }

    public boolean isProteinPartOfMoleculeSet(String proteinAc) {
        boolean result = proteinAndMoleculeSet.containsKey(proteinAc);
        if (result) {
            LOGGER.debug("Protein '{}' is part of a molecule set.", proteinAc);
        } else {
            LOGGER.debug("Protein '{}' is not part of any molecule set.", proteinAc);
        }
        return result;
    }
}
