package com.loanmanagement.infrastructure.web.controller;

import com.loanmanagement.application.dto.*;
import com.loanmanagement.application.usecase.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@SecurityRequirement(name = "basicAuth")
public class LoanController {

    private final CreateLoanUseCase createLoanUseCase;
    private final ListLoansUseCase listLoansUseCase;
    private final ListInstallmentsUseCase listInstallmentsUseCase;
    private final PayLoanUseCase payLoanUseCase;

    public LoanController(
            CreateLoanUseCase createLoanUseCase,
            ListLoansUseCase listLoansUseCase,
            ListInstallmentsUseCase listInstallmentsUseCase,
            PayLoanUseCase payLoanUseCase
    ) {
        this.createLoanUseCase = createLoanUseCase;
        this.listLoansUseCase = listLoansUseCase;
        this.listInstallmentsUseCase = listInstallmentsUseCase;
        this.payLoanUseCase = payLoanUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new loan")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CreateLoanResponse> createLoan(@Valid @RequestBody CreateLoanRequest request) {
        CreateLoanResponse response = createLoanUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List loans for a customer")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and #customerId == authentication.principal.customerId)")
    public ResponseEntity<List<LoanDto>> listLoans(
            @PathVariable Long customerId,
            @RequestParam(required = false) Integer numberOfInstallments,
            @RequestParam(required = false) Boolean isPaid
    ) {
        List<LoanDto> loans = listLoansUseCase.execute(customerId, numberOfInstallments, isPaid);
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/{loanId}/installments")
    @Operation(summary = "List installments for a loan")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and @loanAuthorizationService.isOwner(#loanId, authentication.principal.customerId))")
    public ResponseEntity<List<InstallmentDto>> listInstallments(@PathVariable Long loanId) {
        List<InstallmentDto> installments = listInstallmentsUseCase.execute(loanId);
        return ResponseEntity.ok(installments);
    }

    @PostMapping("/{loanId}/payments")
    @Operation(summary = "Make a payment for a loan")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and @loanAuthorizationService.isOwner(#loanId, authentication.principal.customerId))")
    public ResponseEntity<PayLoanResponse> payLoan(
            @PathVariable Long loanId,
            @Valid @RequestBody PayLoanRequest request
    ) {
        // Ensure the loanId in path matches the one in request
        PayLoanRequest updatedRequest = new PayLoanRequest(loanId, request.amount());
        PayLoanResponse response = payLoanUseCase.execute(updatedRequest);
        return ResponseEntity.ok(response);
    }
}