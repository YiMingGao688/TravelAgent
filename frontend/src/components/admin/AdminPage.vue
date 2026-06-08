<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { adminCreate, adminDelete, adminGet, adminLogin, adminUpdate } from '../../utils/api.js'
import logoUrl from '../../assets/logo.png'

const loggedIn = ref(sessionStorage.getItem('adminToken') === 'simple-admin-token')
const loginForm = ref({ username: 'admin', password: 'admin123' })
const active = ref('dashboard')
const loading = ref(false)
const saving = ref(false)
const dashboard = ref({})
const rows = ref([])
const keyword = ref('')
const pageNo = ref(1)
const pageSize = ref(10)
const editor = ref(null)
const lastRefreshTime = ref('')
const dashboardError = ref('')
const collapsedGroups = ref({})

const modules = [
    { id: 'dashboard', name: '运营总览', icon: 'fa-chart-pie' },
    { id: 'users', name: '用户管理', icon: 'fa-users' },
    { id: 'trip-plans', name: '历史方案管理', icon: 'fa-map' },
    { id: 'routes', name: '路线记录管理', icon: 'fa-route' },
    { id: 'smart-services', name: '一站式服务记录', icon: 'fa-clipboard-check' },
    { id: 'service-reports', name: '出行服务报告', icon: 'fa-file-lines' },
    { id: 'orders', name: '全部模拟订单', icon: 'fa-receipt' },
    { id: 'orders-flight', name: '飞机模拟订单', icon: 'fa-plane' },
    { id: 'orders-train', name: '高铁模拟订单', icon: 'fa-train' },
    { id: 'orders-hotel', name: '酒店模拟订单', icon: 'fa-hotel' },
    { id: 'orders-cruise', name: '邮轮模拟订单', icon: 'fa-ship' },
    { id: 'orders-taxi', name: '接驳用车订单', icon: 'fa-taxi' },
    { id: 'hotels', name: '酒店模拟数据', icon: 'fa-hotel' },
    { id: 'traffic', name: '交通产品模拟数据', icon: 'fa-train' },
    { id: 'risks', name: '风险预警管理', icon: 'fa-triangle-exclamation' },
    { id: 'timelines', name: '全过程时间轴', icon: 'fa-timeline' },
    { id: 'after-sales', name: '售后工单管理', icon: 'fa-headset' },
    { id: 'refund-applications', name: '退款申请审核', icon: 'fa-money-bill-transfer' },
    { id: 'chat-records', name: '咨询记录管理', icon: 'fa-comments' },
    { id: 'profiles', name: '用户偏好管理', icon: 'fa-id-card' }
]

const moduleGroups = [
    { name: '总览', ids: ['dashboard'] },
    { name: 'AI 咨询板块', ids: ['chat-records', 'profiles', 'trip-plans', 'routes'] },
    { name: '模拟数据', ids: ['hotels', 'traffic'] },
    { name: '模拟订单', ids: ['orders', 'orders-flight', 'orders-train', 'orders-hotel', 'orders-cruise', 'orders-taxi'], collapsed: true },
    { name: '闭环服务', ids: ['risks', 'timelines', 'refund-applications', 'after-sales', 'smart-services', 'service-reports', 'users'] }
]

collapsedGroups.value = moduleGroups.reduce((acc, group) => {
    acc[group.name] = Boolean(group.collapsed)
    return acc
}, {})

function modulesByIds(ids) {
    return ids.map(id => modules.find(item => item.id === id)).filter(Boolean)
}

const moduleConfig = {
    users: {
        columns: ['id', 'username', 'email', 'nickname', 'phone', 'role', 'status', 'createTime', 'updateTime'],
        editable: ['username', 'email', 'password', 'nickname', 'phone', 'role', 'status'],
        empty: { username: '', email: '', password: '123456', nickname: '', phone: '', role: 'USER', status: 'ENABLED' }
    },
    'trip-plans': {
        columns: ['id', 'userId', 'title', 'startPlace', 'endPlace', 'sourceType', 'createTime'],
        editable: ['userId', 'title', 'startPlace', 'endPlace', 'sourceType', 'travelPreference', 'budget', 'days', 'content'],
        empty: { userId: '', title: '', startPlace: '', endPlace: '', sourceType: 'ADMIN', travelPreference: '', budget: '', days: 1, content: '' }
    },
    routes: {
        columns: ['id', 'userId', 'startPoint', 'endPoint', 'routeName', 'distance', 'duration', 'cost', 'createTime'],
        editable: ['userId', 'startPoint', 'endPoint', 'routeType', 'routeName', 'distance', 'duration', 'cost', 'description', 'recommendReason', 'preference'],
        empty: { userId: '', startPoint: '', endPoint: '', routeType: 'SMART', routeName: '', distance: 0, duration: 0, cost: 0, description: '', recommendReason: '', preference: '' }
    },
    'smart-services': {
        columns: ['id', 'userId', 'serviceType', 'title', 'startPlace', 'endPlace', 'status', 'createTime'],
        editable: ['userId', 'serviceType', 'title', 'startPlace', 'endPlace', 'status', 'responsePayload'],
        empty: { userId: '', serviceType: 'REALTIME_ALERT', title: '', startPlace: '', endPlace: '', status: 'GENERATED', responsePayload: '' }
    },
    'service-reports': {
        columns: ['id', 'userId', 'title', 'startPlace', 'endPlace', 'sourceType', 'createTime'],
        editable: ['userId', 'tripPlanId', 'title', 'startPlace', 'endPlace', 'reportSummary', 'reportContent', 'sourceType'],
        empty: { userId: '', tripPlanId: null, title: '', startPlace: '', endPlace: '', reportSummary: '', reportContent: '', sourceType: 'ADMIN' }
    },
    orders: {
        columns: ['id', 'orderNo', 'userId', 'orderType', 'productName', 'totalAmount', 'status', 'createTime'],
        editable: [],
        empty: null
    },
    hotels: {
        columns: ['id', 'name', 'city', 'address', 'starLevel', 'score', 'minPrice', 'tags'],
        editable: [],
        empty: null
    },
    traffic: {
        columns: ['id', 'type', 'startPlace', 'endPlace', 'departTime', 'arriveTime', 'price', 'carrierName', 'remainCount'],
        editable: [],
        empty: null
    },
    risks: {
        columns: ['level', 'type', 'title', 'content', 'suggestion'],
        editable: [],
        empty: null
    },
    timelines: {
        columns: ['id', 'userId', 'tripPlanId', 'stage', 'serviceType', 'title', 'triggerTime', 'status', 'createTime'],
        editable: [],
        empty: null
    },
    'after-sales': {
        columns: ['id', 'userId', 'tripPlanId', 'orderId', 'serviceType', 'title', 'status', 'processor', 'processRemark', 'createTime'],
        editable: ['status', 'processor', 'processRemark'],
        empty: null
    },
    'refund-applications': {
        columns: ['id', 'userId', 'orderId', 'title', 'status', 'processor', 'processRemark', 'createTime'],
        editable: ['status', 'processor', 'processRemark'],
        empty: null
    },
    'chat-records': {
        columns: ['id', 'userId', 'sessionId', 'userMessage', 'aiResponse', 'model', 'createTime'],
        editable: ['userId', 'sessionId', 'userMessage', 'aiResponse', 'model'],
        empty: { userId: '', sessionId: '', userMessage: '', aiResponse: '', model: 'admin-manual' }
    },
    profiles: {
        columns: ['id', 'userId', 'city', 'transport', 'style', 'note', 'updatedAt'],
        editable: ['userId', 'city', 'transport', 'style', 'note'],
        empty: { userId: '', city: '', transport: '', style: '', note: '' }
    }
}

const columnLabels = {
    id: 'ID',
    orderNo: '订单号',
    orderType: '订单类型',
    productName: '产品名称',
    totalAmount: '订单金额',
    name: '名称',
    address: '地址',
    starLevel: '星级',
    score: '评分',
    minPrice: '最低价',
    tags: '标签',
    type: '类型',
    departTime: '出发时间',
    arriveTime: '到达时间',
    carrierName: '车次/航班/线路',
    remainCount: '余量',
    level: '风险等级',
    content: '内容',
    suggestion: '建议',
    stage: '阶段',
    serviceType: '服务类型',
    triggerTime: '触发时间',
    processor: '处理人',
    processRemark: '处理备注',
    email: '邮箱',
    username: '账号',
    nickname: '昵称',
    phone: '手机号',
    role: '角色',
    password: '密码',
    status: '状态',
    createTime: '创建时间',
    updateTime: '更新时间',
    updatedAt: '更新时间',
    userId: '用户',
    title: '标题',
    startPlace: '出发地',
    endPlace: '目的地',
    startPoint: '出发地',
    endPoint: '目的地',
    sourceType: '来源',
    travelPreference: '出行偏好',
    budget: '预算',
    days: '天数',
    content: '方案内容',
    routeType: '路线类型',
    routeName: '路线名称',
    distance: '距离',
    duration: '耗时',
    cost: '费用',
    description: '路线说明',
    recommendReason: '推荐理由',
    preference: '偏好',
    serviceType: '服务类型',
    status: '状态',
    responsePayload: '服务内容',
    reportSummary: '报告摘要',
    reportContent: '报告内容',
    tripPlanId: '关联方案',
    sessionId: '会话',
    userMessage: '用户问题',
    aiResponse: 'AI 回复',
    model: '模型',
    city: '常驻城市',
    transport: '交通偏好',
    style: '出行风格',
    note: '备注'
}

const orderModuleTypeMap = {
    'orders-flight': 'FLIGHT',
    'orders-train': 'TRAIN',
    'orders-hotel': 'HOTEL',
    'orders-cruise': 'CRUISE',
    'orders-taxi': 'TAXI'
}

const currentModule = computed(() => modules.find(item => item.id === active.value))
const config = computed(() => moduleConfig[resolveModuleConfigKey(active.value)] || { columns: [], editable: [], empty: null })
const columns = computed(() => config.value.columns)
const editableFields = computed(() => config.value.editable)

const metricCards = computed(() => [
    { label: '用户总数', desc: '平台注册用户规模', value: dashboard.value.userCount || 0, icon: 'fa-users', tone: 'blue', target: 'users', badge: todayBadge('用户') },
    { label: '实时订单', desc: '用户真实下单记录', value: dashboard.value.bookingOrderCount || 0, icon: 'fa-receipt', tone: 'green', target: 'orders', badge: todayBadge('订单') },
    { label: '咨询记录', desc: 'AI 咨询对话沉淀', value: dashboard.value.chatRecordCount || 0, icon: 'fa-comments', tone: 'violet', target: 'chat-records', badge: '持续积累' },
    { label: '路线记录', desc: '交通规划查询记录', value: dashboard.value.routeCount || 0, icon: 'fa-route', tone: 'cyan', target: 'routes', badge: '实时联动' },
    { label: '一站式服务', desc: '主动服务方案记录', value: dashboard.value.smartServiceCount || 0, icon: 'fa-clipboard-check', tone: 'orange', target: 'smart-services', badge: '闭环服务' },
    { label: '服务报告', desc: '可追溯出行报告', value: dashboard.value.serviceReportCount || 0, icon: 'fa-file-lines', tone: 'pink', target: 'service-reports', badge: todayBadge('报告') },
    { label: '用户偏好', desc: '画像与偏好配置', value: dashboard.value.profileCount || 0, icon: 'fa-id-card', tone: 'slate', target: 'profiles', badge: '个性化基础' },
    { label: '今日新增', desc: '今日新增业务总量', value: dashboard.value.todayNewCount || 0, icon: 'fa-bolt', tone: 'red', target: 'dashboard', badge: dashboard.value.todayNewCount ? `今日 +${dashboard.value.todayNewCount}` : '较昨日持平' }
])

const trendSeries = computed(() => [
    { name: '用户增长', values: normalizeTrend(dashboard.value.userTrend), color: '#2563eb' },
    { name: '订单生成', values: normalizeTrend(dashboard.value.tripPlanTrend), color: '#16a34a' },
    { name: '报告生成', values: normalizeTrend(dashboard.value.reportTrend), color: '#7c3aed' }
])

const maxTrend = computed(() => Math.max(1, ...trendSeries.value.flatMap(item => item.values)))
const trendHasData = computed(() => trendSeries.value.some(item => item.values.some(value => Number(value) > 0)))
const todayText = computed(() => new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', weekday: 'short' }))
const sceneEntries = computed(() => entries(dashboard.value.sceneDistribution))
const preferenceEntries = computed(() => entries(dashboard.value.preferenceDistribution))
const serviceTypeEntries = computed(() => entries(dashboard.value.serviceTypeDistribution))
const recentActivities = computed(() => dashboard.value.recentActivities || [])
const systemStatus = computed(() => dashboard.value.systemStatus || {})

const filteredRows = computed(() => {
    const q = keyword.value.trim().toLowerCase()
    if (!q) return rows.value
    return rows.value.filter(row => JSON.stringify(row).toLowerCase().includes(q))
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / pageSize.value)))
const pagedRows = computed(() => {
    const start = (pageNo.value - 1) * pageSize.value
    return filteredRows.value.slice(start, start + pageSize.value)
})

watch([keyword, pageSize, active], () => {
    pageNo.value = 1
})

async function login() {
    try {
        const result = await adminLogin(loginForm.value)
        sessionStorage.setItem('adminToken', result.data.token)
        loggedIn.value = true
        await loadModule('dashboard')
    } catch (error) {
        alert(error.message)
    }
}

async function loadModule(moduleId = active.value) {
    active.value = moduleId
    loading.value = true
    keyword.value = ''
    pageNo.value = 1
    try {
        dashboardError.value = ''
        if (moduleId === 'dashboard') {
            dashboard.value = (await adminGet('dashboard')).data || {}
            lastRefreshTime.value = nowText()
            rows.value = []
        } else {
            const sourceRows = (await adminGet(resolveAdminPath(moduleId))).data || []
            rows.value = filterModuleRows(moduleId, sourceRows)
        }
    } catch (error) {
        alert(error.message || '数据加载失败')
    } finally {
        loading.value = false
    }
}

function resolveModuleConfigKey(moduleId) {
    if (moduleId === 'refund-applications') return 'refund-applications'
    return orderModuleTypeMap[moduleId] ? 'orders' : moduleId
}

function resolveAdminPath(moduleId) {
    if (moduleId === 'refund-applications') return 'after-sales'
    return orderModuleTypeMap[moduleId] ? 'orders' : moduleId
}

function filterModuleRows(moduleId, sourceRows) {
    if (moduleId === 'refund-applications') {
        return sourceRows.filter(row => String(row.serviceType || '').toUpperCase() === 'REFUND')
    }
    const orderType = orderModuleTypeMap[moduleId]
    if (!orderType) return sourceRows
    return sourceRows.filter(row => String(row.orderType || '').toUpperCase() === orderType)
}

function toggleAdminGroup(group) {
    collapsedGroups.value[group.name] = !collapsedGroups.value[group.name]
}

function isGroupCollapsed(group) {
    return Boolean(collapsedGroups.value[group.name])
}

function canReviewRefund(row) {
    return active.value === 'refund-applications' && String(row?.status || '').toUpperCase() === 'PENDING'
}

async function reviewRefund(row, approved) {
    const payload = {
        ...row,
        status: approved ? 'APPROVED' : 'REJECTED',
        processor: 'admin',
        processRemark: approved ? '已同意退款申请，进入模拟退款流程' : '已拒绝退款申请，请用户补充材料或联系客服'
    }
    try {
        await adminUpdate('after-sales', row.id, payload)
        await loadModule(active.value)
        await refreshDashboardQuietly()
    } catch (error) {
        alert(error.message || '退款申请处理失败')
    }
}

function openCreate() {
    if (!config.value.empty) return
    editor.value = { mode: 'create', data: { ...config.value.empty } }
}

function openEdit(row) {
    editor.value = { mode: 'edit', data: { ...row } }
}

async function saveEditor() {
    if (!editor.value) return
    saving.value = true
    try {
        const payload = normalizePayload(editor.value.data)
        if (editor.value.mode === 'create') {
            await adminCreate(resolveAdminPath(active.value), payload)
        } else {
            await adminUpdate(resolveAdminPath(active.value), payload.id, payload)
        }
        editor.value = null
        await loadModule(active.value)
        await refreshDashboardQuietly()
    } catch (error) {
        alert(error.message || '保存失败')
    } finally {
        saving.value = false
    }
}

async function removeRow(row) {
    if (!confirm(`确定删除「${row.title || row.email || row.id}」吗？删除会同步影响数据库。`)) return
    try {
        await adminDelete(resolveAdminPath(active.value), row.id)
        await loadModule(active.value)
        await refreshDashboardQuietly()
    } catch (error) {
        alert(error.message || '删除失败')
    }
}

async function refreshDashboardQuietly() {
    dashboard.value = (await adminGet('dashboard')).data || dashboard.value
}

function normalizePayload(data) {
    const payload = { ...data }
    for (const key of Object.keys(payload)) {
        if (payload[key] === '') payload[key] = null
    }
    return payload
}

function logout() {
    sessionStorage.removeItem('adminToken')
    loggedIn.value = false
}

function short(value) {
    if (value == null) return ''
    const text = String(value)
    return text.length > 80 ? `${text.slice(0, 80)}...` : text
}

function isLongField(field) {
    return ['content', 'aiResponse', 'responsePayload', 'description', 'recommendReason', 'userMessage', 'reportContent', 'reportSummary'].includes(field)
}

function goPage(delta) {
    pageNo.value = Math.min(totalPages.value, Math.max(1, pageNo.value + delta))
}

function entries(map) {
    return Object.entries(map || {}).sort((a, b) => Number(b[1]) - Number(a[1]))
}

function percent(value, list) {
    const max = Math.max(1, ...list.map(item => Number(item[1]) || 0))
    return `${Math.max(6, Math.round((Number(value) / max) * 100))}%`
}

function trendHeight(value) {
    return `${Math.max(8, Math.round((Number(value) / maxTrend.value) * 100))}%`
}

function normalizeTrend(values) {
    const list = Array.isArray(values) ? values : []
    return Array.from({ length: 7 }, (_, index) => Number(list[index]) || 0)
}

function todayBadge(label) {
    const count = Number(dashboard.value.todayNewCount || 0)
    return count > 0 ? `今日 +${count}` : `${label}稳定`
}

function refreshDashboard() {
    return loadModule('dashboard')
}

function handleMetricClick(card) {
    if (!card.target || card.target === 'dashboard') return
    loadModule(card.target)
}

function nowText() {
    return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function chartPoint(value, index, values) {
    const width = 640
    const height = 220
    const paddingX = 34
    const paddingY = 24
    const step = (width - paddingX * 2) / Math.max(1, values.length - 1)
    const usableHeight = height - paddingY * 2
    const y = height - paddingY - (Number(value) / maxTrend.value) * usableHeight
    return `${paddingX + index * step},${Math.max(paddingY, y)}`
}

function linePoints(values) {
    return values.map((value, index) => chartPoint(value, index, values)).join(' ')
}

function areaPoints(values) {
    if (!values.length) return ''
    return `34,220 ${linePoints(values)} 606,220`
}

function statusValue(key) {
    return systemStatus.value?.[key] || '待接入'
}

function sparkHeight(value, n) {
    return `${18 + ((Number(value) + n * 7) % 34)}px`
}

function displayName(value) {
    const map = {
        TRAVEL_REPORT: '出行服务报告',
        INTERMODAL_PLAN: '联程出行规划',
        PASSENGER_LUGGAGE: '旅客行李服务',
        REALTIME_ALERT: '实时出行提醒',
        SPECIAL_SCENE: '特殊场景定制',
        AFTER_SALES: '智能售后协助',
        PAYMENT_CREDIT: '支付信用服务',
        PROACTIVE_LOOP: '主动闭环服务',
        SMART_SERVICE: '一站式出行服务',
        AI_CHAT: '智能咨询',
        USER_SAVE: '用户保存',
        ADMIN: '管理员录入'
    }
    return map[value] || value || '未分类'
}

onMounted(() => {
    if (loggedIn.value) loadModule('dashboard')
})
</script>

<template>
    <div v-if="!loggedIn" class="admin-login">
        <form class="login-card" @submit.prevent="login">
            <img class="login-mark" :src="logoUrl" alt="七维智行" />
            <h1>七维智行管理端</h1>
            <p>运营数据、用户方案和服务记录统一管理</p>
            <label>管理员账号</label>
            <input v-model="loginForm.username" autocomplete="username" />
            <label>密码</label>
            <input v-model="loginForm.password" type="password" autocomplete="current-password" />
            <button type="submit">登录管理后台</button>
            <span>默认账号：admin / admin123</span>
        </form>
    </div>

    <div v-else class="admin-layout">
        <aside class="admin-sidebar">
            <div class="brand">
                <img class="brand-icon" :src="logoUrl" alt="七维智行" />
                <div>
                    <strong>七维智行 Admin</strong>
                    <small>AI 智慧出行全过程服务平台</small>
                </div>
            </div>
            <div class="admin-nav-scroll">
                <section v-for="group in moduleGroups" :key="group.name" class="admin-nav-group">
                    <button type="button" class="admin-group-trigger" :class="{ collapsed: isGroupCollapsed(group) }" @click="toggleAdminGroup(group)">
                        <span>{{ group.name }}</span>
                        <i class="fa-solid fa-chevron-down"></i>
                    </button>
                    <button v-for="item in modulesByIds(group.ids)" v-show="!isGroupCollapsed(group)" :key="item.id" class="admin-module-item" :class="{ active: active === item.id }" @click="loadModule(item.id)">
                        <i :class="['fa-solid', item.icon]"></i>
                        <span>{{ item.name }}</span>
                    </button>
                </section>
            </div>
            <button class="logout" @click="logout">
                <i class="fa-solid fa-right-from-bracket"></i>
                <span>退出登录</span>
            </button>
        </aside>

        <main class="admin-main">
            <header class="admin-header">
                <div>
                    <p class="eyebrow">智能出行数据中心</p>
                    <h1>{{ currentModule?.name }}</h1>
                    <span v-if="active !== 'dashboard'">共 {{ filteredRows.length }} 条记录，当前第 {{ pageNo }} 页</span>
                </div>
                <button class="ghost" @click="loadModule(active)">
                    <i class="fa-solid fa-rotate"></i>
                    刷新数据
                </button>
            </header>

            <section v-if="active === 'dashboard'" class="dashboard cockpit-dashboard">
                <div class="cockpit-hero">
                    <div>
                        <p class="hero-kicker">运营总览</p>
                        <h2>智能出行数据中心</h2>
                        <span>实时汇总用户、方案、咨询、路线、报告等核心业务数据</span>
                    </div>
                    <div class="hero-actions">
                        <div class="time-panel">
                            <span>今日日期</span>
                            <strong>{{ todayText }}</strong>
                        </div>
                        <div class="time-panel">
                            <span>数据更新时间</span>
                            <strong>{{ lastRefreshTime || '待刷新' }}</strong>
                        </div>
                        <button class="refresh-btn" :disabled="loading" @click="refreshDashboard">
                            <i :class="['fa-solid', loading ? 'fa-spinner fa-spin' : 'fa-rotate']"></i>
                            <span>{{ loading ? '刷新中' : '刷新数据' }}</span>
                        </button>
                        <div class="admin-chip"><i class="fa-solid fa-user-shield"></i><span>管理员</span></div>
                    </div>
                </div>

                <div v-if="dashboardError" class="dashboard-error">{{ dashboardError }}</div>

                <div v-if="loading" class="dashboard-skeleton">
                    <i v-for="item in 8" :key="item"></i>
                </div>

                <template v-else>
                    <div class="kpi-grid">
                        <article
                            v-for="card in metricCards"
                            :key="card.label"
                            :class="['kpi-card', card.tone]"
                            @click="handleMetricClick(card)"
                        >
                            <div class="kpi-top">
                                <div class="kpi-icon"><i :class="['fa-solid', card.icon]"></i></div>
                                <span class="kpi-badge">{{ card.badge }}</span>
                            </div>
                            <div class="kpi-body">
                                <span>{{ card.label }}</span>
                                <strong>{{ card.value }}</strong>
                                <p>{{ card.desc }}</p>
                            </div>
                            <div class="sparkline" aria-hidden="true">
                                <i v-for="n in 7" :key="n" :style="{ height: sparkHeight(card.value, n) }"></i>
                            </div>
                        </article>
                    </div>

                    <div class="cockpit-grid">
                        <article class="data-card trend-card">
                            <div class="data-card-head">
                                <div>
                                    <h2>最近 7 天业务趋势</h2>
                                    <p>用户增长、方案生成、报告生成，全部来自数据库统计</p>
                                </div>
                                <div class="legend-list">
                                    <span v-for="series in trendSeries" :key="series.name"><i :style="{ background: series.color }"></i>{{ series.name }}</span>
                                </div>
                            </div>
                            <div v-if="!trendHasData" class="empty-state modern">暂无趋势数据</div>
                            <div v-else class="svg-chart-wrap">
                                <svg viewBox="0 0 640 250" preserveAspectRatio="none" class="line-chart">
                                    <defs>
                                        <linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="0" stop-color="#2563eb" stop-opacity="0.18" />
                                            <stop offset="1" stop-color="#2563eb" stop-opacity="0.02" />
                                        </linearGradient>
                                    </defs>
                                    <line v-for="y in [40, 90, 140, 190, 220]" :key="y" x1="34" :y1="y" x2="606" :y2="y" class="grid-line" />
                                    <polygon v-if="trendSeries[0]" :points="areaPoints(trendSeries[0].values)" fill="url(#trendFill)" />
                                    <polyline
                                        v-for="series in trendSeries"
                                        :key="series.name"
                                        :points="linePoints(series.values)"
                                        :stroke="series.color"
                                        class="trend-line"
                                    />
                                    <g v-for="series in trendSeries" :key="series.name + '-dots'">
                                        <circle
                                            v-for="(value, index) in series.values"
                                            :key="series.name + index"
                                            :cx="chartPoint(value, index, series.values).split(',')[0]"
                                            :cy="chartPoint(value, index, series.values).split(',')[1]"
                                            r="4"
                                            :fill="series.color"
                                        >
                                            <title>{{ series.name }}：{{ value }}</title>
                                        </circle>
                                    </g>
                                </svg>
                                <div class="chart-axis">
                                    <span v-for="day in dashboard.last7Days || []" :key="day">{{ day }}</span>
                                </div>
                            </div>
                        </article>

                        <article class="data-card distribution-card">
                            <div class="data-card-head compact"><h2>出行场景分布</h2><small>待后端提供 sceneDistribution 字段</small></div>
                            <div v-if="!sceneEntries.length" class="empty-state modern">暂无场景数据</div>
                            <div v-else class="donut-layout">
                                <div class="donut-ring"><span>{{ sceneEntries.length }}</span><small>场景</small></div>
                                <div class="progress-list">
                                    <div v-for="item in sceneEntries" :key="item[0]" class="progress-row">
                                        <div><span>{{ displayName(item[0]) }}</span><b>{{ item[1] }}</b></div>
                                        <i><em :style="{ width: percent(item[1], sceneEntries) }"></em></i>
                                    </div>
                                </div>
                            </div>
                        </article>

                        <article class="data-card">
                            <div class="data-card-head compact"><h2>偏好类型分布</h2><small>来自用户方案与画像字段</small></div>
                            <div v-if="!preferenceEntries.length" class="empty-state modern">暂无偏好数据</div>
                            <div v-else class="progress-list">
                                <div v-for="item in preferenceEntries" :key="item[0]" class="progress-row">
                                    <div><span>{{ displayName(item[0]) }}</span><b>{{ item[1] }}</b></div>
                                    <i><em :style="{ width: percent(item[1], preferenceEntries) }"></em></i>
                                </div>
                            </div>
                        </article>

                        <article class="data-card">
                            <div class="data-card-head compact"><h2>服务类型分布</h2><small>来自一站式服务记录</small></div>
                            <div v-if="!serviceTypeEntries.length" class="empty-state modern">暂无服务数据</div>
                            <div v-else class="progress-list service">
                                <div v-for="item in serviceTypeEntries" :key="item[0]" class="progress-row">
                                    <div><span>{{ displayName(item[0]) }}</span><b>{{ item[1] }}</b></div>
                                    <i><em :style="{ width: percent(item[1], serviceTypeEntries) }"></em></i>
                                </div>
                            </div>
                        </article>

                        <article class="data-card activity-card">
                            <div class="data-card-head compact"><h2>最近业务动态</h2><small>需后端提供 recentActivities</small></div>
                            <div v-if="!recentActivities.length" class="empty-state modern">暂无最近动态</div>
                            <div v-else class="activity-list">
                                <div v-for="item in recentActivities.slice(0, 8)" :key="item.id || item.time || item.title" class="activity-item">
                                    <i class="fa-solid fa-circle-dot"></i>
                                    <div><strong>{{ item.title }}</strong><span>{{ item.summary || item.description }}</span></div>
                                    <em>{{ item.time || item.createTime }}</em>
                                </div>
                            </div>
                        </article>

                        <article class="data-card status-card">
                            <div class="data-card-head compact"><h2>数据质量与系统状态</h2><small>未接入真实状态接口时显示待接入</small></div>
                            <div class="status-grid">
                                <div><span>数据库连接</span><strong>{{ statusValue('database') }}</strong></div>
                                <div><span>AI 服务</span><strong>{{ statusValue('ai') }}</strong></div>
                                <div><span>地图服务</span><strong>{{ statusValue('amap') }}</strong></div>
                                <div><span>今日接口调用</span><strong>{{ statusValue('apiCallsToday') }}</strong></div>
                                <div><span>最近刷新时间</span><strong>{{ lastRefreshTime || '待刷新' }}</strong></div>
                            </div>
                        </article>

                        <article class="data-card rank-card">
                            <div class="data-card-head compact"><h2>订单出发地 Top5</h2><small>来自用户实时订单</small></div>
                            <div v-if="!entries(dashboard.popularStartPlaces).length" class="empty-state modern">暂无订单数据</div>
                            <div v-for="item in entries(dashboard.popularStartPlaces).slice(0, 5)" :key="item[0]" class="rank-line modern"><span>{{ item[0] }}</span><b>{{ item[1] }}</b></div>
                        </article>

                        <article class="data-card rank-card">
                            <div class="data-card-head compact"><h2>旅游城市订单 Top5</h2><small>来自用户实时订单</small></div>
                            <div v-if="!entries(dashboard.popularEndPlaces).length" class="empty-state modern">暂无订单数据</div>
                            <div v-for="item in entries(dashboard.popularEndPlaces).slice(0, 5)" :key="item[0]" class="rank-line modern"><span>{{ item[0] }}</span><b>{{ item[1] }}</b></div>
                        </article>
                    </div>
                </template>
            </section>

            <section v-else class="table-card">
                <div class="table-toolbar">
                    <input v-model="keyword" placeholder="搜索当前列表" />
                    <div class="toolbar-actions">
                        <select v-model.number="pageSize">
                            <option :value="10">10 条/页</option>
                            <option :value="20">20 条/页</option>
                            <option :value="50">50 条/页</option>
                        </select>
                        <button @click="loadModule(active)">刷新</button>
                        <button v-if="config.empty" class="primary" @click="openCreate">新增记录</button>
                    </div>
                </div>

                <div v-if="loading" class="loading">正在加载真实数据库数据...</div>
                <table v-else>
                    <thead>
                        <tr>
                            <th v-for="col in columns" :key="col">{{ columnLabels[col] || col }}</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="row in pagedRows" :key="row.id">
                            <td v-for="col in columns" :key="col" :title="row[col]">{{ short(row[col]) }}</td>
                            <td class="actions">
                                <button v-if="canReviewRefund(row)" class="success" @click="reviewRefund(row, true)">同意退款</button>
                                <button v-if="canReviewRefund(row)" class="warning" @click="reviewRefund(row, false)">拒绝退款</button>
                                <button v-if="editableFields.length" @click="openEdit(row)">编辑</button>
                                <button class="danger" @click="removeRow(row)">删除</button>
                            </td>
                        </tr>
                        <tr v-if="!pagedRows.length">
                            <td :colspan="columns.length + 1" class="empty-cell">暂无数据</td>
                        </tr>
                    </tbody>
                </table>

                <div class="pagination">
                    <button :disabled="pageNo === 1" @click="goPage(-1)">上一页</button>
                    <span>第 {{ pageNo }} / {{ totalPages }} 页</span>
                    <button :disabled="pageNo === totalPages" @click="goPage(1)">下一页</button>
                </div>
            </section>
        </main>

        <div v-if="editor" class="modal-mask" @click.self="editor = null">
            <form class="modal" @submit.prevent="saveEditor">
                <h2>{{ editor.mode === 'create' ? '新增记录' : '编辑记录' }}</h2>
                <div v-for="field in editableFields" :key="field" class="field">
                    <label>{{ columnLabels[field] || field }}</label>
                    <textarea v-if="isLongField(field)" v-model="editor.data[field]"></textarea>
                    <input v-else v-model="editor.data[field]" />
                </div>
                <div class="modal-actions">
                    <button type="button" @click="editor = null">取消</button>
                    <button type="submit" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
                </div>
            </form>
        </div>
    </div>
</template>

<style scoped>
.admin-login,
.admin-layout {
    min-height: 100vh;
    background: #f3f6fb;
    color: #111827;
}

.admin-login {
    display: grid;
    place-items: center;
    background:
        radial-gradient(circle at 18% 18%, rgba(37, 99, 235, 0.14), transparent 28%),
        radial-gradient(circle at 84% 16%, rgba(20, 184, 166, 0.12), transparent 26%),
        #f3f6fb;
}

.login-card {
    width: 390px;
    background: rgba(255, 255, 255, 0.92);
    border: 1px solid #e5e7eb;
    border-radius: 18px;
    padding: 30px;
    box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
}

.login-mark {
    width: 48px;
    height: 48px;
    border-radius: 14px;
    display: block;
    object-fit: cover;
    box-shadow: 0 10px 24px rgba(37, 99, 235, 0.18);
}

.login-card h1 {
    font-size: 26px;
    margin: 18px 0 6px;
}

.login-card p,
.login-card span,
.admin-header span,
.chart-card p {
    color: #64748b;
}

label {
    display: block;
    margin: 16px 0 8px;
    font-size: 13px;
    color: #475569;
    font-weight: 700;
}

input,
select,
textarea {
    padding: 11px 12px;
    border: 1px solid #d7dde8;
    border-radius: 10px;
    background: #fff;
    font-family: inherit;
    outline: none;
}

input:focus,
select:focus,
textarea:focus {
    border-color: #2563eb;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

textarea {
    min-height: 120px;
    resize: vertical;
}

.login-card input {
    width: 100%;
}

.login-card button,
.modal-actions button[type="submit"],
.table-toolbar .primary {
    border: none;
    color: #fff;
    background: #2563eb;
}

.login-card button {
    width: 100%;
    margin-top: 22px;
    padding: 12px;
    border-radius: 10px;
    cursor: pointer;
}

.login-card span {
    display: block;
    margin-top: 12px;
    font-size: 12px;
}

.admin-layout {
    display: flex;
    height: 100vh;
    overflow: hidden;
}

.admin-sidebar {
    width: 252px;
    height: 100vh;
    min-height: 0;
    background: #0f172a;
    color: #fff;
    padding: 18px 14px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    box-shadow: 12px 0 30px rgba(15, 23, 42, 0.16);
}

.admin-nav-scroll {
    min-height: 0;
    flex: 1;
    overflow-y: auto;
    padding-right: 3px;
}

.admin-nav-scroll::-webkit-scrollbar {
    width: 6px;
}

.admin-nav-scroll::-webkit-scrollbar-thumb {
    background: rgba(148, 163, 184, 0.45);
    border-radius: 999px;
}

.admin-nav-group {
    display: grid;
    gap: 6px;
    margin-bottom: 14px;
}

.admin-group-trigger {
    height: 34px;
    margin: 4px 2px 2px;
    padding: 0 10px;
    justify-content: space-between;
    color: #cbd5e1;
    background: rgba(15, 23, 42, 0.78);
    border: 1px solid rgba(148, 163, 184, 0.18);
    font-size: 12px;
    font-weight: 900;
}

.admin-group-trigger i {
    margin-left: auto;
    transition: transform .18s ease;
}

.admin-group-trigger.collapsed i {
    transform: rotate(-90deg);
}

.admin-module-item {
    margin-left: 8px;
}

.brand {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 10px 22px;
}

.brand-icon {
    width: 42px;
    height: 42px;
    border-radius: 13px;
    display: block;
    object-fit: cover;
    box-shadow: 0 8px 18px rgba(37, 99, 235, 0.16);
}

.brand strong,
.brand small {
    display: block;
}

.brand small {
    margin-top: 3px;
    color: #94a3b8;
}

.admin-sidebar button {
    height: 42px;
    display: flex;
    align-items: center;
    gap: 11px;
    padding: 0 12px;
    border: none;
    border-radius: 11px;
    background: transparent;
    color: #cbd5e1;
    cursor: pointer;
    text-align: left;
    font-weight: 650;
}

.admin-sidebar button.active,
.admin-sidebar button:hover {
    background: rgba(37, 99, 235, 0.95);
    color: #fff;
}

.logout {
    margin-top: auto;
}

.admin-main {
    flex: 1;
    min-width: 0;
    height: 100vh;
    overflow-y: auto;
    overflow-x: hidden;
    padding: 24px;
    scroll-behavior: smooth;
}

.admin-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 22px;
}

.eyebrow {
    margin: 0 0 5px;
    color: #2563eb;
    font-weight: 800;
    letter-spacing: 0.04em;
}

.admin-header h1 {
    margin: 0 0 6px;
    font-size: 28px;
}

.ghost {
    border: 1px solid #d7dde8;
    background: #fff;
    color: #1f2937;
    border-radius: 10px;
    padding: 10px 14px;
    cursor: pointer;
}

.dashboard {
    display: flex;
    flex-direction: column;
    gap: 18px;
}

.metric-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
}

.metric,
.chart-card,
.table-card {
    background: #fff;
    border: 1px solid #e5e7eb;
    border-radius: 16px;
    box-shadow: 0 14px 32px rgba(15, 23, 42, 0.06);
}

.metric {
    padding: 18px;
    position: relative;
    overflow: hidden;
}

.metric::after {
    content: "";
    position: absolute;
    right: -30px;
    top: -35px;
    width: 96px;
    height: 96px;
    border-radius: 50%;
    background: rgba(37, 99, 235, 0.1);
}

.metric-icon {
    width: 38px;
    height: 38px;
    border-radius: 12px;
    display: grid;
    place-items: center;
    color: #fff;
    background: #2563eb;
}

.metric.green .metric-icon { background: #16a34a; }
.metric.violet .metric-icon { background: #7c3aed; }
.metric.cyan .metric-icon { background: #0891b2; }
.metric.orange .metric-icon { background: #ea580c; }
.metric.pink .metric-icon { background: #db2777; }
.metric.slate .metric-icon { background: #475569; }
.metric.red .metric-icon { background: #dc2626; }

.metric span {
    display: block;
    margin-top: 14px;
    color: #64748b;
    font-weight: 700;
}

.metric strong {
    display: block;
    margin-top: 8px;
    font-size: 34px;
}

.chart-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
}

.chart-card {
    padding: 20px;
}

.chart-card.wide {
    grid-column: span 2;
}

.chart-card h2 {
    margin: 0 0 12px;
    font-size: 18px;
}

.chart-title {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 14px;
}

.chart-title p {
    margin: 4px 0 0;
}

.trend-chart {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.trend-row,
.trend-labels {
    display: grid;
    grid-template-columns: 90px 1fr;
    align-items: end;
    gap: 14px;
}

.trend-name {
    font-size: 13px;
    font-weight: 800;
}

.trend-bars {
    height: 112px;
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 10px;
    align-items: end;
    border-bottom: 1px solid #e5e7eb;
}

.trend-cell {
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
    align-items: center;
    gap: 5px;
}

.trend-cell i {
    width: 100%;
    max-width: 34px;
    border-radius: 10px 10px 0 0;
    opacity: 0.86;
}

.trend-value {
    font-size: 12px;
    color: #64748b;
}

.trend-labels b {
    font-size: 12px;
    color: #64748b;
    text-align: center;
}

.trend-labels {
    grid-template-columns: 90px repeat(7, 1fr);
}

.bar-line {
    margin-top: 12px;
}

.bar-line div,
.rank-line {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    color: #334155;
    font-weight: 700;
}

.bar-line i {
    display: block;
    height: 10px;
    margin-top: 7px;
    border-radius: 999px;
    background: linear-gradient(90deg, #2563eb, #06b6d4);
}

.bar-line.accent i {
    background: linear-gradient(90deg, #16a34a, #f59e0b);
}

.rank-line {
    padding: 12px 0;
    border-bottom: 1px solid #edf2f7;
}

.rank-line:last-child {
    border-bottom: 0;
}

.empty-state {
    padding: 24px;
    border-radius: 12px;
    background: #f8fafc;
    color: #64748b;
    text-align: center;
}

.table-card {
    overflow-x: auto;
}

.table-toolbar {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    padding: 14px;
    border-bottom: 1px solid #edf2f7;
}

.table-toolbar input {
    width: 320px;
}

.toolbar-actions {
    display: flex;
    gap: 8px;
}

.toolbar-actions button {
    padding: 0 14px;
    border: 1px solid #d7dde8;
    border-radius: 10px;
    background: #fff;
    cursor: pointer;
}

table {
    width: 100%;
    border-collapse: collapse;
    min-width: 980px;
}

th,
td {
    padding: 13px 14px;
    border-bottom: 1px solid #edf2f7;
    text-align: left;
    font-size: 13px;
    vertical-align: top;
}

th {
    background: #f8fafc;
    color: #475569;
}

.actions {
    white-space: nowrap;
}

.actions button,
.pagination button {
    border: 1px solid #d7dde8;
    background: #fff;
    border-radius: 8px;
    padding: 7px 10px;
    cursor: pointer;
    margin-right: 6px;
}

.actions .danger {
    color: #b42318;
    border-color: #fecaca;
}

.actions .success {
    color: #047857;
    border-color: #a7f3d0;
    background: #ecfdf5;
}

.actions .warning {
    color: #b45309;
    border-color: #fed7aa;
    background: #fff7ed;
}

.pagination {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 12px;
    padding: 14px;
}

.pagination button:disabled {
    opacity: 0.45;
    cursor: not-allowed;
}

.loading,
.empty-cell {
    padding: 44px;
    color: #64748b;
    text-align: center;
}

.modal-mask {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.42);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 100;
}

.modal {
    width: 620px;
    max-height: 84vh;
    overflow-y: auto;
    background: #fff;
    border-radius: 16px;
    padding: 24px;
    box-shadow: 0 28px 80px rgba(15, 23, 42, 0.24);
}

.field input,
.field textarea {
    width: 100%;
}

.modal-actions {
    display: flex;
    gap: 10px;
    margin-top: 20px;
}

.modal-actions button {
    flex: 1;
    padding: 11px;
    border-radius: 10px;
    border: 1px solid #d7dde8;
    cursor: pointer;
}

@media (max-width: 1180px) {
    .metric-grid,
    .chart-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 860px) {
    .admin-layout {
        flex-direction: column;
    }

    .admin-sidebar {
        width: auto;
        min-height: auto;
        flex-direction: row;
        overflow-x: auto;
    }

    .brand,
    .logout {
        display: none;
    }

    .metric-grid,
    .chart-grid {
        grid-template-columns: 1fr;
    }

    .chart-card.wide {
        grid-column: span 1;
    }

    .table-toolbar,
    .admin-header {
        flex-direction: column;
        align-items: stretch;
    }
}


/* Enterprise cockpit dashboard */
.admin-main {
    background: linear-gradient(180deg, #f5f8ff 0%, #eef4fb 100%);
}

.cockpit-dashboard {
    gap: 18px;
}

.cockpit-hero {
    min-height: 132px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 22px;
    padding: 26px 28px;
    border: 1px solid rgba(148, 163, 184, 0.2);
    border-radius: 22px;
    background:
        linear-gradient(135deg, rgba(37, 99, 235, 0.11), rgba(6, 182, 212, 0.08)),
        rgba(255, 255, 255, 0.88);
    box-shadow: 0 18px 48px rgba(30, 64, 175, 0.08);
}

.hero-kicker {
    margin: 0 0 8px;
    color: #2563eb;
    font-size: 13px;
    font-weight: 800;
}

.cockpit-hero h2 {
    margin: 0;
    color: #0f172a;
    font-size: 30px;
    line-height: 1.2;
}

.cockpit-hero span {
    display: block;
    margin-top: 8px;
    color: #64748b;
    font-size: 14px;
}

.hero-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    justify-content: flex-end;
}

.time-panel,
.admin-chip,
.refresh-btn {
    min-height: 48px;
    border-radius: 14px;
    border: 1px solid rgba(148, 163, 184, 0.22);
    background: rgba(255, 255, 255, 0.78);
    box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.time-panel {
    padding: 8px 12px;
}

.time-panel span {
    margin: 0;
    color: #94a3b8;
    font-size: 12px;
}

.time-panel strong {
    display: block;
    margin-top: 3px;
    color: #0f172a;
    font-size: 14px;
}

.refresh-btn,
.admin-chip {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 0 14px;
    color: #1d4ed8;
    font-weight: 800;
}

.refresh-btn {
    cursor: pointer;
}

.refresh-btn:disabled {
    opacity: 0.65;
    cursor: not-allowed;
}

.dashboard-error {
    padding: 12px 14px;
    border-radius: 14px;
    border: 1px solid #fecaca;
    background: #fff1f2;
    color: #b91c1c;
}

.dashboard-skeleton,
.kpi-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
}

.dashboard-skeleton i {
    height: 158px;
    border-radius: 18px;
    background: linear-gradient(90deg, #e8eef7, #f8fafc, #e8eef7);
    background-size: 240% 100%;
    animation: skeletonMove 1.2s infinite linear;
}

@keyframes skeletonMove {
    from { background-position: 0 0; }
    to { background-position: -240% 0; }
}

.kpi-card,
.data-card {
    border: 1px solid rgba(148, 163, 184, 0.18);
    border-radius: 18px;
    background: rgba(255, 255, 255, 0.92);
    box-shadow: 0 16px 38px rgba(15, 23, 42, 0.06);
    transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease;
}

.kpi-card:hover,
.data-card:hover {
    transform: translateY(-2px);
    border-color: rgba(37, 99, 235, 0.18);
    box-shadow: 0 22px 48px rgba(30, 64, 175, 0.1);
}

.kpi-card {
    position: relative;
    min-height: 158px;
    padding: 18px;
    overflow: hidden;
    cursor: pointer;
}

.kpi-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.kpi-icon {
    width: 46px;
    height: 46px;
    display: grid;
    place-items: center;
    border-radius: 14px;
    color: #fff;
    background: linear-gradient(135deg, #2563eb, #06b6d4);
    box-shadow: 0 12px 26px rgba(37, 99, 235, 0.22);
}

.kpi-card.green .kpi-icon { background: linear-gradient(135deg, #16a34a, #22c55e); }
.kpi-card.violet .kpi-icon { background: linear-gradient(135deg, #7c3aed, #a855f7); }
.kpi-card.cyan .kpi-icon { background: linear-gradient(135deg, #0891b2, #06b6d4); }
.kpi-card.orange .kpi-icon { background: linear-gradient(135deg, #ea580c, #f59e0b); }
.kpi-card.pink .kpi-icon { background: linear-gradient(135deg, #db2777, #f472b6); }
.kpi-card.slate .kpi-icon { background: linear-gradient(135deg, #475569, #64748b); }
.kpi-card.red .kpi-icon { background: linear-gradient(135deg, #dc2626, #fb7185); }

.kpi-badge {
    padding: 5px 9px;
    border-radius: 999px;
    background: #eef6ff;
    color: #2563eb;
    font-size: 12px;
    font-weight: 800;
}

.kpi-body span {
    display: block;
    margin-top: 18px;
    color: #64748b;
    font-size: 15px;
    font-weight: 800;
}

.kpi-body strong {
    display: block;
    margin-top: 6px;
    color: #0f172a;
    font-size: 38px;
    line-height: 1;
    letter-spacing: 0;
}

.kpi-body p {
    margin: 8px 0 0;
    color: #94a3b8;
    font-size: 13px;
}

.sparkline {
    position: absolute;
    right: 16px;
    bottom: 14px;
    display: flex;
    align-items: flex-end;
    gap: 3px;
    opacity: 0.32;
}

.sparkline i {
    width: 5px;
    border-radius: 999px;
    background: #2563eb;
}

.cockpit-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
}

.data-card {
    min-height: 260px;
    padding: 20px;
}

.trend-card {
    grid-column: span 4;
    min-height: 382px;
}

.activity-card,
.status-card {
    grid-column: span 2;
}

.data-card-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;
}

.data-card-head.compact {
    align-items: center;
}

.data-card-head h2 {
    margin: 0;
    color: #0f172a;
    font-size: 18px;
}

.data-card-head p,
.data-card-head small {
    margin: 5px 0 0;
    color: #64748b;
    font-size: 13px;
}

.legend-list {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
}

.legend-list span {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: #475569;
    font-size: 13px;
    font-weight: 700;
}

.legend-list i {
    width: 9px;
    height: 9px;
    border-radius: 50%;
}

.svg-chart-wrap {
    min-height: 286px;
}

.line-chart {
    width: 100%;
    height: 250px;
}

.grid-line {
    stroke: #e5edf7;
    stroke-width: 1;
}

.trend-line {
    fill: none;
    stroke-width: 3.5;
    stroke-linecap: round;
    stroke-linejoin: round;
}

.chart-axis {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 8px;
    padding: 0 28px;
}

.chart-axis span {
    color: #64748b;
    font-size: 12px;
    text-align: center;
}

.empty-state.modern {
    display: grid;
    place-items: center;
    min-height: 170px;
    border-radius: 16px;
    border: 1px dashed #cbd5e1;
    background: #f8fbff;
    color: #64748b;
    font-weight: 700;
}

.donut-layout {
    display: grid;
    grid-template-columns: 128px 1fr;
    gap: 18px;
    align-items: center;
}

.donut-ring {
    width: 118px;
    height: 118px;
    display: grid;
    place-items: center;
    border-radius: 50%;
    background: conic-gradient(#2563eb 0 38%, #06b6d4 38% 62%, #16a34a 62% 78%, #e5edf7 78% 100%);
    position: relative;
}

.donut-ring::after {
    content: "";
    position: absolute;
    inset: 18px;
    border-radius: 50%;
    background: #fff;
}

.donut-ring span,
.donut-ring small {
    position: relative;
    z-index: 1;
    text-align: center;
}

.donut-ring span {
    font-size: 28px;
    color: #0f172a;
    font-weight: 900;
}

.donut-ring small {
    margin-top: 28px;
    color: #64748b;
    font-size: 12px;
}

.progress-list {
    display: flex;
    flex-direction: column;
    gap: 13px;
}

.progress-row div {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    color: #334155;
    font-size: 13px;
    font-weight: 800;
}

.progress-row i {
    display: block;
    height: 9px;
    margin-top: 7px;
    overflow: hidden;
    border-radius: 999px;
    background: #edf2f7;
}

.progress-row em {
    display: block;
    height: 100%;
    border-radius: inherit;
    background: linear-gradient(90deg, #2563eb, #06b6d4);
}

.progress-list.service em {
    background: linear-gradient(90deg, #16a34a, #f59e0b);
}

.activity-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.activity-item {
    display: grid;
    grid-template-columns: 22px 1fr auto;
    gap: 10px;
    align-items: center;
    padding: 11px 12px;
    border-radius: 14px;
    background: #f8fbff;
}

.activity-item i { color: #2563eb; }
.activity-item strong { display: block; color: #0f172a; }
.activity-item span { display: block; margin-top: 3px; color: #64748b; font-size: 12px; }
.activity-item em { color: #94a3b8; font-size: 12px; font-style: normal; }

.status-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
}

.status-grid div {
    min-height: 74px;
    padding: 13px;
    border-radius: 14px;
    background: #f8fbff;
    border: 1px solid #edf2f7;
}

.status-grid span {
    display: block;
    color: #64748b;
    font-size: 12px;
    font-weight: 800;
}

.status-grid strong {
    display: block;
    margin-top: 8px;
    color: #0f172a;
    font-size: 18px;
}

.rank-line.modern {
    padding: 13px 0;
    border-bottom: 1px solid #edf2f7;
}

.rank-line.modern span { color: #334155; }
.rank-line.modern b { color: #2563eb; }

@media (max-width: 1280px) {
    .kpi-grid,
    .dashboard-skeleton {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .cockpit-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .trend-card,
    .activity-card,
    .status-card {
        grid-column: span 2;
    }
}

@media (max-width: 760px) {
    .cockpit-hero,
    .admin-header {
        flex-direction: column;
        align-items: stretch;
    }
    .kpi-grid,
    .dashboard-skeleton,
    .cockpit-grid {
        grid-template-columns: 1fr;
    }
    .trend-card,
    .activity-card,
    .status-card {
        grid-column: span 1;
    }
    .donut-layout,
    .status-grid {
        grid-template-columns: 1fr;
    }
}

</style>
