package com.example.personalfinancemanager.service;

import com.example.personalfinancemanager.dto.goal.*;
import com.example.personalfinancemanager.entity.SavingsGoal;
import com.example.personalfinancemanager.entity.Transaction;
import com.example.personalfinancemanager.entity.TransactionType;
import com.example.personalfinancemanager.entity.User;
import com.example.personalfinancemanager.exception.ForbiddenException;
import com.example.personalfinancemanager.exception.ResourceNotFoundException;
import com.example.personalfinancemanager.exception.ValidationException;
import com.example.personalfinancemanager.repository.SavingsGoalRepository;
import com.example.personalfinancemanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository,
                              TransactionRepository transactionRepository,
                              UserService userService) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    @Transactional
    public GoalResponse createGoal(GoalRequest request) {
        User user = userService.getCurrentAuthenticatedUser();

        if (request.getTargetAmount() == null || request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Target amount must be positive");
        }

        if (request.getTargetDate() == null || !request.getTargetDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Target date must be in the future");
        }

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();

        if (startDate.isAfter(request.getTargetDate())) {
            throw new ValidationException("Start date cannot be after target date");
        }

        SavingsGoal goal = new SavingsGoal();
        goal.setGoalName(request.getGoalName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setTargetDate(request.getTargetDate());
        goal.setStartDate(startDate);
        goal.setUser(user);

        SavingsGoal saved = savingsGoalRepository.save(goal);
        return calculateProgressAndMap(saved, user);
    }

    @Transactional(readOnly = true)
    public GoalListResponse getAllGoalsForCurrentUser() {
        User user = userService.getCurrentAuthenticatedUser();
        List<SavingsGoal> goals = savingsGoalRepository.findByUserOrderByIdDesc(user);

        List<GoalResponse> responses = goals.stream()
                .map(g -> calculateProgressAndMap(g, user))
                .collect(Collectors.toList());

        return new GoalListResponse(responses);
    }

    @Transactional(readOnly = true)
    public GoalResponse getGoalById(Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with id: " + id));

        if (!goal.getUser().equals(user)) {
            throw new ForbiddenException("Access denied to goal id: " + id);
        }

        return calculateProgressAndMap(goal, user);
    }

    @Transactional
    public GoalResponse updateGoal(Long id, GoalUpdateRequest request) {
        User user = userService.getCurrentAuthenticatedUser();

        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with id: " + id));

        if (!goal.getUser().equals(user)) {
            throw new ForbiddenException("Access denied to goal id: " + id);
        }

        if (request.getGoalName() != null && !request.getGoalName().isBlank()) {
            goal.setGoalName(request.getGoalName());
        }

        if (request.getTargetAmount() != null) {
            if (request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Target amount must be positive");
            }
            goal.setTargetAmount(request.getTargetAmount());
        }

        if (request.getTargetDate() != null) {
            if (!request.getTargetDate().isAfter(LocalDate.now())) {
                throw new ValidationException("Target date must be in the future");
            }
            if (goal.getStartDate() != null && request.getTargetDate().isBefore(goal.getStartDate())) {
                throw new ValidationException("Target date cannot be before start date");
            }
            goal.setTargetDate(request.getTargetDate());
        }

        SavingsGoal updated = savingsGoalRepository.save(goal);
        return calculateProgressAndMap(updated, user);
    }

    @Transactional
    public void deleteGoal(Long id) {
        User user = userService.getCurrentAuthenticatedUser();

        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with id: " + id));

        if (!goal.getUser().equals(user)) {
            throw new ForbiddenException("Access denied to goal id: " + id);
        }

        savingsGoalRepository.delete(goal);
    }

    private GoalResponse calculateProgressAndMap(SavingsGoal goal, User user) {
        List<Transaction> transactions = transactionRepository.findByUserAndDateGreaterThanEqual(user, goal.getStartDate());

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(t.getAmount());
            } else if (t.getType() == TransactionType.EXPENSE) {
                totalExpenses = totalExpenses.add(t.getAmount());
            }
        }

        BigDecimal currentProgress = totalIncome.subtract(totalExpenses);

        // Remaining amount calculation
        BigDecimal remainingAmount;
        if (currentProgress.compareTo(goal.getTargetAmount()) >= 0) {
            remainingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else if (currentProgress.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = goal.getTargetAmount().setScale(2, RoundingMode.HALF_UP);
        } else {
            remainingAmount = goal.getTargetAmount().subtract(currentProgress).setScale(2, RoundingMode.HALF_UP);
        }

        // Percentage calculation
        double progressPercentage = 0.0;
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (currentProgress.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = currentProgress.divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                progressPercentage = percentage.setScale(2, RoundingMode.HALF_UP).doubleValue();
            }
        }

        return new GoalResponse(
                goal.getId(),
                goal.getGoalName(),
                goal.getTargetAmount().setScale(2, RoundingMode.HALF_UP),
                goal.getTargetDate(),
                goal.getStartDate(),
                currentProgress.setScale(2, RoundingMode.HALF_UP),
                progressPercentage,
                remainingAmount
        );
    }
}
