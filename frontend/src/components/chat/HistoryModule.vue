<script setup>
defineProps({
    historyList: { type: Array, default: () => [] }
})

const emit = defineEmits(['delete', 'goToChat', 'importTransit', 'importCustom'])

function formatTime(value) {
    if (!value) return ''
    return String(value).replace('T', ' ').slice(0, 16)
}

function parseContent(item) {
    const raw = String(item?.content || '').trim()
    if (!raw) return null
    try {
        return JSON.parse(raw)
    } catch {
        return null
    }
}

function planTitle(item) {
    const json = parseContent(item)
    if (json?.title) return json.title
    if (item.title && !/AI_CHAT|智能出行方案|travel plan/i.test(item.title)) return item.title
    if (item.startPlace && item.endPlace) return `${item.startPlace} 到 ${item.endPlace} 出行方案`
    if (item.endPlace) return `${item.endPlace} 出行方案`
    return '我的出行方案'
}

function routeText(item) {
    const start = item.startPlace || '待补充起点'
    const end = item.endPlace || '待补充终点'
    return `${start} → ${end}`
}

function summaryText(item) {
    const json = parseContent(item)
    if (json?.summary) return json.summary
    const text = String(item.content || '').replace(/\s+/g, ' ').trim()
    return text || '暂无方案内容'
}

function dayPlans(item) {
    const json = parseContent(item)
    const days = json?.days || json?.dailyPlans || json?.itinerary || []
    return Array.isArray(days) ? days.slice(0, 3) : []
}

function dayTitle(day, index) {
    return day?.title || day?.day || `第 ${index + 1} 天`
}

function dayDetail(day) {
    if (typeof day === 'string') return day
    const parts = [
        day?.morning && `上午：${day.morning}`,
        day?.afternoon && `下午：${day.afternoon}`,
        day?.evening && `晚上：${day.evening}`,
        day?.traffic && `交通：${day.traffic}`,
        day?.food && `餐饮：${day.food}`,
        day?.risk && `提醒：${day.risk}`
    ].filter(Boolean)
    return parts.join('；') || day?.content || day?.plan || ''
}

function suitableFor(item) {
    const json = parseContent(item)
    return json?.suitableFor || json?.crowdType || ''
}

function sourceName(value) {
    const map = {
        AI_CHAT: 'AI 咨询',
        ITINERARY: '行程定制',
        ADMIN: '管理端'
    }
    return map[value] || value || '用户保存'
}
</script>

<template>
    <div class="history-container">
        <div class="history-header">
            <div>
                <p>已保存的出行方案会在这里统一管理，可继续导入交通规划或行程定制。</p>
            </div>
            <button v-if="historyList.length" class="clear-btn" @click="emit('delete', -1)">
                <i class="fa-solid fa-trash-can"></i>
                <span>清空全部</span>
            </button>
        </div>

        <div class="empty-module" v-if="!historyList.length">
            <div class="empty-icon"><i class="fa-solid fa-folder-open"></i></div>
            <h3>还没有保存的方案</h3>
            <p>在 AI 咨询或行程定制中保存方案后，会显示在这里。</p>
            <button class="empty-action" @click="emit('goToChat')">
                <i class="fa-solid fa-message"></i>
                <span>去咨询</span>
            </button>
        </div>

        <div class="history-list" v-else>
            <article class="history-card" v-for="(item, index) in historyList" :key="item.id || index">
                <div class="card-top">
                    <div class="route-icon"><i class="fa-solid fa-map-location-dot"></i></div>
                    <button class="delete-btn" @click="emit('delete', index)" title="删除方案">
                        <i class="fa-solid fa-xmark"></i>
                    </button>
                </div>

                <h3>{{ planTitle(item) }}</h3>
                <div class="route-line">{{ routeText(item) }}</div>

                <p class="card-content">{{ summaryText(item) }}</p>

                <div v-if="dayPlans(item).length" class="day-list">
                    <div v-for="(day, dayIndex) in dayPlans(item)" :key="dayIndex" class="day-item">
                        <strong>{{ dayTitle(day, dayIndex) }}</strong>
                        <span>{{ dayDetail(day) }}</span>
                    </div>
                </div>

                <div class="meta-row">
                    <span><i class="fa-regular fa-clock"></i> {{ formatTime(item.createTime || item.createdAt) || '刚刚' }}</span>
                    <span><i class="fa-solid fa-tag"></i> {{ sourceName(item.sourceType) }}</span>
                    <span v-if="item.days"><i class="fa-regular fa-calendar"></i> {{ item.days }} 天</span>
                    <span v-if="item.budget"><i class="fa-solid fa-wallet"></i> {{ item.budget }}</span>
                    <span v-if="suitableFor(item)"><i class="fa-solid fa-user-check"></i> {{ suitableFor(item) }}</span>
                </div>

                <div class="card-actions">
                    <button @click="emit('importTransit', item)">
                        <i class="fa-solid fa-map-location-dot"></i>
                        <span>规划路线</span>
                    </button>
                    <button @click="emit('importCustom', item)">
                        <i class="fa-solid fa-route"></i>
                        <span>定制行程</span>
                    </button>
                </div>
            </article>
        </div>
    </div>
</template>

<style scoped>
.history-container {
    flex: 1;
    padding: 18px 32px 40px;
    overflow-y: auto;
}

.history-header {
    display: flex;
    justify-content: space-between;
    gap: 20px;
    align-items: flex-end;
    margin-bottom: 10px;
}

.history-header p,
.empty-module p,
.card-content,
.meta-row,
.day-item span {
    color: var(--text-secondary);
}

.history-header p {
    margin: 0;
    max-width: 680px;
    line-height: 1.6;
}

.clear-btn,
.empty-action,
.card-actions button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    border: none;
    border-radius: 8px;
    cursor: pointer;
}

.clear-btn {
    padding: 9px 13px;
    background: rgba(220, 53, 69, 0.08);
    color: #dc3545;
}

.empty-module {
    min-height: 58vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
}

.empty-icon {
    width: 82px;
    height: 82px;
    border-radius: 50%;
    background: var(--primary-light);
    color: var(--primary-color);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 34px;
    margin-bottom: 18px;
}

.empty-module h3 {
    color: var(--text-main);
    margin-bottom: 8px;
}

.empty-action {
    margin-top: 18px;
    padding: 10px 18px;
    background: var(--primary-color);
    color: white;
}

.history-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
    gap: 18px;
}

.history-card {
    background: var(--card-bg);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    padding: 18px;
    box-shadow: var(--shadow-sm);
    display: flex;
    flex-direction: column;
    min-height: 320px;
}

.card-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
}

.route-icon {
    width: 38px;
    height: 38px;
    border-radius: 8px;
    background: var(--primary-light);
    color: var(--primary-color);
    display: flex;
    align-items: center;
    justify-content: center;
}

.delete-btn {
    width: 30px;
    height: 30px;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    background: transparent;
    color: var(--text-secondary);
}

.delete-btn:hover {
    background: rgba(220, 53, 69, 0.08);
    color: #dc3545;
}

.history-card h3 {
    color: var(--text-main);
    font-size: 18px;
    line-height: 1.4;
    margin-bottom: 8px;
}

.route-line {
    padding: 8px 10px;
    border-radius: 8px;
    background: var(--bg-color);
    color: var(--primary-color);
    font-weight: 600;
    font-size: 13px;
    margin-bottom: 12px;
}

.card-content {
    font-size: 14px;
    line-height: 1.7;
    white-space: pre-wrap;
}

.day-list {
    display: grid;
    gap: 8px;
    margin: 12px 0;
}

.day-item {
    border: 1px solid var(--border-color);
    background: var(--bg-color);
    border-radius: 8px;
    padding: 10px;
}

.day-item strong {
    display: block;
    color: var(--text-main);
    margin-bottom: 5px;
}

.day-item span {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    font-size: 13px;
    line-height: 1.6;
}

.meta-row {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    font-size: 12px;
    margin: 14px 0;
}

.card-actions {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
    margin-top: auto;
}

.card-actions button {
    padding: 10px;
    background: var(--bg-color);
    color: var(--primary-color);
    border: 1px solid var(--primary-color);
}

@media (max-width: 768px) {
    .history-container {
        padding: 32px 16px;
    }

    .history-header {
        align-items: flex-start;
        flex-direction: column;
    }

    .history-list {
        grid-template-columns: 1fr;
    }
}
</style>
