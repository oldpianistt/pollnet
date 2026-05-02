-- Faz 6: production features (notifications, email verification, password reset, search indexes).

-- =====================================================================
-- email verification
-- =====================================================================
ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMPTZ NULL;

CREATE TABLE email_verifications (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_email_verif_user ON email_verifications(user_id);

-- =====================================================================
-- password reset
-- =====================================================================
CREATE TABLE password_resets (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_pwd_reset_user ON password_resets(user_id);

-- =====================================================================
-- notifications
-- =====================================================================
CREATE TABLE notifications (
    id           UUID         PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,  -- recipient
    type         VARCHAR(32)  NOT NULL,
    payload      JSONB        NOT NULL,
    read_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT notifications_type_chk
        CHECK (type IN ('NEW_FOLLOWER','POLL_ANSWERED','POLL_COMMENTED'))
);
CREATE INDEX idx_notifications_user_created    ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_user_unread     ON notifications(user_id) WHERE read_at IS NULL;

-- =====================================================================
-- Search indexes (ILIKE / case-insensitive substring)
-- pg_trgm gives us GIN indexes for ILIKE '%term%' patterns.
-- =====================================================================
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_polls_title_trgm    ON polls    USING gin (title    gin_trgm_ops);
CREATE INDEX idx_users_username_trgm ON users    USING gin (username gin_trgm_ops);
CREATE INDEX idx_users_display_trgm  ON users    USING gin (display_name gin_trgm_ops);
