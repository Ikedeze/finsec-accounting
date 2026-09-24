package com.finsec.accounting.repository;

import com.finsec.accounting.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findByOrganizationId(String organizationId);
    Optional<Transaction> findByOrganizationIdAndId(String organizationId, String id);
    List<Transaction> findByOrganizationIdAndType(String organizationId, String type);
    List<Transaction> findByOrganizationIdAndTransactionDateBetween(String organizationId, LocalDate startDate, LocalDate endDate);
    List<Transaction> findByOrganizationIdAndCategoryId(String organizationId, String categoryId);
    void deleteByOrganizationId(String organizationId);
}