
package com.passport.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.passport.dto.AdminApplicationDetailResponse;
import com.passport.dto.AdminApplicationStatusHistoryResponse;
import com.passport.dto.AdminApplicationStatusUpdateRequest;
import com.passport.dto.ApplicantProfileResponse;
import com.passport.dto.ApplicationDocumentResponse;
import com.passport.dto.InterviewScheduleRequest;
import com.passport.dto.InterviewScheduleResponse;

import com.passport.entity.ApplicationDocument;
import com.passport.entity.ApplicationInterviewSchedule;
import com.passport.entity.ApplicationStatusHistory;
import com.passport.entity.NotificationOutbox;
import com.passport.entity.PassportApplication;

import com.passport.repository.ApplicantProfileRepository;
import com.passport.repository.ApplicationDocumentRepository;
import com.passport.repository.ApplicationInterviewScheduleRepository;
import com.passport.repository.ApplicationStatusHistoryRepository;
import com.passport.repository.NotificationOutboxRepository;
import com.passport.repository.PassportApplicationRepository;
import com.passport.repository.UserRepository;

@Service
public class AdminApplicationDetailService {

    private final PassportApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ApplicantProfileRepository profileRepository;
    private final ApplicationDocumentRepository documentRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final ApplicationInterviewScheduleRepository interviewRepository;
    private final NotificationOutboxRepository notificationRepository;

    public AdminApplicationDetailService(
            PassportApplicationRepository applicationRepository,
            UserRepository userRepository,
            ApplicantProfileRepository profileRepository,
            ApplicationDocumentRepository documentRepository,
            ApplicationStatusHistoryRepository historyRepository,
            ApplicationInterviewScheduleRepository interviewRepository,
            NotificationOutboxRepository notificationRepository) {

        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.documentRepository = documentRepository;
        this.historyRepository = historyRepository;
        this.interviewRepository = interviewRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public AdminApplicationDetailResponse getApplication(Long applicationId) {

        PassportApplication application = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found"));

        String email = userRepository.findById(application.getApplicantId())
                .map(user -> user.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Applicant account not found"));

        ApplicantProfileResponse profile = profileRepository
                .findByUserId(application.getApplicantId())
                .map(ApplicantProfileResponse::from)
                .orElse(null);

        List<ApplicationDocumentResponse> documents = documentRepository
                .findAllByApplicationIdOrderByUploadedAtDesc(applicationId)
                .stream()
                .map(ApplicationDocumentResponse::from)
                .toList();

        List<AdminApplicationStatusHistoryResponse> history = historyRepository
                .findAllByApplicationIdOrderByChangedAtDesc(applicationId)
                .stream()
                .map(AdminApplicationStatusHistoryResponse::from)
                .toList();

        InterviewScheduleResponse interview = interviewRepository
                .findByApplicationId(applicationId)
                .map(InterviewScheduleResponse::from)
                .orElse(null);

        return new AdminApplicationDetailResponse(
                application.getId(),
                application.getApplicationNumber(),
                application.getApplicantId(),
                email,
                application.getPassportType(),
                application.getApplicationStatus(),
                application.getSubmissionDate(),
                application.getDecisionDate(),
                application.getDecisionReason(),
                application.getRequestedDocumentType(),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                profile,
                documents,
                history,
                interview
        );
    }

    @Transactional
    public AdminApplicationDetailResponse updateApplicationStatus(
            Long applicationId,
            Long adminId,
            AdminApplicationStatusUpdateRequest request) {

        if (request == null || request.status() == null
                || request.status().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Status is required");
        }

        String status = request.status().trim().toUpperCase();
        String reason = normalize(request.reason());

        PassportApplication application = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found"));

        if ("ACTION_REQUIRED".equals(status)) {

            if (reason == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A reason is required");
            }

            String documentType = normalize(request.documentType());

            if (documentType == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The required document type must be specified");
            }

            documentType = documentType.toUpperCase();

            if (!documentType.matches("[A-Z][A-Z0-9_]{1,59}")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid document type");
            }

            application.setRequestedDocumentType(documentType);
        }

        if ("REJECTED".equals(status) && reason == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A rejection reason is required");
        }

        if ("READY_FOR_INTERVIEW".equals(status)) {
            validateInterview(
                    request.interviewDate(),
                    request.interviewTime(),
                    request.location(),
                    request.mode(),
                    request.meetingLink());
        }

        ApplicationStatusHistory statusHistory = recordStatusChange(
                applicationId,
                adminId,
                status,
                reason
        );

        if ("ACTION_REQUIRED".equals(status)) {

            applicationRepository.saveAndFlush(application);

            queueNotification(
                    application,
                    statusHistory.getId(),
                    "ACTION_REQUIRED",
                    "Additional information required for your passport application",
                    "Your passport application requires further action."
                            + "\n\nRequired document: "
                            + application.getRequestedDocumentType()
                            + "\nReason: " + reason
                            + "\n\nPlease log in and upload the requested document."
            );

        } else if ("REJECTED".equals(status)) {

            application.setRequestedDocumentType(null);
            applicationRepository.saveAndFlush(application);

            queueNotification(
                    application,
                    statusHistory.getId(),
                    "APPLICATION_REJECTED",
                    "Update on your passport application",
                    "Your passport application has been rejected."
                            + "\n\nReason: " + reason
            );

        } else if ("READY_FOR_INTERVIEW".equals(status)) {

            ApplicationInterviewSchedule schedule =
                    new ApplicationInterviewSchedule(
                            applicationId,
                            request.interviewDate(),
                            request.interviewTime(),
                            interviewLocation(
                                    request.location(), request.mode()),
                            request.mode().trim().toUpperCase(),
                            normalize(request.meetingLink()),
                            normalize(request.instructions()),
                            adminId
                    );

            interviewRepository.saveAndFlush(schedule);

            queueInterviewNotification(
                    application, statusHistory.getId(), request, false);
        }

        notificationRepository.flush();

        return getApplication(applicationId);
    }

    @Transactional
    public AdminApplicationDetailResponse updateApplicationStatus(
            Long applicationId,
            Long adminId,
            String requestedStatus,
            String reason) {

        recordStatusChange(
                applicationId,
                adminId,
                requestedStatus,
                reason
        );

        return getApplication(applicationId);
    }

    private ApplicationStatusHistory recordStatusChange(
            Long applicationId,
            Long adminId,
            String requestedStatus,
            String reason) {

        PassportApplication application = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found"));

        if (requestedStatus == null || requestedStatus.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Status is required");
        }

        String newStatus = requestedStatus.trim().toUpperCase();
        String currentStatus = application.getApplicationStatus();
        String normalizedReason = normalize(reason);

        Map<String, List<String>> allowedTransitions = Map.of(
                "SUBMITTED", List.of("UNDER_REVIEW"),
                "UNDER_REVIEW", List.of(
                        "ACTION_REQUIRED",
                        "READY_FOR_INTERVIEW",
                        "REJECTED"
                )
        );

        if (!allowedTransitions
                .getOrDefault(currentStatus, List.of())
                .contains(newStatus)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transition from " + currentStatus
                            + " to " + newStatus + " is not allowed");
        }

        if (("ACTION_REQUIRED".equals(newStatus)
                || "REJECTED".equals(newStatus))
                && normalizedReason == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A reason is required for " + newStatus);
        }

        LocalDateTime changedAt = LocalDateTime.now();

        application.changeStatus(
                newStatus,
                normalizedReason,
                changedAt
        );

        applicationRepository.flush();

        return historyRepository.saveAndFlush(
                new ApplicationStatusHistory(
                        application.getId(),
                        currentStatus,
                        newStatus,
                        adminId,
                        normalizedReason
                )
        );
    }

    @Transactional
    public AdminApplicationDetailResponse rescheduleInterview(
            Long applicationId,
            Long adminId,
            InterviewScheduleRequest request) {

        PassportApplication application = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found"));

        if (!"READY_FOR_INTERVIEW".equals(
                application.getApplicationStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Application is not ready for an interview");
        }

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Interview details are required");
        }

        validateInterview(
                request.interviewDate(),
                request.interviewTime(),
                request.location(),
                request.mode(),
                request.meetingLink()
        );

        ApplicationInterviewSchedule schedule = interviewRepository
                .findByApplicationId(applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Interview schedule not found"));

        schedule.updateSchedule(
                request.interviewDate(),
                request.interviewTime(),
                interviewLocation(request.location(), request.mode()),
                request.mode().trim().toUpperCase(),
                normalize(request.meetingLink()),
                normalize(request.instructions()),
                adminId
        );

        interviewRepository.saveAndFlush(schedule);

        ApplicationStatusHistory rescheduleHistory =
                historyRepository.saveAndFlush(
                        new ApplicationStatusHistory(
                                applicationId,
                                "READY_FOR_INTERVIEW",
                                "READY_FOR_INTERVIEW",
                                adminId,
                                "Interview rescheduled to "
                                        + request.interviewDate()
                                        + " at " + request.interviewTime()
                        )
                );

        queueInterviewNotification(
                application, rescheduleHistory.getId(), request, true);

        notificationRepository.flush();

        return getApplication(applicationId);
    }

    private void validateInterview(
            LocalDate date,
            LocalTime time,
            String location,
            String mode,
            String meetingLink) {

        if (date == null || time == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Interview date and time are required");
        }

        if (!LocalDateTime.of(date, time).isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Interview date and time must be in the future");
        }

        if (mode == null || mode.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Interview mode is required");
        }

        String normalizedMode = mode.trim().toUpperCase();

        if (!List.of("IN_PERSON", "ONLINE").contains(normalizedMode)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mode must be IN_PERSON or ONLINE");
        }

        if ("IN_PERSON".equals(normalizedMode)
                && (location == null || location.isBlank())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Interview location is required");
        }

        if ("ONLINE".equals(normalizedMode)) {

            if (meetingLink == null
                    || !meetingLink.trim().matches("^https?://.+")) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A valid HTTP or HTTPS meeting link is required");
            }
        }
    }

    private String interviewLocation(String location, String mode) {
        return "ONLINE".equalsIgnoreCase(mode)
                ? "Online"
                : location.trim();
    }

    private void queueInterviewNotification(
            PassportApplication application,
            Long eventId,
            InterviewScheduleRequest request,
            boolean rescheduled) {

        String body = (rescheduled
                ? "Your passport interview has been rescheduled."
                : "Your passport interview has been scheduled.")
                + "\n\nApplication number: "
                + application.getApplicationNumber()
                + "\nDate: " + request.interviewDate()
                + "\nTime: " + request.interviewTime()
                + "\nMode: " + request.mode()
                + "\nLocation: "
                + interviewLocation(request.location(), request.mode())
                + (normalize(request.meetingLink()) == null
                        ? ""
                        : "\nMeeting link: " + request.meetingLink())
                + (normalize(request.instructions()) == null
                        ? ""
                        : "\nInstructions: " + request.instructions());

        queueNotification(
                application,
                eventId,
                rescheduled
                        ? "INTERVIEW_RESCHEDULED"
                        : "INTERVIEW_SCHEDULED",
                rescheduled
                        ? "Your passport interview has been rescheduled"
                        : "Your passport interview has been scheduled",
                body
        );
    }

    /*
     * Overload for the initial status-update request.
     * Converts it into the interview-specific DTO.
     */
    private void queueInterviewNotification(
            PassportApplication application,
            Long eventId,
            AdminApplicationStatusUpdateRequest request,
            boolean rescheduled) {

        InterviewScheduleRequest interviewRequest =
                new InterviewScheduleRequest(
                        request.interviewDate(),
                        request.interviewTime(),
                        request.location(),
                        request.mode(),
                        request.meetingLink(),
                        request.instructions()
                );

        queueInterviewNotification(
                application,
                eventId,
                interviewRequest,
                rescheduled
        );
    }

    private void queueNotification(
            PassportApplication application,
            Long eventId,
            String type,
            String subject,
            String body) {

        String email = userRepository
                .findById(application.getApplicantId())
                .map(user -> user.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Applicant account not found"));

        notificationRepository.save(new NotificationOutbox(
                application.getId(),
                type,
                "application:" + application.getId()
                        + ":" + type + ":" + eventId,
                email,
                subject,
                body
        ));
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    @Transactional
        public AdminApplicationDetailResponse reviewDocument(
                Long documentId,
                Long adminId,
                String requestedStatus,
                String note) {

        ApplicationDocument document = documentRepository
                .findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found"));

        if (!"PENDING_REVIEW".equals(document.getDocumentStatus())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Only documents awaiting review can be reviewed.");
        }

        if (requestedStatus == null || requestedStatus.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Document review status is required");
        }

        String newStatus = requestedStatus.trim().toUpperCase();
        String normalizedNote = normalize(note);

        if (!List.of("APPROVED", "CORRECTION_REQUIRED")
                .contains(newStatus)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Status must be APPROVED or CORRECTION_REQUIRED");
        }

        if ("CORRECTION_REQUIRED".equals(newStatus)
                && normalizedNote == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A correction note is required");
        }

        // Approving a document does not change the application status.
        if ("APPROVED".equals(newStatus)) {
                document.review(
                        newStatus,
                        adminId,
                        normalizedNote,
                        LocalDateTime.now()
                );

                documentRepository.saveAndFlush(document);

                return getApplication(document.getApplicationId());
        }

        // Requesting a correction must update the complete application workflow.
        PassportApplication application = applicationRepository
                .findById(document.getApplicationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application not found"));

        application.setRequestedDocumentType(document.getDocumentType());

        document.review(
                newStatus,
                adminId,
                normalizedNote,
                LocalDateTime.now()
        );

        documentRepository.saveAndFlush(document);

        ApplicationStatusHistory statusHistory = recordStatusChange(
                application.getId(),
                adminId,
                "ACTION_REQUIRED",
                normalizedNote
        );

        queueNotification(
                application,
                statusHistory.getId(),
                "ACTION_REQUIRED",
                "Additional information required for your passport application",
                "Your passport application requires further action."
                        + "\n\nRequired document: "
                        + document.getDocumentType()
                        + "\nReason: " + normalizedNote
                        + "\n\nPlease log in and upload the requested document."
        );

        notificationRepository.flush();

        return getApplication(application.getId());
        }
}
