<script setup>
defineProps({
    historyList: { type: Array, default: () => [] }
})

const emit = defineEmits(['delete', 'goToChat', 'importTransit', 'importCustom'])

function formatTime(value) {
    if (!value) return ''
    return String(value).replace('T', ' ').slice(0, 16)
}

function displayTitle(item) {
    if (item.title && !/AI_CHAT|智能出行方案/.test(item.title)) return item.title
    if (item.startPlace && item.endPlace) return `${item.startPlace} 到 ${item.endPlace} 的出行方案`
    if (item.endPlace) return `${item.endPlace} 出行方案`
    return '我的出行方案'
}

function displayRoute(item) {
    const start = item.startPlace || '待补充起点'
    const end = item.endPlace || '待补充终点'
    return `${start} → ${end}`
}
</script>

<template>
    <div class="history-container">
        <div class="history-header">
            <div>
                <span class="eyebrow">Saved Plans</span>
                <h2>历史方案</h2>
                <p>保存过的出行方案可以继续导入路线规划或行程定制。</p>
            </div>
            <button v-if="historyList.length" class="clear-btn" @click="emit('delete', -1)">
                <i class="fa-solid fa-trash-can"></i>
                <span>清空</span>
            </button>
        </div>

        <div class="empty-module" v-if="!historyList.length">
            <div class="empty-icon"><i class="fa-solid fa-folder-open"></i></div>
            <h3>还没有保存的方案</h3>
            <p>在 AI 咨询中生成满意的方案后，点击“保存方案”就会出现在这里。</p>
            <button class="empty-action" @click="emit('goToChat')">
                <i class="fa-solid fa-message"></i>
                <span>去咨询</span>
            </button>
        </div>

        <div class="history-list" v-else>
            <article class="history-card" v-for="(item, index) in historyList" :key="item.id || index">
                <div class="card-top">
                    <div class="route-icon"><i class="fa-solid fa-map-location-dot"></i></div>
                    <button class="delete-btn" @click="emit('delete', index)" title="删除">
                        <i class="fa-solid fa-xmark"></i>
                    </button>
                </div>

                <h3>{{ displayTitle(item) }}</h3>
                <div class="route-line">{{ displayRoute(item) }}</div>

                <p class="card-content">{{ item.content }}</p>

                <div class="meta-row">
                    <span><i class="fa-regular fa-clock"></i> {{ formatTime(item.createTime || item.createdAt) || '刚刚' }}</span>
                    <span v-if="item.days"><i class="fa-regular fa-calendar"></i> {{ item.days }} 天</span>
                    <span v-if="item.budget"><i class="fa-solid fa-wallet"></i> {{ item.budget }}</span>
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
    padding: 56px 32px 40px;
    overflow-y: auto;
}

.history-header {
    display: flex;
    justify-content: space-between;
    gap: 20px;
    align-items: flex-end;
    margin-bottom: 24px;
}

.eyebrow {
    color: var(--primary-color);
    font-size: 12px;
    font-weight: 700;
}

.history-header h2 {
    margin: 6px 0;
    color: var(--text-main);
    font-size: 26px;
}

.history-header p,
.empty-module p,
.card-content,
.meta-row {
    color: var(--text-secondary);
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
    display: -webkit-box;
    -webkit-line-clamp: 5;
    -webkit-box-orient: vertical;
    overflow: hidden;
    flex: 1;
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
