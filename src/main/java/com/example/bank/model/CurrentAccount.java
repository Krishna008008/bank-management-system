package com.example.bank.model;

import com.example.bank.exception.InsufficientFundsException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * CurrentAccount extends Account (Inheritance).
 * It overrides the withdraw method (Polymorphism).
 * Allows withdrawal until balance is zero (no minimum balance).
 */
public class CurrentAccount extends Account {
    public CurrentAccount(String accountNumber, String firstName, String lastName, String phoneNumber, String password, BigDecimal initialBalance) {
        super(accountNumber, firstName, lastName, phoneNumber, password, initialBalance);
    }

    public CurrentAccount(String accountNumber, String firstName, String lastName, String phoneNumber, String password, double initialBalance) {
        super(accountNumber, firstName, lastName, phoneNumber, password, initialBalance);
    }

    /**
     * Withdraw method for current account.
     * Allows withdrawal as long as the amount is positive and does not exceed the balance.
     */
    @Override
    public void withdraw(BigDecimal amount) throws InsufficientFundsException {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal standardized = amount.setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal newBalance = getBalance().subtract(standardized);
            if (newBalance.compareTo(BigDecimal.ZERO) >= 0) {
                setBalance(newBalance);
                System.out.println("Withdrawn: $" + standardized.toPlainString());
                addTransaction("Withdrawal: $" + standardized.toPlainString());
            } else {
                throw new InsufficientFundsException("Withdrawal failed. Insufficient funds.");
            }
        } else {
            throw new InsufficientFundsException("Withdrawal amount must be positive.");
        }
    }
}
