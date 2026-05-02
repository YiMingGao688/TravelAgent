<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { adminCreate, adminDelete, adminGet, adminLogin, adminUpdate } from '../../utils/api.js'

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

const modules = [
    { id: 'dashboard', name: '数据概览', icon: 'fa-chart-line' },
    { id: 'users', name: '用户管理', icon: 'fa-users' },
    { id: 'trip-plans', name: '方案管理', icon: 'fa-map' },
    { id: 'chat-records', name: '咨询记录', icon: 'fa-comments' },
    { id: 'profiles', name: '偏好管理', icon: 'fa-id-card' },
    { id: 'routes', name: '路线记录', icon: 'fa-route' },
    { id: 'smart-services', name: '服务记录', icon: 'fa-clipboard-check' }
]

const moduleConfig = {
    users: {
        columns: ['id', 'email', 'password', 'createTime', 'updateTime'],
        editable: ['email', 'password'],
        empty: { email: '', password: '123456' }
    },
    'trip-plans': {
        columns: ['id', 'userId', 'title', 'startPlace', 'endPlace', 'sourceType', 'createTime'],
        editable: ['userId', 'title', 'startPlace', 'endPlace', 'sourceType', 'travelPreference', 'budget', 'days', 'content'],
        empty: { userId: '', title: '', startPlace: '', endPlace: '', sourceType: 'ADMIN', travelPreference: '', budget: '', days: 1, content: '' }
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
    }
}

const columnLabels = {
    id: 'ID',
    email: '邮箱',
    password: '密码',
    createTime: '创建时间',
    updateTime: '更新时间',
    userId: '用户',
    title: '标题',
    startPlace: '起点',
    endPlace: '终点',
    sourceType: '来源',
    sessionId: '会话',
    userMessage: '用户问题',
    aiResponse: 'AI 回复',
    model: '模型',
    city: '常驻城市',
    transport: '交通偏好',
    style: '出行偏好',
    note: '备注',
    updatedAt: '更新时间',
    startPoint: '起点',
    endPoint: '终点',
    routeType: '路线类型',
    routeName: '路线',
    distance: '距离',
    duration: '耗时',
    cost: '费用',
    description: '说明',
    recommendReason: '推荐理由',
    preference: '偏好',
    serviceType: '服务类型',
    status: '状态',
    responsePayload: '方案内容',
    content: '方案内容',
    budget: '预算',
    days: '天数',
    travelPreference: '出行偏好'
}

const currentModule = computed(() => modules.find(item => item.id === active.value))
const config = computed(() => moduleConfig[active.value] || { columns: [], editable: [], empty: null })
const columns = computed(() => config.value.columns)
const editableFields = computed(() => config.value.editable)

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
        if (moduleId === 'dashboard') {
            dashboard.value = (await adminGet('dashboard')).data || {}
            rows.value = []
        } else {
            rows.value = (await adminGet(moduleId)).data || []
        }
    } finally {
        loading.value = false
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
            await adminCreate(active.value, payload)
        } else {
            await adminUpdate(active.value, payload.id, payload)
        }
        editor.value = null
        await loadModule(active.value)
        await refreshDashboardQuietly()
    } catch (error) {
        alert(error.message)
    } finally {
        saving.value = false
    }
}

async function removeRow(row) {
    if (!confirm('确定删除这条记录吗？删除后会同步影响数据库。')) return
    await adminDelete(active.value, row.id)
    rows.value = rows.value.filter(item => item.id !== row.id)
    await refreshDashboardQuietly()
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
    return ['content', 'aiResponse', 'responsePayload', 'description', 'recommendReason', 'userMessage'].includes(field)
}

function goPage(delta) {
    pageNo.value = Math.min(totalPages.value, Math.max(1, pageNo.value + delta))
}

onMounted(() => {
    if (loggedIn.value) loadModule('dashboard')
})
</script>

<template>
    <div v-if="!loggedIn" class="admin-login">
        <form class="login-card" @submit.prevent="login">
            <h1>管理端登录</h1>
            <p>出行服务运营后台</p>
            <label>管理员账号</label>
            <input v-model="loginForm.username" />
            <label>密码</label>
            <input v-model="loginForm.password" type="password" />
            <button type="submit">登录</button>
            <span>默认账号：admin / admin123</span>
        </form>
    </div>

    <div v-else class="admin-layout">
        <aside>
            <div class="brand">
                <i class="fa-solid fa-plane-departure"></i>
                <span>Travel Admin</span>
            </div>
            <button v-for="item in modules" :key="item.id" :class="{ active: active === item.id }" @click="loadModule(item.id)">
                <i :class="['fa-solid', item.icon]"></i>
                <span>{{ item.name }}</span>
            </button>
            <button class="logout" @click="logout">
                <i class="fa-solid fa-right-from-bracket"></i>
                <span>退出</span>
            </button>
        </aside>

        <main>
            <header>
                <div>
                    <h1>{{ currentModule?.name }}</h1>
                    <p v-if="active !== 'dashboard'">共 {{ filteredRows.length }} 条记录，当前第 {{ pageNo }} 页</p>
                </div>
                <a href="#/">返回用户端</a>
            </header>

            <section v-if="active === 'dashboard'" class="dashboard">
                <div class="metric"><span>用户数</span><strong>{{ dashboard.userCount || 0 }}</strong></div>
                <div class="metric"><span>方案数</span><strong>{{ dashboard.tripPlanCount || 0 }}</strong></div>
                <div class="metric"><span>咨询记录</span><strong>{{ dashboard.chatRecordCount || 0 }}</strong></div>
                <div class="metric"><span>路线记录</span><strong>{{ dashboard.routeCount || 0 }}</strong></div>
                <div class="metric"><span>服务记录</span><strong>{{ dashboard.smartServiceCount || 0 }}</strong></div>
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
                        <button v-if="config.empty" class="primary" @click="openCreate">新增</button>
                    </div>
                </div>

                <div v-if="loading" class="loading">加载中...</div>
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
                                <button @click="openEdit(row)">编辑</button>
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
    background: #f5f7fb;
    color: #172033;
}

.admin-login {
    display: flex;
    align-items: center;
    justify-content: center;
}

.login-card {
    width: 380px;
    background: white;
    border: 1px solid #e5e8ef;
    border-radius: 8px;
    padding: 28px;
    box-shadow: 0 12px 30px rgba(16, 24, 40, 0.08);
}

.login-card h1 {
    font-size: 24px;
    margin-bottom: 6px;
}

.login-card p,
header p {
    color: #667085;
}

label {
    display: block;
    margin: 14px 0 8px;
    font-size: 13px;
    color: #475467;
    font-weight: 600;
}

input,
select,
textarea {
    padding: 11px 12px;
    border: 1px solid #d0d5dd;
    border-radius: 8px;
    background: white;
    font-family: inherit;
}

textarea {
    min-height: 110px;
    resize: vertical;
}

.login-card input {
    width: 100%;
}

.login-card button,
.modal-actions button[type="submit"],
.table-toolbar .primary {
    border: none;
    color: white;
    background: #246bfe;
}

.login-card button {
    width: 100%;
    margin-top: 20px;
    padding: 12px;
    border-radius: 8px;
    cursor: pointer;
}

.login-card span {
    display: block;
    margin-top: 12px;
    color: #98a2b3;
    font-size: 12px;
}

.admin-layout {
    display: flex;
}

aside {
    width: 240px;
    background: #111827;
    color: white;
    padding: 18px 14px;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.brand {
    display: flex;
    align-items: center;
    gap: 10px;
    font-weight: 700;
    font-size: 18px;
    padding: 10px 12px 22px;
}

aside button {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 11px 12px;
    border: none;
    border-radius: 8px;
    background: transparent;
    color: #d1d5db;
    cursor: pointer;
    text-align: left;
}

aside button.active,
aside button:hover {
    background: #2563eb;
    color: white;
}

.logout {
    margin-top: auto;
}

main {
    flex: 1;
    min-width: 0;
    padding: 24px;
}

header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 22px;
}

header h1 {
    font-size: 24px;
}

header a {
    color: #2563eb;
    text-decoration: none;
}

.dashboard {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 16px;
}

.metric,
.table-card {
    background: white;
    border: 1px solid #e5e8ef;
    border-radius: 8px;
    box-shadow: 0 8px 20px rgba(16, 24, 40, 0.04);
}

.metric {
    padding: 22px;
}

.metric span {
    color: #667085;
}

.metric strong {
    display: block;
    margin-top: 10px;
    font-size: 32px;
}

.table-card {
    overflow-x: auto;
}

.table-toolbar {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    padding: 14px;
    border-bottom: 1px solid #edf0f5;
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
    border: 1px solid #d0d5dd;
    border-radius: 8px;
    background: white;
    cursor: pointer;
}

.table-card table {
    width: 100%;
    border-collapse: collapse;
    min-width: 900px;
}

th,
td {
    padding: 12px 14px;
    border-bottom: 1px solid #edf0f5;
    text-align: left;
    font-size: 13px;
    vertical-align: top;
}

th {
    background: #f8fafc;
    color: #475467;
}

.actions {
    white-space: nowrap;
}

.actions button,
.pagination button {
    border: 1px solid #d0d5dd;
    background: white;
    border-radius: 6px;
    padding: 6px 9px;
    cursor: pointer;
    margin-right: 6px;
}

.actions .danger {
    color: #b42318;
    border-color: #f2b8b5;
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
    padding: 40px;
    color: #667085;
    text-align: center;
}

.modal-mask {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.35);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 100;
}

.modal {
    width: 560px;
    max-height: 84vh;
    overflow-y: auto;
    background: white;
    border-radius: 8px;
    padding: 24px;
}

.field input,
.field textarea {
    width: 100%;
}

.modal-actions {
    display: flex;
    gap: 10px;
    margin-top: 18px;
}

.modal-actions button {
    flex: 1;
    padding: 10px;
    border-radius: 8px;
    border: 1px solid #d0d5dd;
    cursor: pointer;
}

@media (max-width: 1100px) {
    .dashboard {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}
</style>
