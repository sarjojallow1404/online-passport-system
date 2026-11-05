
package com.passport.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.passport.dto.AdminApplicationDetailResponse;
import com.passport.dto.AdminApplicationListItem;
import com.passport.dto.AdminApplicationStatusUpdateRequest;
import com.passport.dto.AdminDashboardResponse;
import com.passport.dto.AdminDocumentReviewRequest;
import com.passport.dto.InterviewScheduleRequest;

import com.passport.service.AdminApplicationDetailService;
import com.passport.service.AdminDashboardService;
import com.passport.service.ApplicationDocumentService;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;
    private final AdminApplicationDetailService detailService;
    private final ApplicationDocumentService documentService;

    public AdminDashboardController(
            AdminDashboardService dashboardService,
            AdminApplicationDetailService detailService,
            ApplicationDocumentService documentService) {

        this.dashboardService = dashboardService;
        this.detailService = detailService;
        this.documentService = documentService;
    }

    @PatchMapping("/documents/{documentId}/review")
    public AdminApplicationDetailResponse reviewDocument(
            @PathVariable Long documentId,
            @RequestBody AdminDocumentReviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Long adminId = Long.parseLong(jwt.getClaimAsString("uid"));

        return detailService.reviewDocument(
                documentId,
                adminId,
                request.status(),
                request.note()
        );
    }

    @GetMapping("/documents/{documentId}/file")
    public ResponseEntity<Resource> viewDocument(
            @PathVariable Long documentId) {

        ApplicationDocumentService.DocumentFile file =
                documentService.loadDocumentFile(documentId);

        MediaType mediaType;

        try {
            mediaType = MediaType.parseMediaType(file.contentType());
        } catch (IllegalArgumentException exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        if (!mediaType.equals(MediaType.APPLICATION_PDF)
                && !mediaType.equals(MediaType.IMAGE_JPEG)
                && !mediaType.equals(MediaType.IMAGE_PNG)) {

            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ContentDisposition disposition = ContentDisposition
                .inline()
                .filename(file.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "private, no-store")
                .body(file.resource());
    }

    @PatchMapping("/applications/{id}/status")
    public AdminApplicationDetailResponse updateApplicationStatus(
            @PathVariable Long id,
            @RequestBody AdminApplicationStatusUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Long adminId = Long.parseLong(jwt.getClaimAsString("uid"));

        return detailService.updateApplicationStatus(
                id,
                adminId,
                request
        );
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse getDashboard() {
        return dashboardService.getDashboard();
    }

    @GetMapping("/applications/{id}")
    public AdminApplicationDetailResponse getApplication(
            @PathVariable Long id) {

        return detailService.getApplication(id);
    }

    @GetMapping("/applications")
    public Page<AdminApplicationListItem> getApplications(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return dashboardService.searchApplications(
                search,
                status,
                page,
                size
        );
    }

    @PutMapping("/applications/{id}/interview")
    public AdminApplicationDetailResponse rescheduleInterview(
            @PathVariable Long id,
            @RequestBody InterviewScheduleRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Long adminId = Long.parseLong(jwt.getClaimAsString("uid"));

        return detailService.rescheduleInterview(
                id,
                adminId,
                request
        );
    }
}
