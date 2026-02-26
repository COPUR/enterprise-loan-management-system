package com.bank.compliance.infrastructure.web;

import com.bank.compliance.application.ComplianceScreeningService;
import com.bank.compliance.application.dto.ComplianceScreeningRequest;
import com.bank.compliance.application.dto.ComplianceScreeningResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/compliance")
public class ComplianceController {
    private final ComplianceScreeningService service;

    public ComplianceController(ComplianceScreeningService service) {
        this.service = service;
    }

    @PostMapping("/screen")
    public ResponseEntity<ComplianceScreeningResponse> screen(@Valid @RequestBody ComplianceScreeningRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ComplianceScreeningResponse.from(service.screen(request.toCommand())));
    }

    @GetMapping("/screenings/{transactionId}")
    public ResponseEntity<ComplianceScreeningResponse> find(@PathVariable String transactionId) {
        return service.findByTransactionId(transactionId)
                .map(ComplianceScreeningResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
