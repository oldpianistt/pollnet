-- Faz 7: avatar URL on users + direct messaging tables.

-- =====================================================================
-- avatar
-- =====================================================================
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(255) NULL;

-- =====================================================================
-- direct messaging — server-side encrypted (NOT E2EE).
-- Server stores ciphertext only as a defence-in-depth measure (see
-- MessageCipher.java). Real end-to-end encryption is intentionally not
-- implemented here; that requires Signal-protocol-grade work.
-- =====================================================================

-- A conversation = a 1-1 thread between two users. Group DMs are out of scope.
CREATE TABLE conversations (
    id              UUID         PRIMARY KEY,
    user_a_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_b_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    last_message_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    -- One conversation per ordered pair. We canonicalise so user_a_id < user_b_id
    -- (lexicographic UUID order) before insert; that gives us a single row per pair.
    CONSTRAINT conversations_pair_uk    UNIQUE (user_a_id, user_b_id),
    CONSTRAINT conversations_ordered_chk CHECK (user_a_id < user_b_id),
    CONSTRAINT conversations_distinct_chk CHECK (user_a_id <> user_b_id)
);
CREATE INDEX idx_conversations_user_a   ON conversations(user_a_id, last_message_at DESC);
CREATE INDEX idx_conversations_user_b   ON conversations(user_b_id, last_message_at DESC);

-- Each message: ciphertext of body + sender + read_at on the recipient.
CREATE TABLE messages (
    id               UUID          PRIMARY KEY,
    conversation_id  UUID          NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id        UUID          NOT NULL REFERENCES users(id)         ON DELETE CASCADE,
    -- ciphertext = base64(IV || AES-GCM(body)); length capped to 8 KB.
    ciphertext       VARCHAR(8192) NOT NULL,
    attachment_url   VARCHAR(255)  NULL,
    read_at          TIMESTAMPTZ   NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_messages_convo_created ON messages(conversation_id, created_at DESC);
CREATE INDEX idx_messages_unread_per_recipient ON messages(conversation_id, sender_id) WHERE read_at IS NULL;

-- last_seen_at on users so we can show "online ~now" / "5 dk önce".
ALTER TABLE users ADD COLUMN last_seen_at TIMESTAMPTZ NULL;
