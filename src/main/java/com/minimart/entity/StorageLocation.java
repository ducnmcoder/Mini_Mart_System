package com.minimart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "STORAGE_LOCATION", schema = "dbo")
public class StorageLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Long id;

    @Column(name = "location_code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "location_name", nullable = false, length = 120)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected StorageLocation() {
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
}