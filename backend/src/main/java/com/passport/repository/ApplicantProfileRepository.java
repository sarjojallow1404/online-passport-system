package com.passport.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.passport.entity.ApplicantProfile;

public interface ApplicantProfileRepository
        extends JpaRepository<ApplicantProfile, Long> {

    Optional<ApplicantProfile> findByUserId(Long userId);
}
