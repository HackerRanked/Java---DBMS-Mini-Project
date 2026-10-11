package lfrlms;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lfrlms.util.AppLogger;
import lfrlms.util.DBConnection;

/**
 * Purpose: Checks the configured JDBC connection and reads basic database facts.
 * Concept: JDBC, multi-catch, try-with-resources and finally.
 */
public class ConnectionTest {
    public static void main(String[] args) {
        try (Connection connection = DBConnection.getConnection()) {
            String databaseName = readDatabaseName(connection);
            int vehicleCount = readVehicleCount(connection);
            System.out.println("Connected database: " + databaseName);
            System.out.println("Vehicle count: " + vehicleCount);
        } catch (IOException | SQLException exception) {
            AppLogger.logException(ConnectionTest.class.getName(),
                    "The database connection test failed.", exception);
            System.err.println(
                    "Could not connect. Check MySQL, config/db.properties and Connector/J in lib.");
        } finally {
            System.out.println("Connection test finished.");
        }
    }

    private static String readDatabaseName(Connection connection) throws SQLException {
        String sql = "SELECT DATABASE()";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        return "(no database selected)";
    }

    private static int readVehicleCount(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM vehicle";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return 0;
    }
}
