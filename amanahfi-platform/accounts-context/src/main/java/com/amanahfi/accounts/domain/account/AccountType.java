package com.amanahfi.accounts.domain.account;

/**
 * Account types supported by AmanahFi platform
 * Following Islamic banking and modern digital asset requirements
 */
public enum AccountType {
    /**
     * Current account for daily transactions
     */
    CURRENT,
    
    /**
     * Savings account with profit-sharing (Islamic) or interest (conventional)
     */
    SAVINGS,
    
    /**
     * Digital wallet for UAE Central Bank Digital Currency (CBDC)
     */
    CBDC_WALLET,
    
    /**
     * Digital wallet for stablecoins (USDC, USDT, etc.)
     */
    STABLECOIN_WALLET,
    
    /**
     * Investment account for Murabaha and other Islamic products
     */
    INVESTMENT,
    
    /**
     * Business account for corporate customers
     */
    BUSINESS
}