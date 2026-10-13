package lfrlms.gui;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Purpose: Gives vehicle status values a clear visual meaning in tables.
 * Concept: Swing renderer and conditional display.
 */
public class StatusColourRenderer extends DefaultTableCellRenderer {
    private static final Color AVAILABLE = new Color(220, 245, 226);
    private static final Color RENTED = new Color(255, 232, 184);
    private static final Color LEASED = new Color(205, 222, 255);
    private static final Color MAINTENANCE = new Color(255, 214, 214);
    private static final Color INACTIVE = new Color(226, 229, 233);
    private static final Color RESERVED = new Color(224, 231, 255);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean selected, boolean focused, int row, int column) {
        Component cell = super.getTableCellRendererComponent(
                table, value, selected, focused, row, column);
        if (!selected) {
            String status = value == null ? "" : value.toString();
            if ("Available".equals(status)) {
                cell.setBackground(AVAILABLE);
            } else if ("Rented".equals(status)) {
                cell.setBackground(RENTED);
            } else if ("Leased".equals(status)) {
                cell.setBackground(LEASED);
            } else if ("Maintenance".equals(status)) {
                cell.setBackground(MAINTENANCE);
            } else if ("Inactive".equals(status)) {
                cell.setBackground(INACTIVE);
            } else if ("Reserved".equals(status)) {
                cell.setBackground(RESERVED);
            } else {
                cell.setBackground(table.getBackground());
            }
        }
        return cell;
    }
}
