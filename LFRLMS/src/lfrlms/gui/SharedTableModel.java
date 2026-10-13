package lfrlms.gui;

import java.util.Vector;
import javax.swing.table.DefaultTableModel;

/**
 * Purpose: Supplies non-editable, replaceable rows to module tables.
 * Concept: Vector collections and Swing table models.
 */
public class SharedTableModel extends DefaultTableModel {
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
    }
}
