-- V5: WeChat Mini-Program login support
ALTER TABLE `users`
    ADD COLUMN `wx_openid` VARCHAR(64) DEFAULT NULL,
    ADD UNIQUE INDEX `uk_users_wx_openid` (`wx_openid`);
