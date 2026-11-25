package database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DB   = "edutrack_db";

    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    private static final String JDBC_URL = String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            System.getProperty("db.host", DEFAULT_HOST),
            System.getProperty("db.port", DEFAULT_PORT),
            System.getProperty("db.name", DEFAULT_DB)
    );

    private static final String USER = System.getProperty("db.user",
            System.getenv("EDUTRACK_DB_USER") != null ? System.getenv("EDUTRACK_DB_USER") : "root");

    private static final String PASSWORD = System.getProperty("db.password",
            System.getenv("EDUTRACK_DB_PASSWORD") != null ? System.getenv("EDUTRACK_DB_PASSWORD") : "root");

    static {
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
        }
    }

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            throw e;
        }
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    public static boolean testConnection() {
        System.out.println("Testing database connection to " + JDBC_URL + "...");
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                DatabaseMetaData metaData = conn.getMetaData();
                System.out.println("Connected to: " + metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion());

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT 1 AS ping, CURRENT_TIMESTAMP AS server_time, DATABASE() AS active_db")) {
                    if (rs.next()) {
                        System.out.println("Database Ping OK. Active DB: " + rs.getString("active_db"));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            return false;
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println("Starting EduTrack DBConnection self-test...");
        boolean passed = testConnection();
        if (!passed) {
            System.exit(1);
        }
    }
}
