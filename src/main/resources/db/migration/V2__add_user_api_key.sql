-- =====================================================================
-- V2: Add per-user API key column for Quick-Add authentication
-- =====================================================================

ALTER TABLE `users` ADD COLUMN `api_key` VARCHAR(128) DEFAULT NULL;
CREATE UNIQUE INDEX `uk_users_api_key` ON `users` (`api_key`);
