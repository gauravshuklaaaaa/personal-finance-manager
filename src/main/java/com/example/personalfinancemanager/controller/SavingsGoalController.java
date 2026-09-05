package com.example.personalfinancemanager.controller;

import com.example.personalfinancemanager.dto.goal.*;
import com.example.personalfinancemanager.service.SavingsGoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    public SavingsGoalController(SavingsGoalService savingsGoalService) {
        this.savingsGoalService = savingsGoalService;
    }

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody GoalRequest request) {
        GoalResponse response = savingsGoalService.createGoal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<GoalListResponse> getAllGoals() {
        GoalListResponse response = savingsGoalService.getAllGoalsForCurrentUser();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> getGoalById(@PathVariable Long id) {
        GoalResponse response = savingsGoalService.getGoalById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody GoalUpdateRequest request) {
        GoalResponse response = savingsGoalService.updateGoal(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteGoal(@PathVariable Long id) {
        savingsGoalService.deleteGoal(id);
        return ResponseEntity.ok(Map.of("message", "Goal deleted successfully"));
    }
}
