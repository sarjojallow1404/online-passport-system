package com.passport.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.passport.entity.NotificationOutbox;
import com.passport.repository.NotificationOutboxRepository;

@Service
public class NotificationOutboxRecoveryService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 50;
    private static final int STALE_AFTER_MINUTES = 10;

    private static final String RECOVERY_ERROR =
            "Notification processing timed out. Recovered automatically.";

    private final NotificationOutboxRepository notificationRepository;

    public NotificationOutboxRecoveryService(
            NotificationOutboxRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public int recoverStaleNotifications() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(STALE_AFTER_MINUTES);

        List<NotificationOutbox> staleNotifications =
                notificationRepository.findStaleProcessingNotifications(
                        cutoff,
                        BATCH_SIZE);

        for (NotificationOutbox notification : staleNotifications) {

            int nextAttempt = notification.getAttempts() + 1;
            boolean retryable = nextAttempt < MAX_ATTEMPTS;

            LocalDateTime retryAt = null;

            if (retryable) {
                long delayMinutes = 1L << (nextAttempt - 1);
                retryAt = now.plusMinutes(delayMinutes);
            }

            notification.recoverForRetry(
                    retryAt,
                    RECOVERY_ERROR,
                    retryable);
        }

        return staleNotifications.size();
    }
}
