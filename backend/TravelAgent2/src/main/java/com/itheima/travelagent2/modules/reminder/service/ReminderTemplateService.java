package com.itheima.travelagent2.modules.reminder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.travelagent2.modules.reminder.entity.ReminderTemplate;

import java.util.List;

public interface ReminderTemplateService extends IService<ReminderTemplate> {
    List<ReminderTemplate> listBySceneType(String sceneType);
}
