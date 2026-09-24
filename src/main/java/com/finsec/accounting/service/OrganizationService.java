package com.finsec.accounting.service;

import com.finsec.accounting.exception.InvalidTransactionException;
import com.finsec.accounting.exception.ResourceNotFoundException;
import com.finsec.accounting.model.Organization;
import com.finsec.accounting.model.User;
import com.finsec.accounting.repository.CategoryRepository;
import com.finsec.accounting.repository.OrganizationRepository;
import com.finsec.accounting.repository.TransactionRepository;
import com.finsec.accounting.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public OrganizationService(OrganizationRepository organizationRepository,
                               UserRepository userRepository,
                               CategoryRepository categoryRepository,
                               TransactionRepository transactionRepository) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    // Create Organization and attach the creator as User #1
    public Organization createOrganization(String name, String creatorUserId) {
        if (name == null || name.isBlank()) {
            throw new InvalidTransactionException("Organization name cannot be empty.");
        }

        String trimmedName = name.trim();

        // Check for unique organization name
        if (organizationRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new InvalidTransactionException("An organization with the name '" + trimmedName + "' already exists.");
        }

        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + creatorUserId));

        Organization org = new Organization(trimmedName);
        org.setCreatedAt(Instant.now()); // <--- ADD THIS
        org.setUpdatedAt(Instant.now());
        org.getUserIds().add(creator.getId());
        Organization savedOrg = organizationRepository.save(org);

        // Link org back to user
        creator.getOrganizationIds().add(savedOrg.getId());
        creator.setUpdatedAt(Instant.now());
        userRepository.save(creator);

        return savedOrg;
    }

    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // 1. Remove user reference from all organizations they belong to
        if (user.getOrganizationIds() != null && !user.getOrganizationIds().isEmpty()) {
            List<Organization> userOrgs = organizationRepository.findAllById(user.getOrganizationIds());
            for (Organization org : userOrgs) {
                org.getUserIds().remove(userId);
                org.setUpdatedAt(java.time.Instant.now());
                organizationRepository.save(org);
            }
        }

        // 2. Permanently remove the user document
        userRepository.deleteById(userId);
    }

    // 2. Add User to Org (Enforces the MAXIMUM 3 USERS rule!)
    public Organization addUserToOrganization(String orgId, String userIdentifier) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + orgId));

        // Flexible lookup: supports ID, Username, or Email
        User user = userRepository.findById(userIdentifier)
                .orElseGet(() -> userRepository.findByUsernameIgnoreCase(userIdentifier)
                        .orElseGet(() -> userRepository.findByEmailIgnoreCase(userIdentifier)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userIdentifier))));

        String userId = user.getId();

        if (org.getUserIds().size() >= 3) {
            throw new InvalidTransactionException("Organization has reached its limit of 3 users.");
        }

        if (org.getUserIds().contains(userId)) {
            throw new InvalidTransactionException("User is already a member of this organization.");
        }

        org.getUserIds().add(userId);
        organizationRepository.save(org);

        if (!user.getOrganizationIds().contains(orgId)) {
            user.getOrganizationIds().add(orgId);
            userRepository.save(user);
        }

        return org;
    }

    // 3. Fetch all Organizations belonging to a specific user (For JavaFX Dropdown)
    public List<Organization> getUserOrganizations(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return organizationRepository.findAllById(user.getOrganizationIds());
    }

    public Organization getOrganizationById(String id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + id));
    }

    public void deleteOrganization(String orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + orgId));

        // 1. Unlink organization ID from all member users
        if (org.getUserIds() != null && !org.getUserIds().isEmpty()) {
            List<User> members = userRepository.findAllById(org.getUserIds());
            for (User user : members) {
                user.getOrganizationIds().remove(orgId);
                user.setUpdatedAt(java.time.Instant.now());
                userRepository.save(user);
            }
        }

        // 2. Cascade delete linked categories and transactions
        categoryRepository.deleteByOrganizationId(orgId);
        transactionRepository.deleteByOrganizationId(orgId);

        // 3. Delete the organization record
        organizationRepository.deleteById(orgId);
    }
}