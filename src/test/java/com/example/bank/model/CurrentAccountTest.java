package com.example.bank.model;

import com.example.bank.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class CurrentAccountTest {
    private CurrentAccount currentAccount;

    @BeforeEach
    void setUp() {
        currentAccount = new CurrentAccount("AC9876543210", "Bob", "Jones", "9876543210", "hash456", new BigDecimal("300.00"));
    }

    @Test
    void testDepositIncreasesBalance() {
        currentAccount.deposit(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("400.00"), currentAccount.getBalance());
    }

    @Test
    void testWithdrawalAllowsUpToZeroBalance() throws InsufficientFundsException {
        currentAccount.withdraw(new BigDecimal("300.00"));
        assertEquals(new BigDecimal("0.00"), currentAccount.getBalance());
    }

    @Test
    void testWithdrawalExceedingBalanceThrowsException() {
        assertThrows(InsufficientFundsException.class, () -> currentAccount.withdraw(new BigDecimal("300.01")));
    }

    @Test
    void testWithdrawNegativeThrowsException() {
        assertThrows(InsufficientFundsException.class, () -> currentAccount.withdraw(new BigDecimal("-10.00")));
    }
}
