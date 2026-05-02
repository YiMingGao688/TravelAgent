CREATE DATABASE IF NOT EXISTS travel_agent
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE travel_agent;

DROP TABLE IF EXISTS smart_service_record;
DROP TABLE IF EXISTS route;
DROP TABLE IF EXISTS trip_plan;
DROP TABLE IF EXISTS chat_record;
DROP TABLE IF EXISTS user_profile;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(120) NOT NULL UNIQUE,
  password VARCHAR(120) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_profile (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id VARCHAR(120) NOT NULL UNIQUE,
  city VARCHAR(80),
  transport VARCHAR(80),
  style VARCHAR(80),
  note VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_record (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id VARCHAR(120),
  session_id VARCHAR(120) NOT NULL,
  user_message TEXT,
  ai_response LONGTEXT,
  model VARCHAR(80),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_chat_session (session_id),
  INDEX idx_chat_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trip_plan (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id VARCHAR(120) NOT NULL,
  session_id VARCHAR(120),
  title VARCHAR(200),
  start_place VARCHAR(120),
  end_place VARCHAR(120),
  content LONGTEXT NOT NULL,
  raw_question TEXT,
  source_type VARCHAR(40) DEFAULT 'AI_CHAT',
  travel_preference VARCHAR(120),
  budget VARCHAR(80),
  days INT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_trip_user (user_id),
  INDEX idx_trip_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE route (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id VARCHAR(120),
  trip_plan_id BIGINT,
  start_point VARCHAR(120) NOT NULL,
  end_point VARCHAR(120) NOT NULL,
  route_type VARCHAR(40),
  route_name VARCHAR(120),
  distance DOUBLE,
  duration INT,
  cost DOUBLE,
  description VARCHAR(1000),
  recommend_reason VARCHAR(1000),
  preference VARCHAR(120),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_route_points (start_point, end_point),
  INDEX idx_route_user (user_id),
  INDEX idx_route_trip (trip_plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE smart_service_record (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id VARCHAR(120) NOT NULL,
  service_type VARCHAR(60) NOT NULL,
  title VARCHAR(200),
  start_place VARCHAR(120),
  end_place VARCHAR(120),
  request_payload LONGTEXT,
  response_payload LONGTEXT,
  status VARCHAR(40) DEFAULT 'GENERATED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_smart_user (user_id),
  INDEX idx_smart_type (service_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
