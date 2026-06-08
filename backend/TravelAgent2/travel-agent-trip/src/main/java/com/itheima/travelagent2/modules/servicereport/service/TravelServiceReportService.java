package com.itheima.travelagent2.modules.servicereport.service;

import com.itheima.travelagent2.modules.servicereport.dto.TravelServiceReportSaveDTO;
import com.itheima.travelagent2.modules.servicereport.entity.TravelServiceReport;

import java.util.List;

public interface TravelServiceReportService {
    TravelServiceReport save(TravelServiceReportSaveDTO dto);

    TravelServiceReport update(Long id, TravelServiceReportSaveDTO dto);

    List<TravelServiceReport> listByUserId(String userId, String keyword);

    List<TravelServiceReport> listAll(String keyword);

    TravelServiceReport getById(Long id);

    boolean deleteById(Long id);
}
