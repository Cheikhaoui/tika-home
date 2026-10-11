-- Lets the sync alert the owner once after N consecutive failures, and once when it recovers.
ALTER TABLE channel
    ADD COLUMN consecutive_failures INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN failure_alert_sent   BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN last_success_at      TIMESTAMPTZ;
