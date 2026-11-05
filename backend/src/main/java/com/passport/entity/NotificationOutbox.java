
package com.passport.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_outbox")
public class NotificationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "notification_type", nullable = false, length = 60)
    private String notificationType;

    @Column(name = "deduplication_key", nullable = false, unique = true, length = 150)
    private String deduplicationKey;

    @Column(name = "recipient_email", nullable = false, length = 254)
    private String recipientEmail;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(name = "message_body", nullable = false, columnDefinition = "TEXT")
    private String messageBody;

    @Column(name = "delivery_status", nullable = false, length = 20)
    private String deliveryStatus = "PENDING";

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    protected NotificationOutbox() {
    }

    public NotificationOutbox(
            Long applicationId,
            String notificationType,
            String deduplicationKey,
            String recipientEmail,
            String subject,
            String messageBody) {

        this.applicationId = applicationId;
        this.notificationType = notificationType;
        this.deduplicationKey = deduplicationKey;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.messageBody = messageBody;
        this.deliveryStatus = "PENDING";
        this.attempts = 0;
    }

    public void markProcessing(LocalDateTime startedAt) {
        this.deliveryStatus = "PROCESSING";
        this.processingStartedAt = startedAt;
        this.nextAttemptAt = null;
    }

    public void markSent(LocalDateTime sentAt) {
        this.deliveryStatus = "SENT";
        this.sentAt = sentAt;
        this.lastError = null;
        this.nextAttemptAt = null;
        this.processingStartedAt = null;
    }

    public void markFailed(
            String error,
            LocalDateTime nextAttemptAt,
            boolean retryable) {

        this.deliveryStatus = retryable ? "PENDING" : "FAILED";
        this.attempts = this.attempts + 1;
        this.lastError = error;
        this.nextAttemptAt = retryable ? nextAttemptAt : null;
        this.processingStartedAt = null;
    }

    public void recoverForRetry(
        LocalDateTime retryAt,
        String error,
        boolean retryable) {

        if (!"PROCESSING".equals(this.deliveryStatus)) {
            return;
        }

        this.attempts = this.attempts + 1;
        this.deliveryStatus = retryable ? "PENDING" : "FAILED";
        this.nextAttemptAt = retryable ? retryAt : null;
        this.lastError = error;
        this.processingStartedAt = null;
    }
    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public String getDeduplicationKey() {
        return deduplicationKey;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public LocalDateTime getProcessingStartedAt() {
        return processingStartedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
