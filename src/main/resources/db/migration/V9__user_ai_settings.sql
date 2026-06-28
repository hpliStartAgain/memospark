-- =====================================================================
-- MemoSpark — Flyway Migration V9
-- Adds per-user OpenAI-compatible provider settings.
-- API keys are encrypted by the application before persistence.
-- =====================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_settings` (
  `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`            BIGINT       NOT NULL,
  `provider`           VARCHAR(50)  NOT NULL,
  `base_url`           VARCHAR(500) NOT NULL,
  `model`              VARCHAR(100) NOT NULL,
  `api_key_encrypted`  TEXT         DEFAULT NULL,
  `updated_at`         DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_settings_user_id` (`user_id`),
  CONSTRAINT `fk_ai_settings_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
