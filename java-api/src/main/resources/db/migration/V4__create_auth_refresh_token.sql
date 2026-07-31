CREATE TABLE auth_refresh_token
(
    id                   bigint                   NOT NULL PRIMARY KEY,
    tenant_id            bigint                   NOT NULL REFERENCES sys_tenant (id),
    user_id              bigint                   NOT NULL REFERENCES sys_user (id),
    token_hash           varchar(64)              NOT NULL,
    expires_at           timestamp with time zone NOT NULL,
    revoked_at           timestamp with time zone,
    replaced_by_token_id bigint REFERENCES auth_refresh_token (id),
    created_ip           varchar(64),
    user_agent           varchar(512),
    created_at           timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uk_auth_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_auth_refresh_token_user
    ON auth_refresh_token (tenant_id, user_id);

CREATE INDEX idx_auth_refresh_token_expires_at
    ON auth_refresh_token (expires_at);
