package com.passport.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.passport.dto.ApplicantProfileRequest;
import com.passport.dto.ApplicantProfileResponse;
import com.passport.entity.ApplicantProfile;
import com.passport.repository.ApplicantProfileRepository;

@Service
public class ApplicantProfileService {

    private final ApplicantProfileRepository profileRepository;

    public ApplicantProfileService(
            ApplicantProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public Optional<ApplicantProfileResponse> getProfile(Long userId) {
        return profileRepository.findByUserId(userId)
            .map(ApplicantProfileResponse::from);
    }

    @Transactional
    public ApplicantProfileResponse saveProfile(
            Long userId,
            ApplicantProfileRequest request) {

        ApplicantProfile profile = profileRepository.findByUserId(userId)
            .orElseGet(() -> new ApplicantProfile(
                userId,
                request.firstName(),
                request.middleName(),
                request.lastName(),
                request.dateOfBirth(),
                request.placeOfBirth(),
                request.gender(),
                request.nationality(),
                request.phoneNumber(),
                request.addressLine(),
                request.cityOrTown(),
                request.district(),
                request.region(),
                request.country()
            ));

        profile.updateDetails(
            request.firstName(),
            request.middleName(),
            request.lastName(),
            request.dateOfBirth(),
            request.placeOfBirth(),
            request.gender(),
            request.nationality(),
            request.phoneNumber(),
            request.addressLine(),
            request.cityOrTown(),
            request.district(),
            request.region(),
            request.country()
        );

        return ApplicantProfileResponse.from(
            profileRepository.save(profile)
        );
    }
}
