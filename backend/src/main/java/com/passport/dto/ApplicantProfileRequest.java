package com.passport.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record ApplicantProfileRequest(

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    String firstName,

    @Size(max = 100, message = "Middle name must not exceed 100 characters")
    String middleName,

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    String lastName,

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    LocalDate dateOfBirth,

    @Size(max = 150, message = "Place of birth must not exceed 150 characters")
    String placeOfBirth,

    @Size(max = 30, message = "Gender must not exceed 30 characters")
    String gender,

    @NotBlank(message = "Nationality is required")
    @Size(max = 100, message = "Nationality must not exceed 100 characters")
    String nationality,

    @NotBlank(message = "Phone number is required")
    @Size(max = 30, message = "Phone number must not exceed 30 characters")
    String phoneNumber,

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    String addressLine,

    @NotBlank(message = "City or town is required")
    @Size(max = 100, message = "City or town must not exceed 100 characters")
    String cityOrTown,

    @Size(max = 100, message = "District must not exceed 100 characters")
    String district,

    @Size(max = 100, message = "Region must not exceed 100 characters")
    String region,

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country must not exceed 100 characters")
    String country

) {
}
