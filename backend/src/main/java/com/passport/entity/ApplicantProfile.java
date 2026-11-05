package com.passport.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "applicant_profiles")
public class ApplicantProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "place_of_birth", length = 150)
    private String placeOfBirth;

    @Column(length = 30)
    private String gender;

    @Column(nullable = false, length = 100)
    private String nationality;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @Column(name = "address_line", nullable = false, length = 255)
    private String addressLine;

    @Column(name = "city_or_town", nullable = false, length = 100)
    private String cityOrTown;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String region;

    @Column(nullable = false, length = 100)
    private String country = "The Gambia";

    protected ApplicantProfile() {
    }

    public ApplicantProfile(
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
            String country) {
        this.userId = userId;
        updateDetails(
            firstName, middleName, lastName, dateOfBirth,
            placeOfBirth, gender, nationality, phoneNumber,
            addressLine, cityOrTown, district, region, country
        );
    }

    public void updateDetails(
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
            String country) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.placeOfBirth = placeOfBirth;
        this.gender = gender;
        this.nationality = nationality;
        this.phoneNumber = phoneNumber;
        this.addressLine = addressLine;
        this.cityOrTown = cityOrTown;
        this.district = district;
        this.region = region;
        this.country = country;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getFirstName() { return firstName; }
    public String getMiddleName() { return middleName; }
    public String getLastName() { return lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getPlaceOfBirth() { return placeOfBirth; }
    public String getGender() { return gender; }
    public String getNationality() { return nationality; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAddressLine() { return addressLine; }
    public String getCityOrTown() { return cityOrTown; }
    public String getDistrict() { return district; }
    public String getRegion() { return region; }
    public String getCountry() { return country; }
}
