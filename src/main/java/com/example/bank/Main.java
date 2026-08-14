package com.example.bank;

import com.example.bank.service.BankService;
import com.example.bank.model.Account;
import com.example.bank.model.SavingsAccount;
import com.example.bank.exception.AccountNotFoundException;
import com.example.bank.exception.InsufficientFundsException;
import java.util.Random;
import java.util.Scanner;

public class Main {
    private static final Random random = new Random();

    public static void main(String[] args) {
        BankService bankService = new BankService();
        Scanner scanner = new Scanner(System.in);

        System.out.println("========================================");
        System.out.println("   Welcome to Bank Management System");
        System.out.println("========================================");

        boolean exitApp = false;
        while (!exitApp) {
            System.out.println("\n--- Welcome ---");
            System.out.println("1. Sign Up (New User)");
            System.out.println("2. Login (Existing User)");
            System.out.println("3. Exit");
            System.out.print("Enter your choice (1-3): ");

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());

                switch (choice) {
                    case 1:
                        signUp(bankService, scanner);
                        break;
                    case 2:
                        Account loggedInAccount = login(bankService, scanner);
                        if (loggedInAccount != null) {
                            sessionMenu(bankService, scanner, loggedInAccount);
                        }
                        break;
                    case 3:
                        exitApp = true;
                        System.out.println("Thank you for using the Bank Management System. Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter 1, 2, or 3.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }

        scanner.close();
    }

    // ==================== OTP HELPER ====================

    private static String generateOTP() {
        return String.format("%06d", random.nextInt(1000000));
    }

    /**
     * Simulates sending an OTP, prompts the user, and verifies.
     * Returns true if verified, false if user typed 'exit'.
     */
    private static boolean verifyOTP(Scanner scanner) {
        String otp = generateOTP();
        System.out.println("\n>> A 6-digit OTP has been sent to your registered phone.");
        System.out.println(">> [Simulated OTP]: " + otp);

        while (true) {
            System.out.print("Enter OTP: ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("exit")) {
                return false;
            }
            if (input.equals(otp)) {
                System.out.println("OTP verified successfully!");
                return true;
            } else {
                System.out.println("Incorrect OTP. Please try again.");
            }
        }
    }

    /**
     * Prompts for the 4-digit PIN and verifies against the account.
     * Returns true if verified, false if user typed 'exit'.
     */
    private static boolean verifyPIN(Scanner scanner, Account account) {
        while (true) {
            System.out.print("Enter your 4-digit PIN: ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("exit")) {
                return false;
            }
            String storedPassword = account.getPassword();
            boolean matches = false;
            try {
                if (storedPassword != null && (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$"))) {
                    matches = org.mindrot.jbcrypt.BCrypt.checkpw(input, storedPassword);
                } else {
                    matches = input.equals(storedPassword);
                }
            } catch (Exception e) {
                matches = input.equals(storedPassword);
            }

            if (matches) {
                return true;
            } else {
                System.out.println("Incorrect PIN. Please try again.");
            }
        }
    }

    // ==================== NAME VALIDATION ====================

    private static boolean isValidName(String name) {
        return name != null && !name.isEmpty() && name.matches("^[a-zA-Z]+$");
    }

    // ==================== SIGN UP ====================

    private static void signUp(BankService bankService, Scanner scanner) {
        System.out.println("\n--- Sign Up ---");
        System.out.println("[Tip: Type 'exit' at any prompt to cancel and return to main menu]");

        // First Name
        String firstName = "";
        while (true) {
            System.out.print("Enter first name: ");
            firstName = scanner.nextLine().trim();
            if (firstName.equalsIgnoreCase("exit")) return;
            if (isValidName(firstName)) break;
            System.out.println("Error: First name must contain only letters.");
        }

        // Last Name
        String lastName = "";
        while (true) {
            System.out.print("Enter last name: ");
            lastName = scanner.nextLine().trim();
            if (lastName.equalsIgnoreCase("exit")) return;
            if (isValidName(lastName)) break;
            System.out.println("Error: Last name must contain only letters.");
        }

        // Phone Number
        String phoneNumber = "";
        while (true) {
            System.out.print("Enter 10-digit phone number: ");
            phoneNumber = scanner.nextLine().trim();
            if (phoneNumber.equalsIgnoreCase("exit")) return;
            if (!phoneNumber.matches("^\\d{10}$")) {
                System.out.println("Error: Phone number must be exactly 10 digits.");
                continue;
            }
            // Check if phone is already registered
            String accountNumber = "AC" + phoneNumber;
            try {
                bankService.getAccount(accountNumber);
                System.out.println("Error: An account with this phone number already exists. Please login instead.");
                return;
            } catch (AccountNotFoundException e) {
                // Good — phone number is available
                break;
            }
        }

        // OTP Verification
        if (!verifyOTP(scanner)) return;

        // Set 4-digit PIN
        String password = "";
        while (true) {
            System.out.print("Create a 4-digit PIN: ");
            password = scanner.nextLine().trim();
            if (password.equalsIgnoreCase("exit")) return;
            if (password.matches("^\\d{4}$")) break;
            System.out.println("Error: PIN must be exactly 4 digits.");
        }

        // Confirm PIN
        while (true) {
            System.out.print("Confirm your 4-digit PIN: ");
            String confirmPIN = scanner.nextLine().trim();
            if (confirmPIN.equalsIgnoreCase("exit")) return;
            if (confirmPIN.equals(password)) break;
            System.out.println("Error: PINs do not match. Please try again.");
        }

        // Account Type
        String accountType = "";
        while (true) {
            System.out.print("Enter account type (Savings/Current): ");
            accountType = scanner.nextLine().trim();
            if (accountType.equalsIgnoreCase("exit")) return;
            if (accountType.equalsIgnoreCase("Savings") || accountType.equalsIgnoreCase("Current")) break;
            System.out.println("Error: Invalid account type. Use 'Savings' or 'Current'.");
        }

        // Initial Balance
        while (true) {
            double minBalance = accountType.equalsIgnoreCase("Savings") ? SavingsAccount.MINIMUM_BALANCE : 0.0;
            System.out.printf("Enter initial deposit amount (Minimum: $%.2f): ", minBalance);
            String balanceStr = scanner.nextLine().trim();
            if (balanceStr.equalsIgnoreCase("exit")) return;

            try {
                double initialBalance = Double.parseDouble(balanceStr);
                // Validate deposit amount
                if (initialBalance <= 0) {
                    System.out.println("Error: Initial deposit must be greater than zero.");
                    continue;
                }
                if (initialBalance < minBalance) {
                    System.out.println("Error: Initial deposit must be at least $" + minBalance + " for Savings accounts.");
                    continue;
                }
                String accountNumber = "AC" + phoneNumber;
                String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt());
                Account account = bankService.createAccount(accountNumber, firstName, lastName, phoneNumber, hashedPassword, initialBalance, accountType);
                System.out.println("\n>> Account created successfully!");
                System.out.println(">> Your Account Number: " + account.getAccountNumber());
                System.out.println(">> Account Holder: " + account.getAccountHolderName());
                System.out.println(">> Please remember your Account Number and PIN for login.");
                return;
            } catch (NumberFormatException e) {
                System.out.println("Error: Invalid amount. Please enter a valid number.");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // ==================== LOGIN ====================

    private static Account login(BankService bankService, Scanner scanner) {
        System.out.println("\n--- Login ---");
        System.out.println("[Tip: Type 'exit' at any prompt to cancel and return to main menu]");

        // First Name
        String firstName = "";
        while (true) {
            System.out.print("Enter first name: ");
            firstName = scanner.nextLine().trim();
            if (firstName.equalsIgnoreCase("exit")) return null;
            if (isValidName(firstName)) break;
            System.out.println("Error: First name must contain only letters.");
        }

        // Last Name
        String lastName = "";
        while (true) {
            System.out.print("Enter last name: ");
            lastName = scanner.nextLine().trim();
            if (lastName.equalsIgnoreCase("exit")) return null;
            if (isValidName(lastName)) break;
            System.out.println("Error: Last name must contain only letters.");
        }

        // Phone Number
        Account account = null;
        String phoneNumber = "";
        while (true) {
            System.out.print("Enter 10-digit phone number: ");
            phoneNumber = scanner.nextLine().trim();
            if (phoneNumber.equalsIgnoreCase("exit")) return null;
            if (!phoneNumber.matches("^\\d{10}$")) {
                System.out.println("Error: Phone number must be exactly 10 digits.");
                continue;
            }
            try {
                account = bankService.getAccountByPhoneNumber(phoneNumber);
                break;
            } catch (AccountNotFoundException e) {
                System.out.println("Error: " + e.getMessage());
                System.out.println("Please try again or type 'exit' to go back.");
            }
        }

        // Verify name matches
        if (!account.getFirstName().equalsIgnoreCase(firstName) || !account.getLastName().equalsIgnoreCase(lastName)) {
            System.out.println("Error: Name does not match our records for this phone number.");
            return null;
        }

        // OTP Verification
        if (!verifyOTP(scanner)) return null;

        // PIN Verification
        System.out.println("\n>> OTP verified. Please enter your PIN to complete login.");
        if (!verifyPIN(scanner, account)) return null;

        System.out.println("\n>> Login successful! Welcome, " + account.getAccountHolderName() + ".");
        return account;
    }

    // ==================== SESSION MENU ====================

    private static void sessionMenu(BankService bankService, Scanner scanner, Account loggedInAccount) {
        boolean logout = false;
        while (!logout) {
            System.out.println("\n========================================");
            System.out.println("  Logged in as: " + loggedInAccount.getAccountHolderName() + " (" + loggedInAccount.getAccountNumber() + ")");
            System.out.println("========================================");
            System.out.println("1. Check Balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Mini Statement");
            System.out.println("6. Logout");
            System.out.print("Enter your choice (1-6): ");

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());

                switch (choice) {
                    case 1:
                        checkBalance(loggedInAccount);
                        break;
                    case 2:
                        deposit(bankService, scanner, loggedInAccount);
                        break;
                    case 3:
                        withdraw(bankService, scanner, loggedInAccount);
                        break;
                    case 4:
                        transfer(bankService, scanner, loggedInAccount);
                        break;
                    case 5:
                        miniStatement(loggedInAccount);
                        break;
                    case 6:
                        logout = true;
                        System.out.println("Logged out successfully. Returning to Welcome Screen.");
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter a number between 1 and 6.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }
    }

    // ==================== SESSION OPERATIONS ====================

    private static void checkBalance(Account account) {
        System.out.println("\n--- Account Details ---");
        account.displayAccountDetails();
    }

    private static void deposit(BankService bankService, Scanner scanner, Account account) {
        System.out.println("\n--- Deposit ---");
        System.out.println("[Tip: Type 'exit' at any prompt to cancel]");

        // Verify PIN first
        if (!verifyPIN(scanner, account)) return;

        while (true) {
            System.out.print("Enter amount to deposit: ");
            String amountStr = scanner.nextLine().trim();
            if (amountStr.equalsIgnoreCase("exit")) return;

            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    System.out.println("Error: Deposit amount must be greater than zero.");
                    continue;
                }
                bankService.deposit(account.getAccountNumber(), amount);
                System.out.println("Deposit successful!");
                return;
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Please enter a valid number.");
            } catch (AccountNotFoundException e) {
                System.out.println("Error: " + e.getMessage());
                return;
            }
        }
    }

    private static void withdraw(BankService bankService, Scanner scanner, Account account) {
        System.out.println("\n--- Withdraw ---");
        System.out.println("[Tip: Type 'exit' at any prompt to cancel]");

        // Verify PIN first
        if (!verifyPIN(scanner, account)) return;

        while (true) {
            System.out.print("Enter amount to withdraw: ");
            String amountStr = scanner.nextLine().trim();
            if (amountStr.equalsIgnoreCase("exit")) return;

            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    System.out.println("Error: Withdrawal amount must be greater than zero.");
                    continue;
                }
                bankService.withdraw(account.getAccountNumber(), amount);
                System.out.println("Withdrawal successful!");
                return;
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Please enter a valid number.");
            } catch (AccountNotFoundException | InsufficientFundsException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void transfer(BankService bankService, Scanner scanner, Account account) {
        System.out.println("\n--- Transfer ---");
        System.out.println("[Tip: Type 'exit' at any prompt to cancel]");

        // Verify PIN first
        if (!verifyPIN(scanner, account)) return;

        // Destination account
        String destAccountNumber = "";
        while (true) {
            System.out.print("Enter destination account number: ");
            destAccountNumber = scanner.nextLine().trim();
            if (destAccountNumber.equalsIgnoreCase("exit")) return;

            if (destAccountNumber.equals(account.getAccountNumber())) {
                System.out.println("Error: Cannot transfer to your own account.");
                continue;
            }

            try {
                bankService.getAccount(destAccountNumber);
                break;
            } catch (AccountNotFoundException e) {
                System.out.println("Error: " + e.getMessage());
                System.out.println("Please try again.");
            }
        }

        // Transfer amount
        while (true) {
            System.out.print("Enter amount to transfer: ");
            String amountStr = scanner.nextLine().trim();
            if (amountStr.equalsIgnoreCase("exit")) return;

            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    System.out.println("Error: Transfer amount must be greater than zero.");
                    continue;
                }
                bankService.transfer(account.getAccountNumber(), destAccountNumber, amount);
                System.out.println("Transfer successful!");
                return;
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Please enter a valid number.");
            } catch (AccountNotFoundException | InsufficientFundsException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void miniStatement(Account account) {
        System.out.println();
        account.printMiniStatement();
    }
}
