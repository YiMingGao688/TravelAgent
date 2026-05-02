package com.itheima.travelagent2.modules.smartservice.service;

import com.itheima.travelagent2.modules.smartservice.dto.SmartServiceRequestDTO;
import com.itheima.travelagent2.modules.smartservice.dto.TravelReportRequestDTO;
import com.itheima.travelagent2.modules.smartservice.entity.SmartServiceRecord;

import java.util.List;

public interface SmartService {
    SmartServiceRecord generate(SmartServiceRequestDTO dto);

    SmartServiceRecord generateReport(TravelReportRequestDTO dto);

    List<SmartServiceRecord> listByUserId(String userId);

    List<SmartServiceRecord> listAll();

    void deleteById(Long id);
}
