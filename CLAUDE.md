# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Commands

**Build the project:**
```bash
javac Main.java
```
This compiles all Java source files in the current directory.

**Run the application:**
```bash
java Main
```
Starts the Bank Management System console application.

**Run specific test classes:**
```bash
# Compile and run a test (e.g., TestNameValidation)
javac TestNameValidation.java
java TestNameValidation
```
Replace `TestNameValidation` with other test class names (e.g., `TestWithdraw`, `TestMenuWithdraw`, `TestMain`, `TestWithdrawFinal`) as needed.

**Clean compiled files:**
```bash
del *.class
```
Removes all compiled class files.

## Code Architecture and Structure

### High-Level Overview
The Bank Management System is a console-based Java application with a layered structure:
- **Presentation Layer**: `Main.java` handles user input/output via console.
- **Service Layer**: `BankService.java` (in `service` package) manages account operations and business logic.
- **Model Layer**: Account classes (`Account.java`, `SavingsAccount.java`, `CurrentAccount.java` in `model` package) represent bank accounts.
- **Exception Layer**: Custom exceptions (`InsufficientFundsException.java`, `AccountNotFoundException.java` in `exception` package) handle error conditions.

### Key Components

1. **Main.java**: 
   - Entry point with a menu-driven interface.
   - Maps user choices to banking operations (create account, deposit, withdraw, etc.).
   - Uses `Scanner` for console input and `BankService` for processing.

2. **BankService.java**:
   - Central service managing accounts in a `HashMap` (account number → Account object).
   - Provides methods for account creation, deposits, withdrawals, transfers, balance inquiries, and mini-statements.
   - Delegates actual operations to `Account` objects.

3. **Account Hierarchy**:
   - Abstract `Account.java`: Defines common account properties (number, holder name, balance) and operations (deposit, withdraw, display details, mini-statement).
   - `SavingsAccount.java` and `CurrentAccount.java`: Extend `Account` with account-type-specific behavior (currently identical but designed for future differentiation).

4. **Exceptions**:
   - `AccountNotFoundException`: Thrown when an account number doesn't exist.
   - `InsufficientFundsException`: Thrown when withdrawal/transfer exceeds available balance.

### Data Flow
1. User selects an option in `Main.java`.
2. `Main` collects required parameters (account numbers, amounts, etc.) via console.
3. `Main` invokes the corresponding method in `BankService`.
4. `BankService` validates the account(s) exist and performs the operation (or throws exceptions).
5. For balance inquiries and mini-statements, `BankService` retrieves the `Account` object and calls its display methods.
6. Results or error messages are displayed to the user via `Main.java`.

### Design Notes
- The system uses polymorphism: `BankService` works with the abstract `Account` type, allowing different account implementations.
- Extensibility: New account types can be added by extending `Account` without modifying `BankService` (Open/Closed Principle).
- Error handling: Business logic exceptions are caught in `Main.java` and displayed to the user.