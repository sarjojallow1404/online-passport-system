package com.passport.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record InterviewScheduleRequest(
        LocalDate interviewDate,
        LocalTime interviewTime,
        String location,
        String mode,
        String meetingLink,
        String instructions
) {
}
