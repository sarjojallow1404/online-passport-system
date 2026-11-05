-- Track when an outbox notification enters PROCESSING.
ALTER TABLE notification_outbox
ADD COLUMN processing_started_at TIMESTAMP NULL
AFTER next_attempt_at;

-- Index to help locate notifications stuck in PROCESSING.
CREATE INDEX idx_notification_processing
ON notification_outbox (delivery_status, processing_started_at);
