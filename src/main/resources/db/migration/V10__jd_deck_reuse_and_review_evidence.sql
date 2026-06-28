-- =====================================================================
-- MemoSpark — Flyway Migration V10
-- Adds JD deck-reuse metadata and answer evidence for AI-reviewed flashcards.
-- =====================================================================

SET NAMES utf8mb4;

ALTER TABLE `target_skills`
  ADD COLUMN `deck_link_source` VARCHAR(30) NOT NULL DEFAULT 'AI_CREATED' AFTER `deck_id`,
  ADD COLUMN `deck_match_score` DOUBLE DEFAULT NULL AFTER `deck_link_source`;

ALTER TABLE `review_logs`
  ADD COLUMN `user_answer` TEXT DEFAULT NULL AFTER `time_spent_ms`,
  ADD COLUMN `ai_grade` VARCHAR(20) DEFAULT NULL AFTER `user_answer`,
  ADD COLUMN `ai_feedback` TEXT DEFAULT NULL AFTER `ai_grade`,
  ADD COLUMN `ai_suggested_answer` TEXT DEFAULT NULL AFTER `ai_feedback`;
