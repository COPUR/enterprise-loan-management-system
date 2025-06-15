
package com.bank.loanmanagement.handlers;

import com.bank.loanmanagement.util.DatabaseManager;
import com.bank.loanmanagement.util.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerHandler implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String response;
        
        switch (method) {
            case "GET":
                response = getCustomersFromDatabase();
                break;
            case "POST":
                String requestBody = HttpUtils.readRequestBody(exchange);
                response = createCustomerInDatabase(requestBody);
                break;
            default:
                response = "{\"error\": \"Method not supported\"}";
        }
        
        HttpUtils.sendResponse(exchange, response, "application/json");
    }
    
    private String getCustomersFromDatabase() {
        Connection dbConnection = DatabaseManager.getConnection();
        if (dbConnection == null) {
            return "{\"error\": \"Database connection not available\", \"bounded_context\": \"Customer Management (DDD)\"}";
        }
        
        try {
            String sql = "SELECT id, customer_number, first_name, last_name, email, credit_score, annual_income, employment_status, city, state, status FROM customer_management.customers ORDER BY created_at DESC";
            PreparedStatement stmt = dbConnection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            StringBuilder customers = new StringBuilder();
            customers.append("{\n  \"customers\": [\n");
            
            boolean first = true;
            int count = 0;
            while (rs.next()) {
                if (!first) customers.append(",\n");
                customers.append("    {\n");
                customers.append("      \"id\": ").append(rs.getLong("id")).append(",\n");
                customers.append("      \"customerNumber\": \"").append(rs.getString("customer_number")).append("\",\n");
                customers.append("      \"name\": \"").append(rs.getString("first_name")).append(" ").append(rs.getString("last_name")).append("\",\n");
                customers.append("      \"email\": \"").append(rs.getString("email")).append("\",\n");
                customers.append("      \"creditScore\": ").append(rs.getInt("credit_score")).append(",\n");
                customers.append("      \"annualIncome\": ").append(rs.getBigDecimal("annual_income")).append(",\n");
                customers.append("      \"employmentStatus\": \"").append(rs.getString("employment_status")).append("\",\n");
                customers.append("      \"city\": \"").append(rs.getString("city")).append("\",\n");
                customers.append("      \"state\": \"").append(rs.getString("state")).append("\",\n");
                customers.append("      \"status\": \"").append(rs.getString("status")).append("\"\n");
                customers.append("    }");
                first = false;
                count++;
            }
            
            customers.append("\n  ],\n");
            customers.append("  \"total\": ").append(count).append(",\n");
            customers.append("  \"boundedContext\": \"Customer Management (DDD)\",\n");
            customers.append("  \"dataSource\": \"PostgreSQL Database - Live Data\"\n");
            customers.append("}");
            
            rs.close();
            stmt.close();
            
            return customers.toString();
            
        } catch (SQLException e) {
            return "{\"error\": \"Database query failed: " + e.getMessage() + "\", \"bounded_context\": \"Customer Management (DDD)\"}";
        }
    }
    
    private String createCustomerInDatabase(String requestBody) {
        return "{\"message\": \"Customer creation endpoint - requires database implementation\", \"bounded_context\": \"Customer Management (DDD)\"}";
    }
}
package com.bank.loanmanagement.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.bank.loanmanagement.DatabaseConnectedApplication;
import com.bank.loanmanagement.utils.ResponseUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String response;
        
        switch (method) {
            case "GET":
                response = getCustomersFromDatabase();
                break;
            case "POST":
                String requestBody = ResponseUtils.readRequestBody(exchange);
                response = createCustomerInDatabase(requestBody);
                break;
            default:
                response = "{\"error\": \"Method not supported\"}";
        }
        
        ResponseUtils.sendResponse(exchange, response, "application/json");
    }
    
    private static String getCustomersFromDatabase() {
        Connection dbConnection = DatabaseConnectedApplication.getDbConnection();
        if (dbConnection == null) {
            return "{\"error\": \"Database connection not available\", \"bounded_context\": \"Customer Management (DDD)\"}";
        }
        
        try {
            String sql = "SELECT id, customer_number, first_name, last_name, email, credit_score, annual_income, employment_status, city, state, status FROM customer_management.customers ORDER BY created_at DESC";
            PreparedStatement stmt = dbConnection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            StringBuilder customers = new StringBuilder();
            customers.append("{\n  \"customers\": [\n");
            
            boolean first = true;
            int count = 0;
            while (rs.next()) {
                if (!first) customers.append(",\n");
                customers.append("    {\n");
                customers.append("      \"id\": ").append(rs.getLong("id")).append(",\n");
                customers.append("      \"customerNumber\": \"").append(rs.getString("customer_number")).append("\",\n");
                customers.append("      \"name\": \"").append(rs.getString("first_name")).append(" ").append(rs.getString("last_name")).append("\",\n");
                customers.append("      \"email\": \"").append(rs.getString("email")).append("\",\n");
                customers.append("      \"creditScore\": ").append(rs.getInt("credit_score")).append(",\n");
                customers.append("      \"annualIncome\": ").append(rs.getBigDecimal("annual_income")).append(",\n");
                customers.append("      \"employmentStatus\": \"").append(rs.getString("employment_status")).append("\",\n");
                customers.append("      \"city\": \"").append(rs.getString("city")).append("\",\n");
                customers.append("      \"state\": \"").append(rs.getString("state")).append("\",\n");
                customers.append("      \"status\": \"").append(rs.getString("status")).append("\"\n");
                customers.append("    }");
                first = false;
                count++;
            }
            
            customers.append("\n  ],\n");
            customers.append("  \"total\": ").append(count).append(",\n");
            customers.append("  \"boundedContext\": \"Customer Management (DDD)\",\n");
            customers.append("  \"dataSource\": \"PostgreSQL Database - Live Data\"\n");
            customers.append("}");
            
            rs.close();
            stmt.close();
            
            return customers.toString();
            
        } catch (SQLException e) {
            return "{\"error\": \"Database query failed: " + e.getMessage() + "\", \"bounded_context\": \"Customer Management (DDD)\"}";
        }
    }
    
    private static String createCustomerInDatabase(String requestBody) {
        return "{\"message\": \"Customer creation endpoint - requires database implementation\", \"bounded_context\": \"Customer Management (DDD)\"}";
    }
}
