package com.itheima.travelagent2.modules.smartservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.travelagent2.modules.ai.ZhipuAiClient;
import com.itheima.travelagent2.modules.smartservice.dto.SmartServiceRequestDTO;
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
                You are a customer-facing one-stop travel service assistant.
                Always answer in Simplified Chinese.
                Generate a practical service plan, not a technical explanation.
                Cover before-departure reminders, in-trip support, arrival support, special care,
                luggage support, payment/benefit suggestions and backup actions.
                Do not mention model, API, database, competition, closed-loop or user profile jargon.
                Do not fabricate real-time ticketing, weather, crowding or payment results.
                """;

        String userPrompt = """
                Service type: %s
                User: %s
                Start place: %s
                End place: %s
                Travel date: %s
                Preference: %s
                Special need: %s
                Budget: %s
                Passengers: %s
                Luggage: %s
                Payment preference: %s
                Scenario: %s
                Extra requirement: %s

                Please generate a complete customer-facing travel service plan in Simplified Chinese.
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
        record.setTitle(serviceName + " plan");
        record.setStartPlace(dto.getStartPlace());
        record.setEndPlace(dto.getEndPlace());
        record.setRequestPayload(toJson(dto));
        record.setResponsePayload(response);
        record.setStatus("GENERATED");
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
        if ("REALTIME_ALERT".equals(type)) return "real-time reminder";
        if ("INTERMODAL_PLAN".equals(type)) return "intermodal travel";
        if ("SPECIAL_SCENE".equals(type)) return "special care";
        if ("AFTER_SALES".equals(type)) return "after-sales support";
        if ("PASSENGER_LUGGAGE".equals(type)) return "luggage service";
        if ("PAYMENT_CREDIT".equals(type)) return "payment benefits";
        if ("PROACTIVE_LOOP".equals(type)) return "travel concierge";
        return "travel service";
    }
}
