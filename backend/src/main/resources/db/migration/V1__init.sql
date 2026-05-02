-- PollNet — initial schema
-- Tüm UUID'ler uygulama tarafından üretilir (UUID v7 idealdir, JPA katmanında ayarlı).

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =====================================================================
-- users
-- =====================================================================
CREATE TABLE users (
    id                       UUID            PRIMARY KEY,
    username                 VARCHAR(32)     NOT NULL UNIQUE,
    email                    VARCHAR(255)    NOT NULL UNIQUE,
    password_hash            VARCHAR(255)    NOT NULL,
    display_name             VARCHAR(64),
    bio                      TEXT,
    invited_by               UUID            REFERENCES users(id) ON DELETE SET NULL,
    invite_quota             INT             NOT NULL DEFAULT 10,
    invite_quota_reset_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT users_username_format CHECK (username ~ '^[a-zA-Z0-9_]{3,32}$')
);

CREATE INDEX idx_users_invited_by ON users(invited_by);

-- =====================================================================
-- invitations
-- =====================================================================
CREATE TABLE invitations (
    id           UUID         PRIMARY KEY,
    token        VARCHAR(64)  NOT NULL UNIQUE,
    inviter_id   UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    used_by      UUID         REFERENCES users(id) ON DELETE SET NULL,
    used_at      TIMESTAMPTZ,
    expires_at   TIMESTAMPTZ  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invitations_inviter ON invitations(inviter_id, created_at DESC);
CREATE INDEX idx_invitations_unused  ON invitations(inviter_id) WHERE used_by IS NULL;

-- =====================================================================
-- polls
-- =====================================================================
CREATE TABLE polls (
    id                          UUID         PRIMARY KEY,
    author_id                   UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title                       VARCHAR(280) NOT NULL,
    description                 TEXT,
    results_visibility          VARCHAR(16)  NOT NULL,
    open_answers_visibility     VARCHAR(16)  NOT NULL,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT polls_results_visibility_chk
        CHECK (results_visibility IN ('AFTER_VOTE','ALWAYS','AUTHOR_ONLY')),
    CONSTRAINT polls_open_answers_visibility_chk
        CHECK (open_answers_visibility IN ('PUBLIC','AUTHOR_ONLY'))
);

CREATE INDEX idx_polls_author_created ON polls(author_id, created_at DESC);
CREATE INDEX idx_polls_created        ON polls(created_at DESC);

-- =====================================================================
-- questions
-- =====================================================================
CREATE TABLE questions (
    id        UUID         PRIMARY KEY,
    poll_id   UUID         NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    type      VARCHAR(24)  NOT NULL,
    prompt    VARCHAR(500) NOT NULL,
    payload   JSONB        NOT NULL,
    position  INT          NOT NULL,
    CONSTRAINT questions_type_chk
        CHECK (type IN ('SINGLE','MULTIPLE','LIKERT','RANKING','OPEN'))
);

CREATE UNIQUE INDEX idx_questions_poll_position ON questions(poll_id, position);

-- =====================================================================
-- answers
-- =====================================================================
CREATE TABLE answers (
    id           UUID         PRIMARY KEY,
    question_id  UUID         NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    user_id      UUID         NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
    payload      JSONB        NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT answers_question_user_uk UNIQUE (question_id, user_id)
);

CREATE INDEX idx_answers_user      ON answers(user_id);
CREATE INDEX idx_answers_question  ON answers(question_id);

-- =====================================================================
-- follows
-- =====================================================================
CREATE TABLE follows (
    follower_id  UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    followee_id  UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (follower_id, followee_id),
    CONSTRAINT follows_no_self_follow CHECK (follower_id <> followee_id)
);

CREATE INDEX idx_follows_followee ON follows(followee_id);

-- =====================================================================
-- comments
-- =====================================================================
CREATE TABLE comments (
    id          UUID          PRIMARY KEY,
    poll_id     UUID          NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    user_id     UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body        VARCHAR(1000) NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_poll_created ON comments(poll_id, created_at DESC);
CREATE INDEX idx_comments_user         ON comments(user_id);
