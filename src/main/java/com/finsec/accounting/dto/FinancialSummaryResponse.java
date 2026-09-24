package com.finsec.accounting.dto;

import java.math.BigDecimal;
import java.util.Map;

public class FinancialSummaryResponse {

    private BigDecimal totalIncome,
    totalExpenses,
    netBalance;
    private Map<String,BigDecimal> categoryBreakdown;

    public FinancialSummaryResponse() {
    }

    public FinancialSummaryResponse(BigDecimal totalIncome,
                                    BigDecimal totalExpenses,
                                    BigDecimal netBalance,
                                    Map<String, BigDecimal> categoryBreakdown) {
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
        this.netBalance = netBalance;
        this.categoryBreakdown = categoryBreakdown;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;}
    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }
    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getNetBalance() {
        return netBalance;
    }
    public void setNetBalance(BigDecimal netBalance) {
        this.netBalance = netBalance;
    }

    public Map<String, BigDecimal> getCategoryBreakdown() {
        return categoryBreakdown;
    }
    public void setCategoryBreakdown(Map<String, BigDecimal> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }
}
