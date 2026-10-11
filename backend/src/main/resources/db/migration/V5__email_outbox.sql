-- Transactional outbox: emails are inserted in the SAME transaction as the business change
-- (e.g. the booking), then sent asynchronously by a job with retries. A booking can never fail
-- because of email, and an email is never lost if the mail server is down.
CREATE TABLE email_outbox (
    id              UUID PRIMARY KEY,
    type            VARCHAR(50)   NOT NULL,
    recipient       VARCHAR(320)  NOT NULL,
    language        VARCHAR(5)    NOT NULL,
    subject         VARCHAR(300)  NOT NULL,
    html_body       TEXT          NOT NULL, -- rendered when queued: a snapshot of the data at that time
    dedupe_key      VARCHAR(200)  UNIQUE,   -- optional: the same alert is never queued twice
    status          VARCHAR(20)   NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts        INTEGER       NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ   NOT NULL,
    last_error      VARCHAR(1000),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    sent_at         TIMESTAMPTZ
);

-- The sender only ever looks for due PENDING rows: a small partial index.
CREATE INDEX idx_email_outbox_due ON email_outbox (next_attempt_at) WHERE status = 'PENDING';
