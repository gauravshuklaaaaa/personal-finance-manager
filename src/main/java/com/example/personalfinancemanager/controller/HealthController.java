package com.example.personalfinancemanager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> rootEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "Welcome to Personal Finance Manager REST API",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString(),
                "healthCheck", "/api/health"
        ));
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString(),
                "service", "Personal Finance Manager API"
        ));
    }
}
