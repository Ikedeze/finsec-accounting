package com.finsec.accounting.controller;

import com.finsec.accounting.dto.TransactionRequest;
import com.finsec.accounting.dto.TransactionSummaryDTO;
import com.finsec.accounting.model.Transaction;
import com.finsec.accounting.service.TransactionService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions(
            @RequestHeader("X-Organization-Id") String orgId) {
        return ResponseEntity.ok(transactionService.getAllTransactions(orgId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(
            @RequestHeader("X-Organization-Id") String orgId,
            @PathVariable String id) {
        return ResponseEntity.ok(transactionService.getTransactionById(orgId, id));
    }

    @GetMapping("/summary")
    public ResponseEntity<TransactionSummaryDTO> getSummary(
            @RequestHeader("X-Organization-Id") String orgId,
            @Parameter(description = "Start date for summary filtering", example = "YYYY-MM-DD")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for summary filtering", example = "YYYY-MM-DD")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        TransactionSummaryDTO summary = transactionService.getTransactionSummary(orgId, startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    // --- INCOME ENDPOINTS ---
    @PostMapping("/income")
    public ResponseEntity<Transaction> createIncome(
            @RequestHeader("X-Organization-Id") String orgId,
            @RequestBody TransactionRequest request) {
        request.setOrganizationId(orgId); // Inject header into DTO
        return new ResponseEntity<>(transactionService.createIncome(request), HttpStatus.CREATED);
    }

    @PostMapping("/income/bulk")
    public ResponseEntity<List<Transaction>> createBulkIncomes(
            @RequestHeader("X-Organization-Id") String orgId,
            @RequestBody List<TransactionRequest> requests) {
        requests.forEach(req -> req.setOrganizationId(orgId)); // Inject header into all DTOs
        return new ResponseEntity<>(transactionService.createBulkIncomes(requests), HttpStatus.CREATED);
    }

    // --- EXPENSE ENDPOINTS ---
    @PostMapping("/expense")
    public ResponseEntity<Transaction> createExpense(
            @RequestHeader("X-Organization-Id") String orgId,
            @RequestBody TransactionRequest request) {
        request.setOrganizationId(orgId);
        return new ResponseEntity<>(transactionService.createExpense(request), HttpStatus.CREATED);
    }

    @PostMapping("/expense/bulk")
    public ResponseEntity<List<Transaction>> createBulkExpenses(
            @RequestHeader("X-Organization-Id") String orgId,
            @RequestBody List<TransactionRequest> requests) {
        requests.forEach(req -> req.setOrganizationId(orgId));
        return new ResponseEntity<>(transactionService.createBulkExpenses(requests), HttpStatus.CREATED);
    }

    // --- PATCH & DELETE ENDPOINTS ---
    @PatchMapping("/{id}/date")
    public ResponseEntity<Transaction> updateTransactionDate(
            @RequestHeader("X-Organization-Id") String orgId,
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate) {
        return ResponseEntity.ok(transactionService.updateTransactionDate(orgId, id, newDate));
    }

    @PatchMapping("/{id}/month-year")
    public ResponseEntity<Transaction> updateTransactionMonthYear(
            @RequestHeader("X-Organization-Id") String orgId,
            @PathVariable String id,
            @RequestParam String newMonthYear) {
        return ResponseEntity.ok(transactionService.updateTransactionMonthYear(orgId, id, newMonthYear));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @RequestHeader("X-Organization-Id") String orgId,
            @PathVariable String id) {
        transactionService.deleteTransaction(orgId, id);
        return ResponseEntity.noContent().build();
    }
}