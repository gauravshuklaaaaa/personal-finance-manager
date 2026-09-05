package com.example.personalfinancemanager.service;

import com.example.personalfinancemanager.dto.goal.*;
import com.example.personalfinancemanager.entity.Category;
import com.example.personalfinancemanager.entity.SavingsGoal;
import com.example.personalfinancemanager.entity.Transaction;
import com.example.personalfinancemanager.entity.TransactionType;
import com.example.personalfinancemanager.entity.User;
import com.example.personalfinancemanager.exception.ForbiddenException;
import com.example.personalfinancemanager.exception.ResourceNotFoundException;
import com.example.personalfinancemanager.exception.ValidationException;
import com.example.personalfinancemanager.repository.SavingsGoalRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavingsGoalServiceTest {

    @Mock
    private SavingsGoalRepository savingsGoalRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private SavingsGoalService savingsGoalService;

    private User user1;
    private User user2;
    private SavingsGoal sampleGoal;
    private Category incomeCat;
    private Category expenseCat;

    @BeforeEach
    void setUp() {
        user1 = new User(1L, "user1@example.com", "password", "User One", "+1234567890");
        user2 = new User(2L, "user2@example.com", "password", "User Two", "+0987654321");
        sampleGoal = new SavingsGoal(10L, "Emergency Fund", BigDecimal.valueOf(5000.00), LocalDate.now().plusMonths(6), LocalDate.now().minusDays(10), user1);

        incomeCat = new Category(1L, "Salary", TransactionType.INCOME, false, null);
        expenseCat = new Category(2L, "Rent", TransactionType.EXPENSE, false, null);
    }

    @Test
    void createGoal_Success() {
        GoalRequest request = new GoalRequest("Vacation", BigDecimal.valueOf(2000.00), LocalDate.now().plusMonths(3), LocalDate.now());
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenReturn(sampleGoal);
        when(transactionRepository.findByUserAndDateGreaterThanEqual(user1, sampleGoal.getStartDate())).thenReturn(List.of());

        GoalResponse response = savingsGoalService.createGoal(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Emergency Fund", response.getGoalName());
    }

    @Test
    void createGoal_InvalidTargetAmount_ThrowsValidationException() {
        GoalRequest request = new GoalRequest("Vacation", BigDecimal.valueOf(-100.00), LocalDate.now().plusMonths(3), null);
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);

        assertThrows(ValidationException.class, () -> savingsGoalService.createGoal(request));
    }

    @Test
    void createGoal_PastTargetDate_ThrowsValidationException() {
        GoalRequest request = new GoalRequest("Vacation", BigDecimal.valueOf(1000.00), LocalDate.now().minusDays(1), null);
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);

        assertThrows(ValidationException.class, () -> savingsGoalService.createGoal(request));
    }

    @Test
    void createGoal_StartDateAfterTargetDate_ThrowsValidationException() {
        GoalRequest request = new GoalRequest("Vacation", BigDecimal.valueOf(1000.00), LocalDate.now().plusDays(5), LocalDate.now().plusDays(10));
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);

        assertThrows(ValidationException.class, () -> savingsGoalService.createGoal(request));
    }

    @Test
    void getAllGoalsForCurrentUser_Success_CalculatesProgressCorrectly() {
        Transaction t1 = new Transaction(1L, BigDecimal.valueOf(3000.00), LocalDate.now(), incomeCat, "Salary", TransactionType.INCOME, user1);
        Transaction t2 = new Transaction(2L, BigDecimal.valueOf(1000.00), LocalDate.now(), expenseCat, "Rent", TransactionType.EXPENSE, user1);

        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(savingsGoalRepository.findByUserOrderByIdDesc(user1)).thenReturn(List.of(sampleGoal));
        when(transactionRepository.findByUserAndDateGreaterThanEqual(user1, sampleGoal.getStartDate())).thenReturn(List.of(t1, t2));

        GoalListResponse response = savingsGoalService.getAllGoalsForCurrentUser();

        assertNotNull(response);
        assertEquals(1, response.getGoals().size());
        GoalResponse goalResp = response.getGoals().get(0);
        assertEquals(BigDecimal.valueOf(2000.00).setScale(2), goalResp.getCurrentProgress()); // 3000 - 1000 = 2000
        assertEquals(BigDecimal.valueOf(3000.00).setScale(2), goalResp.getRemainingAmount()); // 5000 - 2000 = 3000
        assertEquals(40.0, goalResp.getProgressPercentage()); // (2000/5000) * 100 = 40%
    }

    @Test
    void getGoalById_Success() {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(savingsGoalRepository.findById(10L)).thenReturn(Optional.of(sampleGoal));
        when(transactionRepository.findByUserAndDateGreaterThanEqual(user1, sampleGoal.getStartDate())).thenReturn(List.of());

        GoalResponse response = savingsGoalService.getGoalById(10L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
    }

    @Test
    void getGoalById_AnotherUser_ThrowsForbiddenException() {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user2);
        when(savingsGoalRepository.findById(10L)).thenReturn(Optional.of(sampleGoal));

        assertThrows(ForbiddenException.class, () -> savingsGoalService.getGoalById(10L));
    }

    @Test
    void updateGoal_Success() {
        GoalUpdateRequest request = new GoalUpdateRequest("New Fund Name", BigDecimal.valueOf(6000.00), LocalDate.now().plusMonths(8));
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(savingsGoalRepository.findById(10L)).thenReturn(Optional.of(sampleGoal));
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenReturn(sampleGoal);
        when(transactionRepository.findByUserAndDateGreaterThanEqual(user1, sampleGoal.getStartDate())).thenReturn(List.of());

        GoalResponse response = savingsGoalService.updateGoal(10L, request);

        assertNotNull(response);
        verify(savingsGoalRepository, times(1)).save(sampleGoal);
    }

    @Test
    void deleteGoal_Success() {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(savingsGoalRepository.findById(10L)).thenReturn(Optional.of(sampleGoal));

        assertDoesNotThrow(() -> savingsGoalService.deleteGoal(10L));
        verify(savingsGoalRepository, times(1)).delete(sampleGoal);
    }

    @Test
    void deleteGoal_AnotherUser_ThrowsForbiddenException() {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user2);
        when(savingsGoalRepository.findById(10L)).thenReturn(Optional.of(sampleGoal));

        assertThrows(ForbiddenException.class, () -> savingsGoalService.deleteGoal(10L));
        verify(savingsGoalRepository, never()).delete(any());
    }
}
