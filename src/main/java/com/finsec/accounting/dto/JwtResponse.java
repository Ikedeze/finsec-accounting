package com.finsec.accounting.dto;

import java.util.Set;

public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private String id;       // NEW FIELD
    private String username;
    private String email;
    private Set<String> roles;

    // UPDATE CONSTRUCTOR
    public JwtResponse(String accessToken, String id, String username, String email, Set<String> roles) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }

    public String getToken() { return token; }
    public String getType() { return type; }
    public String getId() { return id; } // NEW GETTER
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public Set<String> getRoles() { return roles; }
}