package database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * ============================================================================
 * VIVA EVALUATION REFERENCE - JDBC ARCHITECTURE & CONCEPTS:
 * ============================================================================
 * 1. WHAT IS JDBC?
 *    JDBC (Java Database Connectivity) is a standard Java API that enables Java
 *    applications to interact with relational database management systems (RDBMS)
 *    in a vendor-independent manner.
 *
 * 2. 3-LAYER ARCHITECTURE CONTEXT:
 *    - Presentation Layer (Console Menus / UI): Captures user inputs.
 *    - Service Layer: Enforces business logic, validation, calculations.
 *    - DAO Layer (Data Access Object): Performs CRUD operations using SQL.
 *    - DBConnection (this class): The foundational database utility providing
 *      managed java.sql.Connection objects to the DAO layer.
 *
 * 3. JDBC DRIVER LOADING (Class.forName vs JDBC 4.0 SPI):
 *    - In JDBC 3.0 (Java 5 and earlier): Explicit driver registration via
 *      `Class.forName("com.mysql.cj.jdbc.Driver")` was mandatory. This dynamically
 *      loads the class bytecode, triggering its static initialization block where
 *      it executes `DriverManager.registerDriver(new Driver())`.
 *    - In JDBC 4.0+ (Java 6 onwards): Automatic discovery occurs via Java SPI
 *      (Service Provider Interface) by reading `META-INF/services/java.sql.Driver`
 *      packaged within `mysql-connector-java-*.jar`.
 *    - Best Practice: Explicit `Class.forName(...)` is maintained below for defense-
 *      in-depth, ensuring predictable behavior across legacy JVMs and command-line
 *      executions where CLASSPATH service loaders may fail.
 *
 * 4. JDBC URL DECONSTRUCTION:
 *    Format: jdbc:mysql://[host]:[port]/[database]?[parameters]
 *    - protocol:       `jdbc` (Java Database Connectivity engine)
 *    - sub-protocol:   `mysql` (identifies MySQL driver/dialect)
 *    - host:           `localhost` (loopback IP 127.0.0.1 for local MySQL server)
 *    - port:           `3306` (standard default port for MySQL)
 *    - database:       `edutrack_db` (target schema created by edutrack_schema.sql)
 *    - parameters:
 *        * useSSL=false: Disables SSL handshake for local development simplicity.
 *        * allowPublicKeyRetrieval=true: Required for MySQL 8.0 caching_sha2_password authentication.
 *        * serverTimezone=UTC: Prevents timezone conversion discrepancies between JVM and MySQL.
 *
 * 5. SQLException HANDLING STRATEGY:
 *    - SQLException encapsulates relational database errors.
 *    - getErrorCode(): Vendor-specific database error code (e.g., MySQL 1062 for Duplicate Key,
 *      1452 for Foreign Key constraint violation).
 *    - getSQLState(): ANSI SQL-standard 5-character diagnostic state code (e.g., "23000" for
 *      integrity constraint violation).
 *    - getMessage(): Human-readable error description returned by MySQL.
 *    - getNextException(): Enables walking through chained exception queues when multiple
 *      errors occur during batch execution.
 * ============================================================================
 */
public class DBConnection {

    // ========================================================================
    // DATABASE CONFIGURATION CONSTANTS
    // (Override via Environment Variables or System Properties if customized)
    // ========================================================================
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DB   = "edutrack_db";

    /** Fully qualified class name for the MySQL 8.0+ Connector/J Driver */
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    /**
     * JDBC URL configured with MySQL 8.0+ recommended connection parameters:
     * - serverTimezone=UTC: Guarantees consistent date/time parsing.
     * - useSSL=false: Avoids keystore configuration for local testing.
     * - allowPublicKeyRetrieval=true: Satisfies sha256_password authentication.
     */
    private static final String JDBC_URL = String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            System.getProperty("db.host", DEFAULT_HOST),
            System.getProperty("db.port", DEFAULT_PORT),
            System.getProperty("db.name", DEFAULT_DB)
    );

    /** MySQL authentication credentials (configurable via system properties or environment) */
    private static final String USER = System.getProperty("db.user",
            System.getenv("EDUTRACK_DB_USER") != null ? System.getenv("EDUTRACK_DB_USER") : "root");

    private static final String PASSWORD = System.getProperty("db.password",
            System.getenv("EDUTRACK_DB_PASSWORD") != null ? System.getenv("EDUTRACK_DB_PASSWORD") : "root");

    // Static initializer block: executed exactly once when the class is loaded by the JVM
    static {
        try {
            /*
             * VIVA POINT: Class.forName dynamically loads the driver class into JVM memory.
             * com.mysql.cj.jdbc.Driver registers itself with DriverManager in its static block.
             */
            Class.forName(DRIVER_CLASS);
            System.out.println("[DBConnection] MySQL JDBC Driver registered successfully: " + DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] CRITICAL ERROR: MySQL JDBC Driver not found on classpath!");
            System.err.println("[DBConnection] Ensure 'mysql-connector-java-8.0.33.jar' exists in lib/ and is added to the classpath.");
            System.err.println("[DBConnection] Exception detail: " + e.getMessage());
        }
    }

    /**
     * Private constructor to prevent external instantiation.
     * Follows the Static Utility / Connection Factory pattern.
     */
    private DBConnection() {
        // Enforce non-instantiability
    }

    /**
     * Obtains a new active connection to the EduTrack MySQL database.
     *
     * @return a valid {@link Connection} instance.
     * @throws SQLException if a database access error occurs or credentials are invalid.
     */
    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
        } catch (SQLException e) {
            // Detailed diagnostic logging for debugging and viva explanation
            System.err.println("[DBConnection] Connection attempt failed!");
            System.err.println("  -> Error Code : " + e.getErrorCode());
            System.err.println("  -> SQL State  : " + e.getSQLState());
            System.err.println("  -> Message    : " + e.getMessage());
            throw e; // Re-throw to caller (DAO layer handles transaction rollback or logging)
        }
    }

    /**
     * Safely closes an open {@link Connection} without leaking resources.
     *
     * @param connection the connection to close (null-safe).
     */
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("[DBConnection] Warning: Error closing database connection: " + e.getMessage());
            }
        }
    }

    /**
     * Verifies connectivity to the MySQL server and outputs rich diagnostic
     * metadata useful for debugging and viva presentation.
     *
     * @return true if connection was successfully opened and closed; false otherwise.
     */
    public static boolean testConnection() {
        System.out.println("=================================================================");
        System.out.println("            EDUTRACK JDBC CONNECTIVITY DIAGNOSTIC               ");
        System.out.println("=================================================================");
        System.out.println("[CONFIG] JDBC URL     : " + JDBC_URL);
        System.out.println("[CONFIG] Driver Class : " + DRIVER_CLASS);
        System.out.println("[CONFIG] DB Username  : " + USER);
        System.out.println("[CONFIG] Password Set : " + (PASSWORD != null && !PASSWORD.isEmpty() ? "YES (masked)" : "NO (empty)"));
        System.out.println("-----------------------------------------------------------------");

        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                DatabaseMetaData metaData = conn.getMetaData();

                System.out.println("[SUCCESS] Connection established successfully!");
                System.out.println("  * Database Product Name    : " + metaData.getDatabaseProductName());
                System.out.println("  * Database Product Version : " + metaData.getDatabaseProductVersion());
                System.out.println("  * Driver Name              : " + metaData.getDriverName());
                System.out.println("  * Driver Version           : " + metaData.getDriverVersion());
                System.out.println("  * Connected Catalog/DB     : " + conn.getCatalog());
                System.out.println("  * Auto-Commit Mode         : " + conn.getAutoCommit());
                System.out.println("  * Read-Only Mode           : " + conn.isReadOnly());
                System.out.println("  * Transaction Isolation    : " + conn.getTransactionIsolation());

                // Execute ping query to verify SQL execution pipeline
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT 1 AS ping, CURRENT_TIMESTAMP AS server_time, DATABASE() AS active_db")) {
                    if (rs.next()) {
                        System.out.println("-----------------------------------------------------------------");
                        System.out.println("[PING TEST] SQL Query Executed: 'SELECT 1, CURRENT_TIMESTAMP, DATABASE()'");
                        System.out.println("  * Ping Response            : " + rs.getInt("ping"));
                        System.out.println("  * MySQL Server Time        : " + rs.getString("server_time"));
                        System.out.println("  * Active Schema            : " + rs.getString("active_db"));
                    }
                }

                System.out.println("=================================================================");
                System.out.println("[RESULT] JDBC CONNECTIVITY TEST PASSED.");
                System.out.println("=================================================================");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("-----------------------------------------------------------------");
            System.err.println("[FAILURE] JDBC CONNECTIVITY TEST FAILED!");
            System.err.println("-----------------------------------------------------------------");
            System.err.println("SQLException Diagnostics:");
            System.err.println("  * SQL State  : " + e.getSQLState());
            System.err.println("  * Error Code : " + e.getErrorCode());
            System.err.println("  * Message    : " + e.getMessage());

            // Diagnostic troubleshooting hints for viva / common setups
            System.err.println("\nTroubleshooting Tips:");
            if (e.getErrorCode() == 1045) {
                System.err.println("  -> Error 1045: Access denied. Check db.user and db.password.");
                System.err.println("     Run with: java -Ddb.user=root -Ddb.password=YOUR_PASSWORD database.DBConnection");
            } else if (e.getErrorCode() == 1049) {
                System.err.println("  -> Error 1049: Unknown database. Execute sql/edutrack_schema.sql first.");
            } else if (e.getMessage() != null && e.getMessage().contains("Communications link failure")) {
                System.err.println("  -> Communications failure: Ensure MySQL service is running on port " + DEFAULT_PORT + ".");
            }
            System.err.println("=================================================================");
            return false;
        }
        return false;
    }

    /**
     * Standalone main method to execute connectivity verification from the command line.
     *
     * Usage:
     *   javac -cp "lib/mysql-connector-java-8.0.33.jar" -d bin src/database/DBConnection.java
     *   java -cp "bin;lib/mysql-connector-java-8.0.33.jar" database.DBConnection
     *
     * With custom credentials:
     *   java -Ddb.user=root -Ddb.password=mypassword -cp "bin;lib/mysql-connector-java-8.0.33.jar" database.DBConnection
     *
     * @param args command-line arguments (optional).
     */
    public static void main(String[] args) {
        System.out.println("Starting EduTrack DBConnection self-test...\n");
        boolean passed = testConnection();
        if (!passed) {
            System.exit(1);
        }
    }
}
