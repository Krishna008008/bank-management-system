package com.example.bank.model;

import com.example.bank.exception.InsufficientFundsException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class representing a bank account.
 * This demonstrates the OOP concept of Abstraction.
 * Monetary balances are stored as BigDecimal to avoid binary floating-point rounding errors.
 */
public abstract class Account {
    // Encapsulation: private fields
    private String accountNumber;
    private String accountHolderName;
    private BigDecimal balance;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String password;

    // Transaction history: stores last 5 transactions
    private List<String> transactionHistory;
    private static final int MAX_TRANSACTIONS = 5;
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Canonical Constructor with BigDecimal
    public Account(String accountNumber, String firstName, String lastName, String phoneNumber, String password, BigDecimal initialBalance) {
        this.accountNumber = accountNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.accountHolderName = firstName + " " + lastName;
        this.balance = initialBalance != null ? initialBalance.setScale(2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        this.transactionHistory = new ArrayList<>();
    }

    // Overloaded Constructor for double
    public Account(String accountNumber, String firstName, String lastName, String phoneNumber, String password, double initialBalance) {
        this(accountNumber, firstName, lastName, phoneNumber, password, BigDecimal.valueOf(initialBalance));
    }

    public void setTransactionHistory(List<String> history) {
        this.transactionHistory = new ArrayList<>(history);
    }

    public List<String> getTransactionHistory() {
        return new ArrayList<>(this.transactionHistory);
    }

    // Getters and Setters (Encapsulation)
    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    protected void setBalance(BigDecimal balance) {
        this.balance = balance != null ? balance.setScale(2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Adds a transaction to the history list.
     * Maintains only the last 5 transactions.
     */
    protected void addTransaction(String transaction) {
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String formattedTransaction = timestamp + " - " + transaction;

        transactionHistory.add(formattedTransaction);

        // Keep only the last 5 transactions
        if (transactionHistory.size() > MAX_TRANSACTIONS) {
            transactionHistory.remove(0);
        }
    }

    /**
     * Abstract method for withdrawing money.
     * This will be implemented by subclasses (Polymorphism).
     * @throws InsufficientFundsException if there are insufficient funds for the withdrawal
     */
    public abstract void withdraw(BigDecimal amount) throws InsufficientFundsException;

    public void withdraw(double amount) throws InsufficientFundsException {
        withdraw(BigDecimal.valueOf(amount));
    }

    /**
     * Method for depositing money.
     */
    public void deposit(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal standardized = amount.setScale(2, RoundingMode.HALF_EVEN);
            balance = balance.add(standardized);
            System.out.println("Deposited: $" + standardized.toPlainString());
            addTransaction("Deposit: $" + standardized.toPlainString());
        } else {
            System.out.println("Deposit amount must be positive.");
        }
    }

    public void deposit(double amount) {
        deposit(BigDecimal.valueOf(amount));
    }

    /**
     * Method to display account details.
     */
    public void displayAccountDetails() {
        System.out.println("Account Number: " + accountNumber);
        System.out.println("Account Holder: " + accountHolderName);
        System.out.println("Balance: $" + balance.toPlainString());
    }

    /**
     * Prints a mini statement showing the last 5 transactions.
     */
    public void printMiniStatement() {
        System.out.println("=== Mini Statement for Account: " + accountNumber + " ===");
        System.out.println("Account Holder: " + accountHolderName);
        System.out.println("Current Balance: $" + balance.toPlainString());
        System.out.println("Last " + Math.min(transactionHistory.size(), MAX_TRANSACTIONS) + " transactions:");

        if (transactionHistory.isEmpty()) {
            System.out.println("No transactions yet.");
        } else {
            // Print transactions in reverse order (most recent first)
            for (int i = transactionHistory.size() - 1; i >= 0; i--) {
                System.out.println(transactionHistory.get(i));
            }
        }
        System.out.println("=========================================");
    }
}
