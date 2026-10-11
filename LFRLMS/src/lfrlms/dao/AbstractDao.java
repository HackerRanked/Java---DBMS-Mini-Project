package lfrlms.dao;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lfrlms.exception.DataAccessException;
import lfrlms.exception.LfrlmsException;
import lfrlms.exception.RecordInUseException;
import lfrlms.util.DBConnection;
import lfrlms.util.QueryLog;

/**
 * Purpose: Shares JDBC resource handling and error translation across DAOs.
 * Concept: Inheritance, try-with-resources and exception chaining.
 */
public abstract class AbstractDao {
    protected final QueryLog queryLog;

    protected AbstractDao() {
        this(new QueryLog());
    }

    protected AbstractDao(QueryLog queryLog) {
        this.queryLog = queryLog == null ? new QueryLog() : queryLog;
    }

    protected ArrayList<HashMap<String, Object>> queryRows(String sql,
            Object... parameters) throws LfrlmsException {
        long started = System.nanoTime();
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters, 1);
            try (ResultSet resultSet = statement.executeQuery()) {
                ArrayList<HashMap<String, Object>> rows = readRows(resultSet);
                recordQuery(sql, parameters, started, rows.size());
                return rows;
            }
        } catch (SQLException exception) {
            recordQuery(sql, parameters, started, -1);
            throw translate(exception, "read database records");
        }
    }

    protected ArrayList<HashMap<String, Object>> callRows(String sql,
            Object... parameters) throws LfrlmsException {
        long started = System.nanoTime();
        try (Connection connection = openConnection();
                CallableStatement statement = connection.prepareCall(sql)) {
            bindParameters(statement, parameters, 1);
            boolean hasResultSet = statement.execute();
            while (!hasResultSet && statement.getUpdateCount() != -1) {
                hasResultSet = statement.getMoreResults();
            }
            ArrayList<HashMap<String, Object>> rows = new ArrayList<HashMap<String, Object>>();
            if (hasResultSet) {
                try (ResultSet resultSet = statement.getResultSet()) {
                    rows = readRows(resultSet);
                }
            }
            recordQuery(sql, parameters, started, rows.size());
            return rows;
        } catch (SQLException exception) {
            recordQuery(sql, parameters, started, -1);
            throw translate(exception, "run a database procedure");
        }
    }

    protected Object callScalarFunction(String sql, int returnType,
            Object... parameters) throws LfrlmsException {
        long started = System.nanoTime();
        try (Connection connection = openConnection();
                CallableStatement statement = connection.prepareCall(sql)) {
            statement.registerOutParameter(1, returnType);
            bindParameters(statement, parameters, 2);
            statement.execute();
            Object value = statement.getObject(1);
            recordQuery(sql, parameters, started, value == null ? 0 : 1);
            return value;
        } catch (SQLException exception) {
            recordQuery(sql, parameters, started, -1);
            throw translate(exception, "run a database function");
        }
    }

    protected int executeUpdate(String sql, Object... parameters)
            throws LfrlmsException {
        long started = System.nanoTime();
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters, 1);
            int rows = statement.executeUpdate();
            recordQuery(sql, parameters, started, rows);
            return rows;
        } catch (SQLException exception) {
            recordQuery(sql, parameters, started, -1);
            throw translate(exception, "update database records");
        }
    }

    protected int executeInsert(String sql, Object... parameters)
            throws LfrlmsException {
        long started = System.nanoTime();
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParameters(statement, parameters, 1);
            int rows = statement.executeUpdate();
            int generatedId = 0;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    generatedId = keys.getInt(1);
                }
            }
            recordQuery(sql, parameters, started, rows);
            return generatedId;
        } catch (SQLException exception) {
            recordQuery(sql, parameters, started, -1);
            throw translate(exception, "insert a database record");
        }
    }

    protected long queryCount(String sql, Object... parameters)
            throws LfrlmsException {
        ArrayList<HashMap<String, Object>> rows = queryRows(sql, parameters);
        if (rows.isEmpty()) {
            return 0L;
        }
        Object value = rows.get(0).values().iterator().next();
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    protected Connection openConnection() throws LfrlmsException {
        try {
            return DBConnection.getConnection();
        } catch (SQLException exception) {
            throw translate(exception, "connect to the database");
        } catch (IOException exception) {
            SQLException wrapped = new SQLException("Database configuration could not be loaded.",
                    exception);
            throw new DataAccessException("Database configuration is unavailable.", wrapped);
        }
    }

    protected void bindParameters(PreparedStatement statement, Object[] parameters,
            int firstIndex) throws SQLException {
        for (int offset = 0; offset < parameters.length; offset++) {
            Object value = parameters[offset];
            int index = firstIndex + offset;
            if (value == null) {
                statement.setNull(index, Types.NULL);
            } else if (value instanceof LocalDate) {
                statement.setDate(index, Date.valueOf((LocalDate) value));
            } else if (value instanceof LocalDateTime) {
                statement.setTimestamp(index, Timestamp.valueOf((LocalDateTime) value));
            } else {
                statement.setObject(index, value);
            }
        }
    }

    protected HashMap<String, Object> firstRow(ArrayList<HashMap<String, Object>> rows) {
        return rows.isEmpty() ? null : rows.get(0);
    }

    protected int intValue(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    protected Integer nullableIntValue(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : null;
    }

    protected String stringValue(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value == null ? null : value.toString();
    }

    protected BigDecimal decimalValue(Map<String, Object> row, String column) {
        Object value = row.get(column);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return value instanceof Number
                ? new BigDecimal(value.toString()) : null;
    }

    protected LocalDate dateValue(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value instanceof Date ? ((Date) value).toLocalDate() : null;
    }

    protected LocalDateTime dateTimeValue(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value instanceof Timestamp ? ((Timestamp) value).toLocalDateTime() : null;
    }

    private ArrayList<HashMap<String, Object>> readRows(ResultSet resultSet)
            throws SQLException {
        ArrayList<HashMap<String, Object>> rows = new ArrayList<HashMap<String, Object>>();
        ResultSetMetaData metadata = resultSet.getMetaData();
        int columnCount = metadata.getColumnCount();
        while (resultSet.next()) {
            HashMap<String, Object> row = new HashMap<String, Object>();
            for (int column = 1; column <= columnCount; column++) {
                row.put(metadata.getColumnLabel(column).toLowerCase(),
                        resultSet.getObject(column));
            }
            rows.add(row);
        }
        return rows;
    }

    private void recordQuery(String sql, Object[] parameters, long started, int rows) {
        String parameterText = java.util.Arrays.deepToString(parameters);
        long elapsedMillis = (System.nanoTime() - started) / 1000000L;
        queryLog.add(sql, parameterText, elapsedMillis, rows);
    }

    private LfrlmsException translate(SQLException exception, String action) {
        if (exception.getErrorCode() == 1451) {
            return new RecordInUseException("Deactivate it instead", exception);
        }
        return new DataAccessException("Could not " + action + ".", exception);
    }
}
