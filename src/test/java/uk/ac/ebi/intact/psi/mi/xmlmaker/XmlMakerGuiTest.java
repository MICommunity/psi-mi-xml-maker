package uk.ac.ebi.intact.psi.mi.xmlmaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.FileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.FileFormater;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.content.DataTypeAndColumn;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.XmlFileWriter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.creators.XmlInteractionsCreator;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotGeneralMapper;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.UniprotResult;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.XmlMakerUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class XmlMakerGuiTest {
    private static final Logger LOGGER = Logger.getLogger(XmlMakerGuiTest.class.getName());
    private static final String TEST_FILE_PATH = "src/test/java/uk/ac/ebi/intact/psi/mi/xmlmaker/testSamples/";
    private static int EXPECTED_COLUMN_COUNT = 0;
    private static final String INTERACTION_ID_ATTRIBUTE = "interaction id=";
    private static final int EXPECTED_INTERACTION_COUNT = 5;

    private FileReader reader;
    private XmlInteractionsCreator xmlInteractionsCreator;
    private XmlFileWriter xmlFileWriter;

    @BeforeEach
    public void setUp() {
        reader = new FileReader();
        xmlFileWriter = new XmlFileWriter(reader);
        xmlInteractionsCreator = new XmlInteractionsCreator(reader, xmlFileWriter, mockColumnAndIndex());
        int numberOfInitialData = DataTypeAndColumn.getInitialData().size();
        int numberOfNotInitialData = DataTypeAndColumn.getNotInitialData().size();
        int numberOfFeatures = 2;

        EXPECTED_COLUMN_COUNT = numberOfInitialData +  (numberOfNotInitialData * numberOfFeatures);
    }

    @Test
    public void testOpenCsvFile_fileHasExpectedColumnCount() {
        testFileOpeningWithExpectedColumns("test_sample.csv");
    }

    @Test
    public void testOpenXslxFile_fileHasExpectedColumnCount() {
        LOGGER.info("Testing file opening: " + "test_sample.xlsx");
        reader.selectFileOpener(TEST_FILE_PATH + "test_sample.xlsx");

        assertEquals(EXPECTED_COLUMN_COUNT, reader.getColumns("Formatted data").size());
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
    public void testInteractionsCreation_Csv_ParseAndCountInteractionIds() throws Exception {
        testInteractionsCreation("test_sample.csv");
    }

    @Test
    public void testInteractionsCreation_workbook_ParseAndCountInteractionIds() throws Exception {
        testInteractionsCreation("test_sample.xlsx");
    }

    private void testInteractionsCreation(String fileName) throws Exception {
        LOGGER.info("Testing interactions creation for file: " + fileName);

        reader.selectFileOpener(TEST_FILE_PATH + fileName);
        reader.setPublicationId("1234");
        reader.setPublicationDb("pubmed");

        xmlFileWriter.setName(fileName);
        xmlFileWriter.setSaveLocation(TEST_FILE_PATH);

        xmlInteractionsCreator.setSheetSelected("Formatted data");
        xmlInteractionsCreator.createParticipantsWithFileFormat();

        File writtenFile = new File(TEST_FILE_PATH + "test_sample/test_sample_0.xml");
        assertTrue(writtenFile.exists(), "XML file should exist");
        assertTrue(writtenFile.length() > 0, "XML file should not be empty");

        int interactionIdCount = countOccurrencesInFile(writtenFile);
        assertEquals(EXPECTED_INTERACTION_COUNT, interactionIdCount, "Number of 'interaction id=' occurrences should match expected interaction count");
    }

    private int countOccurrencesInFile(File file) throws IOException {
        int count = 0;
        try (BufferedReader bufferedReader = new BufferedReader(new java.io.FileReader(file))) {
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
        for (int i = 0; i < DataTypeAndColumn.values().length; i++) {
            columnAndIndex.put(DataTypeAndColumn.values()[i].name, i);
        }
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
        FileReader reader = new FileReader();
        reader.selectFileOpener(TEST_FILE_PATH + "Book1.csv");
        FileFormater formater = new FileFormater(reader);
        formater.selectFileFormater(TEST_FILE_PATH + "Book1.csv", 0, 2, 1, 3, "", false);

        File writtenFile = new File(TEST_FILE_PATH + "Book1_xmlMakerFormatted.csv");
        assertTrue(writtenFile.exists(), "Formatted file should exist");
        assertTrue(writtenFile.length() > 0, "Formatted file should not be empty");
        boolean deleted = writtenFile.delete();
    }

    @Test
    public void testFormaterExcel(){
        FileReader reader = new FileReader();
        reader.selectFileOpener(TEST_FILE_PATH + "Book1.xlsx");
        FileFormater formater = new FileFormater(reader);
        formater.selectFileFormater(TEST_FILE_PATH + "Book1.xlsx", 0, 2, 1, 3, "Book1", false);
        File writtenFile = new File(TEST_FILE_PATH + "Book1_xmlMakerFormatted.xlsx");
        assertTrue(writtenFile.exists(), "Formatted file should exist");
        assertTrue(writtenFile.length() > 0, "Formatted file should not be empty");
        boolean deleted = writtenFile.delete();
    }

    @Test
    public void testFetchFromFile(){
        FileReader reader = new FileReader();
        reader.selectFileOpener(TEST_FILE_PATH + "Book1.xlsx");
        FileFormater formater = new FileFormater(reader);
        reader.setSheetSelectedUpdate("Book1");
        formater.formatExcelFile(0, 2, "Book1", false, 1, 3);
        List<Map<String, String>> participants = formater.getParticipants();

        Map<String, String> participant = participants.get(0);

        String result = formater.getValueFromFile(DataTypeAndColumn.PARTICIPANT_ID.name, participant);

        assertEquals("100;", result);

        File writtenFile = new File(TEST_FILE_PATH + "Book1_xmlMakerFormatted.xlsx");
        boolean deleted = writtenFile.delete();
    }

}

