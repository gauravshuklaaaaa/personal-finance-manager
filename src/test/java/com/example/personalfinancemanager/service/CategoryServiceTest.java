package com.example.personalfinancemanager.service;

import com.example.personalfinancemanager.dto.category.CategoryListResponse;
import com.example.personalfinancemanager.dto.category.CategoryRequest;
import com.example.personalfinancemanager.dto.category.CategoryResponse;
import com.example.personalfinancemanager.entity.Category;
import com.example.personalfinancemanager.entity.TransactionType;
import com.example.personalfinancemanager.entity.User;
import com.example.personalfinancemanager.exception.ConflictException;
import com.example.personalfinancemanager.exception.ForbiddenException;
import com.example.personalfinancemanager.exception.ResourceNotFoundException;
import com.example.personalfinancemanager.exception.ValidationException;
import com.example.personalfinancemanager.repository.CategoryRepository;
import com.example.personalfinancemanager.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CategoryService categoryService;

    private User user1;
    private User user2;
    private Category defaultCategory;
    private Category customCategory;

    @BeforeEach
    void setUp() {
        user1 = new User(1L, "user1@example.com", "password", "User One", "+1234567890");
        user2 = new User(2L, "user2@example.com", "password", "User Two", "+0987654321");
        defaultCategory = new Category(10L, "Salary", TransactionType.INCOME, false, null);
        customCategory = new Category(11L, "Bonus", TransactionType.INCOME, true, user1);
    }

    @Test
    void getCategoriesForCurrentUser_Success() {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(categoryRepository.findAllVisibleForUser(user1)).thenReturn(List.of(defaultCategory, customCategory));

        CategoryListResponse response = categoryService.getCategoriesForCurrentUser();

        assertNotNull(response);
        assertEquals(2, response.getCategories().size());
        assertEquals("Salary", response.getCategories().get(0).getName());
        assertFalse(response.getCategories().get(0).getIsCustom());
        assertEquals("Bonus", response.getCategories().get(1).getName());
        assertTrue(response.getCategories().get(1).getIsCustom());
    }

    @Test
    void createCustomCategory_Success() {
        CategoryRequest request = new CategoryRequest("SideBusiness", TransactionType.INCOME);
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("SideBusiness")).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndUser("SideBusiness", user1)).thenReturn(false);

        Category savedCategory = new Category(12L, "SideBusiness", TransactionType.INCOME, true, user1);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryResponse response = categoryService.createCustomCategory(request);

        assertNotNull(response);
        assertEquals("SideBusiness", response.getName());
        assertEquals(TransactionType.INCOME, response.getType());
        assertTrue(response.getIsCustom());
    }

    @Test
    void createCustomCategory_ConflictWithDefaultCategory() {
        CategoryRequest request = new CategoryRequest("Salary", TransactionType.INCOME);
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Salary")).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.createCustomCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createCustomCategory_ConflictWithExistingUserCustomCategory() {
        CategoryRequest request = new CategoryRequest("Bonus", TransactionType.INCOME);
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Bonus")).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndUser("Bonus", user1)).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.createCustomCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCustomCategory_Success() {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Bonus")).thenReturn(false);
        when(categoryRepository.findByNameIgnoreCaseAndUser("Bonus", user1)).thenReturn(Optional.of(customCategory));
        when(transactionRepository.existsByCategory(customCategory)).thenReturn(false);

        assertDoesNotThrow(() -> categoryService.deleteCustomCategory("Bonus"));
        verify(categoryRepository, times(1)).delete(customCategory);
    }

    @Test
    void deleteCustomCategory_DefaultCategory_ThrowsValidationException() {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Salary")).thenReturn(true);

        assertThrows(ValidationException.class, () -> categoryService.deleteCustomCategory("Salary"));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCustomCategory_ReferencedByTransaction_ThrowsValidationException() {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Bonus")).thenReturn(false);
        when(categoryRepository.findByNameIgnoreCaseAndUser("Bonus", user1)).thenReturn(Optional.of(customCategory));
        when(transactionRepository.existsByCategory(customCategory)).thenReturn(true);

        assertThrows(ValidationException.class, () -> categoryService.deleteCustomCategory("Bonus"));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCustomCategory_BelongsToAnotherUser_ThrowsForbiddenException() {
        Category otherUserCategory = new Category(15L, "PrivateCategory", TransactionType.EXPENSE, true, user2);
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("PrivateCategory")).thenReturn(false);
        when(categoryRepository.findByNameIgnoreCaseAndUser("PrivateCategory", user1)).thenReturn(Optional.empty());
        when(categoryRepository.findAll()).thenReturn(List.of(otherUserCategory));

        assertThrows(ForbiddenException.class, () -> categoryService.deleteCustomCategory("PrivateCategory"));
    }

    @Test
    void deleteCustomCategory_NonExistent_ThrowsResourceNotFoundException() {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user1);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("UnknownCategory")).thenReturn(false);
        when(categoryRepository.findByNameIgnoreCaseAndUser("UnknownCategory", user1)).thenReturn(Optional.empty());
        when(categoryRepository.findAll()).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.deleteCustomCategory("UnknownCategory"));
    }

    @Test
    void getCategoryByNameForUser_Success() {
        when(categoryRepository.findByNameAndUserOrGlobal("Salary", user1)).thenReturn(Optional.of(defaultCategory));

        Category category = categoryService.getCategoryByNameForUser("Salary", user1);
        assertNotNull(category);
        assertEquals("Salary", category.getName());
    }

    @Test
    void getCategoryByNameForUser_NotFound_ThrowsValidationException() {
        when(categoryRepository.findByNameAndUserOrGlobal("NonExistent", user1)).thenReturn(Optional.empty());
        when(categoryRepository.findByNameIgnoreCaseAndUserOrGlobal("NonExistent", user1)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> categoryService.getCategoryByNameForUser("NonExistent", user1));
    }
}
