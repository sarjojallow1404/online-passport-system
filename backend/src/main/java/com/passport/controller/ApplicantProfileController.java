package com.passport.controller;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.passport.dto.ApplicantProfileRequest;
import com.passport.dto.ApplicantProfileResponse;
import com.passport.service.ApplicantProfileService;

@RestController
@RequestMapping("/api/applicant/profile")
public class ApplicantProfileController {

    private final ApplicantProfileService profileService;

    public ApplicantProfileController(
            ApplicantProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<?> getProfile(
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = getUserId(jwt);

        return profileService.getProfile(userId)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Applicant profile has not been created yet")));
    }

    @PutMapping
    public ApplicantProfileResponse saveProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ApplicantProfileRequest request) {

        return profileService.saveProfile(getUserId(jwt), request);
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

    @org.springframework.web.bind.annotation.ExceptionHandler(
            MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRequest(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .findFirst()
            .map(error -> error.getDefaultMessage())
            .orElse("Invalid profile details");

        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(
            HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidJson(
            HttpMessageNotReadableException exception) {

        return ResponseEntity.badRequest().body(
            Map.of("error", "Invalid or missing JSON request body")
        );
    }
}
