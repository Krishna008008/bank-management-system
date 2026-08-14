package com.example.bank;

import com.example.bank.model.Account;
import com.example.bank.service.BankService;

/**
 * Automated test for SQLite Database persistence.
 */
public class TestPersistenceFlow {
    public static void main(String[] args) {
        System.out.println("=== Testing SQLite Database Persistence ===\n");

        BankService bankService1 = new BankService();

        // 1. Clean test account creation
        String phone1 = "9998887771";
        String phone2 = "9998887772";
        String accNum1 = "AC" + phone1;
        String accNum2 = "AC" + phone2;

        try {
            Account acc1 = bankService1.createAccount(accNum1, "David", "Miller", phone1, "$2a$10$hashedpw1", 500.0, "Savings");
            System.out.println("PASS: Created Account 1 in SQLite: " + acc1.getAccountNumber());
        } catch (Exception e) {
            System.out.println("INFO: Account 1 might already exist: " + e.getMessage());
        }

        try {
            Account acc2 = bankService1.createAccount(accNum2, "Emma", "Watson", phone2, "$2a$10$hashedpw2", 300.0, "Current");
            System.out.println("PASS: Created Account 2 in SQLite: " + acc2.getAccountNumber());
        } catch (Exception e) {
            System.out.println("INFO: Account 2 might already exist: " + e.getMessage());
        }

        // 2. Deposit & Transfer operations
        try {
            bankService1.deposit(accNum1, 100.0);
            System.out.println("PASS: Deposited $100 into " + accNum1);

            bankService1.transfer(accNum1, accNum2, 50.0);
            System.out.println("PASS: Transferred $50 from " + accNum1 + " to " + accNum2);
        } catch (Exception e) {
            System.err.println("FAIL during transactions: " + e.getMessage());
        }

        // 3. Simulate program restarting: create a NEW BankService instance to read from SQLite
        System.out.println("\n--- Simulating App Restart (New BankService Instance) ---");
        BankService bankService2 = new BankService();

        try {
            Account reloadedAcc1 = bankService2.getAccount(accNum1);
            Account reloadedAcc2 = bankService2.getAccountByPhoneNumber(phone2);

            System.out.println("PASS: Successfully retrieved Account 1 after restart!");
            reloadedAcc1.displayAccountDetails();
            reloadedAcc1.printMiniStatement();

            System.out.println("\nPASS: Successfully retrieved Account 2 by phone after restart!");
            reloadedAcc2.displayAccountDetails();
            reloadedAcc2.printMiniStatement();

        } catch (Exception e) {
            System.err.println("FAIL verifying persistence: " + e.getMessage());
        }
    }
}
