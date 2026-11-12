package com.example.bank.dao;

import com.example.bank.exception.AccountNotFoundException;
import com.example.bank.exception.InsufficientFundsException;
import com.example.bank.model.Account;
import com.example.bank.model.CurrentAccount;
import com.example.bank.model.SavingsAccount;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
 * AccountDao handles all database operations (CRUD) with audit logging.
 * Uses exact BigDecimal calculations for all financial amounts to prevent rounding drift.
 */
@Repository
public class AccountDao {
    private static final Logger logger = LogManager.getLogger(AccountDao.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AccountDao() {
        DatabaseManager.initializeDatabase();
    }

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
            conn.setAutoCommit(false);

            try (PreparedStatement stmtAcc = conn.prepareStatement(insertAccountSql);
                 PreparedStatement stmtTx = conn.prepareStatement(insertTxSql)) {

                String balanceStr = account.getBalance().setScale(2, RoundingMode.HALF_EVEN).toPlainString();

                stmtAcc.setString(1, account.getAccountNumber());
                stmtAcc.setString(2, account.getFirstName());
                stmtAcc.setString(3, account.getLastName());
                stmtAcc.setString(4, account.getPhoneNumber());
                stmtAcc.setString(5, account.getPassword());
                stmtAcc.setString(6, balanceStr);
                stmtAcc.setString(7, accountType);
                stmtAcc.executeUpdate();

                String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
                stmtTx.setString(1, account.getAccountNumber());
                stmtTx.setString(2, "INITIAL_DEPOSIT");
                stmtTx.setString(3, balanceStr);
                stmtTx.setString(4, balanceStr);
                stmtTx.setString(5, timestamp);
                stmtTx.setString(6, "Initial deposit: $" + balanceStr);
                stmtTx.executeUpdate();

                conn.commit();
                logger.info("Account {} successfully created and saved in DB (Type: {}, Balance: ${})",
                        account.getAccountNumber(), accountType, balanceStr);
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Failed to save account {}, rolled back: {}", account.getAccountNumber(), e.getMessage());
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save account: " + e.getMessage(), e);
        }
    }

    public Account findByAccountNumber(String accountNumber) throws AccountNotFoundException {
        String sql = "SELECT * FROM accounts WHERE account_number = ?;";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, accountNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAccount(rs, conn);
                } else {
                    logger.warn("Lookup failed: Account {} not found", accountNumber);
                    throw new AccountNotFoundException("Account not found with account number: " + accountNumber);
                }
            }
        } catch (SQLException e) {
            logger.error("DB error finding account {}: {}", accountNumber, e.getMessage());
            throw new RuntimeException("Database error finding account: " + e.getMessage(), e);
        }
    }

    public Account findByPhoneNumber(String phoneNumber) throws AccountNotFoundException {
        String sql = "SELECT * FROM accounts WHERE phone_number = ?;";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, phoneNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAccount(rs, conn);
                } else {
                    logger.warn("Lookup failed: No account for phone {}", phoneNumber);
                    throw new AccountNotFoundException("No account found associated with phone number: " + phoneNumber);
                }
            }
        } catch (SQLException e) {
            logger.error("DB error finding phone {}: {}", phoneNumber, e.getMessage());
            throw new RuntimeException("Database error finding account by phone: " + e.getMessage(), e);
        }
    }

    public void deposit(String accountNumber, BigDecimal amount) throws AccountNotFoundException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }

        BigDecimal standardizedAmount = amount.setScale(2, RoundingMode.HALF_EVEN);
        String selectSql = "SELECT balance FROM accounts WHERE account_number = ?;";
        String updateSql = "UPDATE accounts SET balance = ? WHERE account_number = ?;";
        String txSql = """
            INSERT INTO transactions (account_number, transaction_type, amount, balance_after, timestamp, description)
            VALUES (?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal currentBalance;
                try (PreparedStatement stmtSelect = conn.prepareStatement(selectSql)) {
                    stmtSelect.setString(1, accountNumber);
                    try (ResultSet rs = stmtSelect.executeQuery()) {
                        if (rs.next()) {
                            currentBalance = new BigDecimal(rs.getString("balance")).setScale(2, RoundingMode.HALF_EVEN);
                        } else {
                            throw new AccountNotFoundException("Account not found with account number: " + accountNumber);
                        }
                    }
                }

                BigDecimal newBalance = currentBalance.add(standardizedAmount);

                try (PreparedStatement stmtUpdate = conn.prepareStatement(updateSql)) {
                    stmtUpdate.setString(1, newBalance.toPlainString());
                    stmtUpdate.setString(2, accountNumber);
                    stmtUpdate.executeUpdate();
                }

                String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
                try (PreparedStatement stmtTx = conn.prepareStatement(txSql)) {
                    stmtTx.setString(1, accountNumber);
                    stmtTx.setString(2, "DEPOSIT");
                    stmtTx.setString(3, standardizedAmount.toPlainString());
                    stmtTx.setString(4, newBalance.toPlainString());
                    stmtTx.setString(5, timestamp);
                    stmtTx.setString(6, "Deposit: $" + standardizedAmount.toPlainString());
                    stmtTx.executeUpdate();
                }

                conn.commit();
                logger.info("Deposit of ${} to account {} successful. New balance: ${}",
                        standardizedAmount.toPlainString(), accountNumber, newBalance.toPlainString());
            } catch (Exception e) {
                conn.rollback();
                logger.error("Deposit to {} failed, rolled back: {}", accountNumber, e.getMessage());
                if (e instanceof AccountNotFoundException) throw (AccountNotFoundException) e;
                throw new RuntimeException("Deposit transaction failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during deposit: " + e.getMessage(), e);
        }
    }

    public void deposit(String accountNumber, double amount) throws AccountNotFoundException {
        deposit(accountNumber, BigDecimal.valueOf(amount));
    }

    public void withdraw(String accountNumber, BigDecimal amount)
            throws AccountNotFoundException, InsufficientFundsException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InsufficientFundsException("Withdrawal amount must be positive.");
        }

        BigDecimal standardizedAmount = amount.setScale(2, RoundingMode.HALF_EVEN);
        String selectSql = "SELECT balance, account_type FROM accounts WHERE account_number = ?;";
        String updateSql = "UPDATE accounts SET balance = ? WHERE account_number = ?;";
        String txSql = """
            INSERT INTO transactions (account_number, transaction_type, amount, balance_after, timestamp, description)
            VALUES (?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal currentBalance;
                String accountType;

                try (PreparedStatement stmtSelect = conn.prepareStatement(selectSql)) {
                    stmtSelect.setString(1, accountNumber);
                    try (ResultSet rs = stmtSelect.executeQuery()) {
                        if (rs.next()) {
                            currentBalance = new BigDecimal(rs.getString("balance")).setScale(2, RoundingMode.HALF_EVEN);
                            accountType = rs.getString("account_type");
                        } else {
                            throw new AccountNotFoundException("Account not found with account number: " + accountNumber);
                        }
                    }
                }

                BigDecimal newBalance = currentBalance.subtract(standardizedAmount);

                if (accountType.equalsIgnoreCase("Savings")) {
                    if (newBalance.compareTo(SavingsAccount.MINIMUM_BALANCE) < 0) {
                        logger.warn("Withdrawal rejected for Savings Account {}: Balance would drop to ${} (Min: ${})",
                                accountNumber, newBalance.toPlainString(), SavingsAccount.MINIMUM_BALANCE.toPlainString());
                        throw new InsufficientFundsException("Withdrawal failed. Minimum balance of $" +
                                SavingsAccount.MINIMUM_BALANCE.toPlainString() + " must be maintained.");
                    }
                } else {
                    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                        logger.warn("Withdrawal rejected for Current Account {}: Insufficient funds for amount ${}",
                                accountNumber, standardizedAmount.toPlainString());
                        throw new InsufficientFundsException("Withdrawal failed. Insufficient funds.");
                    }
                }

                try (PreparedStatement stmtUpdate = conn.prepareStatement(updateSql)) {
                    stmtUpdate.setString(1, newBalance.toPlainString());
                    stmtUpdate.setString(2, accountNumber);
                    stmtUpdate.executeUpdate();
                }

                String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
                try (PreparedStatement stmtTx = conn.prepareStatement(txSql)) {
                    stmtTx.setString(1, accountNumber);
                    stmtTx.setString(2, "WITHDRAW");
                    stmtTx.setString(3, standardizedAmount.toPlainString());
                    stmtTx.setString(4, newBalance.toPlainString());
                    stmtTx.setString(5, timestamp);
                    stmtTx.setString(6, "Withdrawal: $" + standardizedAmount.toPlainString());
                    stmtTx.executeUpdate();
                }

                conn.commit();
                logger.info("Withdrawal of ${} from account {} successful. New balance: ${}",
                        standardizedAmount.toPlainString(), accountNumber, newBalance.toPlainString());
            } catch (Exception e) {
                conn.rollback();
                logger.error("Withdrawal from {} failed, rolled back: {}", accountNumber, e.getMessage());
                if (e instanceof AccountNotFoundException) throw (AccountNotFoundException) e;
                if (e instanceof InsufficientFundsException) throw (InsufficientFundsException) e;
                throw new RuntimeException("Withdrawal transaction failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during withdrawal: " + e.getMessage(), e);
        }
    }

    public void withdraw(String accountNumber, double amount)
            throws AccountNotFoundException, InsufficientFundsException {
        withdraw(accountNumber, BigDecimal.valueOf(amount));
    }

    public void transfer(String sourceAccountNumber, String destinationAccountNumber, BigDecimal amount)
            throws AccountNotFoundException, InsufficientFundsException {
        if (sourceAccountNumber.equals(destinationAccountNumber)) {
            throw new IllegalArgumentException("Source and destination account numbers must be different.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive.");
        }

        BigDecimal standardizedAmount = amount.setScale(2, RoundingMode.HALF_EVEN);
        String selectSourceSql = "SELECT balance, account_type FROM accounts WHERE account_number = ?;";
        String selectDestSql = "SELECT balance FROM accounts WHERE account_number = ?;";
        String updateSql = "UPDATE accounts SET balance = ? WHERE account_number = ?;";
        String txSql = """
            INSERT INTO transactions (account_number, transaction_type, amount, balance_after, timestamp, description)
            VALUES (?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal srcBalance;
                String srcType;
                try (PreparedStatement stmtSrc = conn.prepareStatement(selectSourceSql)) {
                    stmtSrc.setString(1, sourceAccountNumber);
                    try (ResultSet rs = stmtSrc.executeQuery()) {
                        if (rs.next()) {
                            srcBalance = new BigDecimal(rs.getString("balance")).setScale(2, RoundingMode.HALF_EVEN);
                            srcType = rs.getString("account_type");
                        } else {
                            throw new AccountNotFoundException("Source account not found: " + sourceAccountNumber);
                        }
                    }
                }

                BigDecimal srcNewBalance = srcBalance.subtract(standardizedAmount);
                if (srcType.equalsIgnoreCase("Savings") && srcNewBalance.compareTo(SavingsAccount.MINIMUM_BALANCE) < 0) {
                    logger.warn("Transfer rejected from {}: Minimum balance violation", sourceAccountNumber);
                    throw new InsufficientFundsException("Insufficient funds. Minimum balance of $" +
                            SavingsAccount.MINIMUM_BALANCE.toPlainString() + " must be maintained.");
                } else if (srcNewBalance.compareTo(BigDecimal.ZERO) < 0) {
                    logger.warn("Transfer rejected from {}: Insufficient funds", sourceAccountNumber);
                    throw new InsufficientFundsException("Insufficient funds in account " + sourceAccountNumber);
                }

                BigDecimal destBalance;
                try (PreparedStatement stmtDest = conn.prepareStatement(selectDestSql)) {
                    stmtDest.setString(1, destinationAccountNumber);
                    try (ResultSet rs = stmtDest.executeQuery()) {
                        if (rs.next()) {
                            destBalance = new BigDecimal(rs.getString("balance")).setScale(2, RoundingMode.HALF_EVEN);
                        } else {
                            throw new AccountNotFoundException("Destination account not found: " + destinationAccountNumber);
                        }
                    }
                }
                BigDecimal destNewBalance = destBalance.add(standardizedAmount);

                try (PreparedStatement stmtDeduct = conn.prepareStatement(updateSql)) {
                    stmtDeduct.setString(1, srcNewBalance.toPlainString());
                    stmtDeduct.setString(2, sourceAccountNumber);
                    stmtDeduct.executeUpdate();
                }

                try (PreparedStatement stmtAdd = conn.prepareStatement(updateSql)) {
                    stmtAdd.setString(1, destNewBalance.toPlainString());
                    stmtAdd.setString(2, destinationAccountNumber);
                    stmtAdd.executeUpdate();
                }

                String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
                try (PreparedStatement stmtTx = conn.prepareStatement(txSql)) {
                    stmtTx.setString(1, sourceAccountNumber);
                    stmtTx.setString(2, "TRANSFER_OUT");
                    stmtTx.setString(3, standardizedAmount.toPlainString());
                    stmtTx.setString(4, srcNewBalance.toPlainString());
                    stmtTx.setString(5, timestamp);
                    stmtTx.setString(6, "Transfer to " + destinationAccountNumber + ": -$" + standardizedAmount.toPlainString());
                    stmtTx.executeUpdate();

                    stmtTx.setString(1, destinationAccountNumber);
                    stmtTx.setString(2, "TRANSFER_IN");
                    stmtTx.setString(3, standardizedAmount.toPlainString());
                    stmtTx.setString(4, destNewBalance.toPlainString());
                    stmtTx.setString(5, timestamp);
                    stmtTx.setString(6, "Transfer from " + sourceAccountNumber + ": +$" + standardizedAmount.toPlainString());
                    stmtTx.executeUpdate();
                }

                conn.commit();
                logger.info("Transfer of ${} from {} to {} completed successfully.",
                        standardizedAmount.toPlainString(), sourceAccountNumber, destinationAccountNumber);
            } catch (Exception e) {
                conn.rollback();
                logger.error("Transfer from {} to {} failed, rolled back: {}",
                        sourceAccountNumber, destinationAccountNumber, e.getMessage());
                if (e instanceof AccountNotFoundException) throw (AccountNotFoundException) e;
                if (e instanceof InsufficientFundsException) throw (InsufficientFundsException) e;
                if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
                throw new RuntimeException("Transfer transaction failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during transfer: " + e.getMessage(), e);
        }
    }

    public void transfer(String sourceAccountNumber, String destinationAccountNumber, double amount)
            throws AccountNotFoundException, InsufficientFundsException {
        transfer(sourceAccountNumber, destinationAccountNumber, BigDecimal.valueOf(amount));
    }

    private Account mapRowToAccount(ResultSet rs, Connection conn) throws SQLException {
        String accNum = rs.getString("account_number");
        String firstName = rs.getString("first_name");
        String lastName = rs.getString("last_name");
        String phone = rs.getString("phone_number");
        String password = rs.getString("password_hash");
        BigDecimal balance = new BigDecimal(rs.getString("balance")).setScale(2, RoundingMode.HALF_EVEN);
        String accountType = rs.getString("account_type");

        Account account;
        if (accountType.equalsIgnoreCase("Savings")) {
            account = new SavingsAccount(accNum, firstName, lastName, phone, password, balance);
        } else {
            account = new CurrentAccount(accNum, firstName, lastName, phone, password, balance);
        }

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
        Collections.reverse(transactions);
        account.setTransactionHistory(transactions);

        return account;
    }
}
