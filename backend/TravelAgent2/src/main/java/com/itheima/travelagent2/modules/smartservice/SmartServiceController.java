package com.itheima.travelagent2.modules.smartservice;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.smartservice.dto.SmartServiceRequestDTO;
import com.itheima.travelagent2.modules.smartservice.entity.SmartServiceRecord;
import com.itheima.travelagent2.modules.smartservice.service.SmartService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/list")
    public Result<List<SmartServiceRecord>> list(@RequestParam String userId) {
        return Result.success(smartService.listByUserId(userId));
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        smartService.deleteById(id);
        return Result.success("Deleted");
    }
}
