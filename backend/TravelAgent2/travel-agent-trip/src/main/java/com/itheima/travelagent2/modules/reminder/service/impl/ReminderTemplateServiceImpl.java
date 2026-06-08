package com.itheima.travelagent2.modules.reminder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.travelagent2.modules.reminder.entity.ReminderTemplate;
import com.itheima.travelagent2.modules.reminder.mapper.ReminderTemplateMapper;
import com.itheima.travelagent2.modules.reminder.service.ReminderTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReminderTemplateServiceImpl extends ServiceImpl<ReminderTemplateMapper, ReminderTemplate> implements ReminderTemplateService {
    @Override
    public List<ReminderTemplate> listBySceneType(String sceneType) {
        LambdaQueryWrapper<ReminderTemplate> wrapper = new LambdaQueryWrapper<>();
        if (sceneType != null && !sceneType.isBlank()) {
            wrapper.eq(ReminderTemplate::getSceneType, sceneType.trim());
        }
        wrapper.orderByDesc(ReminderTemplate::getDefaultEnabled).orderByAsc(ReminderTemplate::getId);
        return list(wrapper);
    }
}
