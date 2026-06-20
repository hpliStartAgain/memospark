-- =====================================================================
-- MemoSpark — Flyway Migration V4
-- Adds target-driven interview-prep entities: targets, job JDs, skills.
-- Aligns with JPA entities under com.memospark.core.domain.*
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- targets (target job / interview goal — aggregate root)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `targets` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT       NOT NULL,
  `title`           VARCHAR(200) NOT NULL,
  `company`         VARCHAR(200) DEFAULT NULL,
  `status`          VARCHAR(20)  NOT NULL DEFAULT 'PREPARING',
  `interview_date`  DATE         DEFAULT NULL,
  `notes`           TEXT         DEFAULT NULL,
  `created_at`      DATETIME(6)  NOT NULL,
  `updated_at`      DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_targets_user_id` (`user_id`),
  CONSTRAINT `fk_targets_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- job_jds (raw job descriptions attached to a target)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `job_jds` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `target_id`   BIGINT       NOT NULL,
  `title`       VARCHAR(200) DEFAULT NULL,
  `content`     TEXT         NOT NULL,
  `source`      VARCHAR(100) DEFAULT NULL,
  `created_at`  DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_job_jds_target_id` (`target_id`),
  CONSTRAINT `fk_job_jds_target`
    FOREIGN KEY (`target_id`) REFERENCES `targets` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- target_skills (skill requirement + self-assessed mastery per target)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `target_skills` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT,
  `target_id`    BIGINT        NOT NULL,
  `user_id`      BIGINT        NOT NULL,
  `name`         VARCHAR(200)  NOT NULL,
  `category`     VARCHAR(100)  DEFAULT NULL,
  `description`  VARCHAR(1000) DEFAULT NULL,
  `weight`       INT           NOT NULL DEFAULT 3,
  `self_level`   INT           NOT NULL DEFAULT 0,
  `created_at`   DATETIME(6)   NOT NULL,
  `updated_at`   DATETIME(6)   NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_target_skills_target_id` (`target_id`),
  KEY `idx_target_skills_user_id` (`user_id`),
  CONSTRAINT `fk_target_skills_target`
    FOREIGN KEY (`target_id`) REFERENCES `targets` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_target_skills_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
