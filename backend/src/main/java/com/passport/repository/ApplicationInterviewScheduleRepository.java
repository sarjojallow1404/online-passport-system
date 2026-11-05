package com.passport.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.passport.entity.ApplicationInterviewSchedule;

public interface ApplicationInterviewScheduleRepository
        extends JpaRepository<ApplicationInterviewSchedule, Long> {

    Optional<ApplicationInterviewSchedule> findByApplicationId(Long applicationId);
}
