package com.passport.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.passport.dto.PassportApplicationResponse;
import com.passport.entity.ApplicationStatusHistory;
import com.passport.entity.PassportApplication;
import com.passport.repository.ApplicantProfileRepository;
import com.passport.repository.ApplicationStatusHistoryRepository;
import com.passport.repository.PassportApplicationRepository;

import com.passport.dto.InterviewScheduleResponse;
import com.passport.repository.ApplicationInterviewScheduleRepository;

@Service
public class PassportApplicationService {

    private final PassportApplicationRepository applicationRepository;
    private final ApplicantProfileRepository profileRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final ApplicationInterviewScheduleRepository interviewRepository;

    public PassportApplicationService(
        PassportApplicationRepository applicationRepository,
        ApplicantProfileRepository profileRepository,
        ApplicationStatusHistoryRepository historyRepository,
        ApplicationInterviewScheduleRepository interviewRepository) {

        this.applicationRepository = applicationRepository;
        this.profileRepository = profileRepository;
        this.historyRepository = historyRepository;
        this.interviewRepository = interviewRepository;
    }

    @Transactional
    public PassportApplicationResponse createDraft(Long applicantId) {
        if (applicationRepository.existsByApplicantId(applicantId)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "You already have a passport application. Continue with your existing application."
            );
        }
        if (profileRepository.findByUserId(applicantId).isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Complete your applicant profile before creating an application"
            );
        }

        String applicationNumber =
            "PAS-" + UUID.randomUUID().toString().replace("-", "");

        PassportApplication application =
            new PassportApplication(applicationNumber, applicantId);

        return toResponse(applicationRepository.saveAndFlush(application));
    }

    @Transactional
    public PassportApplicationResponse submitApplication(
            Long applicantId, Long applicationId) {

        PassportApplication application = applicationRepository
            .findByIdAndApplicantId(applicationId, applicantId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Passport application not found"
            ));

        if (profileRepository.findByUserId(applicantId).isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Complete your applicant profile before submitting"
            );
        }

        if (!"DRAFT".equals(application.getApplicationStatus())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Only draft applications can be submitted"
            );
        }

        String previousStatus = application.getApplicationStatus();
        LocalDateTime submittedAt = LocalDateTime.now();

        application.submit(submittedAt);

        historyRepository.save(
            new ApplicationStatusHistory(
                application.getId(),
                previousStatus,
                "SUBMITTED",
                applicantId,
                "Applicant submitted application"
            )
        );

        applicationRepository.flush();

        return toResponse(application);
    }

    @Transactional(readOnly = true)
    public List<PassportApplicationResponse> getMyApplications(
            Long applicantId) {
        return applicationRepository
            .findAllByApplicantIdOrderByIdDesc(applicantId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public PassportApplicationResponse getMyApplication(
            Long applicantId, Long applicationId) {
        PassportApplication application = applicationRepository
            .findByIdAndApplicantId(applicationId, applicantId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Passport application not found"
            ));

        return toResponse(application);
    }

    private PassportApplicationResponse toResponse(
        PassportApplication application) {

    InterviewScheduleResponse interviewSchedule =
            interviewRepository.findByApplicationId(application.getId())
                    .map(InterviewScheduleResponse::from)
                    .orElse(null);

    return PassportApplicationResponse.from(
            application,
            interviewSchedule);
}
}
