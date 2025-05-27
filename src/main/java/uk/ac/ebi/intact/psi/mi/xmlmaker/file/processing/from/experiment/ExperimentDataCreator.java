package uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.from.experiment;

import uk.ac.ebi.intact.psi.mi.xmlmaker.models.Experiment;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.Interaction;
import uk.ac.ebi.intact.psi.mi.xmlmaker.models.Participant;

import java.util.List;

public class ExperimentDataCreator {
    List<Experiment> experiments;

    public Participant createNewParticipant(String id, String name, String experimentalRole) {
        Participant participant = new Participant();
        participant.setId(id);
        participant.setName(name);
        participant.setExperimentalRole(experimentalRole);
        return participant;
    }

    public void createNewInteraction(int interactionNumber) {
        Interaction interaction = new Interaction();
        interaction.setInteractionNumber(interactionNumber);
    }

    public void createNewExperiment(String hostTaxId, String interactionDetectionMethod, String participantIdentificationMethod) {
        Experiment experiment = new Experiment();
        experiment.setHostOrganismTaxId(hostTaxId);
        experiment.setInteractionsDetectionMethod(interactionDetectionMethod);
        experiment.setParticipantsIdentificationMethod(participantIdentificationMethod);
    }
}
