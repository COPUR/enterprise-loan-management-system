
package com.bank.loanmanagement.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static Connection dbConnection;
    
    public static void initializeDatabase() {
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
    
    public static Connection getConnection() {
        return dbConnection;
    }
    
    public static boolean isConnected() {
        return dbConnection != null;
    }
}
