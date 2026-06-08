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
/**
 * 出行服务报告接口。
 * 负责将一站式服务生成的报告真实落库，并支持历史报告列表、详情、更新和删除。
 */
public class ServiceReportController {

    private final TravelServiceReportService reportService;

    public ServiceReportController(TravelServiceReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/save")
    /**
     * 保存服务报告。
     * 保存成功后，客户端“历史出行服务报告”和管理端报告管理都会查询到同一条数据库记录。
     */
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
    /**
     * 按用户和关键词查询报告列表。
     * keyword 用于标题、起终点或摘要搜索，空值时返回该用户全部报告。
     */
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
    /**
     * 删除服务报告。
     * 这里直接调用 Service 删除，避免出现“页面删了但数据库还在”的假删除。
     */
    public Result<String> delete(@PathVariable Long id) {
        System.out.println("后端收到删除 id = " + id);
        boolean removed = reportService.deleteById(id);
        if (!removed) {
            return Result.error("删除失败，数据库中不存在 id = " + id + " 的出行服务报告");
        }
        return Result.success("报告已删除");
    }
}
