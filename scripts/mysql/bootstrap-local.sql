CREATE DATABASE IF NOT EXISTS meteor
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'meteor_app'@'localhost'
    IDENTIFIED BY 'meteor_dev_only';

GRANT ALL PRIVILEGES ON meteor.* TO 'meteor_app'@'localhost';
FLUSH PRIVILEGES;
