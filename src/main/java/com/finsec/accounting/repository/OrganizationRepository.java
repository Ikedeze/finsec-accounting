package com.finsec.accounting.repository;

import com.finsec.accounting.model.Organization;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface OrganizationRepository extends MongoRepository<Organization, String> {
    Optional<Organization> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name); // NEW: Uniqueness check
}