package uk.ac.ebi.intact.psi.mi.xmlmaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.FileFormater;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.DataTypeAndColumn;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.InteractionWriter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.creators.InteractionsCreator;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotGeneralMapper;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotResult;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class XmlMakerGuiTest {
    private static final Logger LOGGER = Logger.getLogger(XmlMakerGuiTest.class.getName());
    private static final String TEST_FILE_PATH = "src/test/java/uk/ac/ebi/intact/psi/mi/xmlmaker/testSamples/";
    private static final int EXPECTED_COLUMN_COUNT = 24;
    private static final String INTERACTION_ID_ATTRIBUTE = "interaction id=";

    private ExcelFileReader reader;
    private InteractionsCreator interactionsCreator;
    private InteractionWriter interactionWriter;

    @BeforeEach
    public void setUp() {
        reader = new ExcelFileReader();
        interactionWriter = new InteractionWriter(reader);
        interactionsCreator = new InteractionsCreator(reader, interactionWriter, mockColumnAndIndex());
    }

    @Test
    public void testOpenCsvFile_fileHasExpectedColumnCount() {
        testFileOpeningWithExpectedColumns("test_sample.csv");
    }

    @Test
    public void testOpenXslxFile_fileHasExpectedColumnCount() {
        LOGGER.info("Testing file opening: " + "test_sample.xlsx");
        reader.selectFileOpener(TEST_FILE_PATH + "test_sample.xlsx");
        assertEquals(EXPECTED_COLUMN_COUNT, reader.getColumns("test_sample").size());
    }

    @Test
    public void testOpenTsvFile_fileHasExpectedColumnCount() {
        testFileOpeningWithExpectedColumns("test_sample.tsv");
    }

    private void testFileOpeningWithExpectedColumns(String fileName) {
        LOGGER.info("Testing file opening: " + fileName);
        reader.selectFileOpener(TEST_FILE_PATH + fileName);
        assertEquals(EXPECTED_COLUMN_COUNT, reader.fileData.size());
    }

    @Test
    public void testUniprotIdFetching_returnExpectedUniprotId() {
        UniprotGeneralMapper genMapper = new UniprotGeneralMapper();

        LOGGER.info("Testing Uniprot classic search");
        assertEquals("P05067", genMapper.fetchUniprotResult("P05067", null, null).get(0).getUniprotAc());

        LOGGER.info("Testing Uniprot null search");
        assertTrue(genMapper.fetchUniprotResult("", null, null).isEmpty());

         LOGGER.info("Testing Uniprot updated ID search");
         assertEquals("P05067", genMapper.fetchUniprotResult("Q9BT38", null, null).get(0).getUniprotAc());
    }

    @Test
    public void testInteractionsCreation_Tsv_ParseAndCountInteractionIds() throws Exception {
        testInteractionsCreation("test_sample.tsv");
    }

    @Test
    public void testInteractionsCreation_workbook_ParseAndCountInteractionIds() throws Exception {
        testInteractionsCreation("test_sample.xlsx");
    }

    private void testInteractionsCreation(String fileName) throws Exception {
        LOGGER.info("Testing interactions creation for file: " + fileName);

        reader.selectFileOpener(TEST_FILE_PATH + fileName);
        reader.publicationId = "1234";


        interactionWriter.setName(fileName);
        interactionWriter.setSaveLocation(TEST_FILE_PATH);

        interactionsCreator.setSheetSelected("test_sample");
        interactionsCreator.createParticipantsWithFileFormat();

        File writtenFile = new File(TEST_FILE_PATH + "test_sample/test_sample_13.xml");
        assertTrue(writtenFile.exists(), "XML file should exist");
        assertTrue(writtenFile.length() > 0, "XML file should not be empty");

        int interactionIdCount = countOccurrencesInFile(writtenFile);
        assertEquals(13, interactionIdCount, "Number of 'interaction id=' occurrences should match expected interaction count");
    }

    private int countOccurrencesInFile(File file) throws IOException {
        int count = 0;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                count += (line.split(XmlMakerGuiTest.INTERACTION_ID_ATTRIBUTE, -1).length - 1);
            }
        }
        return count;
    }

    @Test
    public void testMiFetching_returnExpectedMiId() {
        LOGGER.info("Testing MI fetching");
        assertEquals("MI:1097", XmlMakerUtils.fetchMiId("uniprot"));
    }

    @Test
    public void testFileWriting_CreateAndWriteXmlFile() throws Exception {
        LOGGER.info("Testing XML file writing");
        testInteractionsCreation("test_sample.tsv");
    }

    private Map<String, Integer> mockColumnAndIndex() {
        Map<String, Integer> columnAndIndex = new HashMap<>();

        columnAndIndex.put(DataTypeAndColumn.INTERACTION_NUMBER.name, 5);
        columnAndIndex.put(DataTypeAndColumn.INTERACTION_TYPE.name, 13);
        columnAndIndex.put(DataTypeAndColumn.INTERACTION_DETECTION_METHOD.name, 1);
        columnAndIndex.put(DataTypeAndColumn.HOST_ORGANISM.name, 3);
        columnAndIndex.put(DataTypeAndColumn.EXPERIMENTAL_PREPARATION.name, 13);

        columnAndIndex.put(DataTypeAndColumn.PARTICIPANT_NAME.name, 10);
        columnAndIndex.put(DataTypeAndColumn.PARTICIPANT_ID.name, 8);
        columnAndIndex.put(DataTypeAndColumn.PARTICIPANT_ID_DB.name, 9);
        columnAndIndex.put(DataTypeAndColumn.PARTICIPANT_TYPE.name, 13);
        columnAndIndex.put(DataTypeAndColumn.PARTICIPANT_ORGANISM.name, 16);
        columnAndIndex.put(DataTypeAndColumn.EXPERIMENTAL_ROLE.name, 11);
        columnAndIndex.put(DataTypeAndColumn.PARTICIPANT_IDENTIFICATION_METHOD.name, 2);
        columnAndIndex.put(DataTypeAndColumn.PARTICIPANT_XREF.name, 15);
        columnAndIndex.put(DataTypeAndColumn.PARTICIPANT_XREF_DB.name, 16);

//        columnAndIndex.put(DataTypeAndColumn.FEATURE_SHORT_LABEL.name, 20);
        columnAndIndex.put(DataTypeAndColumn.FEATURE_TYPE.name, 21);
        columnAndIndex.put(DataTypeAndColumn.FEATURE_START.name, 22);
        columnAndIndex.put(DataTypeAndColumn.FEATURE_END.name, 23);

        return columnAndIndex;
    }

    @Test
    public void testUniprotGeneralMapper(){
        UniprotGeneralMapper mapper = new UniprotGeneralMapper();
        ArrayList<UniprotResult> results =  mapper.fetchUniprotResult("222389", null, null);
        assertEquals(25, results.size());
    }

    @Test
    public void testFormaterCsv(){
        ExcelFileReader reader = new ExcelFileReader();
        reader.selectFileOpener(TEST_FILE_PATH + "Book1.csv");
        FileFormater formater = new FileFormater(reader);
        formater.selectFileFormater(TEST_FILE_PATH + "Book1.csv", 0, 2, 1, 3, "", false);

        File writtenFile = new File(TEST_FILE_PATH + "Book1_xmlMakerFormatted.csv");
        assertTrue(writtenFile.exists(), "Formatted file should exist");
        assertTrue(writtenFile.length() > 0, "Formatted file should not be empty");
        writtenFile.delete();
    }

    @Test
    public void testFormaterExcel(){
        ExcelFileReader reader = new ExcelFileReader();
        reader.selectFileOpener(TEST_FILE_PATH + "Book1.xlsx");
        FileFormater formater = new FileFormater(reader);
        formater.selectFileFormater(TEST_FILE_PATH + "Book1.xlsx", 0, 2, 1, 3, "Book1", false);
        File writtenFile = new File(TEST_FILE_PATH + "Book1_xmlMakerFormatted.xlsx");
        assertTrue(writtenFile.exists(), "Formatted file should exist");
        assertTrue(writtenFile.length() > 0, "Formatted file should not be empty");
        writtenFile.delete();
    }
}

