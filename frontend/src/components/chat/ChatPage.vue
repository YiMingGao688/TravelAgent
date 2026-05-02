<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import AIChat from './AIChat.vue'
import TransitModule from './TransitModule.vue'
import CustomModule from './CustomModule.vue'
import HistoryModule from './HistoryModule.vue'
import SmartServiceModule from './SmartServiceModule.vue'
import AppSidebar from '../common/AppSidebar.vue'
import ProfileModal from '../common/ProfileModal.vue'
import { useChat, useProfile, useTheme } from '../../composables/index.js'
import { getCurrentTime } from '../../utils/time'
import { deleteTripPlan, listTripPlans, saveTripPlan } from '../../utils/api.js'

const emit = defineEmits(['logout'])

const activeMenu = ref('ai')
const showSettings = ref(false)
const userName = ref(sessionStorage.getItem('loginUser'))
const showProfileModal = ref(false)
const sidebarCollapsed = ref(true)
const route = ref({ from: '', to: '', preference: '', tripPlanId: null })
const customForm = ref({ destination: '', days: 3, budget: '舒适预算', style: '深度人文' })
const historyList = ref([])
const aiChatRef = ref(null)
const transitRef = ref(null)
const imageUploading = ref(false)

const { messages, aiThinking, aiTyping, sessionId, sendMessage, interrupt, startNewChat, init } = useChat(aiChatRef)
const { profile, loadProfile, saveProfile } = useProfile()
const { initTheme, toggleTheme } = useTheme()

const currentMenuName = computed(() => ({
    ai: '智能 AI 咨询',
    transit: '交通规划',
    custom: '行程定制',
    smart: '一站式出行服务',
    history: '历史方案'
})[activeMenu.value] || '智能出行')

async function switchMenu(id) {
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
    emit('logout')
}

async function handleProfileSave(savedProfile) {
    profile.value = savedProfile
    await saveProfile()
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
    const structured = buildPlanPayload(msg)
    try {
        const result = await saveTripPlan(structured)
        const saved = result.data
        historyList.value.unshift(saved)
        messages.value.push({
            role: 'ai',
            content: `方案已保存到历史方案。识别到的起终点：${saved.startPlace || '未识别'} -> ${saved.endPlace || '未识别'}。`,
            time: getCurrentTime(),
            isPlan: false
        })
    } catch (error) {
        alert(`保存失败：${error.message}`)
    }
}

async function loadTripPlans() {
    const userId = sessionStorage.getItem('loginUser')
    if (!userId) return
    historyList.value = await listTripPlans(userId)
}

async function handleDeleteHistory(index) {
    if (index === -1) {
        if (!confirm('确定清空全部历史方案吗？')) return
        const ids = historyList.value.map(item => item.id).filter(Boolean)
        await Promise.all(ids.map(id => deleteTripPlan(id)))
        historyList.value = []
        return
    }
    const item = historyList.value[index]
    if (!item || !confirm('确定删除这条方案吗？')) return
    if (item.id) await deleteTripPlan(item.id)
    historyList.value.splice(index, 1)
}

function importToTransit(source) {
    const plan = normalizePlan(source)
    route.value = {
        from: plan.startPlace || route.value.from,
        to: plan.endPlace || route.value.to || plan.destination,
        preference: plan.travelPreference || profile.value.style || profile.value.transport || '舒适优先',
        tripPlanId: plan.id || null
    }
    activeMenu.value = 'transit'
    nextTick(() => transitRef.value?.searchSmartRoute(false))
}

function importToCustom(source) {
    const plan = normalizePlan(source)
    customForm.value = {
        destination: plan.endPlace || plan.destination || customForm.value.destination,
        days: plan.days || extractDays(plan.content) || customForm.value.days,
        budget: plan.budget || customForm.value.budget,
        style: plan.travelPreference || profile.value.style || customForm.value.style
    }
    activeMenu.value = 'custom'
}

function buildPlanPayload(msg) {
    const text = msg.content || ''
    const lastUserMessage = [...messages.value].reverse().find(item => item.role === 'user')?.content || msg.rawQuestion || ''
    const extracted = extractPlaces(`${lastUserMessage}\n${text}`)
    return {
        userId: sessionStorage.getItem('loginUser') || 'guest',
        sessionId: msg.sessionId || sessionId.value,
        title: extracted.startPlace && extracted.endPlace ? `${extracted.startPlace} 到 ${extracted.endPlace} 出行方案` : 'AI 智能出行方案',
        startPlace: extracted.startPlace,
        endPlace: extracted.endPlace,
        content: text,
        rawQuestion: lastUserMessage,
        sourceType: 'AI_CHAT',
        travelPreference: profile.value.style || profile.value.transport || '',
        budget: extractBudget(`${lastUserMessage}\n${text}`),
        days: extractDays(`${lastUserMessage}\n${text}`)
    }
}

function normalizePlan(source) {
    if (source?.content && source?.role === 'ai') {
        return buildPlanPayload(source)
    }
    return source || {}
}

function extractPlaces(text) {
    const patterns = [
        /从\s*([\u4e00-\u9fa5A-Za-z0-9·\-\s]{2,30})\s*(?:出发)?(?:到|去|前往)\s*([\u4e00-\u9fa5A-Za-z0-9·\-\s]{2,30})/,
        /起点[:：]\s*([^\n，。；;]{2,30})[\s\S]*?终点[:：]\s*([^\n，。；;]{2,30})/,
        /出发地[:：]\s*([^\n，。；;]{2,30})[\s\S]*?目的地[:：]\s*([^\n，。；;]{2,30})/
    ]
    for (const pattern of patterns) {
        const match = text.match(pattern)
        if (match) {
            return { startPlace: cleanPlace(match[1]), endPlace: cleanPlace(match[2]) }
        }
    }
    const destination = text.match(/(?:去|到|目的地是)\s*([\u4e00-\u9fa5A-Za-z0-9·\-]{2,20})/)
    return { startPlace: profile.value.city || '', endPlace: destination ? cleanPlace(destination[1]) : '' }
}

function cleanPlace(value) {
    return String(value || '').replace(/[，。！？；;,.!?]/g, '').replace(/的.*$/, '').trim()
}

function extractDays(text = '') {
    const match = text.match(/(\d+)\s*(?:天|日)/)
    return match ? Number(match[1]) : null
}

function extractBudget(text = '') {
    const match = text.match(/预算[:：]?\s*([^\n，。]{2,20})/)
    return match ? match[1].trim() : ''
}

function handleNewChat() {
    startNewChat()
    activeMenu.value = 'ai'
}

onMounted(async () => {
    initTheme()
    const userId = sessionStorage.getItem('loginUser')
    if (userId) {
        await loadProfile(userId)
        await loadTripPlans()
    }
    init()
})
</script>

<template>
    <div class="app-layout">
        <div class="overlay" v-if="showSettings" @click="showSettings = false"></div>

        <AppSidebar
            :user-name="userName"
            :show-settings="showSettings"
            @select-menu="switchMenu"
            @toggle-settings="showSettings = !showSettings"
            @open-profile="showProfileModal = true"
            @toggle-theme="toggleTheme"
            @logout="handleLogout"
            @sidebar-toggle="sidebarCollapsed = $event"
            @new-chat="handleNewChat"
        />

        <main class="main-layout">
            <header class="page-header" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
                <div class="header-title">{{ currentMenuName }}</div>
                <div class="status-badge">
                    <i class="fa-solid fa-circle"></i>
                    <span>{{ activeMenu === 'smart' ? '出行服务已准备' : 'AI 出行助手在线' }}</span>
                </div>
            </header>

            <div class="page-body">
                <AIChat
                    v-if="activeMenu === 'ai'"
                    ref="aiChatRef"
                    :initial-messages="messages"
                    :ai-thinking="aiThinking"
                    :ai-typing="aiTyping"
                    :image-uploading="imageUploading"
                    @send-message="handleSendMessage"
                    @interrupt="interrupt"
                    @save-plan="handleSavePlan"
                    @import-transit="importToTransit"
                    @import-custom="importToCustom"
                />

                <div class="module-wrapper transit-page" v-if="activeMenu === 'transit'">
                    <TransitModule ref="transitRef" v-model:route-data="route" @booking="activeMenu = 'smart'" />
                </div>

                <div class="module-wrapper trip-page" v-if="activeMenu === 'custom'">
                    <CustomModule v-model:form-data="customForm" />
                </div>

                <div class="module-wrapper" v-if="activeMenu === 'smart'">
                    <SmartServiceModule :profile="profile" :route-data="route" />
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
