package lfrlms.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import lfrlms.exception.LfrlmsException;
import lfrlms.service.AlertSnapshot;
import lfrlms.service.DashboardService;
import lfrlms.util.AppLogger;

/**
 * Purpose: Displays live fleet totals, revenue and alert details.
 * Concept: Swing layout managers and BackgroundTask for database work.
 */
public class DashboardPanel extends JPanel {
    private final DashboardService dashboardService;
    private final BackgroundTask<HashMap<String, Object>> refreshTask;
    private final HashMap<String, JLabel> values;
    private final DefaultListModel<String> alertItems;
    private final JLabel messageLabel;

    public DashboardPanel() {
        dashboardService = new DashboardService();
        refreshTask = new BackgroundTask<HashMap<String, Object>>();
        values = new HashMap<String, JLabel>();
        alertItems = new DefaultListModel<String>();
        messageLabel = new JLabel(" ");
        buildPanel();
        refresh();
    }

    public void refresh() {
        messageLabel.setText("Loading dashboard...");
        refreshTask.execute(this, new BackgroundTask.Work<HashMap<String, Object>>() {
            @Override
            public HashMap<String, Object> run() throws LfrlmsException {
                return dashboardService.getDashboardSummary();
            }
        }, new BackgroundTask.Completion<HashMap<String, Object>>() {
            @Override
            public void completed(HashMap<String, Object> summary, Exception failure) {
                if (failure != null) {
                    messageLabel.setText("Dashboard unavailable; check the database connection.");
                    AppLogger.logException(DashboardPanel.class.getName(),
                            "Dashboard refresh failed.", failure);
                    return;
                }
                updateValues(summary);
                messageLabel.setText(" ");
            }
        });
    }

    public void updateAlerts(AlertSnapshot snapshot) {
        alertItems.clear();
        for (String item : snapshot.getItems()) {
            alertItems.addElement(item);
        }
        if (alertItems.isEmpty()) {
            alertItems.addElement("No current alerts.");
        }
    }

    public void showAlertFailure() {
        alertItems.clear();
        alertItems.addElement("Alerts unavailable; check the database connection.");
    }

    private void buildPanel() {
        setLayout(new BorderLayout(8, 8));
        Theme.applyPadding(this);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(event -> refresh());
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.add(new JLabel("Fleet overview"), BorderLayout.WEST);
        top.add(messageLabel, BorderLayout.CENTER);
        top.add(refreshButton, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(3, 3, 8, 8));
        addCard(cards, "Total vehicles", "total_vehicles");
        addCard(cards, "Available vehicles", "available_vehicles");
        addCard(cards, "Rented vehicles", "rented_vehicles");
        addCard(cards, "Leased vehicles", "leased_vehicles");
        addCard(cards, "Maintenance vehicles", "maintenance_vehicles");
        addCard(cards, "Active rentals", "active_rentals");
        addCard(cards, "Active leases", "active_leases");
        addCard(cards, "Upcoming rentals", "upcoming_rentals");
        addCard(cards, "Revenue this month", "monthly_revenue");
        add(cards, BorderLayout.CENTER);

        JList<String> list = new JList<String>(alertItems);
        list.setVisibleRowCount(5);
        JScrollPane alertScroll = new JScrollPane(list);
        alertScroll.setBorder(BorderFactory.createTitledBorder("Current alerts"));
        JPanel alertPanel = new JPanel(new BorderLayout());
        alertPanel.add(alertScroll, BorderLayout.CENTER);
        alertPanel.setPreferredSize(new java.awt.Dimension(500, 150));
        add(alertPanel, BorderLayout.SOUTH);
    }

    private void addCard(JPanel cards, String title, String key) {
        JPanel card = new JPanel(new BorderLayout(4, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.HEADER_COLOR),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(Theme.HEADER_COLOR);
        JLabel valueLabel = new JLabel("—", SwingConstants.CENTER);
        valueLabel.setFont(Theme.BASE_FONT.deriveFont(Font.BOLD, 22.0f));
        valueLabel.setForeground(Theme.ACCENT_COLOR);
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        cards.add(card);
        values.put(key, valueLabel);
    }

    private void updateValues(HashMap<String, Object> summary) {
        String[] keys = new String[] {
            "total_vehicles", "available_vehicles", "rented_vehicles",
            "leased_vehicles", "maintenance_vehicles", "active_rentals",
            "active_leases", "upcoming_rentals", "monthly_revenue"
        };
        for (String key : keys) {
            JLabel valueLabel = values.get(key);
            Object value = summary == null ? null : summary.get(key);
            valueLabel.setText(formatValue(value, "monthly_revenue".equals(key)));
        }
    }

    private String formatValue(Object value, boolean money) {
        if (value == null) {
            return money ? "0.00" : "0";
        }
        if (money && value instanceof BigDecimal) {
            return ((BigDecimal) value).setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        return value.toString();
    }
}
