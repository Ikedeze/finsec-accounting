package com.finsec.accounting.controller;

import com.finsec.accounting.model.Organization;
import com.finsec.accounting.service.OrganizationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    // NEW: Handles GET /api/organizations/user/{userId}
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Organization>> getUserOrganizations(@PathVariable String userId) {
        return ResponseEntity.ok(organizationService.getUserOrganizations(userId));
    }

    // NEW: Handles GET /api/organizations?userId=XYZ (or returns all user orgs)
    @GetMapping
    public ResponseEntity<List<Organization>> getOrganizations(@RequestParam(required = false) String userId) {
        if (userId != null && !userId.isBlank()) {
            return ResponseEntity.ok(organizationService.getUserOrganizations(userId));
        }
        // If no userId is passed, you can return a default list or fallback
        return ResponseEntity.ok(List.of());
    }

    // UPDATED: Accepts JSON body instead of request params
    @PostMapping
    public ResponseEntity<Organization> createOrganization(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String creatorUserId = payload.getOrDefault("creatorUserId", "SYSTEM_USER");
        return new ResponseEntity<>(organizationService.createOrganization(name, creatorUserId), HttpStatus.CREATED);
    }

    @PostMapping("/{orgId}/users/{userId}")
    public ResponseEntity<Organization> addUserToOrganization(
            @PathVariable String orgId,
            @PathVariable String userId) {
        return ResponseEntity.ok(organizationService.addUserToOrganization(orgId, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Organization> getOrganizationById(@PathVariable String id) {
        return ResponseEntity.ok(organizationService.getOrganizationById(id));
    }

    @DeleteMapping("/{orgId}")
    public ResponseEntity<Void> deleteOrganization(@PathVariable String orgId) {
        organizationService.deleteOrganization(orgId);
        return ResponseEntity.noContent().build(); // Returns 204 No Content on success
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        organizationService.deleteUser(userId);
        return ResponseEntity.noContent().build(); // Returns 204 No Content on success
    }
}