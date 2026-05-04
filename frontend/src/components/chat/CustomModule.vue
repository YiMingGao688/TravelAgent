<script setup>
import { computed, ref, watch } from 'vue'
import { generateItinerary, deleteTripPlan } from '../../utils/api.js'

const props = defineProps({
    formData: {
        type: Object,
        default: () => ({
            startPlace: '',
            destination: '',
            days: 3,
            travelDate: '',
            crowdType: '普通出行',
            preference: '休闲度假',
            budget: '中等预算',
            style: ''
        })
    }
})

const emit = defineEmits(['update:formData', 'generateService'])

const loading = ref(false)
const statusMessage = ref('')
const savedPlan = ref(null)
const itinerary = ref(null)

const preferences = ['休闲度假', '景点打卡', '美食优先', '少走路', '预算优先', '舒适优先']
const crowdTypes = ['普通出行', '亲子', '老人', '商务', '学生']

const days = computed(() => itinerary.value?.days || [])

watch(() => props.formData, value => {
    if (value?.style && !value.preference) {
        updateField('preference', value.style)
    }
}, { deep: true, immediate: true })

function updateField(key, value) {
    emit('update:formData', { ...props.formData, [key]: value })
}

async function generate() {
    if (!props.formData.destination) {
        statusMessage.value = '请先填写目的地'
        return
    }
    loading.value = true
    statusMessage.value = '正在生成个性化行程...'
    try {
        const result = await generateItinerary({
            userId: sessionStorage.getItem('loginUser') || 'guest',
            sessionId: sessionStorage.getItem('chatSessionId') || '',
            startPlace: props.formData.startPlace || '',
            destination: props.formData.destination,
            days: props.formData.days || 1,
            travelDate: props.formData.travelDate || '',
            crowdType: props.formData.crowdType || '普通出行',
            preference: props.formData.preference || props.formData.style || '休闲度假',
            budget: props.formData.budget || ''
        })
        savedPlan.value = result
        itinerary.value = parseItinerary(result.content)
        statusMessage.value = '行程已生成并保存到历史方案'
    } catch (error) {
        statusMessage.value = `生成失败：${error.message}`
    } finally {
        loading.value = false
    }
}

async function removeSavedPlan() {
    console.log('前端删除 id = ', savedPlan.value?.id, typeof savedPlan.value?.id)
    if (!savedPlan.value?.id) return
    if (!confirm('确定删除刚保存的行程吗？数据库记录会同步删除。')) return
    await deleteTripPlan(savedPlan.value.id)
    savedPlan.value = null
    itinerary.value = null
    statusMessage.value = '行程已从数据库删除'
}

function parseItinerary(content) {
    try {
        return JSON.parse(content)
    } catch (error) {
        return {
            title: savedPlan.value?.title || '个性化行程',
            summary: content,
            suitableFor: props.formData.crowdType,
            riskReminder: '请在出发前确认天气、营业时间和交通变化。',
            trafficAdvice: '建议结合交通规划模块确认实时路线。',
            foodAdvice: '可根据目的地商圈选择就近餐饮。',
            days: []
        }
    }
}

function generateService() {
    emit('generateService', {
        id: savedPlan.value?.id,
        startPlace: props.formData.startPlace,
        endPlace: props.formData.destination,
        travelPreference: props.formData.preference,
        budget: props.formData.budget,
        companions: props.formData.crowdType,
        content: savedPlan.value?.content || ''
    })
}
</script>

<template>
    <div class="custom-module">
        <aside class="custom-sidebar">
            <div class="module-title">
                <i class="fa-solid fa-route"></i>
                <span>个性化行程生成中心</span>
            </div>

            <label>出发地<input :value="formData.startPlace" @input="updateField('startPlace', $event.target.value)" placeholder="例如：南京" /></label>
            <label>目的地<input :value="formData.destination" @input="updateField('destination', $event.target.value)" placeholder="例如：杭州" /></label>
            <div class="form-row">
                <label>出行天数<input type="number" min="1" max="15" :value="formData.days" @input="updateField('days', Number($event.target.value) || 1)" /></label>
                <label>出行日期<input type="date" :value="formData.travelDate" @input="updateField('travelDate', $event.target.value)" /></label>
            </div>
            <label>人群类型
                <select :value="formData.crowdType" @change="updateField('crowdType', $event.target.value)">
                    <option v-for="item in crowdTypes" :key="item">{{ item }}</option>
                </select>
            </label>
            <label>出行偏好
                <select :value="formData.preference || formData.style" @change="updateField('preference', $event.target.value)">
                    <option v-for="item in preferences" :key="item">{{ item }}</option>
                </select>
            </label>
            <label>预算<input :value="formData.budget" @input="updateField('budget', $event.target.value)" placeholder="例如：1500 元以内 / 中等预算" /></label>

            <button class="primary-btn" :disabled="loading" @click="generate">
                <i class="fa-solid fa-wand-magic-sparkles"></i>
                <span>{{ loading ? 'AI 正在生成...' : '生成行程' }}</span>
            </button>
            <p v-if="statusMessage" class="status">{{ statusMessage }}</p>
        </aside>

        <main class="custom-content">
            <div v-if="!itinerary && !loading" class="empty-module">
                <i class="fa-solid fa-map-location-dot"></i>
                <h3>还没有生成行程</h3>
                <p>填写左侧信息后，AI 会生成每日卡片，并真实保存到历史方案。</p>
            </div>

            <div v-if="loading" class="empty-module">
                <div class="spinner"></div>
                <p>正在结合目的地、人群、偏好和预算生成行程...</p>
            </div>

            <section v-if="itinerary && !loading" class="itinerary-result">
                <header class="result-head">
                    <div>
                        <span class="eyebrow">已保存</span>
                        <h2>{{ itinerary.title || savedPlan?.title }}</h2>
                        <p>{{ itinerary.summary }}</p>
                    </div>
                    <div class="result-actions">
                        <button @click="generateService"><i class="fa-solid fa-clipboard-check"></i> 生成出行服务</button>
                        <button class="danger" @click="removeSavedPlan"><i class="fa-solid fa-trash-can"></i> 删除行程</button>
                    </div>
                </header>

                <div class="summary-grid">
                    <div><strong>交通建议</strong><span>{{ itinerary.trafficAdvice || '建议结合交通规划确认实时路线。' }}</span></div>
                    <div><strong>餐饮建议</strong><span>{{ itinerary.foodAdvice || '建议优先选择就近餐饮。' }}</span></div>
                    <div><strong>风险提醒</strong><span>{{ itinerary.riskReminder || '出发前确认天气和交通变化。' }}</span></div>
                    <div><strong>适合人群</strong><span>{{ itinerary.suitableFor || formData.crowdType }}</span></div>
                </div>

                <article v-for="day in days" :key="day.day" class="day-card">
                    <div class="day-header">第 {{ day.day }} 天：{{ day.theme }}</div>
                    <div class="period-grid">
                        <section><span>上午</span><p>{{ day.morning }}</p></section>
                        <section><span>下午</span><p>{{ day.afternoon }}</p></section>
                        <section><span>晚上</span><p>{{ day.evening }}</p></section>
                    </div>
                    <div class="day-notes">
                        <p><strong>交通：</strong>{{ day.traffic }}</p>
                        <p><strong>餐饮：</strong>{{ day.food }}</p>
                        <p><strong>提醒：</strong>{{ day.risk }}</p>
                    </div>
                </article>
            </section>
        </main>
    </div>
</template>

<style scoped>
.custom-module {
    display: flex;
    flex: 1;
    height: 100%;
    overflow: hidden;
}

.custom-sidebar {
    width: 360px;
    background: var(--card-bg);
    border-right: 1px solid var(--border-color);
    padding: 24px;
    overflow-y: auto;
    flex-shrink: 0;
}

.module-title {
    font-size: 18px;
    font-weight: 700;
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--primary-color);
}

label {
    display: block;
    margin-bottom: 14px;
    color: var(--text-secondary);
    font-size: 13px;
    font-weight: 600;
}

input,
select {
    width: 100%;
    margin-top: 8px;
    padding: 11px 12px;
    background: var(--bg-color);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    color: var(--text-main);
    outline: none;
}

.form-row {
    display: grid;
    grid-template-columns: 1fr 1.4fr;
    gap: 10px;
}

.primary-btn,
.result-actions button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    border-radius: 8px;
    cursor: pointer;
}

.primary-btn {
    width: 100%;
    padding: 13px;
    border: none;
    background: var(--primary-color);
    color: white;
    font-weight: 700;
}

.primary-btn:disabled {
    opacity: 0.65;
    cursor: not-allowed;
}

.status {
    margin-top: 12px;
    color: var(--text-secondary);
    line-height: 1.5;
}

.custom-content {
    flex: 1;
    overflow-y: auto;
    padding: 24px;
    background: var(--bg-color);
}

.empty-module {
    min-height: 60vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    color: var(--text-secondary);
}

.empty-module i {
    font-size: 58px;
    color: var(--border-color);
    margin-bottom: 18px;
}

.spinner {
    width: 46px;
    height: 46px;
    border: 4px solid var(--border-color);
    border-top-color: var(--primary-color);
    border-radius: 50%;
    animation: spin 1s linear infinite;
}

@keyframes spin {
    to { transform: rotate(360deg); }
}

.result-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 18px;
}

.eyebrow {
    color: #16823a;
    font-size: 12px;
    font-weight: 700;
}

.result-head h2 {
    color: var(--text-main);
    margin: 6px 0;
}

.result-head p,
.summary-grid span,
.day-notes,
.period-grid p {
    color: var(--text-secondary);
    line-height: 1.6;
}

.result-actions {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    justify-content: flex-end;
}

.result-actions button {
    padding: 9px 11px;
    border: 1px solid var(--primary-color);
    background: var(--card-bg);
    color: var(--primary-color);
}

.result-actions .danger {
    border-color: #dc3545;
    color: #dc3545;
}

.summary-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 18px;
}

.summary-grid div,
.day-card {
    background: var(--card-bg);
    border: 1px solid var(--border-color);
    border-radius: 8px;
}

.summary-grid div {
    padding: 14px;
}

.summary-grid strong,
.summary-grid span {
    display: block;
}

.summary-grid strong {
    color: var(--text-main);
    margin-bottom: 6px;
}

.day-card {
    margin-bottom: 18px;
    overflow: hidden;
}

.day-header {
    padding: 14px 18px;
    background: var(--primary-color);
    color: white;
    font-weight: 700;
}

.period-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
    padding: 16px;
}

.period-grid section {
    padding: 12px;
    background: var(--bg-color);
    border: 1px solid var(--border-color);
    border-radius: 8px;
}

.period-grid span {
    color: var(--primary-color);
    font-weight: 700;
}

.day-notes {
    display: grid;
    gap: 6px;
    padding: 0 16px 16px;
}

@media (max-width: 900px) {
    .custom-module {
        flex-direction: column;
    }

    .custom-sidebar {
        width: 100%;
        border-right: none;
        border-bottom: 1px solid var(--border-color);
    }

    .summary-grid,
    .period-grid {
        grid-template-columns: 1fr;
    }
}
</style>
