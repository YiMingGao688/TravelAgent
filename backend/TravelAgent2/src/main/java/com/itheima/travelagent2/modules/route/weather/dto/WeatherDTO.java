package com.itheima.travelagent2.modules.route.weather.dto;

import lombok.Data;

@Data
public class WeatherDTO {
    private String province;
    private String city;
    private String adcode;
    private String weather;
    private String temperature;
    private String winddirection;
    private String windpower;
    private String humidity;
    private String reporttime;
}