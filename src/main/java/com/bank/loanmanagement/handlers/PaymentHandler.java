
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

public class PaymentHandler implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String response;
        
        switch (method) {
            case "GET":
                response = getPaymentsFromDatabase();
                break;
            case "POST":
                String requestBody = HttpUtils.readRequestBody(exchange);
                response = createPaymentInDatabase(requestBody);
                break;
            default:
                response = "{\"error\": \"Method not supported\"}";
        }
        
        HttpUtils.sendResponse(exchange, response, "application/json");
    }
    
    private String getPaymentsFromDatabase() {
        Connection dbConnection = DatabaseManager.getConnection();
        if (dbConnection == null) {
            return "{\"error\": \"Database connection not available\", \"bounded_context\": \"Payment Processing (DDD)\"}";
        }
        
        try {
            String sql = "SELECT id, payment_number, loan_id, customer_id, payment_type, scheduled_amount, actual_amount, principal_amount, interest_amount, penalty_amount, scheduled_date, actual_payment_date, payment_status, payment_method, transaction_reference FROM payment_processing.payments ORDER BY created_at DESC";
            PreparedStatement stmt = dbConnection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            StringBuilder payments = new StringBuilder();
            payments.append("{\n  \"payments\": [\n");
            
            boolean first = true;
            int count = 0;
            while (rs.next()) {
                if (!first) payments.append(",\n");
                payments.append("    {\n");
                payments.append("      \"id\": ").append(rs.getLong("id")).append(",\n");
                payments.append("      \"paymentNumber\": \"").append(rs.getString("payment_number")).append("\",\n");
                payments.append("      \"loanId\": ").append(rs.getLong("loan_id")).append(",\n");
                payments.append("      \"customerId\": ").append(rs.getLong("customer_id")).append(",\n");
                payments.append("      \"paymentType\": \"").append(rs.getString("payment_type")).append("\",\n");
                payments.append("      \"scheduledAmount\": ").append(rs.getBigDecimal("scheduled_amount")).append(",\n");
                payments.append("      \"actualAmount\": ").append(rs.getBigDecimal("actual_amount")).append(",\n");
                payments.append("      \"principalAmount\": ").append(rs.getBigDecimal("principal_amount")).append(",\n");
                payments.append("      \"interestAmount\": ").append(rs.getBigDecimal("interest_amount")).append(",\n");
                payments.append("      \"penaltyAmount\": ").append(rs.getBigDecimal("penalty_amount")).append(",\n");
                payments.append("      \"scheduledDate\": \"").append(rs.getDate("scheduled_date")).append(",\n");
                payments.append("      \"actualPaymentDate\": \"").append(rs.getTimestamp("actual_payment_date")).append("\",\n");
                payments.append("      \"paymentStatus\": \"").append(rs.getString("payment_status")).append("\",\n");
                payments.append("      \"paymentMethod\": \"").append(rs.getString("payment_method")).append("\",\n");
                payments.append("      \"transactionReference\": \"").append(rs.getString("transaction_reference")).append("\"\n");
                payments.append("    }");
                first = false;
                count++;
            }
            
            payments.append("\n  ],\n");
            payments.append("  \"total\": ").append(count).append(",\n");
            payments.append("  \"boundedContext\": \"Payment Processing (DDD)\",\n");
            payments.append("  \"businessRules\": {\"paymentTypes\": [\"REGULAR\", \"EARLY\", \"PARTIAL\", \"LATE\"], \"calculations\": \"Interest and penalty calculations applied\"},\n");
            payments.append("  \"dataSource\": \"PostgreSQL Database - Live Data\"\n");
            payments.append("}");
            
            rs.close();
            stmt.close();
            
            return payments.toString();
            
        } catch (SQLException e) {
            return "{\"error\": \"Database query failed: " + e.getMessage() + "\", \"bounded_context\": \"Payment Processing (DDD)\"}";
        }
    }
    
    private String createPaymentInDatabase(String requestBody) {
        return "{\"message\": \"Payment creation endpoint - requires database implementation\", \"bounded_context\": \"Payment Processing (DDD)\"}";
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

public class PaymentHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String response;
        
        switch (method) {
            case "GET":
                response = getPaymentsFromDatabase();
                break;
            case "POST":
                String requestBody = ResponseUtils.readRequestBody(exchange);
                response = createPaymentInDatabase(requestBody);
                break;
            default:
                response = "{\"error\": \"Method not supported\"}";
        }
        
        ResponseUtils.sendResponse(exchange, response, "application/json");
    }
    
    private static String getPaymentsFromDatabase() {
        Connection dbConnection = DatabaseConnectedApplication.getDbConnection();
        if (dbConnection == null) {
            return "{\"error\": \"Database connection not available\", \"bounded_context\": \"Payment Processing (DDD)\"}";
        }
        
        try {
            String sql = "SELECT id, payment_number, loan_id, customer_id, payment_type, scheduled_amount, actual_amount, principal_amount, interest_amount, penalty_amount, scheduled_date, actual_payment_date, payment_status, payment_method, transaction_reference FROM payment_processing.payments ORDER BY created_at DESC";
            PreparedStatement stmt = dbConnection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            StringBuilder payments = new StringBuilder();
            payments.append("{\n  \"payments\": [\n");
            
            boolean first = true;
            int count = 0;
            while (rs.next()) {
                if (!first) payments.append(",\n");
                payments.append("    {\n");
                payments.append("      \"id\": ").append(rs.getLong("id")).append(",\n");
                payments.append("      \"paymentNumber\": \"").append(rs.getString("payment_number")).append("\",\n");
                payments.append("      \"loanId\": ").append(rs.getLong("loan_id")).append(",\n");
                payments.append("      \"customerId\": ").append(rs.getLong("customer_id")).append(",\n");
                payments.append("      \"paymentType\": \"").append(rs.getString("payment_type")).append("\",\n");
                payments.append("      \"scheduledAmount\": ").append(rs.getBigDecimal("scheduled_amount")).append(",\n");
                payments.append("      \"actualAmount\": ").append(rs.getBigDecimal("actual_amount")).append(",\n");
                payments.append("      \"principalAmount\": ").append(rs.getBigDecimal("principal_amount")).append(",\n");
                payments.append("      \"interestAmount\": ").append(rs.getBigDecimal("interest_amount")).append(",\n");
                payments.append("      \"penaltyAmount\": ").append(rs.getBigDecimal("penalty_amount")).append(",\n");
                payments.append("      \"scheduledDate\": \"").append(rs.getDate("scheduled_date")).append("\",\n");
                payments.append("      \"actualPaymentDate\": \"").append(rs.getTimestamp("actual_payment_date")).append("\",\n");
                payments.append("      \"paymentStatus\": \"").append(rs.getString("payment_status")).append("\",\n");
                payments.append("      \"paymentMethod\": \"").append(rs.getString("payment_method")).append("\",\n");
                payments.append("      \"transactionReference\": \"").append(rs.getString("transaction_reference")).append("\"\n");
                payments.append("    }");
                first = false;
                count++;
            }
            
            payments.append("\n  ],\n");
            payments.append("  \"total\": ").append(count).append(",\n");
            payments.append("  \"boundedContext\": \"Payment Processing (DDD)\",\n");
            payments.append("  \"businessRules\": {\"paymentTypes\": [\"REGULAR\", \"EARLY\", \"PARTIAL\", \"LATE\"], \"calculations\": \"Interest and penalty calculations applied\"},\n");
            payments.append("  \"dataSource\": \"PostgreSQL Database - Live Data\"\n");
            payments.append("}");
            
            rs.close();
            stmt.close();
            
            return payments.toString();
            
        } catch (SQLException e) {
            return "{\"error\": \"Database query failed: " + e.getMessage() + "\", \"bounded_context\": \"Payment Processing (DDD)\"}";
        }
    }
    
    private static String createPaymentInDatabase(String requestBody) {
        return "{\"message\": \"Payment creation endpoint - requires database implementation\", \"bounded_context\": \"Payment Processing (DDD)\"}";
    }
}
