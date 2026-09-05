package com.example.personalfinancemanager.dto.category;

import com.example.personalfinancemanager.entity.TransactionType;

public class CategoryResponse {

    private String name;
    private TransactionType type;
    private boolean isCustom;

    public CategoryResponse() {
    }

    public CategoryResponse(String name, TransactionType type, boolean isCustom) {
        this.name = name;
        this.type = type;
        this.isCustom = isCustom;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public boolean getIsCustom() {
        return isCustom;
    }

    public void setIsCustom(boolean custom) {
        isCustom = custom;
    }
}
