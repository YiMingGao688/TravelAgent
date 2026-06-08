package com.itheima.travelagent2.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Locale;

@Component
public class DatabaseInitializer {

    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        createUserTable();
        createSmartServiceRecordTable();
        createTravelServiceReportTable();
        createBookingTables();
        createHotelOrderTable();
        createTimelineTable();
        createAfterSaleOrderTable();
        createPlanningFoundationTables();
        seedPlanningFoundationData();
        seedBookingDemoData();
    }

    private void createUserTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS user (
                    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(120) NOT NULL,
                    password VARCHAR(120) NOT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_user_email (email)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            addUserColumnIfMissing(statement, "username", "VARCHAR(80) DEFAULT NULL");
            addUserColumnIfMissing(statement, "nickname", "VARCHAR(80) DEFAULT NULL");
            addUserColumnIfMissing(statement, "phone", "VARCHAR(40) DEFAULT NULL");
            addUserColumnIfMissing(statement, "role", "VARCHAR(32) NOT NULL DEFAULT 'USER'");
            addUserColumnIfMissing(statement, "status", "VARCHAR(32) NOT NULL DEFAULT 'ENABLED'");
            addUserColumnIfMissing(statement, "last_login_time", "DATETIME DEFAULT NULL");
            // 管理端账号也统一落在 user 表。首次登录时如果发现旧明文密码，
            // AdminController 会自动迁移为 BCrypt 密文，避免旧库登录失败。
            statement.executeUpdate("""
                    INSERT IGNORE INTO user
                    (id, username, email, password, nickname, role, status)
                    VALUES
                    (1, 'admin', 'admin@qvtrip.local', '$2a$10$5VXMbnPe0gXn5MuPooqT0u.lsXMsloNsgjf6HVl8VWrzxUuSicjFq', '超级管理员', 'SUPER_ADMIN', 'ENABLED')
                    """);
        } catch (Exception e) {
            throw new IllegalStateException("初始化 user 表失败", e);
        }
    }

    private void addUserColumnIfMissing(Statement statement, String column, String definition) {
        try {
            statement.execute("ALTER TABLE user ADD COLUMN " + column + " " + definition);
        } catch (Exception ignored) {
            // 字段已存在时忽略，保证旧库可平滑升级。
        }
    }

    private void createSmartServiceRecordTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS smart_service_record (
                    id BIGINT NOT NULL PRIMARY KEY,
                    user_id VARCHAR(64) NOT NULL,
                    service_type VARCHAR(64) NOT NULL,
                    title VARCHAR(128) DEFAULT NULL,
                    start_place VARCHAR(128) DEFAULT NULL,
                    end_place VARCHAR(128) DEFAULT NULL,
                    request_payload JSON DEFAULT NULL,
                    response_payload LONGTEXT,
                    status VARCHAR(32) DEFAULT 'GENERATED',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_smart_service_user (user_id),
                    INDEX idx_smart_service_type (service_type),
                    INDEX idx_smart_service_create_time (create_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (Exception e) {
            throw new IllegalStateException("初始化 smart_service_record 表失败", e);
        }
    }

    private void createTravelServiceReportTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS travel_service_report (
                    id BIGINT NOT NULL PRIMARY KEY,
                    user_id VARCHAR(120) NOT NULL,
                    trip_plan_id BIGINT DEFAULT NULL,
                    title VARCHAR(200) DEFAULT NULL,
                    start_place VARCHAR(120) DEFAULT NULL,
                    end_place VARCHAR(120) DEFAULT NULL,
                    report_content LONGTEXT NOT NULL,
                    report_summary VARCHAR(500) DEFAULT NULL,
                    source_type VARCHAR(60) DEFAULT 'USER_SAVE',
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_report_user (user_id),
                    INDEX idx_report_trip (trip_plan_id),
                    INDEX idx_report_create_time (create_time),
                    INDEX idx_report_route (start_place, end_place)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (Exception e) {
            throw new IllegalStateException("初始化 travel_service_report 表失败", e);
        }
    }

    private void createBookingTables() {
        String hotelSql = """
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
                """;
        String roomSql = """
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
                """;
        String trafficSql = """
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
                """;
        String orderSql = """
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
                """;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(hotelSql);
            statement.execute(roomSql);
            statement.execute(trafficSql);
            statement.execute(orderSql);
        } catch (Exception e) {
            throw new IllegalStateException("初始化酒店/交通/订单表失败", e);
        }
    }

    private void createHotelOrderTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS hotel_order (
                    id BIGINT NOT NULL PRIMARY KEY,
                    user_id VARCHAR(80) NOT NULL,
                    hotel_id VARCHAR(120) DEFAULT NULL,
                    hotel_name VARCHAR(180) DEFAULT NULL,
                    city VARCHAR(80) DEFAULT NULL,
                    address VARCHAR(255) DEFAULT NULL,
                    check_in_date DATE DEFAULT NULL,
                    check_out_date DATE DEFAULT NULL,
                    adults INT DEFAULT 2,
                    children INT DEFAULT 0,
                    rooms INT DEFAULT 1,
                    room_type VARCHAR(120) DEFAULT NULL,
                    price DECIMAL(10,2) DEFAULT 0,
                    status VARCHAR(32) DEFAULT 'PENDING',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_hotel_order_user (user_id),
                    INDEX idx_hotel_order_status (status),
                    INDEX idx_hotel_order_create_time (create_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (Exception e) {
            throw new IllegalStateException("初始化 hotel_order 表失败", e);
        }
    }

    private void createTimelineTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS travel_timeline_item (
                    id BIGINT NOT NULL PRIMARY KEY,
                    user_id VARCHAR(80) NOT NULL,
                    trip_plan_id BIGINT DEFAULT NULL,
                    stage VARCHAR(32) NOT NULL,
                    service_type VARCHAR(64) NOT NULL,
                    title VARCHAR(160) NOT NULL,
                    content VARCHAR(1000) DEFAULT NULL,
                    trigger_time VARCHAR(120) DEFAULT NULL,
                    status VARCHAR(32) DEFAULT 'PENDING',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_timeline_user (user_id),
                    INDEX idx_timeline_stage (stage),
                    INDEX idx_timeline_trip (trip_plan_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (Exception e) {
            throw new IllegalStateException("初始化 travel_timeline_item 表失败", e);
        }
    }

    private void createAfterSaleOrderTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS after_sale_order (
                    id BIGINT NOT NULL PRIMARY KEY,
                    user_id VARCHAR(80) NOT NULL,
                    trip_plan_id BIGINT DEFAULT NULL,
                    order_id BIGINT DEFAULT NULL,
                    service_type VARCHAR(64) NOT NULL,
                    title VARCHAR(160) DEFAULT NULL,
                    description VARCHAR(1000) DEFAULT NULL,
                    status VARCHAR(32) DEFAULT 'PENDING',
                    processor VARCHAR(80) DEFAULT NULL,
                    process_remark VARCHAR(1000) DEFAULT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_after_sale_user (user_id),
                    INDEX idx_after_sale_order (order_id),
                    INDEX idx_after_sale_status (status),
                    INDEX idx_after_sale_type (service_type)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            statement.executeUpdate("""
                    INSERT IGNORE INTO after_sale_order
                    (id, user_id, trip_plan_id, order_id, service_type, title, description, status, processor, process_remark)
                    VALUES
                    (5001, 'test_user', NULL, 4002, 'CHANGE', '高铁票改签申请', '用户希望将南京到杭州高铁改到下午出发', 'PENDING', NULL, NULL),
                    (5002, 'test_user', NULL, 4001, 'LUGGAGE', '行李服务咨询', '咨询机票行李托运额度和到达后寄存服务', 'PROCESSING', 'admin', '已提醒用户核验航司托运规则')
                    """);
        } catch (Exception e) {
            throw new IllegalStateException("初始化 after_sale_order 表失败", e);
        }
    }

    private void seedBookingDemoData() {
        String sql = """
                INSERT IGNORE INTO hotel
                (id, name, city, address, cover_image, star_level, score, min_price, tags, description)
                VALUES
                (1001, '杭州西湖湖滨智选酒店', '杭州', '杭州市上城区湖滨路88号', 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=80', 4, 4.7, 428, '近地铁,西湖步行圈,早餐', '适合西湖周边休闲旅行，步行可达湖滨商圈。'),
                (1002, '杭州东站轻住酒店', '杭州', '杭州市上城区火车东站东广场', 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=900&q=80', 3, 4.5, 299, '近高铁站,低预算,交通便利', '适合跨城高铁到达后的短住与中转。'),
                (1003, '南京南站商务酒店', '南京', '南京市雨花台区南京南站北广场', 'https://images.unsplash.com/photo-1564501049412-61c2a3083791?auto=format&fit=crop&w=900&q=80', 4, 4.6, 358, '商务出行,近地铁,可开发票', '适合商务差旅和高铁出行用户。'),
                (1004, '上海人民广场城市酒店', '上海', '上海市黄浦区人民广场地铁站旁', 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=900&q=80', 4, 4.8, 520, '市中心,近地铁,购物便利', '适合城市观光、商务与购物出行。'),
                (1005, '南京夫子庙文化酒店', '南京', '南京市秦淮区夫子庙景区入口', 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=900&q=80', 4, 4.6, 399, '景区周边,美食,亲子', '适合文旅场景和家庭亲子出游。'),
                (1006, '杭州钱江新城行政公寓', '杭州', '杭州市上城区钱江新城核心区', 'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=900&q=80', 5, 4.9, 760, '江景,商务,行政酒廊', '适合商务会议和高品质城市度假。'),
                (1007, '上海虹桥枢纽精选酒店', '上海', '上海市闵行区虹桥交通枢纽旁', 'https://images.unsplash.com/photo-1445019980597-93fa8acb246c?auto=format&fit=crop&w=900&q=80', 4, 4.7, 488, '近机场,近高铁,商务', '适合飞机和高铁联程出行。'),
                (1008, '苏州金鸡湖度假酒店', '苏州', '苏州市工业园区金鸡湖景区', 'https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=900&q=80', 5, 4.8, 699, '湖景,亲子,休闲度假', '适合周末短途和亲子休闲。'),
                (1009, '北京国贸商务酒店', '北京', '北京市朝阳区国贸商务区', 'https://images.unsplash.com/photo-1561501878-aabd62634533?auto=format&fit=crop&w=900&q=80', 5, 4.7, 820, '商务出行,近地铁,会议', '适合企业差旅、会议和高频商务出行。'),
                (1010, '成都春熙路城市酒店', '成都', '成都市锦江区春熙路商圈', 'https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=900&q=80', 4, 4.6, 389, '美食,市中心,低预算', '适合城市漫游和美食旅行。');

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
                (2010, 1005, '文化庭院房', '1张大床', '双早', 459, 6, 1),
                (2011, 1006, '钱江江景大床房', '1张大床', '双早', 860, 6, 1),
                (2012, 1006, '行政套房', '1张大床', '行政礼遇', 1280, 3, 1),
                (2013, 1007, '虹桥商务大床房', '1张大床', '双早', 528, 10, 1),
                (2014, 1007, '机场中转双床房', '2张单人床', '无早', 458, 14, 1),
                (2015, 1008, '金鸡湖亲子房', '1大1小双床', '三早', 759, 5, 1),
                (2016, 1008, '湖景双床房', '2张单人床', '双早', 699, 8, 1),
                (2017, 1009, '国贸商务大床房', '1张大床', '双早', 880, 7, 1),
                (2018, 1009, '会议行政房', '1张大床', '行政早餐', 1180, 4, 1),
                (2019, 1010, '春熙路舒适大床房', '1张大床', '双早', 429, 11, 1),
                (2020, 1010, '美食街景双床房', '2张单人床', '双早', 469, 9, 1);

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
                (3203, 'TAXI', '杭州东站', '西湖湖滨', '2026-05-25 10:20:00', '2026-05-25 10:55:00', '35分', 58, '网约车预估', '舒适型', 99),
                (3204, 'BUS', '上海长途客运南站', '杭州汽车北站', '2026-05-25 09:20:00', '2026-05-25 12:10:00', '2小时50分', 72, '上海-杭州巴士', '普通座', 32),
                (3205, 'METRO', '南京南站', '新街口', '2026-05-25 09:00:00', '2026-05-25 09:28:00', '28分', 4, '南京地铁1号线', '地铁', 999),
                (3206, 'TAXI', '上海虹桥枢纽', '人民广场', '2026-05-25 11:30:00', '2026-05-25 12:05:00', '35分', 82, '网约车预估', '商务型', 80),
                (3207, 'TRAIN', '苏州北', '杭州东', '2026-05-25 14:05:00', '2026-05-25 15:32:00', '1小时27分', 96, 'G7387', '二等座', 58),
                (3208, 'FLIGHT', '北京首都机场', '杭州萧山机场', '2026-05-25 08:10:00', '2026-05-25 10:20:00', '2小时10分', 680, 'QW6601', '经济舱', 44),
                (3209, 'TRAIN', '北京南', '南京南', '2026-05-25 09:00:00', '2026-05-25 12:38:00', '3小时38分', 443, 'G105', '二等座', 68),
                (3210, 'FLIGHT', '成都天府机场', '上海虹桥机场', '2026-05-25 12:40:00', '2026-05-25 15:20:00', '2小时40分', 760, 'QW8808', '经济舱', 35);

                INSERT IGNORE INTO booking_order
                (id, user_id, order_no, order_type, product_id, product_name, start_place, end_place, start_time, end_time, total_amount, contact_name, contact_phone, status, remark)
                VALUES
                (4001, 'test_user', 'DEMO202605250001', 'FLIGHT', 3101, 'QW1021 · 经济舱', '南京禄口机场', '杭州萧山机场', '2026-05-25 09:30:00', '2026-05-25 10:40:00', 420, '体验用户', '13800000000', 'PAID', '演示订单：助手订票联调用'),
                (4002, 'test_user', 'DEMO202605250002', 'TRAIN', 3001, 'G7605 · 二等座', '南京南', '杭州东', '2026-05-25 08:20:00', '2026-05-25 10:05:00', 117, '体验用户', '13800000000', 'UNPAID', '演示订单：待确认高铁订单'),
                (4003, 'test_user', 'DEMO202605250003', 'HOTEL', 1001, '杭州西湖湖滨智选酒店', '南京', '杭州', '2026-05-25 14:00:00', '2026-05-26 12:00:00', 528, '体验用户', '13800000000', 'PAID', '演示订单：酒店联调用'),
                (4004, 'travel-user', 'DEMO202605250004', 'TAXI', 3203, '网约车预估 · 舒适型', '杭州东站', '西湖湖滨', '2026-05-25 10:20:00', '2026-05-25 10:55:00', 58, '体验用户', '13800000000', 'CANCELLED', '演示订单：取消状态展示');
                """;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String item : sql.split(";")) {
                if (!item.trim().isEmpty()) {
                    statement.execute(item);
                }
            }
            seedNanjingHangzhouScenarioData(statement);
        } catch (Exception e) {
            throw new IllegalStateException("初始化酒店/交通模拟数据失败", e);
        }
    }
    private void seedNanjingHangzhouScenarioData(Statement statement) throws Exception {
        String coverImage = "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=80";
        String[] hangzhouAreas = {
                "杭州市余杭区文一西路阿里巴巴西溪园区",
                "杭州市余杭区未来科技城海创园",
                "杭州市余杭区欧美金融城",
                "杭州市余杭区仓前街道良睦路",
                "杭州市余杭区杭州西站枢纽"
        };
        String[] hotelBrands = {
                "七维智选", "云栖商务", "西溪轻居", "未来科技城行政", "同城接驳", "蓝湾精选",
                "启程商务", "景行城市", "行云酒店", "智行公寓", "西站便捷", "阿里园区优选"
        };
        String[] hotelTags = {
                "近阿里园区,商务出差,可开发票",
                "近地铁,早餐,接驳便利",
                "低预算,安静,短住",
                "行政酒廊,会议,高评分",
                "近杭州西站,行李寄存,晚到保留"
        };
        for (int i = 0; i < 60; i++) {
            long hotelId = 11000L + i;
            String area = hangzhouAreas[i % hangzhouAreas.length];
            String brand = hotelBrands[i % hotelBrands.length];
            String name = brand + (i + 1) + "号酒店";
            int star = 3 + (i % 3);
            String score = String.format(Locale.ROOT, "%.1f", 4.3 + (i % 7) * 0.08);
            int price = 260 + (i % 18) * 24;
            String tags = hotelTags[i % hotelTags.length];
            statement.executeUpdate(String.format(Locale.ROOT, """
                    INSERT IGNORE INTO hotel
                    (id, name, city, address, cover_image, star_level, score, min_price, tags, description)
                    VALUES (%d, '%s', '杭州', '%s', '%s', %d, %s, %d, '%s', '%s')
                    """,
                    hotelId, q(name), q(area + " " + (100 + i) + "号"), q(coverImage), star, score, price, q(tags),
                    q("面向南京交通职业技术学院到杭州阿里巴巴总部测试链路的周边住宿资源。")));
            statement.executeUpdate(String.format(Locale.ROOT, """
                    INSERT IGNORE INTO hotel_room
                    (id, hotel_id, room_name, bed_type, breakfast, price, stock, refundable)
                    VALUES (%d, %d, '%s', '%s', '%s', %d, %d, 1)
                    """,
                    21000L + i, hotelId, q(i % 2 == 0 ? "商务大床房" : "舒适双床房"),
                    q(i % 2 == 0 ? "1张大床" : "2张单人床"), q(i % 3 == 0 ? "双早" : "单早"), price + 40, 8 + (i % 12)));
        }

        // 清理旧的种子数据，避免 INSERT IGNORE 保留脏数据
        statement.executeUpdate("DELETE FROM traffic_product WHERE id BETWEEN 33000 AND 33309");

        for (int i = 0; i < 18; i++) {
            int hour = 7 + (i % 10);
            statement.executeUpdate(String.format(Locale.ROOT, """
                    INSERT IGNORE INTO traffic_product
                    (id, type, start_place, end_place, depart_time, arrive_time, duration, price, carrier_name, seat_type, remain_count)
                    VALUES (%d, 'TRAIN', '南京南', '杭州东', '2026-06-%02d %02d:%02d:00', '2026-06-%02d %02d:%02d:00', '%d小时%d分', %d, '%s', '%s', %d)
                    """,
                    33000L + i, 3 + (i % 12), hour, (i * 7) % 60, 3 + (i % 12), hour + 2, ((i * 7) + 3) % 60,
                    1 + (i % 2), 38 + (i % 18), 117 + (i % 5) * 22, q("G" + (7601 + i * 2)),
                    q(i % 4 == 0 ? "一等座" : "二等座"), 25 + i));
        }
        for (int i = 0; i < 10; i++) {
            int hour = 8 + (i % 8);
            statement.executeUpdate(String.format(Locale.ROOT, """
                    INSERT IGNORE INTO traffic_product
                    (id, type, start_place, end_place, depart_time, arrive_time, duration, price, carrier_name, seat_type, remain_count)
                    VALUES (%d, 'FLIGHT', '南京禄口机场', '杭州萧山机场', '2026-06-%02d %02d:%02d:00', '2026-06-%02d %02d:%02d:00', '1小时10分', %d, '%s', '经济舱', %d)
                    """,
                    33100L + i, 4 + (i % 10), hour, (i * 11) % 60, 4 + (i % 10), hour + 1, ((i * 11) + 10) % 60,
                    360 + i * 28, q("QW-NH" + (1200 + i)), 20 + i));
        }
        for (int i = 0; i < 8; i++) {
            int hour = 9 + (i % 6);
            statement.executeUpdate(String.format(Locale.ROOT, """
                    INSERT IGNORE INTO traffic_product
                    (id, type, start_place, end_place, depart_time, arrive_time, duration, price, carrier_name, seat_type, remain_count)
                    VALUES (%d, 'CRUISE', '南京五马渡码头', '杭州武林门码头', '2026-06-%02d %02d:00:00', '2026-06-%02d %02d:30:00', '5小时30分', %d, '%s', '观光舱', %d)
                    """,
                    33200L + i, 5 + (i % 8), hour, 5 + (i % 8), hour + 5, 188 + i * 18, q("运河联程邮轮 " + (i + 1) + "号"), 15 + i));
        }
        for (int i = 0; i < 10; i++) {
            int hour = 10 + (i % 8);
            int departMinute = (i * 9) % 60;
            int durationMinutes = 38 + i;
            int arriveTotalMinutes = hour * 60 + departMinute + durationMinutes;
            int arriveHour = arriveTotalMinutes / 60;
            int arriveMinute = arriveTotalMinutes % 60;
            statement.executeUpdate(String.format(Locale.ROOT, """
                    INSERT IGNORE INTO traffic_product
                    (id, type, start_place, end_place, depart_time, arrive_time, duration, price, carrier_name, seat_type, remain_count)
                    VALUES (%d, 'TAXI', '杭州东站', '杭州阿里巴巴西溪园区', '2026-06-%02d %02d:%02d:00', '2026-06-%02d %02d:%02d:00', '%d分', %d, '%s', '商务型', %d)
                    """,
                    33300L + i, 3 + (i % 12), hour, departMinute, 3 + (i % 12), arriveHour, arriveMinute,
                    durationMinutes, 52 + i * 3, q("园区接驳专车 " + (i + 1)), 30 + i));
        }

        String[] orderTypes = {"TRAIN", "FLIGHT", "HOTEL", "CRUISE", "TAXI"};
        String[] statuses = {"UNPAID", "PAID", "CONFIRMED", "CANCELLED"};
        for (int i = 0; i < 24; i++) {
            String type = orderTypes[i % orderTypes.length];
            long productId = switch (type) {
                case "TRAIN" -> 33000L + (i % 18);
                case "FLIGHT" -> 33100L + (i % 10);
                case "CRUISE" -> 33200L + (i % 8);
                case "TAXI" -> 33300L + (i % 10);
                default -> 11000L + (i % 60);
            };
            String productName = switch (type) {
                case "TRAIN" -> "南京南至杭州东高铁联程";
                case "FLIGHT" -> "南京禄口至杭州萧山航班";
                case "CRUISE" -> "南京至杭州运河邮轮";
                case "TAXI" -> "杭州东站至阿里园区接驳";
                default -> "阿里园区周边酒店";
            };
            statement.executeUpdate(String.format(Locale.ROOT, """
                    INSERT IGNORE INTO booking_order
                    (id, user_id, order_no, order_type, product_id, product_name, start_place, end_place, start_time, end_time, total_amount, contact_name, contact_phone, status, remark)
                    VALUES (%d, 'test_user', '%s', '%s', %d, '%s', '%s', '%s', '2026-06-%02d 09:00:00', '2026-06-%02d 12:00:00', %d, '测试用户', '13800000000', '%s', '南京交通职业技术学院到杭州阿里巴巴总部演示订单')
                    """,
                    43000L + i, q("DEMO-NJHZ-202606-" + String.format(Locale.ROOT, "%03d", i + 1)), q(type), productId, q(productName),
                    q(type.equals("HOTEL") ? "南京交通职业技术学院" : "南京"), q(type.equals("HOTEL") ? "杭州阿里巴巴西溪园区" : "杭州"),
                    3 + (i % 12), 3 + (i % 12), 180 + i * 23, q(statuses[i % statuses.length])));
        }
    }

    private void createPlanningFoundationTables() {
        String transportHubSql = """
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
                    UNIQUE KEY uk_transport_hub_city_name_type (city_name, hub_name, hub_type),
                    INDEX idx_transport_hub_city (city_name),
                    INDEX idx_transport_hub_type (hub_type)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;
        String trainProductSql = """
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
                    UNIQUE KEY uk_train_product_no_time_seat (train_no, departure_time, seat_type),
                    INDEX idx_train_product_route (start_station, end_station),
                    INDEX idx_train_product_city_route (start_city, end_city),
                    INDEX idx_train_product_departure (departure_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;
        String hotelProductSql = """
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
                    UNIQUE KEY uk_hotel_product_name_address (hotel_name, address),
                    INDEX idx_hotel_product_city (city_name),
                    INDEX idx_hotel_product_near_place (near_place),
                    INDEX idx_hotel_product_geo (longitude, latitude)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;
        String reminderTemplateSql = """
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
                    UNIQUE KEY uk_reminder_template_scene_type (scene_type, reminder_type, title),
                    INDEX idx_reminder_template_scene (scene_type)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(transportHubSql);
            statement.execute(trainProductSql);
            statement.execute(hotelProductSql);
            statement.execute(reminderTemplateSql);
        } catch (Exception e) {
            throw new IllegalStateException("初始化智能规划基础表失败", e);
        }
    }

    private void seedPlanningFoundationData() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM transport_hub WHERE id BETWEEN 61001 AND 62004");
            statement.executeUpdate("DELETE FROM train_product WHERE id BETWEEN 63001 AND 63012");
            statement.executeUpdate("DELETE FROM hotel_product WHERE id BETWEEN 64001 AND 64015");
            statement.executeUpdate("DELETE FROM reminder_template WHERE id BETWEEN 65001 AND 65012");
            statement.executeUpdate("""
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
                    (62004, '杭州', '杭州汽车客运中心', 'BUS_STATION', '杭州市上城区九沙大道', 120.270476, 30.315034, '330102', 1, 'ENABLED')
                    """);
            statement.executeUpdate("""
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
                    (63010, 'G7587', '南京南站', '杭州东站', '南京', '杭州', '2026-06-01 18:22:00', '2026-06-01 20:01:00', 99, '二等座', 136, 27, 'AVAILABLE', 'LOCAL_DEMO'),
                    (63011, 'G7605', '南京南站', '杭州东站', '南京', '杭州', '2026-06-01 08:12:00', '2026-06-01 09:48:00', 96, '一等座', 188, 18, 'AVAILABLE', 'LOCAL_DEMO'),
                    (63012, 'G7611', '南京南站', '杭州东站', '南京', '杭州', '2026-06-01 08:46:00', '2026-06-01 10:18:00', 92, '一等座', 206, 12, 'AVAILABLE', 'LOCAL_DEMO')
                    """);
            statement.executeUpdate("""
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
                    (64015, '滨江网商路智选酒店', '杭州', '阿里巴巴滨江园区', '杭州市滨江区网商路', 120.193100, 30.185900, 4.5, 386, 19, '网商路,可开发票,短住', '距阿里滨江园区约900米', 'AVAILABLE', 'LOCAL_DEMO')
                    """);
            statement.executeUpdate("""
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
                    (65012, 'CATCH_TRAIN_FLIGHT', 'TRAFFIC', '迟到风险提醒', '系统将监测道路耗时变化，风险升高时提示备选方案。', 1, '行程中')
                    """);
        } catch (Exception e) {
            throw new IllegalStateException("初始化南京杭州智能规划 seed 数据失败", e);
        }
    }

    private String q(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}
