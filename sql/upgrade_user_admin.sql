-- user 表账号体系升级：普通用户、管理员、超级管理员统一放在 user 表。
ALTER TABLE user ADD COLUMN username VARCHAR(80) DEFAULT NULL;
ALTER TABLE user ADD COLUMN nickname VARCHAR(80) DEFAULT NULL;
ALTER TABLE user ADD COLUMN phone VARCHAR(40) DEFAULT NULL;
ALTER TABLE user ADD COLUMN role VARCHAR(32) NOT NULL DEFAULT 'USER';
ALTER TABLE user ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ENABLED';
ALTER TABLE user ADD COLUMN last_login_time DATETIME DEFAULT NULL;
INSERT IGNORE INTO user (id, username, email, password, nickname, role, status)
VALUES (1, 'admin', 'admin@qvtrip.local', 'admin123', '超级管理员', 'SUPER_ADMIN', 'ENABLED');
