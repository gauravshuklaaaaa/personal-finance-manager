package com.example.personalfinancemanager.repository;

import com.example.personalfinancemanager.entity.Category;
import com.example.personalfinancemanager.entity.Transaction;
import com.example.personalfinancemanager.entity.TransactionType;
import com.example.personalfinancemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndUser(Long id, User user);

    boolean existsByCategory(Category category);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user " +
           "AND (:startDate IS NULL OR t.date >= :startDate) " +
           "AND (:endDate IS NULL OR t.date <= :endDate) " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
           "AND (:type IS NULL OR t.type = :type) " +
           "ORDER BY t.date DESC, t.id DESC")
    List<Transaction> findFilteredTransactions(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryId") Long categoryId,
            @Param("type") TransactionType type
    );

    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND t.date >= :startDate ORDER BY t.date ASC")
    List<Transaction> findByUserAndDateGreaterThanEqual(@Param("user") User user, @Param("startDate") LocalDate startDate);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND t.date >= :startDate AND t.date <= :endDate")
    List<Transaction> findByUserAndDateBetween(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
