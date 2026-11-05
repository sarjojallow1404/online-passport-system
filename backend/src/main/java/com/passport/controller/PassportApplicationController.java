package com.passport.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.passport.dto.PassportApplicationResponse;
import com.passport.service.PassportApplicationService;

@RestController
@RequestMapping("/api/applicant/applications")
public class PassportApplicationController {

    private final PassportApplicationService applicationService;

    public PassportApplicationController(
            PassportApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<?> createDraft(
            @AuthenticationPrincipal Jwt jwt) {
        try {
            PassportApplicationResponse response =
                applicationService.createDraft(getUserId(jwt));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(
                Map.of("error", exception.getReason() == null
                    ? "Unable to create application"
                    : exception.getReason())
            );
        }
    }

    @GetMapping
    public List<PassportApplicationResponse> getMyApplications(
            @AuthenticationPrincipal Jwt jwt) {
        return applicationService.getMyApplications(getUserId(jwt));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submitApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(
                applicationService.submitApplication(getUserId(jwt), id)
            );
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(
                Map.of("error", exception.getReason() == null
                    ? "Unable to submit application"
                    : exception.getReason())
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMyApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(
                applicationService.getMyApplication(getUserId(jwt), id)
            );
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(
                Map.of("error", exception.getReason() == null
                    ? "Passport application not found"
                    : exception.getReason())
            );
        }
    }

    private Long getUserId(Jwt jwt) {
        Object claim = jwt.getClaim("uid");

        if (claim instanceof Number number) {
            return number.longValue();
        }

        throw new IllegalStateException(
            "Authenticated token does not contain a valid user ID"
        );
    }
}
