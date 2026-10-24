CREATE TABLE IF NOT EXISTS app_expiring_value (
    entry_key VARCHAR(191) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    entry_value VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL COMMENT 'UTC',
    PRIMARY KEY (entry_key),
    INDEX idx_app_expiring_value_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
