package com.example.personalfinancemanager.config;

import com.example.personalfinancemanager.entity.Category;
import com.example.personalfinancemanager.entity.TransactionType;
import com.example.personalfinancemanager.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void run_SeedsDefaultCategoriesWhenMissing() {
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull(anyString())).thenReturn(false);

        dataInitializer.run();

        verify(categoryRepository, times(7)).save(any(Category.class));
    }

    @Test
    void run_SkipsSeedingWhenCategoriesExist() {
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull(anyString())).thenReturn(true);

        dataInitializer.run();

        verify(categoryRepository, never()).save(any(Category.class));
    }
}
