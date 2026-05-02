package com.itheima.travelagent2.modules.route.dto;

import lombok.Data;

@Data
public class LocationInfo {
    private String name;
    private String formattedAddress;
    private String location;
    private String adcode;
    private String province;
    private String city;
    private String district;
}
