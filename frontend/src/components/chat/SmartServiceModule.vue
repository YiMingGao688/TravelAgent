<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { generateSmartService, generateTravelReport, listSmartServices, saveServiceReport } from '../../utils/api.js'

const props = defineProps({
    profile: { type: Object, default: () => ({}) },
    routeData: { type: Object, default: () => ({}) },
    routePlan: { type: Object, default: null },
    serviceContext: { type: Object, default: () => ({}) }
})

const services = [
    { id: 'REALTIME_ALERT', name: '实时提醒', icon: 'fa-bell', desc: '天气、拥堵、晚点、换乘风险提醒' },
    { id: 'INTERMODAL_PLAN', name: '联程出行', icon: 'fa-train-subway', desc: '高铁、飞机、地铁、网约车组合规划' },
    { id: 'SPECIAL_SCENE', name: '特殊照顾', icon: 'fa-heart-pulse', desc: '老人、亲子、无障碍、夜间出行' },
    { id: 'AFTER_SALES', name: '售后协助', icon: 'fa-headset', desc: '退改签、延误处理、客服协助' },
    { id: 'PASSENGER_LUGGAGE', name: '行李服务', icon: 'fa-suitcase-rolling', desc: '行李提醒、托运、到站取送建议' },
    { id: 'PAYMENT_CREDIT', name: '支付核验', icon: 'fa-credit-card', desc: '组合支付、会员权益、信用免押' },
    { id: 'PROACTIVE_LOOP', name: '行程管家', icon: 'fa-arrows-rotate', desc: '行前、途中、到达后的主动服务' }
]

const activeType = ref('INTERMODAL_PLAN')
const loading = ref(false)
const reportLoading = ref(false)
const records = ref([])
const reportRecord = ref(null)
const reportSaved = ref(false)
const statusMessage = ref('')
const form = ref(defaultForm())

const activeService = computed(() => services.find(item => item.id === activeType.value))
const latestRecord = computed(() => records.value[0])
const latestTimeline = computed(() => parseTimeline(latestRecord.value?.responsePayload))
const reportSections = computed(() => parseReport(reportRecord.value?.responsePayload))

watch(() => props.serviceContext, value => {
    if (!value || !Object.keys(value).length) return
    form.value = {
        ...form.value,
        startPlace: value.startPlace || form.value.startPlace,
        endPlace: value.endPlace || form.value.endPlace,
        travelDate: value.travelDate || form.value.travelDate,
        preference: value.preference || form.value.preference,
        specialNeed: value.specialNeed || form.value.specialNeed,
        budget: value.budget || form.value.budget,
        passengerInfo: value.passengerInfo || form.value.passengerInfo,
        extraRequirement: value.imported
            ? buildImportedRequirement(value)
            : form.value.extraRequirement
    }
    statusMessage.value = value.imported ? '已从 AI 方案导入一站式服务信息' : ''
}, { deep: true, immediate: true })

watch(() => props.routeData, value => {
    if (!value) return
    form.value.startPlace = form.value.startPlace || value.from || ''
    form.value.endPlace = form.value.endPlace || value.to || ''
    form.value.preference = form.value.preference || value.preference || ''
}, { deep: true, immediate: true })

function defaultForm() {
    return {
        startPlace: '',
        endPlace: '',
        travelDate: '',
        preference: '',
        specialNeed: '',
        budget: '',
        passengerInfo: '',
        luggageInfo: '',
        paymentPreference: '微信/支付宝均可，优先使用会员权益',
        scenario: '日常跨城出行',
        extraRequirement: ''
    }
}

function buildImportedRequirement(value) {
    const parts = []
    if (value.startPlace || value.endPlace) {
        parts.push(`行程方向：${value.startPlace || '出发地'} 到 ${value.endPlace || '目的地'}`)
    }
    if (value.preference) parts.push(`出行偏好：${value.preference}`)
    if (value.budget) parts.push(`预算：${value.budget}`)
    if (value.specialNeed) parts.push(`特殊需求：${value.specialNeed}`)
    return parts.length ? parts.join('；') : '已导入行程定制结果，请生成完整出行服务安排。'
}

async function generate() {
    loading.value = true
    statusMessage.value = '正在生成服务方案...'
    try {
        const payload = {
            ...form.value,
            userId: sessionStorage.getItem('loginUser') || 'guest',
            serviceType: activeType.value,
            startPlace: form.value.startPlace || props.routeData.from || props.profile.city || '',
            endPlace: form.value.endPlace || props.routeData.to || '',
            preference: form.value.preference || props.profile.style || props.profile.transport || ''
        }
        const result = await generateSmartService(payload)
        await loadRecords()
        statusMessage.value = '服务方案已生成并保存'
    } catch (error) {
        statusMessage.value = `生成失败：${error.message}`
    } finally {
        loading.value = false
    }
}

async function generateReport() {
    reportLoading.value = true
    statusMessage.value = '正在生成出行服务报告...'
    try {
        const activeRoute = props.routePlan?.activeRoute
        const payload = {
            userId: sessionStorage.getItem('loginUser') || 'guest',
            startPlace: form.value.startPlace || props.routeData.from || '',
            endPlace: form.value.endPlace || props.routeData.to || '',
            travelDate: form.value.travelDate,
            preference: form.value.preference || props.routeData.preference || '',
            specialNeed: form.value.specialNeed,
            aiPlan: props.serviceContext?.aiPlan || '',
            routeSummary: activeRoute
                ? `${activeRoute.routeName}，约 ${activeRoute.distance} 公里，${activeRoute.duration} 分钟，预计 ${activeRoute.cost} 元。${activeRoute.recommendReason || ''}`
                : '尚未生成路线摘要',
            weatherSummary: weatherSummary(),
            riskSummary: props.routePlan?.risk ? `${props.routePlan.risk.label}：${props.routePlan.risk.reasons.join('；')}` : '暂无风险评估',
            luggageInfo: form.value.luggageInfo,
            afterSalesNeed: form.value.extraRequirement,
            paymentPreference: form.value.paymentPreference
        }
        const result = await generateTravelReport(payload)
        reportRecord.value = result.data
        reportSaved.value = false
        await loadRecords()
        statusMessage.value = '出行服务报告已生成，可点击保存报告'
    } catch (error) {
        statusMessage.value = `报告生成失败：${error.message}`
    } finally {
        reportLoading.value = false
    }
}

async function saveReport() {
    if (!reportRecord.value?.responsePayload) {
        statusMessage.value = '请先生成报告'
        return
    }
    try {
        const content = reportRecord.value.responsePayload
        await saveServiceReport({
            userId: sessionStorage.getItem('loginUser') || 'guest',
            tripPlanId: props.routeData.tripPlanId || null,
            title: reportRecord.value.title,
            startPlace: form.value.startPlace || props.routeData.from || '',
            endPlace: form.value.endPlace || props.routeData.to || '',
            reportContent: content,
            reportSummary: content.length > 160 ? content.slice(0, 160) : content,
            sourceType: 'SMART_SERVICE'
        })
        reportSaved.value = true
        statusMessage.value = '报告已保存，可在历史出行服务报告中查看。'
    } catch (error) {
        statusMessage.value = `保存报告失败：${error.message}`
    }
}

async function loadRecords() {
    const userId = sessionStorage.getItem('loginUser') || 'guest'
    records.value = await listSmartServices(userId)
}

function fillExample(type = activeType.value) {
    activeType.value = type
    form.value = {
        startPlace: props.routeData.from || '南京南站',
        endPlace: props.routeData.to || '杭州东站',
        travelDate: '明天 09:00',
        preference: props.profile.style || '少换乘、舒适优先',
        specialNeed: type === 'SPECIAL_SCENE' ? '同行有老人，行李较多，希望少走路并提前安排协助服务' : '如果出现晚点或拥堵，希望自动给出替代方案',
        budget: '中等预算',
        passengerInfo: '2 名成人，1 名老人',
        luggageInfo: '2 个 24 寸行李箱，1 个背包',
        paymentPreference: '微信支付，优先使用会员权益和信用免押',
        scenario: activeService.value?.desc || '日常跨城出行',
        extraRequirement: '请给出可直接执行的出行安排、提醒节点、预订建议和异常处理方案。'
    }
}

function weatherSummary() {
    const start = props.routePlan?.startWeather
    const end = props.routePlan?.endWeather
    const one = start ? `出发地${start.city || ''}${start.weather || ''}${start.temperature || '-'}℃` : '出发地暂无天气'
    const two = end ? `目的地${end.city || ''}${end.weather || ''}${end.temperature || '-'}℃` : '目的地暂无天气'
    return `${one}；${two}`
}

function parseTimeline(text = '') {
    if (!text) return []
    const fallback = [
        { stage: '行前', title: '确认出行准备', status: '待处理', desc: '核对出发时间、证件、行李和到站方式。' },
        { stage: '行中', title: '途中动态关注', status: '风险提醒', desc: '关注天气、换乘、拥堵和晚点变化。' },
        { stage: '行后', title: '到达后服务', status: '待处理', desc: '确认接驳、发票、售后和行李取送需求。' }
    ]
    const lines = String(text).split(/\n+/).map(item => item.trim()).filter(Boolean)
    const parsed = lines.map(line => {
        const stage = (line.match(/【(行前|行中|行后)】/) || [])[1]
        if (!stage) return null
        const clean = line.replace(/【(行前|行中|行后)】/, '')
        const parts = clean.split(/[｜|]/).map(item => item.trim()).filter(Boolean)
        const title = parts[0] || '服务节点'
        const statusPart = parts.find(item => item.startsWith('状态')) || ''
        const descPart = parts.find(item => item.startsWith('说明')) || parts.slice(1).join('；')
        return {
            stage,
            title,
            status: statusPart.replace(/^状态[:：]/, '') || inferStatus(line),
            desc: descPart.replace(/^说明[:：]/, '') || line
        }
    }).filter(Boolean)
    return parsed.length ? parsed : fallback
}

function parseReport(text = '') {
    if (!text) return []
    const sections = []
    const parts = String(text).split(/(?=一、|二、|三、|四、|五、|六、)/).map(item => item.trim()).filter(Boolean)
    for (const part of parts) {
        const [title, ...rest] = part.split(/\n/)
        sections.push({ title, content: rest.join('\n').trim() || part })
    }
    return sections.length ? sections : [{ title: reportRecord.value?.title || '出行服务报告', content: text }]
}

function inferStatus(text) {
    if (/风险|延误|拥堵|天气|提醒/.test(text)) return '风险提醒'
    if (/完成|已/.test(text)) return '已完成'
    return '待处理'
}

function statusClass(status) {
    if (status === '已完成') return 'done'
    if (status === '风险提醒') return 'risk'
    return 'todo'
}

function groupedTimeline(stage) {
    return latestTimeline.value.filter(item => item.stage === stage)
}

function serviceTitle(type) {
    return services.find(item => item.id === type)?.name || (type === 'TRAVEL_REPORT' ? '出行服务报告' : '出行服务')
}

function recordStatusText(status) {
    const statusMap = {
        SAVED: '已保存',
        GENERATED: '已生成',
        COMPLETED: '已完成',
        PENDING: '待处理',
        FAILED: '生成失败'
    }
    return statusMap[status] || status || '已保存'
}

function servicePanelStateText() {
    return latestRecord.value ? '已生成方案' : '待生成'
}

onMounted(loadRecords)
</script>

<template>
    <div class="service-page">
        <section class="topbar">
            <div>
                <span class="eyebrow">一站式服务</span>
                <h1>智慧出行服务工作台</h1>
                <p>把路线、天气、提醒、行李、售后和支付核验整合到同一份可保存的服务方案里。</p>
            </div>
            <div class="top-actions">
                <button class="ghost" @click="fillExample()">
                    <i class="fa-solid fa-wand-magic-sparkles"></i>
                    <span>填充示例</span>
                </button>
                <button class="primary" :disabled="reportLoading" @click="generateReport">
                    <i class="fa-solid fa-file-lines"></i>
                    <span>{{ reportLoading ? '正在生成报告...' : '生成出行服务报告' }}</span>
                </button>
                <button class="ghost" :disabled="!reportRecord || reportSaved" @click="saveReport">
                    <i class="fa-solid fa-floppy-disk"></i>
                    <span>{{ reportSaved ? '报告已保存' : '保存报告' }}</span>
                </button>
            </div>
        </section>

        <div v-if="statusMessage" class="status-message" :class="{ error: statusMessage.includes('失败') }">
            <i class="fa-solid" :class="statusMessage.includes('失败') ? 'fa-circle-exclamation' : 'fa-circle-check'"></i>
            <span>{{ statusMessage }}</span>
        </div>

        <div class="page-grid">
            <aside class="service-list">
                <button
                    v-for="item in services"
                    :key="item.id"
                    :class="{ active: activeType === item.id }"
                    @click="activeType = item.id"
                >
                    <i :class="['fa-solid', item.icon]"></i>
                    <span>
                        <strong>{{ item.name }}</strong>
                        <small>{{ item.desc }}</small>
                    </span>
                </button>
            </aside>

            <form class="request-panel" @submit.prevent="generate">
                <div class="panel-title">
                    <i :class="['fa-solid', activeService?.icon]"></i>
                    <div>
                        <h2>{{ activeService?.name }}</h2>
                        <p>{{ activeService?.desc }}</p>
                    </div>
                </div>

                <div class="form-row">
                    <label>出发地<input v-model="form.startPlace" placeholder="例如：南京南站" /></label>
                    <label>目的地<input v-model="form.endPlace" placeholder="例如：杭州东站" /></label>
                </div>
                <div class="form-row">
                    <label>出行时间<input v-model="form.travelDate" placeholder="例如：明天 09:00" /></label>
                    <label>预算<input v-model="form.budget" placeholder="例如：中等预算" /></label>
                </div>
                <label>出行偏好<input v-model="form.preference" placeholder="少换乘、舒适优先、时间优先、经济优先" /></label>
                <label>特殊需求<input v-model="form.specialNeed" placeholder="老人、亲子、无障碍、夜间、赶时间、行李较多等" /></label>
                <div class="form-row">
                    <label>同行人<input v-model="form.passengerInfo" placeholder="例如：2 名成人，1 名老人" /></label>
                    <label>行李<input v-model="form.luggageInfo" placeholder="例如：2 个行李箱，1 个背包" /></label>
                </div>
                <label>支付与权益偏好<input v-model="form.paymentPreference" placeholder="微信/支付宝/银行卡，会员权益，信用免押" /></label>
                <label>补充说明<textarea v-model="form.extraRequirement" placeholder="例如：希望到站后直接安排接驳，途中有变化时及时提醒"></textarea></label>

                <button class="primary submit" :disabled="loading">
                    <i class="fa-solid fa-cloud"></i>
                    <span>{{ loading ? '正在生成服务方案...' : '生成服务方案' }}</span>
                </button>
            </form>

            <section class="result-panel">
                <div class="panel-header">
                    <div>
                        <h2>服务方案</h2>
                        <p>按行前、行中、行后拆解为可执行节点</p>
                    </div>
                    <span>{{ servicePanelStateText() }}</span>
                </div>

                <div v-if="!latestRecord" class="empty">
                    <i class="fa-solid fa-route"></i>
                    <p>导入 AI 方案或填写需求后，点击生成服务方案。</p>
                </div>

                <article v-else class="record">
                    <div class="record-head">
                        <strong>{{ latestRecord.title || serviceTitle(latestRecord.serviceType) }}</strong>
                        <span>{{ recordStatusText(latestRecord.status) }}</span>
                    </div>
                    <div class="route-line">{{ latestRecord.startPlace || '出发地' }} → {{ latestRecord.endPlace || '目的地' }}</div>

                    <div v-if="latestRecord.serviceType !== 'TRAVEL_REPORT'" class="timeline">
                        <section v-for="stage in ['行前', '行中', '行后']" :key="stage" class="stage">
                            <h3>{{ stage }}</h3>
                            <div class="node-list">
                                <article v-for="node in groupedTimeline(stage)" :key="stage + node.title + node.desc" class="node-card">
                                    <div>
                                        <strong>{{ node.title }}</strong>
                                        <p>{{ node.desc }}</p>
                                    </div>
                                    <span :class="['node-status', statusClass(node.status)]">{{ node.status }}</span>
                                </article>
                            </div>
                        </section>
                    </div>

                    <div v-else class="report-preview">
                        <section v-for="section in parseReport(latestRecord.responsePayload)" :key="section.title">
                            <h3>{{ section.title }}</h3>
                            <p>{{ section.content }}</p>
                        </section>
                    </div>
                </article>

                <article v-if="reportRecord" class="report-card">
                    <div class="record-head">
                        <strong>{{ reportRecord.title }}</strong>
                        <span>已保存</span>
                    </div>
                    <section v-for="section in reportSections" :key="section.title">
                        <h3>{{ section.title }}</h3>
                        <p>{{ section.content }}</p>
                    </section>
                </article>
            </section>
        </div>
    </div>
</template>

<style scoped>
.service-page {
    height: 100%;
    overflow-y: auto;
    padding: 24px 32px 40px;
    background: var(--bg-color);
}

.topbar {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    gap: 24px;
    margin-bottom: 16px;
}

.eyebrow {
    color: var(--primary-color);
    font-size: 12px;
    font-weight: 700;
}

.topbar h1 {
    margin: 6px 0;
    color: var(--text-main);
    font-size: 28px;
}

.topbar p,
.panel-title p,
.panel-header p,
.empty p,
.service-list small,
.node-card p,
.report-card p,
.report-preview p {
    color: var(--text-secondary);
    line-height: 1.6;
}

.top-actions {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
}

.primary,
.ghost {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    border-radius: 8px;
    padding: 11px 16px;
    cursor: pointer;
    font-weight: 600;
}

.primary {
    border: none;
    background: var(--primary-color);
    color: white;
}

.ghost {
    border: 1px solid var(--border-color);
    background: var(--card-bg);
    color: var(--text-main);
}

.primary:disabled {
    opacity: 0.65;
    cursor: not-allowed;
}

.status-message {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
    padding: 8px 12px;
    border-radius: 999px;
    background: #dcfce7;
    color: #166534;
    font-size: 13px;
}

.status-message.error {
    background: #fee2e2;
    color: #991b1b;
}

.page-grid {
    display: grid;
    grid-template-columns: 250px 410px minmax(0, 1fr);
    gap: 18px;
    align-items: start;
}

.service-list,
.request-panel,
.result-panel {
    background: var(--card-bg);
    border: 1px solid var(--border-color);
    border-radius: 8px;
}

.service-list {
    padding: 10px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    position: sticky;
    top: 0;
}

.service-list button {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    border: none;
    border-radius: 8px;
    padding: 12px;
    background: transparent;
    color: var(--text-main);
    cursor: pointer;
    text-align: left;
}

.service-list button.active,
.service-list button:hover {
    background: var(--primary-light);
}

.service-list i {
    color: var(--primary-color);
    margin-top: 2px;
    width: 20px;
}

.service-list strong,
.service-list small {
    display: block;
}

.request-panel,
.result-panel {
    padding: 18px;
}

.panel-title {
    display: flex;
    gap: 12px;
    align-items: flex-start;
    margin-bottom: 14px;
}

.panel-title i {
    color: var(--primary-color);
    font-size: 22px;
}

.panel-title h2,
.panel-header h2 {
    color: var(--text-main);
    font-size: 18px;
}

.form-row {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
}

label {
    display: block;
    margin-top: 12px;
    color: var(--text-secondary);
    font-size: 13px;
    font-weight: 600;
}

input,
textarea {
    width: 100%;
    margin-top: 7px;
    padding: 10px 11px;
    border: 1px solid var(--border-color);
    border-radius: 8px;
    background: var(--bg-color);
    color: var(--text-main);
    outline: none;
    font-family: inherit;
}

textarea {
    min-height: 86px;
    resize: vertical;
}

.submit {
    width: 100%;
    margin-top: 16px;
}

.panel-header,
.record-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
}

.panel-header span,
.record-head span {
    color: #16823a;
    background: rgba(22, 130, 58, 0.1);
    border-radius: 14px;
    padding: 5px 10px;
    font-size: 12px;
}

.empty {
    min-height: 420px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
}

.empty i {
    font-size: 48px;
    color: var(--border-color);
    margin-bottom: 14px;
}

.record,
.report-card {
    margin-top: 16px;
}

.record-head strong {
    color: var(--text-main);
    font-size: 18px;
}

.route-line {
    margin-top: 12px;
    padding: 9px 11px;
    border-radius: 8px;
    background: var(--bg-color);
    color: var(--primary-color);
    font-weight: 600;
    font-size: 13px;
}

.timeline {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
    margin-top: 14px;
}

.stage {
    min-width: 0;
}

.stage h3,
.report-card h3,
.report-preview h3 {
    margin: 0 0 10px;
    color: var(--text-main);
    font-size: 15px;
}

.node-list {
    display: grid;
    gap: 10px;
}

.node-card {
    min-height: 128px;
    display: flex;
    justify-content: space-between;
    gap: 8px;
    flex-direction: column;
    padding: 12px;
    border: 1px solid var(--border-color);
    border-radius: 8px;
    background: var(--bg-color);
}

.node-card strong {
    color: var(--text-main);
}

.node-status {
    width: fit-content;
    padding: 4px 8px;
    border-radius: 999px;
    font-size: 12px;
}

.node-status.todo {
    background: #eef2ff;
    color: #3730a3;
}

.node-status.done {
    background: #dcfce7;
    color: #166534;
}

.node-status.risk {
    background: #fef3c7;
    color: #92400e;
}

.report-card,
.report-preview {
    padding: 14px;
    border: 1px solid var(--border-color);
    border-radius: 8px;
    background: var(--bg-color);
}

.report-card section,
.report-preview section {
    margin-top: 14px;
}

.report-card p,
.report-preview p {
    white-space: pre-wrap;
}

@media (max-width: 1280px) {
    .page-grid {
        grid-template-columns: 220px minmax(0, 1fr);
    }

    .result-panel {
        grid-column: 1 / -1;
    }
}

@media (max-width: 820px) {
    .service-page {
        padding: 20px 16px;
    }

    .topbar {
        align-items: flex-start;
        flex-direction: column;
    }

    .page-grid,
    .timeline {
        grid-template-columns: 1fr;
    }

    .service-list {
        position: static;
    }
}
</style>
