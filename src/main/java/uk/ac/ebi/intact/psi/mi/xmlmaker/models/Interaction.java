package uk.ac.ebi.intact.psi.mi.xmlmaker.models;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Interaction {
    private int experimentNumber;
    private int interactionNumber;
    private List<Participant> participants;
    private String figureLegend;
    private List<Parameter> parameters;

    public Interaction(){
        participants = new ArrayList<Participant>();
    }
}
