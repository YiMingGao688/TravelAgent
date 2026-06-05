<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import AIChat from './AIChat.vue'
import TransitModule from './TransitModule.vue'
import CustomModule from './CustomModule.vue'
import HistoryModule from './HistoryModule.vue'
import SmartServiceModule from './SmartServiceModule.vue'
import ServiceReportHistoryModule from './ServiceReportHistoryModule.vue'
import AppSidebar from '../common/AppSidebar.vue'
import ProfileModal from '../common/ProfileModal.vue'
import { useChat, useProfile, useTheme } from '../../composables/index.js'
import { getCurrentTime } from '../../utils/time'
import { deleteTripPlan, listTripPlans, saveTripPlan } from '../../utils/api.js'

const props = defineProps({
    isLoggedIn: { type: Boolean, default: false },
    loginUser: { type: String, default: '' }
})

const emit = defineEmits(['logout', 'require-login', 'go-home'])

const activeMenu = ref('ai')
const showSettings = ref(false)
const userName = ref(props.loginUser || sessionStorage.getItem('loginUser') || '')
const showProfileModal = ref(false)
const sidebarCollapsed = ref(true)
const route = ref({ from: '', to: '', preference: '', tripPlanId: null, importSource: '' })
const customForm = ref({ startPlace: '', destination: '', days: 3, travelDate: '', crowdType: '普通出行', preference: '休闲度假', budget: '舒适预算', style: '深度人文' })
const smartContext = ref({})
const routePlan = ref(null)
const historyList = ref([])
const aiChatRef = ref(null)
const transitRef = ref(null)
const imageUploading = ref(false)

const { messages, aiThinking, aiTyping, sessionId, currentTripContext, syncTripContextFromProfile, sendMessage, interrupt, startNewChat, init } = useChat(aiChatRef)
const { profile, loadProfile, saveProfile } = useProfile()
const { initTheme, toggleTheme } = useTheme()

watch(() => props.loginUser, value => {
    userName.value = value || ''
    if (value) {
        loadProfile(value)
        loadTripPlans()
    }
})

const currentMenuName = computed(() => ({
    ai: '智能 AI 咨询',
    transit: '交通规划',
    custom: '行程定制',
    smart: '一站式出行服务',
    history: '历史方案',
    reports: '历史出行服务报告'
})[activeMenu.value] || '智能出行')

const displayMessages = computed(() => messages.value.map(msg => {
    if (msg.role !== 'ai' || msg.isWelcome || msg.skipStructure) return msg
    const structuredPlan = msg.structuredPlan || contextToStructuredPlan(msg.tripContext || currentTripContext.value, msg.rawQuestion) || extractStructuredPlan(msg)
    console.log('当前展示 identifiedTravelInfo = ', structuredPlan)
    return {
        ...msg,
        structuredPlan,
        isPlan: msg.isPlan || structuredPlan.hasUsefulFields
    }
}))

async function switchMenu(id) {
    // 主工作台模块切换。交通规划含地图实例，离开时先销毁，进入时再初始化，避免地图容器重复挂载。
    if (['history', 'reports'].includes(id) && !requireLogin('查看历史记录需要先登录。')) {
        return
    }
    if (activeMenu.value === 'transit' && transitRef.value) {
        transitRef.value.destroyMap()
    }
    activeMenu.value = id
    if (id === 'history') {
        await loadTripPlans()
    }
    if (id === 'transit' && transitRef.value) {
        await nextTick()
        transitRef.value.initMap()
    }
}

async function handleSendMessage(userMsg) {
    // AI 咨询入口：普通文本走流式聊天；图片消息走图片识别接口，保持两个链路互不干扰。
    if (!requireLogin('登录后可继续使用 AI 出行规划服务。')) {
        return
    }
    if (!userMsg.hasImage) {
        sendMessage(userMsg, profile.value)
        return
    }

    messages.value.push(userMsg)
    imageUploading.value = true
    try {
        const formData = new FormData()
        formData.append('file', userMsg.imageFile)
        formData.append('text', userMsg.content || '请分析这张图片，并结合出行场景给出建议。')
        formData.append('userId', sessionStorage.getItem('loginUser') || 'guest')
        formData.append('sessionId', sessionId.value || Date.now().toString())
        formData.append('model', 'zhipu-cloud')

        const res = await fetch('/api/image/chat', { method: 'POST', body: formData })
        if (!res.ok) throw new Error('上传失败')
        const data = await res.json()
        messages.value.push({ role: 'ai', content: data.data || '图片识别完成', time: getCurrentTime(), isPlan: false })
    } catch (error) {
        messages.value.push({ role: 'ai', content: `图片识别失败：${error.message}`, time: getCurrentTime(), isPlan: false })
    } finally {
        imageUploading.value = false
    }
}

function handleLogout() {
    sessionStorage.removeItem('loginUser')
    sessionStorage.removeItem('chatSessionId')
    localStorage.removeItem('loginUser')
    localStorage.removeItem('token')
    userName.value = ''
    showSettings.value = false
    showProfileModal.value = false
    historyList.value = []
    smartContext.value = {}
    routePlan.value = null
    emit('logout')
}

function openProfile() {
    if (!requireLogin('登录后可以保存和同步您的出行偏好。')) {
        return
    }
    showProfileModal.value = true
}

function requireLogin(message) {
    // 首页允许先进入系统，但涉及 userId 的操作必须在这里统一拦截并弹登录框。
    if (props.isLoggedIn || sessionStorage.getItem('loginUser')) {
        return true
    }
    emit('require-login', message)
    return false
}

async function handleProfileSave(savedProfile) {
    const previousProfile = { ...profile.value }
    profile.value = savedProfile
    await saveProfile()
    syncTripContextFromProfile(savedProfile, previousProfile)
    showProfileModal.value = false
    activeMenu.value = 'ai'
    messages.value.push({
        role: 'ai',
        content: `出行偏好已保存。我会优先结合「${profile.value.city || '常驻城市'}」「${profile.value.transport || '出行方式'}」「${profile.value.style || '旅行风格'}」来生成方案。`,
        time: getCurrentTime(),
        isPlan: false
    })
}

async function handleSavePlan(msg) {
    // 保存历史方案必须使用当前最新结构化 TripContext，保证“导入交通/我的行程”拿到一致数据。
    if (!requireLogin('登录后可保存出行方案，并在历史方案中继续查看。')) {
        return
    }
    const structured = buildPlanPayload(msg)
    try {
        const result = await saveTripPlan(structured)
        const saved = result.data
        messages.value.push({
            role: 'ai',
            content: `方案已保存到历史方案。识别到的起终点：${saved.startPlace || '未识别'} → ${saved.endPlace || '未识别'}。`,
            time: getCurrentTime(),
            isPlan: false,
            skipStructure: true
        })
        await loadTripPlans()
    } catch (error) {
        alert(`保存失败：${error.message}`)
    }
}

async function loadTripPlans() {
    // 历史方案列表以后端数据库为准；新增/删除后都重新调用该方法刷新页面。
    const userId = sessionStorage.getItem('loginUser')
    if (!userId) return
    historyList.value = await listTripPlans(userId)
}

async function handleDeleteHistory(index) {
    // 删除走后端真实 DELETE 接口。这里不做前端假删除，成功或失败后都重新拉取数据库列表。
    const deleteId = index === -1 ? 'ALL' : historyList.value[index]?.id
    console.log('前端删除 id = ', deleteId, typeof deleteId)
    try {
        if (index === -1) {
            if (!historyList.value.length || !confirm('确定清空全部历史方案吗？删除后数据库记录也会同步删除。')) return
            const ids = historyList.value.map(item => item.id).filter(Boolean)
            await Promise.allSettled(ids.map(id => deleteTripPlan(id)))
            await loadTripPlans()
            return
        }
        const item = historyList.value[index]
        if (!item || !confirm(`确定删除「${item.title || '这条方案'}」吗？删除后数据库记录也会同步删除。`)) return
        if (!item.id) {
            throw new Error('当前方案缺少数据库 ID，无法执行真实删除')
        }
        await deleteTripPlan(item.id)
        await loadTripPlans()
    } catch (error) {
        await loadTripPlans()
        alert(error.message?.includes('Plan not found') ? '该方案已不存在，已为你刷新历史列表。' : `删除失败：${error.message}`)
    }
}

function importToTransit(source) {
    // 将 AI 方案/历史方案的起终点和偏好传给交通规划模块，并切换到对应页面。
    const plan = normalizePlan(source)
    route.value = {
        from: plan.startPlace || route.value.from,
        to: plan.endPlace || route.value.to || plan.destination,
        preference: plan.travelPreference || plan.preference || profile.value.style || profile.value.transport || '舒适优先',
        tripPlanId: plan.id || null,
        importSource: 'AI 方案'
    }
    activeMenu.value = 'transit'
    nextTick(() => transitRef.value?.initMap())
}

function importToCustom(source) {
    const plan = normalizePlan(source)
    customForm.value = {
        startPlace: plan.startPlace || customForm.value.startPlace,
        destination: plan.endPlace || plan.destination || customForm.value.destination,
        days: plan.days || extractDays(plan.content) || customForm.value.days,
        travelDate: plan.travelDate || customForm.value.travelDate,
        crowdType: plan.companions || customForm.value.crowdType,
        preference: plan.travelPreference || plan.preference || customForm.value.preference,
        budget: plan.budget || customForm.value.budget,
        style: plan.travelPreference || plan.preference || profile.value.style || customForm.value.style
    }
    activeMenu.value = 'custom'
}

function importToSmart(source) {
    const plan = normalizePlan(source)
    smartContext.value = {
        imported: true,
        importSource: 'AI 方案',
        startPlace: plan.startPlace || route.value.from || profile.value.city || '',
        endPlace: plan.endPlace || plan.destination || route.value.to || '',
        travelDate: plan.travelDate || '',
        preference: plan.travelPreference || plan.preference || profile.value.style || profile.value.transport || '',
        specialNeed: plan.specialNeed || plan.companions || '',
        budget: plan.budget || '',
        passengerInfo: plan.companions || '',
        aiPlan: plan.content || ''
    }
    activeMenu.value = 'smart'
}

function handleRouteGenerated(plan) {
    routePlan.value = plan
}

function importRouteToSmart() {
    const activeRoute = routePlan.value?.activeRoute
    smartContext.value = {
        imported: true,
        importSource: '交通规划',
        startPlace: route.value.from,
        endPlace: route.value.to,
        preference: route.value.preference,
        specialNeed: routePlan.value?.risk ? `${routePlan.value.risk.label}：${routePlan.value.risk.reasons.join('；')}` : '',
        aiPlan: activeRoute ? `${activeRoute.routeName}，约 ${activeRoute.distance} 公里，${activeRoute.duration} 分钟。${activeRoute.recommendReason || ''}` : ''
    }
    activeMenu.value = 'smart'
}

function importRouteToCustom() {
    const activeRoute = routePlan.value?.activeRoute
    customForm.value = {
        ...customForm.value,
        startPlace: route.value.from || customForm.value.startPlace,
        destination: route.value.to || customForm.value.destination,
        preference: route.value.preference || customForm.value.preference,
        style: route.value.preference || customForm.value.style,
        budget: customForm.value.budget,
        routeSummary: activeRoute
            ? `${activeRoute.routeName || '推荐路线'}，约 ${activeRoute.distance || '-'} 公里，${activeRoute.duration || '-'} 分钟，预计 ${activeRoute.cost || '-'} 元。${activeRoute.recommendReason || ''}`
            : ''
    }
    activeMenu.value = 'custom'
}

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

function handleNewChat() {
    startNewChat()
    activeMenu.value = 'ai'
}

function syncActiveDatabaseList() {
    if (activeMenu.value === 'history') {
        loadTripPlans()
    }
}

onMounted(async () => {
    initTheme()
    const userId = sessionStorage.getItem('loginUser')
    if (userId) {
        await loadProfile(userId)
        await loadTripPlans()
    }
    init()
    window.addEventListener('focus', syncActiveDatabaseList)
    document.addEventListener('visibilitychange', syncActiveDatabaseList)
})

onBeforeUnmount(() => {
    window.removeEventListener('focus', syncActiveDatabaseList)
    document.removeEventListener('visibilitychange', syncActiveDatabaseList)
})
</script>

<template>
    <div class="app-layout">
        <div class="overlay" v-if="showSettings" @click="showSettings = false"></div>

        <AppSidebar
            :user-name="userName"
            :show-settings="showSettings"
            :active-menu="activeMenu"
            @select-menu="switchMenu"
            @toggle-settings="showSettings = !showSettings"
            @open-profile="openProfile"
            @toggle-theme="toggleTheme"
            @login-account="emit('require-login', '登录后可同步您的出行偏好、历史方案和服务报告。')"
            @logout="handleLogout"
            @sidebar-toggle="sidebarCollapsed = $event"
        />

        <main class="main-layout">
            <header class="page-header" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
                <div class="header-title">{{ currentMenuName }}</div>
                <div class="header-actions">
                    <button class="home-link" type="button" @click="emit('go-home')">
                        <i class="fa-solid fa-house"></i>
                        <span>首页介绍</span>
                    </button>
                    <div class="status-badge">
                        <i class="fa-solid fa-circle"></i>
                        <span>{{ activeMenu === 'smart' ? '出行服务已准备' : '七维智行助手在线' }}</span>
                    </div>
                </div>
            </header>

            <div class="page-body">
                <AIChat
                    v-if="activeMenu === 'ai'"
                    ref="aiChatRef"
                    :initial-messages="displayMessages"
                    :ai-thinking="aiThinking"
                    :ai-typing="aiTyping"
                    :image-uploading="imageUploading"
                    @send-message="handleSendMessage"
                    @interrupt="interrupt"
                    @save-plan="handleSavePlan"
                    @import-transit="importToTransit"
                    @import-smart="importToSmart"
                />

                <button v-if="activeMenu === 'ai'" class="new-chat-floating" @click="handleNewChat">
                    <i class="fa-solid fa-plus"></i>
                    <span>新建对话</span>
                </button>

                <div class="module-wrapper transit-page" v-if="activeMenu === 'transit'">
                    <TransitModule
                        ref="transitRef"
                        v-model:route-data="route"
                        @booking="importRouteToCustom"
                        @route-generated="handleRouteGenerated"
                    />
                </div>

                <div class="module-wrapper trip-page" v-if="activeMenu === 'custom'">
                    <CustomModule
                        v-model:form-data="customForm"
                        @generate-service="importToSmart"
                    />
                </div>

                <div class="module-wrapper" v-if="activeMenu === 'smart'">
                    <SmartServiceModule
                        :profile="profile"
                        :route-data="route"
                        :route-plan="routePlan"
                        :service-context="smartContext"
                        :is-logged-in="props.isLoggedIn"
                        @require-login="emit('require-login', $event)"
                    />
                </div>

                <div class="module-wrapper history-page" v-if="activeMenu === 'history'">
                    <HistoryModule
                        :history-list="historyList"
                        @delete="handleDeleteHistory"
                        @go-to-chat="activeMenu = 'ai'"
                        @import-transit="importToTransit"
                        @import-custom="importToCustom"
                    />
                </div>

                <div class="module-wrapper history-page" v-if="activeMenu === 'reports'">
                    <ServiceReportHistoryModule />
                </div>
            </div>
        </main>

        <ProfileModal
            :visible="showProfileModal"
            :profile="profile"
            @close="showProfileModal = false"
            @save="handleProfileSave"
        />
    </div>
</template>

<style scoped>
@import './chat.css';

.app-layout {
    height: 100vh;
    display: flex;
    overflow: hidden;
}

.main-layout {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
    height: 100vh;
}

.page-header {
    height: 64px;
    flex-shrink: 0;
    background: var(--bg-color);
    border-bottom: 1px solid var(--border-color);
    padding: 0 32px;
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.header-title {
    font-size: 20px;
    font-weight: 600;
    color: var(--text-main);
}

.header-actions {
    display: flex;
    align-items: center;
    gap: 10px;
}

.home-link {
    height: 34px;
    display: inline-flex;
    align-items: center;
    gap: 7px;
    padding: 0 12px;
    border: 1px solid var(--border-color);
    border-radius: 999px;
    background: var(--card-bg);
    color: var(--text-secondary);
    cursor: pointer;
}

.home-link:hover {
    color: var(--primary-color);
    border-color: var(--primary-color);
}

.status-badge {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 12px;
    border-radius: 12px;
    color: #16823a;
    background: rgba(22, 130, 58, 0.1);
    font-size: 13px;
}

.status-badge i {
    font-size: 8px;
}

.overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.1);
    z-index: 80;
}

.page-body {
    flex: 1;
    min-height: 0;
    overflow: hidden;
    background: var(--bg-color);
}

.module-wrapper {
    height: 100%;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    background: var(--bg-color);
}

.new-chat-floating {
    position: fixed;
    right: 32px;
    top: 78px;
    z-index: 20;
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 9px 14px;
    border: 1px solid var(--border-color);
    border-radius: 999px;
    background: var(--card-bg);
    color: var(--text-main);
    box-shadow: var(--shadow-sm);
    cursor: pointer;
}

.new-chat-floating:hover {
    color: var(--primary-color);
    border-color: var(--primary-color);
}

.transit-page {
    flex-direction: row;
}

.trip-page {
    overflow-y: auto;
}

@media (max-width: 768px) {
    .page-header {
        padding: 0 16px;
    }
}
</style>
