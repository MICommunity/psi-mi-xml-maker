package uk.ac.ebi.intact.psi.mi.xmlmaker;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml.DataTypeAndColumn;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml.InteractionsCreator;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapper;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapperGui;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class XmlMakerGuiTest extends TestCase {
    private static final Logger LOGGER = Logger.getLogger(XmlMakerGuiTest.class.getName());
    int numberOfColumns = 24;
    private final String testFilesPath = "src/test/java/uk/ac/ebi/intact/psi/mi/xmlmaker/testSamples/";

    public void testOpenCsvFile(){
        ExcelFileReader reader = new ExcelFileReader();
        LOGGER.info("Testing csv opening");
        reader.selectFileOpener(testFilesPath + "test_sample.csv");
        assertEquals(numberOfColumns, reader.fileData.size());
    }

    public void testOpenXslxFile(){
        ExcelFileReader reader = new ExcelFileReader();
        LOGGER.info("Testing xlsx opening");
        reader.selectFileOpener(testFilesPath + "test_sample.xlsx");
        assertEquals(numberOfColumns, reader.getColumns("test_sample").size());
    }

    public void testOpenTsvFile(){
        ExcelFileReader reader = new ExcelFileReader();
        LOGGER.info("Testing tsv opening");
        reader.selectFileOpener(testFilesPath + "test_sample.tsv");
        assertEquals(numberOfColumns, reader.fileData.size());
    }


    public void testUniprotIdFetching(){
        UniprotMapper mapper = new UniprotMapper();

        LOGGER.info("Testing uniprot classic search");
        String classicSearch = mapper.fetchUniprotResults("P05067", "9606", "UniprotKb");
        assertEquals("P05067", classicSearch);

        LOGGER.info("Testing uniprot null search");
        String nullSearch = mapper.fetchUniprotResults("null", "9606", "ENSEMBL");
        assertEquals("null", nullSearch);

        LOGGER.info("Testing uniprot update id search");
        String updateIdSearch = mapper.fetchUniprotResults("Q9BT38", "9606", "UniprotKb");
        assertEquals("P05067", updateIdSearch);
    }

    public void testParticipantCreation(){
        ExcelFileReader reader = new ExcelFileReader();
        UniprotMapperGui uniprotMapperGui = new UniprotMapperGui(reader);
        Map<String, Integer> columnAndIndex = new HashMap<>();

        reader.selectFileOpener(testFilesPath + "test_sample.tsv");

        InteractionsCreator interactionsCreator = new InteractionsCreator(reader, uniprotMapperGui, columnAndIndex);


    }

    public Map<String, Integer> mockColumnAndIndex() {
        Map<String, Integer> columnAndIndex = new HashMap<>();

        columnAndIndex.put(DataTypeAndColumn.INTERACTION_NUMBER.name, 5);
        columnAndIndex.put(DataTypeAndColumn.INTERACTION_TYPE.name, 13);
        columnAndIndex.put(DataTypeAndColumn.INTERACTION_DETECTION_METHOD.name, 1);
        columnAndIndex.put(DataTypeAndColumn.HOST_ORGANISM.name, 3);

        columnAndIndex.put(DataTypeAndColumn.PARTICIPANT_NAME.name, 10);
        columnAndIndex.put(DataTypeAndColumn.PARTICIPANT_ID.name, 8);
        columnAndIndex.put(DataTypeAndColumn.PARTICIPANT_ID_DB.name, 9);
        columnAndIndex.put(DataTypeAndColumn.PARTICIPANT_TYPE.name, 13);
        columnAndIndex.put(DataTypeAndColumn.PARTICIPANT_ORGANISM.name, 16);
        columnAndIndex.put(DataTypeAndColumn.EXPERIMENTAL_ROLE.name, 11);
        columnAndIndex.put(DataTypeAndColumn.PARTICIPANT_IDENTIFICATION_METHOD.name, 2);
        columnAndIndex.put(DataTypeAndColumn.PARTICIPANT_XREF.name, 15);

        columnAndIndex.put(DataTypeAndColumn.FEATURE_SHORT_LABEL.name, 20);
        columnAndIndex.put(DataTypeAndColumn.FEATURE_TYPE.name, 21);
        columnAndIndex.put(DataTypeAndColumn.FEATURE_START_STATUS.name, 22);
        columnAndIndex.put(DataTypeAndColumn.FEATURE_END_STATUS.name, 23);

        return columnAndIndex;
    }

}
