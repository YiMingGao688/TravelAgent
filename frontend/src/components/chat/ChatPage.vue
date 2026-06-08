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
import { uploadChatImage } from '../../api/aiApi.js'
import { deleteTripPlan, listTripPlans, saveTripPlan } from '../../api/tripApi.js'
import { useChatPageState } from '../../views/chat/composables/useChatPageState.js'

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
const { buildPlanPayload, contextToStructuredPlan, normalizePlan, extractStructuredPlan, extractDays } = useChatPageState({
    messages,
    profile,
    currentTripContext,
    sessionId
})

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
        const data = await uploadChatImage({
            file: userMsg.imageFile,
            text: userMsg.content || '请分析这张图片，并结合出行场景给出建议。',
            userId: sessionStorage.getItem('loginUser') || 'guest',
            sessionId: sessionId.value || Date.now().toString(),
            model: 'zhipu-cloud'
        })
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
@import '../../views/chat/styles/chatPage.css';
</style>
