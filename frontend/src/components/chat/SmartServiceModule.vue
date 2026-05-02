<script setup>
import { computed, onMounted, ref } from 'vue'
import { generateSmartService, listSmartServices } from '../../utils/api.js'

const props = defineProps({
    profile: { type: Object, default: () => ({}) },
    routeData: { type: Object, default: () => ({}) }
})

const services = [
    { id: 'REALTIME_ALERT', name: '实时提醒', icon: 'fa-bell', desc: '天气、拥堵、晚点、换乘风险提醒' },
    { id: 'INTERMODAL_PLAN', name: '联程出行', icon: 'fa-train-subway', desc: '高铁、飞机、地铁、网约车组合规划' },
    { id: 'SPECIAL_SCENE', name: '特殊照顾', icon: 'fa-heart-pulse', desc: '老人、亲子、无障碍、夜间出行' },
    { id: 'AFTER_SALES', name: '售后协助', icon: 'fa-headset', desc: '退改签、延误处理、客服协助' },
    { id: 'PASSENGER_LUGGAGE', name: '行李服务', icon: 'fa-suitcase-rolling', desc: '行李提醒、托运、到站取送建议' },
    { id: 'PAYMENT_CREDIT', name: '支付权益', icon: 'fa-credit-card', desc: '组合支付、信用免押、会员权益' },
    { id: 'PROACTIVE_LOOP', name: '行程管家', icon: 'fa-arrows-rotate', desc: '出发前、途中、到达后的主动服务' }
]

const activeType = ref('INTERMODAL_PLAN')
const loading = ref(false)
const records = ref([])
const form = ref(defaultForm())

const activeService = computed(() => services.find(item => item.id === activeType.value))
const latestRecord = computed(() => records.value[0])

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

async function generate() {
    loading.value = true
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
        records.value.unshift(result.data)
    } catch (error) {
        alert(`生成失败：${error.message}`)
    } finally {
        loading.value = false
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
        endPlace: props.routeData.to || '上海虹桥',
        travelDate: '明天 09:00',
        preference: props.profile.style || '少换乘、舒适优先',
        specialNeed: type === 'SPECIAL_SCENE' ? '同行有老人，行李较多，希望少走路，可以提前预约协助服务' : '如果出现晚点或拥堵，希望自动给出替代方案',
        budget: '中等预算',
        passengerInfo: '2名成人，1名老人',
        luggageInfo: '2个24寸行李箱，1个背包',
        paymentPreference: '微信支付，优先使用会员权益和信用免押',
        scenario: activeService.value?.desc || '日常跨城出行',
        extraRequirement: '请给出可直接执行的出行安排、提醒节点、预订建议和异常处理方案。'
    }
}

function serviceTitle(type) {
    return services.find(item => item.id === type)?.name || '出行服务'
}

onMounted(loadRecords)
</script>

<template>
    <div class="service-page">
        <section class="topbar">
            <div>
                <span class="eyebrow">Travel Care</span>
                <h1>一站式出行服务</h1>
                <p>把路线、提醒、预订、售后、行李和支付权益整合到同一个行程里，让用户少操心。</p>
            </div>
            <button class="primary" @click="fillExample()">
                <i class="fa-solid fa-wand-magic-sparkles"></i>
                <span>填充示例</span>
            </button>
        </section>

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
                    <label>目的地<input v-model="form.endPlace" placeholder="例如：上海虹桥" /></label>
                </div>
                <div class="form-row">
                    <label>出行时间<input v-model="form.travelDate" placeholder="例如：明天 09:00" /></label>
                    <label>预算<input v-model="form.budget" placeholder="例如：中等预算" /></label>
                </div>
                <label>出行偏好<input v-model="form.preference" placeholder="少换乘、舒适优先、时间优先、经济优先" /></label>
                <label>特殊需求<input v-model="form.specialNeed" placeholder="老人、亲子、无障碍、夜间、赶时间、行李较多等" /></label>
                <div class="form-row">
                    <label>同行人<input v-model="form.passengerInfo" placeholder="例如：2名成人，1名老人" /></label>
                    <label>行李<input v-model="form.luggageInfo" placeholder="例如：2个行李箱，1个背包" /></label>
                </div>
                <label>支付与权益偏好<input v-model="form.paymentPreference" placeholder="微信/支付宝/银行卡，会员权益，信用免押" /></label>
                <label>补充说明<textarea v-model="form.extraRequirement" placeholder="例如：希望到站后直接安排接驳，途中有变化时及时提醒"></textarea></label>

                <button class="primary submit" :disabled="loading">
                    <i class="fa-solid fa-cloud"></i>
                    <span>{{ loading ? '正在生成专属方案...' : '生成服务方案' }}</span>
                </button>
            </form>

            <section class="result-panel">
                <div class="panel-header">
                    <div>
                        <h2>专属方案</h2>
                        <p>由云端大模型根据本次需求生成</p>
                    </div>
                    <span>{{ records.length }} 条</span>
                </div>

                <div v-if="!latestRecord" class="empty">
                    <i class="fa-solid fa-route"></i>
                    <p>选择服务类型并填写需求后，这里会生成可执行的出行服务方案。</p>
                </div>

                <article v-else class="record">
                    <div class="record-head">
                        <strong>{{ latestRecord.title || serviceTitle(latestRecord.serviceType) }}</strong>
                        <span>已生成</span>
                    </div>
                    <div class="route-line">{{ latestRecord.startPlace || '出发地' }} → {{ latestRecord.endPlace || '目的地' }}</div>
                    <pre>{{ latestRecord.responsePayload }}</pre>
                </article>

                <div v-if="records.length > 1" class="recent-list">
                    <h3>最近生成</h3>
                    <button v-for="item in records.slice(1, 4)" :key="item.id">
                        <strong>{{ item.title || serviceTitle(item.serviceType) }}</strong>
                        <span>{{ item.startPlace || '出发地' }} → {{ item.endPlace || '目的地' }}</span>
                    </button>
                </div>
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
    margin-bottom: 22px;
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
.recent-list span {
    color: var(--text-secondary);
    line-height: 1.6;
}

.primary {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    border: none;
    border-radius: 8px;
    padding: 11px 16px;
    background: var(--primary-color);
    color: white;
    cursor: pointer;
    font-weight: 600;
}

.page-grid {
    display: grid;
    grid-template-columns: 260px 410px minmax(0, 1fr);
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

.submit:disabled {
    opacity: 0.65;
    cursor: not-allowed;
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

.record {
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
    margin-top: 12px;
    max-height: 520px;
    overflow-y: auto;
}

.recent-list {
    margin-top: 16px;
    display: grid;
    gap: 8px;
}

.recent-list h3 {
    color: var(--text-main);
    font-size: 15px;
}

.recent-list button {
    border: 1px solid var(--border-color);
    background: var(--bg-color);
    border-radius: 8px;
    padding: 10px;
    text-align: left;
    color: var(--text-main);
}

.recent-list strong,
.recent-list span {
    display: block;
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

    .page-grid {
        grid-template-columns: 1fr;
    }

    .service-list {
        position: static;
    }
}
</style>
