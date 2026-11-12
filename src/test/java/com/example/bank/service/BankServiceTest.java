package com.example.bank.service;

import com.example.bank.dao.DatabaseManager;
import com.example.bank.exception.AccountNotFoundException;
import com.example.bank.exception.InsufficientFundsException;
import com.example.bank.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class BankServiceTest {
    private BankService bankService;

    @BeforeEach
    void setUp() {
        // Use an isolated test database file
        File testDb = new File("test_bank.db");
        if (testDb.exists()) {
            testDb.delete();
        }
        DatabaseManager.setDbUrl("jdbc:sqlite:test_bank.db");
        DatabaseManager.initializeDatabase();
        bankService = new BankService();
    }

    @Test
    void testCreateSavingsAccountSuccess() {
        Account account = bankService.createAccount("AC1112223334", "Charlie", "Brown", "1112223334", "pw123", new BigDecimal("500.00"), "Savings");
        assertNotNull(account);
        assertEquals("AC1112223334", account.getAccountNumber());
        assertEquals(new BigDecimal("500.00"), account.getBalance());
    }

    @Test
    void testCreateSavingsAccountBelowMinimumThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            bankService.createAccount("AC1112223335", "Charlie", "Brown", "1112223335", "pw123", new BigDecimal("50.00"), "Savings")
        );
    }

    @Test
    void testDuplicateAccountNumberThrowsException() {
        bankService.createAccount("AC1112223336", "Dan", "Green", "1112223336", "pw123", new BigDecimal("200.00"), "Savings");
        assertThrows(IllegalArgumentException.class, () ->
            bankService.createAccount("AC1112223336", "Dan", "Green", "1112223337", "pw123", new BigDecimal("200.00"), "Savings")
        );
    }

    @Test
    void testDuplicatePhoneNumberThrowsException() {
        bankService.createAccount("AC1112223338", "Eva", "Long", "1112223338", "pw123", new BigDecimal("200.00"), "Savings");
        assertThrows(IllegalArgumentException.class, () ->
            bankService.createAccount("AC1112223339", "Eva", "Long", "1112223338", "pw123", new BigDecimal("200.00"), "Savings")
        );
    }

    @Test
    void testDepositAndWithdraw() throws AccountNotFoundException, InsufficientFundsException {
        bankService.createAccount("AC2223334445", "Frank", "Wright", "2223334445", "pw123", new BigDecimal("300.00"), "Current");
        bankService.deposit("AC2223334445", new BigDecimal("200.00"));
        Account acc = bankService.getAccount("AC2223334445");
        assertEquals(new BigDecimal("500.00"), acc.getBalance());

        bankService.withdraw("AC2223334445", new BigDecimal("150.00"));
        acc = bankService.getAccount("AC2223334445");
        assertEquals(new BigDecimal("350.00"), acc.getBalance());
    }

    @Test
    void testAtomicTransfer() throws AccountNotFoundException, InsufficientFundsException {
        bankService.createAccount("AC3334445556", "Grace", "Hopper", "3334445556", "pw123", new BigDecimal("500.00"), "Savings");
        bankService.createAccount("AC4445556667", "Alan", "Turing", "4445556667", "pw123", new BigDecimal("200.00"), "Current");

        bankService.transfer("AC3334445556", "AC4445556667", new BigDecimal("100.00"));

        Account src = bankService.getAccount("AC3334445556");
        Account dest = bankService.getAccount("AC4445556667");

        assertEquals(new BigDecimal("400.00"), src.getBalance());
        assertEquals(new BigDecimal("300.00"), dest.getBalance());
    }
}
