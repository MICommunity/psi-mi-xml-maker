package uk.ac.ebi.intact.psi.mi.xmlmaker;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * This class provides a loading spinner overlay for a given JFrame.
 * It shows and hides a loading spinner when needed.
 */
public class LoadingSpinner {

    private JPanel glassPane;

    /**
     * Creates and returns a glass pane with a loading spinner for the given frame.
     * The glass pane will display a loading spinner centered in the frame.
     *
     * @param frame the {@link JFrame} to which the glass pane will be attached.
     * @return the {@link JComponent} representing the glass pane with the loading spinner.
     */
    public JComponent createLoadingGlassPane(JFrame frame) {
        glassPane = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
            }
        };

        glassPane.setOpaque(false);
        glassPane.setPreferredSize(frame.getSize());

        ImageIcon loadingIcon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/LoadingSpinner.gif")));

        JLabel loadingLabel = new JLabel(loadingIcon, JLabel.CENTER);
        glassPane.add(loadingLabel, BorderLayout.CENTER);
        frame.setGlassPane(glassPane);

        return glassPane;
    }

    /**
     * Shows the loading spinner by making the glass pane visible.
     * This method should be called when the loading process begins.
     */
    public void showSpinner() {
        SwingUtilities.invokeLater(() -> {
            if (glassPane != null) {
                glassPane.setVisible(true);
            }
        });
    }

    /**
     * Hides the loading spinner by making the glass pane invisible.
     * This method should be called when the loading process finishes.
     */
    public void hideSpinner() {
        SwingUtilities.invokeLater(() -> {
            if (glassPane != null) {
                glassPane.setVisible(false);  // Hide the glass pane with the spinner
            }
        });
    }
}
