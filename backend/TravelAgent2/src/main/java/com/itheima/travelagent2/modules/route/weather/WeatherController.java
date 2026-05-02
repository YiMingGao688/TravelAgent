package com.itheima.travelagent2.modules.route.weather;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.route.weather.dto.WeatherDTO;
import com.itheima.travelagent2.modules.route.weather.service.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/api/weather")
    public Result<WeatherDTO> getWeather(@RequestParam String city) {
        try {
            WeatherDTO weather = weatherService.getWeather(city);
            return Result.success(weather);
        } catch (Exception e) {
            return Result.error(400, "获取天气失败：" + e.getMessage());
        }
    }
}