package com.itheima.travelagent2.modules.reminder;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.reminder.entity.ReminderTemplate;
import com.itheima.travelagent2.modules.reminder.service.ReminderTemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
public class ReminderTemplateController {
    private final ReminderTemplateService reminderTemplateService;

    public ReminderTemplateController(ReminderTemplateService reminderTemplateService) {
        this.reminderTemplateService = reminderTemplateService;
    }

    @GetMapping("/templates")
    public Result<List<ReminderTemplate>> templates(@RequestParam(required = false) String sceneType) {
        return Result.success(reminderTemplateService.listBySceneType(sceneType));
    }
}
