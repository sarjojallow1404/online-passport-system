package com.passport.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.passport.dto.AdminApplicationStatusUpdateRequest;
import com.passport.dto.InterviewScheduleRequest;
import com.passport.entity.ApplicationInterviewSchedule;
import com.passport.entity.ApplicationStatusHistory;
import com.passport.entity.NotificationOutbox;
import com.passport.entity.PassportApplication;
import com.passport.entity.User;
import com.passport.repository.ApplicantProfileRepository;
import com.passport.repository.ApplicationDocumentRepository;
import com.passport.repository.ApplicationInterviewScheduleRepository;
import com.passport.repository.ApplicationStatusHistoryRepository;
import com.passport.repository.NotificationOutboxRepository;
import com.passport.repository.PassportApplicationRepository;
import com.passport.repository.UserRepository;
import com.passport.entity.ApplicationDocument;
import org.mockito.ArgumentCaptor;

class AdminInterviewNotificationTest {

    private static final Long APPLICATION_ID = 55L;
    private static final Long APPLICANT_ID = 200L;
    private static final Long ADMIN_ID = 10L;
    private static final Long HISTORY_EVENT_ID = 701L;

    private PassportApplicationRepository applicationRepository =
            mock(PassportApplicationRepository.class);
    private UserRepository userRepository =
            mock(UserRepository.class);
    private ApplicantProfileRepository profileRepository =
            mock(ApplicantProfileRepository.class);
    private ApplicationDocumentRepository documentRepository =
            mock(ApplicationDocumentRepository.class);
    private ApplicationStatusHistoryRepository historyRepository =
            mock(ApplicationStatusHistoryRepository.class);
    private ApplicationInterviewScheduleRepository interviewRepository =
            mock(ApplicationInterviewScheduleRepository.class);
    private NotificationOutboxRepository notificationRepository =
            mock(NotificationOutboxRepository.class);

    private AdminApplicationDetailService service;
    private PassportApplication application;
    private User applicant;

    @BeforeEach
    void setUp() {
        service = new AdminApplicationDetailService(
                applicationRepository,
                userRepository,
                profileRepository,
                documentRepository,
                historyRepository,
                interviewRepository,
                notificationRepository
        );

        application = mock(PassportApplication.class);
        applicant = mock(User.class);

        when(application.getId()).thenReturn(APPLICATION_ID);
        when(application.getApplicantId()).thenReturn(APPLICANT_ID);
        when(application.getApplicationNumber()).thenReturn("PASS-2026-00055");
        when(application.getPassportType()).thenReturn("ORDINARY");
        when(application.getApplicationStatus()).thenReturn("UNDER_REVIEW");

        when(applicant.getEmail()).thenReturn("applicant@example.com");

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));

        when(userRepository.findById(APPLICANT_ID))
                .thenReturn(Optional.of(applicant));

        when(profileRepository.findByUserId(APPLICANT_ID))
                .thenReturn(Optional.empty());

        when(documentRepository.findAllByApplicationIdOrderByUploadedAtDesc(
                APPLICATION_ID))
                .thenReturn(List.of());

        when(historyRepository.findAllByApplicationIdOrderByChangedAtDesc(
                APPLICATION_ID))
                .thenReturn(List.of());

        when(interviewRepository.findByApplicationId(APPLICATION_ID))
                .thenReturn(Optional.empty());

        ApplicationStatusHistory historyEvent =
                mock(ApplicationStatusHistory.class);

        when(historyEvent.getId()).thenReturn(HISTORY_EVENT_ID);

        when(historyRepository.saveAndFlush(
                any(ApplicationStatusHistory.class)))
                .thenReturn(historyEvent);

        when(interviewRepository.saveAndFlush(
                any(ApplicationInterviewSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void schedulesInPersonInterviewAndQueuesNotification() {
        LocalDate date = LocalDate.now().plusDays(5);
        LocalTime time = LocalTime.of(10, 30);

        AdminApplicationStatusUpdateRequest request =
                new AdminApplicationStatusUpdateRequest(
                        "READY_FOR_INTERVIEW",
                        null,
                        null,
                        date,
                        time,
                        "Passport Office, Banjul",
                        "IN_PERSON",
                        null,
                        "Bring your original documents"
                );

        service.updateApplicationStatus(
                APPLICATION_ID, ADMIN_ID, request);

        ArgumentCaptor<ApplicationInterviewSchedule> scheduleCaptor =
                ArgumentCaptor.forClass(ApplicationInterviewSchedule.class);

        verify(interviewRepository).saveAndFlush(scheduleCaptor.capture());

        ApplicationInterviewSchedule schedule = scheduleCaptor.getValue();

        assertEquals(APPLICATION_ID, schedule.getApplicationId());
        assertEquals(date, schedule.getInterviewDate());
        assertEquals(time, schedule.getInterviewTime());
        assertEquals("Passport Office, Banjul", schedule.getLocation());
        assertEquals("IN_PERSON", schedule.getMode());
        assertEquals("Bring your original documents", schedule.getInstructions());
        assertEquals(ADMIN_ID, schedule.getScheduledBy());

        ArgumentCaptor<NotificationOutbox> notificationCaptor =
                ArgumentCaptor.forClass(NotificationOutbox.class);

        verify(notificationRepository).save(notificationCaptor.capture());

        NotificationOutbox notification = notificationCaptor.getValue();

        assertEquals("INTERVIEW_SCHEDULED", notification.getNotificationType());
        assertEquals("applicant@example.com", notification.getRecipientEmail());
        assertEquals(
                "application:55:INTERVIEW_SCHEDULED:701",
                notification.getDeduplicationKey()
        );
        assertTrue(notification.getMessageBody().contains("Passport Office, Banjul"));
        assertTrue(notification.getMessageBody().contains("Bring your original documents"));
        assertEquals("PENDING", notification.getDeliveryStatus());
    }

    @Test
    void schedulesOnlineInterviewAndIncludesMeetingLink() {
        LocalDate date = LocalDate.now().plusDays(7);
        LocalTime time = LocalTime.of(14, 0);

        AdminApplicationStatusUpdateRequest request =
                new AdminApplicationStatusUpdateRequest(
                        "READY_FOR_INTERVIEW",
                        null,
                        null,
                        date,
                        time,
                        null,
                        "online",
                        "https://meet.example.com/passport-interview",
                        "Join five minutes early"
                );

        service.updateApplicationStatus(
                APPLICATION_ID, ADMIN_ID, request);

        ArgumentCaptor<ApplicationInterviewSchedule> scheduleCaptor =
                ArgumentCaptor.forClass(ApplicationInterviewSchedule.class);

        verify(interviewRepository).saveAndFlush(scheduleCaptor.capture());

        ApplicationInterviewSchedule schedule = scheduleCaptor.getValue();

        assertEquals("Online", schedule.getLocation());
        assertEquals("ONLINE", schedule.getMode());
        assertEquals(
                "https://meet.example.com/passport-interview",
                schedule.getMeetingLink()
        );

        ArgumentCaptor<NotificationOutbox> notificationCaptor =
                ArgumentCaptor.forClass(NotificationOutbox.class);

        verify(notificationRepository).save(notificationCaptor.capture());

        NotificationOutbox notification = notificationCaptor.getValue();

        assertEquals("INTERVIEW_SCHEDULED", notification.getNotificationType());
        assertTrue(notification.getMessageBody().contains("Online"));
        assertTrue(notification.getMessageBody().contains(
                "https://meet.example.com/passport-interview"));
    }

    @Test
    void reschedulesExistingInterviewAndQueuesSeparateNotification() {
        when(application.getApplicationStatus()).thenReturn("READY_FOR_INTERVIEW");
        LocalDate originalDate = LocalDate.now().plusDays(5);
        LocalDate newDate = LocalDate.now().plusDays(12);
        LocalTime newTime = LocalTime.of(11, 0);

        ApplicationInterviewSchedule existingSchedule =
                new ApplicationInterviewSchedule(
                        APPLICATION_ID,
                        originalDate,
                        LocalTime.of(9, 0),
                        "Passport Office, Banjul",
                        "IN_PERSON",
                        null,
                        null,
                        ADMIN_ID
                );

        when(interviewRepository.findByApplicationId(APPLICATION_ID))
                .thenReturn(Optional.of(existingSchedule));

        InterviewScheduleRequest request = new InterviewScheduleRequest(
                newDate,
                newTime,
                "New Passport Office, Banjul",
                "IN_PERSON",
                null,
                "Updated interview instructions"
        );

        service.rescheduleInterview(APPLICATION_ID, ADMIN_ID, request);

        verify(interviewRepository).saveAndFlush(existingSchedule);

        // The existing schedule is updated, not replaced with another record.
        assertEquals(newDate, existingSchedule.getInterviewDate());
        assertEquals(newTime, existingSchedule.getInterviewTime());
        assertEquals(
                "New Passport Office, Banjul",
                existingSchedule.getLocation()
        );
        assertEquals(
                "Updated interview instructions",
                existingSchedule.getInstructions()
        );

        ArgumentCaptor<NotificationOutbox> notificationCaptor =
                ArgumentCaptor.forClass(NotificationOutbox.class);

        verify(notificationRepository).save(notificationCaptor.capture());

        NotificationOutbox notification = notificationCaptor.getValue();

        assertEquals(
                "INTERVIEW_RESCHEDULED",
                notification.getNotificationType()
        );
        assertEquals(
                "application:55:INTERVIEW_RESCHEDULED:701",
                notification.getDeduplicationKey()
        );
        assertTrue(notification.getMessageBody().contains(
                "Your passport interview has been rescheduled."));
        assertTrue(notification.getMessageBody().contains(newDate.toString()));
    }

    @Test
    void rejectsInterviewDateInThePast() {
        AdminApplicationStatusUpdateRequest request =
                new AdminApplicationStatusUpdateRequest(
                        "READY_FOR_INTERVIEW",
                        null,
                        null,
                        LocalDate.now().minusDays(1),
                        LocalTime.of(10, 0),
                        "Passport Office, Banjul",
                        "IN_PERSON",
                        null,
                        null
                );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateApplicationStatus(
                        APPLICATION_ID, ADMIN_ID, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verifyNoInteractions(historyRepository);
        verifyNoInteractions(notificationRepository);
        verify(interviewRepository, never()).saveAndFlush(
                any(ApplicationInterviewSchedule.class));
    }

    @Test
    void rejectsOnlineInterviewWithoutMeetingLink() {
        AdminApplicationStatusUpdateRequest request =
                new AdminApplicationStatusUpdateRequest(
                        "READY_FOR_INTERVIEW",
                        null,
                        null,
                        LocalDate.now().plusDays(5),
                        LocalTime.of(10, 0),
                        null,
                        "ONLINE",
                        null,
                        null
                );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateApplicationStatus(
                        APPLICATION_ID, ADMIN_ID, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verifyNoInteractions(historyRepository);
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void rejectsUnsupportedInterviewMode() {
        AdminApplicationStatusUpdateRequest request =
                new AdminApplicationStatusUpdateRequest(
                        "READY_FOR_INTERVIEW",
                        null,
                        null,
                        LocalDate.now().plusDays(5),
                        LocalTime.of(10, 0),
                        "Passport Office, Banjul",
                        "PHONE",
                        null,
                        null
                );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateApplicationStatus(
                        APPLICATION_ID, ADMIN_ID, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verifyNoInteractions(historyRepository);
        verifyNoInteractions(notificationRepository);
        }
        @Test
        void requestsAdditionalDocumentAndQueuesNotification() {
        String reason = "The uploaded birth certificate is unclear.";

        AdminApplicationStatusUpdateRequest request =
                new AdminApplicationStatusUpdateRequest(
                        "ACTION_REQUIRED",
                        reason,
                        "BIRTH_CERTIFICATE",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

                when(application.getRequestedDocumentType())
        .thenReturn("BIRTH_CERTIFICATE");
        service.updateApplicationStatus(
                APPLICATION_ID, ADMIN_ID, request);

        verify(application).setRequestedDocumentType("BIRTH_CERTIFICATE");
        verify(applicationRepository).saveAndFlush(application);

        ArgumentCaptor<NotificationOutbox> notificationCaptor =
                ArgumentCaptor.forClass(NotificationOutbox.class);

        verify(notificationRepository).save(notificationCaptor.capture());

        NotificationOutbox notification = notificationCaptor.getValue();

        assertEquals("ACTION_REQUIRED", notification.getNotificationType());
        assertEquals(
                "Additional information required for your passport application",
                notification.getSubject()
        );
        assertEquals("applicant@example.com", notification.getRecipientEmail());
        assertEquals(
                "application:55:ACTION_REQUIRED:701",
                notification.getDeduplicationKey()
        );
        assertTrue(notification.getMessageBody().contains("BIRTH_CERTIFICATE"));
        assertTrue(notification.getMessageBody().contains(reason));
        assertEquals("PENDING", notification.getDeliveryStatus());

        verify(notificationRepository).flush();
        }
        @Test
        void rejectsApplicationAndQueuesNotificationWithReason() {
        String reason = "The applicant provided inconsistent personal details.";

        AdminApplicationStatusUpdateRequest request =
                new AdminApplicationStatusUpdateRequest(
                        "REJECTED",
                        reason,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        service.updateApplicationStatus(
                APPLICATION_ID, ADMIN_ID, request);

        verify(application).setRequestedDocumentType(null);
        verify(applicationRepository).saveAndFlush(application);

        ArgumentCaptor<NotificationOutbox> notificationCaptor =
                ArgumentCaptor.forClass(NotificationOutbox.class);

        verify(notificationRepository).save(notificationCaptor.capture());

        NotificationOutbox notification = notificationCaptor.getValue();

        assertEquals("APPLICATION_REJECTED", notification.getNotificationType());
        assertEquals(
                "Update on your passport application",
                notification.getSubject()
        );
        assertEquals("applicant@example.com", notification.getRecipientEmail());
        assertEquals(
                "application:55:APPLICATION_REJECTED:701",
                notification.getDeduplicationKey()
        );
        assertTrue(notification.getMessageBody().contains(
                "Your passport application has been rejected."));
        assertTrue(notification.getMessageBody().contains(reason));
        assertEquals("PENDING", notification.getDeliveryStatus());

        verify(notificationRepository).flush();
        }
        @Test
void approvesPendingReviewDocument() {
    Long documentId = 901L;

    ApplicationDocument document = new ApplicationDocument(
            APPLICATION_ID,
            "PASSPORT_PHOTO",
            "passport-photo.jpg",
            "photo-storage-key.jpg",
            "image/jpeg",
            1024L
    );

    when(documentRepository.findById(documentId))
            .thenReturn(Optional.of(document));

    service.reviewDocument(documentId, ADMIN_ID, "APPROVED", null);

    assertEquals("APPROVED", document.getDocumentStatus());
    assertEquals(ADMIN_ID, document.getReviewerId());

    verify(documentRepository).saveAndFlush(document);
}

@Test
void requestsCorrectionForPendingReviewDocumentAndQueuesNotification() {
    Long documentId = 902L;
    String note = "Please upload a clearer passport photograph.";

    ApplicationDocument document = new ApplicationDocument(
            APPLICATION_ID,
            "PASSPORT_PHOTO",
            "passport-photo.jpg",
            "photo-storage-key.jpg",
            "image/jpeg",
            1024L
    );

    when(documentRepository.findById(documentId))
            .thenReturn(Optional.of(document));

    service.reviewDocument(
            documentId,
            ADMIN_ID,
            "CORRECTION_REQUIRED",
            note
    );

    // Verify document review.
    assertEquals("CORRECTION_REQUIRED", document.getDocumentStatus());
    assertEquals(note, document.getReviewNote());
    assertEquals(ADMIN_ID, document.getReviewerId());

    verify(documentRepository).saveAndFlush(document);

    // Verify the requested replacement document type.
    verify(application).setRequestedDocumentType("PASSPORT_PHOTO");

    // Verify application status transition.
    verify(application).changeStatus(
            eq("ACTION_REQUIRED"),
            eq(note),
            any(java.time.LocalDateTime.class)
    );

    // Verify status history.
    verify(historyRepository).saveAndFlush(
        any(ApplicationStatusHistory.class));

    // Verify applicant notification.
    ArgumentCaptor<NotificationOutbox> notificationCaptor =
            ArgumentCaptor.forClass(NotificationOutbox.class);

    verify(notificationRepository).save(notificationCaptor.capture());

    NotificationOutbox notification = notificationCaptor.getValue();

    assertEquals("ACTION_REQUIRED", notification.getNotificationType());
    assertEquals("applicant@example.com", notification.getRecipientEmail());

    assertEquals(
            "application:55:ACTION_REQUIRED:701",
            notification.getDeduplicationKey()
    );

    assertTrue(notification.getMessageBody().contains("PASSPORT_PHOTO"));
    assertTrue(notification.getMessageBody().contains(note));

    verify(notificationRepository).flush();
}

@Test
void rejectsDocumentCorrectionWithoutNote() {
    Long documentId = 905L;

    ApplicationDocument document = new ApplicationDocument(
            APPLICATION_ID,
            "PASSPORT_PHOTO",
            "passport-photo.jpg",
            "photo-storage-key.jpg",
            "image/jpeg",
            1024L
    );

    when(documentRepository.findById(documentId))
            .thenReturn(Optional.of(document));

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.reviewDocument(
                    documentId,
                    ADMIN_ID,
                    "CORRECTION_REQUIRED",
                    " "
            )
    );

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

    assertEquals("PENDING_REVIEW", document.getDocumentStatus());

    verify(documentRepository, never()).saveAndFlush(any());
    verifyNoInteractions(notificationRepository);
    verifyNoInteractions(historyRepository);
}

@Test
void rejectsReviewOfSupersededDocument() {
    Long documentId = 903L;

    ApplicationDocument document = new ApplicationDocument(
            APPLICATION_ID,
            "PASSPORT_PHOTO",
            "old-passport-photo.jpg",
            "old-photo-storage-key.jpg",
            "image/jpeg",
            1024L
    );

    document.markAsSuperseded();

    when(documentRepository.findById(documentId))
            .thenReturn(Optional.of(document));

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.reviewDocument(
                    documentId, ADMIN_ID, "APPROVED", null)
    );

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

    verify(documentRepository, never()).saveAndFlush(any());
}

@Test
void rejectsReviewOfAlreadyApprovedDocument() {
    Long documentId = 904L;

    ApplicationDocument document = new ApplicationDocument(
            APPLICATION_ID,
            "PASSPORT_PHOTO",
            "passport-photo.jpg",
            "photo-storage-key.jpg",
            "image/jpeg",
            1024L
        );

        document.review(
                "APPROVED",
                ADMIN_ID,
                null,
                java.time.LocalDateTime.now()
        );

        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.reviewDocument(
                        documentId, ADMIN_ID, "CORRECTION_REQUIRED",
                        "Upload another photograph.")
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(documentRepository, never()).saveAndFlush(any());
        }
}
