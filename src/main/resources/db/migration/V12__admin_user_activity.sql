-- V12: Admin panel — user enabled flag + daily activity tracking for DAU

-- Add enabled column to users (default true for existing users)
ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;

-- Daily activity table: one row per (user, date) — powers DAU metrics.
-- Inserted/updated by RequestLoggingFilter on each authenticated API request.
CREATE TABLE user_daily_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_date DATE NOT NULL,
    request_count INT NOT NULL DEFAULT 1,
    last_active_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (user_id, activity_date),
    CONSTRAINT fk_uda_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_uda_date ON user_daily_activity(activity_date);
