package com.passport.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.passport.entity.NotificationOutbox;
import com.passport.repository.NotificationOutboxRepository;

import org.springframework.beans.factory.annotation.Value;
@Component
@ConditionalOnProperty(
        prefix = "passport.notifications",
        name = "enabled",
        havingValue = "true")
public class NotificationOutboxWorker {

    private static final Logger logger =
            LoggerFactory.getLogger(NotificationOutboxWorker.class);

    private static final int MAX_NOTIFICATIONS_PER_CYCLE = 10;

    private final NotificationOutboxClaimService claimService;
    private final NotificationOutboxDeliveryService deliveryService;
    private final NotificationOutboxRecoveryService recoveryService;
    private final NotificationOutboxRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final String mailFrom;

    public NotificationOutboxWorker(
        NotificationOutboxClaimService claimService,
        NotificationOutboxDeliveryService deliveryService,
        NotificationOutboxRecoveryService recoveryService,
        NotificationOutboxRepository notificationRepository,
        JavaMailSender mailSender,
        @Value("${MAIL_FROM:}") String mailFrom) {

        this.claimService = claimService;
        this.deliveryService = deliveryService;
        this.recoveryService = recoveryService;
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;

        if (mailFrom == null || mailFrom.isBlank()) {
            throw new IllegalStateException(
                    "MAIL_FROM must be configured when notifications are enabled.");
        }

        this.mailFrom = mailFrom;
    }

    /**
     * Processes eligible pending notifications.
     */
    @Scheduled(
            fixedDelayString =
                    "${passport.notifications.poll-interval-ms:5000}")
    public void processPendingNotifications() {

        for (int i = 0; i < MAX_NOTIFICATIONS_PER_CYCLE; i++) {

            Optional<Long> claimedId =
                    claimService.claimNextNotification();

            if (claimedId.isEmpty()) {
                return;
            }

            processNotification(claimedId.get());
        }
    }

    /**
     * Recovers notifications left in PROCESSING after a crash.
     */
    @Scheduled(
            fixedDelayString =
                    "${passport.notifications.recovery-interval-ms:60000}")
    public void recoverStaleNotifications() {

        try {
            int recovered = recoveryService.recoverStaleNotifications();

            if (recovered > 0) {
                logger.info(
                        "Recovered {} stale notification(s).",
                        recovered);
            }

        } catch (Exception exception) {
            logger.error(
                    "Notification recovery failed.",
                    exception);
        }
    }

    private void processNotification(Long notificationId) {

        NotificationOutbox notification = notificationRepository
                .findById(notificationId)
                .orElse(null);

        if (notification == null) {
            logger.error(
                    "Claimed notification {} could not be found.",
                    notificationId);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setTo(notification.getRecipientEmail());
            message.setFrom(mailFrom);
            message.setSubject(notification.getSubject());
            message.setText(notification.getMessageBody());

            mailSender.send(message);

            deliveryService.markAsSent(notificationId);

            logger.info(
                    "Notification {} sent successfully.",
                    notificationId);

        } catch (Exception exception) {

            logger.error(
                    "Failed to send notification {}: {}",
                    notificationId,
                    exception.getMessage());

            try {
                deliveryService.markAsFailed(
                        notificationId,
                        exception);

            } catch (Exception updateException) {
                logger.error(
                        "Could not update failed notification {}.",
                        notificationId,
                        updateException);
            }
        }
    }
}
