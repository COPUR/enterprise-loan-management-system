
package com.bank.loanmanagement.handlers;

import com.bank.loanmanagement.util.DatabaseManager;
import com.bank.loanmanagement.util.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.LocalDateTime;

public class HealthHandler implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response = "{\n" +
            "  \"status\": \"UP\",\n" +
            "  \"timestamp\": \"" + LocalDateTime.now() + "\",\n" +
            "  \"java_version\": \"" + System.getProperty("java.version") + "\",\n" +
            "  \"database_connected\": " + DatabaseManager.isConnected() + ",\n" +
            "  \"virtual_threads_enabled\": " + (Runtime.version().feature() >= 21) + "\n" +
            "}";
        HttpUtils.sendResponse(exchange, response, "application/json");
    }
}
package com.bank.loanmanagement.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.bank.loanmanagement.DatabaseConnectedApplication;
import com.bank.loanmanagement.utils.ResponseUtils;

import java.io.IOException;
import java.time.LocalDateTime;

public class HealthHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
        String response = "{\n" +
            "  \"status\": \"UP\",\n" +
            "  \"timestamp\": \"" + LocalDateTime.now() + "\",\n" +
            "  \"java_version\": \"" + System.getProperty("java.version") + "\",\n" +
            "  \"database_connected\": " + (DatabaseConnectedApplication.getDbConnection() != null) + ",\n" +
            "  \"virtual_threads_enabled\": " + (Runtime.version().feature() >= 21) + "\n" +
            "}";
        ResponseUtils.sendResponse(exchange, response, "application/json");
    }
}
