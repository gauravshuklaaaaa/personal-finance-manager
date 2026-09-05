package com.example.personalfinancemanager.config;

import com.example.personalfinancemanager.entity.Category;
import com.example.personalfinancemanager.entity.TransactionType;
import com.example.personalfinancemanager.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public DataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        seedCategoryIfNotExists("Salary", TransactionType.INCOME);
        seedCategoryIfNotExists("Food", TransactionType.EXPENSE);
        seedCategoryIfNotExists("Rent", TransactionType.EXPENSE);
        seedCategoryIfNotExists("Transportation", TransactionType.EXPENSE);
        seedCategoryIfNotExists("Entertainment", TransactionType.EXPENSE);
        seedCategoryIfNotExists("Healthcare", TransactionType.EXPENSE);
        seedCategoryIfNotExists("Utilities", TransactionType.EXPENSE);
    }

    private void seedCategoryIfNotExists(String name, TransactionType type) {
        if (!categoryRepository.existsByNameIgnoreCaseAndUserIsNull(name)) {
            Category category = new Category(name, type, false, null);
            categoryRepository.save(category);
        }
    }
}
