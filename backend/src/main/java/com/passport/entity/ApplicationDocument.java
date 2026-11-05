
package com.passport.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_documents")
public class ApplicationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "document_type", nullable = false, length = 60)
    private String documentType;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "document_status", nullable = false, length = 30)
    private String documentStatus = "PENDING_REVIEW";

    @Column(name = "uploaded_at", insertable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "replaces_document_id")
    private Long replacesDocumentId;

    protected ApplicationDocument() {
    }

    public ApplicationDocument(
            Long applicationId,
            String documentType,
            String originalFilename,
            String storageKey,
            String contentType,
            Long fileSizeBytes) {
        this.applicationId = applicationId;
        this.documentType = documentType;
        this.originalFilename = originalFilename;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.documentStatus = "PENDING_REVIEW";
    }

    public void review(
            String status,
            Long reviewerId,
            String note,
            LocalDateTime reviewedAt) {
        this.documentStatus = status;
        this.reviewerId = reviewerId;
        this.reviewNote = note;
        this.reviewedAt = reviewedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getDocumentStatus() {
        return documentStatus;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public Long getReplacesDocumentId() {
        return replacesDocumentId;
    }

    public void setReplacesDocumentId(Long replacesDocumentId) {
        this.replacesDocumentId = replacesDocumentId;
    }

    public void markAsSuperseded() {
        this.documentStatus = "SUPERSEDED";
    }


}
