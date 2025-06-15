
package com.bank.loanmanagement.handlers;

import com.bank.loanmanagement.util.DatabaseManager;
import com.bank.loanmanagement.util.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.LocalDateTime;

public class SystemInfoHandler implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response = createSystemInfoJson();
        HttpUtils.sendResponse(exchange, response, "application/json");
    }
    
    private String createSystemInfoJson() {
        return "{\n" +
            "  \"service\": \"Enterprise Loan Management System\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"status\": \"running\",\n" +
            "  \"timestamp\": \"" + LocalDateTime.now() + "\",\n" +
            "  \"description\": \"Production-ready loan management with DDD and hexagonal architecture\",\n" +
            "  \"database_connected\": " + DatabaseManager.isConnected() + ",\n" +
            "  \"technology_stack\": {\n" +
            "    \"java\": \"Java 21 with Virtual Threads\",\n" +
            "    \"framework\": \"Spring Boot 3.2\",\n" +
            "    \"database\": \"PostgreSQL 16.9 (production)\",\n" +
            "    \"architecture\": \"Hexagonal Architecture with DDD\"\n" +
            "  },\n" +
            "  \"bounded_contexts\": [\n" +
            "    \"Customer Management\",\n" +
            "    \"Loan Origination\", \n" +
            "    \"Payment Processing\"\n" +
            "  ],\n" +
            "  \"business_rules\": {\n" +
            "    \"installments\": [6, 9, 12, 24],\n" +
            "    \"interest_rates\": \"0.1% - 0.5% monthly\",\n" +
            "    \"max_loan_amount\": 500000,\n" +
            "    \"min_loan_amount\": 1000\n" +
            "  }\n" +
            "}";
    }
}
package com.bank.loanmanagement.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.bank.loanmanagement.DatabaseConnectedApplication;
import com.bank.loanmanagement.utils.ResponseUtils;

import java.io.IOException;
import java.time.LocalDateTime;

public class SystemInfoHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
        String response = createSystemInfoJson();
        ResponseUtils.sendResponse(exchange, response, "application/json");
    }
    
    private String createSystemInfoJson() {
        return "{\n" +
            "  \"service\": \"Enterprise Loan Management System\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"status\": \"running\",\n" +
            "  \"timestamp\": \"" + LocalDateTime.now() + "\",\n" +
            "  \"description\": \"Production-ready loan management with DDD and hexagonal architecture\",\n" +
            "  \"database_connected\": " + (DatabaseConnectedApplication.getDbConnection() != null) + ",\n" +
            "  \"technology_stack\": {\n" +
            "    \"java\": \"Java 21 with Virtual Threads\",\n" +
            "    \"framework\": \"Spring Boot 3.2\",\n" +
            "    \"database\": \"PostgreSQL 16.9 (production)\",\n" +
            "    \"architecture\": \"Hexagonal Architecture with DDD\"\n" +
            "  },\n" +
            "  \"bounded_contexts\": [\n" +
            "    \"Customer Management\",\n" +
            "    \"Loan Origination\", \n" +
            "    \"Payment Processing\"\n" +
            "  ],\n" +
            "  \"business_rules\": {\n" +
            "    \"installments\": [6, 9, 12, 24],\n" +
            "    \"interest_rates\": \"0.1% - 0.5% monthly\",\n" +
            "    \"max_loan_amount\": 500000,\n" +
            "    \"min_loan_amount\": 1000\n" +
            "  }\n" +
            "}";
    }
}
