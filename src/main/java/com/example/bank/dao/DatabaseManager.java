package com.example.bank.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseManager handles SQLite database connections and table initialization.
 * Supports dynamic DB URLs for production ("jdbc:sqlite:bank.db") or testing ("jdbc:sqlite::memory:").
 */
public class DatabaseManager {
    private static final Logger logger = LogManager.getLogger(DatabaseManager.class);
    private static String dbUrl = "jdbc:sqlite:bank.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            logger.error("SQLite JDBC Driver not found: {}", e.getMessage(), e);
        }
    }

    public static synchronized void setDbUrl(String url) {
        dbUrl = url;
    }

    public static synchronized String getDbUrl() {
        return dbUrl;
    }

    /**
     * Obtains a connection to the SQLite database.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    /**
     * Initializes the database schema if tables do not already exist.
     */
    public static void initializeDatabase() {
        String createAccountsTable = """
            CREATE TABLE IF NOT EXISTS accounts (
                account_number TEXT PRIMARY KEY,
                first_name TEXT NOT NULL,
                last_name TEXT NOT NULL,
                phone_number TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                balance REAL NOT NULL,
                account_type TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        """;

        String createTransactionsTable = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                account_number TEXT NOT NULL,
                transaction_type TEXT NOT NULL,
                amount REAL NOT NULL,
                balance_after REAL NOT NULL,
                timestamp TEXT NOT NULL,
                description TEXT,
                FOREIGN KEY (account_number) REFERENCES accounts(account_number)
            );
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(createAccountsTable);
            stmt.execute(createTransactionsTable);
            logger.info("Database initialized successfully at URL: {}", dbUrl);

        } catch (SQLException e) {
            logger.error("Database initialization error: {}", e.getMessage(), e);
        }
    }
}
