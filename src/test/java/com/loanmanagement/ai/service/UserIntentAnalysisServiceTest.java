package com.loanmanagement.ai.service;

import com.loanmanagement.ai.domain.model.*;
import com.loanmanagement.ai.application.port.in.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * User Intent Analysis Service Test
 * Tests for user intent recognition and request conversion
 */
@SpringBootTest
class UserIntentAnalysisServiceTest {

    private UserIntentAnalysisService intentAnalysisService;
    private RequestConversionService conversionService;
    private ContextManagementService contextService;
    private PersonalizationService personalizationService;

    @BeforeEach
    void setUp() {
        // These services don't exist yet - tests will fail initially
        intentAnalysisService = new UserIntentAnalysisService();
        conversionService = new RequestConversionService();
        contextService = new ContextManagementService();
        personalizationService = new PersonalizationService();
    }

    @Test
    @DisplayName("Should identify loan application intent from natural language")
    void shouldIdentifyLoanApplicationIntentFromNaturalLanguage() {
        List<String> loanApplicationInputs = List.of(
            "I want to apply for a loan",
            "Can I get a personal loan?",
            "How do I start a mortgage application?",
            "I'm interested in borrowing money for a car",
            "Need financing for my business"
        );

        for (String input : loanApplicationInputs) {
            UserIntentRequest request = UserIntentRequest.builder()
                .userInput(input)
                .sessionId("session-" + System.currentTimeMillis())
                .timestamp(LocalDateTime.now())
                .build();

            UserIntentResult result = intentAnalysisService.analyzeIntent(request);

            assertThat(result.getPrimaryIntent()).isEqualTo(UserIntent.LOAN_APPLICATION);
            assertThat(result.getConfidenceScore()).isGreaterThan(0.8);
            assertThat(result.getIntentCategory()).isEqualTo(IntentCategory.TRANSACTION);
        }
    }

    @Test
    @DisplayName("Should identify information seeking intents")
    void shouldIdentifyInformationSeekingIntents() {
        Map<String, UserIntent> informationQueries = Map.of(
            "What are your interest rates?", UserIntent.RATE_INQUIRY,
            "What documents do I need?", UserIntent.DOCUMENT_REQUIREMENTS,
            "How long does approval take?", UserIntent.PROCESS_TIMELINE,
            "What's my current loan balance?", UserIntent.BALANCE_INQUIRY,
            "Can you explain the loan terms?", UserIntent.TERMS_EXPLANATION
        );

        for (Map.Entry<String, UserIntent> query : informationQueries.entrySet()) {
            UserIntentRequest request = UserIntentRequest.builder()
                .userInput(query.getKey())
                .build();

            UserIntentResult result = intentAnalysisService.analyzeIntent(request);

            assertThat(result.getPrimaryIntent()).isEqualTo(query.getValue());
            assertThat(result.getIntentCategory()).isEqualTo(IntentCategory.INFORMATION_SEEKING);
        }
    }

    @Test
    @DisplayName("Should detect customer service intents")
    void shouldDetectCustomerServiceIntents() {
        Map<String, UserIntent> serviceRequests = Map.of(
            "I need to speak with someone", UserIntent.HUMAN_AGENT_REQUEST,
            "There's an error in my application", UserIntent.ISSUE_REPORTING,
            "I want to cancel my loan", UserIntent.CANCELLATION_REQUEST,
            "Can you update my information?", UserIntent.ACCOUNT_UPDATE,
            "I'm not happy with the service", UserIntent.COMPLAINT
        );

        for (Map.Entry<String, UserIntent> request : serviceRequests.entrySet()) {
            UserIntentRequest intentRequest = UserIntentRequest.builder()
                .userInput(request.getKey())
                .build();

            UserIntentResult result = intentAnalysisService.analyzeIntent(intentRequest);

            assertThat(result.getPrimaryIntent()).isEqualTo(request.getValue());
            assertThat(result.getIntentCategory()).isEqualTo(IntentCategory.CUSTOMER_SERVICE);
        }
    }

    @Test
    @DisplayName("Should handle complex multi-intent queries")
    void shouldHandleComplexMultiIntentQueries() {
        String complexQuery = "I want to apply for a $50,000 personal loan and also check my credit score. What are the interest rates and how long will approval take?";
        
        UserIntentRequest request = UserIntentRequest.builder()
            .userInput(complexQuery)
            .enableMultiIntentDetection(true)
            .build();

        UserIntentResult result = intentAnalysisService.analyzeIntent(request);

        // Should detect multiple intents
        assertThat(result.getPrimaryIntent()).isEqualTo(UserIntent.LOAN_APPLICATION);
        assertThat(result.getSecondaryIntents()).containsExactlyInAnyOrder(
            UserIntent.CREDIT_SCORE_CHECK,
            UserIntent.RATE_INQUIRY,
            UserIntent.PROCESS_TIMELINE
        );
        assertThat(result.getIntentHierarchy()).isNotEmpty();
    }

    @Test
    @DisplayName("Should convert user intent to structured API requests")
    void shouldConvertUserIntentToStructuredAPIRequests() {
        String userInput = "I need a $25,000 car loan with the lowest interest rate available";
        
        UserIntentRequest intentRequest = UserIntentRequest.builder()
            .userInput(userInput)
            .customerId("CUST-001")
            .build();

        UserIntentResult intentResult = intentAnalysisService.analyzeIntent(intentRequest);
        
        // Convert to API requests
        RequestConversionInput conversionInput = RequestConversionInput.builder()
            .intentResult(intentResult)
            .userInput(userInput)
            .customerId("CUST-001")
            .build();

        List<StructuredAPIRequest> apiRequests = conversionService.convertToAPIRequests(conversionInput);

        assertThat(apiRequests).hasSize(2);
        
        // Should generate rate inquiry request
        StructuredAPIRequest rateRequest = apiRequests.stream()
            .filter(req -> req.getEndpoint().equals("/api/v1/loan-products/rates"))
            .findFirst().orElse(null);
        assertThat(rateRequest).isNotNull();
        assertThat(rateRequest.getMethod()).isEqualTo("GET");
        assertThat(rateRequest.getParameters()).containsKey("loanType");
        assertThat(rateRequest.getParameters().get("loanType")).isEqualTo("AUTO");
        
        // Should generate loan application request
        StructuredAPIRequest applicationRequest = apiRequests.stream()
            .filter(req -> req.getEndpoint().equals("/api/v1/loans/applications"))
            .findFirst().orElse(null);
        assertThat(applicationRequest).isNotNull();
        assertThat(applicationRequest.getMethod()).isEqualTo("POST");
        assertThat(applicationRequest.getBody()).containsKey("requestedAmount");
        assertThat(applicationRequest.getBody().get("requestedAmount")).isEqualTo(25000);
    }

    @Test
    @DisplayName("Should maintain conversation context across interactions")
    void shouldMaintainConversationContextAcrossInteractions() {
        String sessionId = "session-context-test";
        
        // First interaction - establish context
        UserIntentRequest firstRequest = UserIntentRequest.builder()
            .userInput("I'm looking for a mortgage")
            .sessionId(sessionId)
            .customerId("CUST-002")
            .build();

        UserIntentResult firstResult = intentAnalysisService.analyzeIntent(firstRequest);
        
        // Update context
        ConversationContext context = contextService.updateContext(sessionId, firstResult);
        assertThat(context.getCurrentTopic()).isEqualTo(ConversationTopic.MORTGAGE_APPLICATION);
        
        // Second interaction - should use context
        UserIntentRequest secondRequest = UserIntentRequest.builder()
            .userInput("For $400,000")
            .sessionId(sessionId)
            .conversationContext(context)
            .build();

        UserIntentResult secondResult = intentAnalysisService.analyzeIntent(secondRequest);
        
        // Should understand amount refers to mortgage
        assertThat(secondResult.getPrimaryIntent()).isEqualTo(UserIntent.LOAN_APPLICATION);
        assertThat(secondResult.getContextualInformation()).containsKey("mortgageAmount");
        assertThat(secondResult.getContextualInformation().get("mortgageAmount")).isEqualTo(400000);
    }

    @Test
    @DisplayName("Should provide personalized intent analysis")
    void shouldProvidePersonalizedIntentAnalysis() {
        // Create customer profile
        CustomerProfile profile = CustomerProfile.builder()
            .customerId("CUST-003")
            .preferredLoanTypes(List.of(LoanType.MORTGAGE, LoanType.HOME_EQUITY))
            .creditTier(CreditTier.EXCELLENT)
            .previousInteractions(List.of(
                PreviousInteraction.builder()
                    .intent(UserIntent.RATE_INQUIRY)
                    .loanType(LoanType.MORTGAGE)
                    .timestamp(LocalDateTime.now().minusDays(7))
                    .build()
            ))
            .build();

        String userInput = "I want to borrow against my home";
        
        PersonalizedIntentRequest request = PersonalizedIntentRequest.builder()
            .userInput(userInput)
            .customerProfile(profile)
            .enablePersonalization(true)
            .build();

        PersonalizedIntentResult result = personalizationService.analyzePersonalizedIntent(request);

        // Should prefer home equity based on profile
        assertThat(result.getPrimaryIntent()).isEqualTo(UserIntent.LOAN_APPLICATION);
        assertThat(result.getSuggestedLoanType()).isEqualTo(LoanType.HOME_EQUITY);
        assertThat(result.getPersonalizationFactors()).contains("previous_mortgage_interest");
        assertThat(result.getConfidenceBoost()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Should detect urgent or high-priority intents")
    void shouldDetectUrgentOrHighPriorityIntents() {
        Map<String, IntentPriority> urgentIntents = Map.of(
            "My loan payment is due today and I can't access my account", IntentPriority.URGENT,
            "There's been fraud on my account", IntentPriority.CRITICAL,
            "I'm facing foreclosure next week", IntentPriority.URGENT,
            "General question about rates", IntentPriority.NORMAL,
            "When will my application be processed?", IntentPriority.HIGH
        );

        for (Map.Entry<String, IntentPriority> test : urgentIntents.entrySet()) {
            UserIntentRequest request = UserIntentRequest.builder()
                .userInput(test.getKey())
                .enablePriorityDetection(true)
                .build();

            UserIntentResult result = intentAnalysisService.analyzeIntent(request);

            assertThat(result.getPriority()).isEqualTo(test.getValue());
            if (test.getValue() == IntentPriority.URGENT || test.getValue() == IntentPriority.CRITICAL) {
                assertThat(result.getRecommendedAction()).isEqualTo(RecommendedAction.IMMEDIATE_HUMAN_HANDOFF);
            }
        }
    }

    @Test
    @DisplayName("Should generate appropriate follow-up questions")
    void shouldGenerateAppropriateFollowUpQuestions() {
        String userInput = "I want a loan";
        
        UserIntentRequest request = UserIntentRequest.builder()
            .userInput(userInput)
            .generateFollowUps(true)
            .build();

        UserIntentResult result = intentAnalysisService.analyzeIntent(request);

        // Should generate clarifying questions
        assertThat(result.getFollowUpQuestions()).isNotEmpty();
        assertThat(result.getFollowUpQuestions()).anyMatch(q -> 
            q.toLowerCase().contains("type") && q.toLowerCase().contains("loan"));
        assertThat(result.getFollowUpQuestions()).anyMatch(q -> 
            q.toLowerCase().contains("amount"));
        assertThat(result.getFollowUpQuestions()).anyMatch(q -> 
            q.toLowerCase().contains("purpose"));
    }

    @Test
    @DisplayName("Should handle ambiguous inputs gracefully")
    void shouldHandleAmbiguousInputsGracefully() {
        List<String> ambiguousInputs = List.of(
            "help",
            "yes",
            "what?",
            "loan thing",
            "I don't know"
        );

        for (String input : ambiguousInputs) {
            UserIntentRequest request = UserIntentRequest.builder()
                .userInput(input)
                .build();

            UserIntentResult result = intentAnalysisService.analyzeIntent(request);

            assertThat(result.getPrimaryIntent()).isEqualTo(UserIntent.CLARIFICATION_NEEDED);
            assertThat(result.getConfidenceScore()).isLessThan(0.5);
            assertThat(result.getClarificationQuestions()).isNotEmpty();
            assertThat(result.getRecommendedAction()).isEqualTo(RecommendedAction.REQUEST_CLARIFICATION);
        }
    }

    @Test
    @DisplayName("Should support batch intent analysis")
    void shouldSupportBatchIntentAnalysis() {
        List<String> userInputs = List.of(
            "I need a personal loan",
            "What are your mortgage rates?",
            "How do I check my application status?",
            "I want to make a payment",
            "Can I speak to a representative?"
        );

        BatchIntentAnalysisRequest batchRequest = BatchIntentAnalysisRequest.builder()
            .userInputs(userInputs)
            .sessionId("batch-session")
            .customerId("CUST-004")
            .build();

        BatchIntentAnalysisResult batchResult = intentAnalysisService.analyzeBatch(batchRequest);

        assertThat(batchResult.getResults()).hasSize(5);
        assertThat(batchResult.getProcessingTimeMs()).isGreaterThan(0L);
        
        // Verify individual results
        assertThat(batchResult.getResults().get(0).getPrimaryIntent()).isEqualTo(UserIntent.LOAN_APPLICATION);
        assertThat(batchResult.getResults().get(1).getPrimaryIntent()).isEqualTo(UserIntent.RATE_INQUIRY);
        assertThat(batchResult.getResults().get(2).getPrimaryIntent()).isEqualTo(UserIntent.STATUS_CHECK);
        assertThat(batchResult.getResults().get(3).getPrimaryIntent()).isEqualTo(UserIntent.PAYMENT);
        assertThat(batchResult.getResults().get(4).getPrimaryIntent()).isEqualTo(UserIntent.HUMAN_AGENT_REQUEST);
    }

    @Test
    @DisplayName("Should learn from user feedback")
    void shouldLearnFromUserFeedback() {
        String userInput = "I want to refinance";
        
        UserIntentRequest request = UserIntentRequest.builder()
            .userInput(userInput)
            .build();

        UserIntentResult result = intentAnalysisService.analyzeIntent(request);
        
        // Simulate user feedback that the intent was wrong
        IntentFeedback feedback = IntentFeedback.builder()
            .originalInput(userInput)
            .predictedIntent(result.getPrimaryIntent())
            .correctIntent(UserIntent.LOAN_REFINANCING)
            .confidence(result.getConfidenceScore())
            .sessionId(request.getSessionId())
            .customerId(request.getCustomerId())
            .build();

        // Submit feedback for learning
        intentAnalysisService.submitFeedback(feedback);
        
        // Re-analyze the same input
        UserIntentResult improvedResult = intentAnalysisService.analyzeIntent(request);
        
        // Should show improvement
        assertThat(improvedResult.getPrimaryIntent()).isEqualTo(UserIntent.LOAN_REFINANCING);
        assertThat(improvedResult.getConfidenceScore()).isGreaterThan(result.getConfidenceScore());
    }
}