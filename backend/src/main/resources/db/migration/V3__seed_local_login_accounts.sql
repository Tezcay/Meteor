-- 为本地认证演示准备两个可登录账号。V1/V2 已执行，新增数据必须通过新迁移完成。
-- 两个账号的演示密码都是 password；数据库只保存 BCrypt 哈希，不保存明文。
UPDATE users
SET password_hash = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    status = 'ACTIVE'
WHERE email = 'creator@meteor.local';

INSERT INTO users (email, password_hash, display_name, status)
SELECT 'buyer@meteor.local',
       '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
       'Meteor Buyer',
       'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'buyer@meteor.local'
);

-- INSERT IGNORE 配合 (user_id, role) 主键，让迁移面对已有本地演示数据时仍保持幂等。
INSERT IGNORE INTO user_roles (user_id, role)
SELECT id, 'USER'
FROM users
WHERE email = 'buyer@meteor.local';
