-- 七维智行南京-杭州智能规划基础数据
-- 执行方式：mysql -uroot -p123456 travel_agent < sql/seed_demo_nanjing_hangzhou.sql
-- 说明：所有数据使用 INSERT IGNORE，可重复执行；不爬 12306 / OTA，不产生真实支付或出票。

CREATE TABLE IF NOT EXISTS transport_hub (
  id BIGINT NOT NULL PRIMARY KEY,
  city_name VARCHAR(80) NOT NULL,
  hub_name VARCHAR(120) NOT NULL,
  hub_type VARCHAR(40) NOT NULL,
  address VARCHAR(255) DEFAULT NULL,
  longitude DECIMAL(12,6) NOT NULL,
  latitude DECIMAL(12,6) NOT NULL,
  adcode VARCHAR(20) DEFAULT NULL,
  priority INT DEFAULT 100,
  status VARCHAR(32) DEFAULT 'ENABLED',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_transport_hub_city_name_type (city_name, hub_name, hub_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS train_product (
  id BIGINT NOT NULL PRIMARY KEY,
  train_no VARCHAR(40) NOT NULL,
  start_station VARCHAR(120) NOT NULL,
  end_station VARCHAR(120) NOT NULL,
  start_city VARCHAR(80) NOT NULL,
  end_city VARCHAR(80) NOT NULL,
  departure_time DATETIME NOT NULL,
  arrival_time DATETIME NOT NULL,
  duration_minutes INT NOT NULL,
  seat_type VARCHAR(80) DEFAULT '二等座',
  price DECIMAL(10,2) DEFAULT 0,
  stock INT DEFAULT 0,
  status VARCHAR(32) DEFAULT 'AVAILABLE',
  data_source VARCHAR(40) DEFAULT 'LOCAL_DEMO',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_train_product_no_time_seat (train_no, departure_time, seat_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hotel_product (
  id BIGINT NOT NULL PRIMARY KEY,
  hotel_name VARCHAR(180) NOT NULL,
  city_name VARCHAR(80) NOT NULL,
  near_place VARCHAR(160) DEFAULT NULL,
  address VARCHAR(255) DEFAULT NULL,
  longitude DECIMAL(12,6) NOT NULL,
  latitude DECIMAL(12,6) NOT NULL,
  rating DECIMAL(3,1) DEFAULT 0,
  price DECIMAL(10,2) DEFAULT 0,
  stock INT DEFAULT 0,
  tags VARCHAR(255) DEFAULT NULL,
  distance_desc VARCHAR(120) DEFAULT NULL,
  status VARCHAR(32) DEFAULT 'AVAILABLE',
  data_source VARCHAR(40) DEFAULT 'LOCAL_DEMO',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_hotel_product_name_address (hotel_name, address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS reminder_template (
  id BIGINT NOT NULL PRIMARY KEY,
  scene_type VARCHAR(60) NOT NULL,
  reminder_type VARCHAR(60) NOT NULL,
  title VARCHAR(160) NOT NULL,
  content_template VARCHAR(1000) DEFAULT NULL,
  default_enabled TINYINT DEFAULT 1,
  trigger_rule VARCHAR(255) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_reminder_template_scene_type (scene_type, reminder_type, title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DELETE FROM transport_hub WHERE id BETWEEN 61001 AND 62004;
DELETE FROM train_product WHERE id BETWEEN 63001 AND 63012;
DELETE FROM hotel_product WHERE id BETWEEN 64001 AND 64015;
DELETE FROM reminder_template WHERE id BETWEEN 65001 AND 65012;

INSERT IGNORE INTO transport_hub
(id, city_name, hub_name, hub_type, address, longitude, latitude, adcode, priority, status)
VALUES
(61001, '南京', '南京南站', 'TRAIN_STATION', '南京市雨花台区玉兰路98号', 118.797918, 31.970410, '320114', 1, 'ENABLED'),
(61002, '南京', '南京站', 'TRAIN_STATION', '南京市玄武区龙蟠路111号', 118.803677, 32.088193, '320102', 2, 'ENABLED'),
(61003, '南京', '南京禄口国际机场', 'AIRPORT', '南京市江宁区禄口街道', 118.871944, 31.735737, '320115', 1, 'ENABLED'),
(61004, '南京', '南京汽车客运南站', 'BUS_STATION', '南京市雨花台区江南路1号', 118.797633, 31.968895, '320114', 1, 'ENABLED'),
(62001, '杭州', '杭州东站', 'TRAIN_STATION', '杭州市上城区天城路1号', 120.212010, 30.291377, '330102', 1, 'ENABLED'),
(62002, '杭州', '杭州站', 'TRAIN_STATION', '杭州市上城区环城东路1号', 120.178071, 30.246986, '330102', 2, 'ENABLED'),
(62003, '杭州', '杭州萧山国际机场', 'AIRPORT', '杭州市萧山区空港大道', 120.432503, 30.236934, '330109', 1, 'ENABLED'),
(62004, '杭州', '杭州汽车客运中心', 'BUS_STATION', '杭州市上城区九沙大道', 120.270476, 30.315034, '330102', 1, 'ENABLED');

INSERT IGNORE INTO train_product
(id, train_no, start_station, end_station, start_city, end_city, departure_time, arrival_time, duration_minutes, seat_type, price, stock, status, data_source)
VALUES
(63001, 'G7605', '南京南站', '杭州东站', '南京', '杭州', '2026-06-01 08:12:00', '2026-06-01 09:48:00', 96, '二等座', 117, 84, 'AVAILABLE', 'LOCAL_DEMO'),
(63002, 'G7611', '南京南站', '杭州东站', '南京', '杭州', '2026-06-01 08:46:00', '2026-06-01 10:18:00', 92, '二等座', 132, 62, 'AVAILABLE', 'LOCAL_DEMO'),
(63003, 'G7375', '南京南站', '杭州东站', '南京', '杭州', '2026-06-01 09:24:00', '2026-06-01 11:05:00', 101, '二等座', 141, 55, 'AVAILABLE', 'LOCAL_DEMO'),
(63004, 'G7791', '南京南站', '杭州东站', '南京', '杭州', '2026-06-01 10:08:00', '2026-06-01 11:39:00', 91, '二等座', 128, 48, 'AVAILABLE', 'LOCAL_DEMO'),
(63005, 'G1227', '南京南站', '杭州东站', '南京', '杭州', '2026-06-01 10:56:00', '2026-06-01 12:36:00', 100, '二等座', 150, 39, 'AVAILABLE', 'LOCAL_DEMO'),
(63006, 'G7599', '南京南站', '杭州东站', '南京', '杭州', '2026-06-01 12:18:00', '2026-06-01 13:52:00', 94, '二等座', 126, 73, 'AVAILABLE', 'LOCAL_DEMO'),
(63007, 'G7431', '南京南站', '杭州东站', '南京', '杭州', '2026-06-01 13:04:00', '2026-06-01 14:41:00', 97, '二等座', 139, 58, 'AVAILABLE', 'LOCAL_DEMO'),
(63008, 'G7619', '南京南站', '杭州东站', '南京', '杭州', '2026-06-01 14:30:00', '2026-06-01 16:02:00', 92, '二等座', 117, 46, 'AVAILABLE', 'LOCAL_DEMO'),
(63009, 'G7667', '南京南站', '杭州东站', '南京', '杭州', '2026-06-01 16:05:00', '2026-06-01 17:45:00', 100, '二等座', 145, 33, 'AVAILABLE', 'LOCAL_DEMO'),
(63010, 'G7587', '南京南站', '杭州东站', '南京', '杭州', '2026-06-01 18:22:00', '2026-06-01 20:01:00', 99, '二等座', 136, 27, 'AVAILABLE', 'LOCAL_DEMO');

INSERT IGNORE INTO hotel_product
(id, hotel_name, city_name, near_place, address, longitude, latitude, rating, price, stock, tags, distance_desc, status, data_source)
VALUES
(64001, '杭州东站智行商务酒店', '杭州', '杭州东站', '杭州市上城区天城路杭州东站东广场', 120.217800, 30.292900, 4.7, 398, 18, '近高铁站,商务出差,可开发票', '距杭州东站约600米', 'AVAILABLE', 'LOCAL_DEMO'),
(64002, '东站云栖轻居酒店', '杭州', '杭州东站', '杭州市上城区新风路', 120.209600, 30.289900, 4.5, 328, 22, '近地铁,早餐,行李寄存', '距杭州东站约900米', 'AVAILABLE', 'LOCAL_DEMO'),
(64003, '火车东站行政公寓', '杭州', '杭州东站', '杭州市上城区环站东路', 120.221200, 30.295600, 4.6, 456, 12, '行政公寓,安静,晚到保留', '距杭州东站约1.1公里', 'AVAILABLE', 'LOCAL_DEMO'),
(64004, '阿里西溪园区优选酒店', '杭州', '阿里巴巴西溪园区', '杭州市余杭区文一西路969号附近', 120.025900, 30.279700, 4.8, 520, 15, '近阿里园区,会议,可开发票', '距阿里西溪园区约800米', 'AVAILABLE', 'LOCAL_DEMO'),
(64005, '未来科技城商务酒店', '杭州', '阿里巴巴西溪园区', '杭州市余杭区未来科技城海创园', 120.017900, 30.283100, 4.6, 438, 20, '未来科技城,商务,接驳便利', '距阿里西溪园区约1.5公里', 'AVAILABLE', 'LOCAL_DEMO'),
(64006, '西溪轻居会议酒店', '杭州', '阿里巴巴西溪园区', '杭州市余杭区仓前街道良睦路', 120.007300, 30.286400, 4.4, 368, 26, '低预算,会议,早餐', '距阿里西溪园区约2.2公里', 'AVAILABLE', 'LOCAL_DEMO'),
(64007, '欧美金融城行政酒店', '杭州', '阿里巴巴西溪园区', '杭州市余杭区欧美金融城', 120.003800, 30.276900, 4.7, 588, 10, '行政酒廊,高评分,可报销', '距阿里西溪园区约2.6公里', 'AVAILABLE', 'LOCAL_DEMO'),
(64008, '良睦路差旅精选酒店', '杭州', '阿里巴巴西溪园区', '杭州市余杭区良睦路地铁站附近', 120.011900, 30.284500, 4.5, 399, 16, '近地铁,少步行,商务', '距良睦路站约300米', 'AVAILABLE', 'LOCAL_DEMO'),
(64009, '滨江阿里园区商务酒店', '杭州', '阿里巴巴滨江园区', '杭州市滨江区网商路699号附近', 120.190500, 30.187900, 4.7, 498, 14, '近滨江园区,商务,会议', '距阿里滨江园区约700米', 'AVAILABLE', 'LOCAL_DEMO'),
(64010, '滨江江南大道轻居', '杭州', '阿里巴巴滨江园区', '杭州市滨江区江南大道', 120.185500, 30.189800, 4.4, 348, 25, '低预算,近地铁,早餐', '距阿里滨江园区约1.4公里', 'AVAILABLE', 'LOCAL_DEMO'),
(64011, '滨江行政公寓', '杭州', '阿里巴巴滨江园区', '杭州市滨江区滨盛路', 120.198700, 30.191300, 4.6, 468, 11, '行政公寓,洗衣,可开发票', '距阿里滨江园区约2公里', 'AVAILABLE', 'LOCAL_DEMO'),
(64012, '西溪湿地商务花园酒店', '杭州', '阿里巴巴西溪园区', '杭州市西湖区西溪湿地东门', 120.063200, 30.268300, 4.5, 426, 18, '安静,商务,近西溪', '距阿里西溪园区约4公里', 'AVAILABLE', 'LOCAL_DEMO'),
(64013, '杭州东站接驳精选酒店', '杭州', '杭州东站', '杭州市上城区东宁路', 120.214700, 30.286300, 4.3, 288, 30, '接驳便利,低预算,中转', '距杭州东站约1.3公里', 'AVAILABLE', 'LOCAL_DEMO'),
(64014, '未来科技城国际会议酒店', '杭州', '阿里巴巴西溪园区', '杭州市余杭区文一西路', 120.031700, 30.281900, 4.9, 688, 8, '会议,高端商务,行政', '距阿里西溪园区约1.2公里', 'AVAILABLE', 'LOCAL_DEMO'),
(64015, '滨江网商路智选酒店', '杭州', '阿里巴巴滨江园区', '杭州市滨江区网商路', 120.193100, 30.185900, 4.5, 386, 19, '网商路,可开发票,短住', '距阿里滨江园区约900米', 'AVAILABLE', 'LOCAL_DEMO');

INSERT IGNORE INTO reminder_template
(id, scene_type, reminder_type, title, content_template, default_enabled, trigger_rule)
VALUES
(65001, 'BUSINESS_TRIP', 'WEATHER', '出发前天气提醒', '出发前请查看{startCity}和{endCity}天气，雨天请携带雨具并预留换乘时间。', 1, '出发前24小时'),
(65002, 'BUSINESS_TRIP', 'CERTIFICATE', '身份证提醒', '请确认已携带身份证、订单凭证和公司报销信息。', 1, '出发前12小时'),
(65003, 'BUSINESS_TRIP', 'FILE', '会议文件提醒', '请确认电脑、充电器、会议文件、发票抬头、工牌或门禁卡。', 1, '出发前12小时'),
(65004, 'BUSINESS_TRIP', 'DEPARTURE_TIME', '出发时间提醒', '建议按规划时间提前出发，预留安检、进站和换乘缓冲。', 1, '出发前2小时'),
(65005, 'BUSINESS_TRIP', 'TRAFFIC', '路线拥堵提醒', '行中将关注拥堵、换乘和到站风险，如需改线会先征求确认。', 1, '行程中'),
(65006, 'BUSINESS_TRIP', 'HOTEL_CHECKIN', '酒店入住提醒', '请确认酒店入住时间、发票和到店交通方式。', 1, '到达前1小时'),
(65007, 'TOURISM', 'WEATHER', '旅游天气提醒', '请关注目的地天气和景点开放状态。', 1, '出发前24小时'),
(65008, 'TOURISM', 'LUGGAGE', '行李提醒', '请按出游天数准备衣物、证件、充电器和常用药。', 1, '出发前12小时'),
(65009, 'TOURISM', 'TRAFFIC', '景点路线提醒', '景区周边可能拥堵，建议错峰出行。', 1, '行程中'),
(65010, 'CATCH_TRAIN_FLIGHT', 'CERTIFICATE', '证件提醒', '乘车或登机前请确认身份证和订单凭证。', 1, '出发前12小时'),
(65011, 'CATCH_TRAIN_FLIGHT', 'DEPARTURE_TIME', '出发时间提醒', '请根据实时路况提前出发，避免迟到。', 1, '出发前2小时'),
(65012, 'CATCH_TRAIN_FLIGHT', 'TRAFFIC', '迟到风险提醒', '系统将监测道路耗时变化，风险升高时提示备选方案。', 1, '行程中');
