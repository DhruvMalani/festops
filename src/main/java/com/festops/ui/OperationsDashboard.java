package com.festops.ui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * Swing operations dashboard for FestOps. Polls the REST API for incidents,
 * shows them in a severity-coloured table with open/resolved stats, and
 * auto-refreshes every 5 seconds. Network calls run on a {@link SwingWorker}
 * so the EDT never blocks.
 *
 * <p>Standalone desktop client — run {@link DashboardApp} with the Spring Boot
 * server already running on localhost:8080.</p>
 */
public class OperationsDashboard extends JFrame {

    private static final String API_URL = "http://localhost:8080/api/v1/incidents";
    private static final int REFRESH_INTERVAL_MS = 5000;
    private static final String[] COLUMNS =
            {"ID", "Type", "Severity", "Status", "Responder", "Reported At"};
    private static final int SEVERITY_COL = 2;

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final transient HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final transient Gson gson = new Gson();

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel statsLabel = new JLabel("Loading…");
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton refreshButton = new JButton("Refresh");
    private final transient Timer autoRefresh;

    public OperationsDashboard() {
        super("FestOps — Operations Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // --- table (non-editable, severity row colouring) ---
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (isRowSelected(row)) {
                    c.setBackground(getSelectionBackground());
                    c.setForeground(getSelectionForeground());
                } else {
                    c.setBackground(severityColor(String.valueOf(getValueAt(row, SEVERITY_COL))));
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        };
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- stats panel (top) ---
        JPanel statsPanel = new JPanel(new GridLayout(2, 1));
        statsLabel.setFont(statsLabel.getFont().deriveFont(Font.BOLD, 14f));
        statsPanel.add(statsLabel);
        statsPanel.add(statusLabel);
        add(statsPanel, BorderLayout.NORTH);

        // --- controls (bottom) ---
        JPanel controls = new JPanel(new BorderLayout());
        JLabel legend = new JLabel(buildLegend());
        controls.add(legend, BorderLayout.WEST);
        controls.add(refreshButton, BorderLayout.EAST);
        add(controls, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> refresh());

        // --- auto-refresh every 5s ---
        autoRefresh = new Timer(REFRESH_INTERVAL_MS, e -> refresh());
        autoRefresh.setInitialDelay(0);
        autoRefresh.start();
    }

    /** Kick off an async fetch + table update. Safe to call from the EDT. */
    private void refresh() {
        refreshButton.setEnabled(false);
        statusLabel.setText("Refreshing…");

        new SwingWorker<List<IncidentDto>, Void>() {
            @Override
            protected List<IncidentDto> doInBackground() throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .timeout(Duration.ofSeconds(5))
                        .header("Accept", "application/json")
                        .GET()
                        .build();
                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() / 100 != 2) {
                    throw new RuntimeException("HTTP " + response.statusCode());
                }
                java.lang.reflect.Type listType = new TypeToken<List<IncidentDto>>() { }.getType();
                List<IncidentDto> incidents = gson.fromJson(response.body(), listType);
                return incidents != null ? incidents : Collections.emptyList();
            }

            @Override
            protected void done() {
                try {
                    populate(get());
                    statusLabel.setForeground(new Color(0, 128, 0));
                    statusLabel.setText("Last updated " + LocalTime.now().format(CLOCK));
                } catch (Exception ex) {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Cannot reach API at " + API_URL
                            + " — is the server running? (" + rootMessage(ex) + ")");
                } finally {
                    refreshButton.setEnabled(true);
                }
            }
        }.execute();
    }

    /** Replace table rows and recompute stats. Runs on the EDT (from done()). */
    private void populate(List<IncidentDto> incidents) {
        tableModel.setRowCount(0);
        int resolved = 0;
        for (IncidentDto inc : incidents) {
            String responder = inc.assignedResponder != null ? inc.assignedResponder.name : "—";
            tableModel.addRow(new Object[]{
                    inc.id, inc.type, inc.severity, inc.state, responder, inc.reportedAt});
            if ("RESOLVED".equals(inc.state)) {
                resolved++;
            }
        }
        int total = incidents.size();
        int open = total - resolved;
        statsLabel.setText(String.format(
                "Total incidents: %d      Open: %d      Resolved: %d", total, open, resolved));
    }

    private static Color severityColor(String severity) {
        if (severity == null) {
            return Color.WHITE;
        }
        return switch (severity) {
            case "CRITICAL" -> new Color(255, 102, 102); // red
            case "HIGH" -> new Color(255, 178, 102);      // orange
            case "MEDIUM" -> new Color(255, 255, 153);    // yellow
            case "LOW" -> new Color(153, 255, 153);       // green
            default -> Color.WHITE;
        };
    }

    private static String buildLegend() {
        return "<html><b>Severity:</b> "
                + "<font color='#FF6666'>■</font> CRITICAL  "
                + "<font color='#FFB266'>■</font> HIGH  "
                + "<font color='#CCCC00'>■</font> MEDIUM  "
                + "<font color='#66FF66'>■</font> LOW</html>";
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName()
                + (cause.getMessage() != null ? ": " + cause.getMessage() : "");
    }

    /** Gson target matching the /api/v1/incidents JSON (only the fields we display). */
    private static final class IncidentDto {
        String id;
        String type;
        String severity;
        String state;
        String reportedAt;
        Responder assignedResponder;
    }

    private static final class Responder {
        String name;
    }
}
