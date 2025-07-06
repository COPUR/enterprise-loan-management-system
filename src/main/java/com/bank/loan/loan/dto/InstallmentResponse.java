package com.bank.loan.loan.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Installment Response DTO
 * 
 * Response object for loan installment details including
 * amortization schedule information.
 */
public class InstallmentResponse {
    
    private Integer installmentNumber;
    private BigDecimal amount;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal remainingBalance;
    private LocalDate dueDate;
    private String status;
    private LocalDate paidDate;
    private BigDecimal paidAmount;

    // Constructors
    public InstallmentResponse() {}

    public InstallmentResponse(Integer installmentNumber, BigDecimal amount, 
                             BigDecimal principalAmount, BigDecimal interestAmount,
                             BigDecimal remainingBalance, LocalDate dueDate, String status) {
        this.installmentNumber = installmentNumber;
        this.amount = amount;
        this.principalAmount = principalAmount;
        this.interestAmount = interestAmount;
        this.remainingBalance = remainingBalance;
        this.dueDate = dueDate;
        this.status = status;
    }

    // Getters and Setters
    public Integer getInstallmentNumber() {
        return installmentNumber;
    }

    public void setInstallmentNumber(Integer installmentNumber) {
        this.installmentNumber = installmentNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public void setInterestAmount(BigDecimal interestAmount) {
        this.interestAmount = interestAmount;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(LocalDate paidDate) {
        this.paidDate = paidDate;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    @Override
    public String toString() {
        return "InstallmentResponse{" +
                "installmentNumber=" + installmentNumber +
                ", amount=" + amount +
                ", principalAmount=" + principalAmount +
                ", interestAmount=" + interestAmount +
                ", remainingBalance=" + remainingBalance +
                ", dueDate=" + dueDate +
                ", status='" + status + '\'' +
                '}';
    }
}