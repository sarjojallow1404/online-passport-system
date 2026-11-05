package com.passport.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "passport_applications")
public class PassportApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_number", nullable = false, unique = true, length = 40)
    private String applicationNumber;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "passport_type", nullable = false, length = 40)
    private String passportType = "ORDINARY";

    @Column(name = "application_status", nullable = false, length = 40)
    private String applicationStatus = "DRAFT";

    @Column(name = "submission_date")
    private LocalDateTime submissionDate;

    @Column(name = "decision_date")
    private LocalDateTime decisionDate;

    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;

    @Column(name = "requested_document_type", length = 60)
    private String requestedDocumentType;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected PassportApplication() {
    }

    public PassportApplication(String applicationNumber, Long applicantId) {
        this.applicationNumber = applicationNumber;
        this.applicantId = applicantId;
        this.passportType = "ORDINARY";
        this.applicationStatus = "DRAFT";
    }


public void changeStatus(
        String newStatus,
        String reason,
        LocalDateTime changedAt) {
    this.applicationStatus = newStatus;
    this.decisionReason = reason;

    if ("READY_FOR_INTERVIEW".equals(newStatus)
            || "REJECTED".equals(newStatus)) {
        this.decisionDate = changedAt;
    }
}

   public void submit(java.time.LocalDateTime submittedAt) {
        if (!"DRAFT".equals(this.applicationStatus)) {
            throw new IllegalStateException(
                "Only draft applications can be submitted"
            );
        }

        this.applicationStatus = "SUBMITTED";
        this.submissionDate = submittedAt;
    }

    public void completeDocumentReplacement(LocalDateTime changedAt) {
        if (!"ACTION_REQUIRED".equals(this.applicationStatus)) {
            throw new IllegalStateException(
                    "Document replacement is only allowed when action is required.");
        }

        this.applicationStatus = "UNDER_REVIEW";
        this.decisionReason = null;
        this.requestedDocumentType = null;
    }

    public Long getId() {
        return id;
    }

    public String getApplicationNumber() {
        return applicationNumber;
    }

    public Long getApplicantId() {
        return applicantId;
    }

    public String getPassportType() {
        return passportType;
    }

    public String getApplicationStatus() {
        return applicationStatus;
    }

    public LocalDateTime getSubmissionDate() {
        return submissionDate;
    }

    public LocalDateTime getDecisionDate() {
        return decisionDate;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public String getRequestedDocumentType() {
        return requestedDocumentType;
    }

    public void setRequestedDocumentType(String requestedDocumentType) {
        this.requestedDocumentType = requestedDocumentType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
