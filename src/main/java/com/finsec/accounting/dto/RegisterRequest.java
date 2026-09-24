// 🔄 UPDATED
package com.finsec.accounting.dto;

import java.util.Set;

public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private Set<String> roles; // ➕ ADDED: Optional role set (e.g. ["ROLE_ADMIN"])

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Set<String> getRoles() { return roles; } // ➕ ADDED
    public void setRoles(Set<String> roles) { this.roles = roles; } // ➕ ADDED
}