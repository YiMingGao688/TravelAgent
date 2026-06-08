package com.itheima.travelagent2.modules.risk.dto;

import lombok.Data;

@Data
public class RiskWarningDTO {
    private String level;
    private String type;
    private String title;
    private String content;
    private String suggestion;
}
