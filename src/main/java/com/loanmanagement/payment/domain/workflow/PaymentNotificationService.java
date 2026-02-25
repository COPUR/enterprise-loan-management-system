package com.loanmanagement.payment.domain.workflow;

import com.loanmanagement.payment.domain.model.PaymentWorkflowId;
import com.loanmanagement.payment.domain.model.PaymentWorkflowState;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for sending notifications during payment workflow execution.
 * Handles various notification types and channels.
 */
@Slf4j
public class PaymentNotificationService {
    
    private final NotificationChannelProvider channelProvider;
    private final NotificationTemplateEngine templateEngine;
    private final NotificationAuditLogger auditLogger;
    
    public PaymentNotificationService(
            NotificationChannelProvider channelProvider,
            NotificationTemplateEngine templateEngine,
            NotificationAuditLogger auditLogger) {
        
        this.channelProvider = channelProvider;
        this.templateEngine = templateEngine;
        this.auditLogger = auditLogger;
    }
    
    /**
     * Sends a workflow state change notification.
     */
    public CompletableFuture<NotificationResult> notifyStateChange(
            PaymentWorkflowExecution execution,
            PaymentWorkflowState previousState,
            PaymentWorkflowState newState) {
        
        log.info("Sending state change notification for workflow {}: {} -> {}",
                execution.getWorkflowId(), previousState, newState);
        
        NotificationRequest request = NotificationRequest.builder()
                .notificationType(NotificationType.STATE_CHANGE)
                .workflowId(execution.getWorkflowId())
                .paymentId(execution.getPaymentId())
                .customerId(execution.getCustomerId())
                .templateId("workflow.state.change")
                .parameters(Map.of(
                        "workflowId", execution.getWorkflowId().getValue(),
                        "previousState", previousState.name(),
                        "newState", newState.name(),
                        "timestamp", Instant.now().toString()
                ))
                .priority(determinePriority(newState))
                .channels(determineChannels(execution, newState))
                .build();
        
        return sendNotification(request);
    }
    
    /**
     * Sends a workflow completion notification.
     */
    public CompletableFuture<NotificationResult> notifyCompletion(
            PaymentWorkflowExecution execution,
            PaymentWorkflowResult result) {
        
        log.info("Sending completion notification for workflow {}",
                execution.getWorkflowId());
        
        NotificationRequest request = NotificationRequest.builder()
                .notificationType(NotificationType.COMPLETION)
                .workflowId(execution.getWorkflowId())
                .paymentId(execution.getPaymentId())
                .customerId(execution.getCustomerId())
                .templateId(result.isSuccessful() ? 
                        "workflow.completion.success" : "workflow.completion.failure")
                .parameters(Map.of(
                        "workflowId", execution.getWorkflowId().getValue(),
                        "paymentId", result.getPaymentId(),
                        "success", result.isSuccess(),
                        "transactionId", result.getTransactionId() != null ? 
                                result.getTransactionId() : "N/A",
                        "duration", result.getDurationMillis() != null ?
                                result.getDurationMillis().toString() : "N/A"
                ))
                .priority(NotificationPriority.HIGH)
                .channels(List.of(NotificationChannel.EMAIL, NotificationChannel.SMS))
                .build();
        
        return sendNotification(request);
    }
    
    /**
     * Sends an error notification.
     */
    public CompletableFuture<NotificationResult> notifyError(
            PaymentWorkflowExecution execution,
            PaymentWorkflowException error) {
        
        log.error("Sending error notification for workflow {}: {}",
                execution.getWorkflowId(), error.getMessage());
        
        NotificationRequest request = NotificationRequest.builder()
                .notificationType(NotificationType.ERROR)
                .workflowId(execution.getWorkflowId())
                .paymentId(execution.getPaymentId())
                .customerId(execution.getCustomerId())
                .templateId("workflow.error")
                .parameters(Map.of(
                        "workflowId", execution.getWorkflowId().getValue(),
                        "errorCode", error.getErrorCode(),
                        "errorMessage", error.getMessage(),
                        "retryable", error.isRetryable()
                ))
                .priority(NotificationPriority.CRITICAL)
                .channels(List.of(NotificationChannel.EMAIL, 
                        NotificationChannel.SMS, 
                        NotificationChannel.PUSH))
                .build();
        
        return sendNotification(request);
    }
    
    /**
     * Sends an approval required notification.
     */
    public CompletableFuture<NotificationResult> notifyApprovalRequired(
            PaymentWorkflowExecution execution,
            String approverRole,
            Map<String, Object> approvalContext) {
        
        log.info("Sending approval required notification for workflow {}",
                execution.getWorkflowId());
        
        NotificationRequest request = NotificationRequest.builder()
                .notificationType(NotificationType.APPROVAL_REQUIRED)
                .workflowId(execution.getWorkflowId())
                .paymentId(execution.getPaymentId())
                .customerId(execution.getCustomerId())
                .templateId("workflow.approval.required")
                .parameters(Map.of(
                        "workflowId", execution.getWorkflowId().getValue(),
                        "approverRole", approverRole,
                        "approvalContext", approvalContext
                ))
                .priority(NotificationPriority.HIGH)
                .channels(List.of(NotificationChannel.EMAIL, NotificationChannel.PUSH))
                .recipientRole(approverRole)
                .build();
        
        return sendNotification(request);
    }
    
    /**
     * Sends a batch notification for multiple workflows.
     */
    public CompletableFuture<List<NotificationResult>> notifyBatch(
            List<PaymentWorkflowExecution> executions,
            NotificationType type,
            String templateId) {
        
        log.info("Sending batch notification for {} workflows", executions.size());
        
        List<CompletableFuture<NotificationResult>> futures = executions.stream()
                .map(execution -> {
                    NotificationRequest request = NotificationRequest.builder()
                            .notificationType(type)
                            .workflowId(execution.getWorkflowId())
                            .paymentId(execution.getPaymentId())
                            .customerId(execution.getCustomerId())
                            .templateId(templateId)
                            .parameters(Map.of(
                                    "workflowId", execution.getWorkflowId().getValue(),
                                    "currentState", execution.getCurrentState().name()
                            ))
                            .priority(NotificationPriority.MEDIUM)
                            .channels(List.of(NotificationChannel.EMAIL))
                            .build();
                    
                    return sendNotification(request);
                })
                .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }
    
    /**
     * Sends a notification through configured channels.
     */
    private CompletableFuture<NotificationResult> sendNotification(
            NotificationRequest request) {
        
        // Render the notification content
        NotificationContent content = templateEngine.render(
                request.getTemplateId(),
                request.getParameters());
        
        // Send through each channel
        List<CompletableFuture<ChannelResult>> channelFutures = 
                request.getChannels().stream()
                        .map(channel -> channelProvider
                                .getChannel(channel)
                                .send(request, content))
                        .toList();
        
        return CompletableFuture.allOf(
                channelFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<ChannelResult> channelResults = channelFutures.stream()
                            .map(CompletableFuture::join)
                            .toList();
                    
                    NotificationResult result = NotificationResult.builder()
                            .notificationId(generateNotificationId())
                            .request(request)
                            .sentAt(Instant.now())
                            .channelResults(channelResults)
                            .overallSuccess(channelResults.stream()
                                    .anyMatch(ChannelResult::isSuccess))
                            .build();
                    
                    // Audit the notification
                    auditLogger.logNotification(result);
                    
                    return result;
                });
    }
    
    /**
     * Determines notification priority based on state.
     */
    private NotificationPriority determinePriority(PaymentWorkflowState state) {
        return switch (state) {
            case FAILED, REJECTED -> NotificationPriority.CRITICAL;
            case COMPLETED, APPROVED -> NotificationPriority.HIGH;
            case PROCESSING, VALIDATING -> NotificationPriority.MEDIUM;
            default -> NotificationPriority.LOW;
        };
    }
    
    /**
     * Determines notification channels based on execution and state.
     */
    private List<NotificationChannel> determineChannels(
            PaymentWorkflowExecution execution,
            PaymentWorkflowState state) {
        
        // Critical states use all channels
        if (state == PaymentWorkflowState.FAILED || 
            state == PaymentWorkflowState.REJECTED) {
            return List.of(NotificationChannel.EMAIL, 
                          NotificationChannel.SMS, 
                          NotificationChannel.PUSH);
        }
        
        // Completion states use email and SMS
        if (state == PaymentWorkflowState.COMPLETED) {
            return List.of(NotificationChannel.EMAIL, NotificationChannel.SMS);
        }
        
        // Default to email only
        return List.of(NotificationChannel.EMAIL);
    }
    
    /**
     * Generates a unique notification ID.
     */
    private String generateNotificationId() {
        return "NOTIF-" + System.currentTimeMillis() + "-" + 
                Thread.currentThread().getId();
    }
    
    /**
     * Notification request details.
     */
    @Value
    @Builder
    public static class NotificationRequest {
        NotificationType notificationType;
        PaymentWorkflowId workflowId;
        String paymentId;
        String customerId;
        String templateId;
        Map<String, Object> parameters;
        NotificationPriority priority;
        List<NotificationChannel> channels;
        String recipientRole;
    }
    
    /**
     * Notification result.
     */
    @Value
    @Builder
    public static class NotificationResult {
        String notificationId;
        NotificationRequest request;
        Instant sentAt;
        List<ChannelResult> channelResults;
        boolean overallSuccess;
    }
    
    /**
     * Channel result.
     */
    @Value
    @Builder
    public static class ChannelResult {
        NotificationChannel channel;
        boolean success;
        String messageId;
        String errorMessage;
    }
    
    /**
     * Notification types.
     */
    public enum NotificationType {
        STATE_CHANGE,
        COMPLETION,
        ERROR,
        APPROVAL_REQUIRED,
        REMINDER,
        ESCALATION
    }
    
    /**
     * Notification priorities.
     */
    public enum NotificationPriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    /**
     * Notification channels.
     */
    public enum NotificationChannel {
        EMAIL,
        SMS,
        PUSH,
        IN_APP,
        WEBHOOK
    }
    
    /**
     * Notification content.
     */
    @Value
    @Builder
    public static class NotificationContent {
        String subject;
        String body;
        String htmlBody;
        Map<String, String> headers;
    }
    
    /**
     * Channel provider interface.
     */
    public interface NotificationChannelProvider {
        NotificationChannelHandler getChannel(NotificationChannel channel);
    }
    
    /**
     * Channel handler interface.
     */
    public interface NotificationChannelHandler {
        CompletableFuture<ChannelResult> send(
                NotificationRequest request,
                NotificationContent content);
    }
    
    /**
     * Template engine interface.
     */
    public interface NotificationTemplateEngine {
        NotificationContent render(String templateId, Map<String, Object> parameters);
    }
    
    /**
     * Audit logger interface.
     */
    public interface NotificationAuditLogger {
        void logNotification(NotificationResult result);
    }
}