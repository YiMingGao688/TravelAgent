package com.itheima.travelagent2.modules.chain;

/**
 * 出行规划状态机阶段。
 * 大模型只负责结构化解析和自然语言总结，路线/交通/酒店必须来自高德API或数据库。
 */
public enum PlanningState {
    /** 初始状态：等待用户输入 */
    IDLE,

    /** AI 需求解析：大模型提取结构化需求，规则引擎兜底 */
    INTENT_PARSE,

    /** 缺失信息追问：startPlace/endPlace/departureDate 等缺失时暂停流程 */
    CLARIFY,

    /** 地点确认：目的地模糊时提供高德候选项让用户选择 */
    LOCATION_RESOLVE,

    /** 交通枢纽选择：跨城场景下匹配南京南站、杭州东站等 */
    HUB_SELECT,

    /** 路线构建：本地接驳 + 高铁 + 到达接驳分段 */
    ROUTE_BUILD,

    /** 交通产品查询：高铁班次、航班等 DB 产品匹配 */
    TRAFFIC_QUERY,

    /** 酒店匹配：目的地附近酒店 POI 搜索 */
    HOTEL_MATCH,

    /** 风险评估：天气、耗时、特殊人群、行李等规则检查 */
    RISK_ASSESS,

    /** 等待用户确认方案（确认后才生成模拟订单和提醒） */
    AWAIT_CONFIRM,

    /** 模拟订单生成 */
    MOCK_ORDER,

    /** 实时提醒生成 */
    REMINDERS,

    /** 规划完成 */
    COMPLETED
}
