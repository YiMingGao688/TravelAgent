package com.itheima.travelagent2.modules.chain;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.chain.dto.TravelChainRequestDTO;
import com.itheima.travelagent2.modules.chain.dto.TravelChainResponseDTO;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/travel-chain")
public class TravelChainController {
    private final TravelPlanningOrchestrator orchestrator;

    public TravelChainController(TravelPlanningOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * ★ 智能体编排入口：状态机驱动。
     * 响应字段: stage / status / toolCalls / nextQuestion / partialResult
     */
    @PostMapping("/plan")
    public Result<TravelChainResponseDTO> plan(@RequestBody TravelChainRequestDTO request) {
        return Result.success(orchestrator.plan(request));
    }

    /**
     * ★ 用户确认方案后调用：生成模拟订单 + 实时提醒 + 服务报告。
     * 需要先调用 /plan 到 AWAIT_CONFIRM 阶段。
     */
    @PostMapping("/confirm")
    public Result<TravelChainResponseDTO> confirm(@RequestBody Map<String, Object> body) {
        String sessionId = String.valueOf(body.getOrDefault("sessionId", ""));
        if (sessionId.isBlank()) {
            return Result.error(400, "缺少 sessionId，无法确认操作");
        }
        PlanningContext ctx = orchestrator.getContext(sessionId);
        if (ctx == null) {
            return Result.error(404, "未找到对应的规划会话，请重新规划");
        }
        if (ctx.getCurrentState() != PlanningState.AWAIT_CONFIRM) {
            return Result.error(400, "当前状态为 " + ctx.getCurrentState()
                    + "，不允许确认操作。请先完成规划到 AWAIT_CONFIRM 阶段。");
        }
        return Result.success(orchestrator.confirm(ctx));
    }
}
