package com.example.personalfinancemanager.service;

import com.example.personalfinancemanager.dto.auth.RegisterRequest;
import com.example.personalfinancemanager.dto.auth.RegisterResponse;
import com.example.personalfinancemanager.entity.User;
import com.example.personalfinancemanager.exception.ConflictException;
import com.example.personalfinancemanager.exception.UnauthorizedException;
import com.example.personalfinancemanager.repository.UserRepository;
import com.example.personalfinancemanager.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username/email already registered: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());

        User savedUser = userRepository.save(user);

        return new RegisterResponse("User registered successfully", savedUser.getId());
    }

    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new UnauthorizedException("Authenticated user not found in database"));
        } else if (principal instanceof User user) {
            return user;
        }

        throw new UnauthorizedException("Invalid authentication principal");
    }
}
