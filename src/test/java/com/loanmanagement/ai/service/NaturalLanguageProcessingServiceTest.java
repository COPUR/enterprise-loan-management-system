package com.loanmanagement.ai.service;

import com.loanmanagement.ai.domain.model.*;
import com.loanmanagement.ai.application.port.in.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Natural Language Processing Service Test
 * Tests for NLP capabilities in loan processing
 */
@SpringBootTest
class NaturalLanguageProcessingServiceTest {

    private NaturalLanguageProcessingService nlpService;
    private IntentAnalysisService intentService;
    private EntityExtractionService entityService;
    private SentimentAnalysisService sentimentService;
    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        // These services don't exist yet - tests will fail initially
        nlpService = new NaturalLanguageProcessingService();
        intentService = new IntentAnalysisService();
        entityService = new EntityExtractionService();
        sentimentService = new SentimentAnalysisService();
        conversationService = new ConversationService();
    }

    @Test
    @DisplayName("Should process natural language loan inquiry")
    void shouldProcessNaturalLanguageLoanInquiry() {
        // Natural language input
        String userInput = "I need a $50,000 loan for buying a house. I make $75,000 per year and have good credit.";
        
        // Process natural language
        NLPProcessingRequest request = NLPProcessingRequest.builder()
            .text(userInput)
            .sessionId("session-123")
            .customerId("CUST-001")
            .timestamp(LocalDateTime.now())
            .build();

        NLPProcessingResult result = nlpService.processText(request);

        // Verify NLP processing
        assertThat(result).isNotNull();
        assertThat(result.getIntent()).isEqualTo(Intent.LOAN_APPLICATION);
        assertThat(result.getEntities()).isNotEmpty();
        assertThat(result.getExtractedAmount()).isEqualTo(BigDecimal.valueOf(50000));
        assertThat(result.getExtractedIncome()).isEqualTo(BigDecimal.valueOf(75000));
        assertThat(result.getLoanPurpose()).isEqualTo(LoanPurpose.HOME_PURCHASE);
        assertThat(result.getConfidence()).isGreaterThan(0.8);
    }

    @Test
    @DisplayName("Should analyze user intent accurately")
    void shouldAnalyzeUserIntentAccurately() {
        // Test various intents
        Map<String, Intent> testCases = Map.of(
            "I want to apply for a personal loan", Intent.LOAN_APPLICATION,
            "What's my loan balance?", Intent.BALANCE_INQUIRY,
            "I'd like to make a payment", Intent.PAYMENT,
            "Can I get pre-approved for a mortgage?", Intent.PRE_APPROVAL,
            "What interest rates do you offer?", Intent.RATE_INQUIRY,
            "I want to speak to a loan officer", Intent.HUMAN_HANDOFF
        );

        for (Map.Entry<String, Intent> testCase : testCases.entrySet()) {
            IntentAnalysisRequest request = IntentAnalysisRequest.builder()
                .text(testCase.getKey())
                .context(ConversationContext.LOAN_INQUIRY)
                .build();

            IntentAnalysisResult result = intentService.analyzeIntent(request);

            assertThat(result.getPrimaryIntent()).isEqualTo(testCase.getValue());
            assertThat(result.getConfidence()).isGreaterThan(0.7);
        }
    }

    @Test
    @DisplayName("Should extract financial entities from text")
    void shouldExtractFinancialEntitiesFromText() {
        String text = "I'm looking for a $25,000 auto loan with a 5-year term. My credit score is 720 and I earn $60,000 annually.";
        
        EntityExtractionRequest request = EntityExtractionRequest.builder()
            .text(text)
            .entityTypes(List.of(EntityType.AMOUNT, EntityType.LOAN_TYPE, EntityType.TERM, EntityType.CREDIT_SCORE, EntityType.INCOME))
            .build();

        EntityExtractionResult result = entityService.extractEntities(request);

        // Verify extracted entities
        assertThat(result.getEntities()).hasSize(5);
        
        FinancialEntity amountEntity = result.getEntityByType(EntityType.AMOUNT);
        assertThat(amountEntity.getValue()).isEqualTo("25000");
        assertThat(amountEntity.getNormalizedValue()).isEqualTo(BigDecimal.valueOf(25000));
        
        FinancialEntity loanTypeEntity = result.getEntityByType(EntityType.LOAN_TYPE);
        assertThat(loanTypeEntity.getValue()).isEqualTo("auto loan");
        assertThat(loanTypeEntity.getNormalizedValue()).isEqualTo(LoanType.AUTO);
        
        FinancialEntity termEntity = result.getEntityByType(EntityType.TERM);
        assertThat(termEntity.getValue()).isEqualTo("5-year");
        assertThat(termEntity.getNormalizedValue()).isEqualTo(60); // months
    }

    @Test
    @DisplayName("Should analyze customer sentiment")
    void shouldAnalyzeCustomerSentiment() {
        Map<String, Sentiment> sentimentTests = Map.of(
            "I'm very happy with your service!", Sentiment.POSITIVE,
            "Your loan process is terrible and slow", Sentiment.NEGATIVE,
            "The application is okay, nothing special", Sentiment.NEUTRAL,
            "I'm frustrated with the delays", Sentiment.NEGATIVE,
            "Thank you for the quick approval!", Sentiment.POSITIVE
        );

        for (Map.Entry<String, Sentiment> test : sentimentTests.entrySet()) {
            SentimentAnalysisRequest request = SentimentAnalysisRequest.builder()
                .text(test.getKey())
                .context(ConversationContext.CUSTOMER_SERVICE)
                .build();

            SentimentAnalysisResult result = sentimentService.analyzeSentiment(request);

            assertThat(result.getSentiment()).isEqualTo(test.getValue());
            assertThat(result.getConfidence()).isGreaterThan(0.6);
            assertThat(result.getEmotionalIndicators()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("Should handle multi-turn conversations")
    void shouldHandleMultiTurnConversations() {
        String sessionId = "session-456";
        
        // First turn
        ConversationTurn turn1 = ConversationTurn.builder()
            .sessionId(sessionId)
            .userInput("I need a loan")
            .timestamp(LocalDateTime.now())
            .build();
        
        ConversationResponse response1 = conversationService.processTurn(turn1);
        assertThat(response1.getSystemResponse()).contains("What type of loan");
        assertThat(response1.getContextUpdates()).containsKey("awaiting_loan_type");
        
        // Second turn
        ConversationTurn turn2 = ConversationTurn.builder()
            .sessionId(sessionId)
            .userInput("Personal loan for $15,000")
            .timestamp(LocalDateTime.now())
            .build();
        
        ConversationResponse response2 = conversationService.processTurn(turn2);
        assertThat(response2.getSystemResponse()).contains("$15,000 personal loan");
        assertThat(response2.getExtractedData()).containsKey("loan_amount");
        assertThat(response2.getExtractedData()).containsKey("loan_type");
    }

    @Test
    @DisplayName("Should convert natural language to structured loan application")
    void shouldConvertNaturalLanguageToStructuredLoanApplication() {
        String conversationTranscript = """
            User: Hi, I'd like to apply for a mortgage
            System: Great! I can help you with that. What's the loan amount you're looking for?
            User: I need $300,000 for buying a house in California
            System: And what's your annual income?
            User: I make $120,000 per year as a software engineer
            System: How long have you been employed?
            User: 3 years at my current job
            System: What's your credit score?
            User: Last time I checked it was around 750
            """;
        
        ConversationToApplicationRequest request = ConversationToApplicationRequest.builder()
            .conversationTranscript(conversationTranscript)
            .sessionId("session-789")
            .customerId("CUST-002")
            .build();

        StructuredLoanApplication application = nlpService.convertConversationToApplication(request);

        // Verify structured application
        assertThat(application).isNotNull();
        assertThat(application.getLoanAmount()).isEqualTo(BigDecimal.valueOf(300000));
        assertThat(application.getLoanPurpose()).isEqualTo(LoanPurpose.HOME_PURCHASE);
        assertThat(application.getApplicantIncome()).isEqualTo(BigDecimal.valueOf(120000));
        assertThat(application.getEmploymentType()).isEqualTo(EmploymentType.FULL_TIME);
        assertThat(application.getCreditScore()).isEqualTo(750);
        assertThat(application.getEmploymentLength()).isEqualTo(36); // months
        assertThat(application.getPropertyState()).isEqualTo("California");
    }

    @Test
    @DisplayName("Should provide loan recommendations based on conversation")
    void shouldProvideLoanRecommendationsBasedOnConversation() {
        String userQuery = "I'm a first-time homebuyer with $80k income, $15k down payment, looking for something around $250k";
        
        RecommendationRequest request = RecommendationRequest.builder()
            .userQuery(userQuery)
            .customerId("CUST-003")
            .includeEducationalContent(true)
            .build();

        LoanRecommendationResponse response = nlpService.generateRecommendations(request);

        // Verify recommendations
        assertThat(response.getRecommendations()).isNotEmpty();
        assertThat(response.getEducationalContent()).isNotEmpty();
        assertThat(response.getNextSteps()).isNotEmpty();
        
        LoanProductRecommendation topRecommendation = response.getRecommendations().get(0);
        assertThat(topRecommendation.getProductType()).isEqualTo(LoanType.CONVENTIONAL_MORTGAGE);
        assertThat(topRecommendation.getReasoningExplanation()).contains("first-time homebuyer");
    }

    @Test
    @DisplayName("Should handle ambiguous or unclear requests")
    void shouldHandleAmbiguousOrUnclearRequests() {
        String ambiguousInput = "loan stuff";
        
        NLPProcessingRequest request = NLPProcessingRequest.builder()
            .text(ambiguousInput)
            .sessionId("session-unclear")
            .build();

        NLPProcessingResult result = nlpService.processText(request);

        // Should request clarification
        assertThat(result.getIntent()).isEqualTo(Intent.UNCLEAR);
        assertThat(result.getClarificationNeeded()).isTrue();
        assertThat(result.getSuggestedQuestions()).isNotEmpty();
        assertThat(result.getConfidence()).isLessThan(0.5);
    }

    @Test
    @DisplayName("Should support multilingual processing")
    void shouldSupportMultilingualProcessing() {
        Map<String, String> multilingualInputs = Map.of(
            "en", "I need a $20,000 personal loan",
            "es", "Necesito un préstamo personal de $20,000",
            "fr", "J'ai besoin d'un prêt personnel de 20 000 $"
        );

        for (Map.Entry<String, String> input : multilingualInputs.entrySet()) {
            MultilingualProcessingRequest request = MultilingualProcessingRequest.builder()
                .text(input.getValue())
                .detectedLanguage(input.getKey())
                .targetLanguage("en")
                .build();

            MultilingualProcessingResult result = nlpService.processMultilingual(request);

            assertThat(result.getTranslatedText()).contains("20,000");
            assertThat(result.getTranslatedText()).contains("personal loan");
            assertThat(result.getExtractedIntent()).isEqualTo(Intent.LOAN_APPLICATION);
            assertThat(result.getExtractedAmount()).isEqualTo(BigDecimal.valueOf(20000));
        }
    }

    @Test
    @DisplayName("Should validate extracted financial information")
    void shouldValidateExtractedFinancialInformation() {
        String invalidInput = "I need a loan for negative five million dollars with 200% interest";
        
        NLPProcessingRequest request = NLPProcessingRequest.builder()
            .text(invalidInput)
            .enableValidation(true)
            .build();

        NLPProcessingResult result = nlpService.processText(request);

        // Should detect validation issues
        assertThat(result.getValidationErrors()).isNotEmpty();
        assertThat(result.getValidationErrors()).anyMatch(error -> 
            error.contains("negative amount") || error.contains("invalid amount"));
        assertThat(result.getValidationErrors()).anyMatch(error -> 
            error.contains("interest rate") || error.contains("unrealistic rate"));
    }

    @Test
    @DisplayName("Should generate contextual responses")
    void shouldGenerateContextualResponses() {
        ConversationContext context = ConversationContext.builder()
            .sessionId("session-context")
            .customerId("CUST-004")
            .currentStep(ConversationStep.INCOME_VERIFICATION)
            .extractedData(Map.of(
                "loan_amount", 50000,
                "loan_type", "personal"
            ))
            .build();

        String userInput = "My income varies month to month";
        
        ContextualResponseRequest request = ContextualResponseRequest.builder()
            .userInput(userInput)
            .context(context)
            .build();

        ContextualResponse response = nlpService.generateContextualResponse(request);

        // Should provide relevant guidance for variable income
        assertThat(response.getSystemResponse()).contains("variable income");
        assertThat(response.getSystemResponse()).contains("average");
        assertThat(response.getFollowUpQuestions()).isNotEmpty();
        assertThat(response.getUpdatedContext().getCurrentStep()).isEqualTo(ConversationStep.INCOME_VERIFICATION);
    }

    @Test
    @DisplayName("Should handle system errors gracefully")
    void shouldHandleSystemErrorsGracefully() {
        // Simulate system error
        nlpService.setModelAvailable(false);
        
        String userInput = "I need help with my loan application";
        
        NLPProcessingRequest request = NLPProcessingRequest.builder()
            .text(userInput)
            .sessionId("session-error")
            .build();

        // Should not throw exception, but provide fallback response
        NLPProcessingResult result = nlpService.processText(request);
        
        assertThat(result).isNotNull();
        assertThat(result.isFallbackMode()).isTrue();
        assertThat(result.getSystemMessage()).contains("connect you with a representative");
    }
}