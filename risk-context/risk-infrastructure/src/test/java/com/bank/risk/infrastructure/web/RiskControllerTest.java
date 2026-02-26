package com.bank.risk.infrastructure.web;

import com.bank.risk.application.RiskAssessmentService;
import com.bank.risk.domain.RiskAssessment;
import com.bank.risk.domain.RiskDecision;
import com.bank.risk.domain.command.RiskEvaluationCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RiskControllerTest {

    private RiskAssessmentService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(RiskAssessmentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RiskController(service)).build();
    }

    @Test
    void shouldAssessRisk() throws Exception {
        RiskAssessment assessment = RiskAssessment.create("TX-1", new BigDecimal("200"), "AED", 60, RiskDecision.REVIEW, List.of("HIGH_AMOUNT"));
        when(service.assess(any(RiskEvaluationCommand.class))).thenReturn(assessment);

        mockMvc.perform(post("/api/v1/risk/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"transactionId":"TX-1","amount":200,"currency":"AED","highRiskCountry":false,"velocityScore":45}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("TX-1"))
                .andExpect(jsonPath("$.decision").value("REVIEW"));
    }

    @Test
    void shouldReturnAssessmentByTransactionId() throws Exception {
        RiskAssessment assessment = RiskAssessment.create("TX-2", new BigDecimal("100"), "AED", 10, RiskDecision.ALLOW, List.of("COMPLIANT"));
        when(service.findByTransactionId("TX-2")).thenReturn(Optional.of(assessment));
        when(service.findByTransactionId("TX-404")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/risk/assessments/TX-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TX-2"));

        mockMvc.perform(get("/api/v1/risk/assessments/TX-404"))
                .andExpect(status().isNotFound());
    }
}
