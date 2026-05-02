package com.itheima.travelagent2.modules.smartservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.travelagent2.modules.ai.ZhipuAiClient;
import com.itheima.travelagent2.modules.smartservice.dto.SmartServiceRequestDTO;
import com.itheima.travelagent2.modules.smartservice.dto.TravelReportRequestDTO;
import com.itheima.travelagent2.modules.smartservice.entity.SmartServiceRecord;
import com.itheima.travelagent2.modules.smartservice.mapper.SmartServiceRecordMapper;
import com.itheima.travelagent2.modules.smartservice.service.SmartService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SmartServiceImpl implements SmartService {

    private final SmartServiceRecordMapper recordMapper;
    private final ZhipuAiClient zhipuAiClient;
    private final ObjectMapper objectMapper;

    public SmartServiceImpl(SmartServiceRecordMapper recordMapper,
                            ZhipuAiClient zhipuAiClient,
                            ObjectMapper objectMapper) {
        this.recordMapper = recordMapper;
        this.zhipuAiClient = zhipuAiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public SmartServiceRecord generate(SmartServiceRequestDTO dto) {
        String serviceName = serviceName(dto.getServiceType());
        String systemPrompt = """
                你是面向旅客的一站式出行服务助手。
                只用简体中文回答，不能出现英文标题。
                生成可执行服务方案，不要写技术说明。
                请覆盖行前、行中、行后服务，并包含提醒、特殊照顾、行李、支付权益、售后协助和异常预案。
                不要提模型、API、数据库、比赛、闭环、画像等开发者视角词汇。
                不要编造真实票务、天气、拥堵、支付结果，只能基于用户已提供信息做建议。
                """;

        String userPrompt = """
                服务类型：%s
                用户：%s
                出发地：%s
                目的地：%s
                出行时间：%s
                出行偏好：%s
                特殊需求：%s
                预算：%s
                同行人：%s
                行李：%s
                支付偏好：%s
                出行场景：%s
                补充要求：%s

                请生成一份面向旅客的服务方案，按“行前、行中、行后”三段输出。
                每段至少 2 个节点。每个节点用以下格式：
                【行前】节点标题｜状态：待处理/已完成/风险提醒｜说明：一句可执行建议
                """.formatted(
                serviceName,
                value(dto.getUserId()),
                value(dto.getStartPlace()),
                value(dto.getEndPlace()),
                value(dto.getTravelDate()),
                value(dto.getPreference()),
                value(dto.getSpecialNeed()),
                value(dto.getBudget()),
                value(dto.getPassengerInfo()),
                value(dto.getLuggageInfo()),
                value(dto.getPaymentPreference()),
                value(dto.getScenario()),
                value(dto.getExtraRequirement())
        );

        String response = zhipuAiClient.chat(systemPrompt, userPrompt);

        SmartServiceRecord record = new SmartServiceRecord();
        record.setUserId(value(dto.getUserId()));
        record.setServiceType(dto.getServiceType());
        record.setTitle(serviceName + "方案");
        record.setStartPlace(dto.getStartPlace());
        record.setEndPlace(dto.getEndPlace());
        record.setRequestPayload(toJson(dto));
        record.setResponsePayload(response);
        record.setStatus("GENERATED");
        recordMapper.insert(record);
        return record;
    }

    @Override
    public SmartServiceRecord generateReport(TravelReportRequestDTO dto) {
        String start = value(dto.getStartPlace());
        String end = value(dto.getEndPlace());
        String title = "《" + start + "—" + end + "智慧出行服务报告》";
        String systemPrompt = """
                你是智慧出行服务报告助手。
                只用简体中文，面向旅客和答辩展示场景。
                报告要体现主动提醒、精准规划、服务融合和结果可保存。
                不要出现模型、接口、数据库、比赛闭环、用户画像等开发者表达。
                """;
        String userPrompt = """
                报告标题：%s
                出发地：%s
                目的地：%s
                出行时间：%s
                偏好：%s
                特殊需求：%s
                AI 方案：%s
                路线摘要：%s
                天气摘要：%s
                风险摘要：%s
                行李信息：%s
                售后需求：%s
                支付偏好：%s

                请生成完整报告，结构如下：
                一、行程概览
                二、路线与天气
                三、风险预警
                四、行李与特殊照顾
                五、售后与支付建议
                六、执行清单
                """.formatted(
                title,
                start,
                end,
                value(dto.getTravelDate()),
                value(dto.getPreference()),
                value(dto.getSpecialNeed()),
                value(dto.getAiPlan()),
                value(dto.getRouteSummary()),
                value(dto.getWeatherSummary()),
                value(dto.getRiskSummary()),
                value(dto.getLuggageInfo()),
                value(dto.getAfterSalesNeed()),
                value(dto.getPaymentPreference())
        );

        String response = zhipuAiClient.chat(systemPrompt, userPrompt);
        SmartServiceRecord record = new SmartServiceRecord();
        record.setUserId(value(dto.getUserId()));
        record.setServiceType("TRAVEL_REPORT");
        record.setTitle(title);
        record.setStartPlace(dto.getStartPlace());
        record.setEndPlace(dto.getEndPlace());
        record.setRequestPayload(toJson(dto));
        record.setResponsePayload(response);
        record.setStatus("SAVED");
        recordMapper.insert(record);
        return record;
    }

    @Override
    public List<SmartServiceRecord> listByUserId(String userId) {
        LambdaQueryWrapper<SmartServiceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmartServiceRecord::getUserId, userId).orderByDesc(SmartServiceRecord::getCreateTime);
        return recordMapper.selectList(wrapper);
    }

    @Override
    public List<SmartServiceRecord> listAll() {
        LambdaQueryWrapper<SmartServiceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SmartServiceRecord::getCreateTime);
        return recordMapper.selectList(wrapper);
    }

    @Override
    public void deleteById(Long id) {
        recordMapper.deleteById(id);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String value(String value) {
        return value == null || value.trim().isEmpty() ? "not provided" : value.trim();
    }

    private String serviceName(String type) {
        if ("REALTIME_ALERT".equals(type)) return "实时提醒";
        if ("INTERMODAL_PLAN".equals(type)) return "联程出行";
        if ("SPECIAL_SCENE".equals(type)) return "特殊照顾";
        if ("AFTER_SALES".equals(type)) return "售后协助";
        if ("PASSENGER_LUGGAGE".equals(type)) return "行李服务";
        if ("PAYMENT_CREDIT".equals(type)) return "支付权益";
        if ("PROACTIVE_LOOP".equals(type)) return "行程管家";
        if ("TRAVEL_REPORT".equals(type)) return "出行服务报告";
        return "出行服务";
    }
}
