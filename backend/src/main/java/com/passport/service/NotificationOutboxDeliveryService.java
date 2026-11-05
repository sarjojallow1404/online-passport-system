package com.passport.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.passport.entity.NotificationOutbox;
import com.passport.repository.NotificationOutboxRepository;

@Service
public class NotificationOutboxDeliveryService {

    private static final int MAX_ATTEMPTS = 5;

    private final NotificationOutboxRepository notificationRepository;

    public NotificationOutboxDeliveryService(
            NotificationOutboxRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void markAsSent(Long notificationId) {
        NotificationOutbox notification = findNotification(notificationId);

        notification.markSent(LocalDateTime.now());
    }

    @Transactional
    public void markAsFailed(Long notificationId, Exception exception) {
        NotificationOutbox notification = findNotification(notificationId);

        int nextAttempt = notification.getAttempts() + 1;
        boolean retryable = nextAttempt < MAX_ATTEMPTS;

        LocalDateTime retryAt = null;

        if (retryable) {
            long delayMinutes = 1L << (nextAttempt - 1);
            retryAt = LocalDateTime.now().plusMinutes(delayMinutes);
        }

        String error = exception.getMessage();

        if (error == null || error.isBlank()) {
            error = exception.getClass().getSimpleName();
        }

        notification.markFailed(error, retryAt, retryable);
    }

    private NotificationOutbox findNotification(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Notification not found: " + notificationId));
    }
}
