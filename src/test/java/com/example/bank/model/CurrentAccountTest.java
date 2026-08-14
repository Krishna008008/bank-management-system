package com.example.bank.model;

import com.example.bank.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CurrentAccountTest {
    private CurrentAccount currentAccount;

    @BeforeEach
    void setUp() {
        currentAccount = new CurrentAccount("AC9876543210", "Bob", "Jones", "9876543210", "hash456", 300.0);
    }

    @Test
    void testDepositIncreasesBalance() {
        currentAccount.deposit(100.0);
        assertEquals(400.0, currentAccount.getBalance());
    }

    @Test
    void testWithdrawalAllowsUpToZeroBalance() throws InsufficientFundsException {
        currentAccount.withdraw(300.0);
        assertEquals(0.0, currentAccount.getBalance());
    }

    @Test
    void testWithdrawalExceedingBalanceThrowsException() {
        assertThrows(InsufficientFundsException.class, () -> currentAccount.withdraw(300.01));
    }

    @Test
    void testWithdrawNegativeThrowsException() {
        assertThrows(InsufficientFundsException.class, () -> currentAccount.withdraw(-10.0));
    }
}
