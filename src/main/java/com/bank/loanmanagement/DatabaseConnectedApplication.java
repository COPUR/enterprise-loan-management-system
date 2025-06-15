package com.bank.loanmanagement;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.concurrent.*;

import com.sun.net.httpserver.*;

public class DatabaseConnectedApplication {

    private static final int PORT = 5000;
    private static HttpServer server;
    private static Connection dbConnection;

    public static void main(String[] args) throws IOException {
        System.out.println("Starting Enterprise Loan Management System");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Using Virtual Threads: " + (Runtime.version().feature() >= 21));

        // Initialize PostgreSQL connection
        initializeDatabase();

        server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

        // Configure endpoints
        server.createContext("/", new SystemInfoHandler());
        server.createContext("/health", new HealthHandler());
        server.createContext("/api/customers", new CustomerHandler());
        server.createContext("/api/loans", new LoanHandler());
        server.createContext("/api/payments", new PaymentHandler());

        // Use virtual threads if available (Java 21+)
        if (Runtime.version().feature() >= 21) {
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        } else {
            server.setExecutor(Executors.newFixedThreadPool(10));
        }

        server.start();
        System.out.println("Enterprise Loan Management System started on port " + PORT);
        System.out.println("Technology Stack: Java 21 + Spring Boot 3.2 Architecture");
        System.out.println("PostgreSQL Database Connected: " + (dbConnection != null));
        System.out.println("Access: http://localhost:" + PORT + "/");
    }

    private static void initializeDatabase() {
        try {
            // Explicitly load PostgreSQL driver
            Class.forName("org.postgresql.Driver");

            String databaseUrl = System.getenv("DATABASE_URL");
            if (databaseUrl != null) {
                dbConnection = DriverManager.getConnection(databaseUrl);
                System.out.println("Connected to PostgreSQL database");
                System.out.println("Database URL: " + databaseUrl.substring(0, Math.min(30, databaseUrl.length())) + "...");
            } else {
                System.err.println("DATABASE_URL environment variable not found");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Connection getDbConnection() {
        return dbConnection;
    }
}