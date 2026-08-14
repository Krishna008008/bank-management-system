package com.example.bank.service;

import com.example.bank.exception.AccountNotFoundException;
import com.example.bank.exception.InsufficientFundsException;
import com.example.bank.model.Account;
import com.example.bank.model.CurrentAccount;
import com.example.bank.model.SavingsAccount;

import java.util.HashMap;
import java.util.Map;

/**
 * BankService manages the accounts and provides banking operations.
 * It uses a HashMap to store accounts (key: accountNumber, value: Account object).
 */
public class BankService {
    // Map to store accounts
    private Map<String, Account> accounts;

    public BankService() {
        this.accounts = new HashMap<>();
    }

    /**
     * Creates a new account and adds it to the bank.
     * @param accountNumber The account number
     * @param firstName The first name of the account holder
     * @param lastName The last name of the account holder
     * @param phoneNumber The phone number of the account holder
     * @param password The password/PIN of the account holder
     * @param initialBalance The initial balance
     * @param accountType The type of account ("Savings" or "Current")
     * @return The created account
     */
    public Account createAccount(String accountNumber, String firstName, String lastName, String phoneNumber, String password, double initialBalance, String accountType) {
        if (accounts.containsKey(accountNumber)) {
            throw new IllegalArgumentException("Account with account number " + accountNumber + " already exists.");
        }

        Account account;
        if (accountType.equalsIgnoreCase("Savings")) {
            if (initialBalance < SavingsAccount.MINIMUM_BALANCE) {
                throw new IllegalArgumentException("Savings account requires a minimum initial balance of $" + SavingsAccount.MINIMUM_BALANCE + ".");
            }
            account = new SavingsAccount(accountNumber, firstName, lastName, phoneNumber, password, initialBalance);
        } else if (accountType.equalsIgnoreCase("Current")) {
            if (initialBalance < 0.0) {
                throw new IllegalArgumentException("Initial balance cannot be negative.");
            }
            account = new CurrentAccount(accountNumber, firstName, lastName, phoneNumber, password, initialBalance);
        } else {
            throw new IllegalArgumentException("Invalid account type. Use 'Savings' or 'Current'.");
        }
        accounts.put(accountNumber, account);
        return account;
    }

    /**
     * Retrieves an account by its phone number.
     * @param phoneNumber The phone number to search for
     * @return The account if found
     * @throws AccountNotFoundException if the account is not found
     */
    public Account getAccountByPhoneNumber(String phoneNumber) throws AccountNotFoundException {
        for (Account account : accounts.values()) {
            if (account.getPhoneNumber().equals(phoneNumber)) {
                return account;
            }
        }
        throw new AccountNotFoundException("No account found associated with phone number: " + phoneNumber);
    }

    /**
     * Retrieves an account by its account number.
     * @param accountNumber The account number to search for
     * @return The account if found
     * @throws AccountNotFoundException if the account is not found
     */
    public Account getAccount(String accountNumber) throws AccountNotFoundException {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException("Account not found with account number: " + accountNumber);
        }
        return account;
    }

    /**
     * Deposits money into the specified account.
     * @param accountNumber The account number
     * @param amount The amount to deposit
     * @throws AccountNotFoundException if the account is not found
     */
    public void deposit(String accountNumber, double amount) throws AccountNotFoundException {
        Account account = getAccount(accountNumber);
        account.deposit(amount);
    }

    /**
     * Withdraws money from the specified account.
     * @param accountNumber The account number
     * @param amount The amount to withdraw
     * @throws AccountNotFoundException if the account is not found
     * @throws InsufficientFundsException if there are insufficient funds for the withdrawal
     */
    public void withdraw(String accountNumber, double amount) throws AccountNotFoundException, InsufficientFundsException {
        Account account = getAccount(accountNumber);
        account.withdraw(amount);
    }

    /**
     * Displays the details of the specified account.
     * @param accountNumber The account number
     * @throws AccountNotFoundException if the account is not found
     */
    public void displayAccountDetails(String accountNumber) throws AccountNotFoundException {
        Account account = getAccount(accountNumber);
        account.displayAccountDetails();
    }

    /**
     * Transfers money from one account to another.
     * @param sourceAccountNumber The account number to transfer from
     * @param destinationAccountNumber The account number to transfer to
     * @param amount The amount to transfer
     * @throws AccountNotFoundException if either account doesn't exist
     * @throws InsufficientFundsException if source account has insufficient balance
     */
    public void transfer(String sourceAccountNumber, String destinationAccountNumber, double amount)
            throws AccountNotFoundException, InsufficientFundsException {
        if (sourceAccountNumber.equals(destinationAccountNumber)) {
            throw new IllegalArgumentException("Source and destination account numbers must be different.");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive.");
        }

        // Check if source account exists
        Account sourceAccount = getAccount(sourceAccountNumber);

        // Check if destination account exists
        Account destinationAccount = getAccount(destinationAccountNumber);

        // Check if source account has sufficient balance
        if (sourceAccount.getBalance() < amount) {
            throw new InsufficientFundsException("Insufficient funds in account " + sourceAccountNumber +
                                               ". Available balance: $" + sourceAccount.getBalance());
        }

        // Perform the transfer
        sourceAccount.withdraw(amount);
        destinationAccount.deposit(amount);

        System.out.println("Transferred $" + amount + " from account " + sourceAccountNumber +
                          " to account " + destinationAccountNumber);
    }
}
