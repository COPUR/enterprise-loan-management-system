package com.loanmanagement.shared.application.port.out;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Outbound Port for HTTP Client Communication
 * Abstracts HTTP client operations from the application layer
 */
public interface HttpClientPort {
    
    /**
     * Execute a GET request
     */
    <T> HttpResponse<T> get(String url, Class<T> responseType);
    
    /**
     * Execute a GET request with headers
     */
    <T> HttpResponse<T> get(String url, Map<String, String> headers, Class<T> responseType);
    
    /**
     * Execute a POST request
     */
    <T, R> HttpResponse<R> post(String url, T requestBody, Class<R> responseType);
    
    /**
     * Execute a POST request with headers
     */
    <T, R> HttpResponse<R> post(String url, T requestBody, Map<String, String> headers, Class<R> responseType);
    
    /**
     * Execute a PUT request
     */
    <T, R> HttpResponse<R> put(String url, T requestBody, Class<R> responseType);
    
    /**
     * Execute a DELETE request
     */
    <T> HttpResponse<T> delete(String url, Class<T> responseType);
    
    /**
     * Execute an async GET request
     */
    <T> CompletableFuture<HttpResponse<T>> getAsync(String url, Class<T> responseType);
    
    /**
     * Execute an async POST request
     */
    <T, R> CompletableFuture<HttpResponse<R>> postAsync(String url, T requestBody, Class<R> responseType);
    
    /**
     * HTTP Response wrapper
     */
    record HttpResponse<T>(
            int statusCode,
            T body,
            Map<String, String> headers,
            boolean successful
    ) {
        public boolean isSuccessful() {
            return successful;
        }
        
        public boolean isClientError() {
            return statusCode >= 400 && statusCode < 500;
        }
        
        public boolean isServerError() {
            return statusCode >= 500;
        }
    }
}