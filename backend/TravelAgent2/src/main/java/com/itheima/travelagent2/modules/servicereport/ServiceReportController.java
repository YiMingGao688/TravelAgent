package com.itheima.travelagent2.modules.servicereport;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.servicereport.dto.TravelServiceReportSaveDTO;
import com.itheima.travelagent2.modules.servicereport.entity.TravelServiceReport;
import com.itheima.travelagent2.modules.servicereport.service.TravelServiceReportService;
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
@RequestMapping("/api/service-report")
public class ServiceReportController {

    private final TravelServiceReportService reportService;

    public ServiceReportController(TravelServiceReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/save")
    public Result<TravelServiceReport> save(@RequestBody TravelServiceReportSaveDTO dto) {
        if (dto.getUserId() == null || dto.getUserId().isBlank()) {
            return Result.error(400, "userId 不能为空");
        }
        if (dto.getReportContent() == null || dto.getReportContent().isBlank()) {
            return Result.error(400, "报告内容不能为空");
        }
        return Result.success("报告已保存", reportService.save(dto));
    }

    @PostMapping
    public Result<TravelServiceReport> create(@RequestBody TravelServiceReportSaveDTO dto) {
        return save(dto);
    }

    @GetMapping("/list")
    public Result<List<TravelServiceReport>> list(@RequestParam String userId,
                                                  @RequestParam(required = false) String keyword) {
        if (userId == null || userId.isBlank()) {
            return Result.error(400, "userId 不能为空");
        }
        return Result.success(reportService.listByUserId(userId, keyword));
    }

    @GetMapping("/{id}")
    public Result<TravelServiceReport> detail(@PathVariable Long id) {
        TravelServiceReport report = reportService.getById(id);
        if (report == null) {
            return Result.error(404, "报告不存在");
        }
        return Result.success(report);
    }

    @PutMapping("/{id}")
    public Result<TravelServiceReport> update(@PathVariable Long id, @RequestBody TravelServiceReportSaveDTO dto) {
        TravelServiceReport report = reportService.update(id, dto);
        if (report == null) {
            return Result.error(404, "报告不存在");
        }
        return Result.success("报告已更新", report);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        System.out.println("后端收到删除 id = " + id);
        boolean removed = reportService.deleteById(id);
        if (!removed) {
            return Result.error("删除失败，数据库中不存在 id = " + id + " 的出行服务报告");
        }
        return Result.success("报告已删除");
    }
}
