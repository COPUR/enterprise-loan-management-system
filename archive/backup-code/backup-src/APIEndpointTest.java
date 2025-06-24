package com.bank.loanmanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

public class APIEndpointTest {
    
    private static final String BASE_URL = "http://localhost:5000";
    private HttpClient httpClient;
    
    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    @Test
    @DisplayName("Should return 200 OK for health endpoint")
    void shouldReturn200ForHealthEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/health"))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("status"));
        assertTrue(response.body().contains("running"));
    }
    
    @Test
    @DisplayName("Should return customer data from customers endpoint")
    void shouldReturnCustomerDataFromCustomersEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/customers"))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("customers"));
        assertTrue(response.body().contains("customer_id"));
        assertTrue(response.body().contains("first_name"));
        assertTrue(response.body().contains("email"));
    }
    
    @Test
    @DisplayName("Should return loan data from loans endpoint")
    void shouldReturnLoanDataFromLoansEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/loans"))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("loans"));
        assertTrue(response.body().contains("loan_id"));
        assertTrue(response.body().contains("loan_amount"));
        assertTrue(response.body().contains("interest_rate"));
        assertTrue(response.body().contains("installments"));
    }
    
    @Test
    @DisplayName("Should return payment data from payments endpoint")
    void shouldReturnPaymentDataFromPaymentsEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/payments"))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("payments"));
        assertTrue(response.body().contains("payment_id"));
        assertTrue(response.body().contains("payment_amount"));
        assertTrue(response.body().contains("payment_status"));
    }
    
    @Test
    @DisplayName("Should return FAPI compliance report")
    void shouldReturnFAPIComplianceReport() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/v1/fapi/compliance-report"))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("fapi_compliance_assessment"));
        assertTrue(response.body().contains("overall_compliance_score"));
        assertTrue(response.body().contains("71.4%"));
    }
    
    @Test
    @DisplayName("Should return TDD coverage report")
    void shouldReturnTDDCoverageReport() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/v1/tdd/coverage-report"))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("tdd_coverage_assessment"));
        assertTrue(response.body().contains("overall_coverage_rate"));
        assertTrue(response.body().contains("test_categories"));
    }
    
    @Test
    @DisplayName("Should handle OPTIONS requests for CORS")
    void shouldHandleOPTIONSRequestsForCORS() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/customers"))
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Should handle CORS preflight
        assertTrue(response.statusCode() == 200 || response.statusCode() == 204);
    }
    
    @Test
    @DisplayName("Should include security headers in responses")
    void shouldIncludeSecurityHeadersInResponses() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/health"))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        
        // Check for security headers
        assertTrue(response.headers().firstValue("X-Content-Type-Options").isPresent());
        assertTrue(response.headers().firstValue("X-Frame-Options").isPresent());
        assertTrue(response.headers().firstValue("Strict-Transport-Security").isPresent());
    }
    
    @Test
    @DisplayName("Should handle large response payloads efficiently")
    void shouldHandleLargeResponsePayloadsEfficiently() throws Exception {
        long startTime = System.currentTimeMillis();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/customers"))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        assertEquals(200, response.statusCode());
        assertTrue(responseTime < 2000, "Response time: " + responseTime + "ms should be under 2000ms");
        assertTrue(response.body().length() > 0);
    }
    
    @Test
    @DisplayName("Should handle concurrent API requests")
    void shouldHandleConcurrentAPIRequests() throws Exception {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(10);
        java.util.List<java.util.concurrent.Future<HttpResponse<String>>> futures = new java.util.ArrayList<>();
        
        // Submit 20 concurrent requests
        for (int i = 0; i < 20; i++) {
            futures.add(executor.submit(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/health"))
                        .GET()
                        .build();
                    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        
        // Verify all requests succeed
        for (java.util.concurrent.Future<HttpResponse<String>> future : futures) {
            HttpResponse<String> response = future.get();
            assertEquals(200, response.statusCode());
        }
        
        executor.shutdown();
    }
}