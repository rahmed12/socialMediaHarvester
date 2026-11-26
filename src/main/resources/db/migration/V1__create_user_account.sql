-- People on our team who log in: Viewer, Operator or Admin (GLOSSARY.md).
CREATE TABLE user_account (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         TEXT        NOT NULL,
    password_hash TEXT        NOT NULL,
    role          TEXT        NOT NULL CHECK (role IN ('VIEWER', 'OPERATOR', 'ADMIN')),
    created_at    TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX user_account_email_key ON user_account (lower(email));
