package com.passport.dto;

import java.util.Map;

public record AdminDashboardResponse(
    long totalApplications,
    Map<String, Long> applicationsByStatus
) {}
