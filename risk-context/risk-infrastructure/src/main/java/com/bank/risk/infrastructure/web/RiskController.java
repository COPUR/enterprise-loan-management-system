package com.bank.risk.infrastructure.web;

import com.bank.risk.application.RiskAssessmentService;
import com.bank.risk.application.dto.EvaluateRiskRequest;
import com.bank.risk.application.dto.RiskAssessmentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/risk")
public class RiskController {
    private final RiskAssessmentService service;

    public RiskController(RiskAssessmentService service) {
        this.service = service;
    }

    @PostMapping("/assess")
    public ResponseEntity<RiskAssessmentResponse> assess(@Valid @RequestBody EvaluateRiskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RiskAssessmentResponse.from(service.assess(request.toCommand())));
    }

    @GetMapping("/assessments/{transactionId}")
    public ResponseEntity<RiskAssessmentResponse> find(@PathVariable String transactionId) {
        return service.findByTransactionId(transactionId)
                .map(RiskAssessmentResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
