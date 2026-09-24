package com.finsec.accounting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.finsec.accounting.model.PaymentMethod;

import java.time.LocalDate;

public class TransactionRequest {

    // NEW: We will inject this from the HTTP Header in the Controller
    private String organizationId;

    private String amount;
    private String monthYear;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;

    private String categoryName;
    private String description;
    private String recordedBy;

    private PaymentMethod paymentMethod;
    private String bankDetails;

    public TransactionRequest() {}

    // NEW Getters and Setters
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getBankDetails() { return bankDetails; }
    public void setBankDetails(String bankDetails) { this.bankDetails = bankDetails; }
    public String getMonthYear() { return monthYear; }
    public void setMonthYear(String monthYear) { this.monthYear = monthYear; }
}