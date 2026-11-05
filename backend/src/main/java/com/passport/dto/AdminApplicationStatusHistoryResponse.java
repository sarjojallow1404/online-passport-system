package com.passport.dto;

import java.time.LocalDateTime;

import com.passport.entity.ApplicationStatusHistory;

public record AdminApplicationStatusHistoryResponse(
    Long id,
    String previousStatus,
    String newStatus,
    Long changedBy,
    String changeNote,
    LocalDateTime changedAt
) {
    public static AdminApplicationStatusHistoryResponse from(
            ApplicationStatusHistory history) {
        return new AdminApplicationStatusHistoryResponse(
            history.getId(),
            history.getPreviousStatus(),
            history.getNewStatus(),
            history.getChangedBy(),
            history.getChangeNote(),
            history.getChangedAt()
        );
    }
}
