<script setup>
import { ref, nextTick } from 'vue'

const props = defineProps({
    initialMessages: { type: Array, default: () => [] },
    aiThinking: { type: Boolean, default: false },
    aiTyping: { type: Boolean, default: false },
    imageUploading: { type: Boolean, default: false }
})

const emit = defineEmits([
    'sendMessage',
    'scrollBottom',
    'interrupt',
    'savePlan',
    'importTransit',
    'importSmart'
])

const inputText = ref('')
const chatRef = ref(null)
const imageInputRef = ref(null)
const uploadedImage = ref(null)

function getCurrentTime() {
    return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

async function scrollToBottom() {
    await nextTick()
    if (chatRef.value) {
        chatRef.value.scrollTop = chatRef.value.scrollHeight
    }
    emit('scrollBottom')
}

async function sendMessage() {
    if ((!inputText.value.trim() && !uploadedImage.value) || props.aiThinking || props.aiTyping) return

    const userMessage = {
        role: 'user',
        content: uploadedImage.value
            ? `我上传了一张图片${inputText.value.trim() ? '：' + inputText.value.trim() : ''}`
            : inputText.value.trim(),
        time: getCurrentTime(),
        hasImage: !!uploadedImage.value,
        imageFile: uploadedImage.value
    }

    emit('sendMessage', userMessage)
    inputText.value = ''
    uploadedImage.value = null
    if (imageInputRef.value) imageInputRef.value.value = ''
    await scrollToBottom()
}

function handleKeydown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        sendMessage()
    }
}

function handleImageChange(event) {
    uploadedImage.value = event.target.files?.[0] || null
}

function clearInput() {
    inputText.value = ''
    uploadedImage.value = null
    if (imageInputRef.value) imageInputRef.value.value = ''
}

function formatContent(content) {
    if (!content) return ''
    return String(content)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;')
        .replace(/\r\n|\n|\r/g, '<br>')
}

function isActionablePlan(msg) {
    if (!msg || msg.role !== 'ai' || msg.isWelcome) return false
    if (msg.structuredPlan?.hasUsefulFields) return true
    if (msg.isPlan === true) return true
    const text = String(msg.content || '')
    if (text.length < 40) return false
    if (/失败|错误|异常|不可用|未完成配置|No response body/i.test(text)) return false
    return /方案|路线|行程|出行|交通|换乘|推荐|预算|耗时|目的地|起点|终点/.test(text)
}

function fieldValue(plan, key) {
    const value = plan?.[key]
    return value === null || value === undefined || value === '' ? '未识别' : value
}

defineExpose({ scrollToBottom, chatRef })
</script>

<template>
    <div class="ai-chat-module">
        <div class="chat-container" ref="chatRef">
            <div v-for="(msg, index) in initialMessages" :key="index" :class="['message-row', msg.role]">
                <div class="avatar">
                    <span v-if="msg.role === 'ai'" class="emoji-avatar">AI</span>
                    <i v-else class="fa-solid fa-user"></i>
                </div>
                <div class="message-content" :class="{ 'has-actions': isActionablePlan(msg) }">
                    <div class="bubble" :class="{ 'ai-message': msg.role === 'ai' }" v-html="formatContent(msg.content)"></div>

                    <div v-if="isActionablePlan(msg) && msg.structuredPlan" class="structured-card">
                        <div class="structured-head">
                            <strong>已识别出行信息</strong>
                            <span>用于保存与导入</span>
                        </div>
                        <div class="field-grid">
                            <div><span>起点</span><strong>{{ fieldValue(msg.structuredPlan, 'startPlace') }}</strong></div>
                            <div><span>终点</span><strong>{{ fieldValue(msg.structuredPlan, 'endPlace') }}</strong></div>
                            <div><span>场景</span><strong>{{ fieldValue(msg.structuredPlan, 'scenario') }}</strong></div>
                            <div><span>天数</span><strong>{{ fieldValue(msg.structuredPlan, 'days') }}</strong></div>
                            <div><span>偏好</span><strong>{{ fieldValue(msg.structuredPlan, 'preference') }}</strong></div>
                            <div><span>预算</span><strong>{{ fieldValue(msg.structuredPlan, 'budget') }}</strong></div>
                            <div class="wide"><span>同行人群</span><strong>{{ fieldValue(msg.structuredPlan, 'companions') }}</strong></div>
                        </div>
                    </div>

                    <div class="time">{{ msg.time }}</div>
                    <div v-if="isActionablePlan(msg)" class="plan-actions">
                        <button class="plan-btn" @click="emit('savePlan', msg)">
                            <i class="fa-solid fa-save"></i>
                            <span>保存方案</span>
                        </button>
                        <button class="plan-btn ghost" @click="emit('importTransit', msg)">
                            <i class="fa-solid fa-map-location-dot"></i>
                            <span>导入交通</span>
                        </button>
                    </div>
                </div>
            </div>

            <div v-if="props.aiThinking" class="message-row ai">
                <div class="avatar"><span class="emoji-avatar">AI</span></div>
                <div class="message-content">
                    <div class="bubble thinking-bubble">
                        <div class="thinking-head">
                            <div class="thinking-orbit">
                                <span></span><span></span><span></span>
                            </div>
                            <strong>正在理解您的出行需求</strong>
                        </div>
                        <div class="thinking-steps">
                            <span>识别起点终点</span>
                            <span>结合偏好与预算</span>
                            <span>整理可执行方案</span>
                        </div>
                    </div>
                </div>
            </div>

            <div v-if="props.imageUploading" class="message-row ai">
                <div class="avatar"><span class="emoji-avatar">AI</span></div>
                <div class="message-content">
                    <div class="bubble upload-indicator">
                        <i class="fa-solid fa-spinner fa-spin"></i>
                        <span>正在识别图片...</span>
                    </div>
                </div>
            </div>
        </div>

        <div class="input-wrapper">
            <div v-if="uploadedImage" class="image-preview">
                <span><i class="fa-solid fa-image"></i> {{ uploadedImage.name }}</span>
                <button @click="uploadedImage = null" title="移除图片"><i class="fa-solid fa-xmark"></i></button>
            </div>
            <div class="input-box">
                <button class="icon-btn" @click="imageInputRef.click()" title="上传图片">
                    <i class="fa-solid fa-paperclip"></i>
                </button>
                <input ref="imageInputRef" type="file" accept="image/*" style="display:none" @change="handleImageChange" />
                <textarea
                    v-model="inputText"
                    @keydown="handleKeydown"
                    placeholder="告诉我您的出行目标，例如：从南京出发去杭州玩两天，预算1500，偏舒适"
                ></textarea>
                <button class="icon-btn" @click="clearInput" title="清空">
                    <i class="fa-solid fa-trash-can"></i>
                </button>
                <button v-if="props.aiTyping" class="stop-btn" @click="emit('interrupt')" title="停止生成">
                    <i class="fa-solid fa-stop"></i>
                </button>
                <button v-else class="send-btn" @click="sendMessage" :disabled="(!inputText.trim() && !uploadedImage) || props.aiThinking" title="发送">
                    <i class="fa-solid fa-paper-plane"></i>
                </button>
            </div>
        </div>
    </div>
</template>

<style scoped>
.ai-chat-module {
    display: flex;
    flex-direction: column;
    height: 100%;
    overflow: hidden;
}

.chat-container {
    flex: 1;
    overflow-y: auto;
    padding: 24px 32px 40px;
    display: flex;
    flex-direction: column;
    align-items: center;
}

.message-row {
    width: min(920px, 100%);
    display: flex;
    gap: 12px;
    margin-bottom: 20px;
}

.message-row.user {
    flex-direction: row-reverse;
}

.avatar {
    width: 36px;
    height: 36px;
    flex: 0 0 36px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--primary-light);
    color: var(--primary-color);
    font-size: 13px;
    font-weight: 800;
}

.message-row.user .avatar {
    background: var(--primary-color);
    color: white;
}

.message-content {
    max-width: 78%;
}

.bubble {
    padding: 13px 16px;
    border-radius: 14px;
    background: var(--card-bg);
    border: 1px solid var(--border-color);
    color: var(--text-main);
    line-height: 1.75;
    font-size: 14px;
    box-shadow: var(--shadow-sm);
}

.message-row.user .bubble {
    background: var(--primary-color);
    color: white;
    border-color: var(--primary-color);
}

.time {
    margin-top: 6px;
    color: var(--text-secondary);
    font-size: 11px;
}

.structured-card {
    margin-top: 10px;
    padding: 14px;
    border: 1px solid var(--border-color);
    border-radius: 12px;
    background: var(--card-bg);
}

.structured-head {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
}

.structured-head strong {
    color: var(--text-main);
}

.structured-head span,
.field-grid span {
    color: var(--text-secondary);
    font-size: 12px;
}

.field-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
}

.field-grid div {
    padding: 10px;
    border-radius: 8px;
    background: var(--bg-color);
}

.field-grid strong,
.field-grid span {
    display: block;
}

.field-grid strong {
    margin-top: 5px;
    color: var(--text-main);
}

.field-grid .wide {
    grid-column: span 3;
}

.plan-actions {
    display: flex;
    gap: 8px;
    margin-top: 10px;
}

.plan-btn {
    border: 0;
    border-radius: 8px;
    padding: 9px 12px;
    display: inline-flex;
    align-items: center;
    gap: 7px;
    background: var(--primary-color);
    color: white;
    cursor: pointer;
}

.plan-btn.ghost {
    background: var(--primary-light);
    color: var(--primary-color);
}

.thinking-bubble,
.upload-indicator {
    display: grid;
    gap: 10px;
}

.thinking-head {
    display: flex;
    align-items: center;
    gap: 10px;
}

.thinking-orbit {
    display: flex;
    gap: 4px;
}

.thinking-orbit span {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--primary-color);
    animation: pulse 1s infinite alternate;
}

.thinking-orbit span:nth-child(2) {
    animation-delay: 0.15s;
}

.thinking-orbit span:nth-child(3) {
    animation-delay: 0.3s;
}

.thinking-steps {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.thinking-steps span {
    padding: 5px 8px;
    border-radius: 999px;
    background: var(--primary-light);
    color: var(--primary-color);
    font-size: 12px;
}

@keyframes pulse {
    from { opacity: 0.35; transform: translateY(0); }
    to { opacity: 1; transform: translateY(-3px); }
}

.input-wrapper {
    padding: 14px 32px 20px;
    border-top: 1px solid var(--border-color);
    background: var(--card-bg);
}

.image-preview {
    width: min(920px, 100%);
    margin: 0 auto 8px;
    padding: 8px 10px;
    border-radius: 8px;
    display: flex;
    justify-content: space-between;
    color: var(--text-secondary);
    background: var(--bg-color);
}

.image-preview button,
.icon-btn,
.send-btn,
.stop-btn {
    border: 0;
    cursor: pointer;
}

.input-box {
    width: min(920px, 100%);
    margin: 0 auto;
    display: flex;
    align-items: flex-end;
    gap: 8px;
    padding: 8px;
    border: 1px solid var(--border-color);
    border-radius: 14px;
    background: var(--bg-color);
}

.input-box textarea {
    flex: 1;
    min-height: 42px;
    max-height: 110px;
    border: 0;
    outline: none;
    resize: none;
    background: transparent;
    color: var(--text-main);
    line-height: 1.5;
}

.icon-btn,
.send-btn,
.stop-btn {
    width: 38px;
    height: 38px;
    border-radius: 10px;
    display: grid;
    place-items: center;
}

.icon-btn {
    background: transparent;
    color: var(--text-secondary);
}

.send-btn {
    background: var(--primary-color);
    color: white;
}

.send-btn:disabled {
    opacity: 0.45;
    cursor: not-allowed;
}

.stop-btn {
    background: #dc3545;
    color: white;
}

@media (max-width: 760px) {
    .chat-container {
        padding: 18px 16px;
    }

    .message-content {
        max-width: calc(100% - 48px);
    }

    .field-grid {
        grid-template-columns: 1fr;
    }

    .field-grid .wide {
        grid-column: auto;
    }

    .input-wrapper {
        padding: 12px 16px;
    }
}
</style>
