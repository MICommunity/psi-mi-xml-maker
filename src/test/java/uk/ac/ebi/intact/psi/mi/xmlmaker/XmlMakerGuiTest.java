package uk.ac.ebi.intact.psi.mi.xmlmaker;

import junit.framework.TestCase;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml.DataTypeAndColumn;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml.InteractionWriter;
import uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml.InteractionsCreator;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapper;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapperGui;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The XmlMakerGuiTest class is used to test different functions of the XML maker.
 * It takes in input different similar files saved under different formats. Those files
 * contain lines with missing cells to check the good processing of those.
 */
public class XmlMakerGuiTest extends TestCase {
    private static final Logger LOGGER = Logger.getLogger(XmlMakerGuiTest.class.getName());
    private static final String TEST_FILE_PATH = "src/test/java/uk/ac/ebi/intact/psi/mi/xmlmaker/testSamples/";
    private final Map<String, Integer> COLUMN_AND_INDEX = mockColumnAndIndex();
    private final int numberOfColumns = 24;

    private ExcelFileReader reader;
    private InteractionsCreator interactionsCreator;

    @Override
    protected void setUp() throws Exception {
        LoadingSpinner loadingSpinner = new LoadingSpinner();
        super.setUp();
        reader = new ExcelFileReader();
        UniprotMapperGui uniprotMapperGui = new UniprotMapperGui(reader, loadingSpinner);
        interactionsCreator = new InteractionsCreator(reader, uniprotMapperGui, COLUMN_AND_INDEX);
    }

    public void testOpenCsvFile_fileHasExpectedColumnCount(){
        ExcelFileReader reader = new ExcelFileReader();
        LOGGER.info("Testing csv opening");
        reader.selectFileOpener(TEST_FILE_PATH + "test_sample.csv");
        assertEquals(numberOfColumns, reader.fileData.size());
    }

    public void testOpenXslxFile_fileHasExpectedColumnCount(){
        ExcelFileReader reader = new ExcelFileReader();
        LOGGER.info("Testing xlsx opening");
        reader.selectFileOpener(TEST_FILE_PATH + "test_sample.xlsx");
        assertEquals(numberOfColumns, reader.getColumns("test_sample").size());
    }

    public void testOpenTsvFile_fileHasExpectedColumnCount(){
        ExcelFileReader reader = new ExcelFileReader();
        LOGGER.info("Testing tsv opening");
        reader.selectFileOpener(TEST_FILE_PATH + "test_sample.tsv");
        assertEquals(numberOfColumns, reader.fileData.size());
    }

    public void testUniprotIdFetching_returnExpectedUniprotId(){
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

    public void testInteractionsCreation_returnExpectedInteractionNumber() throws Exception {
        LOGGER.info("Testing participant creation");
        setUp();

        reader.selectFileOpener(TEST_FILE_PATH + "test_sample.tsv");
        interactionsCreator.createParticipantsWithFileFormat(COLUMN_AND_INDEX);

        assertEquals(13,interactionsCreator.xmlModelledInteractions.size());
    }

    public void testMiFetching_returnExpectedMiId() {
        LOGGER.info("Testing MI fetching");
        XmlMakerUtils utils = new XmlMakerUtils();
        String fetched = utils.fetchMiId("uniprot");
        assertEquals("MI:1097", fetched);
    }

    public void testFetchingDataWithSeparator_returnExpectedDataSize() throws Exception {
        LOGGER.info("Test fetching data with separator");
        setUp();

        reader.selectFileOpener(TEST_FILE_PATH + "test_sample.tsv");
        interactionsCreator.fetchDataFileWithSeparator(COLUMN_AND_INDEX);
        int dataListSize = interactionsCreator.dataList.size();
        assertEquals(24, dataListSize);
    }

    public void testFetchingDataWithWorkbook_returnExpectedDataSize() throws Exception {
        LOGGER.info("Test fetching data with workbook");
        setUp();

        reader.selectFileOpener(TEST_FILE_PATH + "test_sample.xlsx");
        interactionsCreator.sheetSelected = "test_sample";
        interactionsCreator.fetchDataWithWorkbook(COLUMN_AND_INDEX);
        int interactionsSize = interactionsCreator.xmlModelledInteractions.size();
        assertEquals(24, interactionsSize);
    }

    public void testFileWriting_CreateAndWriteXmlFile() throws Exception {
        LOGGER.info("Test writing xml file");
        setUp();
        InteractionWriter interactionWriter = new InteractionWriter(interactionsCreator, reader);

        reader.selectFileOpener(TEST_FILE_PATH + "test_sample.tsv");
        interactionsCreator.createParticipantsWithFileFormat(COLUMN_AND_INDEX);
        reader.publicationId = "1234";
        interactionWriter.interactionsWriter(TEST_FILE_PATH + "file_writing_test");
    }

    public Map<String, Integer> mockColumnAndIndex() {
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

        columnAndIndex.put(DataTypeAndColumn.FEATURE_SHORT_LABEL.name, 20);
        columnAndIndex.put(DataTypeAndColumn.FEATURE_TYPE.name, 21);
        columnAndIndex.put(DataTypeAndColumn.FEATURE_START_STATUS.name, 22);
        columnAndIndex.put(DataTypeAndColumn.FEATURE_END_STATUS.name, 23);

        return columnAndIndex;
    }

}
