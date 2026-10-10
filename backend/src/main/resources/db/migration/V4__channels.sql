-- External booking platforms (Airbnb, Booking.com, ...) whose iCal calendars we import.
CREATE TABLE channel (
    id                  UUID PRIMARY KEY,
    property_id         UUID          NOT NULL REFERENCES property (id),
    type                VARCHAR(20)   NOT NULL CHECK (type IN ('AIRBNB', 'BOOKING_COM', 'VRBO', 'OTHER')),
    name                VARCHAR(100)  NOT NULL,
    import_url          VARCHAR(2000) NOT NULL, -- contains a secret token: never log or expose it
    enabled             BOOLEAN       NOT NULL DEFAULT TRUE,
    last_synced_at      TIMESTAMPTZ,
    last_sync_status    VARCHAR(20) CHECK (last_sync_status IN ('SUCCESS', 'FAILED')),
    last_error          VARCHAR(1000),
    last_conflict_count INTEGER       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_channel_property ON channel (property_id);

-- Imported blocks belong to a channel and are identified by the iCal event UID,
-- so a re-sync updates or removes them instead of creating duplicates.
ALTER TABLE blocked_date
    ADD CONSTRAINT fk_blocked_date_channel FOREIGN KEY (channel_id) REFERENCES channel (id) ON DELETE CASCADE,
    ADD CONSTRAINT uq_blocked_date_channel_uid UNIQUE (channel_id, external_uid),
    ADD CONSTRAINT blocked_date_ical_has_channel
        CHECK (source <> 'ICAL' OR (channel_id IS NOT NULL AND external_uid IS NOT NULL));

-- Secret used in the public export URL (/ical/{token}.ics) so the calendar can't be guessed.
-- Default = two random UUIDs without dashes (64 hex characters); existing rows get one too.
ALTER TABLE property
    ADD COLUMN ical_export_token VARCHAR(64) NOT NULL
        DEFAULT (replace(gen_random_uuid()::text, '-', '') || replace(gen_random_uuid()::text, '-', '')),
    ADD CONSTRAINT uq_property_ical_export_token UNIQUE (ical_export_token);
