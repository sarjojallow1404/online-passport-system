package com.passport.dto;

import java.time.LocalDate;

import com.passport.entity.ApplicantProfile;

public record ApplicantProfileResponse(
    Long id,
    Long userId,
    String firstName,
    String middleName,
    String lastName,
    LocalDate dateOfBirth,
    String placeOfBirth,
    String gender,
    String nationality,
    String phoneNumber,
    String addressLine,
    String cityOrTown,
    String district,
    String region,
    String country
) {
    public static ApplicantProfileResponse from(ApplicantProfile profile) {
        return new ApplicantProfileResponse(
            profile.getId(),
            profile.getUserId(),
            profile.getFirstName(),
            profile.getMiddleName(),
            profile.getLastName(),
            profile.getDateOfBirth(),
            profile.getPlaceOfBirth(),
            profile.getGender(),
            profile.getNationality(),
            profile.getPhoneNumber(),
            profile.getAddressLine(),
            profile.getCityOrTown(),
            profile.getDistrict(),
            profile.getRegion(),
            profile.getCountry()
        );
    }
}
