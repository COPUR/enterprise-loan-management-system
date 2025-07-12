package com.amanahfi.platform.cbdc.domain;

/**
 * Status of Digital Dirham wallet
 */
public enum DirhamStatus {
    ACTIVE,         // Wallet is active and can send/receive
    FROZEN,         // Wallet is frozen (regulatory compliance)
    SUSPENDED,      // Wallet is suspended (temporary)
    CLOSED,         // Wallet is permanently closed
    RESTRICTED     // Wallet has restricted functionality
}