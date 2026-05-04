package com.itheima.travelagent2.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class DatabaseInitializer {

    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        createSmartServiceRecordTable();
        createTravelServiceReportTable();
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
}
