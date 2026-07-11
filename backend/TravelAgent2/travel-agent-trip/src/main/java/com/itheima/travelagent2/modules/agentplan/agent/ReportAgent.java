package com.itheima.travelagent2.modules.agentplan.agent;

import com.itheima.travelagent2.modules.agentplan.entity.AgentStepRecord;
import com.itheima.travelagent2.modules.agentplan.entity.PlanSession;
import com.itheima.travelagent2.modules.agentplan.entity.TripItem;
import com.itheima.travelagent2.modules.agentplan.entity.TripPlanSummary;
import com.itheima.travelagent2.modules.agentplan.mapper.TripPlanSummaryMapper;
import com.itheima.travelagent2.modules.agentplan.service.AgentStepRecorder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReportAgent {
    private final AgentStepRecorder recorder;
    private final TripPlanSummaryMapper summaryMapper;

    public ReportAgent(AgentStepRecorder recorder, TripPlanSummaryMapper summaryMapper) {
        this.recorder = recorder;
        this.summaryMapper = summaryMapper;
    }

    public TripPlanSummary generate(PlanSession session, List<TripItem> items, TripPlanSummary existing) {
        AgentStepRecord step = recorder.start(session.getId(), "ReportAgent", "FINAL_SUMMARY",
                "总行程卡片生成", "正在汇总已确认行程、总费用、风险和提醒事项", items);
        BigDecimal totalPrice = items.stream()
                .map(TripItem::getPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long transportCount = items.stream().filter(item -> !"HOTEL".equals(item.getItemType())
                && !"REMINDER".equals(item.getItemType())).count();
        long hotelCount = items.stream().filter(item -> "HOTEL".equals(item.getItemType())).count();
        long reminderCount = items.stream().filter(item -> "REMINDER".equals(item.getItemType())).count();

        TripPlanSummary summary = existing == null ? new TripPlanSummary() : existing;
        summary.setSessionId(session.getId());
        summary.setUserId(session.getUserId());
        summary.setTitle(firstText(session.getSessionTitle(), session.getStartPlace() + " 到 " + session.getEndPlace() + " 智能行程"));
        summary.setSummaryText("已确认 " + items.size() + " 个行程项，包含交通 " + transportCount
                + " 项、住宿 " + hotelCount + " 项、提醒 " + reminderCount + " 项。");
        summary.setTotalPrice(totalPrice);
        summary.setTotalDuration("以已确认交通与住宿时间为准");
        summary.setRiskLevel(items.size() >= 3 ? "LOW" : "MEDIUM");
        summary.setReminderJson(recorder.toJson(items.stream().filter(item -> "REMINDER".equals(item.getItemType())).toList()));
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("transportCount", transportCount);
        report.put("hotelCount", hotelCount);
        report.put("reminderCount", reminderCount);
        report.put("items", items);
        summary.setReportJson(recorder.toJson(report));
        summary.setStatus("DRAFT");
        if (summary.getId() == null) {
            summaryMapper.insert(summary);
        } else {
            summaryMapper.updateById(summary);
        }
        recorder.success(step, "总行程卡片已生成，可确认最终行程、导出报告或保存方案", summary);
        return summary;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
