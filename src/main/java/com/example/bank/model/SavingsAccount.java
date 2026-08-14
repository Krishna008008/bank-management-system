package com.example.bank.model;

import com.example.bank.exception.InsufficientFundsException;

/**
 * SavingsAccount extends Account (Inheritance).
 * It overrides the withdraw method (Polymorphism).
 */
public class SavingsAccount extends Account {
    // Minimum balance for savings account
    public static final double MINIMUM_BALANCE = 100.0;

    public SavingsAccount(String accountNumber, String firstName, String lastName, String phoneNumber, String password, double initialBalance) {
        super(accountNumber, firstName, lastName, phoneNumber, password, initialBalance);
    }

    /**
     * Withdraw method for savings account.
     * Ensures that the balance does not go below the minimum balance.
     */
    @Override
    public void withdraw(double amount) throws InsufficientFundsException {
        if (amount > 0) {
            double newBalance = getBalance() - amount;
            if (newBalance >= MINIMUM_BALANCE) {
                setBalance(newBalance);
                System.out.println("Withdrawn: $" + amount);
                addTransaction("Withdrawal: $" + amount);
            } else {
                throw new InsufficientFundsException("Withdrawal failed. Minimum balance of $" + MINIMUM_BALANCE + " must be maintained.");
            }
        } else {
            throw new InsufficientFundsException("Withdrawal amount must be positive.");
        }
    }
}
