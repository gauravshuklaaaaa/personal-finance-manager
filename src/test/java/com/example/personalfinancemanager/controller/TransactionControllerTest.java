package com.example.personalfinancemanager.controller;

import com.example.personalfinancemanager.dto.transaction.*;
import com.example.personalfinancemanager.entity.TransactionType;
import com.example.personalfinancemanager.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Test
    @WithMockUser
    void createTransaction_Success_Returns201Created() throws Exception {
        TransactionRequest request = new TransactionRequest(BigDecimal.valueOf(50000.00), LocalDate.now().minusDays(1), "Salary", "January Salary");
        TransactionResponse response = new TransactionResponse(1L, BigDecimal.valueOf(50000.00), LocalDate.now().minusDays(1), "Salary", "January Salary", TransactionType.INCOME);

        when(transactionService.createTransaction(any(TransactionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(50000.00))
                .andExpect(jsonPath("$.type").value("INCOME"));
    }

    @Test
    @WithMockUser
    void getTransactions_Success_Returns200OK() throws Exception {
        TransactionResponse response = new TransactionResponse(1L, BigDecimal.valueOf(50000.00), LocalDate.now().minusDays(1), "Salary", "January Salary", TransactionType.INCOME);
        when(transactionService.getTransactions(null, null, null, null)).thenReturn(new TransactionListResponse(List.of(response)));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions.length()").value(1));
    }

    @Test
    @WithMockUser
    void updateTransaction_Success_Returns200OK() throws Exception {
        TransactionUpdateRequest request = new TransactionUpdateRequest(BigDecimal.valueOf(60000.00), null, "Updated Salary");
        TransactionResponse response = new TransactionResponse(1L, BigDecimal.valueOf(60000.00), LocalDate.now().minusDays(1), "Salary", "Updated Salary", TransactionType.INCOME);

        when(transactionService.updateTransaction(eq(1L), any(TransactionUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(60000.00))
                .andExpect(jsonPath("$.description").value("Updated Salary"));
    }

    @Test
    @WithMockUser
    void deleteTransaction_Success_Returns200OK() throws Exception {
        doNothing().when(transactionService).deleteTransaction(1L);

        mockMvc.perform(delete("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction deleted successfully"));
    }

    @Test
    void getTransactions_Unauthenticated_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }
}
