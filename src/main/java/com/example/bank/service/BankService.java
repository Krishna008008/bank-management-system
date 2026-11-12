package com.example.bank.service;

import com.example.bank.dao.AccountDao;
import com.example.bank.exception.AccountNotFoundException;
import com.example.bank.exception.InsufficientFundsException;
import com.example.bank.model.Account;
import com.example.bank.model.CurrentAccount;
import com.example.bank.model.SavingsAccount;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BankService coordinates banking business logic with Log4j2 audit logging.
 * Uses exact BigDecimal amounts for financial operations.
 */
@Service
public class BankService {
    private static final Logger logger = LogManager.getLogger(BankService.class);
    private final AccountDao accountDao;

    @Autowired
    public BankService(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    public BankService() {
        this(new AccountDao());
    }

    public Account createAccount(String accountNumber, String firstName, String lastName,
                                 String phoneNumber, String password, BigDecimal initialBalance, String accountType) {

        try {
            accountDao.findByAccountNumber(accountNumber);
            logger.warn("Account creation rejected: Account {} already exists", accountNumber);
            throw new IllegalArgumentException("Account with account number " + accountNumber + " already exists.");
        } catch (AccountNotFoundException ignored) {
        }

        try {
            accountDao.findByPhoneNumber(phoneNumber);
            logger.warn("Account creation rejected: Phone {} already registered", phoneNumber);
            throw new IllegalArgumentException("An account with this phone number already exists.");
        } catch (AccountNotFoundException ignored) {
        }

        if (initialBalance == null) {
            throw new IllegalArgumentException("Initial balance cannot be null.");
        }

        BigDecimal standardizedBalance = initialBalance.setScale(2, RoundingMode.HALF_EVEN);
        Account account;
        if (accountType.equalsIgnoreCase("Savings")) {
            if (standardizedBalance.compareTo(SavingsAccount.MINIMUM_BALANCE) < 0) {
                logger.warn("Savings creation rejected: Initial deposit ${} is below min ${}",
                        standardizedBalance.toPlainString(), SavingsAccount.MINIMUM_BALANCE.toPlainString());
                throw new IllegalArgumentException("Savings account requires a minimum initial balance of $" + SavingsAccount.MINIMUM_BALANCE.toPlainString() + ".");
            }
            account = new SavingsAccount(accountNumber, firstName, lastName, phoneNumber, password, standardizedBalance);
        } else if (accountType.equalsIgnoreCase("Current")) {
            if (standardizedBalance.compareTo(BigDecimal.ZERO) < 0) {
                logger.warn("Current account creation rejected: Negative balance ${}", standardizedBalance.toPlainString());
                throw new IllegalArgumentException("Initial balance cannot be negative.");
            }
            account = new CurrentAccount(accountNumber, firstName, lastName, phoneNumber, password, standardizedBalance);
        } else {
            logger.warn("Account creation rejected: Invalid type '{}'", accountType);
            throw new IllegalArgumentException("Invalid account type. Use 'Savings' or 'Current'.");
        }

        accountDao.saveAccount(account, accountType);
        return account;
    }

    public Account createAccount(String accountNumber, String firstName, String lastName,
                                 String phoneNumber, String password, double initialBalance, String accountType) {
        return createAccount(accountNumber, firstName, lastName, phoneNumber, password, BigDecimal.valueOf(initialBalance), accountType);
    }

    public Account getAccountByPhoneNumber(String phoneNumber) throws AccountNotFoundException {
        return accountDao.findByPhoneNumber(phoneNumber);
    }

    public Account getAccount(String accountNumber) throws AccountNotFoundException {
        return accountDao.findByAccountNumber(accountNumber);
    }

    public void deposit(String accountNumber, BigDecimal amount) throws AccountNotFoundException {
        accountDao.deposit(accountNumber, amount);
    }

    public void deposit(String accountNumber, double amount) throws AccountNotFoundException {
        deposit(accountNumber, BigDecimal.valueOf(amount));
    }

    public void withdraw(String accountNumber, BigDecimal amount) throws AccountNotFoundException, InsufficientFundsException {
        accountDao.withdraw(accountNumber, amount);
    }

    public void withdraw(String accountNumber, double amount) throws AccountNotFoundException, InsufficientFundsException {
        withdraw(accountNumber, BigDecimal.valueOf(amount));
    }

    public void displayAccountDetails(String accountNumber) throws AccountNotFoundException {
        Account account = getAccount(accountNumber);
        account.displayAccountDetails();
    }

    public void transfer(String sourceAccountNumber, String destinationAccountNumber, BigDecimal amount)
            throws AccountNotFoundException, InsufficientFundsException {
        accountDao.transfer(sourceAccountNumber, destinationAccountNumber, amount);
    }

    public void transfer(String sourceAccountNumber, String destinationAccountNumber, double amount)
            throws AccountNotFoundException, InsufficientFundsException {
        transfer(sourceAccountNumber, destinationAccountNumber, BigDecimal.valueOf(amount));
    }
}
