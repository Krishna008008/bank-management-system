package com.example.bank.dao;

import com.example.bank.exception.AccountNotFoundException;
import com.example.bank.exception.InsufficientFundsException;
import com.example.bank.model.Account;
import com.example.bank.model.SavingsAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class AccountDaoTest {
    private AccountDao accountDao;

    @BeforeEach
    void setUp() {
        File testDb = new File("test_dao_bank.db");
        if (testDb.exists()) {
            testDb.delete();
        }
        DatabaseManager.setDbUrl("jdbc:sqlite:test_dao_bank.db");
        accountDao = new AccountDao();
    }

    @Test
    void testSaveAndFindByAccountNumber() throws AccountNotFoundException {
        SavingsAccount acc = new SavingsAccount("AC5556667778", "Ada", "Lovelace", "5556667778", "hashpw", new BigDecimal("600.00"));
        accountDao.saveAccount(acc, "Savings");

        Account fetched = accountDao.findByAccountNumber("AC5556667778");
        assertNotNull(fetched);
        assertEquals("Ada Lovelace", fetched.getAccountHolderName());
        assertEquals(new BigDecimal("600.00"), fetched.getBalance());
    }

    @Test
    void testFindByPhoneNumber() throws AccountNotFoundException {
        SavingsAccount acc = new SavingsAccount("AC6667778889", "Ken", "Thompson", "6667778889", "hashpw", new BigDecimal("700.00"));
        accountDao.saveAccount(acc, "Savings");

        Account fetched = accountDao.findByPhoneNumber("6667778889");
        assertNotNull(fetched);
        assertEquals("AC6667778889", fetched.getAccountNumber());
    }

    @Test
    void testTransferRollbackOnInsufficientFunds() throws AccountNotFoundException {
        SavingsAccount acc1 = new SavingsAccount("AC7778889990", "Linus", "Torvalds", "7778889990", "hashpw", new BigDecimal("150.00"));
        SavingsAccount acc2 = new SavingsAccount("AC8889990001", "Dennis", "Ritchie", "8889990001", "hashpw", new BigDecimal("500.00"));
        accountDao.saveAccount(acc1, "Savings");
        accountDao.saveAccount(acc2, "Savings");

        // Transfer 100 would leave acc1 with 50 (below minimum 100) -> should fail and rollback
        assertThrows(InsufficientFundsException.class, () -> accountDao.transfer("AC7778889990", "AC8889990001", new BigDecimal("100.00")));

        Account verify1 = accountDao.findByAccountNumber("AC7778889990");
        Account verify2 = accountDao.findByAccountNumber("AC8889990001");
        assertEquals(new BigDecimal("150.00"), verify1.getBalance());
        assertEquals(new BigDecimal("500.00"), verify2.getBalance());
    }
}
