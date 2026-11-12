# Bank Management System

![Java CI](https://github.com/Krishna008008/bank-management-system/actions/workflows/ci.yml/badge.svg)
[![Java 17](https://img.shields.io/badge/Java-17-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![SQLite](https://img.shields.io/badge/SQLite-3-green.svg)](https://www.sqlite.org/)
[![JUnit 5](https://img.shields.io/badge/JUnit-5-orange.svg)](https://junit.org/junit5/)
[![Log4j2](https://img.shields.io/badge/Log4j-2-red.svg)](https://logging.apache.org/log4j/2.x/)

A modern core banking web application and REST API built in **Java 17** with **Spring Boot 3**, demonstrating decimal monetary precision with `BigDecimal`, SQLite persistence with ACID transactions, BCrypt PIN hashing, structured Log4j2 audit logging, and automated integration tests with MockMvc and JUnit 5.

---

## 🚀 Features

- **Account Management**: Supports `SavingsAccount` (with a $100.00 minimum balance constraint) and `CurrentAccount`.
- **Financial Precision**: All monetary values are handled using `BigDecimal` (scaled to 2 decimal places with `HALF_EVEN` banker's rounding) to avoid IEEE 754 floating-point rounding errors.
- **Security**: 
  - 4-digit PIN secured using **BCrypt** hashing with salt generation.
  - 6-digit simulated **OTP verification** upon sign-up and login.
- **Transactions & ACID Compliance**:
  - Deposit, withdrawal, and account-to-account transfers.
  - Multi-account transfers run within explicit SQL transactions (`setAutoCommit(false)` with rollback on failure).
  - Mini-statement displaying recent timestamped account activities.
- **Persistence**: Embedded **SQLite** database (`bank.db`) storing account profiles and transaction ledger entries.
- **Audit Logging**: Application events and security-sensitive operations logged via **Log4j 2** (`logs/bank_activity.log`) with rolling file retention.
- **Automated Testing**: Unit and integration test suite using **JUnit 5** and **Spring Boot MockMvc** with isolated test database instances.
- **CI/CD**: Cloud build and automated test runs configured via **GitHub Actions**.
- **Interfaces**:
  - Web Banking Portal served directly from Spring Boot embedded Tomcat (`http://localhost:8080/`).
  - RESTful JSON API (`/api/accounts`, `/api/transactions/*`, `/api/auth/login`).
  - Interactive terminal CLI (`Main.java`).

---

## 🛠️ Tech Stack & Dependencies

- **Framework**: Spring Boot 3.2.5 (`spring-boot-starter-web`, `spring-boot-starter-log4j2`, `spring-boot-starter-test`)
- **Language**: Java 17
- **Build Tool**: Apache Maven
- **Database**: SQLite JDBC (`org.xerial:sqlite-jdbc`)
- **Security**: jBCrypt (`org.mindrot:jbcrypt`)
- **JSON Serialization**: Spring Web / Jackson & Google Gson
- **Logging**: Apache Log4j 2 (`log4j-api`, `log4j-core`)
- **Testing**: JUnit 5 Jupiter & Spring MockMvc
- **CI/CD**: GitHub Actions
- **Containerization**: Docker (multi-stage) & Docker Compose

---

## 💡 Key Design Decisions

### 1. Why `BigDecimal` Instead of `double` / `float`?
In standard binary floating-point representation (IEEE 754), fractions like `0.10` or `0.01` cannot be represented with exact precision in base 2. In financial code, accumulated rounding drifts (`0.1 + 0.2 = 0.30000000000000004`) lead to ledger imbalances and fail statutory accounting standards. This project uses `java.math.BigDecimal` with explicit scale (2 decimal places) and `RoundingMode.HALF_EVEN` across all domain models, calculations, and database storage.

### 2. Spring Boot Architecture
The application is structured into standard Spring Boot architectural layers:
* **Web Layer**: [`BankApiController`](src/main/java/com/example/bank/api/BankApiController.java) annotated with `@RestController` handling JSON payloads, HTTP status codes, and exception mapping.
* **Service Layer**: [`BankService`](src/main/java/com/example/bank/service/BankService.java) annotated with `@Service` coordinating banking business logic.
* **Persistence Layer**: [`AccountDao`](src/main/java/com/example/bank/dao/AccountDao.java) annotated with `@Repository` managing SQLite JDBC connections and explicit ACID transactions.
* **Static Portal**: Web assets (`index.html`, `style.css`, `app.js`) are served directly by embedded Tomcat from `src/main/resources/static/`.

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

### 3. Run Web Portal & Spring Boot Application
```powershell
mvn spring-boot:run
```
or run the packaged JAR:
```powershell
java -jar target/bank-management-1.0.0.jar
```
*(On Java 22+: `java --enable-native-access=ALL-UNNAMED -jar target/bank-management-1.0.0.jar`)*

* 🌐 **Web Banking UI**: Open `http://localhost:8080/` in your browser.
* 📡 **REST API Base**: `http://localhost:8080/api`

### 4. Run with Docker Compose
```bash
docker compose up --build -d
```
Stop container:
```bash
docker compose down
```

### 5. Run Interactive Console (Terminal Mode)
```powershell
java -cp target/bank-management-1.0.0.jar com.example.bank.Main
```
