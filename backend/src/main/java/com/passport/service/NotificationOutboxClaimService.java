package com.passport.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.passport.entity.NotificationOutbox;
import com.passport.repository.NotificationOutboxRepository;

@Service
public class NotificationOutboxClaimService {

    private final NotificationOutboxRepository notificationRepository;

    public NotificationOutboxClaimService(
            NotificationOutboxRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Optional<Long> claimNextNotification() {
        LocalDateTime now = LocalDateTime.now();

        Optional<NotificationOutbox> notification =
                notificationRepository.claimNextPendingNotification(now);

        if (notification.isEmpty()) {
            return Optional.empty();
        }

        NotificationOutbox claimed = notification.get();
        claimed.markProcessing(now);

        notificationRepository.save(claimed);

        return Optional.of(claimed.getId());
    }
}
