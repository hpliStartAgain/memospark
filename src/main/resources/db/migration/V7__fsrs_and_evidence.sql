-- =====================================================================
-- MemoSpark — Flyway Migration V7
-- Adds FSRS scheduling state and evidence-ready review snapshots.
--   * card_progress.stability / difficulty store the DSR memory model
--   * srs_settings.desired_retention controls interval targeting
--   * review_logs.prev_stability / prev_difficulty make undo complete
-- Aligns with CardProgress, SrsSettings, and ReviewLog entities.
-- =====================================================================

SET NAMES utf8mb4;

ALTER TABLE `card_progress`
  ADD COLUMN `stability`  DOUBLE NOT NULL DEFAULT 0.0 AFTER `ease_factor`,
  ADD COLUMN `difficulty` DOUBLE NOT NULL DEFAULT 5.0 AFTER `stability`;

ALTER TABLE `srs_settings`
  ADD COLUMN `desired_retention` DOUBLE NOT NULL DEFAULT 0.9 AFTER `second_interval`;

ALTER TABLE `review_logs`
  ADD COLUMN `prev_stability`  DOUBLE DEFAULT NULL AFTER `prev_ease_factor`,
  ADD COLUMN `prev_difficulty` DOUBLE DEFAULT NULL AFTER `prev_stability`;
