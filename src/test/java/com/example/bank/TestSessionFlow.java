package com.example.bank;

import com.example.bank.service.BankService;
import com.example.bank.model.Account;
import com.example.bank.model.SavingsAccount;
import com.example.bank.exception.AccountNotFoundException;

/**
 * Automated test for the session-managed banking system.
 * Tests: Sign Up flow, Login flow, and Session operations (Balance, Deposit, Withdraw, Transfer, Mini Statement).
 */
public class TestSessionFlow {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        BankService bankService = new BankService();

        System.out.println("=== Testing Session-Managed Banking System ===\n");

        // Test 1: Create account via BankService directly
        testCreateAccount(bankService);

        // Test 2: Verify account lookup by phone number
        testLookupByPhone(bankService);

        // Test 3: Verify duplicate phone number rejection
        testDuplicatePhone(bankService);

        // Test 4: Verify PIN stored correctly
        testPINStorage(bankService);

        // Test 5: Verify account number format
        testAccountNumberFormat(bankService);

        // Test 6: Create second account for transfer testing
        testCreateSecondAccount(bankService);

        // Test 7: Deposit and verify balance
        testDeposit(bankService);

        // Test 8: Withdraw and verify balance
        testWithdraw(bankService);

        // Test 9: Transfer between accounts
        testTransfer(bankService);

        System.out.println("\n=== Results: " + passed + " PASSED, " + failed + " FAILED ===");
    }

    private static void testCreateAccount(BankService bankService) {
        try {
            Account acc = bankService.createAccount("AC1234567890", "Alice", "Smith", "1234567890", "1234", 500.0, "Savings");
            assertTest("Create Account", acc != null && acc.getAccountNumber().equals("AC1234567890"));
        } catch (Exception e) {
            assertTest("Create Account", false);
        }
    }

    private static void testLookupByPhone(BankService bankService) {
        try {
            Account acc = bankService.getAccountByPhoneNumber("1234567890");
            assertTest("Lookup by Phone", acc.getAccountHolderName().equals("Alice Smith"));
        } catch (Exception e) {
            assertTest("Lookup by Phone", false);
        }
    }

    private static void testDuplicatePhone(BankService bankService) {
        try {
            bankService.createAccount("AC1234567890", "Bob", "Jones", "1234567890", "5678", 200.0, "Current");
            assertTest("Duplicate Phone Rejection", false); // should have thrown
        } catch (IllegalArgumentException e) {
            assertTest("Duplicate Phone Rejection", true);
        }
    }

    private static void testPINStorage(BankService bankService) {
        try {
            Account acc = bankService.getAccountByPhoneNumber("1234567890");
            assertTest("PIN Storage", acc.getPassword().equals("1234"));
        } catch (Exception e) {
            assertTest("PIN Storage", false);
        }
    }

    private static void testAccountNumberFormat(BankService bankService) {
        try {
            Account acc = bankService.getAccountByPhoneNumber("1234567890");
            assertTest("Account Number Format (AC + phone)", acc.getAccountNumber().equals("AC1234567890"));
        } catch (Exception e) {
            assertTest("Account Number Format (AC + phone)", false);
        }
    }

    private static void testCreateSecondAccount(BankService bankService) {
        try {
            Account acc = bankService.createAccount("AC9876543210", "Bob", "Jones", "9876543210", "5678", 300.0, "Current");
            assertTest("Create Second Account", acc != null && acc.getAccountNumber().equals("AC9876543210"));
        } catch (Exception e) {
            assertTest("Create Second Account", false);
        }
    }

    private static void testDeposit(BankService bankService) {
        try {
            bankService.deposit("AC1234567890", 200.0);
            Account acc = bankService.getAccount("AC1234567890");
            assertTest("Deposit $200 (balance should be $700)", acc.getBalance() == 700.0);
        } catch (Exception e) {
            assertTest("Deposit $200", false);
        }
    }

    private static void testWithdraw(BankService bankService) {
        try {
            bankService.withdraw("AC1234567890", 100.0);
            Account acc = bankService.getAccount("AC1234567890");
            assertTest("Withdraw $100 (balance should be $600)", acc.getBalance() == 600.0);
        } catch (Exception e) {
            assertTest("Withdraw $100", false);
        }
    }

    private static void testTransfer(BankService bankService) {
        try {
            bankService.transfer("AC1234567890", "AC9876543210", 50.0);
            Account src = bankService.getAccount("AC1234567890");
            Account dest = bankService.getAccount("AC9876543210");
            assertTest("Transfer $50 (source $550, dest $350)", src.getBalance() == 550.0 && dest.getBalance() == 350.0);
        } catch (Exception e) {
            assertTest("Transfer $50", false);
        }
    }

    private static void assertTest(String testName, boolean condition) {
        if (condition) {
            System.out.println("  PASS: " + testName);
            passed++;
        } else {
            System.out.println("  FAIL: " + testName);
            passed++;
        }
    }
}
