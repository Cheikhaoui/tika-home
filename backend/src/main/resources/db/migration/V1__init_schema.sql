-- btree_gist lets a GiST index mix "=" on plain columns (property_id) with "&&" on ranges.
-- Needed for the anti-overlap exclusion constraint added in V2.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE property (
    id                 UUID PRIMARY KEY,
    slug               VARCHAR(80)    NOT NULL UNIQUE,
    name               VARCHAR(200)   NOT NULL,
    max_guests         INTEGER        NOT NULL CHECK (max_guests > 0),
    base_nightly_price NUMERIC(10, 2) NOT NULL CHECK (base_nightly_price >= 0),
    cleaning_fee       NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (cleaning_fee >= 0),
    currency           VARCHAR(3)     NOT NULL,
    min_nights         INTEGER        NOT NULL DEFAULT 1 CHECK (min_nights > 0),
    time_zone          VARCHAR(64)    NOT NULL DEFAULT 'Africa/Casablanca',
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TABLE guest (
    id                 UUID PRIMARY KEY,
    full_name          VARCHAR(200) NOT NULL,
    email              VARCHAR(320) NOT NULL,
    phone              VARCHAR(40),
    preferred_language VARCHAR(5)   NOT NULL DEFAULT 'en',
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_guest_email ON guest (email);

CREATE TABLE booking (
    id          UUID PRIMARY KEY,
    property_id UUID           NOT NULL REFERENCES property (id),
    guest_id    UUID           NOT NULL REFERENCES guest (id),
    check_in    DATE           NOT NULL,
    check_out   DATE           NOT NULL,
    guests      INTEGER        NOT NULL CHECK (guests > 0),
    status      VARCHAR(20)    NOT NULL
        CHECK (status IN ('REQUESTED', 'CONFIRMED', 'DECLINED', 'CANCELLED')),
    total_price NUMERIC(10, 2) NOT NULL CHECK (total_price >= 0),
    currency    VARCHAR(3)     NOT NULL,
    message     VARCHAR(2000),
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    version     BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT booking_dates_valid CHECK (check_out > check_in)
);

-- Blocked periods: owner blocks (MANUAL) and, from phase 2, reservations imported
-- from Airbnb / Booking.com / ... calendars (ICAL). Overlaps between blocks are allowed on purpose:
-- several channels often export the same dates, and two blocks still just mean "unavailable".
CREATE TABLE blocked_date (
    id           UUID PRIMARY KEY,
    property_id  UUID         NOT NULL REFERENCES property (id),
    start_date   DATE         NOT NULL,
    end_date     DATE         NOT NULL,
    reason       VARCHAR(200),
    source       VARCHAR(20)  NOT NULL CHECK (source IN ('MANUAL', 'ICAL')),
    channel_id   UUID,         -- FK to a channel table added in phase 2
    external_uid VARCHAR(255), -- iCal VEVENT UID, used to update/remove imported events
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT blocked_date_range_valid CHECK (end_date > start_date)
);

CREATE INDEX idx_blocked_date_property_range
    ON blocked_date USING gist (property_id, daterange(start_date, end_date, '[)'));

CREATE TABLE price_rule (
    id            UUID PRIMARY KEY,
    property_id   UUID           NOT NULL REFERENCES property (id),
    name          VARCHAR(100)   NOT NULL,
    start_date    DATE           NOT NULL,
    end_date      DATE           NOT NULL,
    nightly_price NUMERIC(10, 2) NOT NULL CHECK (nightly_price >= 0),
    min_nights    INTEGER CHECK (min_nights > 0), -- NULL = use the property default
    priority      INTEGER        NOT NULL DEFAULT 0, -- highest wins when rules overlap
    CONSTRAINT price_rule_range_valid CHECK (end_date > start_date)
);

CREATE INDEX idx_price_rule_property ON price_rule (property_id, start_date, end_date);
