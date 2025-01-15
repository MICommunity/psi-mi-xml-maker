package uk.ac.ebi.intact.psi.mi.xmlmaker.events;

import lombok.Getter;
import java.util.Map;

@Getter
public class ColumnAndIndexesEvent {
    private final Map<String, Integer> dataAndIndexes;

    public ColumnAndIndexesEvent(Map<String, Integer> dataAndIndexes) {
        this.dataAndIndexes = dataAndIndexes;
    }

    public interface ListenerColumnAndIndexesEvent {
        void handle(uk.ac.ebi.intact.psi.mi.xmlmaker.events.ColumnAndIndexesEvent event);
    }
}


