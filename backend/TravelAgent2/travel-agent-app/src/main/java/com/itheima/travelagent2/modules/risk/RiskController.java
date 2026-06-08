package com.itheima.travelagent2.modules.risk;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.intent.dto.StructuredTravelDemand;
import com.itheima.travelagent2.modules.risk.dto.RiskWarningDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/risk")
public class RiskController {
    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @PostMapping("/evaluate")
    public Result<List<RiskWarningDTO>> evaluate(@RequestBody StructuredTravelDemand demand) {
        return Result.success(riskService.generateWarnings(demand, null));
    }
}
