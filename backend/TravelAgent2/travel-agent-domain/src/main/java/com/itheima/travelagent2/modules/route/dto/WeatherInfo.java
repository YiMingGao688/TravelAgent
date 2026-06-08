package com.itheima.travelagent2.modules.route.dto;

import lombok.Data;

@Data
public class WeatherInfo {
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
