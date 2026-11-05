
package com.passport.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.passport.dto.AdminApplicationListItem;
import com.passport.dto.AdminDashboardResponse;
import com.passport.repository.PassportApplicationRepository;

@Service
public class AdminDashboardService {

    private final PassportApplicationRepository applicationRepository;

    public AdminDashboardService(
            PassportApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        Map<String, Long> counts = new LinkedHashMap<>();

        counts.put("SUBMITTED",
                applicationRepository.countByApplicationStatus("SUBMITTED"));
        counts.put("UNDER_REVIEW",
                applicationRepository.countByApplicationStatus("UNDER_REVIEW"));
        counts.put("ACTION_REQUIRED",
                applicationRepository.countByApplicationStatus("ACTION_REQUIRED"));
        counts.put("READY_FOR_INTERVIEW",
                applicationRepository.countByApplicationStatus("READY_FOR_INTERVIEW"));
        counts.put("REJECTED",
                applicationRepository.countByApplicationStatus("REJECTED"));
        counts.put("DRAFT",
                applicationRepository.countByApplicationStatus("DRAFT"));

        long totalApplications =
                applicationRepository.countByApplicationStatusNot("DRAFT");

        return new AdminDashboardResponse(totalApplications, counts);
    }

    @Transactional(readOnly = true)
    public Page<AdminApplicationListItem> searchApplications(
            String search,
            String status,
            int page,
            int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        String normalizedSearch =
                search == null || search.isBlank() ? null : search.trim();

        String normalizedStatus =
                status == null || status.isBlank() ? null : status.trim();

        return applicationRepository.searchForAdmin(
                normalizedSearch,
                normalizedStatus,
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(Sort.Direction.DESC, "id")
                )
        );
    }
}
