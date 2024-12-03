package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

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

public class InteractionsCreator {
    ExcelFileReader excelFileReader;
    UniprotMapperGui uniprotMapperGui;
    OrganismSelector organismSelector;


    ArrayList<InteractionWithIndexes> interactionWithIndexes = new ArrayList<>();
    ArrayList<XmlParticipantEvidence> xmlParticipants = new ArrayList<>();
    ArrayList<XmlInteractionEvidence> xmlModelledInteractions = new ArrayList<>();
    static OLSClient olsClient = new OLSClient(new OLSWsConfig());
    Map<String, Integer> columnAndIndex;
    int sheetSelected;

    public InteractionsCreator(ExcelFileReader reader, UniprotMapperGui uniprotMapperGui,
                               OrganismSelector organismSelector, Map<String, Integer> columnAndIndex, int sheetSelected) {
        this.excelFileReader = reader;
        this.uniprotMapperGui = uniprotMapperGui;
        this.organismSelector = organismSelector;
        this.columnAndIndex = columnAndIndex;
        this.sheetSelected = sheetSelected;
    }

    public void createParticipantsWithFileFormat(Map<String, Integer> columnAndIndex, int sheetSelected){
        xmlParticipants.clear();
        xmlModelledInteractions.clear();
        excelFileReader.selectFileOpener(excelFileReader.currentFilePath);
        if (excelFileReader.workbook == null){
            createGroups(dataTypeAndColumn.INTERACTION_NUMBER.value);
            createParticipantsWithFileSeparator(columnAndIndex);
        } else {
            createGroupsWithWorkbook(dataTypeAndColumn.INTERACTION_NUMBER.value, sheetSelected);
            createParticipantsWithWorkbook(columnAndIndex, sheetSelected - 1);
        }
        createInteractions();
    }

    public void createGroups(String columnSelected) {
        int interactionNumberIndex = getInteractionNumberIndex(columnSelected);

        if (interactionNumberIndex == -1) {
            throw new IllegalStateException("Interaction number column not found in file.");
        }
        ArrayList<ArrayList<String>> data = excelFileReader.readFileWithSeparator();
        String currentInteractionNumber = null;
        Integer startIndex = null;
        int endIndex;

        for (int i = 1; i < data.size(); i++) { // start at 1 to skip header
            String interactionNumber = data.get(i).get(interactionNumberIndex);
            if (!Objects.equals(interactionNumber, currentInteractionNumber)) {
                if (startIndex != null) {
                    endIndex = i-1;
                    interactionWithIndexes.add(new InteractionWithIndexes(currentInteractionNumber, startIndex, endIndex));
                }
                currentInteractionNumber = interactionNumber;
                startIndex = i;
            }
        }
        if (startIndex != null) {
            interactionWithIndexes.add(new InteractionWithIndexes(currentInteractionNumber, startIndex, data.size()-1));
        }
    }

    public void createGroupsWithWorkbook(String columnSelected, int sheetSelected) {
        if (excelFileReader.workbook == null) {
            throw new IllegalStateException("Workbook not loaded. Please ensure an Excel file is opened.");
        }

        Sheet sheet = excelFileReader.workbook.getSheetAt(sheetSelected - 1);
        int interactionNumberIndex = getColumnIndexFromSheet(sheet, columnSelected);

        if (interactionNumberIndex == -1) {
            throw new IllegalStateException("Interaction number column not found in sheet.");
        }

        String currentInteractionNumber = null;
        Integer startIndex = null;
        int endIndex;

        for (int i = 1; i < sheet.getLastRowNum(); i++) { // Start at 1 to skip the header row
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Cell cell = row.getCell(interactionNumberIndex);
            if (cell == null) continue;

            String interactionNumber = cell.toString().trim();
            if (!Objects.equals(interactionNumber, currentInteractionNumber)) {
                if (startIndex != null) {
                    endIndex = i-1;
                    interactionWithIndexes.add(new InteractionWithIndexes(currentInteractionNumber, startIndex, endIndex));
                }
                currentInteractionNumber = interactionNumber;
                startIndex = i;
            }
        }
        if (startIndex != null) {
            interactionWithIndexes.add(new InteractionWithIndexes(currentInteractionNumber, startIndex, sheet.getLastRowNum()-1));
        }
    }

    private int getColumnIndexFromSheet(Sheet sheet, String columnName) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new IllegalStateException("Header row not found in the sheet.");
        }

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null && columnName.equals(cell.toString().trim())) {
                return i;
            }
        }
        return -1;
    }

    public int getInteractionNumberIndex(String columnSelected) {
        ArrayList<String> firstRow = excelFileReader.readFileWithSeparator().get(0);
        for (int i = 0; i < firstRow.size(); i++) {
            if (firstRow.get(i).equals(columnSelected)) {
                return i;
            }
        }
        return -1;
    }

    public void createParticipantsWithFileSeparator(Map<String, Integer> columnAndIndex) {
        ArrayList<ArrayList<String>> data = excelFileReader.readFileWithSeparator();

        for (ArrayList<String> datum : data) {
            String name = datum.get(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_NAME.value));
            String participantType = datum.get(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_TYPE.value));
            String participantTypeMiId = null;
            try {
                participantTypeMiId = olsClient.getExactTermByName(participantType, "mi") != null
                        ? olsClient.getExactTermByName(participantType, "mi").getOboId().getIdentifier()
                        : null;
            } catch (NullPointerException e) {
                System.err.println("Failed to retrieve MI ID for participantType: " + participantType);
            }
            XmlCvTerm type = new XmlCvTerm(participantType, participantTypeMiId);

            int participantOrganism = Integer.parseInt(organismSelector.getSelectedOrganism());
            XmlOrganism organism = new XmlOrganism(participantOrganism);

            String participantId = datum.get(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_ID.value));
            String participantIdDb = datum.get(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_ID_DB.value));

            String participantIdDbMiId = null;
            if (participantIdDb != null && !participantIdDb.isEmpty()) {
                try {
                    Term term = olsClient.getExactTermByName(participantIdDb, "mi");
                    if (term != null && term.getOboId() != null) {
                        participantIdDbMiId = term.getOboId().getIdentifier();
                    }
                } catch (NullPointerException e) {
                    System.err.println("Failed to retrieve MI ID for participantIdDb: " + participantIdDb);
                }
            }
            XmlXref uniqueId = new XmlXref(new XmlCvTerm(participantIdDb, participantIdDbMiId), participantId);

            XmlProtein protein = new XmlProtein(name, type, organism, uniqueId);
            XmlParticipantEvidence participantEvidence = new XmlParticipantEvidence(protein);

            String experimentalRole = datum.get(columnAndIndex.get(dataTypeAndColumn.EXPERIMENTAL_ROLE.value));
            String experimentalRoleMiId = null;
            CvTerm bioRole = null;
            if (experimentalRole != null) {
                try {
                    Term roleTerm = olsClient.getExactTermByName(experimentalRole, "mi");
                    if (roleTerm != null && roleTerm.getOboId() != null) {
                        experimentalRoleMiId = roleTerm.getOboId().getIdentifier();
                    }
                } catch (NullPointerException e) {
                    System.err.println("Failed to retrieve MI ID for experimentalRole: " + experimentalRole);
                }
            }

            if (experimentalRoleMiId != null) {
                bioRole = new XmlCvTerm(experimentalRole, experimentalRoleMiId);
            } else {
                System.err.println("Experimental role MI ID is null; defaulting to null bioRole.");
            }

            if (bioRole != null) {
                participantEvidence.setExperimentalRole(bioRole);
            } else {
                System.err.println("bioRole is null; skipping setting experimental role on participant evidence.");
            }

            xmlParticipants.add(participantEvidence);
        }
    }

    public void createParticipantsWithWorkbook(Map<String, Integer> columnAndIndex, int sheetSelected) {
        Workbook workbook = excelFileReader.workbook;
        for (int i = 1; i <= workbook.getSheetAt(sheetSelected).getLastRowNum(); i++) {
            Row row = workbook.getSheetAt(sheetSelected).getRow(i);

            String name = row.getCell(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_NAME.value)).getStringCellValue();

            String participantType = row.getCell(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_TYPE.value)).getStringCellValue();
            String participantTypeMiId = olsClient.getExactTermByName(participantType, "mi").getOboId().getIdentifier();
            XmlCvTerm type = new XmlCvTerm(participantType, participantTypeMiId);

            int participantOrganism = Integer.parseInt(organismSelector.getSelectedOrganism()); //TODO: go back to the column
            XmlOrganism organism = new XmlOrganism(participantOrganism);

            String participantId = row.getCell(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_ID.value)).getStringCellValue();
            String participantIdDb = row.getCell(columnAndIndex.get(dataTypeAndColumn.PARTICIPANT_ID_DB.value)).getStringCellValue();
            String participantIdDbMiId = olsClient.getExactTermByName(participantIdDb, "mi").getOboId().
                    getIdentifier();
            XmlXref uniqueId = new XmlXref(new XmlCvTerm(participantIdDb, participantIdDbMiId), participantId);

            XmlInteractor interactor = new XmlInteractor(name, type, organism, uniqueId);
            interactor.setShortName(name);

            XmlParticipantEvidence participantEvidence = new XmlParticipantEvidence(interactor);

//            String featureCell = row.getCell(columnAndIndex.get("Feature")).getStringCellValue();
//            XmlFeature feature = new XmlFeature();
//            if (feature){
//           participantEvidence.addFeature(); //TODO: add features
//            }

            String experimentalRole = row.getCell(columnAndIndex.get(dataTypeAndColumn.EXPERIMENTAL_ROLE.value)).getStringCellValue();
            String experimentalRoleMiId = olsClient.getExactTermByName(experimentalRole, "mi").getOboId().
                    getIdentifier();
            CvTerm experimentalRoleCvTerm = new XmlCvTerm(experimentalRole, experimentalRoleMiId);
            participantEvidence.setExperimentalRole(experimentalRoleCvTerm);

            xmlParticipants.add(participantEvidence);
        }
    }

    public void createInteractions() {
        for (InteractionWithIndexes interactionWithIndexes : this.interactionWithIndexes) {
            XmlInteractionEvidence interaction = new XmlInteractionEvidence();
            String interactionDetectionMethod = "detectionMethod";
            String identificationMethod = "identificationMethod";
            String hostOrganism = "hostOrganism";

            for (int j = interactionWithIndexes.startIndex; j <= interactionWithIndexes.endIndex; j++) {
                interaction.addParticipant(xmlParticipants.get(j));
                if (excelFileReader.workbook != null) {
                    interactionDetectionMethod = getColumnFromWorkbook(dataTypeAndColumn.INTERACTION_DETECTION_METHOD.value, j);
                    identificationMethod = getColumnFromWorkbook(dataTypeAndColumn.PARTICIPANT_IDENTIFICATION_METHOD.value, j);
                    hostOrganism = getColumnFromWorkbook(dataTypeAndColumn.HOST_ORGANISM.value, j);
                }
                else if (excelFileReader.fileData != null) {
                    interactionDetectionMethod = getColumnFromFileWithSeparator(dataTypeAndColumn.INTERACTION_DETECTION_METHOD.value, j);
                    identificationMethod = getColumnFromFileWithSeparator(dataTypeAndColumn.PARTICIPANT_IDENTIFICATION_METHOD.value, j);
                    hostOrganism = getColumnFromFileWithSeparator(dataTypeAndColumn.HOST_ORGANISM.value, j);
                }
            }

            CvTerm interactionType = new XmlCvTerm("association", "MI:0356"); //TODO: check that
            interaction.setInteractionType(interactionType);

            //TODO: add attributeList

            int hostOrganismInt = Integer.parseInt(excelFileReader.organismTaxIdFormatter(hostOrganism));
            XmlOrganism organism = new XmlOrganism(hostOrganismInt);

            String interactionDetectionMiId = olsClient.getExactTermByName(interactionDetectionMethod, "mi").getOboId().
                    getIdentifier();
            XmlCvTerm detectionMethod = new XmlCvTerm(interactionDetectionMethod, interactionDetectionMiId);
            Publication publication = new BibRef("TOSEE"); //TODO: UI
            XmlExperiment experiment = new XmlExperiment(publication, detectionMethod, organism);

            String identificationMethodMiId = olsClient.getExactTermByName(identificationMethod, "mi").getOboId().
                    getIdentifier();
            XmlCvTerm identificationMethodCv = new XmlCvTerm(identificationMethod, identificationMethodMiId);
            experiment.setParticipantIdentificationMethod(identificationMethodCv);



            interaction.setExperiment(experiment);
            xmlModelledInteractions.add(interaction);
        }
    }

    public String getColumnFromWorkbook(String toGet, int rowSelected) {
        Workbook workbook = excelFileReader.workbook;
        return workbook.getSheetAt(sheetSelected).getRow(rowSelected).getCell(columnAndIndex.get(toGet)).getStringCellValue();
    }

    public String getColumnFromFileWithSeparator(String toGet, int rowSelected) {
        ArrayList<ArrayList<String>> fileData = excelFileReader.fileData;
        return fileData.get(rowSelected).get(columnAndIndex.get(toGet));
    }

}
