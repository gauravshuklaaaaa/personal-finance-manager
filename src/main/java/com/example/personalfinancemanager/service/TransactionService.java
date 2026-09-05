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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final UserService userService;

    public TransactionService(TransactionRepository transactionRepository,
                               CategoryService categoryService,
                               UserService userService) {
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
        this.userService = userService;
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        User user = userService.getCurrentAuthenticatedUser();

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be positive");
        }

        if (request.getDate() != null && request.getDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Transaction date cannot be in the future");
        }

        Category category = categoryService.getCategoryByNameForUser(request.getCategory(), user);

        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setDate(request.getDate());
        transaction.setCategory(category);
        transaction.setDescription(request.getDescription());
        transaction.setType(category.getType()); // Type is derived from category
        transaction.setUser(user);

        Transaction saved = transactionRepository.save(transaction);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public TransactionListResponse getTransactions(LocalDate startDate, LocalDate endDate, Long categoryId, TransactionType type) {
        User user = userService.getCurrentAuthenticatedUser();

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ValidationException("startDate cannot be after endDate");
        }

        List<Transaction> transactions = transactionRepository.findFilteredTransactions(user, startDate, endDate, categoryId, type);
        List<TransactionResponse> responses = transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new TransactionListResponse(responses);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionUpdateRequest request) {
        User user = userService.getCurrentAuthenticatedUser();

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        if (!transaction.getUser().equals(user)) {
            throw new ForbiddenException("Access denied to transaction id: " + id);
        }

        // Validate date modification restriction
        if (request.getDate() != null && !request.getDate().equals(transaction.getDate())) {
            throw new ValidationException("The date field cannot be modified");
        }

        if (request.getAmount() != null) {
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Amount must be positive");
            }
            transaction.setAmount(request.getAmount());
        }

        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            Category category = categoryService.getCategoryByNameForUser(request.getCategory(), user);
            transaction.setCategory(category);
            transaction.setType(category.getType()); // Type derived from new category
        }

        if (request.getDescription() != null) {
            transaction.setDescription(request.getDescription());
        }

        Transaction updated = transactionRepository.save(transaction);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        User user = userService.getCurrentAuthenticatedUser();

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        if (!transaction.getUser().equals(user)) {
            throw new ForbiddenException("Access denied to transaction id: " + id);
        }

        transactionRepository.delete(transaction);
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAmount(),
                t.getDate(),
                t.getCategory().getName(),
                t.getDescription(),
                t.getType()
        );
    }
}
