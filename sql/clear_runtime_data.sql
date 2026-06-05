-- 清空七维智行运行数据：保留 hotel / hotel_room / traffic_product 商品目录，避免首页搜索无产品。
-- 排名、趋势、热门城市等展示会从 booking_order 的实时订单重新统计。
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE booking_order;
TRUNCATE TABLE chat_record;
TRUNCATE TABLE trip_context;
TRUNCATE TABLE trip_plan;
TRUNCATE TABLE route;
TRUNCATE TABLE smart_service_record;
TRUNCATE TABLE travel_service_report;
TRUNCATE TABLE user_profile;
TRUNCATE TABLE user;
SET FOREIGN_KEY_CHECKS = 1;
