-- 七维智行首页前后端联动模拟数据
-- 使用数据库：travel_agent

CREATE TABLE IF NOT EXISTS hotel (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    city VARCHAR(80) NOT NULL,
    address VARCHAR(255) DEFAULT NULL,
    cover_image VARCHAR(500) DEFAULT NULL,
    star_level INT DEFAULT 0,
    score DECIMAL(3,1) DEFAULT 0,
    min_price DECIMAL(10,2) DEFAULT 0,
    tags VARCHAR(255) DEFAULT NULL,
    description VARCHAR(1000) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_hotel_city (city),
    INDEX idx_hotel_price (min_price),
    INDEX idx_hotel_score (score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hotel_room (
    id BIGINT NOT NULL PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    room_name VARCHAR(120) NOT NULL,
    bed_type VARCHAR(80) DEFAULT NULL,
    breakfast VARCHAR(40) DEFAULT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT DEFAULT 0,
    refundable TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_room_hotel (hotel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS traffic_product (
    id BIGINT NOT NULL PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    start_place VARCHAR(120) DEFAULT NULL,
    end_place VARCHAR(120) DEFAULT NULL,
    depart_time DATETIME DEFAULT NULL,
    arrive_time DATETIME DEFAULT NULL,
    duration VARCHAR(60) DEFAULT NULL,
    price DECIMAL(10,2) DEFAULT 0,
    carrier_name VARCHAR(120) DEFAULT NULL,
    seat_type VARCHAR(80) DEFAULT NULL,
    remain_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_traffic_type (type),
    INDEX idx_traffic_route (start_place, end_place),
    INDEX idx_traffic_depart (depart_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS booking_order (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id VARCHAR(80) NOT NULL,
    order_no VARCHAR(80) NOT NULL,
    order_type VARCHAR(32) NOT NULL,
    product_id BIGINT DEFAULT NULL,
    product_name VARCHAR(180) DEFAULT NULL,
    start_place VARCHAR(120) DEFAULT NULL,
    end_place VARCHAR(120) DEFAULT NULL,
    start_time DATETIME DEFAULT NULL,
    end_time DATETIME DEFAULT NULL,
    total_amount DECIMAL(10,2) DEFAULT 0,
    contact_name VARCHAR(80) DEFAULT NULL,
    contact_phone VARCHAR(40) DEFAULT NULL,
    status VARCHAR(32) DEFAULT 'UNPAID',
    remark VARCHAR(500) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_order_user (user_id),
    INDEX idx_order_status (status),
    INDEX idx_order_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO hotel
(id, name, city, address, cover_image, star_level, score, min_price, tags, description)
VALUES
(1001, '杭州西湖湖滨智选酒店', '杭州', '杭州市上城区湖滨路88号', 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=80', 4, 4.7, 428, '近地铁,西湖步行圈,早餐', '适合西湖周边休闲旅行，步行可达湖滨商圈。'),
(1002, '杭州东站轻住酒店', '杭州', '杭州市上城区火车东站东广场', 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=900&q=80', 3, 4.5, 299, '近高铁站,低预算,交通便利', '适合跨城高铁到达后的短住与中转。'),
(1003, '南京南站商务酒店', '南京', '南京市雨花台区南京南站北广场', 'https://images.unsplash.com/photo-1564501049412-61c2a3083791?auto=format&fit=crop&w=900&q=80', 4, 4.6, 358, '商务出行,近地铁,可开发票', '适合商务差旅和高铁出行用户。'),
(1004, '上海人民广场城市酒店', '上海', '上海市黄浦区人民广场地铁站旁', 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=900&q=80', 4, 4.8, 520, '市中心,近地铁,购物便利', '适合城市观光、商务与购物出行。'),
(1005, '南京夫子庙文化酒店', '南京', '南京市秦淮区夫子庙景区入口', 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=900&q=80', 4, 4.6, 399, '景区周边,美食,亲子', '适合文旅场景和家庭亲子出游。');

INSERT IGNORE INTO hotel_room
(id, hotel_id, room_name, bed_type, breakfast, price, stock, refundable)
VALUES
(2001, 1001, '湖景大床房', '1张大床', '双早', 528, 8, 1),
(2002, 1001, '舒适双床房', '2张单人床', '双早', 468, 12, 1),
(2003, 1002, '轻住大床房', '1张大床', '无早', 299, 16, 1),
(2004, 1002, '商务双床房', '2张单人床', '单早', 339, 10, 1),
(2005, 1003, '商务大床房', '1张大床', '双早', 398, 9, 1),
(2006, 1003, '行政双床房', '2张单人床', '双早', 468, 6, 1),
(2007, 1004, '城市景观房', '1张大床', '双早', 620, 7, 1),
(2008, 1004, '高级双床房', '2张单人床', '双早', 560, 8, 1),
(2009, 1005, '秦淮亲子房', '1大1小双床', '三早', 499, 5, 1),
(2010, 1005, '文化庭院房', '1张大床', '双早', 459, 6, 1);

INSERT IGNORE INTO traffic_product
(id, type, start_place, end_place, depart_time, arrive_time, duration, price, carrier_name, seat_type, remain_count)
VALUES
(3001, 'TRAIN', '南京南', '杭州东', '2026-05-25 08:20:00', '2026-05-25 10:05:00', '1小时45分', 117, 'G7605', '二等座', 80),
(3002, 'TRAIN', '南京南', '杭州东', '2026-05-25 09:10:00', '2026-05-25 11:02:00', '1小时52分', 147, 'G7621', '一等座', 22),
(3003, 'TRAIN', '上海虹桥', '杭州东', '2026-05-25 10:00:00', '2026-05-25 10:55:00', '55分', 73, 'G7313', '二等座', 64),
(3004, 'TRAIN', '杭州东', '南京南', '2026-05-25 17:30:00', '2026-05-25 19:21:00', '1小时51分', 117, 'G7618', '二等座', 45),
(3005, 'TRAIN', '南京南', '上海虹桥', '2026-05-25 12:15:00', '2026-05-25 13:35:00', '1小时20分', 139, 'G7019', '商务座', 6),
(3101, 'FLIGHT', '南京禄口机场', '杭州萧山机场', '2026-05-25 09:30:00', '2026-05-25 10:40:00', '1小时10分', 420, 'QW1021', '经济舱', 36),
(3102, 'FLIGHT', '上海浦东机场', '杭州萧山机场', '2026-05-25 11:00:00', '2026-05-25 12:00:00', '1小时', 380, 'QW2032', '经济舱', 42),
(3103, 'FLIGHT', '杭州萧山机场', '南京禄口机场', '2026-05-25 18:20:00', '2026-05-25 19:25:00', '1小时5分', 460, 'QW1188', '经济舱', 21),
(3104, 'FLIGHT', '南京禄口机场', '上海虹桥机场', '2026-05-25 13:30:00', '2026-05-25 14:20:00', '50分', 350, 'QW3306', '经济舱', 29),
(3105, 'FLIGHT', '上海虹桥机场', '南京禄口机场', '2026-05-25 16:40:00', '2026-05-25 17:35:00', '55分', 360, 'QW3307', '经济舱', 33),
(3201, 'BUS', '南京汽车客运站', '杭州汽车北站', '2026-05-25 08:00:00', '2026-05-25 12:30:00', '4小时30分', 88, '南京-杭州大巴', '普通座', 28),
(3202, 'METRO', '杭州东站', '西湖文化广场', '2026-05-25 10:30:00', '2026-05-25 10:55:00', '25分', 5, '杭州地铁1号线', '地铁', 999),
(3203, 'TAXI', '杭州东站', '西湖湖滨', '2026-05-25 10:20:00', '2026-05-25 10:55:00', '35分', 58, '网约车预估', '舒适型', 99);
