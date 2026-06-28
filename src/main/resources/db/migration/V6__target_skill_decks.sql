-- =====================================================================
-- MemoSpark — Flyway Migration V6
-- Completes the JD → skill → deck → cards loop:
--   * links each target skill to a generated study deck
--   * stores AI-proposed sub-topics + suggested card count so cards can be
--     generated on demand (one AI call per skill) instead of being discarded.
-- Aligns with com.memospark.core.domain.TargetSkill.
-- =====================================================================

SET NAMES utf8mb4;

ALTER TABLE `target_skills`
  ADD COLUMN `deck_id`              BIGINT        DEFAULT NULL AFTER `user_id`,
  ADD COLUMN `topics`               VARCHAR(2000) DEFAULT NULL AFTER `description`,
  ADD COLUMN `suggested_card_count` INT           NOT NULL DEFAULT 0 AFTER `weight`;

ALTER TABLE `target_skills`
  ADD KEY `idx_target_skills_deck_id` (`deck_id`),
  ADD CONSTRAINT `fk_target_skills_deck`
    FOREIGN KEY (`deck_id`) REFERENCES `decks` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE;
