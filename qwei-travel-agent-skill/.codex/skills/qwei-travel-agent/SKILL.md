---
name: qwei-travel-agent
description: Use this skill when modifying the 七维智行 / TravelAgent smart travel project. Applies to homepage one-click planning, AMap location resolution, trip-chain planning, simulated resources/orders, map visualization, real-time reminders, admin data management, after-sale service, and service reports.
---

# 七维智行项目 Skill

## 项目定位

七维智行不是 OTA 订票平台，也不是普通地图导航工具，而是“面向公众出行全过程服务的智能体平台”。

系统目标：用户输入出发地和自然语言需求后，智能体站在用户立场帮助分析、比较、规划和提醒，但不替用户做最终决定。涉及关键选择时，必须让用户确认。

核心链路：

用户输入需求 → AI 结构化解析 → 地点解析与候选确认 → 高德路线/天气/POI → 构建完整出行链 → 查询数据库模拟资源 → 生成行程规划表 → 用户确认/微调 → 模拟购票/预约 → 导入地图可视化 → 开启实时提醒 → 风险预警与动态重规划 → 售后工单 → 服务报告。

## 禁止事项

1. 不允许把项目改成普通票务/酒店商城。
2. 不允许把路线、酒店、车次、订单主流程写死在前端。
3. 不允许没有经纬度就调用路线规划。
4. 不允许把模糊地点直接默认成某个地点，例如“阿里巴巴”“西湖”“医院”“机场”。
5. 不允许出差两天时直接显示“不需要酒店”。应该追问是否需要酒店。
6. 不允许前端只用 setTimeout 假装 AI 思考。必须由后端阶段结果、SSE 或 planningTrace 驱动展示。
7. 不允许把跨城出行简化为“南京 → 杭州”单段路线。
8. 不允许爬 12306、携程、去哪儿、飞猪、美团等页面。
9. 不允许真实支付、真实出票、真实下单。
10. 不允许为了 UI 效果新建重复首页。
11. 不允许破坏已有大模型 API、高德 API、登录逻辑、左侧导航和右下角助手。

## 用户确认原则

智能体只能推荐和解释，不替用户最终决策。以下动作必须用户确认：

- 选择具体地点
- 选择出行方案
- 是否需要酒店
- 提醒内容
- 是否生成模拟订单
- 是否模拟支付
- 是否导入地图
- 是否开启实时提醒
- 是否切换替代方案

## 首页一键规划流程

必须按真实智能体工作流处理：

1. INTENT_PARSE：AI 解析自然语言，输出结构化 JSON。
2. GEO_RESOLVE：解析出发地、目的地、途经点，获取高德候选地点。
3. USER_CLARIFY：如果地点模糊或缺少关键字段，暂停流程并追问。
4. TRIP_CHAIN_BUILD：构建完整出行链。
5. TOOL_CALL：调用高德路线、天气、POI；查询数据库模拟资源。
6. PLAN_GENERATE：生成候选方案。
7. RESOURCE_MATCH：匹配交通、酒店、接驳等资源。
8. RISK_ANALYZE：分析天气、拥堵、迟到、步行、换乘、行李、酒店距离等风险。
9. TIMELINE_GENERATE：生成全过程时间轴。
10. USER_CONFIRM：用户确认方案、提醒、酒店、订单。
11. ORDER_PREVIEW：用户确认后生成模拟预约/订单。
12. MAP_IMPORT：把行程规划表导入地图模块可视化。
13. MONITOR_ENABLE：开启实时提醒任务。

如果某一步需要用户确认，流程必须暂停，不能继续硬生成。

## 结构化解析规则

大模型不要直接生成最终行程正文，必须先输出结构化 JSON。

示例：用户输入“6月1日去杭州阿里巴巴总部出差两天，给我规划一下”。

应识别为：

- sceneType = BUSINESS_TRIP
- startPlace = 用户定位或输入的出发地
- endPlace = 杭州阿里巴巴总部
- days = 2
- peopleCount 默认 1，除非用户另说
- needHotel = null，不能直接 false
- needReminder = true
- businessItems = 身份证、电脑、充电器、会议文件、发票抬头
- ambiguousPlaces 包含“杭州阿里巴巴总部”
- missingFields 至少包含 departureTime、needHotel
- clarifyingQuestion 需要追问具体阿里巴巴园区、出发时间、是否需要酒店

## 场景规则

### BUSINESS_TRIP 商务出差

关键词：出差、开会、客户、公司、总部、园区、商务、会议、拜访。

默认关注：准时优先、少换乘、少步行、靠近目的地、文件证件提醒、发票/报销、路线风险监控。

默认提醒：身份证、电脑、充电器、会议文件、发票抬头、工牌或门禁卡。

如果 days >= 2 且用户没有明确说“不需要酒店”，必须设置 needHotel = null，并追问是否需要匹配目的地附近酒店。

### SHORT_TRIP 旅游/短途游

关键词：旅游、玩、一日游、两日游、周末游、景点。

默认关注：景点顺序、住宿匹配、舒适优先、天气提醒、少折返。

### CATCH_TRAIN_FLIGHT 赶车/赶飞机

关键词：赶车、赶飞机、几点出门、来得及吗、高铁、航班、机场、火车站。

默认关注：倒推出发时间、实时路况、迟到风险、证件提醒。

### MULTI_STOP_TASK 多目的地

关键词：先去、再去、最后、顺路、跑几个地方。

默认关注：多点排序、少折返、时间窗口、地图可视化。

### LUGGAGE / ELDERLY / CHILD / MIN_WALK

关键词：行李箱、大件行李、老人、孩子、父母、少走路、少步行、少换乘。

必须降低步行和换乘权重，优先生成少步行、少换乘、打车接驳方案。

## 地点解析与确认

所有地点必须经过：文本地点 → /api/geo/resolve → 高德 POI 候选 → 用户确认 → 保存经纬度 → 路线规划。

地点对象必须包含：name、address、longitude、latitude、adcode、poiId、source。

以下地点必须候选确认：阿里巴巴、杭州阿里巴巴、西湖、医院、客户公司、大学、学校、机场、火车站、总部、园区。

出发地支持三种来源：浏览器定位、手动输入、省市区限定 + 关键词。浏览器定位后必须调用高德逆地理编码。

## 出行链 TripSegment

跨城出行必须拆段，不允许单段展示。

典型链路：

- LOCAL_ACCESS：出发地 → 出发交通枢纽
- INTERCITY：出发交通枢纽 → 到达交通枢纽
- LOCAL_TRANSFER：到达交通枢纽 → 最终目的地
- HOTEL_TRANSFER：如需住宿，加入酒店相关段
- RETURN_SEGMENT：返程段

示例：南京大学 → 南京南站 → 杭州东站 → 阿里巴巴西溪园区 → 酒店。

每个 TripSegment 应包含 segmentType、start、end、transportMode、distance、duration、cost、routePolyline、riskWarnings、source。

## 高德 API 使用原则

高德负责真实地点、经纬度、POI、路线、天气、地图可视化数据。

必须打印日志：

- [AMAP] geocode startPlace=xxx result=...
- [AMAP] geocode endPlace=xxx result=...
- [AMAP] route planning distance=xxx duration=xxx
- [AMAP] weather result=xxx
- [AMAP] poi search keyword=xxx result=...

高德失败时必须明确返回错误或 fallback，不允许静默假数据。

## 模拟数据原则

真实数据：高德地点、经纬度、路线、天气、POI。

模拟数据：高铁车次、航班数据、酒店价格库存、接驳服务、订单、支付、售后、服务报告、提醒模板。

模拟数据必须存数据库，通过后端接口读取；不要写死在前端。

## 模拟资源库与订单分离

模拟资源库表示系统可匹配和预约的基础资源：transport_hub、train_product、flight_product、hotel_product、transfer_product、scenic_poi。

交易联调表示用户选择资源后生成的交易记录：mock_order、mock_order_item、mock_payment_record。

关系：资源表 → mock_order_item → mock_order → mock_payment_record。

## 南京—杭州演示数据集

不要爬虫。应准备 seed_demo_nanjing_hangzhou.sql，包含：南京南站、杭州东站、南京禄口机场、杭州萧山机场、南京南到杭州东高铁车次、阿里巴巴园区附近酒店、杭州东到阿里巴巴接驳服务、风险规则、提醒模板。

## 行程规划表

最终必须生成可确认、可微调、可导入地图的行程规划表。字段包括：每段起点、终点、交通方式、预计出发时间、预计到达时间、耗时、价格、路线说明、天气、风险、用户确认状态、是否可预约。

## 地图导入

用户确认行程规划表后，可以导入地图模块。地图模块展示每段 marker、polyline、预计时间、预计价格、出发地天气、目的地天气、计划出发时天气、风险点、酒店位置、交通枢纽位置。

可以临时使用 localStorage.currentMapPlan，但长期应后端保存。

## 实时提醒与动态重规划

实时提醒是核心亮点。流程：用户确认方案 → 后端保存 confirmed_trip → 创建 travel_monitor_task → 定时调用高德天气/路线 → 规则判断风险 → 生成 notification → 前端/小程序提醒 → 用户确认是否切换方案 → 更新行程规划表和地图。

典型提醒：天气、证件、电脑/文件、出发时间、路线拥堵、高铁/航班时间、到站接驳、酒店入住、迟到风险、行程结束报告。

检测到风险时，不能直接替用户切换，必须询问。

## 前端展示

规划结果不能一次性硬显示全部卡片。优先使用后端 SSE 阶段事件；如果不用 SSE，后端必须返回 planningTrace，前端根据 trace 展示。不能只有前端 setTimeout 假装思考。

枚举必须转中文：car→自驾，train→高铁，bus→公交，subway→地铁，flight→飞机，min_walk→少步行，BUSINESS_TRIP→商务出差，SHORT_TRIP→短途旅游。缺失字段显示“待补充”。不要显示 null、undefined、-、car、min_walk。

## 管理端结构

侧边栏使用分组折叠菜单：

总览：运营总览。

AI 咨询模块：咨询记录管理、历史方案管理、路线记录管理。

模拟资源库：交通枢纽管理、高铁车次管理、航班数据管理、酒店资源管理、接驳服务管理、景点 POI 管理。

交易联调：模拟预约管理、模拟订单管理、模拟支付记录。

闭环服务：风险预警管理、实时提醒任务、全过程时间轴、售后工单管理、服务报告管理。

系统管理：用户管理、角色与权限、操作日志、系统设置。

## UI 风格

蓝白商务风，卡片圆角，轻阴影，信息层级清楚。不要像原始后台表格，不要过度 AI 渐变，不要玩具感。首页搜索面板参考 docs/ui-reference/home-search-panel-final.png；规划结果参考 docs/ui-reference/home-planning-result.png。

## 验收用例

用例 1：出发地为南京大学或浏览器定位南京，需求为“6月1日去杭州阿里巴巴总部出差两天，给我规划一下”。预期：识别商务出差，追问阿里巴巴具体园区、出发时间、是否需要酒店；生成南京大学→南京南站→杭州东站→阿里巴巴园区的完整出行链；每段有方式、耗时、价格；可生成行程规划表、模拟购票、导入地图、开启实时提醒。

用例 2：出发地杭州西湖景区，需求为“从杭州西湖景区出发去灵隐寺，下午再去杭州东站，两个人，少步行”。预期：startPlace=杭州西湖景区，waypoints=[灵隐寺]，endPlace=杭州东站；三个地点均解析经纬度；生成市内多段少步行路线；无酒店需求则不显示酒店模块。

## 每次修改前

必须读取本 Skill 和 AGENTS.md，扫描当前项目结构，找到真实文件路径，输出准备修改的文件清单。不要盲目新建文件。

## 每次修改后

必须输出：修改文件清单、新增接口、新增 DTO/VO、新增数据库表或 seed 数据、一键规划执行链路、高德 API 调用位置、地点确认流程、出行链分段逻辑、模拟资源与订单关系、测试步骤。
