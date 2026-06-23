package com.festops.ui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Launches the {@link OperationsDashboard} Swing desktop client.
 *
 * <p>Run this with the FestOps Spring Boot server already running on
 * localhost:8080. The dashboard polls {@code /api/v1/incidents} and refreshes
 * every 5 seconds.</p>
 */
public class DashboardApp {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to the default look and feel
        }
        SwingUtilities.invokeLater(() -> new OperationsDashboard().setVisible(true));
    }
}
