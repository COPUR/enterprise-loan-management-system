package com.bank.loan.loan.security.par.model;

public class PARResponse {
    
    private final String requestUri;
    private final int expiresIn;
    
    public PARResponse(String requestUri, int expiresIn) {
        this.requestUri = requestUri;
        this.expiresIn = expiresIn;
    }
    
    public String getRequestUri() {
        return requestUri;
    }
    
    public int getExpiresIn() {
        return expiresIn;
    }
    
    @Override
    public String toString() {
        return "PARResponse{" +
                "requestUri='" + requestUri + '\'' +
                ", expiresIn=" + expiresIn +
                '}';
    }
}