package com.example.bank.service;

import com.example.bank.dao.DatabaseManager;
import com.example.bank.exception.AccountNotFoundException;
import com.example.bank.exception.InsufficientFundsException;
import com.example.bank.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

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
        Account account = bankService.createAccount("AC1112223334", "Charlie", "Brown", "1112223334", "pw123", 500.0, "Savings");
        assertNotNull(account);
        assertEquals("AC1112223334", account.getAccountNumber());
        assertEquals(500.0, account.getBalance());
    }

    @Test
    void testCreateSavingsAccountBelowMinimumThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            bankService.createAccount("AC1112223335", "Charlie", "Brown", "1112223335", "pw123", 50.0, "Savings")
        );
    }

    @Test
    void testDuplicateAccountNumberThrowsException() {
        bankService.createAccount("AC1112223336", "Dan", "Green", "1112223336", "pw123", 200.0, "Savings");
        assertThrows(IllegalArgumentException.class, () ->
            bankService.createAccount("AC1112223336", "Dan", "Green", "1112223337", "pw123", 200.0, "Savings")
        );
    }

    @Test
    void testDuplicatePhoneNumberThrowsException() {
        bankService.createAccount("AC1112223338", "Eva", "Long", "1112223338", "pw123", 200.0, "Savings");
        assertThrows(IllegalArgumentException.class, () ->
            bankService.createAccount("AC1112223339", "Eva", "Long", "1112223338", "pw123", 200.0, "Savings")
        );
    }

    @Test
    void testDepositAndWithdraw() throws AccountNotFoundException, InsufficientFundsException {
        bankService.createAccount("AC2223334445", "Frank", "Wright", "2223334445", "pw123", 300.0, "Current");
        bankService.deposit("AC2223334445", 200.0);
        Account acc = bankService.getAccount("AC2223334445");
        assertEquals(500.0, acc.getBalance());

        bankService.withdraw("AC2223334445", 150.0);
        acc = bankService.getAccount("AC2223334445");
        assertEquals(350.0, acc.getBalance());
    }

    @Test
    void testAtomicTransfer() throws AccountNotFoundException, InsufficientFundsException {
        bankService.createAccount("AC3334445556", "Grace", "Hopper", "3334445556", "pw123", 500.0, "Savings");
        bankService.createAccount("AC4445556667", "Alan", "Turing", "4445556667", "pw123", 200.0, "Current");

        bankService.transfer("AC3334445556", "AC4445556667", 100.0);

        Account src = bankService.getAccount("AC3334445556");
        Account dest = bankService.getAccount("AC4445556667");

        assertEquals(400.0, src.getBalance());
        assertEquals(300.0, dest.getBalance());
    }
}
