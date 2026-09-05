package com.example.personalfinancemanager.controller;

import com.example.personalfinancemanager.dto.goal.*;
import com.example.personalfinancemanager.service.SavingsGoalService;
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
class SavingsGoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SavingsGoalService savingsGoalService;

    @Test
    @WithMockUser
    void createGoal_Success_Returns201Created() throws Exception {
        GoalRequest request = new GoalRequest("Emergency Fund", BigDecimal.valueOf(5000.00), LocalDate.now().plusYears(1), LocalDate.now());
        GoalResponse response = new GoalResponse(1L, "Emergency Fund", BigDecimal.valueOf(5000.00), LocalDate.now().plusYears(1), LocalDate.now(), BigDecimal.valueOf(1000.00), 20.0, BigDecimal.valueOf(4000.00));

        when(savingsGoalService.createGoal(any(GoalRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.goalName").value("Emergency Fund"))
                .andExpect(jsonPath("$.currentProgress").value(1000.00))
                .andExpect(jsonPath("$.progressPercentage").value(20.0));
    }

    @Test
    @WithMockUser
    void getAllGoals_Success_Returns200OK() throws Exception {
        GoalResponse response = new GoalResponse(1L, "Emergency Fund", BigDecimal.valueOf(5000.00), LocalDate.now().plusYears(1), LocalDate.now(), BigDecimal.valueOf(1000.00), 20.0, BigDecimal.valueOf(4000.00));
        when(savingsGoalService.getAllGoalsForCurrentUser()).thenReturn(new GoalListResponse(List.of(response)));

        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goals.length()").value(1));
    }

    @Test
    @WithMockUser
    void getGoalById_Success_Returns200OK() throws Exception {
        GoalResponse response = new GoalResponse(1L, "Emergency Fund", BigDecimal.valueOf(5000.00), LocalDate.now().plusYears(1), LocalDate.now(), BigDecimal.valueOf(1000.00), 20.0, BigDecimal.valueOf(4000.00));
        when(savingsGoalService.getGoalById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/goals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void updateGoal_Success_Returns200OK() throws Exception {
        GoalUpdateRequest request = new GoalUpdateRequest("Emergency Fund", BigDecimal.valueOf(6000.00), LocalDate.now().plusYears(1));
        GoalResponse response = new GoalResponse(1L, "Emergency Fund", BigDecimal.valueOf(6000.00), LocalDate.now().plusYears(1), LocalDate.now(), BigDecimal.valueOf(1000.00), 16.67, BigDecimal.valueOf(5000.00));

        when(savingsGoalService.updateGoal(eq(1L), any(GoalUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/goals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetAmount").value(6000.00));
    }

    @Test
    @WithMockUser
    void deleteGoal_Success_Returns200OK() throws Exception {
        doNothing().when(savingsGoalService).deleteGoal(1L);

        mockMvc.perform(delete("/api/goals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Goal deleted successfully"));
    }

    @Test
    void getAllGoals_Unauthenticated_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isUnauthorized());
    }
}
