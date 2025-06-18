package uk.ac.ebi.intact.psi.mi.xmlmaker.utils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.logging.Logger;

public class GuiUtils {
    private static final Logger LOGGER = Logger.getLogger(GuiUtils.class.getName());


    /**
     * Installs a listener to receive notification when the text of any
     * {@code JTextComponent} is changed. Internally, it installs a
     * {@link DocumentListener} on the text component's {@link Document},
     * and a {@link PropertyChangeListener} on the text component to detect
     * if the {@code Document} itself is replaced.
     *
     * @param text any text component, such as a {@link JTextField}
     *        or {@link JTextArea}
     * @param changeListener a listener to receieve {@link ChangeEvent}s
     *        when the text is changed; the source object for the events
     *        will be the text component
     * @throws NullPointerException if either parameter is null
     */
    public static void addChangeListener(JTextComponent text, ChangeListener changeListener) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(changeListener);
        DocumentListener dl = new DocumentListener() {
            private int lastChange = 0, lastNotifiedChange = 0;

            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                lastChange++;
                SwingUtilities.invokeLater(() -> {
                    if (lastNotifiedChange != lastChange) {
                        lastNotifiedChange = lastChange;
                        changeListener.stateChanged(new ChangeEvent(text));
                    }
                });
            }
        };
        text.addPropertyChangeListener("document", (PropertyChangeEvent e) -> {
            Document d1 = (Document)e.getOldValue();
            Document d2 = (Document)e.getNewValue();
            if (d1 != null) d1.removeDocumentListener(dl);
            if (d2 != null) d2.addDocumentListener(dl);
            dl.changedUpdate(null);
        });
        Document d = text.getDocument();
        if (d != null) d.addDocumentListener(dl);
    }

    /**
     * Configures combo box dimensions and adds the default item to the combo box.
     *
     * @param comboBox    The combo box to be configured.
     * @param defaultItem The default item to add to the combo box.
     * @return The configured combo box.
     */
    public static JComboBox<String> setComboBoxDimension(JComboBox<String> comboBox, String defaultItem) {
        comboBox.addItem(defaultItem);
        comboBox.setPreferredSize(new Dimension(200, 50));
        comboBox.setMaximumSize(new Dimension(200, 50));
        return comboBox;
    }

    /**
     * Displays an error message in a dialog box.
     * @param message message to display
     */
    public static void showErrorDialog(String message) {
        LOGGER.severe("Error: " + message);
        JOptionPane.showMessageDialog(new JFrame(), message, "ERROR", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Displays an error message in a dialog box.
     * @param message message to display
     * @return boolean answer from user
     */
    public static boolean showConfirmDialog(String message) {
        int choice = JOptionPane.showConfirmDialog(
                new JFrame(),
                message,
                "Confirmation",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        return choice == JOptionPane.OK_OPTION;
    }

    /**
     * Displays an informational message in a dialog box.
     * @param message message to display
     */
    public static void showInfoDialog(String message) {
        JOptionPane.showMessageDialog(new JFrame(), message, "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
    }

}
