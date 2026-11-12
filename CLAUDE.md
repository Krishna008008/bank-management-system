# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) and AI coding assistants when working with code in this repository.

## Build and Test Commands

* **Run all automated tests:**
  ```bash
  mvn test
  ```
* **Run a single test class:**
  ```bash
  mvn test -Dtest=BankServiceTest
  mvn test -Dtest=AccountDaoTest
  mvn test -Dtest=BankApiTest
  ```
* **Run Spring Boot application locally:**
  ```bash
  mvn spring-boot:run
  ```
* **Clean and package executable Spring Boot JAR:**
  ```bash
  mvn clean package
  ```
* **Run the packaged JAR:**
  ```bash
  java -jar target/bank-management-1.0.0.jar
  # On Java 22+ (suppresses native library warning for SQLite):
  java --enable-native-access=ALL-UNNAMED -jar target/bank-management-1.0.0.jar
  ```
* **Run the interactive console application:**
  ```bash
  java -cp target/bank-management-1.0.0.jar com.example.bank.Main
  ```
* **Run via Docker Compose:**
  ```bash
  docker compose up --build -d
  docker compose down
  ```

---

## Code Architecture and Package Structure

```
com.example.bank
├── BankApplication.java # Spring Boot entry point (@SpringBootApplication)
├── api/                 # Spring MVC REST Controller (@RestController)
├── config/              # WebMvcConfig (static resource handlers)
├── dao/                 # Persistence layer (@Repository, DatabaseManager)
├── exception/           # Domain-specific checked exceptions
├── model/               # Core domain entities (Account, SavingsAccount, CurrentAccount)
├── service/             # Business orchestration & validation (@Service)
└── Main.java            # Optional interactive CLI terminal
```

### Key Architectural Guidelines

1. **Spring Boot Framework**:
   - Backend is powered by Spring Boot 3 (`spring-boot-starter-web`, `spring-boot-starter-log4j2`, `spring-boot-starter-test`).
   - Use `@RestController`, `@Service`, and `@Repository` for dependency injection.
   - Web assets are served directly from `src/main/resources/static/`.

2. **Monetary Precision (`BigDecimal`)**:
   - Never use `float` or `double` for currency balances or transaction amounts.
   - All financial amounts must be represented using `java.math.BigDecimal` with scale 2 and `RoundingMode.HALF_EVEN`.
   - In SQLite, balance and amount fields are stored as `TEXT` to prevent IEEE 754 floating-point coercion.

3. **Persistence and ACID Transactions**:
   - Database operations run against SQLite via `DatabaseManager`.
   - Transfers and balance updates use explicit manual transaction boundaries:
     ```java
     conn.setAutoCommit(false);
     // ... execute statements ...
     conn.commit();
     // on error:
     conn.rollback();
     ```

4. **Security**:
   - PINs are hashed using `jBCrypt` with salt generation (`BCrypt.hashpw`, `BCrypt.checkpw`). Never log or persist plaintext PINs.

5. **Testing Conventions**:
   - Controller integration tests use `@SpringBootTest` and `MockMvc`.
   - Tests dynamically isolate database state using separate SQLite test files (e.g. `test_bank.db`).
