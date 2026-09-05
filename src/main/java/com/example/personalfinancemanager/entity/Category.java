package com.example.personalfinancemanager.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "categories", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "user_id"})
})
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(name = "is_custom", nullable = false)
    private boolean isCustom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Category() {
    }

    public Category(String name, TransactionType type, boolean isCustom, User user) {
        this.name = name;
        this.type = type;
        this.isCustom = isCustom;
        this.user = user;
    }

    public Category(Long id, String name, TransactionType type, boolean isCustom, User user) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.isCustom = isCustom;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isCustom() {
        return isCustom;
    }

    public void setCustom(boolean custom) {
        isCustom = custom;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
