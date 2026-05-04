package com.itheima.travelagent2.modules.smartservice;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.smartservice.dto.SmartServiceRequestDTO;
import com.itheima.travelagent2.modules.smartservice.dto.TravelReportRequestDTO;
import com.itheima.travelagent2.modules.smartservice.entity.SmartServiceRecord;
import com.itheima.travelagent2.modules.smartservice.service.SmartService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/smart-service")
public class SmartServiceController {

    private final SmartService smartService;

    public SmartServiceController(SmartService smartService) {
        this.smartService = smartService;
    }

    @PostMapping("/generate")
    public Result<SmartServiceRecord> generate(@RequestBody SmartServiceRequestDTO dto) {
        if (dto.getUserId() == null || dto.getUserId().isBlank()) {
            return Result.error(400, "User id is required");
        }
        if (dto.getServiceType() == null || dto.getServiceType().isBlank()) {
            return Result.error(400, "Service type is required");
        }
        try {
            return Result.success("OK", smartService.generate(dto));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "Service plan generation failed. Check cloud AI and database configuration.");
        }
    }

    @PostMapping("/report")
    public Result<SmartServiceRecord> generateReport(@RequestBody TravelReportRequestDTO dto) {
        if (dto.getUserId() == null || dto.getUserId().isBlank()) {
            return Result.error(400, "User id is required");
        }
        try {
            return Result.success("OK", smartService.generateReport(dto));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "出行服务报告生成失败，请检查云端 AI 或数据库配置。");
        }
    }

    @GetMapping("/list")
    public Result<List<SmartServiceRecord>> list(@RequestParam String userId) {
        return Result.success(smartService.listByUserId(userId));
    }

    @GetMapping("/{id}")
    public Result<SmartServiceRecord> detail(@PathVariable Long id) {
        SmartServiceRecord record = smartService.getById(id);
        if (record == null) {
            return Result.error(404, "Service record not found");
        }
        return Result.success(record);
    }

    @PostMapping
    public Result<SmartServiceRecord> create(@RequestBody SmartServiceRecord record) {
        return Result.success("Created", smartService.create(record));
    }

    @PutMapping("/{id}")
    public Result<SmartServiceRecord> update(@PathVariable Long id, @RequestBody SmartServiceRecord record) {
        SmartServiceRecord updated = smartService.update(id, record);
        if (updated == null) {
            return Result.error(404, "Service record not found");
        }
        return Result.success("Updated", updated);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        System.out.println("后端收到删除 id = " + id);
        boolean removed = smartService.deleteById(id);
        if (!removed) {
            return Result.error("删除失败，数据库中不存在 id = " + id + " 的服务记录");
        }
        return Result.success("Deleted");
    }
}
