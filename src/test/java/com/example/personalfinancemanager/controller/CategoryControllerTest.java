package com.example.personalfinancemanager.controller;

import com.example.personalfinancemanager.dto.category.CategoryListResponse;
import com.example.personalfinancemanager.dto.category.CategoryRequest;
import com.example.personalfinancemanager.dto.category.CategoryResponse;
import com.example.personalfinancemanager.entity.TransactionType;
import com.example.personalfinancemanager.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    @WithMockUser
    void getCategories_Success_Returns200OK() throws Exception {
        CategoryResponse c1 = new CategoryResponse("Salary", TransactionType.INCOME, false);
        CategoryResponse c2 = new CategoryResponse("SideBusiness", TransactionType.INCOME, true);

        when(categoryService.getCategoriesForCurrentUser()).thenReturn(new CategoryListResponse(List.of(c1, c2)));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories.length()").value(2))
                .andExpect(jsonPath("$.categories[0].name").value("Salary"));
    }

    @Test
    @WithMockUser
    void createCategory_Success_Returns201Created() throws Exception {
        CategoryRequest request = new CategoryRequest("Freelance", TransactionType.INCOME);
        CategoryResponse response = new CategoryResponse("Freelance", TransactionType.INCOME, true);

        when(categoryService.createCustomCategory(any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Freelance"))
                .andExpect(jsonPath("$.isCustom").value(true));
    }

    @Test
    @WithMockUser
    void deleteCategory_Success_Returns200OK() throws Exception {
        doNothing().when(categoryService).deleteCustomCategory("Freelance");

        mockMvc.perform(delete("/api/categories/Freelance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted successfully"));
    }

    @Test
    void getCategories_Unauthenticated_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }
}
