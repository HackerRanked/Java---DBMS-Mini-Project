package lfrlms.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import lfrlms.exception.LfrlmsException;
import lfrlms.exception.ValidationException;
import lfrlms.util.QueryLog;

/**
 * Purpose: Reads database metadata and explains logged SELECT statements.
 * Concept: PreparedStatement, metadata and allowlisted identifiers.
 */
public class InsightsDao extends AbstractDao {
    private static final String[] TABLE_NAMES = new String[] {
        "brand", "vehicle_category", "branch", "customer", "individual_customer",
        "corporate_customer", "driver", "vehicle", "rental", "lease", "payment",
        "maintenance"
    };

    public InsightsDao() {
        super(QueryLog.getShared());
    }

    public ArrayList<String> listTableNames() {
        ArrayList<String> names = new ArrayList<String>();
        for (String tableName : TABLE_NAMES) {
            names.add(tableName);
        }
        return names;
    }

    public ArrayList<QueryLog.QueryEntry> recentQueries() {
        return queryLog.getRecentRecords();
    }

    public ArrayList<HashMap<String, Object>> explain(QueryLog.QueryEntry entry)
            throws LfrlmsException {
        String selectSql = validatedSelect(entry);
        return queryRows("EXPLAIN " + selectSql, entry.getParameterValues());
    }

    public String showCreateTable(String tableName) throws LfrlmsException {
        if (!isAllowedTable(tableName)) {
            throw new ValidationException("Choose one of the listed database tables.");
        }
        ArrayList<HashMap<String, Object>> rows = queryRows(
                "SHOW CREATE TABLE `" + tableName + "`");
        HashMap<String, Object> row = firstRow(rows);
        Object definition = row == null ? null : row.get("create table");
        return definition == null ? "Table definition is unavailable." : definition.toString();
    }

    public ArrayList<HashMap<String, Object>> listViews() throws LfrlmsException {
        String sql = "SELECT TABLE_NAME AS object_name, VIEW_DEFINITION AS definition "
                + "FROM information_schema.VIEWS WHERE TABLE_SCHEMA = DATABASE() "
                + "ORDER BY TABLE_NAME";
        return queryRows(sql);
    }

    public ArrayList<HashMap<String, Object>> listRoutines(String routineType)
            throws LfrlmsException {
        if (!"FUNCTION".equals(routineType) && !"PROCEDURE".equals(routineType)) {
            throw new ValidationException("Choose a supported routine type.");
        }
        String sql = "SELECT ROUTINE_NAME AS object_name, ROUTINE_DEFINITION AS definition "
                + "FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = DATABASE() "
                + "AND ROUTINE_TYPE = ? ORDER BY ROUTINE_NAME";
        return queryRows(sql, routineType);
    }

    public ArrayList<HashMap<String, Object>> listTriggers() throws LfrlmsException {
        String sql = "SELECT TRIGGER_NAME AS object_name, "
                + "CONCAT(ACTION_TIMING, ' ', EVENT_MANIPULATION, ' ON ', EVENT_OBJECT_TABLE, "
                + "CHAR(10), ACTION_STATEMENT) AS definition "
                + "FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE() "
                + "ORDER BY TRIGGER_NAME";
        return queryRows(sql);
    }

    private String validatedSelect(QueryLog.QueryEntry entry) throws ValidationException {
        if (entry == null || entry.getSql() == null) {
            throw new ValidationException("Select a logged query first.");
        }
        String sql = entry.getSql().trim();
        String upper = sql.toUpperCase(Locale.ROOT);
        if (!upper.startsWith("SELECT") || (upper.length() > 6
                && !Character.isWhitespace(upper.charAt(6)))) {
            throw new ValidationException("EXPLAIN is available only for logged SELECT queries.");
        }
        int semicolon = sql.indexOf(';');
        if (semicolon >= 0) {
            if (semicolon != sql.length() - 1) {
                throw new ValidationException("Only a single logged SELECT can be explained.");
            }
            sql = sql.substring(0, semicolon).trim();
        }
        return sql;
    }

    private boolean isAllowedTable(String tableName) {
        if (tableName == null) {
            return false;
        }
        for (String allowedName : TABLE_NAMES) {
            if (allowedName.equals(tableName)) {
                return true;
            }
        }
        return false;
    }
}
