package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.*;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.xml.model.extension.xml300.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapperGui;
import java.util.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.XmlMakerUtils;
import java.util.stream.Collectors;


public class InteractionsCreator {
    final ExcelFileReader excelFileReader;
    final UniprotMapperGui uniprotMapperGui;
    final XmlMakerUtils utils = new XmlMakerUtils();

    final List<XmlParticipantEvidence> xmlParticipants = new ArrayList<>();
    final List<XmlInteractionEvidence> xmlModelledInteractions = new ArrayList<>();
    final Map<String, Integer> columnAndIndex;
    final int sheetSelected;

    @Setter @Getter
    public String publicationId;

    final List<Map<String, String>> dataList = new ArrayList<>();

    public InteractionsCreator(ExcelFileReader reader, UniprotMapperGui uniprotMapperGui, Map<String, Integer> columnAndIndex, int sheetSelected) {
        this.excelFileReader = reader;
        this.uniprotMapperGui = uniprotMapperGui;
        this.columnAndIndex = columnAndIndex;
        this.sheetSelected = sheetSelected;
        this.publicationId = excelFileReader.publicationId;
    }

    public void createParticipantsWithFileFormat(Map<String, Integer> columnAndIndex){
        xmlParticipants.clear();
        xmlModelledInteractions.clear();
        dataList.clear();

        excelFileReader.selectFileOpener(excelFileReader.currentFilePath);
        if (excelFileReader.workbook == null){
            fetchDataFileWithSeparator(columnAndIndex);
        } else {
            fetchDataWithWorkbook(columnAndIndex);
        }
        createGroups();
        createInteractions();
    }

    public XmlParticipantEvidence createParticipant(Map<String, String> data) {
        String name = data.get(DataTypeAndColumn.PARTICIPANT_NAME.name);

        String participantType = data.get(DataTypeAndColumn.PARTICIPANT_TYPE.name);
        String participantTypeMiId = utils.fetchMiId(participantType);
        CvTerm type = new XmlCvTerm(participantType, participantTypeMiId);

        String participantOrganism = data.get(DataTypeAndColumn.PARTICIPANT_ORGANISM.name);
        Organism organism = new XmlOrganism(Integer.parseInt(utils.fetchTaxIdForOrganism(participantOrganism)));

        String participantId = data.get(DataTypeAndColumn.PARTICIPANT_ID.name);
        String participantIdDb = data.get(DataTypeAndColumn.PARTICIPANT_ID_DB.name);
        String participantIdDbMiId = utils.fetchMiId(participantIdDb);
        Xref uniqueId = new XmlXref(new XmlCvTerm(participantIdDb, participantIdDbMiId), participantId);

        Interactor participant = new XmlPolymer(name, type, organism, uniqueId);
        XmlParticipantEvidence participantEvidence = new XmlParticipantEvidence(participant);

        String experimentalRole = data.get(DataTypeAndColumn.EXPERIMENTAL_ROLE.name);
        String experimentalRoleMiId = utils.fetchMiId(experimentalRole);
        CvTerm experimentalRoleCv = new XmlCvTerm(experimentalRole, experimentalRoleMiId);
        participantEvidence.setExperimentalRole(experimentalRoleCv);

        String featureShortLabel = data.get(DataTypeAndColumn.FEATURE_SHORT_LABEL.name);
        String featureType = data.get(DataTypeAndColumn.FEATURE_TYPE.name);
        String featureTypeMiId = utils.fetchMiId(featureType);
        CvTerm featureTypeCv = new XmlCvTerm(featureType, featureTypeMiId);
        XmlFeatureEvidence featureEvidence = new XmlFeatureEvidence(featureTypeCv);
        featureEvidence.setShortName(featureShortLabel);

        XmlRange featureRange = new XmlRange();
        String featureStartRange = data.get(DataTypeAndColumn.FEATURE_START_STATUS.name);
        String featureStartRangeMiId = utils.fetchMiId(featureStartRange);
        XmlCvTerm featureStartRangeCv = new XmlCvTerm(featureStartRange, featureStartRangeMiId);
        featureRange.setJAXBStartStatus(featureStartRangeCv);

        String featureEndRange = data.get(DataTypeAndColumn.FEATURE_END_STATUS.name);
        String featureEndRangeMiId = utils.fetchMiId(featureEndRange);
        XmlCvTerm featureEndRangeCv = new XmlCvTerm(featureEndRange, featureEndRangeMiId);
        featureRange.setJAXBEndStatus(featureEndRangeCv);

        featureEvidence.setJAXBRangeWrapper(new AbstractXmlFeature.JAXBRangeWrapper());
        featureEvidence.getRanges().add(featureRange);
        participantEvidence.addFeature(featureEvidence);

        String experimentalPreparation = data.get(DataTypeAndColumn.EXPERIMENTAL_PREPARATION.name);
        String experimentalPreparationMiId = utils.fetchMiId(experimentalPreparation);
        CvTerm experimentalPreparationCv = new XmlCvTerm(experimentalPreparation, experimentalPreparationMiId);
        participantEvidence.getExperimentalPreparations().add(experimentalPreparationCv);

        //TODO: FETCH XREFS
//        CvTerm xrefCv = new XmlCvTerm("test", "MI0123");
//        XmlXref xref = new XmlXref(xrefCv, "idTEST", "MI0123");
//        participantEvidence.getXrefs().add(xref);

        String participantIdentificationMethod = data.get(DataTypeAndColumn.PARTICIPANT_IDENTIFICATION_METHOD.name);
        String participantIdentificationMethodMiId = utils.fetchMiId(participantIdentificationMethod);
        CvTerm participantIdentificationMethodCv = new XmlCvTerm(participantIdentificationMethod, participantIdentificationMethodMiId);
        participantEvidence.getIdentificationMethods().add(participantIdentificationMethodCv);

        return participantEvidence;
    }

    public void fetchDataFileWithSeparator(Map<String, Integer> columnAndIndex) {
        List<List<String>> data = excelFileReader.readFileWithSeparator();
        for (List<String> datum : data) {
            Map<String, String> dataMap = new HashMap<>();
            for (DataTypeAndColumn column : DataTypeAndColumn.values()) {
                dataMap.put(column.name, datum.get(columnAndIndex.get(column.name)));
            }
            dataList.add(dataMap);
        }
    }

    public void fetchDataWithWorkbook(Map<String, Integer> columnAndIndex) {
        Workbook workbook = excelFileReader.workbook;
        Sheet sheet = workbook.getSheetAt(sheetSelected);

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            //TODO: handle if it is string or numeric values

            Map<String, String> dataMap = new HashMap<>();
            for (DataTypeAndColumn value : DataTypeAndColumn.values()) {
                dataMap.put(value.name, value.extractString.apply(row.getCell(columnAndIndex.get(value.name))));
            }
            dataList.add(dataMap);
        }
    }

    public Map<String, List<Map<String, String>>> createGroups(){
        return dataList.stream().collect(Collectors.groupingBy(participant ->
                participant.get(DataTypeAndColumn.INTERACTION_NUMBER.name)));
    }

    public void createInteractions(){
        Map<String, List<Map<String, String>>> groups = createGroups();

        for (Map.Entry<String, List<Map<String, String>>> group : groups.entrySet()) {
            XmlInteractionEvidence interaction = new XmlInteractionEvidence();

            String interactionDetectionMethod = "detectionMethod";
            String participantIdentificationMethod = "participantIdentificationMethod";
            String hostOrganism = "hostOrganism";
            String interactionType = "interactionType";

            for (Map<String, String> participant : group.getValue()) {
                XmlParticipantEvidence newParticipant = createParticipant(participant);
                interaction.addParticipant(newParticipant);

                interactionDetectionMethod = participant.get(DataTypeAndColumn.INTERACTION_DETECTION_METHOD.name);
                participantIdentificationMethod = participant.get(DataTypeAndColumn.PARTICIPANT_IDENTIFICATION_METHOD.name);
                hostOrganism = participant.get(DataTypeAndColumn.HOST_ORGANISM.name);
                interactionType = participant.get(DataTypeAndColumn.INTERACTION_TYPE.name);

            }

            String interactionTypeMiId = utils.fetchMiId(interactionType);
            CvTerm interactionTypeCv = new XmlCvTerm(interactionType, interactionTypeMiId);
            interaction.setInteractionType(interactionTypeCv);

            int hostOrganismInt = Integer.parseInt(utils.fetchTaxIdForOrganism(hostOrganism));
            Organism organism = new XmlOrganism(hostOrganismInt);

            String interactionDetectionMiId = utils.fetchMiId(interactionDetectionMethod);
            CvTerm detectionMethod = new XmlCvTerm(interactionDetectionMethod, interactionDetectionMiId);
            Publication publication = new BibRef(excelFileReader.getPublicationId());
            XmlExperiment experiment = new XmlExperiment(publication, detectionMethod, organism);

            String identificationMethodMiId = utils.fetchMiId(participantIdentificationMethod);
            CvTerm identificationMethodCv = new XmlCvTerm(participantIdentificationMethod, identificationMethodMiId);

            experiment.setParticipantIdentificationMethod(identificationMethodCv);

            interaction.setExperiment(experiment);
            xmlModelledInteractions.add(interaction);
        }
    }

}
