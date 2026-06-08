package com.itheima.travelagent2.modules.hub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.travelagent2.modules.hub.entity.TransportHub;

import java.util.List;

public interface TransportHubService extends IService<TransportHub> {
    List<TransportHub> listByCity(String city);

    TransportHub recommend(String startCity, String endCity, String mode, boolean startSide);
}
