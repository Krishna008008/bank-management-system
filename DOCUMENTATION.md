# Bank Management System — Technical Specification & Architecture Guide

---

## 1. Executive Summary & Vision

The **Bank Management System** is a modern Core Banking web application and REST API developed in **Java 17** using **Spring Boot 3**. It features:

* **Spring Boot 3 Web Architecture**: Embedded Tomcat web server, Spring MVC `@RestController`, and dependency injection (`@Service`, `@Repository`).
* **Persistent SQLite Database**: ACID-compliant financial transactions using explicit commit and rollback boundaries.
* **Monetary Precision**: Built using `BigDecimal` with 2 decimal places and `HALF_EVEN` (banker's rounding) to prevent IEEE 754 floating-point rounding drift.
* **Security & Authentication**: Salted **BCrypt** PIN hashing and **OTP (One-Time Password)** verification.
* **Audit Logging**: Managed via **Apache Log4j 2** (`spring-boot-starter-log4j2`) with rolling file retention.
* **Automated Quality Assurance**: Complete test suite using **JUnit 5** and **Spring Boot MockMvc**, validated in GitHub Actions CI.
* **Modern Web Portal UI**: Responsive dashboard for customer self-service, served directly from `src/main/resources/static/`.
* **Containerization**: Multi-stage **Docker** build and `docker-compose` orchestration.

---

## 2. Project Evolution: What Was Done, Why It Was Done & How It Was Done

The system was constructed progressively across several architectural phases:

| Phase / Milestone | Core Changes | Why It Was Done | How It Was Done |
| :--- | :--- | :--- | :--- |
| **Phase 1: Domain Modeling & OOP Core** | Initial console prototype | Establish clear separation of concerns and encapsulate banking rules in pure Java. | Designed abstract base class `Account`, subclassed by `SavingsAccount` and `CurrentAccount`. Business operations orchestrated via `BankService` and presented via `Main.java`. |
| **Phase 2: Database Persistence & ACID Transactions** | SQLite JDBC & DAO Layer | In-memory storage lost all state when the app stopped. Banking demands durable storage and atomic, failure-proof transfers. | Integrated SQLite JDBC (`org.xerial:sqlite-jdbc`). Created `DatabaseManager` and `AccountDao` implementing explicit SQL transaction boundaries (`conn.setAutoCommit(false)`, `conn.commit()`, and `conn.rollback()`). |
| **Phase 3: Security & Audit Logging** | BCrypt & Log4j 2 | Storing plaintext PINs violates banking standards. A tamper-evident audit log is mandatory for financial tracking. | Added `jBCrypt` (`org.mindrot:jbcrypt`) for salt generation and hash verification. Configured Apache Log4j 2 via `log4j2.xml` with rolling file policies writing to `logs/bank_activity.log`. |
| **Phase 4: Automated Testing & CI/CD Pipeline** | JUnit 5 & GitHub Actions | Prevent regressions during refactoring and ensure reliable code before merging. | Created unit and integration tests using JUnit 5 Jupiter with isolated dynamic test SQLite databases. Configured `.github/workflows/ci.yml` to automatically compile, test, and package executable fat JARs on push/PR. |
| **Phase 5: Financial Precision Refactoring** | `BigDecimal` Migration | Floating-point `double` causes binary rounding errors in financial balances. | Refactored models, DAO, service layer, REST controller, and tests to use `java.math.BigDecimal` (scale 2, `HALF_EVEN`) and stored balances as `TEXT` in SQLite. |
| **Phase 6: Spring Boot 3 Migration** | Framework Modernization | Raw `HttpServer` did not match industry enterprise standards. Target job profiles require Spring Boot. | Removed raw `HttpServer`. Migrated to Spring Boot 3.2.5 with `spring-boot-starter-web`, `@RestController`, `@Service`, `@Repository`, Spring MockMvc integration tests, and static web serving via Tomcat. |

---

## 3. High-Level Architecture & Component Stack

```
                          +------------------------------------------+
                          |        Client Interfaces                 |
                          |  - Web UI (index.html / app.js)          |
                          |  - Terminal Console (Main.java)          |
                          +------------------------------------------+
                                       |                 |
                       HTTP / JSON     |                 | Direct Calls
                                       v                 |
                          +-----------------------+      |
                          | Spring Boot Web Layer |      |
                          | - Embedded Tomcat     |      |
                          | - BankApiController   |      |
                          |   (@RestController)   |      |
                          | - WebMvcConfig        |      |
                          +-----------------------+      |
                                       |                 |
                                       v                 v
                          +------------------------------------------+
                          | BankService (@Service)                   |
                          | - Domain rule validation                 |
                          | - Password hashing (jBCrypt)             |
                          | - OTP generation & check                 |
                          +------------------------------------------+
                                       |
                                       v
                          +------------------------------------------+
                          | Domain Model Layer                       |
                          | - Account (Abstract)                     |
                          | - SavingsAccount (Min $100.00)           |
                          | - CurrentAccount ($0.00 minimum)         |
                          +------------------------------------------+
                                       |
                                       v
                          +------------------------------------------+
                          | Persistence Layer (@Repository)          |
                          | - AccountDao                             |
                          | - Explicit ACID SQL transactions         |
                          | - Commit / Rollback management           |
                          | - Log4j 2 Audit Trail                    |
                          +------------------------------------------+
                                       |
                                       v
                          +------------------------------------------+
                          | SQLite Database Engine (bank.db)         |
                          | - accounts (balance TEXT)                |
                          | - transactions (ledger entries)          |
                          +------------------------------------------+
```

---

## 4. Module-by-Module Technical Breakdown

### 4.1. Domain Model Layer (`com.example.bank.model`)
* **`Account.java`**:
  * **Abstraction & Encapsulation**: Private fields (`accountNumber`, `firstName`, `lastName`, `phoneNumber`, `password`, `balance`, `transactionHistory`).
  * **Financial Precision**: Field `balance` is of type `BigDecimal` with scale 2.
  * **Polymorphism**: Declares abstract `withdraw(BigDecimal amount) throws InsufficientFundsException`.
  * **Transaction History**: Maintains the 5 most recent timestamped transactions in memory for rapid display.
* **`SavingsAccount.java`**:
  * Enforces a mandatory minimum balance constraint (`MINIMUM_BALANCE = new BigDecimal("100.00")`). Rejects any withdrawal that would cause the balance to fall below $100.00.
* **`CurrentAccount.java`**:
  * Designed for flexible daily operations; allows withdrawals down to `$0.00` without penalty.

---

### 4.2. Data Access & Persistence Layer (`com.example.bank.dao`)
* **`DatabaseManager.java`**:
  * Manages SQLite database connection URLs (`bank.db` by default, dynamic URL support for tests).
  * Automatically creates SQLite tables on startup:
    * `accounts`: `account_number`, `first_name`, `last_name`, `phone_number` (unique), `password_hash`, `balance TEXT`, `account_type`, `created_at`.
    * `transactions`: `id`, `account_number`, `transaction_type`, `amount TEXT`, `balance_after TEXT`, `timestamp`, `description`.
* **`AccountDao.java`**:
  * Annotated with `@Repository`.
  * Implements CRUD operations against SQLite.
  * **ACID Transactions**: For deposits, withdrawals, and account-to-account transfers, `AccountDao` sets `conn.setAutoCommit(false)`. If any operation fails (e.g., recipient not found or sender has insufficient funds), `conn.rollback()` restores both accounts to their original state.
  * Writes ledger entries to the `transactions` table concurrently with account balance updates.

---

### 4.3. Business Service Layer (`com.example.bank.service`)
* **`BankService.java`**:
  * Annotated with `@Service`.
  * Injects `AccountDao` via constructor dependency injection.
  * Validates business invariants:
    * Rejects account creation if the account number or phone number already exists.
    * Enforces initial deposit rules (`>= $100.00` for Savings, `>= $0.00` for Current).
    * Handles lookup by phone number or account number.
    * Delegates financial transactions to `AccountDao` and logs outcomes via Log4j 2.

---

### 4.4. Exception Handling Layer (`com.example.bank.exception`)
* **`AccountNotFoundException.java`**: Checked exception thrown when an account query yields no record.
* **`InsufficientFundsException.java`**: Checked exception thrown when a withdrawal or transfer violates balance rules or minimum thresholds.

---

### 4.5. Presentation & Web API Layer (`com.example.bank.api` & `BankApplication.java`)
* **`BankApplication.java`**:
  * Primary `@SpringBootApplication` entry point.
* **`BankApiController.java`**:
  * Annotated with `@RestController`, `@RequestMapping("/api")`, and `@CrossOrigin(origins = "*")`.
  * Endpoints provided:
    * `POST /api/accounts`: Create a new account with hashed PIN.
    * `POST /api/auth/login`: Authenticate via phone and PIN.
    * `GET /api/accounts/{accountNumber}`: Fetch account holder name, balance, and details.
    * `GET /api/accounts/{accountNumber}/statement`: Fetch mini-statement transaction history.
    * `POST /api/transactions/deposit`: Deposit money into an account.
    * `POST /api/transactions/withdraw`: Withdraw money with PIN verification.
    * `POST /api/transactions/transfer`: Atomic transfer between two accounts with PIN verification.
  * Handlers for `AccountNotFoundException` (404), `InsufficientFundsException` (400), `IllegalArgumentException` (400).
* **`WebMvcConfig.java`**:
  * Configures static resource routing for the web dashboard.
* **`Main.java`**:
  * Optional interactive terminal console interface.

---

### 4.6. Web Client Interface (`src/main/resources/static`)
* **`index.html`**: Semantic layout featuring a top navigation bar, status indicator, auth cards (Login & Register tabs), and post-login dashboard with action cards (Deposit, Withdraw, Transfer) and mini-statement ledger.
* **`style.css`**: Polished visual design using CSS variables, custom typography (Plus Jakarta Sans & JetBrains Mono), responsive flex/grid layouts, credit/debit transaction pills, and animated toast notifications.
* **`app.js`**: Asynchronous client logic using `fetch()` to call the REST API, session persistence via `sessionStorage`, auto-formatting of currency, and dynamic rendering of recent transactions.

---

## 5. How to Build & Run the System

### 1. Run Automated Tests
```powershell
mvn test
```

### 2. Package into Executable Spring Boot JAR
```powershell
mvn clean package
```

### 3. Run with Spring Boot
```powershell
mvn spring-boot:run
```
or run the packaged JAR:
```powershell
java -jar target/bank-management-1.0.0.jar
```
*(On Java 22+: `java --enable-native-access=ALL-UNNAMED -jar target/bank-management-1.0.0.jar`)*

* **Web Portal**: [http://localhost:8080/](http://localhost:8080/)
* **REST API**: [http://localhost:8080/api](http://localhost:8080/api)

### 4. Run Interactive Console (Terminal Mode)
```powershell
java -cp target/bank-management-1.0.0.jar com.example.bank.Main
```

### 5. Run with Docker Compose
```bash
docker compose up --build -d
docker compose down
```
