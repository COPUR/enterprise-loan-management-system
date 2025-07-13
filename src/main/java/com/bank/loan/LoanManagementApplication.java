package com.bank.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.bank.loan.config.ApplicationConfiguration;

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
@SpringBootApplication(scanBasePackages = "com.bank.loan.config")
@Import(ApplicationConfiguration.class)
public class LoanManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanManagementApplication.class, args);
    }
}