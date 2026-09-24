package com.finsec.accounting.repository;

import com.finsec.accounting.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends MongoRepository<Category, String> {
    Optional<Category> findByOrganizationIdAndNameIgnoreCase(String organizationId, String name);
    List<Category> findByOrganizationIdAndType(String organizationId, String type);
    List<Category> findByOrganizationId(String organizationId);
    void deleteByOrganizationId(String organizationId);
}