
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

public class LoanHandler implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String response;
        
        switch (method) {
            case "GET":
                response = getLoansFromDatabase();
                break;
            case "POST":
                String requestBody = HttpUtils.readRequestBody(exchange);
                response = createLoanInDatabase(requestBody);
                break;
            default:
                response = "{\"error\": \"Method not supported\"}";
        }
        
        HttpUtils.sendResponse(exchange, response, "application/json");
    }
    
    private String getLoansFromDatabase() {
        Connection dbConnection = DatabaseManager.getConnection();
        if (dbConnection == null) {
            return "{\"error\": \"Database connection not available\", \"bounded_context\": \"Loan Origination (DDD)\"}";
        }
        
        try {
            String sql = "SELECT id, loan_number, customer_id, principal_amount, installment_count, monthly_interest_rate, monthly_payment_amount, total_amount, outstanding_balance, loan_status, disbursement_date, maturity_date FROM loan_origination.loans ORDER BY created_at DESC";
            PreparedStatement stmt = dbConnection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            StringBuilder loans = new StringBuilder();
            loans.append("{\n  \"loans\": [\n");
            
            boolean first = true;
            int count = 0;
            while (rs.next()) {
                if (!first) loans.append(",\n");
                loans.append("    {\n");
                loans.append("      \"id\": ").append(rs.getLong("id")).append(",\n");
                loans.append("      \"loanNumber\": \"").append(rs.getString("loan_number")).append("\",\n");
                loans.append("      \"customerId\": ").append(rs.getLong("customer_id")).append(",\n");
                loans.append("      \"principalAmount\": ").append(rs.getBigDecimal("principal_amount")).append(",\n");
                loans.append("      \"installmentCount\": ").append(rs.getInt("installment_count")).append(",\n");
                loans.append("      \"monthlyInterestRate\": ").append(rs.getBigDecimal("monthly_interest_rate")).append(",\n");
                loans.append("      \"monthlyPaymentAmount\": ").append(rs.getBigDecimal("monthly_payment_amount")).append(",\n");
                loans.append("      \"totalAmount\": ").append(rs.getBigDecimal("total_amount")).append(",\n");
                loans.append("      \"outstandingBalance\": ").append(rs.getBigDecimal("outstanding_balance")).append(",\n");
                loans.append("      \"loanStatus\": \"").append(rs.getString("loan_status")).append("\",\n");
                loans.append("      \"disbursementDate\": \"").append(rs.getTimestamp("disbursement_date")).append("\",\n");
                loans.append("      \"maturityDate\": \"").append(rs.getDate("maturity_date")).append("\"\n");
                loans.append("    }");
                first = false;
                count++;
            }
            
            loans.append("\n  ],\n");
            loans.append("  \"total\": ").append(count).append(",\n");
            loans.append("  \"boundedContext\": \"Loan Origination (DDD)\",\n");
            loans.append("  \"businessRules\": {\"installmentsAllowed\": [6, 9, 12, 24], \"interestRateRange\": \"0.1% - 0.5% monthly\"},\n");
            loans.append("  \"dataSource\": \"PostgreSQL Database - Live Data\"\n");
            loans.append("}");
            
            rs.close();
            stmt.close();
            
            return loans.toString();
            
        } catch (SQLException e) {
            return "{\"error\": \"Database query failed: " + e.getMessage() + "\", \"bounded_context\": \"Loan Origination (DDD)\"}";
        }
    }
    
    private String createLoanInDatabase(String requestBody) {
        return "{\"message\": \"Loan creation endpoint - requires database implementation\", \"bounded_context\": \"Loan Origination (DDD)\"}";
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

public class LoanHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String response;
        
        switch (method) {
            case "GET":
                response = getLoansFromDatabase();
                break;
            case "POST":
                String requestBody = ResponseUtils.readRequestBody(exchange);
                response = createLoanInDatabase(requestBody);
                break;
            default:
                response = "{\"error\": \"Method not supported\"}";
        }
        
        ResponseUtils.sendResponse(exchange, response, "application/json");
    }
    
    private static String getLoansFromDatabase() {
        Connection dbConnection = DatabaseConnectedApplication.getDbConnection();
        if (dbConnection == null) {
            return "{\"error\": \"Database connection not available\", \"bounded_context\": \"Loan Origination (DDD)\"}";
        }
        
        try {
            String sql = "SELECT id, loan_number, customer_id, principal_amount, installment_count, monthly_interest_rate, monthly_payment_amount, total_amount, outstanding_balance, loan_status, disbursement_date, maturity_date FROM loan_origination.loans ORDER BY created_at DESC";
            PreparedStatement stmt = dbConnection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            StringBuilder loans = new StringBuilder();
            loans.append("{\n  \"loans\": [\n");
            
            boolean first = true;
            int count = 0;
            while (rs.next()) {
                if (!first) loans.append(",\n");
                loans.append("    {\n");
                loans.append("      \"id\": ").append(rs.getLong("id")).append(",\n");
                loans.append("      \"loanNumber\": \"").append(rs.getString("loan_number")).append("\",\n");
                loans.append("      \"customerId\": ").append(rs.getLong("customer_id")).append(",\n");
                loans.append("      \"principalAmount\": ").append(rs.getBigDecimal("principal_amount")).append(",\n");
                loans.append("      \"installmentCount\": ").append(rs.getInt("installment_count")).append(",\n");
                loans.append("      \"monthlyInterestRate\": ").append(rs.getBigDecimal("monthly_interest_rate")).append(",\n");
                loans.append("      \"monthlyPaymentAmount\": ").append(rs.getBigDecimal("monthly_payment_amount")).append(",\n");
                loans.append("      \"totalAmount\": ").append(rs.getBigDecimal("total_amount")).append(",\n");
                loans.append("      \"outstandingBalance\": ").append(rs.getBigDecimal("outstanding_balance")).append(",\n");
                loans.append("      \"loanStatus\": \"").append(rs.getString("loan_status")).append("\",\n");
                loans.append("      \"disbursementDate\": \"").append(rs.getTimestamp("disbursement_date")).append("\",\n");
                loans.append("      \"maturityDate\": \"").append(rs.getDate("maturity_date")).append("\"\n");
                loans.append("    }");
                first = false;
                count++;
            }
            
            loans.append("\n  ],\n");
            loans.append("  \"total\": ").append(count).append(",\n");
            loans.append("  \"boundedContext\": \"Loan Origination (DDD)\",\n");
            loans.append("  \"businessRules\": {\"installmentsAllowed\": [6, 9, 12, 24], \"interestRateRange\": \"0.1% - 0.5% monthly\"},\n");
            loans.append("  \"dataSource\": \"PostgreSQL Database - Live Data\"\n");
            loans.append("}");
            
            rs.close();
            stmt.close();
            
            return loans.toString();
            
        } catch (SQLException e) {
            return "{\"error\": \"Database query failed: " + e.getMessage() + "\", \"bounded_context\": \"Loan Origination (DDD)\"}";
        }
    }
    
    private static String createLoanInDatabase(String requestBody) {
        return "{\"message\": \"Loan creation endpoint - requires database implementation\", \"bounded_context\": \"Loan Origination (DDD)\"}";
    }
}
Ω