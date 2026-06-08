package com.itheima.travelagent2.modules.chain;

import com.itheima.travelagent2.modules.amap.AmapRouteService;
import com.itheima.travelagent2.modules.amap.dto.AmapRouteResult;
import com.itheima.travelagent2.modules.chain.dto.TripSegmentDTO;
import com.itheima.travelagent2.modules.chain.dto.TravelChainRequestDTO;
import com.itheima.travelagent2.modules.hub.entity.TransportHub;
import com.itheima.travelagent2.modules.hub.service.TransportHubService;
import com.itheima.travelagent2.modules.hub.service.impl.TransportHubServiceImpl;
import com.itheima.travelagent2.modules.intent.dto.StructuredTravelDemand;
import com.itheima.travelagent2.modules.product.hotel.entity.HotelProduct;
import com.itheima.travelagent2.modules.product.hotel.service.HotelProductService;
import com.itheima.travelagent2.modules.product.train.entity.TrainProduct;
import com.itheima.travelagent2.modules.product.train.service.TrainProductService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TripSegmentBuilderService {
    private final TransportHubService transportHubService;
    private final TrainProductService trainProductService;
    private final HotelProductService hotelProductService;
    private final AmapRouteService amapRouteService;

    public TripSegmentBuilderService(TransportHubService transportHubService,
                                     TrainProductService trainProductService,
                                     HotelProductService hotelProductService,
                                     AmapRouteService amapRouteService) {
        this.transportHubService = transportHubService;
        this.trainProductService = trainProductService;
        this.hotelProductService = hotelProductService;
        this.amapRouteService = amapRouteService;
    }

    public SegmentBuildResult build(TravelChainRequestDTO request, StructuredTravelDemand demand) {
        // ★ 城市名标准化
        String startCity = normalizeCityFull(firstText(request.getStartCity(), inferCity(demand.getStartPlace())));
        String endCity = normalizeCityFull(firstText(request.getEndCity(), inferCity(demand.getEndPlace())));
        boolean crossCity = hasText(startCity) && hasText(endCity) && !startCity.equals(endCity);

        // ★ 请求级缓存：同一规划内相同查询只查一次数据库
        Map<String, Object> requestCache = new HashMap<>();

        List<TripSegmentDTO> segments = new ArrayList<>();
        List<TrainProduct> trains = new ArrayList<>();
        List<HotelProduct> hotels = new ArrayList<>();

        if (!crossCity) {
            segments.add(localRoute("LOCAL_TRANSFER", demand.getStartPlace(), demand.getEndPlace(),
                    request.getStartLocation(), request.getEndLocation()));
            return new SegmentBuildResult(segments, trains, hotels);
        }

        TransportHub startHub = transportHubService.recommend(startCity, endCity, "TRAIN", true);
        TransportHub endHub = transportHubService.recommend(startCity, endCity, "TRAIN", false);
        if (startHub == null || endHub == null) {
            throw new IllegalStateException("跨城规划缺少交通枢纽数据：" + startCity + " → " + endCity);
        }

        segments.add(localRoute("LOCAL_ACCESS", demand.getStartPlace(), startHub.getHubName(),
                request.getStartLocation(), lngLat(startHub)));

        // ★ 使用请求级缓存查询高铁产品
        String trainDate = normalizeDate(demand.getDepartureDate());
        String trainCacheKey = "TRAIN|" + startHub.getHubName() + "|" + endHub.getHubName() + "|" + trainDate;
        @SuppressWarnings("unchecked")
        List<TrainProduct> cachedTrains = (List<TrainProduct>) requestCache.get(trainCacheKey);
        if (cachedTrains == null) {
            cachedTrains = trainProductService.search(startHub.getHubName(), endHub.getHubName(), trainDate);
            requestCache.put(trainCacheKey, cachedTrains);
            System.out.println("[SEGMENT] train search key=" + trainCacheKey + " count=" + cachedTrains.size());
        } else {
            System.out.println("[SEGMENT] train cache hit key=" + trainCacheKey);
        }
        trains = cachedTrains;
        TrainProduct train = trains.isEmpty() ? null : trains.get(0);
        segments.add(intercityTrainSegment(startHub, endHub, train));

        if (!samePlace(demand.getEndPlace(), endHub.getHubName())) {
            segments.add(localRoute("LOCAL_TRANSFER", endHub.getHubName(), demand.getEndPlace(),
                    lngLat(endHub), request.getEndLocation()));
        }

        if (Boolean.TRUE.equals(demand.getNeedHotel())) {
            String hotelCacheKey = "HOTEL|" + endCity + "|" + demand.getEndPlace();
            @SuppressWarnings("unchecked")
            List<HotelProduct> cachedHotels = (List<HotelProduct>) requestCache.get(hotelCacheKey);
            if (cachedHotels == null) {
                hotels = hotelProductService.search(endCity, demand.getEndPlace(), null, null, 3000);
                if (hotels.isEmpty()) {
                    hotels = hotelProductService.search(endCity, endHub.getHubName(), null, null, 3000);
                }
                requestCache.put(hotelCacheKey, hotels);
                System.out.println("[SEGMENT] hotel search city=" + endCity + " near=" + demand.getEndPlace() + " count=" + hotels.size());
            } else {
                hotels = cachedHotels;
                System.out.println("[SEGMENT] hotel cache hit key=" + hotelCacheKey);
            }

            if (!hotels.isEmpty()) {
                HotelProduct hotel = hotels.get(0);
                segments.add(localRoute("HOTEL_TRANSFER", demand.getEndPlace(), hotel.getHotelName(),
                        request.getEndLocation(), lngLat(hotel)));
                segments.get(segments.size() - 1).setProductId(hotel.getId());
                segments.get(segments.size() - 1).setProduct(hotel);
            }
        }

        System.out.println("[SEGMENT] build done startCity=" + startCity + " endCity=" + endCity
                + " crossCity=" + crossCity + " segments=" + segments.size()
                + " trains=" + trains.size() + " hotels=" + hotels.size());

        return new SegmentBuildResult(segments, trains, hotels);
    }

    private TripSegmentDTO localRoute(String type, String startName, String endName, String startLngLat, String endLngLat) {
        AmapRouteResult route = amapRouteService.planDriving(startLngLat, endLngLat);
        TripSegmentDTO segment = new TripSegmentDTO();
        segment.setSegmentType(type);
        segment.setTitle(startName + " → " + endName);
        segment.setStartPlace(startName);
        segment.setEndPlace(endName);
        segment.setStartLocation(startLngLat);
        segment.setEndLocation(endLngLat);
        segment.setTransportMode(type.equals("LOCAL_ACCESS") || type.equals("LOCAL_TRANSFER") ? "TAXI" : "DRIVE");
        segment.setDurationMinutes(route.getDurationMinutes());
        segment.setDistanceKm(route.getDistanceKm());
        segment.setEstimatedCost(BigDecimal.valueOf(route.getEstimatedCost() == null ? 0D : route.getEstimatedCost()));
        segment.setRouteSummary(route.getRouteSummary());
        segment.setRoutePolyline(route.getRawPolyline());
        segment.setSource("AMAP");
        return segment;
    }

    private TripSegmentDTO intercityTrainSegment(TransportHub startHub, TransportHub endHub, TrainProduct train) {
        TripSegmentDTO segment = new TripSegmentDTO();
        segment.setSegmentType("INTERCITY");
        segment.setTitle(startHub.getHubName() + " → " + endHub.getHubName());
        segment.setStartPlace(startHub.getHubName());
        segment.setEndPlace(endHub.getHubName());
        segment.setStartLocation(lngLat(startHub));
        segment.setEndLocation(lngLat(endHub));
        segment.setTransportMode("TRAIN");
        segment.setSource("LOCAL_DEMO");
        if (train != null) {
            segment.setDurationMinutes(train.getDurationMinutes());
            segment.setEstimatedCost(train.getPrice());
            segment.setProductId(train.getId());
            segment.setProduct(train);
            segment.setRouteSummary(train.getTrainNo() + " " + train.getSeatType() + " "
                    + train.getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                    + "-"
                    + train.getArrivalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        } else {
            segment.setRouteSummary("未匹配到可预约高铁资源，请调整日期或车站。");
            segment.setEstimatedCost(BigDecimal.ZERO);
        }
        return segment;
    }

    private String lngLat(TransportHub hub) {
        return hub.getLongitude() + "," + hub.getLatitude();
    }

    private String lngLat(HotelProduct hotel) {
        return hotel.getLongitude() + "," + hotel.getLatitude();
    }

    String normalizeDate(String date) {
        if (!hasText(date)) {
            return null;
        }
        String value = date.trim();
        if (value.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
            String[] parts = value.split("-");
            return "%04d-%02d-%02d".formatted(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
        if (value.matches("\\d{1,2}月\\d{1,2}[日号]")) {
            String[] parts = value.replace("日", "").replace("号", "").split("月");
            return "2026-%02d-%02d".formatted(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        return null;
    }

    private String inferCity(String place) {
        if (!hasText(place)) return "";
        if (place.contains("南京")) return "南京";
        if (place.contains("杭州") || place.contains("阿里巴巴")) return "杭州";
        if (place.contains("上海")) return "上海";
        if (place.contains("北京")) return "北京";
        if (place.contains("苏州")) return "苏州";
        if (place.contains("成都")) return "成都";
        if (place.contains("广州")) return "广州";
        if (place.contains("深圳")) return "深圳";
        return "";
    }

    /**
     * ★ 使用统一的城市名标准化方法。
     */
    private String normalizeCityFull(String raw) {
        return TransportHubServiceImpl.normalizeCityName(raw);
    }

    private boolean samePlace(String a, String b) {
        return hasText(a) && hasText(b) && (a.contains(b.replace("站", "")) || b.contains(a.replace("站", "")));
    }

    private String firstText(String... values) {
        for (String value : values) if (hasText(value)) return value.trim();
        return "";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public record SegmentBuildResult(List<TripSegmentDTO> segments, List<TrainProduct> trains, List<HotelProduct> hotels) {
    }
}
