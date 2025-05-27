package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.from.experiment;

import org.apache.poi.ss.usermodel.Row;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.Experiment;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.Interaction;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.Participant;
import uk.ac.ebi.intact.psi.mi.xmlmaker.utils.FileUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.FileUtils.*;

public class ExcelReader {

    private int baitIdColumnIndex;
    private int preyIdColumnIndex;

    private int baitNameColumnIndex;
    private int preyNameColumnIndex;

    String sheetSelected;

    boolean binaryInteractions;

    final ExcelFileReader excelFileReader;
    ExperimentDataCreator experimentDataCreator = new ExperimentDataCreator();

    ExcelReader(ExcelFileReader excelFileReader) {
        this.excelFileReader = excelFileReader;
    }

    public void createDataFromFile(){
        Iterator<Row> iterator = excelFileReader.readWorkbookSheet(sheetSelected);

        int experimentNumber = 0;
        Experiment experiment = new Experiment();

        List<Interaction> interactions = new ArrayList<>();

        int currentInteractionNumber = 0;
        String lastBait = null;

        while (iterator.hasNext()) {
            Row row = iterator.next();

            List<Participant> participants = new ArrayList<>();

            String baitId = getCellValueAsString(row.getCell(baitIdColumnIndex));
            String preyId = getCellValueAsString(row.getCell(preyIdColumnIndex));

            String baitName = baitNameColumnIndex == -1 ? "" : getCellValueAsString(row.getCell(baitNameColumnIndex));
            String preyName = preyNameColumnIndex == -1 ? "" : getCellValueAsString(row.getCell(preyNameColumnIndex));

            Participant baitParticipant = experimentDataCreator.createNewParticipant(baitId, baitName, "bait");
            Participant preyParticipant = experimentDataCreator.createNewParticipant(preyId, preyName, "prey");

            if (binaryInteractions) {
                currentInteractionNumber++;
                Interaction interaction = new Interaction();
                interaction.setInteractionNumber(currentInteractionNumber);
                interaction.getParticipants().add(baitParticipant);
                interaction.getParticipants().add(preyParticipant);
                interactions.add(interaction);
            } else {
                if (lastBait == null || !lastBait.equals(baitId)) {
                    currentInteractionNumber++;
                    lastBait = baitId;

                }
                participants.add(preyParticipant);
            }

        }
        iterator.remove();
    }
}