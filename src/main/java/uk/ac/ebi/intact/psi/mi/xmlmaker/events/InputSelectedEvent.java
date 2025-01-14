package uk.ac.ebi.intact.psi.mi.xmlmaker.events;

import lombok.Getter;

import java.io.File;


@Getter
public class InputSelectedEvent {

    private final File selectedFile;

    public InputSelectedEvent(File selectedFile) {
        this.selectedFile = selectedFile;
    }

    public interface Listener {
        void handle(InputSelectedEvent event);
    }
}
