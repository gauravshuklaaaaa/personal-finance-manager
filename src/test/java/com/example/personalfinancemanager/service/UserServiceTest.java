package com.example.personalfinancemanager.service;

import com.example.personalfinancemanager.dto.auth.RegisterRequest;
import com.example.personalfinancemanager.dto.auth.RegisterResponse;
import com.example.personalfinancemanager.entity.User;
import com.example.personalfinancemanager.exception.ConflictException;
import com.example.personalfinancemanager.exception.UnauthorizedException;
import com.example.personalfinancemanager.repository.UserRepository;
import com.example.personalfinancemanager.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1L, "user@example.com", "encodedPassword", "John Doe", "+1234567890");
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerUser_Success() {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123", "John Doe", "+1234567890");

        when(userRepository.existsByUsername("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        RegisterResponse response = userService.registerUser(request);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals("User registered successfully", response.getMessage());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_DuplicateUsername_ThrowsConflictException() {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123", "John Doe", "+1234567890");

        when(userRepository.existsByUsername("user@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.registerUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getCurrentAuthenticatedUser_Success_WithUserPrincipal() {
        UserPrincipal userPrincipal = new UserPrincipal(sampleUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        User current = userService.getCurrentAuthenticatedUser();

        assertNotNull(current);
        assertEquals("user@example.com", current.getUsername());
    }

    @Test
    void getCurrentAuthenticatedUser_Success_WithUserEntity() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(sampleUser, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        User current = userService.getCurrentAuthenticatedUser();

        assertNotNull(current);
        assertEquals("user@example.com", current.getUsername());
    }

    @Test
    void getCurrentAuthenticatedUser_Unauthenticated_ThrowsUnauthorizedException() {
        SecurityContextHolder.clearContext();
        assertThrows(UnauthorizedException.class, () -> userService.getCurrentAuthenticatedUser());
    }

    @Test
    void getCurrentAuthenticatedUser_Anonymous_ThrowsUnauthorizedException() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("anonymousUser", null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(UnauthorizedException.class, () -> userService.getCurrentAuthenticatedUser());
    }

    @Test
    void getCurrentAuthenticatedUser_UserNotFoundInDb_ThrowsUnauthorizedException() {
        UserPrincipal userPrincipal = new UserPrincipal(sampleUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> userService.getCurrentAuthenticatedUser());
    }
}
