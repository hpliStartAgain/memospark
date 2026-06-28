-- =====================================================================
-- MemoSpark — Flyway Migration V8
-- Adds AI mock interview sessions and per-question answer feedback.
-- Scored sessions feed ReadinessService.mockPerformance.
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `mock_interviews` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `target_id`        BIGINT       NOT NULL,
  `user_id`          BIGINT       NOT NULL,
  `type`             VARCHAR(30)  NOT NULL DEFAULT 'MIXED',
  `status`           VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',
  `question_count`   INT          NOT NULL DEFAULT 0,
  `average_score`    DOUBLE       DEFAULT NULL,
  `summary_feedback` TEXT         DEFAULT NULL,
  `started_at`       DATETIME(6)  NOT NULL,
  `finished_at`      DATETIME(6)  DEFAULT NULL,
  `updated_at`       DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_mock_interviews_target_id` (`target_id`),
  KEY `idx_mock_interviews_user_id` (`user_id`),
  KEY `idx_mock_interviews_target_status_finished` (`target_id`, `status`, `finished_at`),
  CONSTRAINT `fk_mock_interviews_target`
    FOREIGN KEY (`target_id`) REFERENCES `targets` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_mock_interviews_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `mock_interview_questions` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `interview_id`    BIGINT       NOT NULL,
  `question_order`  INT          NOT NULL,
  `dimension`       VARCHAR(50)  NOT NULL,
  `question`        TEXT         NOT NULL,
  `rubric`          TEXT         DEFAULT NULL,
  `user_answer`     TEXT         DEFAULT NULL,
  `score`           INT          DEFAULT NULL,
  `feedback`        TEXT         DEFAULT NULL,
  `answered_at`     DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_mock_questions_interview_id` (`interview_id`),
  CONSTRAINT `fk_mock_questions_interview`
    FOREIGN KEY (`interview_id`) REFERENCES `mock_interviews` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
