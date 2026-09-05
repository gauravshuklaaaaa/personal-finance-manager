package com.example.personalfinancemanager.config;

import com.example.personalfinancemanager.security.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    public SecurityConfig(CustomUserDetailsService userDetailsService, ObjectMapper objectMapper) {
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/health", "/h2-console/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation().changeSessionId()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    Map<String, Object> errorDetails = new LinkedHashMap<>();
                    errorDetails.put("timestamp", LocalDateTime.now().toString());
                    errorDetails.put("status", HttpStatus.UNAUTHORIZED.value());
                    errorDetails.put("error", "Unauthorized");
                    errorDetails.put("message", "Full authentication is required to access this resource");
                    errorDetails.put("path", request.getRequestURI());
                    objectMapper.writeValue(response.getOutputStream(), errorDetails);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    Map<String, Object> errorDetails = new LinkedHashMap<>();
                    errorDetails.put("timestamp", LocalDateTime.now().toString());
                    errorDetails.put("status", HttpStatus.FORBIDDEN.value());
                    errorDetails.put("error", "Forbidden");
                    errorDetails.put("message", "Access denied for the requested resource");
                    errorDetails.put("path", request.getRequestURI());
                    objectMapper.writeValue(response.getOutputStream(), errorDetails);
                })
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    Map<String, String> body = Map.of("message", "Logout successful");
                    objectMapper.writeValue(response.getOutputStream(), body);
                })
            );

        http.authenticationProvider(authenticationProvider());

        return http.build();
    }
}
