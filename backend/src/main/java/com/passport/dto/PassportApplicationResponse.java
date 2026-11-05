
package com.passport.dto;

import java.time.LocalDateTime;

import com.passport.entity.PassportApplication;

public record PassportApplicationResponse(
        Long id,
        String applicationNumber,
        Long applicantId,
        String passportType,
        String applicationStatus,
        LocalDateTime submissionDate,
        LocalDateTime decisionDate,
        String decisionReason,
        String requestedDocumentType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        InterviewScheduleResponse interviewSchedule) {

    public static PassportApplicationResponse from(
            PassportApplication application) {
        return from(application, null);
    }

    public static PassportApplicationResponse from(
            PassportApplication application,
            InterviewScheduleResponse interviewSchedule) {

        return new PassportApplicationResponse(
                application.getId(),
                application.getApplicationNumber(),
                application.getApplicantId(),
                application.getPassportType(),
                application.getApplicationStatus(),
                application.getSubmissionDate(),
                application.getDecisionDate(),
                application.getDecisionReason(),
                application.getRequestedDocumentType(),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                interviewSchedule
        );
    }
}
