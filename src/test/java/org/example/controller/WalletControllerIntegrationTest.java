package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.WalletRequest;
import org.example.enums.OperationType;
import org.example.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WalletControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID walletId;

    @BeforeEach
    void setup() {
        walletRepository.deleteAll();
        walletId = UUID.randomUUID();
        walletRepository.save(org.example.entity.Wallet.builder()
                .id(walletId)
                .balance(1000)
                .build());
    }

    @Test
    void getBalance_existingWallet_shouldReturnBalance() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/{id}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(1000));
    }

    @Test
    void getBalance_nonExistingWallet_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void postDeposit_shouldIncreaseBalance() throws Exception {
        WalletRequest request = new WalletRequest();
        request.setWalletId(walletId);
        request.setOperationType(OperationType.DEPOSIT);
        request.setAmount(500);

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Проверяем баланс
        mockMvc.perform(get("/api/v1/wallets/{id}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1500));
    }

    @Test
    void postWithdraw_insufficientFunds_shouldReturn422() throws Exception {
        WalletRequest request = new WalletRequest();
        request.setWalletId(walletId);
        request.setOperationType(OperationType.WITHDRAW);
        request.setAmount(2000);

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }
}