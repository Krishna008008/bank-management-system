package com.example.bank.model;

import com.example.bank.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SavingsAccountTest {
    private SavingsAccount savingsAccount;

    @BeforeEach
    void setUp() {
        savingsAccount = new SavingsAccount("AC1234567890", "Alice", "Smith", "1234567890", "hash123", 500.0);
    }

    @Test
    void testInitialBalanceAndGetters() {
        assertEquals("AC1234567890", savingsAccount.getAccountNumber());
        assertEquals("Alice Smith", savingsAccount.getAccountHolderName());
        assertEquals(500.0, savingsAccount.getBalance());
    }

    @Test
    void testDepositIncreasesBalance() {
        savingsAccount.deposit(200.0);
        assertEquals(700.0, savingsAccount.getBalance());
    }

    @Test
    void testWithdrawAboveMinimumBalance() throws InsufficientFundsException {
        savingsAccount.withdraw(200.0);
        assertEquals(300.0, savingsAccount.getBalance());
    }

    @Test
    void testWithdrawExactMinimumBalance() throws InsufficientFundsException {
        savingsAccount.withdraw(400.0); // 500 - 400 = 100 (Minimum Balance)
        assertEquals(100.0, savingsAccount.getBalance());
    }

    @Test
    void testWithdrawBelowMinimumBalanceThrowsException() {
        assertThrows(InsufficientFundsException.class, () -> savingsAccount.withdraw(400.01));
    }

    @Test
    void testWithdrawNegativeAmountThrowsException() {
        assertThrows(InsufficientFundsException.class, () -> savingsAccount.withdraw(-50.0));
    }
}
