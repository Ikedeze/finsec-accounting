package com.finsec.accounting.ui.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finsec.accounting.dto.TransactionRequest;
import com.finsec.accounting.dto.TransactionSummaryDTO;
import com.finsec.accounting.model.Organization;
import com.finsec.accounting.model.Transaction;
import com.finsec.accounting.ui.UserSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ApiClient {

    // Clean base URL without trailing /api
    private static final String BASE_URL = "http://localhost:8080";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private String jwtToken;

    public ApiClient() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public boolean login(String username, String password) {
        try {
            String jsonBody = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", username, password);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("token")) {
                    this.jwtToken = root.get("token").asText();

                    // NEW: Capture the user ID and save it to the session
                    if (root.has("id")) {
                        UserSession.getInstance().setActiveUserId(root.get("id").asText());
                    } else {
                        System.err.println("WARNING: Login succeeded, but no User ID was returned by backend.");
                    }

                    return true;
                }
            } else {
                System.err.println("Login Failed: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getSummary(String startDate, String endDate) throws Exception {
        String orgId = UserSession.getInstance().getActiveOrganizationId();
        if (orgId == null) orgId = "";

        StringBuilder url = new StringBuilder(BASE_URL + "/api/reports/summary?");
        if (startDate != null) url.append("startDate=").append(startDate).append("&");
        if (endDate != null) url.append("endDate=").append(endDate);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .header("X-Organization-Id", orgId)
                .GET();

        if (jwtToken != null && !jwtToken.isBlank()) {
            builder.header("Authorization", "Bearer " + jwtToken);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public CompletableFuture<HttpResponse<String>> sendTransaction(String endpoint, TransactionRequest requestPayload) {
        try {
            String orgId = UserSession.getInstance().getActiveOrganizationId();
            String jsonBody = objectMapper.writeValueAsString(requestPayload);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/transactions" + endpoint))
                    .header("Content-Type", "application/json")
                    .header("X-Organization-Id", orgId != null ? orgId : "") // CRITICAL
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            if (jwtToken != null && !jwtToken.isBlank()) {
                builder.header("Authorization", "Bearer " + jwtToken);
            }

            return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<HttpResponse<String>> deleteTransactionAsync(String id) {
        try {
            String orgId = UserSession.getInstance().getActiveOrganizationId();
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/transactions/" + id))
                    .header("X-Organization-Id", orgId != null ? orgId : "") // CRITICAL
                    .DELETE();

            if (jwtToken != null && !jwtToken.isBlank()) {
                builder.header("Authorization", "Bearer " + jwtToken);
            }

            return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<HttpResponse<String>> sendBulkTransactions(String endpoint, List<TransactionRequest> requests) {
        try {
            String orgId = UserSession.getInstance().getActiveOrganizationId();
            String jsonBody = objectMapper.writeValueAsString(requests);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/transactions" + endpoint))
                    .header("Content-Type", "application/json")
                    .header("X-Organization-Id", orgId != null ? orgId : "") // CRITICAL
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            if (jwtToken != null && !jwtToken.isBlank()) {
                builder.header("Authorization", "Bearer " + jwtToken);
            }

            return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<HttpResponse<String>> patchTransactionDate(String id, String newDateIso) {
        try {
            String orgId = UserSession.getInstance().getActiveOrganizationId();
            String url = BASE_URL + "/api/transactions/" + id + "/date?newDate=" + newDateIso;

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Organization-Id", orgId != null ? orgId : "") // CRITICAL
                    .method("PATCH", HttpRequest.BodyPublishers.noBody());

            if (jwtToken != null && !jwtToken.isBlank()) {
                builder.header("Authorization", "Bearer " + jwtToken);
            }

            return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<HttpResponse<String>> patchTransactionMonthYear(String id, String newMonthYear) {
        try {
            String orgId = UserSession.getInstance().getActiveOrganizationId();
            String encodedMonthYear = java.net.URLEncoder.encode(newMonthYear, java.nio.charset.StandardCharsets.UTF_8);
            String url = BASE_URL + "/api/transactions/" + id + "/month-year?newMonthYear=" + encodedMonthYear;

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Organization-Id", orgId != null ? orgId : "") // CRITICAL
                    .method("PATCH", HttpRequest.BodyPublishers.noBody());

            if (jwtToken != null && !jwtToken.isBlank()) {
                builder.header("Authorization", "Bearer " + jwtToken);
            }

            return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<List<Transaction>> getAllTransactionsAsync() {
        String orgId = UserSession.getInstance().getActiveOrganizationId();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/transactions"))
                .header("X-Organization-Id", orgId) // CRITICAL: Send Organization ID
                .GET();

        if (jwtToken != null && !jwtToken.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + jwtToken);
        }

        return httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        return objectMapper.readValue(response.body(), new TypeReference<List<Transaction>>() {});
                    } catch (Exception e) {
                        e.printStackTrace();
                        return List.of();
                    }
                });
    }

    public CompletableFuture<List<Organization>> getAllOrganizationsAsync() {
        String activeUserId = UserSession.getInstance().getActiveUserId();

        String url = BASE_URL + "/api/organizations";
        if (activeUserId != null && !activeUserId.isBlank()) {
            url = BASE_URL + "/api/organizations/user/" + activeUserId;
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();

        if (jwtToken != null && !jwtToken.isBlank()) {
            builder.header("Authorization", "Bearer " + jwtToken);
        }

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        // Return the full list of objects, don't map to IDs!
                        return objectMapper.readValue(
                                response.body(),
                                new TypeReference<List<Organization>>() {}
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                        return List.of();
                    }
                });
    }

    public CompletableFuture<HttpResponse<String>> createOrganizationAsync(String orgName) {
        String activeUserId = UserSession.getInstance().getActiveUserId();

        if (activeUserId == null || activeUserId.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException("No active user logged in."));
        }

        // Payload matches what OrganizationController expects
        String jsonPayload = String.format("{\"name\":\"%s\", \"creatorUserId\":\"%s\"}", orgName, activeUserId);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/organizations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

        if (getJwtToken() != null && !getJwtToken().isBlank()) {
            builder.header("Authorization", "Bearer " + getJwtToken());
        }

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    public CompletableFuture<String> getSummaryAsync(String startDate, String endDate) {
        // 1. Get the organization ID
        String orgId = UserSession.getInstance().getActiveOrganizationId();

        // 2. Build URL
        StringBuilder urlBuilder = new StringBuilder(BASE_URL + "/api/transactions/summary");
        if (startDate != null && endDate != null && !startDate.isBlank() && !endDate.isBlank()) {
            urlBuilder.append("?startDate=").append(startDate).append("&endDate=").append(endDate);
        }

        // 3. Attach headers
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(urlBuilder.toString()))
                .header("X-Organization-Id", orgId) // CRITICAL: Send Organization ID
                .GET();

        if (jwtToken != null && !jwtToken.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + jwtToken);
        }

        // 4. Send Async
        return httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return response.body(); // Return the JSON string
                    } else {
                        throw new RuntimeException("Failed to fetch summary: " + response.body());
                    }
                });
    }

    public void downloadExcelReport(String reportType, LocalDate startDate, LocalDate endDate, String orgId, java.io.File targetFile) throws Exception {
        String endpoint = "/api/reports/excel/ledger";
        if ("Monthly Summary".equals(reportType)) {
            endpoint = "/api/reports/excel/monthly";
        } else if ("Category Breakdown".equals(reportType)) {
            endpoint = "/api/reports/excel/category";
        }

        StringBuilder urlBuilder = new StringBuilder(BASE_URL + endpoint + "?");
        if (startDate != null) {
            urlBuilder.append("startDate=").append(startDate.toString()).append("&");
        }
        if (endDate != null) {
            urlBuilder.append("endDate=").append(endDate.toString());
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(urlBuilder.toString()))
                .header("X-Organization-Id", orgId)
                .GET();

        if (jwtToken != null && !jwtToken.isBlank()) {
            builder.header("Authorization", "Bearer " + jwtToken);
        }

        HttpResponse<Path> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofFile(targetFile.toPath()));

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to download report. Server returned status: " + response.statusCode());
        }
    }

    public CompletableFuture<HttpResponse<String>> deleteOrganizationAsync(String id) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/organizations/" + id))
                    .DELETE();

            if (jwtToken != null && !jwtToken.isBlank()) {
                builder.header("Authorization", "Bearer " + jwtToken);
            }

            return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<HttpResponse<String>> deleteUserAsync(String userId) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/organizations/users/" + userId))
                    .DELETE();

            if (jwtToken != null && !jwtToken.isBlank()) {
                builder.header("Authorization", "Bearer " + jwtToken);
            }

            return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<HttpResponse<String>> registerAsync(String username, String email, String password) {
        try {
            String jsonBody = String.format("{\"username\":\"%s\", \"email\":\"%s\", \"password\":\"%s\"}",
                    username, email, password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<HttpResponse<String>> addUserToOrgAsync(String orgId, String userId) {
        try {
            String encodedIdentifier = java.net.URLEncoder.encode(userId, java.nio.charset.StandardCharsets.UTF_8);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/organizations/" + orgId + "/users/" + encodedIdentifier))
                    .POST(HttpRequest.BodyPublishers.noBody());

            if (jwtToken != null && !jwtToken.isBlank()) {
                builder.header("Authorization", "Bearer " + jwtToken);
            }

            return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public String getJwtToken() { return jwtToken; }
    public void setJwtToken(String jwtToken) { this.jwtToken = jwtToken; }
}