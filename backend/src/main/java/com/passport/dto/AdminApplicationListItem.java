package com.passport.dto;

import java.time.LocalDateTime;

public record AdminApplicationListItem(
    Long id,
    String applicationNumber,
    Long applicantId,
    String applicantEmail,
    String firstName,
    String middleName,
    String lastName,
    String passportType,
    String applicationStatus,
    LocalDateTime submissionDate,
    LocalDateTime createdAt
) {}
