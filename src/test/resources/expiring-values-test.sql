CREATE TABLE IF NOT EXISTS app_expiring_value (
    entry_key VARCHAR(191) NOT NULL PRIMARY KEY,
    entry_value VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL
);
