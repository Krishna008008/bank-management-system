package com.example.bank.api;

import com.example.bank.dao.DatabaseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BankApiTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        File testDb = new File("test_api_bank.db");
        if (testDb.exists()) {
            testDb.delete();
        }
        DatabaseManager.setDbUrl("jdbc:sqlite:test_api_bank.db");
        DatabaseManager.initializeDatabase();
    }

    @Test
    void testCreateAccountAndLoginApi() throws Exception {
        String createJson = """
            {
                "phoneNumber": "8881112223",
                "firstName": "Grace",
                "lastName": "Hopper",
                "password": "4321",
                "initialBalance": 600.00,
                "accountType": "Savings"
            }
        """;

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.accountNumber").value("AC8881112223"));

        String loginJson = """
            {
                "phoneNumber": "8881112223",
                "password": "4321"
            }
        """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.accountHolderName").value("Grace Hopper"));
    }

    @Test
    void testDepositAndTransferApi() throws Exception {
        String accA = """
            {"phoneNumber":"7770001111","firstName":"UserA","lastName":"Test","password":"1111","initialBalance":500.00,"accountType":"Savings"}
        """;
        String accB = """
            {"phoneNumber":"7770002222","firstName":"UserB","lastName":"Test","password":"2222","initialBalance":200.00,"accountType":"Current"}
        """;

        mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(accA))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(accB))
                .andExpect(status().isCreated());

        String depositJson = """
            {"accountNumber":"AC7770001111","amount":100.00,"pin":"1111"}
        """;
        mockMvc.perform(post("/api/transactions/deposit").contentType(MediaType.APPLICATION_JSON).content(depositJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String transferJson = """
            {"fromAccount":"AC7770001111","toAccount":"AC7770002222","amount":150.00,"pin":"1111"}
        """;
        mockMvc.perform(post("/api/transactions/transfer").contentType(MediaType.APPLICATION_JSON).content(transferJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/accounts/AC7770002222"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(350.00));
    }
}
