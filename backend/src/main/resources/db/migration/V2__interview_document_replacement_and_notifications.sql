-- =========================================================
-- V2: Interview scheduling, document replacement and email
-- =========================================================

-- 1. Record the exact document requested from an applicant.
ALTER TABLE passport_applications
ADD COLUMN requested_document_type VARCHAR(60) NULL
AFTER decision_reason;


-- 2. Link a replacement document to its previous document.
ALTER TABLE application_documents
ADD COLUMN replaces_document_id BIGINT NULL
AFTER review_note;

ALTER TABLE application_documents
ADD KEY idx_documents_replaces (replaces_document_id);

ALTER TABLE application_documents
ADD CONSTRAINT fk_documents_replaced_document
FOREIGN KEY (replaces_document_id)
REFERENCES application_documents (id)
ON DELETE SET NULL;


-- 3. Store the current interview schedule for each application.
CREATE TABLE application_interview_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    interview_date DATE NOT NULL,
    interview_time TIME NOT NULL,
    location VARCHAR(255) NOT NULL,
    mode VARCHAR(30) NOT NULL,
    meeting_link VARCHAR(1000) NULL,
    instructions TEXT NULL,
    scheduled_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uq_interview_application (application_id),
    KEY idx_interview_date (interview_date),

    CONSTRAINT fk_interview_application
        FOREIGN KEY (application_id)
        REFERENCES passport_applications (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_interview_scheduled_by
        FOREIGN KEY (scheduled_by)
        REFERENCES users (id),

    CONSTRAINT chk_interview_mode
        CHECK (mode IN ('IN_PERSON', 'ONLINE'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- 4. Queue email notifications for reliable delivery.
CREATE TABLE notification_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    notification_type VARCHAR(60) NOT NULL,
    deduplication_key VARCHAR(150) NOT NULL,
    recipient_email VARCHAR(254) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message_body TEXT NOT NULL,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NULL DEFAULT NULL,
    last_error TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL DEFAULT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uq_notification_deduplication (deduplication_key),
    KEY idx_notification_delivery (
        delivery_status,
        next_attempt_at
    ),
    KEY idx_notification_application (application_id),

    CONSTRAINT fk_notification_application
        FOREIGN KEY (application_id)
        REFERENCES passport_applications (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_notification_delivery_status
        CHECK (
            delivery_status IN (
                'PENDING',
                'PROCESSING',
                'SENT',
                'FAILED'
            )
        ),

    CONSTRAINT chk_notification_attempts
        CHECK (attempts >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
