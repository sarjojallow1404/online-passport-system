package com.passport.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.passport.entity.ApplicationInterviewSchedule;

public record InterviewScheduleResponse(
        Long id,
        LocalDate interviewDate,
        LocalTime interviewTime,
        String location,
        String mode,
        String meetingLink,
        String instructions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static InterviewScheduleResponse from(
            ApplicationInterviewSchedule schedule) {
        if (schedule == null) {
            return null;
        }

        return new InterviewScheduleResponse(
                schedule.getId(),
                schedule.getInterviewDate(),
                schedule.getInterviewTime(),
                schedule.getLocation(),
                schedule.getMode(),
                schedule.getMeetingLink(),
                schedule.getInstructions(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt());
    }
}
