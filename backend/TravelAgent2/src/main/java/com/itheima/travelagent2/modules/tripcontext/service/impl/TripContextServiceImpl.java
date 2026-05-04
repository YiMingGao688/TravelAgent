package com.itheima.travelagent2.modules.tripcontext.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.travelagent2.modules.chat.dto.ChatRequestDTO;
import com.itheima.travelagent2.modules.tripcontext.dto.TripContextPatchDTO;
import com.itheima.travelagent2.modules.tripcontext.entity.TripContext;
import com.itheima.travelagent2.modules.tripcontext.mapper.TripContextMapper;
import com.itheima.travelagent2.modules.tripcontext.service.TripContextService;
import com.itheima.travelagent2.modules.user.entity.UserProfile;
import com.itheima.travelagent2.modules.user.mapper.UserProfileMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TripContextServiceImpl implements TripContextService {

    private static final Pattern DAYS_PATTERN = Pattern.compile("([一二两三四五六七八九十0-9]+)\\s*(?:天|日)");
    private static final Pattern SINGLE_CITY_TRIP_PATTERN = Pattern.compile(
            "([\\u4e00-\\u9fa5A-Za-z]{2,20})(?:的)?[一二两三四五六七八九十0-9]+(?:天[一二两三四五六七八九十0-9]*晚?|日游)(?:计划|行程|旅游|旅行|攻略|路线|方案|自由行)*"
    );
    private static final Pattern CITY_SUFFIX_PATTERN = Pattern.compile(
            "([\\u4e00-\\u9fa5A-Za-z]{2,20})(?:的)?(?:计划|行程|旅游|旅行|攻略|路线|方案|自由行)$"
    );
    private static final List<String> INVALID_PLACE_WORDS = List.of(
            "规划", "安排", "推荐", "生成", "制定", "查询", "做", "帮我",
            "出行", "行程", "旅游", "旅行", "攻略", "方案", "路线", "计划",
            "时间优先", "省钱优先", "舒适优先"
    );

    private final TripContextMapper tripContextMapper;
    private final UserProfileMapper userProfileMapper;

    public TripContextServiceImpl(TripContextMapper tripContextMapper, UserProfileMapper userProfileMapper) {
        this.tripContextMapper = tripContextMapper;
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    public TripContext getBySessionId(String sessionId) {
        if (isBlank(sessionId)) {
            return null;
        }
        LambdaQueryWrapper<TripContext> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TripContext::getSessionId, sessionId).last("LIMIT 1");
        TripContext context = tripContextMapper.selectOne(wrapper);
        if (context == null) {
            return null;
        }
        TripContext normalized = normalizeTripContextBeforeSaveAndReturn(context, "");
        tripContextMapper.updateById(normalized);
        return normalized;
    }

    @Override
    public TripContext mergeForMessage(ChatRequestDTO dto) {
        String userMessage = dto.getMessage();
        TripContext oldContext = getBySessionId(dto.getSessionId());
        TripContextPatchDTO patch = parsePatch(userMessage);
        System.out.println("原始用户输入 = " + userMessage);
        System.out.println("旧 TripContext = " + oldContext);
        System.out.println("AI抽取patch = " + patch);

        TripContext merged = oldContext == null ? createInitialContext(dto) : oldContext;
        applyPatch(merged, patch);
        System.out.println("merge后context = " + merged);

        TripContext normalized = normalizeTripContextBeforeSaveAndReturn(merged, userMessage);
        normalized.setUpdateTime(LocalDateTime.now());

        if (normalized.getId() == null) {
            normalized.setCreateTime(LocalDateTime.now());
            tripContextMapper.insert(normalized);
        } else {
            tripContextMapper.updateById(normalized);
        }

        System.out.println("最终清洗后context = " + normalized);
        return normalized;
    }

    @Override
    public TripContextPatchDTO parsePatch(String message) {
        String text = normalize(message);
        TripContextPatchDTO patch = new TripContextPatchDTO();
        if (isBlank(text)) {
            return patch;
        }

        parsePlaces(text, patch);
        parseDays(text, patch);
        parseBudget(text, patch);
        parseScene(text, patch);
        parseCompanion(text, patch);
        parseTransport(text, patch);
        parsePreference(text, patch);
        parsePaceAndWalking(text, patch);
        parseHotel(text, patch);
        parseFood(text, patch);
        parseSpecialNeeds(text, patch);
        patch.setStartPlace(cleanPlaceName(patch.getStartPlace()));
        patch.setEndPlace(cleanPlaceName(patch.getEndPlace()));
        return patch;
    }

    private TripContext normalizeTripContextBeforeSaveAndReturn(TripContext context, String userMessage) {
        if (context == null) {
            return null;
        }
        String source = normalize(userMessage) + " " + nullToEmpty(context.getStartPlace()) + " " + nullToEmpty(context.getEndPlace());

        if (context.getDays() == null) {
            Integer extractedDays = extractDays(source);
            if (extractedDays != null) {
                context.setDays(extractedDays);
            }
        }

        context.setStartPlace(cleanPlaceName(context.getStartPlace()));
        context.setEndPlace(cleanPlaceName(context.getEndPlace()));

        String singleCity = extractSingleCityDestination(userMessage);
        if (notBlank(singleCity) && doesNotContainExplicitStart(userMessage)) {
            context.setStartPlace(resolveDefaultStartPlace(context));
            context.setEndPlace(cleanPlaceName(singleCity));
        }

        if (isInvalidPlace(context.getStartPlace())) {
            context.setStartPlace(null);
        }
        if (isInvalidPlace(context.getEndPlace())) {
            context.setEndPlace(null);
        }
        if (isBlank(context.getStartPlace()) && notBlank(context.getEndPlace())) {
            context.setStartPlace(resolveDefaultStartPlace(context));
        }

        if (context.getDays() == null) {
            Integer extractedDays = extractDays(source);
            if (extractedDays != null) {
                context.setDays(extractedDays);
            }
        }

        if (isBlank(context.getSceneType()) && looksLikeTravelRequest(userMessage, context)) {
            context.setSceneType("休闲旅行");
        }

        return context;
    }

    private TripContext createInitialContext(ChatRequestDTO dto) {
        TripContext context = new TripContext();
        context.setSessionId(dto.getSessionId());
        context.setUserId(blankToDefault(dto.getUserId(), "guest"));

        UserProfile profile = findProfile(dto.getUserId());
        context.setStartPlace(firstNonBlank(dto.getCity(), profile == null ? null : profile.getCity()));
        context.setTransportMode(firstNonBlank(dto.getTransport(), profile == null ? null : profile.getTransport()));
        context.setPreference(firstNonBlank(dto.getStyle(), profile == null ? null : profile.getStyle()));
        context.setSpecialNeeds(profile == null ? null : profile.getNote());
        return context;
    }

    private UserProfile findProfile(String userId) {
        if (isBlank(userId)) {
            return null;
        }
        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProfile::getUserId, userId).last("LIMIT 1");
        return userProfileMapper.selectOne(wrapper);
    }

    private void applyPatch(TripContext context, TripContextPatchDTO patch) {
        if (notBlank(patch.getStartPlace())) context.setStartPlace(patch.getStartPlace());
        if (notBlank(patch.getEndPlace())) context.setEndPlace(patch.getEndPlace());
        if (notBlank(patch.getSceneType())) context.setSceneType(patch.getSceneType());
        if (patch.getDays() != null) context.setDays(patch.getDays());
        if (notBlank(patch.getPreference())) context.setPreference(patch.getPreference());
        if (notBlank(patch.getBudget())) context.setBudget(patch.getBudget());
        if (notBlank(patch.getCompanionType())) context.setCompanionType(patch.getCompanionType());
        if (notBlank(patch.getTransportMode())) context.setTransportMode(patch.getTransportMode());
        if (notBlank(patch.getPacePreference())) context.setPacePreference(patch.getPacePreference());
        if (notBlank(patch.getWalkingPreference())) context.setWalkingPreference(patch.getWalkingPreference());
        if (notBlank(patch.getHotelPreference())) context.setHotelPreference(patch.getHotelPreference());
        if (notBlank(patch.getFoodPreference())) context.setFoodPreference(patch.getFoodPreference());
        if (notBlank(patch.getSpecialNeeds())) context.setSpecialNeeds(patch.getSpecialNeeds());
    }

    private void parsePlaces(String text, TripContextPatchDTO patch) {
        Matcher route = Pattern.compile("(?:从)?([\\u4e00-\\u9fa5A-Za-z]{2,20})(?:出发)?(?:到|去|前往|抵达)([\\u4e00-\\u9fa5A-Za-z0-9的一二两三四五六七八九十天日游计划行程旅游旅行攻略路线方案自由行晚]{2,50})").matcher(text);
        if (route.find() && !looksLikeModifier(text)) {
            patch.setStartPlace(cleanPlaceName(route.group(1)));
            patch.setEndPlace(cleanPlaceName(route.group(2)));
        }

        Matcher explicitStart = Pattern.compile("(?:起点|出发地)(?:改成|换成|改为|是|为)?\\s*([\\u4e00-\\u9fa5A-Za-z]{2,20})").matcher(text);
        if (explicitStart.find()) {
            patch.setStartPlace(cleanPlaceName(explicitStart.group(1)));
        }

        Matcher explicitEnd = Pattern.compile("(?:终点|目的地)(?:改成|换成|改为|是|为)?\\s*([\\u4e00-\\u9fa5A-Za-z0-9的一二两三四五六七八九十天日游计划行程旅游旅行攻略路线方案自由行晚]{2,50})").matcher(text);
        if (explicitEnd.find()) {
            patch.setEndPlace(cleanPlaceName(explicitEnd.group(1)));
        }

        String singleCity = extractSingleCityDestination(text);
        if (notBlank(singleCity) && doesNotContainExplicitStart(text)) {
            patch.setStartPlace(null);
            patch.setEndPlace(cleanPlaceName(singleCity));
        }
    }

    private void parseDays(String text, TripContextPatchDTO patch) {
        Integer days = extractDays(text);
        if (days != null) {
            patch.setDays(days);
        }
    }

    private void parseBudget(String text, TripContextPatchDTO patch) {
        if (!(text.contains("预算") || text.contains("费用") || text.contains("花费") || text.contains("控制在"))) {
            return;
        }
        Matcher digitBudget = Pattern.compile("(?:预算|费用|花费|控制在)?(?:改成|改为|换成|调整到|是|为|控制在)?\\s*(\\d{2,7})\\s*(?:元|块|以内|左右)?").matcher(text);
        if (digitBudget.find()) {
            patch.setBudget(digitBudget.group(1));
            return;
        }
        Matcher unitBudget = Pattern.compile("(?:预算|费用|花费|控制在)?(?:改成|改为|换成|调整到|是|为|控制在)?\\s*([一二两三四五六七八九十百千万]+|\\d+(?:千|万))\\s*(?:元|块|以内|左右)?").matcher(text);
        if (unitBudget.find()) {
            Integer amount = parseMoneyAmount(unitBudget.group(1));
            if (amount != null) {
                patch.setBudget(String.valueOf(amount));
            }
        }
    }

    private void parseScene(String text, TripContextPatchDTO patch) {
        if (containsAny(text, "亲子游", "亲子出行")) patch.setSceneType("亲子旅行");
        else if (containsAny(text, "商务", "出差")) patch.setSceneType("商务出行");
        else if (containsAny(text, "老人", "长辈")) patch.setSceneType("长辈友好旅行");
        else if (containsAny(text, "学生")) patch.setSceneType("学生旅行");
        else if (looksLikeTravelRequest(text, null)) patch.setSceneType("休闲旅行");
    }

    private void parseCompanion(String text, TripContextPatchDTO patch) {
        if (containsAny(text, "同行人群改成情侣", "情侣")) patch.setCompanionType("情侣");
        else if (containsAny(text, "同行人群改成朋友", "朋友", "同学")) patch.setCompanionType("朋友");
        else if (containsAny(text, "同行人群改成亲子", "亲子", "孩子", "儿童")) patch.setCompanionType("亲子");
        else if (containsAny(text, "同行人群改成老人", "老人", "长辈")) patch.setCompanionType("老人");
        else if (containsAny(text, "一个人", "独自")) patch.setCompanionType("单人");
    }

    private void parseTransport(String text, TripContextPatchDTO patch) {
        if (containsAny(text, "高铁", "动车")) patch.setTransportMode("高铁");
        else if (containsAny(text, "飞机", "航班")) patch.setTransportMode("飞机");
        else if (containsAny(text, "自驾", "开车")) patch.setTransportMode("自驾");
        else if (containsAny(text, "地铁")) patch.setTransportMode("地铁优先");
    }

    private void parsePreference(String text, TripContextPatchDTO patch) {
        Map<String, String> preferences = new LinkedHashMap<>();
        preferences.put("时间优先", "时间优先");
        preferences.put("省钱优先", "省钱优先");
        preferences.put("经济优先", "省钱优先");
        preferences.put("舒适优先", "舒适优先");
        preferences.put("城市散步", "散步");
        preferences.put("散步", "散步");
        preferences.put("漫步", "散步");
        preferences.put("美食", "美食");
        preferences.put("景点", "景点打卡");
        preferences.put("打卡", "景点打卡");
        preferences.put("人文", "人文体验");
        preferences.put("博物馆", "人文体验");
        preferences.put("自然", "自然风景");

        for (Map.Entry<String, String> entry : preferences.entrySet()) {
            if (text.contains(entry.getKey())) {
                patch.setPreference(entry.getValue());
                return;
            }
        }
    }

    private void parsePaceAndWalking(String text, TripContextPatchDTO patch) {
        if (containsAny(text, "节奏轻松", "轻松一点", "慢一点", "不赶")) patch.setPacePreference("轻松");
        if (containsAny(text, "少走路", "少步行", "不想走太多", "步行少")) patch.setWalkingPreference("少走路");
        if (containsAny(text, "可以多走", "徒步")) patch.setWalkingPreference("可接受较多步行");
    }

    private void parseHotel(String text, TripContextPatchDTO patch) {
        Matcher hotel = Pattern.compile("(酒店|住宿|住处)(.*)").matcher(text);
        if (hotel.find()) {
            String value = hotel.group(2).trim();
            if (isBlank(value)) value = hotel.group(1);
            patch.setHotelPreference(value.length() > 80 ? value.substring(0, 80) : value);
        }
    }

    private void parseFood(String text, TripContextPatchDTO patch) {
        if (containsAny(text, "饮食清淡", "清淡一点", "少油少辣")) patch.setFoodPreference("清淡");
        else if (containsAny(text, "本地美食", "特色小吃")) patch.setFoodPreference("本地特色");
        else if (text.contains("不要美食")) patch.setFoodPreference("不以美食为主");
    }

    private void parseSpecialNeeds(String text, TripContextPatchDTO patch) {
        if (containsAny(text, "无障碍", "轮椅", "老人", "带老人", "带孩子", "婴儿车")) {
            patch.setSpecialNeeds(text.length() > 120 ? text.substring(0, 120) : text);
        }
    }

    private Integer extractDays(String text) {
        if (isBlank(text)) {
            return null;
        }
        Matcher matcher = DAYS_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return parseNumber(matcher.group(1));
    }

    private Integer parseNumber(String value) {
        if (value == null) {
            return null;
        }
        if (value.matches("\\d+")) {
            return Integer.parseInt(value);
        }
        return chineseNumber(value);
    }

    private Integer parseMoneyAmount(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String text = value.trim();
        Matcher digitWithUnit = Pattern.compile("(\\d+)(千|万)").matcher(text);
        if (digitWithUnit.matches()) {
            int base = Integer.parseInt(digitWithUnit.group(1));
            return base * ("万".equals(digitWithUnit.group(2)) ? 10000 : 1000);
        }
        int result = 0;
        int section = 0;
        int number = 0;
        for (int i = 0; i < text.length(); i++) {
            String current = String.valueOf(text.charAt(i));
            Integer digit = chineseNumber(current);
            if (digit != null) {
                number = digit;
                continue;
            }
            if ("十".equals(current)) {
                section += (number == 0 ? 1 : number) * 10;
                number = 0;
            } else if ("百".equals(current)) {
                section += (number == 0 ? 1 : number) * 100;
                number = 0;
            } else if ("千".equals(current)) {
                section += (number == 0 ? 1 : number) * 1000;
                number = 0;
            } else if ("万".equals(current)) {
                result += (section + number) * 10000;
                section = 0;
                number = 0;
            }
        }
        int total = result + section + number;
        return total > 0 ? total : null;
    }

    private String extractSingleCityDestination(String text) {
        String value = stripTaskPrefix(normalize(text));
        Matcher tripMatcher = SINGLE_CITY_TRIP_PATTERN.matcher(value);
        if (tripMatcher.find()) {
            return cleanPlaceName(tripMatcher.group(1));
        }
        Matcher suffixMatcher = CITY_SUFFIX_PATTERN.matcher(value);
        if (suffixMatcher.find()) {
            return cleanPlaceName(suffixMatcher.group(1));
        }
        return null;
    }

    private String cleanPlaceName(String place) {
        if (place == null) return null;

        String value = place.trim();
        if (value.isEmpty()) return null;

        value = stripTaskPrefix(value);
        value = value.replaceAll("^(去|到|前往|抵达)", "");
        value = value.replaceAll("^(一个|一份|的)+", "");
        value = value.replaceAll("[，。；、,.!?\\s]", "");

        if (isInvalidPlace(value)) return null;

        value = value.replaceAll("(预算|费用|花费).*$", "");
        value = value.replaceAll("的?[一二两三四五六七八九十0-9]+天[一二两三四五六七八九十0-9]*晚?(计划|行程|旅游|旅行|攻略|路线|方案|自由行)*$", "");
        value = value.replaceAll("的?[一二两三四五六七八九十0-9]+日游$", "");
        value = value.replaceAll("(的)?(计划|行程|旅游|旅行|攻略|路线|方案|自由行|出行)$", "");

        value = value.replaceAll("的$", "");
        value = value.trim();
        if (value.isEmpty() || isInvalidPlace(value)) return null;

        return value;
    }

    private String stripTaskPrefix(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("^(帮我|请帮我|给我|请|想要|我要|我想|规划|安排|推荐|生成|制定|做一个|做|查询)", "");
    }

    private boolean isInvalidPlace(String value) {
        if (value == null) return true;
        String v = value.trim();
        return v.isEmpty() || INVALID_PLACE_WORDS.contains(v);
    }

    private String resolveDefaultStartPlace(TripContext context) {
        String current = cleanPlaceName(context.getStartPlace());
        if (notBlank(current) && !isInvalidPlace(current)) {
            return current;
        }
        UserProfile profile = findProfile(context.getUserId());
        return profile == null ? null : cleanPlaceName(profile.getCity());
    }

    private boolean doesNotContainExplicitStart(String text) {
        String value = normalize(text);
        return !Pattern.compile("(?:从[\\u4e00-\\u9fa5A-Za-z]{2,20}(?:出发)?(?:到|去|前往|抵达)|[\\u4e00-\\u9fa5A-Za-z]{2,20}出发(?:到|去|前往|抵达)|起点|出发地)").matcher(value).find();
    }

    private boolean looksLikeTravelRequest(String text, TripContext context) {
        String value = normalize(text);
        return containsAny(value, "计划", "行程", "旅游", "旅行", "攻略", "路线", "方案", "自由行", "日游")
                || DAYS_PATTERN.matcher(value).find()
                || (context != null && context.getDays() != null);
    }

    private boolean looksLikeModifier(String text) {
        return containsAny(text, "改成", "改为", "换成", "不要", "调整", "改一下");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replace("兩", "两");
    }

    private Integer chineseNumber(String value) {
        return switch (value) {
            case "一" -> 1;
            case "二", "两" -> 2;
            case "三" -> 3;
            case "四" -> 4;
            case "五" -> 5;
            case "六" -> 6;
            case "七" -> 7;
            case "八" -> 8;
            case "九" -> 9;
            case "十" -> 10;
            default -> null;
        };
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (notBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String blankToDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean notBlank(String value) {
        return !isBlank(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
