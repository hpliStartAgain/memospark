-- =====================================================================
-- MemoSpark MySQL Schema Init Script
-- Target: MySQL 8.x
-- Idempotent: safe to run multiple times (uses IF NOT EXISTS).
-- Aligns with JPA entities under com.memospark.core.domain.*
-- Hibernate naming strategy: camelCase -> snake_case.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `memospark`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `memospark`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `username`   VARCHAR(50)  NOT NULL,
  `password`   VARCHAR(255) NOT NULL,
  `role`       VARCHAR(10)  NOT NULL DEFAULT 'USER',
  `created_at` DATETIME(6)  NOT NULL,
  `api_key`    VARCHAR(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`),
  UNIQUE KEY `uk_users_api_key` (`api_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- decks
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `decks` (
  `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
  `name`                  VARCHAR(200) NOT NULL,
  `description`           VARCHAR(1000) DEFAULT NULL,
  `type`                  VARCHAR(20)  NOT NULL DEFAULT 'CUSTOM',
  `user_id`               BIGINT       DEFAULT NULL,
  `daily_review_limit`    INT          DEFAULT NULL,
  `daily_new_card_limit`  INT          DEFAULT NULL,
  `created_at`            DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_decks_user_id` (`user_id`),
  CONSTRAINT `fk_decks_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- cards
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cards` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `deck_id`    BIGINT       NOT NULL,
  `front`      TEXT         NOT NULL,
  `back`       TEXT         NOT NULL,
  `tags`       VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cards_deck_id` (`deck_id`),
  CONSTRAINT `fk_cards_deck`
    FOREIGN KEY (`deck_id`) REFERENCES `decks` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- card_progress (1:1 with cards)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `card_progress` (
  `id`                  BIGINT  NOT NULL AUTO_INCREMENT,
  `card_id`             BIGINT  NOT NULL,
  `repetitions`         INT     NOT NULL DEFAULT 0,
  `ease_factor`         DOUBLE  NOT NULL DEFAULT 2.5,
  `review_interval`     INT     NOT NULL DEFAULT 0,
  `next_review_date`    DATE    NOT NULL,
  `last_review_date`    DATE    DEFAULT NULL,
  `first_learned_date`  DATE    DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_card_progress_card_id` (`card_id`),
  CONSTRAINT `fk_card_progress_card`
    FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- review_logs
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `review_logs` (
  `id`                       BIGINT  NOT NULL AUTO_INCREMENT,
  `card_id`                  BIGINT  NOT NULL,
  `review_date`              DATE    NOT NULL,
  `quality`                  INT     NOT NULL,
  `time_spent_ms`            BIGINT  DEFAULT NULL,
  `prev_repetitions`         INT     DEFAULT NULL,
  `prev_ease_factor`         DOUBLE  DEFAULT NULL,
  `prev_interval`            INT     DEFAULT NULL,
  `prev_next_review_date`    DATE    DEFAULT NULL,
  `prev_last_review_date`    DATE    DEFAULT NULL,
  `prev_first_learned_date`  DATE    DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_review_logs_card_id` (`card_id`),
  KEY `idx_review_logs_review_date` (`review_date`),
  CONSTRAINT `fk_review_logs_card`
    FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- srs_settings
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `srs_settings` (
  `id`                   BIGINT NOT NULL AUTO_INCREMENT,
  `user_id`              BIGINT DEFAULT NULL,
  `initial_ease_factor`  DOUBLE NOT NULL DEFAULT 2.5,
  `min_ease_factor`      DOUBLE NOT NULL DEFAULT 1.3,
  `first_interval`       INT    NOT NULL DEFAULT 1,
  `second_interval`      INT    NOT NULL DEFAULT 6,
  PRIMARY KEY (`id`),
  KEY `idx_srs_settings_user_id` (`user_id`),
  CONSTRAINT `fk_srs_settings_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- code_problems
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `code_problems` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
  `problem_number`      INT          NOT NULL,
  `title`               VARCHAR(200) NOT NULL,
  `difficulty`          VARCHAR(20)  NOT NULL,
  `description`         TEXT         NOT NULL,
  `java_template`       TEXT         NOT NULL,
  `python_template`     TEXT         NOT NULL,
  `java_driver_code`    TEXT         NOT NULL,
  `python_driver_code`  TEXT         NOT NULL,
  `test_cases_json`     TEXT         NOT NULL,
  `hint`                TEXT         DEFAULT NULL,
  `tags`                VARCHAR(500) DEFAULT NULL,
  `category`            VARCHAR(50)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_problems_problem_number` (`problem_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- code_submissions
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `code_submissions` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `problem_id`    BIGINT       NOT NULL,
  `user_id`       BIGINT       DEFAULT NULL,
  `language`      VARCHAR(20)  NOT NULL,
  `code`          TEXT         NOT NULL,
  `status`        VARCHAR(30)  NOT NULL,
  `passed_cases`  INT          NOT NULL,
  `total_cases`   INT          NOT NULL,
  `submitted_at`  DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_code_submissions_problem_id` (`problem_id`),
  KEY `idx_code_submissions_user_id` (`user_id`),
  CONSTRAINT `fk_code_submissions_problem`
    FOREIGN KEY (`problem_id`) REFERENCES `code_problems` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_code_submissions_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- problem_notes (unique per user+problem)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `problem_notes` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`          BIGINT       NOT NULL,
  `problem_id`       BIGINT       DEFAULT NULL,
  `bookmark_type`    VARCHAR(10)  DEFAULT NULL,
  `starred`          BIT(1)       NOT NULL DEFAULT b'0',
  `note`             TEXT         DEFAULT NULL,
  `error_reason`     VARCHAR(20)  DEFAULT NULL,
  `retry_count`      INT          NOT NULL DEFAULT 0,
  `ease_factor`      DOUBLE       NOT NULL DEFAULT 2.5,
  `retry_interval`   INT          NOT NULL DEFAULT 0,
  `next_retry_date`  DATE         DEFAULT NULL,
  `last_retry_date`  DATE         DEFAULT NULL,
  `created_at`       DATETIME(6)  NOT NULL,
  `updated_at`       DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_problem_notes_user_problem` (`user_id`, `problem_id`),
  KEY `idx_problem_notes_problem_id` (`problem_id`),
  CONSTRAINT `fk_problem_notes_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_problem_notes_problem`
    FOREIGN KEY (`problem_id`) REFERENCES `code_problems` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- password_reset_tokens: token-based password reset flow
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

-- =====================================================================
-- Done. Start the Spring Boot app; AdminInitializer will:
--   - create default admin account (username=admin)
--   - seed built-in decks and code problems (from CSV resources)
-- =====================================================================
