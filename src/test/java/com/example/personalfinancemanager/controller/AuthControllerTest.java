package com.example.personalfinancemanager.controller;

import com.example.personalfinancemanager.dto.auth.LoginRequest;
import com.example.personalfinancemanager.dto.auth.RegisterRequest;
import com.example.personalfinancemanager.dto.auth.RegisterResponse;
import com.example.personalfinancemanager.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    void register_Success_Returns201Created() throws Exception {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123", "John Doe", "+1234567890");
        RegisterResponse response = new RegisterResponse("User registered successfully", 1L);

        when(userService.registerUser(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    void register_InvalidEmail_Returns400BadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest("invalid-email", "password123", "John Doe", "+1234567890");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_Success_Returns200OK() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        UsernamePasswordAuthenticationToken authResult = new UsernamePasswordAuthenticationToken("user@example.com", "password123");

        when(authenticationManager.authenticate(any())).thenReturn(authResult);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void login_BadCredentials_Returns401Unauthorized() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "wrongpassword");

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
