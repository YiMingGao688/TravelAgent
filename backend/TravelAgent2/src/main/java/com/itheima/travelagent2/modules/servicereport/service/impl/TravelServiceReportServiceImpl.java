package com.itheima.travelagent2.modules.servicereport.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.travelagent2.modules.servicereport.dto.TravelServiceReportSaveDTO;
import com.itheima.travelagent2.modules.servicereport.entity.TravelServiceReport;
import com.itheima.travelagent2.modules.servicereport.mapper.TravelServiceReportMapper;
import com.itheima.travelagent2.modules.servicereport.service.TravelServiceReportService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TravelServiceReportServiceImpl implements TravelServiceReportService {

    private final TravelServiceReportMapper reportMapper;

    public TravelServiceReportServiceImpl(TravelServiceReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    @Override
    public TravelServiceReport save(TravelServiceReportSaveDTO dto) {
        TravelServiceReport report = new TravelServiceReport();
        apply(report, dto);
        if (blank(report.getSourceType())) {
            report.setSourceType("USER_SAVE");
        }
        reportMapper.insert(report);
        return report;
    }

    @Override
    public TravelServiceReport update(Long id, TravelServiceReportSaveDTO dto) {
        TravelServiceReport report = reportMapper.selectById(id);
        if (report == null) {
            return null;
        }
        apply(report, dto);
        reportMapper.updateById(report);
        return report;
    }

    @Override
    public List<TravelServiceReport> listByUserId(String userId, String keyword) {
        LambdaQueryWrapper<TravelServiceReport> wrapper = baseKeywordWrapper(keyword);
        wrapper.eq(TravelServiceReport::getUserId, userId)
                .orderByDesc(TravelServiceReport::getCreateTime);
        return reportMapper.selectList(wrapper);
    }

    @Override
    public List<TravelServiceReport> listAll(String keyword) {
        LambdaQueryWrapper<TravelServiceReport> wrapper = baseKeywordWrapper(keyword);
        wrapper.orderByDesc(TravelServiceReport::getCreateTime);
        return reportMapper.selectList(wrapper);
    }

    @Override
    public TravelServiceReport getById(Long id) {
        return reportMapper.selectById(id);
    }

    @Override
    public boolean deleteById(Long id) {
        return reportMapper.deleteById(id) > 0;
    }

    private LambdaQueryWrapper<TravelServiceReport> baseKeywordWrapper(String keyword) {
        LambdaQueryWrapper<TravelServiceReport> wrapper = new LambdaQueryWrapper<>();
        if (!blank(keyword)) {
            String q = keyword.trim();
            wrapper.and(item -> item
                    .like(TravelServiceReport::getUserId, q)
                    .or().like(TravelServiceReport::getTitle, q)
                    .or().like(TravelServiceReport::getStartPlace, q)
                    .or().like(TravelServiceReport::getEndPlace, q)
                    .or().like(TravelServiceReport::getReportSummary, q));
        }
        return wrapper;
    }

    private void apply(TravelServiceReport report, TravelServiceReportSaveDTO dto) {
        report.setUserId(trim(dto.getUserId()));
        report.setTripPlanId(dto.getTripPlanId());
        report.setTitle(defaultTitle(dto));
        report.setStartPlace(trim(dto.getStartPlace()));
        report.setEndPlace(trim(dto.getEndPlace()));
        report.setReportContent(trim(dto.getReportContent()));
        report.setReportSummary(defaultSummary(dto));
        report.setSourceType(trim(dto.getSourceType()));
    }

    private String defaultTitle(TravelServiceReportSaveDTO dto) {
        if (!blank(dto.getTitle())) {
            return dto.getTitle().trim();
        }
        String start = trim(dto.getStartPlace());
        String end = trim(dto.getEndPlace());
        if (!blank(start) && !blank(end)) {
            return "《" + start + "—" + end + "智慧出行服务报告》";
        }
        return "智慧出行服务报告";
    }

    private String defaultSummary(TravelServiceReportSaveDTO dto) {
        if (!blank(dto.getReportSummary())) {
            return dto.getReportSummary().trim();
        }
        String content = trim(dto.getReportContent());
        if (blank(content)) {
            return "";
        }
        return content.length() > 160 ? content.substring(0, 160) : content;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
