package com.itheima.travelagent2.modules.route.weather.service;

import com.itheima.travelagent2.modules.route.weather.dto.WeatherDTO;

public interface WeatherService {
    WeatherDTO getWeather(String city);
}