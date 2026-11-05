package com.passport.controller;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.passport.dto.ApplicationDocumentResponse;
import com.passport.service.ApplicationDocumentService;

@RestController
@RequestMapping("/api/applicant/applications/{applicationId}/documents")
public class ApplicationDocumentController {

    private final ApplicationDocumentService documentService;

    public ApplicationDocumentController(
            ApplicationDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<?> uploadDocument(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long applicationId,
        @RequestParam("documentType") String documentType,
        @RequestParam("file") MultipartFile file) {

        try {
            Long applicantId = getUserId(jwt);

            ApplicationDocumentResponse response =
                    documentService.uploadDocument(
                            applicantId, applicationId, documentType, file);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode())
                    .body(java.util.Map.of(
                            "error",
                            exception.getReason() == null
                                    ? "The document request could not be completed."
                                    : exception.getReason()));
        }
    }

   @GetMapping
public ResponseEntity<?> getDocuments(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long applicationId) {
        try {
            Long applicantId = getUserId(jwt);

            List<ApplicationDocumentResponse> documents =
                    documentService.getMyDocuments(applicantId, applicationId);

            return ResponseEntity.ok(documents);

        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode())
                    .body(java.util.Map.of(
                            "error",
                            exception.getReason() == null
                                    ? "The document request could not be completed."
                                    : exception.getReason()));
        }
    }

    private Long getUserId(Jwt jwt) {
        Object uid = jwt.getClaim("uid");

        if (uid instanceof Number number) {
            return number.longValue();
        }

        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid authentication token.");
    }
}
