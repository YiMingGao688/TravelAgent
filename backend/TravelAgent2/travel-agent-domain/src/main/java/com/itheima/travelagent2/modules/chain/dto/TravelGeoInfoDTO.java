package com.itheima.travelagent2.modules.chain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TravelGeoInfoDTO {
    private TravelGeoPointDTO start;
    private TravelGeoPointDTO end;
    private List<TravelGeoPointDTO> waypoints = new ArrayList<>();
}
