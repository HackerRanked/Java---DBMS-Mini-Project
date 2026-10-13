package lfrlms.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import lfrlms.exception.LfrlmsException;
import lfrlms.service.DashboardService;
import lfrlms.util.AppLogger;

/**
 * Purpose: Hosts the application modules and a live database status bar.
 * Concept: Swing tabs, layout managers and safe window shutdown.
 */
public class MainFrame extends JFrame {
    private static final DateTimeFormatter CLOCK_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss");
    private final DashboardService dashboardService;
    private final BackgroundTask<HashMap<String, Object>> statusTask;
    private final JLabel clockLabel;
    private final JLabel databaseLabel;
    private final JLabel alertLabel;
    private final JLabel messageLabel;
    private final Timer clockTimer;
    private final Timer statusTimer;

    public MainFrame() {
        super("Luxury Fleet Rental & Leasing Management System");
        dashboardService = new DashboardService();
        statusTask = new BackgroundTask<HashMap<String, Object>>();
        clockLabel = new JLabel();
        databaseLabel = new JLabel("Database: checking...");
        alertLabel = new JLabel("Alerts: checking...");
        messageLabel = new JLabel(" ");
        clockTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                updateClock();
            }
        });
        statusTimer = new Timer(60000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                refreshStatus();
            }
        });
        buildWindow();
        updateClock();
        clockTimer.start();
        statusTimer.start();
        refreshStatus();
    }

    private void buildWindow() {
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Dashboard", placeholder("Dashboard summaries will be added later."));
        tabs.addTab("Vehicles", new VehiclesPanel());
        tabs.addTab("Customers", new CustomersPanel());
        tabs.addTab("Rentals", placeholder("Rental workflows will be added later."));
        tabs.addTab("Leases", placeholder("Lease workflows will be added later."));
        tabs.addTab("Payments", placeholder("Payment workflows will be added later."));
        tabs.addTab("Maintenance", placeholder("Maintenance workflows will be added later."));
        tabs.addTab("Drivers", placeholder("Driver management will be added later."));
        tabs.addTab("Reports", placeholder("Reports will be added later."));
        tabs.addTab("DBMS Insights", placeholder("Database insights will be added later."));
        add(tabs, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                clockTimer.stop();
                statusTimer.stop();
                statusTask.ignorePendingResults();
                dispose();
            }
        });
        setSize(1200, 750);
        setMinimumSize(new java.awt.Dimension(960, 620));
        setLocationRelativeTo(null);
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        left.add(clockLabel);
        left.add(databaseLabel);
        left.add(alertLabel);
        JButton retryButton = new JButton("Retry connection");
        retryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                refreshStatus();
            }
        });
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 3));
        right.add(messageLabel);
        right.add(retryButton);
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel placeholder(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private void updateClock() {
        clockLabel.setText(LocalDateTime.now().format(CLOCK_FORMAT));
    }

    private void refreshStatus() {
        messageLabel.setText("Checking database...");
        statusTask.execute(this, new BackgroundTask.Work<HashMap<String, Object>>() {
            @Override
            public HashMap<String, Object> run() throws LfrlmsException {
                return dashboardService.getSummary();
            }
        }, new BackgroundTask.Completion<HashMap<String, Object>>() {
            @Override
            public void completed(HashMap<String, Object> summary, Exception failure) {
                if (failure != null) {
                    databaseLabel.setText("Database: unavailable");
                    alertLabel.setText("Alerts: unavailable");
                    messageLabel.setText(
                            "Cannot connect. Check MySQL and config/db.properties.");
                    AppLogger.logException(MainFrame.class.getName(),
                            "Database status check failed.", failure);
                    return;
                }
                databaseLabel.setText("Database: connected");
                alertLabel.setText(alertSummary(summary));
                messageLabel.setText(" ");
            }
        });
    }

    private String alertSummary(HashMap<String, Object> summary) {
        return "Alerts overdue/upcoming/leases/maintenance/licences: "
                + count(summary, "overdueRentals") + "/"
                + count(summary, "upcomingRentals") + "/" + count(summary, "leasesEnding")
                + "/" + count(summary, "maintenanceDue") + "/"
                + count(summary, "licensesExpiring");
    }

    private int count(HashMap<String, Object> summary, String name) {
        Object value = summary.get(name);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }
}
