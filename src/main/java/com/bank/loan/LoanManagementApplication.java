package com.bank.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Enterprise Banking System - Main Application
 * 
 * This is the main entry point for the Enterprise Loan Management System.
 * It provides comprehensive loan origination, payment processing, and 
 * Islamic finance capabilities for Tier 1 banking institutions.
 * 
 * Features:
 * - Loan Origination (Personal, Business, Mortgage, Vehicle)
 * - Payment Processing with early/late payment handling
 * - Islamic Finance (Murabaha, Ijarah, Musharakah)
 * - SAGA Workflow Management
 * - Regulatory Compliance (AML, GDPR, PCI DSS)
 * - Real-time Integration (Credit Bureau, Core Banking, Payment Gateway)
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
@EnableCaching
public class LoanManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanManagementApplication.class, args);
    }
}