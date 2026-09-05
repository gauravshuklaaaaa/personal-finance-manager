package com.example.personalfinancemanager.service;

import com.example.personalfinancemanager.dto.transaction.*;
import com.example.personalfinancemanager.entity.Category;
import com.example.personalfinancemanager.entity.Transaction;
import com.example.personalfinancemanager.entity.TransactionType;
import com.example.personalfinancemanager.entity.User;
import com.example.personalfinancemanager.exception.ForbiddenException;
import com.example.personalfinancemanager.exception.ResourceNotFoundException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransactionService transactionService;

    private User user1;
    private User user2;
    private Category salaryCategory;
    private Transaction sampleTransaction;

    @BeforeEach
    void setUp() {
        user1 = new User(1L, "user1@example.com", "password", "User One", "+1234567890");
        user2 = new User(2L, "user2@example.com", "password", "User Two", "+0987654321");
        salaryCategory = new Category(10L, "Salary", TransactionType.INCOME, false, null);
        sampleTransaction = new Transaction(100L, BigDecimal.valueOf(5000.00), LocalDate.now().minusDays(1), salaryCategory, "Monthly salary", TransactionType.INCOME, user1);
    }

    @Test
    void createTransaction_Success() {
        TransactionRequest request = new TransactionRequest(BigDecimal.valueOf(5000.00), LocalDate.now().minusDays(1), "Salary", "Monthly salary");

        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(categoryService.getCategoryByNameForUser("Salary", user1)).thenReturn(salaryCategory);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        TransactionResponse response = transactionService.createTransaction(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(BigDecimal.valueOf(5000.00), response.getAmount());
        assertEquals("Salary", response.getCategory());
        assertEquals(TransactionType.INCOME, response.getType());
    }

    @Test
    void createTransaction_NegativeAmount_ThrowsValidationException() {
        TransactionRequest request = new TransactionRequest(BigDecimal.valueOf(-500.00), LocalDate.now(), "Salary", "Test");
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);

        assertThrows(ValidationException.class, () -> transactionService.createTransaction(request));
    }

    @Test
    void createTransaction_FutureDate_ThrowsValidationException() {
        TransactionRequest request = new TransactionRequest(BigDecimal.valueOf(500.00), LocalDate.now().plusDays(1), "Salary", "Test");
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);

        assertThrows(ValidationException.class, () -> transactionService.createTransaction(request));
    }

    @Test
    void getTransactions_Success() {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(transactionRepository.findFilteredTransactions(user1, null, null, null, null))
                .thenReturn(List.of(sampleTransaction));

        TransactionListResponse response = transactionService.getTransactions(null, null, null, null);

        assertNotNull(response);
        assertEquals(1, response.getTransactions().size());
        assertEquals(100L, response.getTransactions().get(0).getId());
    }

    @Test
    void getTransactions_InvalidDateRange_ThrowsValidationException() {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().minusDays(5);

        assertThrows(ValidationException.class, () -> transactionService.getTransactions(start, end, null, null));
    }

    @Test
    void updateTransaction_Success() {
        TransactionUpdateRequest request = new TransactionUpdateRequest(BigDecimal.valueOf(6000.00), null, "Updated description");
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(transactionRepository.findById(100L)).thenReturn(Optional.of(sampleTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        TransactionResponse response = transactionService.updateTransaction(100L, request);

        assertNotNull(response);
        verify(transactionRepository, times(1)).save(sampleTransaction);
    }

    @Test
    void updateTransaction_DateModificationAttempt_ThrowsValidationException() {
        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setDate(LocalDate.now().minusDays(10)); // Different date

        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(transactionRepository.findById(100L)).thenReturn(Optional.of(sampleTransaction));

        assertThrows(ValidationException.class, () -> transactionService.updateTransaction(100L, request));
    }

    @Test
    void updateTransaction_AnotherUsersTransaction_ThrowsForbiddenException() {
        TransactionUpdateRequest request = new TransactionUpdateRequest(BigDecimal.valueOf(6000.00), null, "Updated");
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user2);
        when(transactionRepository.findById(100L)).thenReturn(Optional.of(sampleTransaction));

        assertThrows(ForbiddenException.class, () -> transactionService.updateTransaction(100L, request));
    }

    @Test
    void updateTransaction_NotFound_ThrowsResourceNotFoundException() {
        TransactionUpdateRequest request = new TransactionUpdateRequest();
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.updateTransaction(999L, request));
    }

    @Test
    void deleteTransaction_Success() {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(transactionRepository.findById(100L)).thenReturn(Optional.of(sampleTransaction));

        assertDoesNotThrow(() -> transactionService.deleteTransaction(100L));
        verify(transactionRepository, times(1)).delete(sampleTransaction);
    }

    @Test
    void deleteTransaction_AnotherUser_ThrowsForbiddenException() {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user2);
        when(transactionRepository.findById(100L)).thenReturn(Optional.of(sampleTransaction));

        assertThrows(ForbiddenException.class, () -> transactionService.deleteTransaction(100L));
        verify(transactionRepository, never()).delete(any());
    }

    @Test
    void deleteTransaction_NotFound_ThrowsResourceNotFoundException() {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.deleteTransaction(999L));
    }
}
