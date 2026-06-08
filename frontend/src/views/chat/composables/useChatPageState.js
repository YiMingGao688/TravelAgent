export function useChatPageState({ messages, profile, currentTripContext, sessionId }) {
    function buildPlanPayload(msg) {
        const text = msg.content || ''
        const structured = msg.structuredPlan || contextToStructuredPlan(msg.tripContext || currentTripContext.value, msg.rawQuestion) || extractStructuredPlan(msg)
        return {
            userId: sessionStorage.getItem('loginUser') || 'guest',
            sessionId: msg.sessionId || sessionId.value,
            title: structured.startPlace && structured.endPlace ? `${structured.startPlace} 到 ${structured.endPlace} 出行方案` : 'AI 智能出行方案',
            startPlace: structured.startPlace,
            endPlace: structured.endPlace,
            content: text,
            rawQuestion: structured.rawQuestion,
            sourceType: 'AI_CHAT',
            travelPreference: structured.preference || '',
            budget: structured.budget || '',
            days: Number(structured.days) || null
        }
    }

    function contextToStructuredPlan(context, rawQuestion = '') {
        if (!context) return null
        return {
            startPlace: context.startPlace || '',
            endPlace: context.endPlace || '',
            scenario: context.sceneType || '',
            days: context.days || null,
            preference: context.preference || '',
            budget: context.budget || '',
            companions: context.companionType || '',
            transportMode: context.transportMode || '',
            pacePreference: context.pacePreference || '',
            walkingPreference: context.walkingPreference || '',
            hotelPreference: context.hotelPreference || '',
            foodPreference: context.foodPreference || '',
            specialNeeds: context.specialNeeds || '',
            rawQuestion,
            hasUsefulFields: Boolean(context.startPlace || context.endPlace || context.days || context.budget || context.preference || context.companionType || context.sceneType)
        }
    }

    function normalizePlan(source) {
        if (source?.content && source?.role === 'ai') {
            return buildPlanPayload(source)
        }
        return source || {}
    }

    function legacyExtractStructuredPlan(msg) {
        const aiText = String(msg?.content || '')
        const rawQuestion = String(msg?.rawQuestion || [...messages.value].reverse().find(item => item.role === 'user')?.content || '')
        const text = `${rawQuestion}\n${aiText}`
        const places = extractPlaces(rawQuestion, aiText)
        const days = extractDays(text)
        const budget = extractBudget(text)
        const preference = extractPreference(text)
        const companions = extractCompanions(text)
        const scenario = extractScenario(text)
        return {
            startPlace: places.startPlace,
            endPlace: places.endPlace,
            scenario,
            days,
            preference,
            budget,
            companions,
            rawQuestion,
            hasUsefulFields: Boolean(places.startPlace || places.endPlace || days || budget || preference || companions || scenario)
        }
    }

    function legacyExtractPlaces(question, answer) {
        const source = `${question}\n${answer}`
        const explicitPatterns = [
            /(?:从|自)\s*([^，。；;,.!?、\n]{2,24})\s*(?:出发)?(?:到|去|前往|抵达)\s*([^，。；;,.!?、\n]{2,24})/,
            /(?:起点|出发地)[:：]\s*([^，。；;,.!?、\n]{2,24})[\s\S]{0,120}(?:终点|目的地)[:：]\s*([^，。；;,.!?、\n]{2,24})/
        ]
        for (const pattern of explicitPatterns) {
            const match = source.match(pattern)
            if (match) {
                return { startPlace: cleanPlace(match[1]), endPlace: cleanPlace(match[2]) }
            }
        }

        const destinationPatterns = [
            /(?:去|到|前往)\s*([^，。；;,.!?、\n]{2,20}?)(?:玩|旅游|出差|看展|游玩|旅行|两天|三天|[0-9一二三四五六七八九十]+天)/,
            /目的地[:：]\s*([^，。；;,.!?、\n]{2,20})/
        ]
        let endPlace = ''
        for (const pattern of destinationPatterns) {
            const match = source.match(pattern)
            if (match) {
                endPlace = cleanPlace(match[1])
                break
            }
        }

        return {
            startPlace: endPlace ? (profile.value.city || '') : '',
            endPlace
        }
    }

    function legacyCleanPlace(value) {
        return String(value || '')
            .replace(/(玩|游玩|旅游|旅行|出差|看展).*$/g, '')
            .replace(/[，。；;,.!?、]/g, '')
            .replace(/^(去|到|前往|从|自)/g, '')
            .replace(/的?$/g, '')
            .trim()
    }

    function legacyExtractDays(text = '') {
        const digit = text.match(/(\d+)\s*(?:天|日)/)
        if (digit) return Number(digit[1])
        const cn = text.match(/([一二两三四五六七八九十])\s*(?:天|日)/)
        if (!cn) return null
        return { 一: 1, 二: 2, 两: 2, 三: 3, 四: 4, 五: 5, 六: 6, 七: 7, 八: 8, 九: 9, 十: 10 }[cn[1]] || null
    }

    function legacyExtractBudget(text = '') {
        const match = text.match(/(?:预算|花费|费用)[:：]?\s*([0-9]+(?:\s*[-~到]\s*[0-9]+)?\s*(?:元|块)?|经济|中等|舒适|高端)/)
        return match ? match[1].trim() : ''
    }

    function legacyExtractPreference(text = '') {
        const options = ['舒适优先', '少换乘', '时间优先', '经济优先', '亲子友好', '老人友好', '人文', '美食', '自然风景']
        return options.find(item => text.includes(item)) || ''
    }

    function legacyExtractCompanions(text = '') {
        const options = ['老人', '孩子', '儿童', '亲子', '情侣', '朋友', '同事', '家庭']
        const hit = options.filter(item => text.includes(item))
        return hit.length ? [...new Set(hit)].join('、') : ''
    }

    function legacyExtractScenario(text = '') {
        if (/商务|出差|会议/.test(text)) return '商务出行'
        if (/亲子|孩子|儿童|家庭/.test(text)) return '亲子出行'
        if (/老人|无障碍|轮椅/.test(text)) return '特殊照顾'
        if (/周末|两天|三天|旅游|游玩|旅行/.test(text)) return '休闲旅行'
        return ''
    }

    function extractStructuredPlan(msg) {
        const aiText = stripUiText(String(msg?.content || ''))
        const rawQuestion = stripUiText(String(msg?.rawQuestion || [...messages.value].reverse().find(item => item.role === 'user')?.content || ''))
        const text = `${rawQuestion}\n${aiText}`
        const places = extractPlaces(rawQuestion, aiText)
        const days = extractDays(text)
        const budget = extractBudget(text)
        const preference = extractPreference(text)
        const companions = extractCompanions(text)
        const scenario = extractScenario(text)
        return {
            startPlace: places.startPlace,
            endPlace: places.endPlace,
            scenario,
            days,
            preference,
            budget,
            companions,
            rawQuestion,
            hasUsefulFields: Boolean(places.startPlace || places.endPlace || days || budget || preference || companions || scenario)
        }
    }

    function extractPlaces(question, answer) {
        const source = `${stripUiText(question)}\n${stripUiText(answer)}`
        const explicitPatterns = [
            /(?:从|自|起点|出发地)\s*[:：]?\s*([^\s，。；;,.!?、\n]{2,24})\s*(?:出发)?(?:到|去|至|前往|抵达)\s*([^\s，。；;,.!?、\n]{2,24})/,
            /(?:从|自)\s*([^\s，。；;,.!?、\n]{2,24})\s*(?:去|到|至|前往)\s*([^\s，。；;,.!?、\n]{2,24})/,
            /(?:起点|出发地)\s*[:：]\s*([^\s，。；;,.!?、\n]{2,24})[\s\S]{0,120}(?:终点|目的地)\s*[:：]\s*([^\s，。；;,.!?、\n]{2,24})/
        ]
        for (const pattern of explicitPatterns) {
            const match = source.match(pattern)
            if (match) {
                return { startPlace: cleanPlace(match[1]), endPlace: cleanPlace(match[2]) }
            }
        }

        const destinationPatterns = [
            /(?:去|到|前往)\s*([^\s，。；;,.!?、\n]{2,20}?)(?:玩|旅游|出差|看展|游玩|旅行|两天|三天|[0-9一二两三四五六七八九十]+天)/,
            /目的地\s*[:：]\s*([^\s，。；;,.!?、\n]{2,20})/
        ]
        let endPlace = ''
        for (const pattern of destinationPatterns) {
            const match = source.match(pattern)
            if (match) {
                endPlace = cleanPlace(match[1])
                break
            }
        }

        return {
            startPlace: endPlace ? cleanPlace(profile.value.city || '') : '',
            endPlace
        }
    }

    function cleanPlace(value) {
        const text = String(value || '')
            .replace(/<[^>]*>/g, '')
            .replace(/(已识别.*|未识别.*|用于保存.*|保存到历史方案.*)$/g, '')
            .replace(/(玩|游玩|旅游|旅行|出差|看展|两天|三天|[0-9一二两三四五六七八九十]+天).*$/g, '')
            .replace(/[，。；;,.!?、\n\r]/g, '')
            .replace(/^(从|自|去|到|至|前往|起点|终点|目的地|出发地)[:：]?/g, '')
            .trim()
        return normalizeCityName(text)
    }

    function extractDays(text = '') {
        const digit = text.match(/(\d+)\s*(?:天|日)/)
        if (digit) return Number(digit[1])
        const cn = text.match(/([一二两三四五六七八九十])\s*(?:天|日)/)
        if (!cn) return null
        return { 一: 1, 二: 2, 两: 2, 三: 3, 四: 4, 五: 5, 六: 6, 七: 7, 八: 8, 九: 9, 十: 10 }[cn[1]] || null
    }

    function extractBudget(text = '') {
        const match = text.match(/(?:预算|花费|费用)[:：]?\s*([0-9]+(?:\s*[-~到至]\s*[0-9]+)?\s*(?:元|块|人民币)?|经济|中等|舒适|高端)/)
        return match ? match[1].trim() : ''
    }

    function extractPreference(text = '') {
        const options = ['舒适优先', '少换乘', '少走路', '时间优先', '经济优先', '亲子友好', '老人友好', '人文', '美食', '自然风景']
        return options.find(item => text.includes(item)) || ''
    }

    function extractCompanions(text = '') {
        const options = ['老人', '孩子', '儿童', '亲子', '情侣', '朋友', '同事', '家庭']
        const hit = options.filter(item => text.includes(item))
        return hit.length ? [...new Set(hit)].join('、') : ''
    }

    function extractScenario(text = '') {
        if (/商务|出差|会议/.test(text)) return '商务出行'
        if (/亲子|孩子|儿童|家庭/.test(text)) return '亲子出行'
        if (/老人|无障碍|轮椅|少走路/.test(text)) return '特殊照顾'
        if (/周末|两天|三天|旅游|游玩|旅行/.test(text)) return '休闲旅行'
        return ''
    }

    function stripUiText(value = '') {
        return String(value)
            .replace(/已识别出行信息[\s\S]*/g, '')
            .replace(/用于保存与导入/g, '')
            .replace(/方案已保存到历史方案[\s\S]*/g, '')
    }

    function normalizeCityName(value = '') {
        const hit = String(value).match(/([\u4e00-\u9fa5]{2,12}(?:市|省|区|县|州|盟))/)
        if (hit) return hit[1]
        return String(value).replace(/[^\u4e00-\u9fa5A-Za-z0-9·\-]/g, '').slice(0, 16)
    }

    return {
        buildPlanPayload,
        contextToStructuredPlan,
        normalizePlan,
        extractStructuredPlan,
        extractDays
    }
}
