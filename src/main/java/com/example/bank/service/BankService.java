package com.example.bank.service;

import com.example.bank.dao.AccountDao;
import com.example.bank.exception.AccountNotFoundException;
import com.example.bank.exception.InsufficientFundsException;
import com.example.bank.model.Account;
import com.example.bank.model.CurrentAccount;
import com.example.bank.model.SavingsAccount;

/**
 * BankService coordinates banking business logic and delegates
 * permanent storage operations to AccountDao (SQLite Database).
 */
public class BankService {
    private final AccountDao accountDao;

    public BankService() {
        this.accountDao = new AccountDao();
    }

    /**
     * Creates a new account and persists it in the SQLite database.
     */
    public Account createAccount(String accountNumber, String firstName, String lastName,
                                 String phoneNumber, String password, double initialBalance, String accountType) {

        // Validate if account already exists
        try {
            accountDao.findByAccountNumber(accountNumber);
            throw new IllegalArgumentException("Account with account number " + accountNumber + " already exists.");
        } catch (AccountNotFoundException ignored) {
            // Expected - account number is available
        }

        // Validate if phone number already exists
        try {
            accountDao.findByPhoneNumber(phoneNumber);
            throw new IllegalArgumentException("An account with this phone number already exists.");
        } catch (AccountNotFoundException ignored) {
            // Expected - phone number is available
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

        // Save to SQLite database
        accountDao.saveAccount(account, accountType);
        return account;
    }

    /**
     * Retrieves an account by its phone number from the database.
     */
    public Account getAccountByPhoneNumber(String phoneNumber) throws AccountNotFoundException {
        return accountDao.findByPhoneNumber(phoneNumber);
    }

    /**
     * Retrieves an account by its account number from the database.
     */
    public Account getAccount(String accountNumber) throws AccountNotFoundException {
        return accountDao.findByAccountNumber(accountNumber);
    }

    /**
     * Deposits money into the specified account and persists to database.
     */
    public void deposit(String accountNumber, double amount) throws AccountNotFoundException {
        accountDao.deposit(accountNumber, amount);
    }

    /**
     * Withdraws money from the specified account and persists to database.
     */
    public void withdraw(String accountNumber, double amount) throws AccountNotFoundException, InsufficientFundsException {
        accountDao.withdraw(accountNumber, amount);
    }

    /**
     * Displays details of the specified account.
     */
    public void displayAccountDetails(String accountNumber) throws AccountNotFoundException {
        Account account = getAccount(accountNumber);
        account.displayAccountDetails();
    }

    /**
     * Atomically transfers money from source account to destination account in SQLite.
     */
    public void transfer(String sourceAccountNumber, String destinationAccountNumber, double amount)
            throws AccountNotFoundException, InsufficientFundsException {
        accountDao.transfer(sourceAccountNumber, destinationAccountNumber, amount);
    }
}
