package com.finsec.accounting.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;

@Document(collection = "categories")
public class Category {
    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    @Indexed
    private String organizationId;
    private String type; // "INCOME" or "EXPENSE"

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Category() {}

    public Category(String name, String type, String organizationId) {
        this.name = name;
        this.type = (type != null) ? type.toUpperCase() : null;
        this.organizationId = organizationId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) {
        this.type = (type != null) ? type.toUpperCase() : null; }

    public String getOrganizationId() {
        return organizationId;
    }
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

}
