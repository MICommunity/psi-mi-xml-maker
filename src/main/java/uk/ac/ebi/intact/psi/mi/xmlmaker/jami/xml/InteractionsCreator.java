package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.*;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.xml.model.extension.xml300.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.organisms.OrganismSelector;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapperGui;
import java.util.*;
import uk.ac.ebi.pride.utilities.ols.web.service.client.OLSClient;
import uk.ac.ebi.pride.utilities.ols.web.service.config.OLSWsConfig;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;
import uk.ac.ebi.intact.psi.mi.xmlmaker.XmlMakerUtils;
import java.util.stream.Collectors;


public class InteractionsCreator {
    final ExcelFileReader excelFileReader;
    final UniprotMapperGui uniprotMapperGui;
    final OrganismSelector organismSelector;
    final XmlMakerUtils utils = new XmlMakerUtils();

    final ArrayList<InteractionWithIndexes> interactionWithIndexes = new ArrayList<>();
    final ArrayList<XmlParticipantEvidence> xmlParticipants = new ArrayList<>();
    final ArrayList<XmlInteractionEvidence> xmlModelledInteractions = new ArrayList<>();
    static final OLSClient olsClient = new OLSClient(new OLSWsConfig());
    final Map<String, Integer> columnAndIndex;
    final int sheetSelected;

    @Setter @Getter
    public String publicationId;

    List<Map<String, String>> dataList = new ArrayList<>();

    public InteractionsCreator(ExcelFileReader reader, UniprotMapperGui uniprotMapperGui,
                               OrganismSelector organismSelector, Map<String, Integer> columnAndIndex, int sheetSelected) {
        this.excelFileReader = reader;
        this.uniprotMapperGui = uniprotMapperGui;
        this.organismSelector = organismSelector;
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
        createGroupsUpdated();
        createInteractionUpdate();
    }

    public XmlParticipantEvidence createParticipant(Map<String, String> data) {
        String name = data.get(dataTypeAndColumn.PARTICIPANT_NAME.value);

        String participantType = data.get(dataTypeAndColumn.PARTICIPANT_TYPE.value);
        String participantTypeMiId = null;
        try {
            Term term = olsClient.getExactTermByName(participantType, "mi");
            participantTypeMiId = (term != null && term.getOboId() != null) ? term.getOboId().getIdentifier() : null;
        } catch (NullPointerException e) {
            System.err.println("Failed to retrieve MI ID for participantType: " + participantType);
        }
        XmlCvTerm type = new XmlCvTerm(participantType, participantTypeMiId);

        String participantOrganism = data.get(dataTypeAndColumn.PARTICIPANT_ORGANISM.value);
        XmlOrganism organism = new XmlOrganism(utils.findMostSimilarOrganism(participantOrganism));

        String participantId = data.get(dataTypeAndColumn.PARTICIPANT_ID.value);
        String participantIdDb = data.get(dataTypeAndColumn.PARTICIPANT_ID_DB.value);
        String participantIdDbMiId = null;
        if (participantIdDb != null && !participantIdDb.isEmpty()) {
            try {
                Term term = olsClient.getExactTermByName(participantIdDb, "mi");
                participantIdDbMiId = (term != null && term.getOboId() != null) ? term.getOboId().getIdentifier() : null;
            } catch (NullPointerException e) {
                System.err.println("Failed to retrieve MI ID for participantIdDb: " + participantIdDb);
            }
        }
        XmlXref uniqueId = new XmlXref(new XmlCvTerm(participantIdDb, participantIdDbMiId), participantId);

        XmlProtein protein = new XmlProtein(name, type, organism, uniqueId);
        XmlParticipantEvidence participantEvidence = new XmlParticipantEvidence(protein);

        String experimentalRole = data.get(dataTypeAndColumn.EXPERIMENTAL_ROLE.value);
        String experimentalRoleMiId = null;
        CvTerm bioRole = null;
        if (experimentalRole != null) {
            try {
                Term roleTerm = olsClient.getExactTermByName(experimentalRole, "mi");
                experimentalRoleMiId = (roleTerm != null && roleTerm.getOboId() != null) ? roleTerm.getOboId().getIdentifier() : null;
            } catch (NullPointerException e) {
                System.err.println("Failed to retrieve MI ID for experimentalRole: " + experimentalRole);
            }
        }
        if (experimentalRoleMiId != null) {
            bioRole = new XmlCvTerm(experimentalRole, experimentalRoleMiId);
        }
        if (bioRole != null) {
            participantEvidence.setExperimentalRole(bioRole);
        } else {
            System.err.println("bioRole is null; skipping setting experimental role on participant evidence.");
        }

        String featureShortLabel = data.get(dataTypeAndColumn.FEATURE_SHORT_LABEL.value);
        String featureType = data.get(dataTypeAndColumn.FEATURE_TYPE.value);
        String featureTypeMiId = null;
        try {
            Term term = olsClient.getExactTermByName(featureType, "mi");
            featureTypeMiId = (term != null && term.getOboId() != null) ? term.getOboId().getIdentifier() : null;
        } catch (NullPointerException e) {
            System.err.println("Failed to retrieve MI ID for featureType: " + featureType);
        }
        XmlCvTerm featureTypeCv = new XmlCvTerm(featureType, featureTypeMiId);
        XmlFeatureEvidence featureEvidence = new XmlFeatureEvidence(featureTypeCv);
        featureEvidence.setShortName(featureShortLabel);

        XmlRange featureRange = new XmlRange();
        String featureStartRange = data.get(dataTypeAndColumn.FEATURE_START_STATUS.value);
        String featureStartRangeMiId = null;
        try {
            Term term = olsClient.getExactTermByName(featureStartRange, "mi");
            featureStartRangeMiId = (term != null && term.getOboId() != null) ? term.getOboId().getIdentifier() : null;
        } catch (NullPointerException e) {
            System.err.println("Failed to retrieve MI ID for featureStartRange: " + featureStartRange);
        }
        XmlCvTerm featureStartRangeCv = new XmlCvTerm(featureStartRange, featureStartRangeMiId);
        featureRange.setJAXBStartStatus(featureStartRangeCv);

        String featureEndRange = data.get(dataTypeAndColumn.FEATURE_END_STATUS.value);
        String featureEndRangeMiId = null;
        try {
            Term term = olsClient.getExactTermByName(featureEndRange, "mi");
            featureEndRangeMiId = (term != null && term.getOboId() != null) ? term.getOboId().getIdentifier() : null;
        } catch (NullPointerException e) {
            System.err.println("Failed to retrieve MI ID for featureEndRange: " + featureEndRange);
        }
        XmlCvTerm featureEndRangeCv = new XmlCvTerm(featureEndRange, featureEndRangeMiId);
        featureRange.setJAXBEndStatus(featureEndRangeCv);

        featureEvidence.setJAXBRangeWrapper(new AbstractXmlFeature.JAXBRangeWrapper());
        featureEvidence.getRanges().add(featureRange);
        participantEvidence.addFeature(featureEvidence);

        String experimentalPreparation = data.get(dataTypeAndColumn.EXPERIMENTAL_PREPARATION.value);
        String experimentalPreparationMiId = null;
        try {
            Term term = olsClient.getExactTermByName(experimentalPreparation, "mi");
            experimentalPreparationMiId = (term != null && term.getOboId() != null) ? term.getOboId().getIdentifier() : null;
        } catch (NullPointerException e) {
            System.err.println("Failed to retrieve MI ID for experimentalPreparation: " + experimentalPreparation);
        }
        XmlCvTerm experimentalPreparationCv = new XmlCvTerm(experimentalPreparation, experimentalPreparationMiId);
        participantEvidence.getExperimentalPreparations().add(experimentalPreparationCv);

        return participantEvidence;
    }

    public void fetchDataFileWithSeparator(Map<String, Integer> columnAndIndex) {
        ArrayList<ArrayList<String>> data = excelFileReader.readFileWithSeparator();
        for (ArrayList<String> datum : data) {
            Map<String, String> dataMap = new HashMap<>();

            dataMap.put(dataTypeAndColumn.INTERACTION_NUMBER.value, datum.get(columnAndIndex.get(dataTypeAndColumn.INTERACTION_NUMBER.value)));
            dataMap.put(dataTypeAndColumn.INTERACTION_DETECTION_METHOD.value, datum.get(columnAndIndex.get(dataTypeAndColumn.INTERACTION_DETECTION_METHOD.value)));
            dataMap.put(dataTypeAndColumn.INTERACTION_TYPE.value, datum.get(columnAndIndex.get(dataTypeAndColumn.INTERACTION_TYPE.value)));
            dataMap.put(dataTypeAndColumn.HOST_ORGANISM.value, datum.get(columnAndIndex.get(dataTypeAndColumn.HOST_ORGANISM.value)));
            dataMap.put(dataTypeAndColumn.PARTICIPANT_IDENTIFICATION_METHOD.value, datum.get(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_IDENTIFICATION_METHOD.value)));
            dataMap.put(dataTypeAndColumn.PARTICIPANT_NAME.value, datum.get(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_NAME.value)));
            dataMap.put(dataTypeAndColumn.PARTICIPANT_TYPE.value, datum.get(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_TYPE.value)));
            dataMap.put(dataTypeAndColumn.PARTICIPANT_ORGANISM.value, datum.get(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_ORGANISM.value)));
            dataMap.put(dataTypeAndColumn.PARTICIPANT_ID.value, datum.get(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_ID.value)));
            dataMap.put(dataTypeAndColumn.PARTICIPANT_ID_DB.value, datum.get(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_ID_DB.value)));
            dataMap.put(dataTypeAndColumn.EXPERIMENTAL_ROLE.value, datum.get(columnAndIndex.get(dataTypeAndColumn.EXPERIMENTAL_ROLE.value)));
            dataMap.put(dataTypeAndColumn.FEATURE_SHORT_LABEL.value, datum.get(columnAndIndex.get(dataTypeAndColumn.FEATURE_SHORT_LABEL.value)));
            dataMap.put(dataTypeAndColumn.FEATURE_TYPE.value, datum.get(columnAndIndex.get(dataTypeAndColumn.FEATURE_TYPE.value)));
            dataMap.put(dataTypeAndColumn.FEATURE_START_STATUS.value, datum.get(columnAndIndex.get(dataTypeAndColumn.FEATURE_START_STATUS.value)));
            dataMap.put(dataTypeAndColumn.FEATURE_END_STATUS.value, datum.get(columnAndIndex.get(dataTypeAndColumn.FEATURE_END_STATUS.value)));
            dataMap.put(dataTypeAndColumn.EXPERIMENTAL_PREPARATION.value, datum.get(columnAndIndex.get(dataTypeAndColumn.EXPERIMENTAL_PREPARATION.value)));
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
            dataMap.put(dataTypeAndColumn.INTERACTION_NUMBER.value,
                    String.valueOf(row.getCell(columnAndIndex.get(dataTypeAndColumn.INTERACTION_NUMBER.value)).getNumericCellValue()));
            dataMap.put(dataTypeAndColumn.INTERACTION_DETECTION_METHOD.value,
                    row.getCell(columnAndIndex.get(dataTypeAndColumn.INTERACTION_DETECTION_METHOD.value)).getStringCellValue());
            dataMap.put(dataTypeAndColumn.INTERACTION_TYPE.value,
                    row.getCell(columnAndIndex.get(dataTypeAndColumn.INTERACTION_TYPE.value)).getStringCellValue());
            dataMap.put(dataTypeAndColumn.HOST_ORGANISM.value,
                    row.getCell(columnAndIndex.get(dataTypeAndColumn.HOST_ORGANISM.value)).getStringCellValue());
            dataMap.put(dataTypeAndColumn.PARTICIPANT_IDENTIFICATION_METHOD.value,
                    row.getCell(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_IDENTIFICATION_METHOD.value)).getStringCellValue());
            dataMap.put(dataTypeAndColumn.PARTICIPANT_NAME.value,
                    row.getCell(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_NAME.value)).getStringCellValue());
            dataMap.put(dataTypeAndColumn.PARTICIPANT_TYPE.value,
                    row.getCell(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_TYPE.value)).getStringCellValue());
            dataMap.put(dataTypeAndColumn.PARTICIPANT_ORGANISM.value,
                    row.getCell(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_ORGANISM.value)).getStringCellValue());
            dataMap.put(dataTypeAndColumn.PARTICIPANT_ID.value,
                    row.getCell(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_ID.value)).getStringCellValue());
            dataMap.put(dataTypeAndColumn.PARTICIPANT_ID_DB.value,
                    row.getCell(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_ID_DB.value)).getStringCellValue());
            dataMap.put(dataTypeAndColumn.EXPERIMENTAL_ROLE.value,
                    row.getCell(columnAndIndex.get(dataTypeAndColumn.EXPERIMENTAL_ROLE.value)).getStringCellValue());
            dataMap.put(dataTypeAndColumn.FEATURE_SHORT_LABEL.value,
                    row.getCell(columnAndIndex.get(dataTypeAndColumn.FEATURE_SHORT_LABEL.value)).getStringCellValue());
            dataMap.put(dataTypeAndColumn.FEATURE_TYPE.value,
                    row.getCell(columnAndIndex.get(dataTypeAndColumn.FEATURE_TYPE.value)).getStringCellValue());
            dataMap.put(dataTypeAndColumn.FEATURE_START_STATUS.value,
                    row.getCell(columnAndIndex.get(dataTypeAndColumn.FEATURE_START_STATUS.value)).getStringCellValue());
            dataMap.put(dataTypeAndColumn.FEATURE_END_STATUS.value,
                    row.getCell(columnAndIndex.get(dataTypeAndColumn.FEATURE_END_STATUS.value)).getStringCellValue());
            dataMap.put(dataTypeAndColumn.EXPERIMENTAL_PREPARATION.value,
                    row.getCell(columnAndIndex.get(dataTypeAndColumn.EXPERIMENTAL_PREPARATION.value)).getStringCellValue());

            dataList.add(dataMap);
        }
    }

    public Map<String, List<Map<String, String>>> createGroupsUpdated(){
        return dataList.stream().collect(Collectors.groupingBy(participant ->
                participant.get(dataTypeAndColumn.INTERACTION_NUMBER.value)));
    }

    public void createInteractionUpdate(){
        Map<String, List<Map<String, String>>> groups = createGroupsUpdated();

        for (Map.Entry<String, List<Map<String, String>>> group : groups.entrySet()) {
            XmlInteractionEvidence interaction = new XmlInteractionEvidence();

            String interactionDetectionMethod = "detectionMethod";
            String participantIdentificationMethod = "participantIdentificationMethod";
            String hostOrganism = "hostOrganism";
            String interactionType = "interactionType";

            for (Map<String, String> participant : group.getValue()) {
                XmlParticipantEvidence newParticipant = createParticipant(participant);
                interaction.addParticipant(newParticipant);

                interactionDetectionMethod = participant.get(dataTypeAndColumn.INTERACTION_DETECTION_METHOD.value);
                participantIdentificationMethod = participant.get(dataTypeAndColumn.PARTICIPANT_IDENTIFICATION_METHOD.value);
                hostOrganism = participant.get(dataTypeAndColumn.HOST_ORGANISM.value);
                interactionType = participant.get(dataTypeAndColumn.INTERACTION_TYPE.value);

            }

            String interactionTypeMiId = olsClient.getExactTermByName(interactionType, "mi").getOboId().
                    getIdentifier();
            CvTerm interactionTypeCv = new XmlCvTerm(interactionType, interactionTypeMiId);
            interaction.setInteractionType(interactionTypeCv);

            int hostOrganismInt = utils.findMostSimilarOrganism(hostOrganism);
            XmlOrganism organism = new XmlOrganism(hostOrganismInt);

            String interactionDetectionMiId = olsClient.getExactTermByName(interactionDetectionMethod, "mi").getOboId().
                    getIdentifier();
            XmlCvTerm detectionMethod = new XmlCvTerm(interactionDetectionMethod, interactionDetectionMiId);
            Publication publication = new BibRef(excelFileReader.getPublicationId());
            XmlExperiment experiment = new XmlExperiment(publication, detectionMethod, organism);

            String identificationMethodMiId = olsClient.getExactTermByName(participantIdentificationMethod, "mi").getOboId().
                    getIdentifier();
            XmlCvTerm identificationMethodCv = new XmlCvTerm(participantIdentificationMethod, identificationMethodMiId);

            experiment.setParticipantIdentificationMethod(identificationMethodCv); //TODO: not working
            experiment.getParticipantIdentificationMethod().setMIIdentifier(identificationMethodMiId);
            experiment.getParticipantIdentificationMethod().setFullName(participantIdentificationMethod);

            interaction.setExperiment(experiment);
            xmlModelledInteractions.add(interaction);
        }
    }
}
