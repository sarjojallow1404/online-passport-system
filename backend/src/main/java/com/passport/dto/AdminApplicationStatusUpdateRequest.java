package com.passport.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record AdminApplicationStatusUpdateRequest(
        String status,
        String reason,
        String documentType,
        LocalDate interviewDate,
        LocalTime interviewTime,
        String location,
        String mode,
        String meetingLink,
        String instructions
) {
}
