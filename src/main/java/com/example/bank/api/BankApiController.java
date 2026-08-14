package com.example.bank.api;

import com.example.bank.exception.AccountNotFoundException;
import com.example.bank.exception.InsufficientFundsException;
import com.example.bank.model.Account;
import com.example.bank.service.BankService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class BankApiController implements HttpHandler {
    private static final Logger logger = LogManager.getLogger(BankApiController.class);
    private static final Gson gson = new Gson();
    private final BankService bankService;

    public BankApiController(BankService bankService) {
        this.bankService = bankService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Handle CORS Pre-flight options request
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 204, "");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        logger.info("Incoming HTTP {} request to {}", method, path);

        try {
            if ("POST".equalsIgnoreCase(method) && "/api/accounts".equals(path)) {
                handleCreateAccount(exchange);
            } else if ("POST".equalsIgnoreCase(method) && "/api/auth/login".equals(path)) {
                handleLogin(exchange);
            } else if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/accounts/") && path.endsWith("/statement")) {
                handleGetStatement(exchange, path);
            } else if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/accounts/")) {
                handleGetAccount(exchange, path);
            } else if ("POST".equalsIgnoreCase(method) && "/api/transactions/deposit".equals(path)) {
                handleDeposit(exchange);
            } else if ("POST".equalsIgnoreCase(method) && "/api/transactions/withdraw".equals(path)) {
                handleWithdraw(exchange);
            } else if ("POST".equalsIgnoreCase(method) && "/api/transactions/transfer".equals(path)) {
                handleTransfer(exchange);
            } else {
                sendError(exchange, 404, "Endpoint not found: " + path);
            }
        } catch (Exception e) {
            logger.error("Unhandled API exception on {}: {}", path, e.getMessage(), e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleCreateAccount(HttpExchange exchange) throws IOException {
        JsonObject body = parseRequestBody(exchange);
        if (body == null) {
            sendError(exchange, 400, "Invalid JSON payload");
            return;
        }

        try {
            String phoneNumber = body.get("phoneNumber").getAsString();
            String firstName = body.get("firstName").getAsString();
            String lastName = body.get("lastName").getAsString();
            String password = body.get("password").getAsString();
            double initialBalance = body.get("initialBalance").getAsDouble();
            String accountType = body.get("accountType").getAsString();

            String accountNumber = "AC" + phoneNumber;
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            Account account = bankService.createAccount(
                    accountNumber, firstName, lastName, phoneNumber, hashedPassword, initialBalance, accountType);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("accountNumber", account.getAccountNumber());
            resp.put("accountHolderName", account.getAccountHolderName());
            resp.put("phoneNumber", account.getPhoneNumber());
            resp.put("balance", account.getBalance());
            resp.put("accountType", accountType);

            sendJsonResponse(exchange, 201, resp);
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        JsonObject body = parseRequestBody(exchange);
        if (body == null) {
            sendError(exchange, 400, "Invalid JSON payload");
            return;
        }

        try {
            String phoneNumber = body.get("phoneNumber").getAsString();
            String password = body.get("password").getAsString();

            Account account = bankService.getAccountByPhoneNumber(phoneNumber);
            if (verifyPassword(password, account.getPassword())) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", true);
                resp.put("accountNumber", account.getAccountNumber());
                resp.put("accountHolderName", account.getAccountHolderName());
                resp.put("balance", account.getBalance());
                resp.put("phoneNumber", account.getPhoneNumber());
                sendJsonResponse(exchange, 200, resp);
            } else {
                sendError(exchange, 401, "Invalid PIN/Password");
            }
        } catch (AccountNotFoundException e) {
            sendError(exchange, 404, e.getMessage());
        }
    }

    private void handleGetAccount(HttpExchange exchange, String path) throws IOException {
        String accNum = path.substring("/api/accounts/".length()).trim();
        try {
            Account account = bankService.getAccount(accNum);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("accountNumber", account.getAccountNumber());
            resp.put("accountHolderName", account.getAccountHolderName());
            resp.put("balance", account.getBalance());
            resp.put("phoneNumber", account.getPhoneNumber());
            sendJsonResponse(exchange, 200, resp);
        } catch (AccountNotFoundException e) {
            sendError(exchange, 404, e.getMessage());
        }
    }

    private void handleGetStatement(HttpExchange exchange, String path) throws IOException {
        String accNum = path.replace("/api/accounts/", "").replace("/statement", "").trim();
        try {
            Account account = bankService.getAccount(accNum);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("accountNumber", account.getAccountNumber());
            resp.put("balance", account.getBalance());
            // Statement records are mapped in memory by AccountDao
            sendJsonResponse(exchange, 200, resp);
        } catch (AccountNotFoundException e) {
            sendError(exchange, 404, e.getMessage());
        }
    }

    private void handleDeposit(HttpExchange exchange) throws IOException {
        JsonObject body = parseRequestBody(exchange);
        if (body == null) {
            sendError(exchange, 400, "Invalid JSON payload");
            return;
        }

        try {
            String accNum = body.get("accountNumber").getAsString();
            double amount = body.get("amount").getAsDouble();
            String pin = body.has("pin") ? body.get("pin").getAsString() : null;

            Account account = bankService.getAccount(accNum);
            if (pin != null && !verifyPassword(pin, account.getPassword())) {
                sendError(exchange, 401, "Invalid PIN");
                return;
            }

            bankService.deposit(accNum, amount);
            Account updated = bankService.getAccount(accNum);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Deposit of $" + amount + " successful");
            resp.put("newBalance", updated.getBalance());
            sendJsonResponse(exchange, 200, resp);
        } catch (AccountNotFoundException e) {
            sendError(exchange, 404, e.getMessage());
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        }
    }

    private void handleWithdraw(HttpExchange exchange) throws IOException {
        JsonObject body = parseRequestBody(exchange);
        if (body == null) {
            sendError(exchange, 400, "Invalid JSON payload");
            return;
        }

        try {
            String accNum = body.get("accountNumber").getAsString();
            double amount = body.get("amount").getAsDouble();
            String pin = body.has("pin") ? body.get("pin").getAsString() : null;

            Account account = bankService.getAccount(accNum);
            if (pin != null && !verifyPassword(pin, account.getPassword())) {
                sendError(exchange, 401, "Invalid PIN");
                return;
            }

            bankService.withdraw(accNum, amount);
            Account updated = bankService.getAccount(accNum);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Withdrawal of $" + amount + " successful");
            resp.put("newBalance", updated.getBalance());
            sendJsonResponse(exchange, 200, resp);
        } catch (AccountNotFoundException e) {
            sendError(exchange, 404, e.getMessage());
        } catch (InsufficientFundsException e) {
            sendError(exchange, 400, e.getMessage());
        }
    }

    private void handleTransfer(HttpExchange exchange) throws IOException {
        JsonObject body = parseRequestBody(exchange);
        if (body == null) {
            sendError(exchange, 400, "Invalid JSON payload");
            return;
        }

        try {
            String fromAcc = body.get("fromAccount").getAsString();
            String toAcc = body.get("toAccount").getAsString();
            double amount = body.get("amount").getAsDouble();
            String pin = body.has("pin") ? body.get("pin").getAsString() : null;

            Account sender = bankService.getAccount(fromAcc);
            if (pin != null && !verifyPassword(pin, sender.getPassword())) {
                sendError(exchange, 401, "Invalid PIN");
                return;
            }

            bankService.transfer(fromAcc, toAcc, amount);
            Account updated = bankService.getAccount(fromAcc);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Transfer of $" + amount + " to " + toAcc + " completed successfully");
            resp.put("newBalance", updated.getBalance());
            sendJsonResponse(exchange, 200, resp);
        } catch (AccountNotFoundException e) {
            sendError(exchange, 404, e.getMessage());
        } catch (InsufficientFundsException | IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        }
    }

    private boolean verifyPassword(String input, String storedHash) {
        if (storedHash != null && (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$"))) {
            return BCrypt.checkpw(input, storedHash);
        }
        return input.equals(storedHash);
    }

    private JsonObject parseRequestBody(HttpExchange exchange) {
        try (InputStream is = exchange.getRequestBody()) {
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return gson.fromJson(json, JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        sendResponse(exchange, statusCode, json);
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        sendResponse(exchange, statusCode, gson.toJson(error));
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
        byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if (statusCode == 204 || bytes.length == 0) {
            exchange.sendResponseHeaders(statusCode, -1);
        } else {
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
