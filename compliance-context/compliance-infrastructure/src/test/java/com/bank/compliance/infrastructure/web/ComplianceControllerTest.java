package com.bank.compliance.infrastructure.web;

import com.bank.compliance.application.ComplianceScreeningService;
import com.bank.compliance.domain.ComplianceDecision;
import com.bank.compliance.domain.ComplianceResult;
import com.bank.compliance.domain.command.ComplianceScreeningCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ComplianceControllerTest {

    private ComplianceScreeningService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ComplianceScreeningService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ComplianceController(service)).build();
    }

    @Test
    void shouldScreenCompliance() throws Exception {
        ComplianceResult result = ComplianceResult.create("TX-1", "C1", ComplianceDecision.REVIEW, List.of("PEP_HIGH_VALUE_REVIEW"));
        when(service.screen(any(ComplianceScreeningCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/compliance/screen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"transactionId":"TX-1","customerId":"C1","amount":12000,"sanctionsHit":false,"kycVerified":true,"pep":true}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("TX-1"))
                .andExpect(jsonPath("$.decision").value("REVIEW"));
    }

    @Test
    void shouldFindComplianceByTransactionId() throws Exception {
        ComplianceResult result = ComplianceResult.create("TX-2", "C1", ComplianceDecision.PASS, List.of("COMPLIANT"));
        when(service.findByTransactionId("TX-2")).thenReturn(Optional.of(result));
        when(service.findByTransactionId("TX-404")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/compliance/screenings/TX-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TX-2"));

        mockMvc.perform(get("/api/v1/compliance/screenings/TX-404"))
                .andExpect(status().isNotFound());
    }
}
