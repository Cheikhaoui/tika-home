-- ShedLock: one row per scheduled job. Before running, an instance "takes" the row (lock_until in the
-- future); other instances skip that run. Protects the iCal sync and email sender when several app
-- instances run at once (e.g. during a deploy).
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMPTZ  NOT NULL,
    locked_at  TIMESTAMPTZ  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
