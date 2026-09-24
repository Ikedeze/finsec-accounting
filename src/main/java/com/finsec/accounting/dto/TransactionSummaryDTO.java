package com.finsec.accounting.dto;

import java.math.BigDecimal;
import java.util.Map;

public class TransactionSummaryDTO {

    private BigDecimal openingBalance;   // Balance Brought Forward (BBF prior to startDate)
    private BigDecimal totalIncome;      // Total income WITHIN selected date range
    private BigDecimal totalExpenses;    // Total expenses WITHIN selected date range
    private BigDecimal closingBalance;   // openingBalance + totalIncome - totalExpenses

    // Separated breakdowns for side-by-side reports
    private Map<String, BigDecimal> incomeCategoryBreakdown;
    private Map<String, BigDecimal> expenseCategoryBreakdown;

    // Asset Breakdown (Cash vs. Bank)
    private BigDecimal cashAtHand;
    private BigDecimal cashAtBank;

    public TransactionSummaryDTO() {}

    public TransactionSummaryDTO(BigDecimal openingBalance, BigDecimal totalIncome,
                                 BigDecimal totalExpenses, BigDecimal closingBalance,
                                 Map<String, BigDecimal> incomeCategoryBreakdown,
                                 Map<String, BigDecimal> expenseCategoryBreakdown,
                                 BigDecimal cashAtHand, BigDecimal cashAtBank) {
        this.openingBalance = openingBalance;
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
        this.closingBalance = closingBalance;
        this.incomeCategoryBreakdown = incomeCategoryBreakdown;
        this.expenseCategoryBreakdown = expenseCategoryBreakdown;
        this.cashAtHand = cashAtHand;
        this.cashAtBank = cashAtBank;
    }

    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }

    public BigDecimal getTotalIncome() { return totalIncome; }
    public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }

    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(BigDecimal totalExpenses) { this.totalExpenses = totalExpenses; }

    public BigDecimal getClosingBalance() { return closingBalance; }
    public void setClosingBalance(BigDecimal closingBalance) { this.closingBalance = closingBalance; }

    public Map<String, BigDecimal> getIncomeCategoryBreakdown() { return incomeCategoryBreakdown; }
    public void setIncomeCategoryBreakdown(Map<String, BigDecimal> incomeCategoryBreakdown) { this.incomeCategoryBreakdown = incomeCategoryBreakdown; }

    public Map<String, BigDecimal> getExpenseCategoryBreakdown() { return expenseCategoryBreakdown; }
    public void setExpenseCategoryBreakdown(Map<String, BigDecimal> expenseCategoryBreakdown) { this.expenseCategoryBreakdown = expenseCategoryBreakdown; }

    public BigDecimal getCashAtHand() { return cashAtHand; }
    public void setCashAtHand(BigDecimal cashAtHand) { this.cashAtHand = cashAtHand; }

    public BigDecimal getCashAtBank() { return cashAtBank; }
    public void setCashAtBank(BigDecimal cashAtBank) { this.cashAtBank = cashAtBank; }
}