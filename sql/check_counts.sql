SELECT 'booking_order' AS t, COUNT(*) AS c FROM booking_order
UNION ALL SELECT 'chat_record', COUNT(*) FROM chat_record
UNION ALL SELECT 'trip_context', COUNT(*) FROM trip_context
UNION ALL SELECT 'trip_plan', COUNT(*) FROM trip_plan
UNION ALL SELECT 'route', COUNT(*) FROM route
UNION ALL SELECT 'smart_service_record', COUNT(*) FROM smart_service_record
UNION ALL SELECT 'travel_service_report', COUNT(*) FROM travel_service_report
UNION ALL SELECT 'user_profile', COUNT(*) FROM user_profile
UNION ALL SELECT 'user', COUNT(*) FROM user
UNION ALL SELECT 'hotel_catalog', COUNT(*) FROM hotel
UNION ALL SELECT 'traffic_catalog', COUNT(*) FROM traffic_product;
