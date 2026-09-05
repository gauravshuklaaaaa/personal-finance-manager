package com.example.personalfinancemanager.repository;

import com.example.personalfinancemanager.entity.Category;
import com.example.personalfinancemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.user IS NULL OR c.user = :user")
    List<Category> findAllVisibleForUser(@Param("user") User user);

    @Query("SELECT c FROM Category c WHERE c.name = :name AND (c.user IS NULL OR c.user = :user)")
    Optional<Category> findByNameAndUserOrGlobal(@Param("name") String name, @Param("user") User user);

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND (c.user IS NULL OR c.user = :user)")
    Optional<Category> findByNameIgnoreCaseAndUserOrGlobal(@Param("name") String name, @Param("user") User user);

    boolean existsByNameIgnoreCaseAndUser(String name, User user);

    boolean existsByNameIgnoreCaseAndUserIsNull(String name);

    Optional<Category> findByNameIgnoreCaseAndUser(String name, User user);

    Optional<Category> findByNameIgnoreCaseAndUserIsNull(String name);
}
