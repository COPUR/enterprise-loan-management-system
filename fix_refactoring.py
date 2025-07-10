import os
import shutil

def find_and_replace_in_files(root_dir, replacements):
    """Find and replace strings in all .java files under root_dir."""
    for dirpath, _, filenames in os.walk(root_dir):
        for filename in filenames:
            if filename.endswith(".java"):
                filepath = os.path.join(dirpath, filename)
                try:
                    with open(filepath, 'r', encoding='utf-8') as f:
                        content = f.read()
                except UnicodeDecodeError:
                    print(f"Skipping file with encoding issues: {filepath}")
                    continue
                
                original_content = content
                for old_str, new_str in replacements.items():
                    content = content.replace(old_str, new_str)
                
                if original_content != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(content)
                    print(f"Updated imports in: {filepath}")

def main():
    base_src_path = "/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/src"
    main_java_path = os.path.join(base_src_path, "main/java/com/bank")
    test_java_path = os.path.join(base_src_path, "test/java/com/bank")

    # 1. Consolidate com.bank.loan.loan into com.bank.loan
    for base_path in [main_java_path, test_java_path]:
        src = os.path.join(base_path, "loan/loan")
        dest = os.path.join(base_path, "loan")
        if os.path.exists(src):
            for item in os.listdir(src):
                s_item = os.path.join(src, item)
                d_item = os.path.join(dest, item)
                if os.path.isdir(s_item):
                    shutil.move(s_item, d_item)
                else:
                    shutil.move(s_item, d_item)
            if os.path.exists(src) and not os.listdir(src):
                os.rmdir(src)
            print(f"Moved contents of {src} to {dest}")

    # 2. Define all required package name replacements
    replacements = {
        "com.bank.loan.loan": "com.bank.loan",
        "com.bank.loanmanagement.customermanagement": "com.bank.customer",
        "com.bank.loanmanagement.customer": "com.bank.customer",
        "com.bank.loanmanagement.payment": "com.bank.payment",
        "com.bank.loanmanagement.risk": "com.bank.risk",
        "com.bank.loanmanagement.loan": "com.bank.loan",
        "com.bank.loanmanagement": "com.bank.loan",
        "com.bank.loans": "com.bank.loan",
        "com.bank.loan.domain.services": "com.bank.loan.domain.services", # This is already correct
        "com.bank.loan.domain.ports.out": "com.bank.loan.domain.ports.out", # This is already correct
        "com.bank.loan.domain.events": "com.bank.loan.domain.events", # This is already correct
        "com.bank.loan.domain.party": "com.bank.loan.domain.party", # This is already correct
        "com.bank.loan.domain.shared.ExternalEventPublisher": "com.bank.loan.infrastructure.events.ExternalEventPublisher",
        "com.bank.loanmanagement.domain.model.Customer": "com.bank.customer.domain.model.Customer",
        "com.bank.loanmanagement.domain.model.LoanRequest": "com.bank.loan.domain.model.LoanRequest",
        "com.bank.loanmanagement.domain.model.UserIntentAnalysis": "com.bank.loan.domain.model.UserIntentAnalysis",
        "com.bank.loan.domain.loan.LoanAggregate": "com.bank.loan.domain.loan.LoanAggregate", # This is already correct
        "com.bank.loan.domain.loan.CustomerId": "com.bank.customer.domain.model.CustomerId",
        "com.bank.loan.domain.loan.AIRiskAssessment": "com.bank.loan.domain.loan.AIRiskAssessment", # This is already correct
        "com.bank.loan.domain.loan.ComplianceCheck": "com.bank.loan.domain.loan.ComplianceCheck", # This is already correct
        "com.bank.loan.domain.shared.DomainEventPublisher": "com.bank.loan.infrastructure.events.DomainEventPublisher",
        "com.bank.loan.domain.shared.Customer": "com.bank.customer.domain.model.Customer",
        "com.bank.loan.domain.loan.PaymentWaterfall": "com.bank.loan.domain.loan.PaymentWaterfall", # This is already correct
        "com.bank.loan.domain.services.PaymentScheduleGenerator": "com.bank.loan.domain.services.PaymentScheduleGenerator",
        "com.bank.loan.domain.services.PaymentWaterfallService": "com.bank.loan.domain.services.PaymentWaterfallService",
        "com.bank.loan.domain.services.LateFeeCalculationService": "com.bank.loan.domain.services.LateFeeCalculationService",
        "com.bank.loan.domain.services.LateFeeAssessment": "com.bank.loan.domain.services.LateFeeAssessment",
        "com.bank.loan.domain.ports.out.LoanRepository": "com.bank.loan.domain.ports.out.LoanRepository",
        "com.bank.loan.domain.party.ComplianceLevel": "com.bank.loan.domain.party.ComplianceLevel",
        "com.bank.loan.domain.party.PartyStatus": "com.bank.loan.domain.party.PartyStatus",
        "com.bank.loan.domain.party.PartyType": "com.bank.loan.domain.party.PartyType",
        "com.bank.loan.domain.party.RoleSource": "com.bank.loan.domain.party.RoleSource",
        "com.bank.loan.domain.party.GroupRole": "com.bank.loan.domain.party.GroupRole",
        "com.bank.loan.domain.party.GroupType": "com.bank.loan.domain.party.GroupType",
        "com.bank.loan.domain.ports.NaturalLanguageProcessingPort": "com.bank.loan.domain.ports.NaturalLanguageProcessingPort",
        "com.bank.loan.domain.ports.NaturalLanguageProcessingPort.LoanRequest": "com.bank.loan.domain.model.LoanRequest",
        "com.bank.loan.domain.ports.NaturalLanguageProcessingPort.UserIntentAnalysis": "com.bank.loan.domain.model.UserIntentAnalysis",
        "com.bank.loan.domain.ports.NaturalLanguageProcessingPort.FinancialParameters": "com.bank.loan.domain.model.FinancialParameters",
        "com.bank.loan.domain.ports.NaturalLanguageProcessingPort.RequestAssessment": "com.bank.loan.domain.model.RequestAssessment",
        "com.bank.loan.saga.model.SagaExecution": "com.bank.loan.saga.model.SagaExecution",
        "com.bank.loan.saga.definition.SagaStepDefinition": "com.bank.loan.saga.definition.SagaStepDefinition",
        "com.bank.loan.saga.definition.SagaDefinition": "com.bank.loan.saga.definition.SagaDefinition",
        "com.bank.loan.saga.orchestrator.SagaOrchestrator.SagaStatus": "com.bank.loan.saga.orchestrator.SagaOrchestrator.SagaStatus",
        "com.bank.loan.saga.model.SagaExecutionImpl": "com.bank.loan.saga.model.SagaExecutionImpl",
        "com.bank.loan.saga.model.SagaStepImpl": "com.bank.loan.saga.model.SagaStepImpl",
        "com.bank.loan.saga.context.SagaContext": "com.bank.loan.saga.context.SagaContext",
        "com.bank.loan.saga.executor.SagaStepExecutor": "com.bank.loan.saga.executor.SagaStepExecutor",
        "com.bank.loan.saga.orchestrator.SpringSagaOrchestrator": "com.bank.loan.saga.orchestrator.SpringSagaOrchestrator",
        "com.bank.loan.saga.IntelligentLoanOriginationSaga": "com.bank.loan.saga.IntelligentLoanOriginationSaga",
        "com.bank.loan.security.config.SecurityProperties": "com.bank.loan.security.config.SecurityProperties",
        "com.bank.loan.security.config.FAPISecurityConfig": "com.bank.loan.security.config.FAPISecurityConfig",
        "com.bank.loan.security.filter.FAPIRateLimitingFilter": "com.bank.loan.security.filter.FAPIRateLimitingFilter",
        "com.bank.loan.security.model.TokenAnalytics": "com.bank.loan.security.model.TokenAnalytics",
        "com.bank.loan.security.FAPITokenManagementService": "com.bank.loan.security.FAPITokenManagementService",
        "com.bank.loan.security.model.TemporaryTokenData": "com.bank.loan.security.model.TemporaryTokenData",
        "com.bank.loan.security.model.FAPITokenRequest": "com.bank.loan.security.model.FAPITokenRequest",
        "com.bank.loan.security.model.FAPISecurityConfiguration": "com.bank.loan.security.model.FAPISecurityConfiguration",
        "com.bank.loan.security.model.FAPITokenResponse": "com.bank.loan.security.model.FAPITokenResponse",
        "com.bank.loan.security.model.TokenBinding": "com.bank.loan.security.model.TokenBinding",
        "com.bank.loan.security.model.TokenValidationService": "com.bank.loan.security.model.TokenValidationService",
        "com.bank.loan.security.exceptions.FAPISecurityException": "com.bank.loan.security.exceptions.FAPISecurityException",
        "com.bank.loan.security.FAPITokenRefreshRequest": "com.bank.loan.security.FAPITokenRefreshRequest",
        "com.bank.loan.security.FAPITokenRevocationRequest": "com.bank.loan.security.FAPITokenRevocationRequest",
        "com.bank.loan.security.FAPITokenValidationRequest": "com.bank.loan.security.FAPITokenValidationRequest",
        "com.bank.loan.security.FAPITokenValidationResult": "com.bank.loan.security.FAPITokenValidationResult",
        "com.bank.loan.security.RateLimitingService": "com.bank.loan.security.RateLimitingService",
        "com.bank.loan.security.StoredTokenData": "com.bank.loan.security.StoredTokenData",
        "com.bank.loan.security.TemporaryTokenRequest": "com.bank.loan.security.TemporaryTokenRequest",
        "com.bank.loan.security.TemporaryTokenResponse": "com.bank.loan.security.TemporaryTokenResponse",
        "com.bank.loan.security.TokenAnalytics": "com.bank.loan.security.TokenAnalytics",
        "com.bank.loan.security.config.SecurityProperties.Cors": "com.bank.loan.security.config.SecurityProperties.Cors",
        "com.bank.loan.security.config.SecurityProperties.Jwt": "com.bank.loan.security.config.SecurityProperties.Jwt",
        "com.bank.loan.security.config.SecurityProperties.Password": "com.bank.loan.security.config.SecurityProperties.Password",
        "com.bank.loan.security.config.SecurityProperties.RateLimit": "com.bank.loan.security.config.SecurityProperties.RateLimit",
        "com.bank.loan.domain.shared.Customer": "com.bank.customer.domain.model.Customer",
        "com.bank.loan.domain.loan.CustomerId": "com.bank.customer.domain.model.CustomerId",
        "com.bank.loan.domain.loan.AIRiskAssessment": "com.bank.loan.domain.loan.AIRiskAssessment",
        "com.bank.loan.domain.loan.ComplianceCheck": "com.bank.loan.domain.loan.ComplianceCheck",
        "com.bank.loan.domain.loan.PaymentWaterfall": "com.bank.loan.domain.loan.PaymentWaterfall",
        "com.bank.loan.domain.services.PaymentScheduleGenerator": "com.bank.loan.domain.services.PaymentScheduleGenerator",
        "com.bank.loan.domain.services.PaymentWaterfallService": "com.bank.loan.domain.services.PaymentWaterfallService",
        "com.bank.loan.domain.services.LateFeeCalculationService": "com.bank.loan.domain.services.LateFeeCalculationService",
        "com.bank.loan.domain.services.LateFeeAssessment": "com.bank.loan.domain.services.LateFeeAssessment",
        "com.bank.loan.domain.ports.out.LoanRepository": "com.bank.loan.domain.ports.out.LoanRepository",
        "com.bank.loan.domain.events": "com.bank.loan.domain.events",
        "com.bank.loan.domain.party": "com.bank.loan.domain.party",
        "com.bank.loan.domain.shared.ExternalEventPublisher": "com.bank.loan.infrastructure.events.ExternalEventPublisher",
        "com.bank.loan.domain.ports.NaturalLanguageProcessingPort.LoanRequest": "com.bank.loan.domain.model.LoanRequest",
        "com.bank.loan.domain.ports.NaturalLanguageProcessingPort.UserIntentAnalysis": "com.bank.loan.domain.model.UserIntentAnalysis",
        "com.bank.loan.domain.ports.NaturalLanguageProcessingPort.FinancialParameters": "com.bank.loan.domain.model.FinancialParameters",
        "com.bank.loan.domain.ports.NaturalLanguageProcessingPort.RequestAssessment": "com.bank.loan.domain.model.RequestAssessment",
        "com.bank.loan.domain.ports.out.CustomerRepository": "com.bank.customer.domain.port.out.CustomerRepository",
        "com.bank.loan.domain.ports.in.CustomerManagementUseCase": "com.bank.customer.domain.port.in.CustomerManagementUseCase",
        "com.bank.loan.domain.ports.NaturalLanguageProcessingPort": "com.bank.loan.domain.ports.NaturalLanguageProcessingPort",
        "com.bank.loan.domain.shared.DomainEventPublisher": "com.bank.loan.infrastructure.events.DomainEventPublisher",
        "com.bank.loan.domain.loan.LoanAggregate": "com.bank.loan.domain.loan.LoanAggregate",
        "com.bank.loan.domain.loan.LoanId": "com.bank.loan.domain.loan.LoanId",
        "com.bank.loan.domain.loan.LoanStatus": "com.bank.loan.domain.loan.LoanStatus",
        "com.bank.loan.domain.loan.InterestRate": "com.bank.loan.domain.loan.InterestRate",
        "com.bank.loan.domain.loan.LoanTerm": "com.bank.loan.domain.loan.LoanTerm",
        "com.bank.loan.domain.loan.LoanType": "com.bank.loan.domain.loan.LoanType",
        "com.bank.loan.domain.loan.RiskLevel": "com.bank.loan.domain.loan.RiskLevel",
        "com.bank.loan.domain.shared.AggregateRoot": "com.bank.loan.domain.shared.AggregateRoot",
        "com.bank.loan.domain.shared.Money": "com.bank.loan.domain.shared.Money",
        "com.bank.loan.domain.shared.DomainEvent": "com.bank.loan.domain.shared.DomainEvent",
        "com.bank.loan.domain.shared.CustomerProfile": "com.bank.loan.domain.shared.CustomerProfile",
        "com.bank.loan.domain.shared.EntityId": "com.bank.loan.domain.shared.EntityId",
        "com.bank.loan.domain.shared.DomainId": "com.bank.loan.domain.shared.DomainId",
        "com.bank.loan.domain.shared.BianTypes": "com.bank.loan.domain.shared.BianTypes",
        "com.bank.loan.domain.shared.BerlinGroupTypes": "com.bank.loan.domain.shared.BerlinGroupTypes",
        "com.bank.loan.domain.shared.DomainEntity": "com.bank.loan.domain.shared.DomainEntity",
        "com.bank.loan.domain.shared.CreditScore": "com.bank.loan.domain.shared.CreditScore",
        "com.bank.loan.domain.shared.LoanRequest": "com.bank.loan.domain.shared.LoanRequest",
        "com.bank.loan.domain.shared.UserIntentAnalysis": "com.bank.loan.domain.shared.UserIntentAnalysis",
        "com.bank.loan.domain.shared.FinancialParameters": "com.bank.loan.domain.shared.FinancialParameters",
        "com.bank.loan.domain.shared.RequestAssessment": "com.bank.loan.domain.shared.RequestAssessment",
        "com.bank.loan.domain.shared.PaymentWaterfall": "com.bank.loan.domain.shared.PaymentWaterfall",
        "com.bank.loan.domain.shared.PaymentScheduleGenerator": "com.bank.loan.domain.shared.PaymentScheduleGenerator",
        "com.bank.loan.domain.shared.LateFeeCalculationService": "com.bank.loan.domain.shared.LateFeeCalculationService",
        "com.bank.loan.domain.shared.LateFeeAssessment": "com.bank.loan.domain.shared.LateFeeAssessment",
        "com.bank.loan.domain.shared.LoanRepository": "com.bank.loan.domain.ports.out.LoanRepository",
        "com.bank.loan.domain.shared.AIRiskAssessment": "com.bank.loan.domain.loan.AIRiskAssessment",
        "com.bank.loan.domain.shared.ComplianceCheck": "com.bank.loan.domain.loan.ComplianceCheck",
        "com.bank.loan.domain.shared.PaymentWaterfallService": "com.bank.loan.domain.services.PaymentWaterfallService",
        "com.bank.loan.domain.shared.PaymentScheduleGenerator": "com.bank.loan.domain.services.PaymentScheduleGenerator",
        "com.bank.loan.domain.shared.LateFeeCalculationService": "com.bank.loan.domain.services.LateFeeCalculationService",
        "com.bank.loan.domain.shared.LateFeeAssessment": "com.bank.loan.domain.services.LateFeeAssessment",
        "com.bank.loan.domain.shared.LoanRepository": "com.bank.loan.domain.ports.out.LoanRepository",
        "com.bank.loan.domain.shared.AIRiskAssessmentPort": "com.bank.loan.application.ports.out.AIRiskAssessmentPort",
        "com.bank.loan.domain.shared.ComplianceCheckPort": "com.bank.loan.application.ports.out.ComplianceCheckPort",
        "com.bank.loan.domain.shared.CustomerRepository": "com.bank.customer.domain.port.out.CustomerRepository",
        "com.bank.loan.domain.shared.CustomerManagementUseCase": "com.bank.customer.domain.port.in.CustomerManagementUseCase",
        "com.bank.loan.domain.shared.NaturalLanguageProcessingPort": "com.bank.loan.domain.ports.NaturalLanguageProcessingPort",
        "com.bank.loan.domain.shared.SagaExecution": "com.bank.loan.saga.model.SagaExecution",
        "com.bank.loan.domain.shared.SagaStepDefinition": "com.bank.loan.saga.definition.SagaStepDefinition",
        "com.bank.loan.domain.shared.SagaDefinition": "com.bank.loan.saga.definition.SagaDefinition",
        "com.bank.loan.domain.shared.SagaOrchestrator": "com.bank.loan.saga.orchestrator.SagaOrchestrator",
        "com.bank.loan.domain.shared.SpringSagaOrchestrator": "com.bank.loan.saga.orchestrator.SpringSagaOrchestrator",
        "com.bank.loan.domain.shared.IntelligentLoanOriginationSaga": "com.bank.loan.saga.IntelligentLoanOriginationSaga",
        "com.bank.loan.domain.shared.SecurityProperties": "com.bank.loan.security.config.SecurityProperties",
        "com.bank.loan.domain.shared.FAPISecurityConfig": "com.bank.loan.security.config.FAPISecurityConfig",
        "com.bank.loan.domain.shared.FAPIRateLimitingFilter": "com.bank.loan.security.filter.FAPIRateLimitingFilter",
        "com.bank.loan.domain.shared.TokenAnalytics": "com.bank.loan.security.model.TokenAnalytics",
        "com.bank.loan.domain.shared.FAPITokenManagementService": "com.bank.loan.security.FAPITokenManagementService",
        "com.bank.loan.domain.shared.TemporaryTokenData": "com.bank.loan.security.model.TemporaryTokenData",
        "com.bank.loan.domain.shared.FAPITokenRequest": "com.bank.loan.security.model.FAPITokenRequest",
        "com.bank.loan.domain.shared.FAPISecurityConfiguration": "com.bank.loan.security.model.FAPISecurityConfiguration",
        "com.bank.loan.domain.shared.FAPITokenResponse": "com.bank.loan.security.model.FAPITokenResponse",
        "com.bank.loan.domain.shared.TokenBinding": "com.bank.loan.security.model.TokenBinding",
        "com.bank.loan.domain.shared.TokenValidationService": "com.bank.loan.security.model.TokenValidationService",
        "com.bank.loan.domain.shared.FAPISecurityException": "com.bank.loan.security.exceptions.FAPISecurityException",
        "com.bank.loan.domain.shared.FAPITokenRefreshRequest": "com.bank.loan.security.FAPITokenRefreshRequest",
        "com.bank.loan.domain.shared.FAPITokenRevocationRequest": "com.bank.loan.security.FAPITokenRevocationRequest",
        "com.bank.loan.domain.shared.FAPITokenValidationRequest": "com.bank.loan.security.FAPITokenValidationRequest",
        "com.bank.loan.domain.shared.FAPITokenValidationResult": "com.bank.loan.security.FAPITokenValidationResult",
        "com.bank.loan.domain.shared.RateLimitingService": "com.bank.loan.security.RateLimitingService",
        "com.bank.loan.domain.shared.StoredTokenData": "com.bank.loan.security.StoredTokenData",
        "com.bank.loan.domain.shared.TemporaryTokenRequest": "com.bank.loan.security.TemporaryTokenRequest",
        "com.bank.loan.domain.shared.TemporaryTokenResponse": "com.bank.loan.security.TemporaryTokenResponse",
        "com.bank.loan.domain.shared.TokenAnalytics": "com.bank.loan.security.TokenAnalytics",
        "com.bank.loan.domain.shared.SecurityProperties.Cors": "com.bank.loan.security.config.SecurityProperties.Cors",
        "com.bank.loan.domain.shared.SecurityProperties.Jwt": "com.bank.loan.security.config.SecurityProperties.Jwt",
        "com.bank.loan.domain.shared.SecurityProperties.Password": "com.bank.loan.security.config.SecurityProperties.Password",
        "com.bank.loan.domain.shared.SecurityProperties.RateLimit": "com.bank.loan.security.config.SecurityProperties.RateLimit",
        "com.bank.loan.domain.shared.LoanRequest": "com.bank.loan.domain.model.LoanRequest",
        "com.bank.loan.domain.shared.UserIntentAnalysis": "com.bank.loan.domain.model.UserIntentAnalysis",
        "com.bank.loan.domain.shared.FinancialParameters": "com.bank.loan.domain.model.FinancialParameters",
        "com.bank.loan.domain.shared.RequestAssessment": "com.bank.loan.domain.model.RequestAssessment",
        "com.bank.loan.domain.shared.PaymentWaterfall": "com.bank.loan.domain.loan.PaymentWaterfall",
        "com.bank.loan.domain.shared.PaymentScheduleGenerator": "com.bank.loan.domain.services.PaymentScheduleGenerator",
        "com.bank.loan.domain.shared.LateFeeCalculationService": "com.bank.loan.domain.services.LateFeeCalculationService",
        "com.bank.loan.domain.shared.LateFeeAssessment": "com.bank.loan.domain.services.LateFeeAssessment",
        "com.bank.loan.domain.shared.LoanRepository": "com.bank.loan.domain.ports.out.LoanRepository",
        "com.bank.loan.domain.shared.AIRiskAssessment": "com.bank.loan.domain.loan.AIRiskAssessment",
        "com.bank.loan.domain.shared.ComplianceCheck": "com.bank.loan.domain.loan.ComplianceCheck",
        "com.bank.loan.domain.shared.PaymentWaterfallService": "com.bank.loan.domain.services.PaymentWaterfallService",
        "com.bank.loan.domain.shared.PaymentScheduleGenerator": "com.bank.loan.domain.services.PaymentScheduleGenerator",
        "com.bank.loan.domain.shared.LateFeeCalculationService": "com.bank.loan.domain.services.LateFeeCalculationService",
        "com.bank.loan.domain.shared.LateFeeAssessment": "com.bank.loan.domain.services.LateFeeAssessment",
        "com.bank.loan.domain.shared.LoanRepository": "com.bank.loan.domain.ports.out.LoanRepository",
        "com.bank.loan.domain.shared.AIRiskAssessmentPort": "com.bank.loan.application.ports.out.AIRiskAssessmentPort",
        "com.bank.loan.domain.shared.ComplianceCheckPort": "com.bank.loan.application.ports.out.ComplianceCheckPort",
        "com.bank.loan.domain.shared.CustomerRepository": "com.bank.customer.domain.port.out.CustomerRepository",
        "com.bank.loan.domain.shared.CustomerManagementUseCase": "com.bank.customer.domain.port.in.CustomerManagementUseCase",
        "com.bank.loan.domain.shared.NaturalLanguageProcessingPort": "com.bank.loan.domain.ports.NaturalLanguageProcessingPort",
        "com.bank.loan.domain.shared.SagaExecution": "com.bank.loan.saga.model.SagaExecution",
        "com.bank.loan.domain.shared.SagaStepDefinition": "com.bank.loan.saga.definition.SagaStepDefinition",
        "com.bank.loan.domain.shared.SagaDefinition": "com.bank.loan.saga.definition.SagaDefinition",
        "com.bank.loan.domain.shared.SagaOrchestrator": "com.bank.loan.saga.orchestrator.SagaOrchestrator",
        "com.bank.loan.domain.shared.SpringSagaOrchestrator": "com.bank.loan.saga.orchestrator.SpringSagaOrchestrator",
        "com.bank.loan.domain.shared.IntelligentLoanOriginationSaga": "com.bank.loan.saga.IntelligentLoanOriginationSaga",
        "com.bank.loan.domain.shared.SecurityProperties": "com.bank.loan.security.config.SecurityProperties",
        "com.bank.loan.domain.shared.FAPISecurityConfig": "com.bank.loan.security.config.FAPISecurityConfig",
        "com.bank.loan.domain.shared.FAPIRateLimitingFilter": "com.bank.loan.security.filter.FAPIRateLimitingFilter",
        "com.bank.loan.domain.shared.TokenAnalytics": "com.bank.loan.security.model.TokenAnalytics",
        "com.bank.loan.domain.shared.FAPITokenManagementService": "com.bank.loan.security.FAPITokenManagementService",
        "com.bank.loan.domain.shared.TemporaryTokenData": "com.bank.loan.security.model.TemporaryTokenData",
        "com.bank.loan.domain.shared.FAPITokenRequest": "com.bank.loan.security.model.FAPITokenRequest",
        "com.bank.loan.domain.shared.FAPISecurityConfiguration": "com.bank.loan.security.model.FAPISecurityConfiguration",
        "com.bank.loan.domain.shared.FAPITokenResponse": "com.bank.loan.security.model.FAPITokenResponse",
        "com.bank.loan.domain.shared.TokenBinding": "com.bank.loan.security.model.TokenBinding",
        "com.bank.loan.domain.shared.TokenValidationService": "com.bank.loan.security.model.TokenValidationService",
        "com.bank.loan.domain.shared.FAPISecurityException": "com.bank.loan.security.exceptions.FAPISecurityException",
        "com.bank.loan.domain.shared.FAPITokenRefreshRequest": "com.bank.loan.security.FAPITokenRefreshRequest",
        "com.bank.loan.domain.shared.FAPITokenRevocationRequest": "com.bank.loan.security.FAPITokenRevocationRequest",
        "com.bank.loan.domain.shared.FAPITokenValidationRequest": "com.bank.loan.security.FAPITokenValidationRequest",
        "com.bank.loan.domain.shared.FAPITokenValidationResult": "com.bank.loan.security.FAPITokenValidationResult",
        "com.bank.loan.domain.shared.RateLimitingService": "com.bank.loan.security.RateLimitingService",
        "com.bank.loan.domain.shared.StoredTokenData": "com.bank.loan.security.StoredTokenData",
        "com.bank.loan.domain.shared.TemporaryTokenRequest": "com.bank.loan.security.TemporaryTokenRequest",
        "com.bank.loan.domain.shared.TemporaryTokenResponse": "com.bank.loan.security.TemporaryTokenResponse",
        "com.bank.loan.domain.shared.TokenAnalytics": "com.bank.loan.security.TokenAnalytics",
        "com.bank.loan.domain.shared.SecurityProperties.Cors": "com.bank.loan.security.config.SecurityProperties.Cors",
        "com.bank.loan.domain.shared.SecurityProperties.Jwt": "com.bank.loan.security.config.SecurityProperties.Jwt",
        "com.bank.loan.domain.shared.SecurityProperties.Password": "com.bank.loan.security.config.SecurityProperties.Password",
        "com.bank.loan.domain.shared.SecurityProperties.RateLimit": "com.bank.loan.security.config.SecurityProperties.RateLimit",
    }

    # 3. Apply replacements across the entire source
    find_and_replace_in_files(base_src_path, replacements)

    print("Refactoring fix complete.")

if __name__ == "__main__":
    main()