package com.example.personalfinancemanager.repository;

import com.example.personalfinancemanager.entity.SavingsGoal;
import com.example.personalfinancemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByUserOrderByIdDesc(User user);
    Optional<SavingsGoal> findByIdAndUser(Long id, User user);
}
