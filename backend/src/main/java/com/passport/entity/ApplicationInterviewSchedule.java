package com.passport.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_interview_schedules")
public class ApplicationInterviewSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false, unique = true)
    private Long applicationId;

    @Column(name = "interview_date", nullable = false)
    private LocalDate interviewDate;

    @Column(name = "interview_time", nullable = false)
    private LocalTime interviewTime;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(nullable = false, length = 30)
    private String mode;

    @Column(name = "meeting_link", length = 1000)
    private String meetingLink;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "scheduled_by", nullable = false)
    private Long scheduledBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected ApplicationInterviewSchedule() {
    }

    public ApplicationInterviewSchedule(
            Long applicationId,
            LocalDate interviewDate,
            LocalTime interviewTime,
            String location,
            String mode,
            String meetingLink,
            String instructions,
            Long scheduledBy) {
        this.applicationId = applicationId;
        this.scheduledBy = scheduledBy;
        updateSchedule(
                interviewDate,
                interviewTime,
                location,
                mode,
                meetingLink,
                instructions,
                scheduledBy
        );
    }

    public void updateSchedule(
            LocalDate interviewDate,
            LocalTime interviewTime,
            String location,
            String mode,
            String meetingLink,
            String instructions,
            Long scheduledBy) {
        this.interviewDate = interviewDate;
        this.interviewTime = interviewTime;
        this.location = location;
        this.mode = mode;
        this.meetingLink = meetingLink;
        this.instructions = instructions;
        this.scheduledBy = scheduledBy;
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public LocalDate getInterviewDate() {
        return interviewDate;
    }

    public LocalTime getInterviewTime() {
        return interviewTime;
    }

    public String getLocation() {
        return location;
    }

    public String getMode() {
        return mode;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public String getInstructions() {
        return instructions;
    }

    public Long getScheduledBy() {
        return scheduledBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
