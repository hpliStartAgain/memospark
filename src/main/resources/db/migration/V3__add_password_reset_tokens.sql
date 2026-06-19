-- =====================================================================
-- V3: Add password_reset_tokens table for password reset flow
-- =====================================================================

CREATE TABLE IF NOT EXISTS `password_reset_tokens` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       NOT NULL,
  `token`      VARCHAR(128) NOT NULL,
  `expires_at` DATETIME(6)  NOT NULL,
  `used`       BIT(1)       NOT NULL DEFAULT b'0',
  `created_at` DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_password_reset_tokens_token` (`token`),
  KEY `idx_password_reset_tokens_user_id` (`user_id`),
  CONSTRAINT `fk_password_reset_tokens_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
