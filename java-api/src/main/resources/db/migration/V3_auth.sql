ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS password_hash varchar(100);

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS token_version integer
        NOT NULL DEFAULT 1;

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS last_login_at timestamptz;

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS password_changed_at timestamptz;