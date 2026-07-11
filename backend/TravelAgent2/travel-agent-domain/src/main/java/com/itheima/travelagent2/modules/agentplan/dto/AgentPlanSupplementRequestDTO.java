package com.itheima.travelagent2.modules.agentplan.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AgentPlanSupplementRequestDTO {
    private LocalDateTime departTime;
    private LocalDateTime returnTime;
    private BigDecimal budget;
    private String preference;
    private String remark;
}
