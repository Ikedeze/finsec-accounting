package com.finsec.accounting.service;

import com.finsec.accounting.dto.TransactionRequest;
import com.finsec.accounting.dto.TransactionSummaryDTO;
import com.finsec.accounting.exception.InvalidTransactionException;
import com.finsec.accounting.exception.ResourceNotFoundException;
import com.finsec.accounting.model.Category;
import com.finsec.accounting.model.PaymentMethod;
import com.finsec.accounting.model.Transaction;
import com.finsec.accounting.repository.CategoryRepository;
import com.finsec.accounting.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    public Transaction createTransaction(Transaction transaction) {
        if (transaction.getOrganizationId() == null || transaction.getOrganizationId().isBlank()) {
            throw new InvalidTransactionException("Organization ID is required.");
        }

        if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transaction amount must be greater than zero.");
        }

        if (transaction.getType() != null) {
            transaction.setType(transaction.getType().toUpperCase());
        }

        if (!"INCOME".equals(transaction.getType()) && !"EXPENSE".equals(transaction.getType())) {
            throw new InvalidTransactionException("Transaction type must be either 'INCOME' or 'EXPENSE'.");
        }

        if (transaction.getPaymentMethod() == null) {
            transaction.setPaymentMethod(PaymentMethod.CASH);
        }

        // Category Logic now includes OrganizationId isolation
        //  Category Logic (Using explicit setters to guarantee compilation)
        if (transaction.getCategoryName() != null && !transaction.getCategoryName().isBlank()) {
            String categoryName = transaction.getCategoryName().trim();
            String categoryType = transaction.getType();
            String orgId = transaction.getOrganizationId();

            Category category = categoryRepository.findByOrganizationIdAndNameIgnoreCase(orgId, categoryName)
                    .orElseGet(() -> {
                        Category newCat = new Category();
                        newCat.setName(categoryName);
                        newCat.setType(categoryType);
                        newCat.setOrganizationId(orgId);
                        newCat.setCreatedAt(java.time.Instant.now()); // <--- ADD THIS
                        newCat.setUpdatedAt(java.time.Instant.now());
                        return categoryRepository.save(newCat);
                    });

            transaction.setCategoryId(category.getId());
            transaction.setCategoryName(category.getName());
        }
        else {
            throw new InvalidTransactionException("Category name is required.");
        }

        if (transaction.getTransactionDate() == null) {
            transaction.setTransactionDate(LocalDate.now());
        }

        if (transaction.getMonthYear() != null && !transaction.getMonthYear().isBlank()) {
            transaction.setMonthYear(transaction.getMonthYear().trim());
        }
        else if (transaction.getTransactionDate() != null) {
            String derivedMonthYear = transaction.getTransactionDate()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH));
            transaction.setMonthYear(derivedMonthYear);
        }

        if (transaction.getCreatedAt() == null) {
            transaction.setCreatedAt(java.time.Instant.now());
        }
        transaction.setUpdatedAt(java.time.Instant.now()); // <--- ADD THIS

        return transactionRepository.save(transaction);
    }

    public Transaction processTransaction(TransactionRequest request, String type) {
        Transaction transaction = new Transaction();
        transaction.setOrganizationId(request.getOrganizationId()); // Set the Org ID
        transaction.setType(type.toUpperCase());

        if (request.getAmount() != null) {
            String cleanAmount = request.getAmount().replaceAll("[,_]", "").trim();
            transaction.setAmount(new BigDecimal(cleanAmount));
        }

        transaction.setTransactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDate.now());
        transaction.setCategoryName(request.getCategoryName());
        transaction.setDescription(request.getDescription());
        transaction.setRecordedBy(request.getRecordedBy() != null ? request.getRecordedBy() : "System");
        transaction.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.CASH);
        transaction.setBankDetails(request.getBankDetails());

        return createTransaction(transaction);
    }

    public Transaction createIncome(TransactionRequest request) { return processTransaction(request, "INCOME"); }
    public Transaction createExpense(TransactionRequest request) { return processTransaction(request, "EXPENSE"); }

    public List<Transaction> createBulkIncomes(List<TransactionRequest> requests) {
        return requests.stream().map(req -> processTransaction(req, "INCOME")).collect(Collectors.toList());
    }
    public List<Transaction> createBulkExpenses(List<TransactionRequest> requests) {
        return requests.stream().map(req -> processTransaction(req, "EXPENSE")).collect(Collectors.toList());
    }

    // Now fetches ONLY transactions for the specific organization
    public List<Transaction> getAllTransactions(String orgId) {
        return transactionRepository.findByOrganizationId(orgId);
    }

    // Ensures the transaction being fetched belongs to the organization
    public Transaction getTransactionById(String orgId, String id) {
        return transactionRepository.findByOrganizationIdAndId(orgId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found or access denied."));
    }

    // Summary Engine now isolates by organization!
    public TransactionSummaryDTO getTransactionSummary(String orgId, LocalDate startDate, LocalDate endDate) {
        List<Transaction> allTransactions = transactionRepository.findByOrganizationId(orgId);

        // Convert filter boundaries to YearMonth periods
        YearMonth startMonth = (startDate != null) ? YearMonth.from(startDate) : null;
        YearMonth endMonth = (endDate != null) ? YearMonth.from(endDate) : null;

        DateTimeFormatter monthYearFormatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("MMMM yyyy")
                .toFormatter(Locale.ENGLISH);

        // 1. Calculate Opening Balance (All periods prior to startMonth)
        BigDecimal openingBalance = BigDecimal.ZERO;
        if (startMonth != null) {
            openingBalance = allTransactions.stream()
                    .filter(t -> {
                        YearMonth ym = extractYearMonth(t, monthYearFormatter);
                        return ym != null && ym.isBefore(startMonth);
                    })
                    .map(t -> "INCOME".equalsIgnoreCase(t.getType()) ? t.getAmount() : t.getAmount().negate())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // 2. Filter Transactions within the requested month/year range
        List<Transaction> periodTransactions = allTransactions.stream()
                .filter(t -> {
                    YearMonth ym = extractYearMonth(t, monthYearFormatter);
                    if (ym == null) return false;

                    boolean isAfterOrEqualStart = (startMonth == null) || !ym.isBefore(startMonth);
                    boolean isBeforeOrEqualEnd = (endMonth == null) || !ym.isAfter(endMonth);
                    return isAfterOrEqualStart && isBeforeOrEqualEnd;
                })
                .collect(Collectors.toList());

        // 3. Totals for Period
        BigDecimal totalIncome = periodTransactions.stream()
                .filter(t -> "INCOME".equalsIgnoreCase(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = periodTransactions.stream()
                .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal closingBalance = openingBalance.add(totalIncome).subtract(totalExpenses);

        // 4. Category Breakdowns
        Map<String, BigDecimal> incomeCategoryBreakdown = periodTransactions.stream()
                .filter(t -> "INCOME".equalsIgnoreCase(t.getType()) && t.getCategoryName() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getCategoryName,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        Map<String, BigDecimal> expenseCategoryBreakdown = periodTransactions.stream()
                .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()) && t.getCategoryName() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getCategoryName,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        // 5. Up-to-date Transactions (Everything up to endMonth)
        List<Transaction> upToDateTransactions = allTransactions.stream()
                .filter(t -> {
                    YearMonth ym = extractYearMonth(t, monthYearFormatter);
                    return ym != null && (endMonth == null || !ym.isAfter(endMonth));
                })
                .collect(Collectors.toList());

        BigDecimal cashAtHand = upToDateTransactions.stream()
                .filter(t -> PaymentMethod.CASH.equals(t.getPaymentMethod()))
                .map(t -> "INCOME".equalsIgnoreCase(t.getType()) ? t.getAmount() : t.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cashAtBank = upToDateTransactions.stream()
                .filter(t -> PaymentMethod.BANK.equals(t.getPaymentMethod()))
                .map(t -> "INCOME".equalsIgnoreCase(t.getType()) ? t.getAmount() : t.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TransactionSummaryDTO(
                openingBalance, totalIncome, totalExpenses, closingBalance,
                incomeCategoryBreakdown, expenseCategoryBreakdown, cashAtHand, cashAtBank
        );
    }

    // Helper method to resolve YearMonth from monthYear string or transactionDate fallback
    private YearMonth extractYearMonth(Transaction t, DateTimeFormatter formatter) {
        String my = t.getMonthYear();
        if (my != null && !my.trim().isEmpty()) {
            try {
                return YearMonth.parse(my.trim().replaceAll("\\s+", " "), formatter);
            } catch (Exception ignored) {
                // Fall through to transactionDate if format parsing fails
            }
        }
        if (t.getTransactionDate() != null) {
            return YearMonth.from(t.getTransactionDate());
        }
        return null;
    }

    public Transaction updateTransactionDate(String orgId, String id, LocalDate newDate) {
        Transaction transaction = getTransactionById(orgId, id); // Uses our secured method
        if (newDate != null) {
            transaction.setTransactionDate(newDate);
        }
        transaction.setUpdatedAt(java.time.Instant.now());
        return transactionRepository.save(transaction);
    }

    public Transaction updateTransactionMonthYear(String orgId, String id, String newMonthYear) {
        Transaction transaction = getTransactionById(orgId, id);
        if (newMonthYear != null && !newMonthYear.isBlank()) {
            transaction.setMonthYear(newMonthYear.trim());
        }
        transaction.setUpdatedAt(java.time.Instant.now());
        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(String orgId, String id) {
        Transaction transaction = getTransactionById(orgId, id); // Verify it exists and belongs to Org
        transactionRepository.delete(transaction);
    }
}