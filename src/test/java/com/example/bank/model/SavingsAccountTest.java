package com.example.bank.model;

import com.example.bank.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class SavingsAccountTest {
    private SavingsAccount savingsAccount;

    @BeforeEach
    void setUp() {
        savingsAccount = new SavingsAccount("AC1234567890", "Alice", "Smith", "1234567890", "hash123", new BigDecimal("500.00"));
    }

    @Test
    void testInitialBalanceAndGetters() {
        assertEquals("AC1234567890", savingsAccount.getAccountNumber());
        assertEquals("Alice Smith", savingsAccount.getAccountHolderName());
        assertEquals(new BigDecimal("500.00"), savingsAccount.getBalance());
    }

    @Test
    void testDepositIncreasesBalance() {
        savingsAccount.deposit(new BigDecimal("200.00"));
        assertEquals(new BigDecimal("700.00"), savingsAccount.getBalance());
    }

    @Test
    void testWithdrawAboveMinimumBalance() throws InsufficientFundsException {
        savingsAccount.withdraw(new BigDecimal("200.00"));
        assertEquals(new BigDecimal("300.00"), savingsAccount.getBalance());
    }

    @Test
    void testWithdrawExactMinimumBalance() throws InsufficientFundsException {
        savingsAccount.withdraw(new BigDecimal("400.00")); // 500 - 400 = 100 (Minimum Balance)
        assertEquals(new BigDecimal("100.00"), savingsAccount.getBalance());
    }

    @Test
    void testWithdrawBelowMinimumBalanceThrowsException() {
        assertThrows(InsufficientFundsException.class, () -> savingsAccount.withdraw(new BigDecimal("400.01")));
    }

    @Test
    void testWithdrawNegativeAmountThrowsException() {
        assertThrows(InsufficientFundsException.class, () -> savingsAccount.withdraw(new BigDecimal("-50.00")));
    }
}
