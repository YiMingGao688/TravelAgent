<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
    visible: { type: Boolean, default: false },
    profile: {
        type: Object,
        default: () => ({
            city: '南京',
            transport: '不限',
            style: '舒适优先',
            note: ''
        })
    }
})

const emit = defineEmits(['close', 'save'])
const localProfile = ref({ ...props.profile })

watch(() => props.profile, value => {
    localProfile.value = { ...value }
}, { deep: true })

function handleSave() {
    emit('save', { ...localProfile.value })
}

const transportOptions = ['不限', '飞机', '高铁', '地铁/公交', '网约车', '自驾']
const styleOptions = ['舒适优先', '时间优先', '经济优先', '少换乘', '休闲度假', '深度人文', '自然风光', '亲子友好']
</script>

<template>
    <Teleport to="body">
        <div v-if="props.visible" class="profile-modal">
            <div class="modal-overlay" @click="emit('close')"></div>
            <div class="modal-content">
                <div class="modal-header">
                    <h3>出行偏好</h3>
                    <button class="close-btn" @click="emit('close')">
                        <i class="fa-solid fa-xmark"></i>
                    </button>
                </div>

                <div class="modal-body">
                    <div class="form-group">
                        <label>常驻城市</label>
                        <input type="text" class="form-control" v-model="localProfile.city" placeholder="例如：南京">
                        <p>当你没有说明起点时，AI 会优先把这里作为默认出发地。</p>
                    </div>

                    <div class="form-group">
                        <label>常用出行方式</label>
                        <select class="form-control" v-model="localProfile.transport">
                            <option v-for="option in transportOptions" :key="option" :value="option">{{ option }}</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label>出行偏好</label>
                        <select class="form-control" v-model="localProfile.style">
                            <option v-for="option in styleOptions" :key="option" :value="option">{{ option }}</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label>其他需求</label>
                        <textarea
                            class="form-control"
                            v-model="localProfile.note"
                            rows="3"
                            placeholder="例如：带老人出行、行李较多、不想走太多路"
                        ></textarea>
                    </div>
                </div>

                <div class="modal-footer">
                    <button class="btn-cancel" @click="emit('close')">取消</button>
                    <button class="btn-save" @click="handleSave">
                        <i class="fa-solid fa-check"></i> 保存
                    </button>
                </div>
            </div>
        </div>
    </Teleport>
</template>

<style scoped>
.profile-modal {
    position: fixed;
    inset: 0;
    z-index: 1000;
    display: flex;
    align-items: center;
    justify-content: center;
}

.modal-overlay {
    position: absolute;
    inset: 0;
    background: rgba(0, 0, 0, 0.55);
}

.modal-content {
    position: relative;
    width: 90%;
    max-width: 480px;
    background: var(--card-bg);
    border-radius: 12px;
    box-shadow: var(--shadow-lg);
    overflow: hidden;
}

.modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px 24px;
    border-bottom: 1px solid var(--border-color);
}

.modal-header h3 {
    font-size: 18px;
    color: var(--text-main);
}

.close-btn {
    width: 32px;
    height: 32px;
    border: none;
    border-radius: 50%;
    background: var(--bg-color);
    color: var(--text-secondary);
    cursor: pointer;
}

.modal-body {
    padding: 24px;
}

.form-group {
    margin-bottom: 18px;
}

.form-group label {
    display: block;
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 8px;
    color: var(--text-main);
}

.form-group p {
    margin-top: 6px;
    color: var(--text-secondary);
    font-size: 12px;
}

.form-control {
    width: 100%;
    padding: 12px 14px;
    background: var(--bg-color);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    color: var(--text-main);
    font-size: 14px;
    box-sizing: border-box;
    font-family: inherit;
}

.form-control:focus {
    outline: none;
    border-color: var(--primary-color);
}

textarea.form-control {
    resize: vertical;
    min-height: 80px;
}

.modal-footer {
    display: flex;
    gap: 12px;
    padding: 20px 24px;
    border-top: 1px solid var(--border-color);
}

.btn-cancel,
.btn-save {
    flex: 1;
    padding: 12px;
    border-radius: 8px;
    border: none;
    cursor: pointer;
    font-weight: 600;
}

.btn-cancel {
    background: var(--bg-color);
    color: var(--text-main);
    border: 1px solid var(--border-color);
}

.btn-save {
    background: var(--primary-color);
    color: white;
}
</style>
