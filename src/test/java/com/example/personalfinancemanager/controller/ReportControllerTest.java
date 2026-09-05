package com.example.personalfinancemanager.controller;

import com.example.personalfinancemanager.dto.report.MonthlyReportResponse;
import com.example.personalfinancemanager.dto.report.YearlyReportResponse;
import com.example.personalfinancemanager.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    @WithMockUser
    void getMonthlyReport_Success_Returns200OK() throws Exception {
        MonthlyReportResponse response = new MonthlyReportResponse(1, 2024, Map.of("Salary", BigDecimal.valueOf(3000.00)), Map.of("Food", BigDecimal.valueOf(400.00)), BigDecimal.valueOf(2600.00));
        when(reportService.getMonthlyReport(2024, 1)).thenReturn(response);

        mockMvc.perform(get("/api/reports/monthly/2024/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(1))
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.netSavings").value(2600.00));
    }

    @Test
    @WithMockUser
    void getYearlyReport_Success_Returns200OK() throws Exception {
        YearlyReportResponse response = new YearlyReportResponse(2024, Map.of("Salary", BigDecimal.valueOf(36000.00)), Map.of("Food", BigDecimal.valueOf(4800.00)), BigDecimal.valueOf(31200.00));
        when(reportService.getYearlyReport(2024)).thenReturn(response);

        mockMvc.perform(get("/api/reports/yearly/2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.netSavings").value(31200.00));
    }

    @Test
    void getMonthlyReport_Unauthenticated_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/reports/monthly/2024/1"))
                .andExpect(status().isUnauthorized());
    }
}
