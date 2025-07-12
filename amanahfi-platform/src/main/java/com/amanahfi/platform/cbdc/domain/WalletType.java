package com.amanahfi.platform.cbdc.domain;

/**
 * Types of Digital Dirham wallets
 */
public enum WalletType {
    CENTRAL_BANK,           // UAE Central Bank wallet (minting/burning)
    COMMERCIAL_BANK,        // Commercial bank wallet
    ISLAMIC_BANK,           // Islamic bank wallet (Sharia compliant)
    PAYMENT_SERVICE,        // Payment service provider wallet
    CORPORATE,              // Corporate wallet
    RETAIL,                 // Individual retail wallet
    GOVERNMENT,             // Government entity wallet
    ESCROW,                 // Escrow/custody wallet
    CROSS_BORDER,          // Cross-border payment wallet
    SMART_CONTRACT         // Smart contract controlled wallet
}