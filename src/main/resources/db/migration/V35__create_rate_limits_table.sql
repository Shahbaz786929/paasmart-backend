CREATE TABLE rate_limits (
    id BIGSERIAL PRIMARY KEY,
    rate_key VARCHAR(150) NOT NULL UNIQUE,
    request_count INT NOT NULL DEFAULT 0,
    window_start TIMESTAMP NOT NULL
);