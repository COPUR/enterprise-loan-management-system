package com.bank.loanmanagement.infrastructure.caching;

import com.bank.loanmanagement.config.RedisConfig;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class RedisConnectionManager {
    
    private static final AtomicBoolean isConnected = new AtomicBoolean(false);
    private static final AtomicLong connectionCount = new AtomicLong(0);
    private static final AtomicLong operationCount = new AtomicLong(0);
    private static final ExecutorService connectionPool = Executors.newFixedThreadPool(
        RedisConfig.ConnectionConfig.MAX_CONNECTIONS
    );
    
    private static Socket redisSocket;
    private static PrintWriter writer;
    private static BufferedReader reader;
    
    // Connection management
    public static synchronized boolean connect() {
        try {
            if (isConnected.get()) {
                return true;
            }
            
            redisSocket = new Socket();
            redisSocket.connect(
                new InetSocketAddress(
                    RedisConfig.ConnectionConfig.REDIS_HOST,
                    RedisConfig.ConnectionConfig.REDIS_PORT
                ),
                RedisConfig.ConnectionConfig.CONNECTION_TIMEOUT
            );
            
            redisSocket.setSoTimeout(RedisConfig.ConnectionConfig.READ_TIMEOUT);
            
            writer = new PrintWriter(redisSocket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(redisSocket.getInputStream()));
            
            // Authenticate if password is provided
            if (RedisConfig.ConnectionConfig.REDIS_PASSWORD != null) {
                authenticate(RedisConfig.ConnectionConfig.REDIS_PASSWORD);
            }
            
            // Select database
            selectDatabase(RedisConfig.ConnectionConfig.DATABASE_INDEX);
            
            isConnected.set(true);
            connectionCount.incrementAndGet();
            
            System.out.println("Redis connection established: " + 
                RedisConfig.ConnectionConfig.REDIS_HOST + ":" + 
                RedisConfig.ConnectionConfig.REDIS_PORT);
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Failed to connect to Redis: " + e.getMessage());
            isConnected.set(false);
            return false;
        }
    }
    
    private static void authenticate(String password) throws IOException {
        sendCommand("AUTH", password);
        String response = readResponse();
        if (!"+OK".equals(response)) {
            throw new IOException("Authentication failed: " + response);
        }
    }
    
    private static void selectDatabase(int dbIndex) throws IOException {
        sendCommand("SELECT", String.valueOf(dbIndex));
        String response = readResponse();
        if (!"+OK".equals(response)) {
            throw new IOException("Database selection failed: " + response);
        }
    }
    
    // Core Redis operations
    public static synchronized String set(String key, String value, long ttlSeconds) {
        if (!ensureConnection()) return null;
        
        try {
            if (ttlSeconds > 0) {
                sendCommand("SETEX", key, String.valueOf(ttlSeconds), value);
            } else {
                sendCommand("SET", key, value);
            }
            
            String response = readResponse();
            operationCount.incrementAndGet();
            return response.startsWith("+OK") ? "OK" : null;
            
        } catch (IOException e) {
            handleConnectionError(e);
            return null;
        }
    }
    
    public static synchronized String get(String key) {
        if (!ensureConnection()) return null;
        
        try {
            sendCommand("GET", key);
            String response = readResponse();
            operationCount.incrementAndGet();
            
            if (response.startsWith("$-1")) {
                return null; // Key not found
            } else if (response.startsWith("$")) {
                return readBulkString(response);
            }
            
            return response;
            
        } catch (IOException e) {
            handleConnectionError(e);
            return null;
        }
    }
    
    public static synchronized boolean exists(String key) {
        if (!ensureConnection()) return false;
        
        try {
            sendCommand("EXISTS", key);
            String response = readResponse();
            operationCount.incrementAndGet();
            
            return response.startsWith(":1");
            
        } catch (IOException e) {
            handleConnectionError(e);
            return false;
        }
    }
    
    public static synchronized boolean delete(String key) {
        if (!ensureConnection()) return false;
        
        try {
            sendCommand("DEL", key);
            String response = readResponse();
            operationCount.incrementAndGet();
            
            return response.startsWith(":1");
            
        } catch (IOException e) {
            handleConnectionError(e);
            return false;
        }
    }
    
    public static synchronized boolean expire(String key, long ttlSeconds) {
        if (!ensureConnection()) return false;
        
        try {
            sendCommand("EXPIRE", key, String.valueOf(ttlSeconds));
            String response = readResponse();
            operationCount.incrementAndGet();
            
            return response.startsWith(":1");
            
        } catch (IOException e) {
            handleConnectionError(e);
            return false;
        }
    }
    
    public static synchronized long ttl(String key) {
        if (!ensureConnection()) return -1;
        
        try {
            sendCommand("TTL", key);
            String response = readResponse();
            operationCount.incrementAndGet();
            
            if (response.startsWith(":")) {
                return Long.parseLong(response.substring(1));
            }
            
            return -1;
            
        } catch (IOException | NumberFormatException e) {
            handleConnectionError(e);
            return -1;
        }
    }
    
    // Hash operations for complex objects
    public static synchronized boolean hset(String key, String field, String value) {
        if (!ensureConnection()) return false;
        
        try {
            sendCommand("HSET", key, field, value);
            String response = readResponse();
            operationCount.incrementAndGet();
            
            return response.startsWith(":1") || response.startsWith(":0");
            
        } catch (IOException e) {
            handleConnectionError(e);
            return false;
        }
    }
    
    public static synchronized String hget(String key, String field) {
        if (!ensureConnection()) return null;
        
        try {
            sendCommand("HGET", key, field);
            String response = readResponse();
            operationCount.incrementAndGet();
            
            if (response.startsWith("$-1")) {
                return null;
            } else if (response.startsWith("$")) {
                return readBulkString(response);
            }
            
            return response;
            
        } catch (IOException e) {
            handleConnectionError(e);
            return null;
        }
    }
    
    // List operations for sequences
    public static synchronized boolean lpush(String key, String value) {
        if (!ensureConnection()) return false;
        
        try {
            sendCommand("LPUSH", key, value);
            String response = readResponse();
            operationCount.incrementAndGet();
            
            return response.startsWith(":");
            
        } catch (IOException e) {
            handleConnectionError(e);
            return false;
        }
    }
    
    public static synchronized String lpop(String key) {
        if (!ensureConnection()) return null;
        
        try {
            sendCommand("LPOP", key);
            String response = readResponse();
            operationCount.incrementAndGet();
            
            if (response.startsWith("$-1")) {
                return null;
            } else if (response.startsWith("$")) {
                return readBulkString(response);
            }
            
            return response;
            
        } catch (IOException e) {
            handleConnectionError(e);
            return null;
        }
    }
    
    // Increment operations for counters
    public static synchronized long incr(String key) {
        if (!ensureConnection()) return -1;
        
        try {
            sendCommand("INCR", key);
            String response = readResponse();
            operationCount.incrementAndGet();
            
            if (response.startsWith(":")) {
                return Long.parseLong(response.substring(1));
            }
            
            return -1;
            
        } catch (IOException | NumberFormatException e) {
            handleConnectionError(e);
            return -1;
        }
    }
    
    // Connection utility methods
    private static boolean ensureConnection() {
        if (!isConnected.get()) {
            return connect();
        }
        return true;
    }
    
    private static void sendCommand(String... args) throws IOException {
        StringBuilder command = new StringBuilder();
        command.append("*").append(args.length).append("\r\n");
        
        for (String arg : args) {
            command.append("$").append(arg.length()).append("\r\n");
            command.append(arg).append("\r\n");
        }
        
        writer.print(command.toString());
        writer.flush();
    }
    
    private static String readResponse() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Connection closed");
        }
        return line;
    }
    
    private static String readBulkString(String lengthLine) throws IOException {
        int length = Integer.parseInt(lengthLine.substring(1));
        if (length == -1) return null;
        
        char[] buffer = new char[length];
        reader.read(buffer, 0, length);
        reader.readLine(); // Read trailing CRLF
        
        return new String(buffer);
    }
    
    private static void handleConnectionError(Exception e) {
        System.err.println("Redis operation failed: " + e.getMessage());
        isConnected.set(false);
        
        // Attempt reconnection
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000); // Wait 1 second before retry
                connect();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    // Health check and metrics
    public static synchronized boolean ping() {
        if (!ensureConnection()) return false;
        
        try {
            sendCommand("PING");
            String response = readResponse();
            return "+PONG".equals(response);
            
        } catch (IOException e) {
            handleConnectionError(e);
            return false;
        }
    }
    
    public static boolean isConnected() {
        return isConnected.get();
    }
    
    public static long getConnectionCount() {
        return connectionCount.get();
    }
    
    public static long getOperationCount() {
        return operationCount.get();
    }
    
    // Graceful shutdown
    public static synchronized void disconnect() {
        try {
            if (isConnected.get() && redisSocket != null) {
                sendCommand("QUIT");
                readResponse();
                
                reader.close();
                writer.close();
                redisSocket.close();
                
                isConnected.set(false);
                System.out.println("Redis connection closed gracefully");
            }
        } catch (IOException e) {
            System.err.println("Error during Redis disconnect: " + e.getMessage());
        } finally {
            connectionPool.shutdown();
        }
    }
}