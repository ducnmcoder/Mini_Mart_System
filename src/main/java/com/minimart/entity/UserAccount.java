package com.minimart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "USER_ACCOUNT", schema = "dbo")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected UserAccount() {
    }

    public void updateProfile(String fullName, String phone) {
        this.fullName = fullName;
        this.phone = phone;
        markUpdated();
    }

    public void changePassword(String encodedPassword) {
        this.passwordHash = encodedPassword;
        markUpdated();
    }

    public void recordSuccessfulLogin() {
        this.lastLoginAt = LocalDateTime.now();
        markUpdated();
    }

    public void activateDemoAccount(String encodedPassword) {
        this.passwordHash = encodedPassword;
        if (status == AccountStatus.PENDING_PASSWORD) {
            this.status = AccountStatus.ACTIVE;
        }
        markUpdated();
    }

    public void deactivate() {
        this.status = AccountStatus.INACTIVE;
        markUpdated();
    }

    public boolean isLoginAllowed() {
        return status == AccountStatus.ACTIVE && role.isActive();
    }

    private void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
