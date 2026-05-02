<script setup>
/**
 * 旅游行程定制模块
 * 功能：根据用户选择生成多日游行程
 */
import { ref, nextTick } from 'vue'

/** 组件Props */
const props = defineProps({
    formData: {
        type: Object,
        default: () => ({
            destination: '云南大理',
            days: 3,
            budget: '舒适',
            style: '深度人文'
        })
    }
})

/** 组件事件 */
const emit = defineEmits(['update:formData', 'generate'])

/** 是否正在生成行程 */
const isGenerating = ref(false)

/** 生成的行程数据 */
const generatedItinerary = ref(null)

/** 行程内容区域的DOM引用 */
const customContentRef = ref(null)

/**
 * 生成旅游行程
 */
function generateItinerary() {
    if (!props.formData.destination) {
        alert('请填写您想去的目的地')
        return
    }

    isGenerating.value = true
    generatedItinerary.value = null

    emit('generate', {
        destination: props.formData.destination,
        days: props.formData.days,
        budget: props.formData.budget,
        style: props.formData.style
    })

    setTimeout(() => {
        const templates = [
            {
                theme: `初识 ${props.formData.destination}，感受当地风情`,
                activities: [
                    { time: '09:00', title: '抵达与安顿', desc: '前往预定好的特色民宿或酒店办理入住，稍作休整。' },
                    { time: '11:30', title: '寻味地道美食', desc: '在附近的特色街区品尝当地标志性的小吃和美食。' },
                    { time: '14:00', title: '漫步核心景区', desc: `沉浸式游览 ${props.formData.destination} 最著名的文化打卡点，拍照留念。` },
                    { time: '19:00', title: '夜景与晚风', desc: '晚上欣赏城市夜景，在清吧或小酒馆感受休闲时光。' }
                ]
            },
            {
                theme: `深入自然，探寻绝美风光`,
                activities: [
                    { time: '08:30', title: '丰盛早餐', desc: '品尝当地特色早点，开启元气满满的一天。' },
                    { time: '10:00', title: '自然景观探秘', desc: '前往郊外的山水、海滨或森林等自然风光名胜区游玩。' },
                    { time: '13:00', title: '观景餐厅午餐', desc: '在风景优美的餐厅享用午餐，边吃边赏景。' },
                    { time: '15:00', title: '户外体验项目', desc: '体验骑行、游船、短途徒步等当地特色的户外娱乐。' }
                ]
            },
            {
                theme: `自由闲散，慢节奏告别`,
                activities: [
                    { time: '09:30', title: '慵懒起床', desc: '睡到自然醒，享受一份惬意的早午餐(Brunch)。' },
                    { time: '11:00', title: '特产与文创', desc: '前往当地的手工艺品街道，购买纪念品和伴手礼。' },
                    { time: '14:30', title: '下午茶时光', desc: '找一家视野绝佳的咖啡店，回味这几天的精彩旅程。' },
                    { time: '17:00', title: '返程准备', desc: '整理行囊，专车接送至机场/高铁站，结束愉快的旅程。' }
                ]
            }
        ]

        const result = []
        for (let i = 0; i < props.formData.days; i++) {
            result.push(templates[i % 3])
        }

        generatedItinerary.value = result
        isGenerating.value = false

        nextTick(() => {
            if (customContentRef.value) {
                customContentRef.value.scrollTop = 0
            }
        })
    }, 2500)
}

/**
 * 暴露方法给父组件
 */
defineExpose({
    generatedItinerary,
    isGenerating,
    customContentRef
})
</script>

<template>
    <div class="custom-module">
        <!-- 左侧配置表单 -->
        <div class="custom-sidebar">
            <div class="transit-title">
                <i class="fa-solid fa-route"></i> 定制专属行程
            </div>

            <div class="form-group" style="margin-top: 10px;">
                <label>你想去哪儿玩？</label>
                <input
                    type="text"
                    class="form-control"
                    :value="formData.destination"
                    @input="emit('update:formData', { ...formData, destination: $event.target.value })"
                    placeholder="如：云南大理、三亚"
                >
            </div>

            <div class="form-group">
                <label>游玩天数</label>
                <input
                    type="number"
                    class="form-control"
                    :value="formData.days"
                    @input="emit('update:formData', { ...formData, days: parseInt($event.target.value) || 1 })"
                    min="1"
                    max="15"
                >
            </div>

            <div class="form-group">
                <label>人均预算</label>
                <select
                    class="form-control"
                    :value="formData.budget"
                    @change="emit('update:formData', { ...formData, budget: $event.target.value })"
                >
                    <option value="经济">经济穷游 (￥1000以内)</option>
                    <option value="舒适">舒适轻奢 (￥1000-3000)</option>
                    <option value="豪华">尊贵豪华 (￥3000以上)</option>
                </select>
            </div>

            <div class="form-group">
                <label>行程偏好</label>
                <select
                    class="form-control"
                    :value="formData.style"
                    @change="emit('update:formData', { ...formData, style: $event.target.value })"
                >
                    <option value="深度人文">深度人文</option>
                    <option value="自然风光">自然风光</option>
                    <option value="特种兵打卡">特种兵打卡</option>
                    <option value="亲子家庭">亲子休闲</option>
                </select>
            </div>

            <button
                class="btn-booking"
                @click="generateItinerary"
                :disabled="isGenerating"
            >
                <i class="fa-solid fa-wand-magic-sparkles"></i>
                {{ isGenerating ? 'AI 正在深度规划中...' : '生成定制行程' }}
            </button>
        </div>

        <!-- 右侧行程展示 -->
        <div class="custom-content" ref="customContentRef">
            <!-- 空状态 -->
            <div v-if="!generatedItinerary && !isGenerating" class="empty-module">
                <i class="fa-solid fa-map-location-dot"></i>
                <p>请在左侧填写信息，AI 将为您量身定制专属旅游路线</p>
            </div>

            <!-- 加载中状态 -->
            <div v-if="isGenerating" class="empty-module">
                <div class="spinner-custom"></div>
                <p style="margin-top: 20px;">AI 正在搜集当地交通、景点与美食，为您编排最优路线...</p>
            </div>

            <!-- 行程结果 -->
            <div v-if="generatedItinerary && !isGenerating" class="itinerary-result">
                <h2>
                    {{ formData.destination }}
                    {{ formData.days }}日
                    {{ formData.style }} 专属定制游
                </h2>

                <div class="day-card" v-for="(day, index) in generatedItinerary" :key="index">
                    <div class="day-header">第 {{ index + 1 }} 天：{{ day.theme }}</div>
                    <div class="day-timeline">
                        <div class="timeline-item" v-for="(act, aIndex) in day.activities" :key="aIndex">
                            <div class="time">{{ act.time }}</div>
                            <div class="timeline-content">
                                <h4>{{ act.title }}</h4>
                                <p>{{ act.desc }}</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
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
    width: 340px;
    background: var(--card-bg);
    border-right: 1px solid var(--border-color);
    padding: 24px;
    overflow-y: auto;
    flex-shrink: 0;
}

.transit-title {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--primary-color);
}

.form-group {
    margin-bottom: 16px;
}

.form-group label {
    display: block;
    font-size: 13px;
    font-weight: 500;
    margin-bottom: 8px;
    color: var(--text-secondary);
}

.form-control {
    width: 100%;
    padding: 12px 14px;
    background: var(--bg-color);
    border: 1px solid var(--border-color);
    border-radius: 10px;
    color: var(--text-main);
    font-size: 14px;
    outline: none;
    transition: all 0.2s;
}

.form-control:focus {
    border-color: var(--primary-color);
    box-shadow: 0 0 0 3px rgba(0,122,255,0.1);
}

.btn-booking {
    width: 100%;
    padding: 14px;
    background: var(--primary-color);
    color: white;
    border: none;
    border-radius: 12px;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 8px;
    margin-top: 16px;
    transition: transform 0.2s;
    box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

.btn-booking:hover:not(:disabled) {
    transform: translateY(-2px);
}

.btn-booking:disabled {
    opacity: 0.7;
    cursor: not-allowed;
}

.custom-content {
    flex: 1;
    overflow-y: auto;
    padding: 24px;
    background: var(--bg-color);
}

.empty-module {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 60vh;
    color: var(--text-secondary);
    font-size: 16px;
}

.empty-module i {
    font-size: 64px;
    margin-bottom: 20px;
    color: var(--border-color);
}

.spinner-custom {
    width: 48px;
    height: 48px;
    border: 4px solid var(--border-color);
    border-top-color: var(--primary-color);
    border-radius: 50%;
    animation: spin 1s linear infinite;
}

@keyframes spin {
    to { transform: rotate(360deg); }
}

.itinerary-result h2 {
    font-size: 22px;
    font-weight: 600;
    color: var(--text-main);
    margin-bottom: 24px;
    text-align: center;
}

.day-card {
    background: var(--card-bg);
    border-radius: 16px;
    margin-bottom: 20px;
    overflow: hidden;
    border: 1px solid var(--border-color);
}

.day-header {
    background: linear-gradient(135deg, var(--primary-color), #5862d6);
    color: white;
    padding: 14px 20px;
    font-size: 15px;
    font-weight: 600;
}

.day-timeline {
    padding: 16px 20px;
}

.timeline-item {
    display: flex;
    gap: 16px;
    margin-bottom: 16px;
    position: relative;
}

.timeline-item::before {
    content: '';
    position: absolute;
    left: 35px;
    top: 32px;
    bottom: -8px;
    width: 2px;
    background: var(--border-color);
}

.timeline-item:last-child {
    margin-bottom: 0;
}

.timeline-item:last-child::before {
    display: none;
}

.timeline-item .time {
    width: 50px;
    font-size: 12px;
    color: var(--primary-color);
    font-weight: 600;
    flex-shrink: 0;
    text-align: center;
    padding-top: 2px;
}

.timeline-item .timeline-content {
    flex: 1;
    background: var(--bg-color);
    border-radius: 10px;
    padding: 12px 16px;
    border: 1px solid var(--border-color);
}

.timeline-item h4 {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-main);
    margin-bottom: 4px;
}

.timeline-item p {
    font-size: 13px;
    color: var(--text-secondary);
    line-height: 1.5;
}

@media (max-width: 768px) {
    .custom-module {
        flex-direction: column;
    }
    
    .custom-sidebar {
        width: 100%;
        border-right: none;
        border-bottom: 1px solid var(--border-color);
    }
    
    .custom-content {
        padding: 16px;
    }
}
</style>