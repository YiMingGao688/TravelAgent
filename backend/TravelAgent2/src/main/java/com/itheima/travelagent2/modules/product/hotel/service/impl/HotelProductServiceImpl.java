package com.itheima.travelagent2.modules.product.hotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.travelagent2.modules.product.hotel.entity.HotelProduct;
import com.itheima.travelagent2.modules.product.hotel.mapper.HotelProductMapper;
import com.itheima.travelagent2.modules.product.hotel.service.HotelProductService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class HotelProductServiceImpl extends ServiceImpl<HotelProductMapper, HotelProduct> implements HotelProductService {
    @Override
    public List<HotelProduct> search(String city, String nearPlace, BigDecimal lng, BigDecimal lat, Integer radius) {
        LambdaQueryWrapper<HotelProduct> wrapper = new LambdaQueryWrapper<>();
        if (hasText(city)) {
            wrapper.like(HotelProduct::getCityName, normalizeCity(city));
        }
        if (hasText(nearPlace)) {
            wrapper.and(w -> w.like(HotelProduct::getNearPlace, nearPlace)
                    .or().like(HotelProduct::getAddress, nearPlace)
                    .or().like(HotelProduct::getTags, nearPlace));
        }
        wrapper.eq(HotelProduct::getStatus, "AVAILABLE")
                .gt(HotelProduct::getStock, 0)
                .orderByDesc(HotelProduct::getRating)
                .orderByAsc(HotelProduct::getPrice);
        List<HotelProduct> products = list(wrapper);
        if (lng != null && lat != null) {
            int maxRadius = radius == null || radius <= 0 ? 3000 : radius;
            return products.stream()
                    .filter(item -> distanceMeters(lng, lat, item.getLongitude(), item.getLatitude()) <= maxRadius)
                    .sorted(Comparator.comparingDouble(item -> distanceMeters(lng, lat, item.getLongitude(), item.getLatitude())))
                    .toList();
        }
        return products;
    }

    private double distanceMeters(BigDecimal lng, BigDecimal lat, BigDecimal itemLng, BigDecimal itemLat) {
        if (lng == null || lat == null || itemLng == null || itemLat == null) {
            return Double.MAX_VALUE;
        }
        double dx = lng.doubleValue() - itemLng.doubleValue();
        double dy = lat.doubleValue() - itemLat.doubleValue();
        return Math.sqrt(dx * dx + dy * dy) * 111_000D;
    }

    private String normalizeCity(String city) {
        return city == null ? "" : city.trim().replace("市", "");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
