package uk.ac.ebi.intact.psi.mi.xmlmaker;

import javax.swing.*;
import java.awt.*;

public class LoadingSpinner {

    private JPanel glassPane;

    public JComponent createLoadingGlassPane(JFrame frame) {
        glassPane = new JPanel(new BorderLayout(200, 200)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(0, 0, 0, 100));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        glassPane.setOpaque(false);

        ImageIcon loadingIcon = new ImageIcon("src/main/resources/LoadingSpinner.gif");

        JLabel loadingLabel = new JLabel("Loading... ", loadingIcon, JLabel.CENTER);
        glassPane.add(loadingLabel, BorderLayout.CENTER);
        frame.setGlassPane(glassPane);

        return glassPane;
    }

    public void showSpinner() {
        if (glassPane != null) {
            glassPane.setVisible(true);
        }
    }

    public void hideSpinner() {
        if (glassPane != null) {
            glassPane.setVisible(false);
        }
    }
}
