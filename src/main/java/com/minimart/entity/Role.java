package com.minimart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.minimart.entity.converter.AppRoleConverter;

@Entity
@Table(name = "ROLE", schema = "dbo")
public class Role {

    @Id
    @Column(name = "role_id")
    private Long id;

    @Convert(converter = AppRoleConverter.class)
    @Column(name = "role_name", nullable = false, length = 30)
    private AppRole name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected Role() {
    }

    public Long getId() {
        return id;
    }

    public AppRole getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }
}
