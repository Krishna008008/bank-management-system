package com.example.bank.api;

import com.example.bank.exception.AccountNotFoundException;
import com.example.bank.exception.InsufficientFundsException;
import com.example.bank.model.Account;
import com.example.bank.service.BankService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring MVC REST Controller for Core Banking operations.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BankApiController {
    private static final Logger logger = LogManager.getLogger(BankApiController.class);
    private final BankService bankService;

    @Autowired
    public BankApiController(BankService bankService) {
        this.bankService = bankService;
    }

    // ==================== REQUEST DTOs ====================

    public static class CreateAccountRequest {
        public String phoneNumber;
        public String firstName;
        public String lastName;
        public String password;
        public BigDecimal initialBalance;
        public String accountType;
    }

    public static class LoginRequest {
        public String phoneNumber;
        public String password;
    }

    public static class DepositRequest {
        public String accountNumber;
        public BigDecimal amount;
        public String pin;
    }

    public static class WithdrawRequest {
        public String accountNumber;
        public BigDecimal amount;
        public String pin;
    }

    public static class TransferRequest {
        public String fromAccount;
        public String toAccount;
        public BigDecimal amount;
        public String pin;
    }

    // ==================== ENDPOINTS ====================

    @PostMapping("/accounts")
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody CreateAccountRequest req) {
        logger.info("REST request to create account for phone: {}", req.phoneNumber);
        if (req.phoneNumber == null || req.firstName == null || req.lastName == null || req.password == null) {
            return errorResponse(HttpStatus.BAD_REQUEST, "Missing required fields");
        }

        String accountNumber = "AC" + req.phoneNumber;
        String hashedPassword = BCrypt.hashpw(req.password, BCrypt.gensalt());

        Account account = bankService.createAccount(
                accountNumber, req.firstName, req.lastName, req.phoneNumber, hashedPassword, req.initialBalance, req.accountType);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("accountNumber", account.getAccountNumber());
        resp.put("accountHolderName", account.getAccountHolderName());
        resp.put("phoneNumber", account.getPhoneNumber());
        resp.put("balance", account.getBalance().setScale(2, RoundingMode.HALF_EVEN));
        resp.put("accountType", req.accountType);

        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest req) throws AccountNotFoundException {
        logger.info("REST login attempt for phone: {}", req.phoneNumber);
        Account account = bankService.getAccountByPhoneNumber(req.phoneNumber);

        if (verifyPassword(req.password, account.getPassword())) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("accountNumber", account.getAccountNumber());
            resp.put("accountHolderName", account.getAccountHolderName());
            resp.put("balance", account.getBalance().setScale(2, RoundingMode.HALF_EVEN));
            resp.put("phoneNumber", account.getPhoneNumber());
            return ResponseEntity.ok(resp);
        } else {
            return errorResponse(HttpStatus.UNAUTHORIZED, "Invalid PIN/Password");
        }
    }

    @GetMapping("/accounts/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getAccount(@PathVariable String accountNumber) throws AccountNotFoundException {
        Account account = bankService.getAccount(accountNumber);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("accountNumber", account.getAccountNumber());
        resp.put("accountHolderName", account.getAccountHolderName());
        resp.put("balance", account.getBalance().setScale(2, RoundingMode.HALF_EVEN));
        resp.put("phoneNumber", account.getPhoneNumber());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/accounts/{accountNumber}/statement")
    public ResponseEntity<Map<String, Object>> getStatement(@PathVariable String accountNumber) throws AccountNotFoundException {
        Account account = bankService.getAccount(accountNumber);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("accountNumber", account.getAccountNumber());
        resp.put("balance", account.getBalance().setScale(2, RoundingMode.HALF_EVEN));
        resp.put("transactions", account.getTransactionHistory());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/transactions/deposit")
    public ResponseEntity<Map<String, Object>> deposit(@RequestBody DepositRequest req) throws AccountNotFoundException {
        Account account = bankService.getAccount(req.accountNumber);
        if (req.pin != null && !verifyPassword(req.pin, account.getPassword())) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "Invalid PIN");
        }

        bankService.deposit(req.accountNumber, req.amount);
        Account updated = bankService.getAccount(req.accountNumber);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Deposit of $" + req.amount.setScale(2, RoundingMode.HALF_EVEN).toPlainString() + " successful");
        resp.put("newBalance", updated.getBalance().setScale(2, RoundingMode.HALF_EVEN));
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/transactions/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(@RequestBody WithdrawRequest req)
            throws AccountNotFoundException, InsufficientFundsException {
        Account account = bankService.getAccount(req.accountNumber);
        if (req.pin != null && !verifyPassword(req.pin, account.getPassword())) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "Invalid PIN");
        }

        bankService.withdraw(req.accountNumber, req.amount);
        Account updated = bankService.getAccount(req.accountNumber);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Withdrawal of $" + req.amount.setScale(2, RoundingMode.HALF_EVEN).toPlainString() + " successful");
        resp.put("newBalance", updated.getBalance().setScale(2, RoundingMode.HALF_EVEN));
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/transactions/transfer")
    public ResponseEntity<Map<String, Object>> transfer(@RequestBody TransferRequest req)
            throws AccountNotFoundException, InsufficientFundsException {
        Account sender = bankService.getAccount(req.fromAccount);
        if (req.pin != null && !verifyPassword(req.pin, sender.getPassword())) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "Invalid PIN");
        }

        bankService.transfer(req.fromAccount, req.toAccount, req.amount);
        Account updated = bankService.getAccount(req.fromAccount);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Transfer of $" + req.amount.setScale(2, RoundingMode.HALF_EVEN).toPlainString() + " to " + req.toAccount + " completed successfully");
        resp.put("newBalance", updated.getBalance().setScale(2, RoundingMode.HALF_EVEN));
        return ResponseEntity.ok(resp);
    }

    // ==================== EXCEPTION HANDLERS ====================

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(AccountNotFoundException ex) {
        return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientFunds(InsufficientFundsException ex) {
        return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        logger.error("Internal API error: {}", ex.getMessage(), ex);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error: " + ex.getMessage());
    }

    // ==================== HELPERS ====================

    private boolean verifyPassword(String input, String storedHash) {
        if (storedHash != null && (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$"))) {
            return BCrypt.checkpw(input, storedHash);
        }
        return input.equals(storedHash);
    }

    private ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return ResponseEntity.status(status).body(error);
    }
}
