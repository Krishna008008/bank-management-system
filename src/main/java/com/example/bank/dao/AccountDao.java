package com.example.bank.dao;

import com.example.bank.exception.AccountNotFoundException;
import com.example.bank.exception.InsufficientFundsException;
import com.example.bank.model.Account;
import com.example.bank.model.CurrentAccount;
import com.example.bank.model.SavingsAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AccountDao (Data Access Object) handles all database operations (CRUD)
 * for Accounts and Transactions in the SQLite database.
 */
public class AccountDao {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AccountDao() {
        // Ensure database tables exist upon DAO creation
        DatabaseManager.initializeDatabase();
    }

    /**
     * Saves a new account and its initial deposit record into SQLite.
     */
    public void saveAccount(Account account, String accountType) {
        String insertAccountSql = """
            INSERT INTO accounts (account_number, first_name, last_name, phone_number, password_hash, balance, account_type)
            VALUES (?, ?, ?, ?, ?, ?, ?);
        """;

        String insertTxSql = """
            INSERT INTO transactions (account_number, transaction_type, amount, balance_after, timestamp, description)
            VALUES (?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Start database transaction

            try (PreparedStatement stmtAcc = conn.prepareStatement(insertAccountSql);
                 PreparedStatement stmtTx = conn.prepareStatement(insertTxSql)) {

                // 1. Insert account record
                stmtAcc.setString(1, account.getAccountNumber());
                stmtAcc.setString(2, account.getFirstName());
                stmtAcc.setString(3, account.getLastName());
                stmtAcc.setString(4, account.getPhoneNumber());
                stmtAcc.setString(5, account.getPassword());
                stmtAcc.setDouble(6, account.getBalance());
                stmtAcc.setString(7, accountType);
                stmtAcc.executeUpdate();

                // 2. Insert initial deposit transaction
                String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
                stmtTx.setString(1, account.getAccountNumber());
                stmtTx.setString(2, "INITIAL_DEPOSIT");
                stmtTx.setDouble(3, account.getBalance());
                stmtTx.setDouble(4, account.getBalance());
                stmtTx.setString(5, timestamp);
                stmtTx.setString(6, "Initial deposit: $" + account.getBalance());
                stmtTx.executeUpdate();

                conn.commit(); // Commit both operations
            } catch (SQLException e) {
                conn.rollback(); // Rollback if any error occurs
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save account: " + e.getMessage(), e);
        }
    }

    /**
     * Finds an account by its account number.
     */
    public Account findByAccountNumber(String accountNumber) throws AccountNotFoundException {
        String sql = "SELECT * FROM accounts WHERE account_number = ?;";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, accountNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAccount(rs, conn);
                } else {
                    throw new AccountNotFoundException("Account not found with account number: " + accountNumber);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error finding account: " + e.getMessage(), e);
        }
    }

    /**
     * Finds an account by its registered phone number.
     */
    public Account findByPhoneNumber(String phoneNumber) throws AccountNotFoundException {
        String sql = "SELECT * FROM accounts WHERE phone_number = ?;";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, phoneNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAccount(rs, conn);
                } else {
                    throw new AccountNotFoundException("No account found associated with phone number: " + phoneNumber);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error finding account by phone: " + e.getMessage(), e);
        }
    }

    /**
     * Deposits an amount into an account and records the transaction.
     */
    public void deposit(String accountNumber, double amount) throws AccountNotFoundException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }

        String selectSql = "SELECT balance FROM accounts WHERE account_number = ?;";
        String updateSql = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?;";
        String txSql = """
            INSERT INTO transactions (account_number, transaction_type, amount, balance_after, timestamp, description)
            VALUES (?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                double currentBalance = 0;
                try (PreparedStatement stmtSelect = conn.prepareStatement(selectSql)) {
                    stmtSelect.setString(1, accountNumber);
                    try (ResultSet rs = stmtSelect.executeQuery()) {
                        if (rs.next()) {
                            currentBalance = rs.getDouble("balance");
                        } else {
                            throw new AccountNotFoundException("Account not found with account number: " + accountNumber);
                        }
                    }
                }

                double newBalance = currentBalance + amount;

                // Update account balance
                try (PreparedStatement stmtUpdate = conn.prepareStatement(updateSql)) {
                    stmtUpdate.setDouble(1, amount);
                    stmtUpdate.setString(2, accountNumber);
                    stmtUpdate.executeUpdate();
                }

                // Record transaction
                String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
                try (PreparedStatement stmtTx = conn.prepareStatement(txSql)) {
                    stmtTx.setString(1, accountNumber);
                    stmtTx.setString(2, "DEPOSIT");
                    stmtTx.setDouble(3, amount);
                    stmtTx.setDouble(4, newBalance);
                    stmtTx.setString(5, timestamp);
                    stmtTx.setString(6, "Deposit: $" + amount);
                    stmtTx.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof AccountNotFoundException) throw (AccountNotFoundException) e;
                throw new RuntimeException("Deposit transaction failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during deposit: " + e.getMessage(), e);
        }
    }

    /**
     * Withdraws an amount from an account and records the transaction.
     */
    public void withdraw(String accountNumber, double amount)
            throws AccountNotFoundException, InsufficientFundsException {
        if (amount <= 0) {
            throw new InsufficientFundsException("Withdrawal amount must be positive.");
        }

        String selectSql = "SELECT balance, account_type FROM accounts WHERE account_number = ?;";
        String updateSql = "UPDATE accounts SET balance = balance - ? WHERE account_number = ?;";
        String txSql = """
            INSERT INTO transactions (account_number, transaction_type, amount, balance_after, timestamp, description)
            VALUES (?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                double currentBalance;
                String accountType;

                try (PreparedStatement stmtSelect = conn.prepareStatement(selectSql)) {
                    stmtSelect.setString(1, accountNumber);
                    try (ResultSet rs = stmtSelect.executeQuery()) {
                        if (rs.next()) {
                            currentBalance = rs.getDouble("balance");
                            accountType = rs.getString("account_type");
                        } else {
                            throw new AccountNotFoundException("Account not found with account number: " + accountNumber);
                        }
                    }
                }

                double newBalance = currentBalance - amount;

                // Enforce account balance rules
                if (accountType.equalsIgnoreCase("Savings")) {
                    if (newBalance < SavingsAccount.MINIMUM_BALANCE) {
                        throw new InsufficientFundsException("Withdrawal failed. Minimum balance of $" +
                                SavingsAccount.MINIMUM_BALANCE + " must be maintained.");
                    }
                } else {
                    if (newBalance < 0) {
                        throw new InsufficientFundsException("Withdrawal failed. Insufficient funds.");
                    }
                }

                // Update account balance
                try (PreparedStatement stmtUpdate = conn.prepareStatement(updateSql)) {
                    stmtUpdate.setDouble(1, amount);
                    stmtUpdate.setString(2, accountNumber);
                    stmtUpdate.executeUpdate();
                }

                // Record transaction
                String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
                try (PreparedStatement stmtTx = conn.prepareStatement(txSql)) {
                    stmtTx.setString(1, accountNumber);
                    stmtTx.setString(2, "WITHDRAW");
                    stmtTx.setDouble(3, amount);
                    stmtTx.setDouble(4, newBalance);
                    stmtTx.setString(5, timestamp);
                    stmtTx.setString(6, "Withdrawal: $" + amount);
                    stmtTx.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof AccountNotFoundException) throw (AccountNotFoundException) e;
                if (e instanceof InsufficientFundsException) throw (InsufficientFundsException) e;
                throw new RuntimeException("Withdrawal transaction failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during withdrawal: " + e.getMessage(), e);
        }
    }

    /**
     * Atomically transfers money from source account to destination account (ACID transaction).
     */
    public void transfer(String sourceAccountNumber, String destinationAccountNumber, double amount)
            throws AccountNotFoundException, InsufficientFundsException {
        if (sourceAccountNumber.equals(destinationAccountNumber)) {
            throw new IllegalArgumentException("Source and destination account numbers must be different.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive.");
        }

        String selectSourceSql = "SELECT balance, account_type FROM accounts WHERE account_number = ?;";
        String selectDestSql = "SELECT balance FROM accounts WHERE account_number = ?;";
        String deductSql = "UPDATE accounts SET balance = balance - ? WHERE account_number = ?;";
        String addSql = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?;";
        String txSql = """
            INSERT INTO transactions (account_number, transaction_type, amount, balance_after, timestamp, description)
            VALUES (?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Atomic transaction
            try {
                // 1. Verify source account
                double srcBalance;
                String srcType;
                try (PreparedStatement stmtSrc = conn.prepareStatement(selectSourceSql)) {
                    stmtSrc.setString(1, sourceAccountNumber);
                    try (ResultSet rs = stmtSrc.executeQuery()) {
                        if (rs.next()) {
                            srcBalance = rs.getDouble("balance");
                            srcType = rs.getString("account_type");
                        } else {
                            throw new AccountNotFoundException("Source account not found: " + sourceAccountNumber);
                        }
                    }
                }

                // 2. Check sufficient funds
                double srcNewBalance = srcBalance - amount;
                if (srcType.equalsIgnoreCase("Savings") && srcNewBalance < SavingsAccount.MINIMUM_BALANCE) {
                    throw new InsufficientFundsException("Insufficient funds. Minimum balance of $" +
                            SavingsAccount.MINIMUM_BALANCE + " must be maintained.");
                } else if (srcNewBalance < 0) {
                    throw new InsufficientFundsException("Insufficient funds in account " + sourceAccountNumber);
                }

                // 3. Verify destination account
                double destBalance;
                try (PreparedStatement stmtDest = conn.prepareStatement(selectDestSql)) {
                    stmtDest.setString(1, destinationAccountNumber);
                    try (ResultSet rs = stmtDest.executeQuery()) {
                        if (rs.next()) {
                            destBalance = rs.getDouble("balance");
                        } else {
                            throw new AccountNotFoundException("Destination account not found: " + destinationAccountNumber);
                        }
                    }
                }
                double destNewBalance = destBalance + amount;

                // 4. Deduct from source
                try (PreparedStatement stmtDeduct = conn.prepareStatement(deductSql)) {
                    stmtDeduct.setDouble(1, amount);
                    stmtDeduct.setString(2, sourceAccountNumber);
                    stmtDeduct.executeUpdate();
                }

                // 5. Add to destination
                try (PreparedStatement stmtAdd = conn.prepareStatement(addSql)) {
                    stmtAdd.setDouble(1, amount);
                    stmtAdd.setString(2, destinationAccountNumber);
                    stmtAdd.executeUpdate();
                }

                // 6. Record source transaction
                String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
                try (PreparedStatement stmtTx = conn.prepareStatement(txSql)) {
                    stmtTx.setString(1, sourceAccountNumber);
                    stmtTx.setString(2, "TRANSFER_OUT");
                    stmtTx.setDouble(3, amount);
                    stmtTx.setDouble(4, srcNewBalance);
                    stmtTx.setString(5, timestamp);
                    stmtTx.setString(6, "Transfer to " + destinationAccountNumber + ": -$" + amount);
                    stmtTx.executeUpdate();

                    // 7. Record destination transaction
                    stmtTx.setString(1, destinationAccountNumber);
                    stmtTx.setString(2, "TRANSFER_IN");
                    stmtTx.setDouble(3, amount);
                    stmtTx.setDouble(4, destNewBalance);
                    stmtTx.setString(5, timestamp);
                    stmtTx.setString(6, "Transfer from " + sourceAccountNumber + ": +$" + amount);
                    stmtTx.executeUpdate();
                }

                conn.commit(); // Commit all changes together
            } catch (Exception e) {
                conn.rollback(); // Rollback if anything fails
                if (e instanceof AccountNotFoundException) throw (AccountNotFoundException) e;
                if (e instanceof InsufficientFundsException) throw (InsufficientFundsException) e;
                if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
                throw new RuntimeException("Transfer transaction failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during transfer: " + e.getMessage(), e);
        }
    }

    /**
     * Maps a database ResultSet row to an Account object and loads its recent transactions.
     */
    private Account mapRowToAccount(ResultSet rs, Connection conn) throws SQLException {
        String accNum = rs.getString("account_number");
        String firstName = rs.getString("first_name");
        String lastName = rs.getString("last_name");
        String phone = rs.getString("phone_number");
        String password = rs.getString("password_hash");
        double balance = rs.getDouble("balance");
        String accountType = rs.getString("account_type");

        Account account;
        if (accountType.equalsIgnoreCase("Savings")) {
            account = new SavingsAccount(accNum, firstName, lastName, phone, password, balance);
        } else {
            account = new CurrentAccount(accNum, firstName, lastName, phone, password, balance);
        }

        // Load last 5 transactions for mini-statement
        String txSql = "SELECT description, timestamp FROM transactions WHERE account_number = ? ORDER BY id DESC LIMIT 5;";
        List<String> transactions = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(txSql)) {
            stmt.setString(1, accNum);
            try (ResultSet txRs = stmt.executeQuery()) {
                while (txRs.next()) {
                    String time = txRs.getString("timestamp");
                    String desc = txRs.getString("description");
                    transactions.add(time + " - " + desc);
                }
            }
        }
        // Reverse so that oldest is first in list (printMiniStatement prints newest on top)
        Collections.reverse(transactions);
        account.setTransactionHistory(transactions);

        return account;
    }
}
