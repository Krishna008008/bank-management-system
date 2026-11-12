package com.example.bank.model;

import com.example.bank.exception.InsufficientFundsException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * SavingsAccount extends Account (Inheritance).
 * It overrides the withdraw method (Polymorphism).
 */
public class SavingsAccount extends Account {
    // Minimum balance for savings account
    public static final BigDecimal MINIMUM_BALANCE = new BigDecimal("100.00");

    public SavingsAccount(String accountNumber, String firstName, String lastName, String phoneNumber, String password, BigDecimal initialBalance) {
        super(accountNumber, firstName, lastName, phoneNumber, password, initialBalance);
    }

    public SavingsAccount(String accountNumber, String firstName, String lastName, String phoneNumber, String password, double initialBalance) {
        super(accountNumber, firstName, lastName, phoneNumber, password, initialBalance);
    }

    /**
     * Withdraw method for savings account.
     * Ensures that the balance does not go below the minimum balance.
     */
    @Override
    public void withdraw(BigDecimal amount) throws InsufficientFundsException {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal standardized = amount.setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal newBalance = getBalance().subtract(standardized);
            if (newBalance.compareTo(MINIMUM_BALANCE) >= 0) {
                setBalance(newBalance);
                System.out.println("Withdrawn: $" + standardized.toPlainString());
                addTransaction("Withdrawal: $" + standardized.toPlainString());
            } else {
                throw new InsufficientFundsException("Withdrawal failed. Minimum balance of $" + MINIMUM_BALANCE.toPlainString() + " must be maintained.");
            }
        } else {
            throw new InsufficientFundsException("Withdrawal amount must be positive.");
        }
    }
}
