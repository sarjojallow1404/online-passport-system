package com.passport.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.passport.entity.ApplicationStatusHistory;

public interface ApplicationStatusHistoryRepository
        extends JpaRepository<ApplicationStatusHistory, Long> {

    java.util.List<ApplicationStatusHistory>
        findAllByApplicationIdOrderByChangedAtDesc(Long applicationId);
}
