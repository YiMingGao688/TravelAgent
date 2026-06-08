<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { deleteServiceReport, getServiceReport, listServiceReports } from '../../utils/api.js'

const reports = ref([])
const keyword = ref('')
const loading = ref(false)
const detail = ref(null)
const statusMessage = ref('')

async function loadReports() {
    loading.value = true
    try {
        const userId = sessionStorage.getItem('loginUser') || 'guest'
        reports.value = await listServiceReports(userId, keyword.value)
    } catch (error) {
        statusMessage.value = `加载失败：${error.message}`
    } finally {
        loading.value = false
    }
}

async function openDetail(id) {
    try {
        detail.value = await getServiceReport(id)
        statusMessage.value = ''
    } catch (error) {
        detail.value = null
        if (error.message?.includes('报告不存在')) {
            statusMessage.value = '这份报告已不在数据库中，已同步刷新列表。'
            await loadReports()
            return
        }
        statusMessage.value = `查看失败：${error.message}`
    }
}

async function removeReport(item) {
    if (!confirm(`确定删除「${item.title || '这份报告'}」吗？删除后数据库记录会同步删除。`)) return
    try {
        await deleteServiceReport(item.id)
        statusMessage.value = '报告已删除'
        if (detail.value?.id === item.id) detail.value = null
        await loadReports()
    } catch (error) {
        statusMessage.value = `删除失败：${error.message}`
        await loadReports()
    }
}

function formatTime(value) {
    return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

function handleFocus() {
    loadReports()
}

onMounted(() => {
    loadReports()
    window.addEventListener('focus', handleFocus)
    document.addEventListener('visibilitychange', handleFocus)
})

onBeforeUnmount(() => {
    window.removeEventListener('focus', handleFocus)
    document.removeEventListener('visibilitychange', handleFocus)
})
</script>

<template>
    <div class="report-page">
        <header class="report-header">
            <div>
                <p>这里始终从数据库读取，刷新或回到页面时会自动同步最新报告。</p>
            </div>
            <div class="search-box">
                <input v-model="keyword" placeholder="搜索标题、用户、起终点" @keydown.enter="loadReports" />
                <button @click="loadReports"><i class="fa-solid fa-rotate"></i> 同步</button>
            </div>
        </header>

        <div v-if="statusMessage" class="status">{{ statusMessage }}</div>
        <div v-if="loading" class="empty">正在从数据库同步报告...</div>

        <div v-else-if="!reports.length" class="empty">
            <i class="fa-regular fa-file-lines"></i>
            <h3>暂无保存的服务报告</h3>
            <p>数据库中没有当前用户的报告记录。</p>
        </div>

        <div v-else class="report-grid">
            <article v-for="item in reports" :key="item.id" class="report-card">
                <div class="card-top">
                    <i class="fa-solid fa-file-shield"></i>
                    <button @click="removeReport(item)" title="删除报告"><i class="fa-solid fa-xmark"></i></button>
                </div>
                <h3>{{ item.title || '智慧出行服务报告' }}</h3>
                <div class="route-line">{{ item.startPlace || '出发地' }} → {{ item.endPlace || '目的地' }}</div>
                <p>{{ item.reportSummary || item.reportContent || '暂无报告摘要' }}</p>
                <div class="meta">
                    <span>方案：{{ item.tripPlanId || '未关联' }}</span>
                    <span>{{ formatTime(item.createTime) }}</span>
                </div>
                <button class="detail-btn" @click="openDetail(item.id)">查看详情</button>
            </article>
        </div>

        <div v-if="detail" class="detail-mask" @click.self="detail = null">
            <article class="detail-panel">
                <button class="close" @click="detail = null"><i class="fa-solid fa-xmark"></i></button>
                <h2>{{ detail.title }}</h2>
                <div class="route-line">{{ detail.startPlace || '出发地' }} → {{ detail.endPlace || '目的地' }}</div>
                <p class="summary">{{ detail.reportSummary }}</p>
                <pre>{{ detail.reportContent }}</pre>
            </article>
        </div>
    </div>
</template>

<style scoped>
.report-page {
    flex: 1;
    padding: 28px 32px 40px;
    overflow-y: auto;
}

.report-header {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 20px;
    margin-bottom: 16px;
}

.report-header p,
.empty p,
.report-card p,
.meta,
.summary {
    color: var(--text-secondary);
    line-height: 1.6;
}

.report-header p {
    margin: 0;
    max-width: 560px;
}

.detail-panel h2 {
    margin: 6px 0;
    color: var(--text-main);
    font-size: 26px;
}

.search-box {
    display: flex;
    gap: 8px;
}

.search-box input {
    width: 260px;
    padding: 10px 12px;
    border: 1px solid var(--border-color);
    border-radius: 8px;
    background: var(--card-bg);
    color: var(--text-main);
}

.search-box button,
.detail-btn {
    border: none;
    border-radius: 8px;
    background: var(--primary-color);
    color: white;
    padding: 10px 14px;
    cursor: pointer;
}

.status {
    display: inline-block;
    margin-bottom: 14px;
    padding: 8px 12px;
    border-radius: 999px;
    background: var(--primary-light);
    color: var(--primary-color);
}

.empty {
    min-height: 58vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
}

.empty i {
    font-size: 56px;
    color: var(--border-color);
    margin-bottom: 16px;
}

.report-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
    gap: 18px;
}

.report-card {
    min-height: 300px;
    display: flex;
    flex-direction: column;
    padding: 18px;
    border: 1px solid var(--border-color);
    border-radius: 8px;
    background: var(--card-bg);
    box-shadow: var(--shadow-sm);
}

.card-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
}

.card-top i {
    color: var(--primary-color);
}

.card-top button,
.close {
    border: none;
    background: transparent;
    color: var(--text-secondary);
    cursor: pointer;
}

.report-card h3 {
    color: var(--text-main);
    line-height: 1.4;
}

.route-line {
    margin: 10px 0;
    padding: 8px 10px;
    border-radius: 8px;
    background: var(--bg-color);
    color: var(--primary-color);
    font-weight: 700;
    font-size: 13px;
}

.report-card p {
    flex: 1;
    display: -webkit-box;
    -webkit-line-clamp: 5;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.meta {
    display: flex;
    justify-content: space-between;
    gap: 10px;
    font-size: 12px;
    margin: 12px 0;
}

.detail-mask {
    position: fixed;
    inset: 0;
    z-index: 1000;
    background: rgba(15, 23, 42, 0.35);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
}

.detail-panel {
    width: min(860px, 100%);
    max-height: 86vh;
    overflow-y: auto;
    position: relative;
    border-radius: 8px;
    background: var(--card-bg);
    padding: 26px;
}

.close {
    position: absolute;
    right: 18px;
    top: 18px;
}

pre {
    white-space: pre-wrap;
    word-break: break-word;
    line-height: 1.8;
    color: var(--text-main);
    font-family: inherit;
    background: var(--bg-color);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    padding: 16px;
}

@media (max-width: 760px) {
    .report-page {
        padding: 32px 16px;
    }

    .report-header {
        align-items: flex-start;
        flex-direction: column;
    }

    .search-box,
    .search-box input {
        width: 100%;
    }
}
</style>
