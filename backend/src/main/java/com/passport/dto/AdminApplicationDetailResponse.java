package com.passport.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminApplicationDetailResponse(
    Long id,
    String applicationNumber,
    Long applicantId,
    String applicantEmail,
    String passportType,
    String applicationStatus,
    LocalDateTime submissionDate,
    LocalDateTime decisionDate,
    String decisionReason,
    String requestedDocumentType,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    ApplicantProfileResponse applicantProfile,
    List<ApplicationDocumentResponse> documents,
    List<AdminApplicationStatusHistoryResponse> statusHistory,
    InterviewScheduleResponse interviewSchedule
) {}
