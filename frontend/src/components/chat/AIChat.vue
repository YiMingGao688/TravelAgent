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
    'importCustom',
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
            ? `我上传了一张图片${inputText.value.trim() ? '，' + inputText.value.trim() : ''}`
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
                        <button class="plan-btn ghost" @click="emit('importCustom', msg)">
                            <i class="fa-solid fa-route"></i>
                            <span>导入行程</span>
                        </button>
                        <button class="plan-btn ghost" @click="emit('importSmart', msg)">
                            <i class="fa-solid fa-briefcase-medical"></i>
                            <span>导入一站式服务</span>
                        </button>
                    </div>
                </div>
            </div>

            <div v-if="props.aiThinking" class="message-row ai">
                <div class="avatar"><span class="emoji-avatar">AI</span></div>
                <div class="message-content">
                    <div class="bubble">
                        <div class="typing-indicator"><span></span><span></span><span></span></div>
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
                    placeholder="告诉我您的出行目标，例如：从南京出发去杭州玩两天，预算 1500，偏舒适"
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
    width: 100%;
    max-width: 900px;
    display: flex;
    gap: 12px;
    margin-bottom: 24px;
}

.message-row.user {
    justify-content: flex-end;
}

.message-row.user .avatar {
    order: 2;
    background: var(--primary-color);
    color: white;
}

.avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: var(--card-bg);
    border: 1px solid var(--border-color);
    color: var(--primary-color);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    font-size: 12px;
    font-weight: 700;
}

.message-content {
    max-width: 76%;
    display: flex;
    flex-direction: column;
    position: relative;
}

.message-row.user .message-content {
    align-items: flex-end;
}

.bubble {
    padding: 16px 20px;
    border-radius: 18px;
    background: var(--card-bg);
    border: 1px solid var(--border-color);
    color: var(--text-main);
    line-height: 1.75;
    white-space: pre-wrap;
    word-break: break-word;
    box-shadow: var(--shadow-sm);
}

.message-row.user .bubble {
    background: var(--primary-color);
    color: white;
    border-color: var(--primary-color);
}

.structured-card {
    margin-top: 10px;
    padding: 12px;
    border: 1px solid var(--border-color);
    border-radius: 8px;
    background: var(--bg-color);
}

.structured-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
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
    gap: 8px;
}

.field-grid div {
    min-height: 58px;
    padding: 8px;
    border: 1px solid var(--border-color);
    border-radius: 8px;
    background: var(--card-bg);
}

.field-grid .wide {
    grid-column: span 3;
}

.field-grid strong {
    display: block;
    margin-top: 4px;
    color: var(--text-main);
    line-height: 1.4;
}

.time {
    margin-top: 6px;
    font-size: 12px;
    color: var(--text-secondary);
}

.plan-actions {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    margin-top: 10px;
}

.plan-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 7px 12px;
    border: none;
    border-radius: 8px;
    background: var(--primary-color);
    color: white;
    cursor: pointer;
    font-size: 13px;
}

.plan-btn.ghost {
    background: var(--card-bg);
    color: var(--primary-color);
    border: 1px solid var(--primary-color);
}

.typing-indicator {
    display: flex;
    gap: 4px;
}

.typing-indicator span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--text-secondary);
    animation: bounce 1.2s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

.upload-indicator {
    display: flex;
    align-items: center;
    gap: 8px;
}

.input-wrapper {
    flex-shrink: 0;
    padding: 16px 32px;
    border-top: 1px solid var(--border-color);
    background: var(--bg-color);
}

.input-box {
    max-width: 900px;
    margin: 0 auto;
    display: flex;
    align-items: flex-end;
    gap: 10px;
    padding: 12px 14px;
    border-radius: 18px;
    background: var(--card-bg);
    border: 1px solid var(--border-color);
    box-shadow: var(--shadow-md);
}

.input-box textarea {
    flex: 1;
    min-height: 28px;
    max-height: 120px;
    border: none;
    outline: none;
    resize: none;
    background: transparent;
    color: var(--text-main);
    font-size: 15px;
    line-height: 1.5;
    font-family: inherit;
}

.icon-btn,
.send-btn,
.stop-btn,
.image-preview button {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    border: none;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
}

.icon-btn,
.image-preview button {
    background: var(--bg-color);
    color: var(--text-secondary);
}

.send-btn {
    background: var(--primary-color);
    color: white;
}

.send-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.stop-btn {
    background: #dc3545;
    color: white;
}

.image-preview {
    max-width: 900px;
    margin: 0 auto 10px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    border: 1px solid var(--border-color);
    border-radius: 8px;
    background: var(--card-bg);
    color: var(--text-secondary);
    font-size: 14px;
}

@keyframes bounce {
    0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
    40% { transform: scale(1); opacity: 1; }
}

@media (max-width: 768px) {
    .chat-container,
    .input-wrapper {
        padding-left: 16px;
        padding-right: 16px;
    }

    .message-content {
        max-width: 86%;
    }

    .field-grid {
        grid-template-columns: 1fr;
    }

    .field-grid .wide {
        grid-column: auto;
    }
}
</style>
