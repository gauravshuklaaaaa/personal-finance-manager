package com.example.personalfinancemanager.dto.goal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import java.math.BigDecimal;
import java.time.LocalDate;

public class GoalUpdateRequest {

    private String goalName;

    @DecimalMin(value = "0.01", message = "Target amount must be positive")
    private BigDecimal targetAmount;

    @Future(message = "Target date must be in the future")
    private LocalDate targetDate;

    public GoalUpdateRequest() {
    }

    public GoalUpdateRequest(String goalName, BigDecimal targetAmount, LocalDate targetDate) {
        this.goalName = goalName;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
    }

    public String getGoalName() {
        return goalName;
    }

    public void setGoalName(String goalName) {
        this.goalName = goalName;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }
}
