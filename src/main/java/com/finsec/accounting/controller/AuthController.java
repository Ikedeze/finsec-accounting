// 🔄 UPDATED
package com.finsec.accounting.controller;

import com.finsec.accounting.dto.UserResponse;
import com.finsec.accounting.dto.JwtResponse;
import com.finsec.accounting.dto.LoginRequest;
import com.finsec.accounting.dto.RegisterRequest;
import com.finsec.accounting.model.Role;
import com.finsec.accounting.model.User;
import com.finsec.accounting.repository.UserRepository;
import com.finsec.accounting.security.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Add to AuthController.java class header:
@io.swagger.v3.oas.annotations.tags.Tag(name = "Authentication & User Management",
        description = "Endpoints for User Login, Registration, and Admin CRUD operations")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          PasswordEncoder encoder,
                          JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        Set<Role> roles = new HashSet<>();

        // 💡 Check if an ADMIN already exists anywhere in the database
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(u -> u.getRoles().contains(Role.ROLE_ADMIN));

        // Bootstrap: If no users exist, automatically make the first account ROLE_ADMIN
        if (userRepository.count() == 0) {
            roles.add(Role.ROLE_ADMIN);
            roles.add(Role.ROLE_USER);
        } else {
            // Assign requested roles
            if (registerRequest.getRoles() != null && !registerRequest.getRoles().isEmpty()) {
                for (String roleStr : registerRequest.getRoles()) {
                    if ("ROLE_ADMIN".equalsIgnoreCase(roleStr) || "ADMIN".equalsIgnoreCase(roleStr)) {
                        if (adminExists) {
                            return ResponseEntity.badRequest()
                                    .body("Error: An Admin already exists! Only one Admin is allowed.");
                        }
                        roles.add(Role.ROLE_ADMIN);
                    } else {
                        roles.add(Role.ROLE_USER);
                    }
                }
            } else {
                roles.add(Role.ROLE_USER);
            }
        }

        User user = new User(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                encoder.encode(registerRequest.getPassword()),
                roles
        );

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully with roles: " + roles);
    }

    @Operation(summary = "Log in user",
            description = "Authenticates credentials and returns a JWT Bearer token")
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateToken(loginRequest.getUsername());

        User user = userRepository.findByUsernameIgnoreCase(loginRequest.getUsername()).orElseThrow();
        Set<String> userRoles = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        // UPDATED CONSTRUCTOR CALL HERE
        return ResponseEntity.ok(new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), userRoles));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get all users",
            description = "Retrieves a list of all registered accounts (Requires ROLE_ADMIN)")
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(user -> {
                    Set<String> roleNames = user.getRoles().stream()
                            .map(Object::toString) // 👈 SAFE: Handles both String and Enum gracefully
                            .collect(Collectors.toSet());

                    return new UserResponse(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            roleNames
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }


    @DeleteMapping("/users/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        userRepository.delete(user);
        return ResponseEntity.ok("User '" + username + "' deleted successfully.");
    }
}