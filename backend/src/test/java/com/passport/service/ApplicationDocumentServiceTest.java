
package com.passport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.passport.dto.ApplicationDocumentResponse;
import com.passport.entity.ApplicationDocument;
import com.passport.entity.ApplicationStatusHistory;
import com.passport.entity.PassportApplication;
import com.passport.repository.ApplicationDocumentRepository;
import com.passport.repository.ApplicationStatusHistoryRepository;
import com.passport.repository.PassportApplicationRepository;
import org.mockito.ArgumentCaptor;
class ApplicationDocumentServiceTest {

    private static final Long APPLICANT_ID = 200L;
    private static final Long APPLICATION_ID = 55L;

    private ApplicationDocumentRepository documentRepository;
    private PassportApplicationRepository applicationRepository;
    private DocumentStorageService documentStorageService;
    private ApplicationStatusHistoryRepository historyRepository;

    private ApplicationDocumentService service;

    @BeforeEach
    void setUp() {
        documentRepository = mock(ApplicationDocumentRepository.class);
        applicationRepository = mock(PassportApplicationRepository.class);
        documentStorageService = mock(DocumentStorageService.class);
        historyRepository = mock(ApplicationStatusHistoryRepository.class);

        service = new ApplicationDocumentService(
                documentRepository,
                applicationRepository,
                documentStorageService,
                historyRepository);

        when(documentRepository.save(any(ApplicationDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void successfullyReplacesRequestedDocumentAndReturnsApplicationToReview()
            throws Exception {

        PassportApplication application =
                createApplication("ACTION_REQUIRED");

        application.setRequestedDocumentType("BIRTH_CERTIFICATE");

        when(applicationRepository.findByIdAndApplicantId(
                APPLICATION_ID, APPLICANT_ID))
                .thenReturn(Optional.of(application));

        ApplicationDocument previousDocument =
                mock(ApplicationDocument.class);

        when(previousDocument.getId()).thenReturn(777L);

        when(documentRepository
                .findFirstByApplicationIdAndDocumentTypeOrderByUploadedAtDesc(
                        APPLICATION_ID, "BIRTH_CERTIFICATE"))
                .thenReturn(Optional.of(previousDocument));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "birth-certificate.pdf",
                "application/pdf",
                "%PDF-test-content".getBytes());

        when(documentStorageService.store(file))
                .thenReturn(new DocumentStorageService.StoredFile(
                        "new-document.pdf",
                        "birth-certificate.pdf",
                        "application/pdf",
                        file.getSize()));

        ApplicationDocumentResponse response = service.uploadDocument(
                APPLICANT_ID,
                APPLICATION_ID,
                "BIRTH_CERTIFICATE",
                file);

        // The previous document must be superseded.
        verify(previousDocument).markAsSuperseded();
        verify(documentRepository).save(previousDocument);

        // The replacement must be saved and linked to the previous document.
        ArgumentCaptor<ApplicationDocument> documentCaptor =
                ArgumentCaptor.forClass(ApplicationDocument.class);

        verify(documentRepository, times(2))
                .save(documentCaptor.capture());

        ApplicationDocument replacementDocument =
                documentCaptor.getAllValues().get(1);

        assertEquals(
                777L,
                replacementDocument.getReplacesDocumentId()
        );

        // The application must return to UNDER_REVIEW.
        assertEquals("UNDER_REVIEW", application.getApplicationStatus());
        assertEquals(null, application.getRequestedDocumentType());
        assertEquals(null, application.getDecisionReason());

        // A status history entry must be recorded.
        verify(historyRepository).save(any(ApplicationStatusHistory.class));

        // The upload response must describe the newly stored document.
        assertNotNull(response);
        assertEquals(APPLICATION_ID, response.applicationId());
        assertEquals("BIRTH_CERTIFICATE", response.documentType());
        assertEquals("birth-certificate.pdf", response.originalFilename());
        assertEquals("application/pdf", response.contentType());
        assertEquals(file.getSize(), response.fileSizeBytes());

        verify(documentStorageService).store(file);
    }

    @Test
    void rejectsUploadWhenDocumentTypeDoesNotMatchRequestedType()
            throws Exception {

        PassportApplication application =
                createApplication("ACTION_REQUIRED");

        application.setRequestedDocumentType("BIRTH_CERTIFICATE");

        when(applicationRepository.findByIdAndApplicantId(
                APPLICATION_ID, APPLICANT_ID))
                .thenReturn(Optional.of(application));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "wrong-document.pdf",
                "application/pdf",
                "%PDF-test-content".getBytes());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.uploadDocument(
                        APPLICANT_ID,
                        APPLICATION_ID,
                        "NATIONAL_ID",
                        file));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        verify(documentStorageService, never()).store(any());
        verify(documentRepository, never())
                .save(any(ApplicationDocument.class));
        verify(historyRepository, never())
                .save(any(ApplicationStatusHistory.class));
    }

    @Test
    void rejectsReplacementWhenNoDocumentTypeWasRequested()
            throws Exception {

        PassportApplication application =
                createApplication("ACTION_REQUIRED");

        when(applicationRepository.findByIdAndApplicantId(
                APPLICATION_ID, APPLICANT_ID))
                .thenReturn(Optional.of(application));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "birth-certificate.pdf",
                "application/pdf",
                "%PDF-test-content".getBytes());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.uploadDocument(
                        APPLICANT_ID,
                        APPLICATION_ID,
                        "BIRTH_CERTIFICATE",
                        file));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(documentStorageService, never()).store(any());
        verify(documentRepository, never())
                .save(any(ApplicationDocument.class));
    }

    @Test
    void rejectsUploadsWhenApplicationIsNotDraftOrAwaitingCorrection()
            throws Exception {

        PassportApplication application =
                createApplication("SUBMITTED");

        when(applicationRepository.findByIdAndApplicantId(
                APPLICATION_ID, APPLICANT_ID))
                .thenReturn(Optional.of(application));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "birth-certificate.pdf",
                "application/pdf",
                "%PDF-test-content".getBytes());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.uploadDocument(
                        APPLICANT_ID,
                        APPLICATION_ID,
                        "BIRTH_CERTIFICATE",
                        file));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(documentStorageService, never()).store(any());
        verify(documentRepository, never())
                .save(any(ApplicationDocument.class));
        verify(historyRepository, never())
                .save(any(ApplicationStatusHistory.class));
    }

    @Test
    void rejectsEmptyReplacementFile() throws Exception {

        PassportApplication application =
                createApplication("ACTION_REQUIRED");

        application.setRequestedDocumentType("BIRTH_CERTIFICATE");

        when(applicationRepository.findByIdAndApplicantId(
                APPLICATION_ID, APPLICANT_ID))
                .thenReturn(Optional.of(application));

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "birth-certificate.pdf",
                "application/pdf",
                new byte[0]);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.uploadDocument(
                        APPLICANT_ID,
                        APPLICATION_ID,
                        "BIRTH_CERTIFICATE",
                        emptyFile));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(documentStorageService, never()).store(any());
        verify(documentRepository, never())
                .save(any(ApplicationDocument.class));
        verify(historyRepository, never())
                .save(any(ApplicationStatusHistory.class));
    }

    private PassportApplication createApplication(String status) {
        PassportApplication application = new PassportApplication(
                "PASS-2026-00055",
                APPLICANT_ID);

        LocalDateTime now = LocalDateTime.now();

        if ("SUBMITTED".equals(status)) {
            application.submit(now);
        } else if ("UNDER_REVIEW".equals(status)) {
            application.submit(now);
            application.changeStatus("UNDER_REVIEW", null, now);
        } else if ("ACTION_REQUIRED".equals(status)) {
            application.submit(now);
            application.changeStatus("UNDER_REVIEW", null, now);
            application.changeStatus(
                    "ACTION_REQUIRED",
                    "Please provide a replacement document.",
                    now);
        }

        return application;
    }
}
