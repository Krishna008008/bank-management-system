package com.example.bank.api;

import com.example.bank.dao.DatabaseManager;
import com.example.bank.service.BankService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

public class BankApiTest {
    private static final int TEST_PORT = 8089;
    private static BankServer bankServer;
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    @BeforeAll
    static void setUpAll() throws Exception {
        File testDb = new File("test_api_bank.db");
        if (testDb.exists()) {
            testDb.delete();
        }
        DatabaseManager.setDbUrl("jdbc:sqlite:test_api_bank.db");
        DatabaseManager.initializeDatabase();

        BankService bankService = new BankService();
        bankServer = new BankServer();
        bankServer.start(TEST_PORT, bankService);
    }

    @AfterAll
    static void tearDownAll() {
        if (bankServer != null) {
            bankServer.stop();
        }
        File testDb = new File("test_api_bank.db");
        if (testDb.exists()) {
            testDb.delete();
        }
    }

    @Test
    void testCreateAccountAndLoginApi() throws Exception {
        // 1. Create Account via POST /api/accounts
        String createJson = """
            {
                "phoneNumber": "8881112223",
                "firstName": "Grace",
                "lastName": "Hopper",
                "password": "4321",
                "initialBalance": 600.0,
                "accountType": "Savings"
            }
        """;

        HttpRequest createReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/accounts"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createJson))
                .build();

        HttpResponse<String> createResp = client.send(createReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, createResp.statusCode());

        JsonObject createObj = gson.fromJson(createResp.body(), JsonObject.class);
        assertTrue(createObj.get("success").getAsBoolean());
        assertEquals("AC8881112223", createObj.get("accountNumber").getAsString());

        // 2. Login via POST /api/auth/login
        String loginJson = """
            {
                "phoneNumber": "8881112223",
                "password": "4321"
            }
        """;

        HttpRequest loginReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                .build();

        HttpResponse<String> loginResp = client.send(loginReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, loginResp.statusCode());

        JsonObject loginObj = gson.fromJson(loginResp.body(), JsonObject.class);
        assertTrue(loginObj.get("success").getAsBoolean());
    }

    @Test
    void testDepositAndTransferApi() throws Exception {
        // 1. Create account A and B
        String accA = """
            {"phoneNumber":"7770001111","firstName":"UserA","lastName":"Test","password":"1111","initialBalance":500.0,"accountType":"Savings"}
        """;
        String accB = """
            {"phoneNumber":"7770002222","firstName":"UserB","lastName":"Test","password":"2222","initialBalance":200.0,"accountType":"Current"}
        """;

        client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + TEST_PORT + "/api/accounts")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(accA)).build(), HttpResponse.BodyHandlers.ofString());
        client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + TEST_PORT + "/api/accounts")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(accB)).build(), HttpResponse.BodyHandlers.ofString());

        // 2. Deposit $100 to A
        String depositJson = """
            {"accountNumber":"AC7770001111","amount":100.0,"pin":"1111"}
        """;
        HttpResponse<String> depResp = client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + TEST_PORT + "/api/transactions/deposit")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(depositJson)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, depResp.statusCode());

        // 3. Transfer $150 from A to B
        String transferJson = """
            {"fromAccount":"AC7770001111","toAccount":"AC7770002222","amount":150.0,"pin":"1111"}
        """;
        HttpResponse<String> txResp = client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + TEST_PORT + "/api/transactions/transfer")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(transferJson)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, txResp.statusCode());

        // 4. Verify Account B balance is 200 + 150 = 350
        HttpResponse<String> getB = client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + TEST_PORT + "/api/accounts/AC7770002222")).GET().build(), HttpResponse.BodyHandlers.ofString());
        JsonObject bObj = gson.fromJson(getB.body(), JsonObject.class);
        assertEquals(350.0, bObj.get("balance").getAsDouble());
    }
}
