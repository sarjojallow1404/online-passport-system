
package com.passport.dto;

import java.time.LocalDateTime;

import com.passport.entity.ApplicationDocument;

public record ApplicationDocumentResponse(
        Long id,
        Long applicationId,
        String documentType,
        String originalFilename,
        String contentType,
        Long fileSizeBytes,
        String documentStatus,
        LocalDateTime uploadedAt,
        LocalDateTime reviewedAt,
        String reviewNote) {

    public static ApplicationDocumentResponse from(
            ApplicationDocument document) {
        return new ApplicationDocumentResponse(
                document.getId(),
                document.getApplicationId(),
                document.getDocumentType(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getFileSizeBytes(),
                document.getDocumentStatus(),
                document.getUploadedAt(),
                document.getReviewedAt(),
                document.getReviewNote());
    }
}
