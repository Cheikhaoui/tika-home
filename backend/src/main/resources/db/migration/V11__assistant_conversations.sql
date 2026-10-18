-- AI guest assistant logs (phase 7): what guests asked, what the assistant answered, and what it cost.
-- Used for the owner's "Conversations" page, rate limits and the daily budget.
-- Privacy: no IP address is stored, only an HMAC of it (visitor_hash) to group a visitor's messages.
CREATE TABLE assistant_conversation (
    id              UUID PRIMARY KEY,
    property_id     UUID          NOT NULL REFERENCES property (id),
    visitor_hash    VARCHAR(64)   NOT NULL,
    language        VARCHAR(5)    NOT NULL,             -- language of the latest guest message
    guest_messages  INTEGER       NOT NULL DEFAULT 0,
    cost_usd        NUMERIC(12,6) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL,
    last_message_at TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_assistant_conversation_recent ON assistant_conversation (last_message_at DESC);
CREATE INDEX idx_assistant_conversation_visitor ON assistant_conversation (visitor_hash);

CREATE TABLE assistant_message (
    id                 UUID PRIMARY KEY,
    conversation_id    UUID          NOT NULL REFERENCES assistant_conversation (id) ON DELETE CASCADE,
    role               VARCHAR(10)   NOT NULL CHECK (role IN ('GUEST', 'ASSISTANT')),
    text               TEXT          NOT NULL,
    tools_used         VARCHAR(200),                    -- e.g. "check_stay,find_available_dates"
    refused            BOOLEAN       NOT NULL DEFAULT false,
    failed             BOOLEAN       NOT NULL DEFAULT false, -- Claude error / guest left: no answer
    input_tokens       INTEGER       NOT NULL DEFAULT 0,
    output_tokens      INTEGER       NOT NULL DEFAULT 0,
    cache_read_tokens  INTEGER       NOT NULL DEFAULT 0,
    cache_write_tokens INTEGER       NOT NULL DEFAULT 0,
    cost_usd           NUMERIC(12,6) NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_assistant_message_conversation ON assistant_message (conversation_id, created_at);
CREATE INDEX idx_assistant_message_created ON assistant_message (created_at); -- daily budget, hourly limit
