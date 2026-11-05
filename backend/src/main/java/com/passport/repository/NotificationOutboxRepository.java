
package com.passport.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.passport.entity.NotificationOutbox;

public interface NotificationOutboxRepository
        extends JpaRepository<NotificationOutbox, Long> {

    /**
     * Claims the oldest eligible notification.
     * Must execute inside a transaction.
     */
    @Query(value = """
        SELECT *
        FROM notification_outbox
        WHERE delivery_status = 'PENDING'
          AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
        ORDER BY created_at ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<NotificationOutbox> claimNextPendingNotification(
            @Param("now") LocalDateTime now);

    /**
     * Finds notifications that have remained PROCESSING
     * beyond the recovery threshold.
     * Must execute inside a transaction.
     */
    @Query(value = """
        SELECT *
        FROM notification_outbox
        WHERE delivery_status = 'PROCESSING'
          AND processing_started_at IS NOT NULL
          AND processing_started_at <= :cutoff
        ORDER BY processing_started_at ASC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<NotificationOutbox> findStaleProcessingNotifications(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("batchSize") int batchSize);
}
