package com.example.personalfinancemanager.service;

import com.example.personalfinancemanager.dto.category.CategoryListResponse;
import com.example.personalfinancemanager.dto.category.CategoryRequest;
import com.example.personalfinancemanager.dto.category.CategoryResponse;
import com.example.personalfinancemanager.entity.Category;
import com.example.personalfinancemanager.entity.User;
import com.example.personalfinancemanager.exception.ConflictException;
import com.example.personalfinancemanager.exception.ForbiddenException;
import com.example.personalfinancemanager.exception.ResourceNotFoundException;
import com.example.personalfinancemanager.exception.ValidationException;
import com.example.personalfinancemanager.repository.CategoryRepository;
import com.example.personalfinancemanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public CategoryService(CategoryRepository categoryRepository,
                           TransactionRepository transactionRepository,
                           UserService userService) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public CategoryListResponse getCategoriesForCurrentUser() {
        User user = userService.getCurrentAuthenticatedUser();
        List<Category> categories = categoryRepository.findAllVisibleForUser(user);

        List<CategoryResponse> responses = categories.stream()
                .map(c -> new CategoryResponse(c.getName(), c.getType(), c.isCustom()))
                .collect(Collectors.toList());

        return new CategoryListResponse(responses);
    }

    @Transactional
    public CategoryResponse createCustomCategory(CategoryRequest request) {
        User user = userService.getCurrentAuthenticatedUser();

        // Check if category name already exists as global default
        if (categoryRepository.existsByNameIgnoreCaseAndUserIsNull(request.getName())) {
            throw new ConflictException("Category with name '" + request.getName() + "' already exists as a default category");
        }

        // Check if user already has a custom category with this name
        if (categoryRepository.existsByNameIgnoreCaseAndUser(request.getName(), user)) {
            throw new ConflictException("Category with name '" + request.getName() + "' already exists for this user");
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setType(request.getType());
        category.setCustom(true);
        category.setUser(user);

        Category saved = categoryRepository.save(category);
        return new CategoryResponse(saved.getName(), saved.getType(), saved.isCustom());
    }

    @Transactional
    public void deleteCustomCategory(String name) {
        User user = userService.getCurrentAuthenticatedUser();

        // Check if it's a default category first
        if (categoryRepository.existsByNameIgnoreCaseAndUserIsNull(name)) {
            throw new ValidationException("Default categories cannot be deleted: " + name);
        }

        // Check if user has this custom category
        Optional<Category> customCategoryOpt = categoryRepository.findByNameIgnoreCaseAndUser(name, user);
        if (customCategoryOpt.isEmpty()) {
            // Check if it exists for another user
            List<Category> otherUserCategories = categoryRepository.findAll().stream()
                    .filter(c -> c.getName().equalsIgnoreCase(name) && c.getUser() != null && !c.getUser().equals(user))
                    .collect(Collectors.toList());
            if (!otherUserCategories.isEmpty()) {
                throw new ForbiddenException("Cannot delete another user's custom category: " + name);
            }
            throw new ResourceNotFoundException("Category not found: " + name);
        }

        Category category = customCategoryOpt.get();

        // Check if referenced by any transaction
        if (transactionRepository.existsByCategory(category)) {
            throw new ValidationException("Category '" + name + "' is currently referenced by transactions and cannot be deleted");
        }

        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public Category getCategoryByNameForUser(String name, User user) {
        return categoryRepository.findByNameAndUserOrGlobal(name, user)
                .or(() -> categoryRepository.findByNameIgnoreCaseAndUserOrGlobal(name, user))
                .orElseThrow(() -> new ValidationException("Category not found or not accessible: " + name));
    }
}
