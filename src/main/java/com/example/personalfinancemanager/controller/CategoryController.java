package com.example.personalfinancemanager.controller;

import com.example.personalfinancemanager.dto.category.CategoryListResponse;
import com.example.personalfinancemanager.dto.category.CategoryRequest;
import com.example.personalfinancemanager.dto.category.CategoryResponse;
import com.example.personalfinancemanager.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<CategoryListResponse> getCategories() {
        CategoryListResponse response = categoryService.getCategoriesForCurrentUser();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCustomCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Map<String, String>> deleteCategory(@PathVariable String name) {
        categoryService.deleteCustomCategory(name);
        return ResponseEntity.ok(Map.of("message", "Category deleted successfully"));
    }
}
