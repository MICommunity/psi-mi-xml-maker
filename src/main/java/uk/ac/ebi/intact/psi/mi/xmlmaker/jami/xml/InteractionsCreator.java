package uk.ac.ebi.intact.psi.mi.xmlmaker.jami.xml;

import org.apache.poi.ss.usermodel.*;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.xml.model.extension.xml300.*;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;

import java.util.*;

public class InteractionsCreator {
    ExcelFileReader excelFileReader;
    ArrayList<InteractionWithIndexes> interactionWithIndexes = new ArrayList<>();
    ArrayList<XmlParticipantEvidence> xmlParticipants = new ArrayList<>();
    ArrayList<XmlInteractionEvidence> xmlModelledInteractions = new ArrayList<>();
    PsiMiXmlMaker xmlMaker;

    public InteractionsCreator(ExcelFileReader reader) {
        this.excelFileReader = reader;
        this.xmlMaker = new PsiMiXmlMaker(this);
    }

    public void createParticipantsWithFileFormat(Map<String, Integer> columnAndIndex, int sheetSelected){
        xmlParticipants.clear();
        xmlModelledInteractions.clear();
        excelFileReader.selectFileOpener(excelFileReader.currentFilePath);
        if (excelFileReader.workbook == null){
            createGroups("Interaction number"); //TODO: UI for that
            createParticipantsWithFileSeparator(columnAndIndex);
        } else {
            createGroupsWithWorkbook("Interaction number", sheetSelected); //TODO: UI for that
            createParticipantsWithWorkbook(columnAndIndex, sheetSelected - 1);
        }
        createOneInteraction();
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

        for (int i = 1; i <= data.size(); i++) { // start at 1 to skip header
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
        if (startIndex != null && currentInteractionNumber != null) {
            interactionWithIndexes.add(new InteractionWithIndexes(currentInteractionNumber, startIndex, data.size()));
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
            interactionWithIndexes.add(new InteractionWithIndexes(currentInteractionNumber, startIndex, sheet.getLastRowNum()));
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
            String name = datum.get(columnAndIndex.get("Participant name"));
            String fullName = datum.get(columnAndIndex.get("Participant full name"));
            XmlCvTerm type = new XmlCvTerm("Test", "MI:1234");
            XmlOrganism organism = new XmlOrganism(9606);
            XmlXref uniqueId = new XmlXref(new XmlCvTerm("testDb", "MI:5678"), "id");
            XmlProtein protein = new XmlProtein(name, fullName, type, organism, uniqueId);
//            XmlModelledParticipant participant = new XmlModelledParticipant(protein);
            XmlParticipantEvidence participantEvidence = new XmlParticipantEvidence(protein);
            xmlParticipants.add(participantEvidence);
        }
    }

    public void createParticipantsWithWorkbook(Map<String, Integer> columnAndIndex, int sheetSelected) {
        Workbook workbook = excelFileReader.workbook;
        for (int i = 0; i <= workbook.getSheetAt(sheetSelected).getLastRowNum(); i++) {
            Row row = workbook.getSheetAt(sheetSelected).getRow(i);
            String name = row.getCell(columnAndIndex.get("Participant name")).getStringCellValue();
            String fullName = row.getCell(columnAndIndex.get("Participant full name")).getStringCellValue();
            XmlCvTerm type = new XmlCvTerm("protein", "MI:0356");
            XmlOrganism organism = new XmlOrganism(9606); // TODO: take the organism from uniprot panel
            XmlXref uniqueId = new XmlXref(new XmlCvTerm("uniprotkb", "MI:0356"), "P12345"); //TODO: set to protein and check if it is other component
            XmlProtein protein = new XmlProtein(name, fullName, type, organism, uniqueId);
            CvTerm bioRole = new XmlCvTerm("bioRole", "MI:0356");


//            XmlModelledParticipant participant = new XmlModelledParticipant(protein);
//            ModelledFeature experimentalFeature = new XmlModelledFeature("Bait", "MI:0356");

            XmlParticipantEvidence participantEvidence = new XmlParticipantEvidence(protein);

//            participant.addFeature(experimentalFeature);
            xmlParticipants.add(participantEvidence);
        }
    }

    public void createOneInteraction() {

        Publication publication = new BibRef("14681455");
        XmlXref onthologyId = new XmlXref(new XmlCvTerm("psi-mi", "MI:0488"), "MI:0469");
        Source source = new XmlSource("IntAct", "European Bioinformatics Institute", onthologyId, "http://www.ebi.ac.uk", "address", publication);
        source.setUrl("http://www.ebi.ac.uk");

        XmlOrganism organism = new XmlOrganism(9606);

        XmlCvTerm detectionMethod = new XmlCvTerm("detectionMethod", "MI:0356");
        XmlExperiment experiment = new XmlExperiment(publication, detectionMethod, organism);

        XmlInteractionEvidence evidence = new XmlInteractionEvidence();
        evidence.setExperimentAndAddInteractionEvidence(experiment);

        XrefContainer xrefContainer = new XrefContainer();

        xrefContainer.setJAXBPrimaryRef(onthologyId);

        for (InteractionWithIndexes interactionWithIndexes : this.interactionWithIndexes) {
            XmlInteractionEvidence interaction = new XmlInteractionEvidence();
            for (int j = interactionWithIndexes.startIndex; j <= interactionWithIndexes.endIndex; j++) {
                interaction.addParticipant((ParticipantEvidence) xmlParticipants.get(j));
            }

            CvTerm interactionType = new XmlCvTerm("interactionType", "MI:0356");
            interaction.setInteractionType(interactionType);
//            interaction.setSource(source);
//            interaction.setJAXBOrganism(organism);
            xmlModelledInteractions.add(interaction);
        }
        xmlMaker.interactionsWriter();
    }
}
