package com.example.personalfinancemanager.dto.auth;

public class AuthResponseMessage {

    private String message;

    public AuthResponseMessage() {
    }

    public AuthResponseMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
