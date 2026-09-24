package com.finsec.accounting.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;

    // NEW: Ties the transaction to a specific organization.
    // The @Indexed annotation ensures fast lookups when fetching an organization's data!
    @Indexed
    private String organizationId;

    private String type; // "INCOME" or "EXPENSE"
    private BigDecimal amount;

    @Indexed
    private LocalDate transactionDate;

    private String categoryId;
    private String categoryName;
    private String description;
    private String recordedBy; // "Mobile User", "Desktop Admin"
    private String monthYear;

    private PaymentMethod paymentMethod; // CASH or BANK
    private String bankDetails; // Optional (e.g., "Fidelity Bank PLC")

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Transaction() {}

    // UPDATED Constructor to include organizationId
    public Transaction(String organizationId, String type, BigDecimal amount, LocalDate transactionDate,
                       String categoryId, String categoryName, String description,
                       String recordedBy, PaymentMethod paymentMethod,
                       String bankDetails, String monthYear) {
        this.organizationId = organizationId;
        this.type = type;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.description = description;
        this.recordedBy = recordedBy;
        this.paymentMethod = paymentMethod;
        this.bankDetails = bankDetails;
        this.monthYear = monthYear;
    }

    // NEW Getters and Setters for OrganizationId
    public String getOrganizationId() {
        return organizationId;
    }
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getMonthYear() { return monthYear; }
    public void setMonthYear(String monthYear) { this.monthYear = monthYear; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getBankDetails() { return bankDetails; }
    public void setBankDetails(String bankDetails) { this.bankDetails = bankDetails; }

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