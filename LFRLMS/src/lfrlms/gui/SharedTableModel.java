package lfrlms.gui;

import java.awt.Color;
import java.awt.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Vector;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * Purpose: Supplies non-editable, replaceable rows to module tables.
 * Concept: Vector collections and Swing table models.
 */
public class SharedTableModel extends DefaultTableModel {
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private JTable attachedTable;

    public SharedTableModel(String[] columns) {
        super(columns, 0);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public void replaceRows(Vector<Vector<Object>> rows) {
        Vector<String> identifiers = new Vector<String>();
        for (int column = 0; column < getColumnCount(); column++) {
            identifiers.add(getColumnName(column));
        }
        setDataVector(rows, identifiers);
        if (attachedTable != null) {
            applyColumnRenderers(attachedTable);
        }
    }

    public static JTable createTable(SharedTableModel model) {
        JTable table = new JTable(model);
        table.setFont(Theme.BASE_FONT);
        table.setRowHeight(Theme.ROW_HEIGHT);
        table.setBackground(Theme.TABLE_BACKGROUND);
        table.setForeground(Theme.TEXT_COLOR);
        table.setSelectionBackground(Theme.ACCENT_COLOR);
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(new Color(221, 228, 234));
        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.BASE_FONT.deriveFont(java.awt.Font.BOLD));
        header.setBackground(Theme.HEADER_COLOR);
        header.setForeground(Color.WHITE);
        model.attachedTable = table;
        model.applyColumnRenderers(table);
        return table;
    }

    private void applyColumnRenderers(JTable table) {
        for (int column = 0; column < getColumnCount(); column++) {
            String name = getColumnName(column).toLowerCase();
            if (name.contains("status")) {
                table.getColumnModel().getColumn(column)
                        .setCellRenderer(new StatusColourRenderer());
            } else {
                table.getColumnModel().getColumn(column)
                        .setCellRenderer(new ZebraDateTimeRenderer());
            }
        }
    }

    private static class ZebraDateTimeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            Component cell = super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            if (!selected) {
                cell.setBackground(row % 2 == 0
                        ? Theme.TABLE_BACKGROUND : Theme.ZEBRA_COLOR);
                cell.setForeground(Theme.TEXT_COLOR);
            }
            return cell;
        }

        @Override
        protected void setValue(Object value) {
            if (value instanceof LocalDateTime) {
                setText(((LocalDateTime) value).format(DATE_TIME_FORMAT));
            } else {
                setText(value == null ? "" : value.toString());
            }
        }
    }
}
