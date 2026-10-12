-- Owner accounts for the admin dashboard. Passwords are BCrypt hashes, never plain text.
CREATE TABLE admin_user (
    id              UUID PRIMARY KEY,
    email           VARCHAR(320) NOT NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,
    failed_attempts INTEGER      NOT NULL DEFAULT 0, -- consecutive wrong passwords
    locked_until    TIMESTAMPTZ,                     -- brute-force protection
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
