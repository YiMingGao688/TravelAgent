package com.itheima.travelagent2.modules.chain.dto;

import lombok.Data;

@Data
public class TravelGeoPointDTO {
    private String name;
    private String formattedAddress;
    private Double lng;
    private Double lat;
    private String adcode;
    private String city;
    private String district;
}
