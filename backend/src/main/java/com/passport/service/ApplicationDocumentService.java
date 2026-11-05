package com.passport.service;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.passport.dto.ApplicationDocumentResponse;
import com.passport.entity.ApplicationDocument;
import com.passport.entity.PassportApplication;
import com.passport.repository.ApplicationDocumentRepository;
import com.passport.repository.PassportApplicationRepository;

import org.springframework.core.io.Resource;

import com.passport.entity.ApplicationStatusHistory;
import com.passport.repository.ApplicationStatusHistoryRepository;

@Service
public class ApplicationDocumentService {

    private final ApplicationDocumentRepository documentRepository;
    private final PassportApplicationRepository applicationRepository;
    private final DocumentStorageService documentStorageService;
    private final ApplicationStatusHistoryRepository historyRepository;

    public ApplicationDocumentService(
        ApplicationDocumentRepository documentRepository,
        PassportApplicationRepository applicationRepository,
        DocumentStorageService documentStorageService,
        ApplicationStatusHistoryRepository historyRepository) {

        this.documentRepository = documentRepository;
        this.applicationRepository = applicationRepository;
        this.documentStorageService = documentStorageService;
        this.historyRepository = historyRepository;
  }

    @Transactional

        public ApplicationDocumentResponse uploadDocument(
                Long applicantId,
                Long applicationId,
                String documentType,
                MultipartFile file) {

        PassportApplication application = applicationRepository
                .findByIdAndApplicantId(applicationId, applicantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Passport application was not found."));

        if (documentType == null || documentType.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Document type is required.");
        }

        String normalizedType = documentType.trim().toUpperCase();

        if (!normalizedType.matches("[A-Z][A-Z0-9_]{1,59}")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Document type must contain 2–60 letters, numbers, or underscores.");
        }

        boolean isDraft = "DRAFT".equals(application.getApplicationStatus());
        boolean isReplacement =
                "ACTION_REQUIRED".equals(application.getApplicationStatus());

        ApplicationDocument previousDocument = null;

        if (isDraft) {
                // Normal document uploads remain restricted to draft applications.
        } else if (isReplacement) {

                String requestedType = application.getRequestedDocumentType();

                if (requestedType == null || requestedType.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "There is no specific document awaiting replacement.");
                }

                if (!requestedType.equals(normalizedType)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can only upload the document requested by the administrator: "
                                + requestedType.replace('_', ' ') + ".");
                }

                previousDocument = documentRepository
                        .findFirstByApplicationIdAndDocumentTypeOrderByUploadedAtDesc(
                                applicationId, normalizedType)
                        .orElse(null);

        } else {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Documents cannot be uploaded because this application is not a draft or awaiting document correction.");
        }

        if (file == null || file.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Please select a file to upload.");
        }

        try {
                DocumentStorageService.StoredFile storedFile =
                        documentStorageService.store(file);

                ApplicationDocument document = new ApplicationDocument(
                        applicationId,
                        normalizedType,
                        storedFile.originalFilename(),
                        storedFile.storageKey(),
                        storedFile.contentType(),
                        storedFile.fileSizeBytes());

                if (isReplacement) {

                if (previousDocument != null) {
                        document.setReplacesDocumentId(previousDocument.getId());
                        previousDocument.markAsSuperseded();
                        documentRepository.save(previousDocument);
                }

                application.completeDocumentReplacement(
                        java.time.LocalDateTime.now());

                historyRepository.save(new ApplicationStatusHistory(
                        applicationId,
                        "ACTION_REQUIRED",
                        "UNDER_REVIEW",
                        applicantId,
                        "Applicant uploaded the requested replacement document: "
                                + normalizedType.replace('_', ' ') + "."));
                }

                ApplicationDocument saved = documentRepository.save(document);

                return toResponse(saved);

        } catch (IOException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The file could not be stored. Check that it is a valid supported file.",
                        exception);
        }
        }

    @Transactional(readOnly = true)
    public List<ApplicationDocumentResponse> getMyDocuments(
            Long applicantId,
            Long applicationId) {

        applicationRepository.findByIdAndApplicantId(applicationId, applicantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Passport application was not found."));

        return documentRepository
                .findAllByApplicationIdOrderByUploadedAtDesc(applicationId)
                .stream()
                .map(ApplicationDocumentService::toResponse)
                .toList();
    }

    private static ApplicationDocumentResponse toResponse(
            ApplicationDocument document) {
        return new ApplicationDocumentResponse(
                document.getId(),
                document.getApplicationId(),
                document.getDocumentType(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getFileSizeBytes(),
                document.getDocumentStatus(),
                document.getUploadedAt(),
                document.getReviewedAt(),
                document.getReviewNote());
    }
@Transactional(readOnly = true)
public DocumentFile loadDocumentFile(Long documentId) {
    ApplicationDocument document = documentRepository
            .findById(documentId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Document not found."));

    try {
        Resource resource =
                documentStorageService.load(document.getStorageKey());

        return new DocumentFile(
                resource,
                document.getContentType(),
                document.getOriginalFilename());

    } catch (IOException exception) {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Document file is unavailable.");
    }
}

public record DocumentFile(
        Resource resource,
        String contentType,
        String filename) {
}
}
