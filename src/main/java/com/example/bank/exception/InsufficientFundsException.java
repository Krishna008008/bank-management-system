package com.example.bank.exception;

/**
 * Custom exception for insufficient funds.
 */
public class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
