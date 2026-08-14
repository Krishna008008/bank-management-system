# Bank Management System

![Java CI](https://github.com/Krishna008008/bank-management-system/actions/workflows/ci.yml/badge.svg)
[![Java 17](https://img.shields.io/badge/Java-17-blue.svg)](https://www.oracle.com/java/)
[![SQLite](https://img.shields.io/badge/SQLite-3-green.svg)](https://www.sqlite.org/)
[![JUnit 5](https://img.shields.io/badge/JUnit-5-orange.svg)](https://junit.org/junit5/)
[![Log4j2](https://img.shields.io/badge/Log4j-2-red.svg)](https://logging.apache.org/log4j/2.x/)

A robust, enterprise-grade Core Banking Application built in Java with SQLite persistence, ACID-compliant transactions, BCrypt PIN hashing, OTP simulation, Log4j2 audit logging, and comprehensive automated JUnit 5 tests.

---

## 🚀 Features

- **Account Management**: Supports `SavingsAccount` (with minimum balance constraints) and `CurrentAccount`.
- **Security**: 
  - 4-digit PIN secured using **BCrypt** hashing with salt generation.
  - 6-digit simulated **OTP verification** upon sign-up and login.
- **Transactions**:
  - Deposit, Withdrawal, and Account-to-Account Transfers.
  - **ACID Compliant**: Money transfers use atomic SQL transactions (`setAutoCommit(false)` with rollback on failure).
  - Mini-statement showing the last 5 transactions with timestamps.
- **Persistence**: Embedded **SQLite** database (`bank.db`) storing account profiles and historical transaction logs.
- **Audit Logging**: Enterprise logging configured via **Log4j2** (`logs/bank_activity.log`) with rolling file retention.
- **Automated Testing**: 100% test coverage of core banking logic using **JUnit 5** and in-memory test databases.
- **Continuous Integration**: Automated cloud builds and test runs via **GitHub Actions**.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Java 17
- **Build Tool**: Apache Maven
- **Database**: SQLite JDBC (`org.xerial:sqlite-jdbc`)
- **Security**: jBCrypt (`org.mindrot:jbcrypt`)
- **Logging**: Apache Log4j 2 (`log4j-api`, `log4j-core`)
- **Testing**: JUnit 5 Jupiter (`junit-jupiter-api`, `junit-jupiter-engine`)
- **CI/CD**: GitHub Actions

---

## 📦 How to Build & Run

### 1. Run Automated Tests
```powershell
mvn test
```

### 2. Package into Executable JAR
```powershell
mvn clean package
```

### 3. Run Console Application
```powershell
java -jar target/bank-management-1.0.0-jar-with-dependencies.jar
```

### 4. Run REST API Server
```powershell
java -cp target/bank-management-1.0.0-jar-with-dependencies.jar com.example.bank.api.BankServer
```
REST API will be live on `http://localhost:8080/api` with endpoints:
- `POST /api/accounts` (Sign up)
- `POST /api/auth/login` (Authentication)
- `GET /api/accounts/{id}` (Account Details)
- `POST /api/transactions/deposit` (Deposit)
- `POST /api/transactions/withdraw` (Withdraw)
- `POST /api/transactions/transfer` (Atomic Transfer)

