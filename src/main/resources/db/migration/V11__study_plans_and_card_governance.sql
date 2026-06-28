-- =====================================================================
-- MemoSpark - Flyway Migration V11
-- Adds card learning metadata, AI scheduling evidence, and study plans.
-- =====================================================================

SET NAMES utf8mb4;

ALTER TABLE `cards`
  ADD COLUMN `content_difficulty` VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' AFTER `tags`,
  ADD COLUMN `learning_stage` VARCHAR(20) NOT NULL DEFAULT 'FOUNDATION' AFTER `content_difficulty`,
  ADD COLUMN `stage_order` INT NOT NULL DEFAULT 0 AFTER `learning_stage`,
  ADD COLUMN `governance_note` VARCHAR(500) DEFAULT NULL AFTER `stage_order`,
  ADD COLUMN `governed_at` DATETIME(6) DEFAULT NULL AFTER `governance_note`;

UPDATE `cards`
SET `stage_order` = `id`
WHERE `stage_order` = 0;

ALTER TABLE `review_logs`
  ADD COLUMN `learning_mode` VARCHAR(20) DEFAULT NULL AFTER `ai_suggested_answer`,
  ADD COLUMN `ai_recommended_review_days` INT DEFAULT NULL AFTER `learning_mode`;

CREATE TABLE `study_plans` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `target_id`     BIGINT       NOT NULL,
  `user_id`       BIGINT       NOT NULL,
  `start_date`    DATE         NOT NULL,
  `target_date`   DATE         NOT NULL,
  `weekly_hours`  INT          NOT NULL,
  `summary`       TEXT         NOT NULL,
  `strategy`      TEXT         NOT NULL,
  `roadmap_json`  LONGTEXT     NOT NULL,
  `active`        TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at`    DATETIME(6)  NOT NULL,
  `updated_at`    DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_study_plans_target_active` (`target_id`, `active`),
  KEY `idx_study_plans_user_active` (`user_id`, `active`),
  CONSTRAINT `fk_study_plans_target`
    FOREIGN KEY (`target_id`) REFERENCES `targets` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_study_plans_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `study_plan_items` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `plan_id`        BIGINT       NOT NULL,
  `deck_id`        BIGINT       DEFAULT NULL,
  `plan_date`      DATE         NOT NULL,
  `week_number`    INT          NOT NULL,
  `item_type`      VARCHAR(20)  NOT NULL,
  `learning_stage` VARCHAR(20)  DEFAULT NULL,
  `title`          VARCHAR(200) NOT NULL,
  `objective`      VARCHAR(1000) DEFAULT NULL,
  `target_count`   INT          NOT NULL DEFAULT 1,
  `sort_order`     INT          NOT NULL DEFAULT 0,
  `completed_at`   DATETIME(6)  DEFAULT NULL,
  `created_at`     DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_plan_items_plan_date` (`plan_id`, `plan_date`),
  KEY `idx_plan_items_deck_date` (`deck_id`, `plan_date`),
  CONSTRAINT `fk_plan_items_plan`
    FOREIGN KEY (`plan_id`) REFERENCES `study_plans` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_plan_items_deck`
    FOREIGN KEY (`deck_id`) REFERENCES `decks` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

