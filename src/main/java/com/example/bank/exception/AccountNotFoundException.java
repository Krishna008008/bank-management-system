package com.example.bank.exception;

/**
 * Custom exception for when an account is not found.
 */
public class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
