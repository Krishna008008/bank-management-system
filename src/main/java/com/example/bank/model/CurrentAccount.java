package com.example.bank.model;

import com.example.bank.exception.InsufficientFundsException;

/**
 * CurrentAccount extends Account (Inheritance).
 * It overrides the withdraw method (Polymorphism).
 * Allows withdrawal until balance is zero (no minimum balance, but can be extended for overdraft).
 */
public class CurrentAccount extends Account {
    public CurrentAccount(String accountNumber, String firstName, String lastName, String phoneNumber, String password, double initialBalance) {
        super(accountNumber, firstName, lastName, phoneNumber, password, initialBalance);
    }

    /**
     * Withdraw method for current account.
     * Allows withdrawal as long as the amount is positive and does not exceed the balance.
     */
    @Override
    public void withdraw(double amount) throws InsufficientFundsException {
        if (amount > 0) {
            double newBalance = getBalance() - amount;
            if (newBalance >= 0) {
                setBalance(newBalance);
                System.out.println("Withdrawn: $" + amount);
                addTransaction("Withdrawal: $" + amount);
            } else {
                throw new InsufficientFundsException("Withdrawal failed. Insufficient funds.");
            }
        } else {
            throw new InsufficientFundsException("Withdrawal amount must be positive.");
        }
    }
}
