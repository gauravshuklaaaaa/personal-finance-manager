package com.example.personalfinancemanager.service;

import com.example.personalfinancemanager.dto.report.MonthlyReportResponse;
import com.example.personalfinancemanager.dto.report.YearlyReportResponse;
import com.example.personalfinancemanager.entity.Category;
import com.example.personalfinancemanager.entity.Transaction;
import com.example.personalfinancemanager.entity.TransactionType;
import com.example.personalfinancemanager.entity.User;
import com.example.personalfinancemanager.exception.ValidationException;
import com.example.personalfinancemanager.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReportService reportService;

    private User user1;
    private Category salaryCategory;
    private Category rentCategory;
    private Transaction incomeTx;
    private Transaction expenseTx;

    @BeforeEach
    void setUp() {
        user1 = new User(1L, "user1@example.com", "password", "User One", "+1234567890");
        salaryCategory = new Category(10L, "Salary", TransactionType.INCOME, false, null);
        rentCategory = new Category(11L, "Rent", TransactionType.EXPENSE, false, null);

        incomeTx = new Transaction(100L, BigDecimal.valueOf(3000.00), LocalDate.of(2024, 1, 15), salaryCategory, "Jan Salary", TransactionType.INCOME, user1);
        expenseTx = new Transaction(101L, BigDecimal.valueOf(1200.00), LocalDate.of(2024, 1, 20), rentCategory, "Jan Rent", TransactionType.EXPENSE, user1);
    }

    @Test
    void getMonthlyReport_Success() {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(transactionRepository.findByUserAndDateBetween(eq(user1), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(incomeTx, expenseTx));

        MonthlyReportResponse report = reportService.getMonthlyReport(2024, 1);

        assertNotNull(report);
        assertEquals(1, report.getMonth());
        assertEquals(2024, report.getYear());
        assertEquals(1, report.getTotalIncome().size());
        assertEquals(BigDecimal.valueOf(3000.00), report.getTotalIncome().get("Salary"));
        assertEquals(1, report.getTotalExpenses().size());
        assertEquals(BigDecimal.valueOf(1200.00), report.getTotalExpenses().get("Rent"));
        assertEquals(BigDecimal.valueOf(1800.00).setScale(2), report.getNetSavings());
    }

    @Test
    void getMonthlyReport_InvalidMonth_ThrowsValidationException() {
        assertThrows(ValidationException.class, () -> reportService.getMonthlyReport(2024, 13));
        assertThrows(ValidationException.class, () -> reportService.getMonthlyReport(2024, 0));
    }

    @Test
    void getYearlyReport_Success() {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(transactionRepository.findByUserAndDateBetween(eq(user1), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(incomeTx, expenseTx));

        YearlyReportResponse report = reportService.getYearlyReport(2024);

        assertNotNull(report);
        assertEquals(2024, report.getYear());
        assertEquals(BigDecimal.valueOf(3000.00), report.getTotalIncome().get("Salary"));
        assertEquals(BigDecimal.valueOf(1200.00), report.getTotalExpenses().get("Rent"));
        assertEquals(BigDecimal.valueOf(1800.00).setScale(2), report.getNetSavings());
    }

    @Test
    void getYearlyReport_InvalidYear_ThrowsValidationException() {
        assertThrows(ValidationException.class, () -> reportService.getYearlyReport(1800));
    }
}
