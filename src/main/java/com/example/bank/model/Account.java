package com.example.bank.model;

import com.example.bank.exception.InsufficientFundsException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class representing a bank account.
 * This demonstrates the OOP concept of Abstraction.
 */
public abstract class Account {
    // Encapsulation: private fields
    private String accountNumber;
    private String accountHolderName;
    private double balance;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String password;

    // Transaction history: stores last 5 transactions
    private List<String> transactionHistory;
    private static final int MAX_TRANSACTIONS = 5;
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Constructor
    public Account(String accountNumber, String firstName, String lastName, String phoneNumber, String password, double initialBalance) {
        this.accountNumber = accountNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.accountHolderName = firstName + " " + lastName;
        this.balance = initialBalance;
        this.transactionHistory = new ArrayList<>();
        // Record initial balance as first transaction
        addTransaction("Initial deposit: $" + initialBalance);
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

    public double getBalance() {
        return balance;
    }

    protected void setBalance(double balance) {
        this.balance = balance;
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
    public abstract void withdraw(double amount) throws InsufficientFundsException;

    /**
     * Method for depositing money.
     */
    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
            System.out.println("Deposited: $" + amount);
            addTransaction("Deposit: $" + amount);
        } else {
            System.out.println("Deposit amount must be positive.");
        }
    }

    /**
     * Method to display account details.
     */
    public void displayAccountDetails() {
        System.out.println("Account Number: " + accountNumber);
        System.out.println("Account Holder: " + accountHolderName);
        System.out.println("Balance: $" + balance);
    }

    /**
     * Prints a mini statement showing the last 5 transactions.
     */
    public void printMiniStatement() {
        System.out.println("=== Mini Statement for Account: " + accountNumber + " ===");
        System.out.println("Account Holder: " + accountHolderName);
        System.out.println("Current Balance: $" + balance);
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
