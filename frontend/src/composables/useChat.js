import { ref, nextTick } from 'vue'
import { streamChat, parseSSEEvent, getChatHistory } from '../utils/api.js'
import { getCurrentTime } from '../utils/time.js'

const welcomeMessage = () => ({
    role: 'ai',
    isWelcome: true,
    content: '您好，我是您的智能出行助手。您可以告诉我目的地、出行时间、预算和偏好，我会结合您填写的出行偏好生成可执行的出行方案。',
    time: getCurrentTime()
})

export function useChat(aiChatRef) {
    const messages = ref([])
    const aiThinking = ref(false)
    const aiTyping = ref(false)
    const sessionId = ref('')
    let cancelTyping = null
    let stopped = false

    function initSessionId() {
        let id = sessionStorage.getItem('chatSessionId')
        if (!id) {
            id = crypto.randomUUID()
            sessionStorage.setItem('chatSessionId', id)
        }
        sessionId.value = id
    }

    async function loadHistory() {
        if (!sessionId.value) return

        try {
            const userId = sessionStorage.getItem('loginUser')
            if (!userId) {
                messages.value = [welcomeMessage()]
                return
            }

            const history = await getChatHistory(sessionId.value, userId)
            if (!history.length) {
                messages.value = [welcomeMessage()]
                return
            }

            messages.value = history.flatMap(record => ([
                { role: 'user', content: record.userMessage, time: formatTime(record.createTime) },
                {
                    role: 'ai',
                    content: record.aiResponse,
                    time: formatTime(record.createTime),
                    sessionId: record.sessionId,
                    rawQuestion: record.userMessage,
                    isPlan: looksLikePlan(record.aiResponse)
                }
            ]))
        } catch (error) {
            console.error('加载聊天历史失败:', error)
            messages.value = [welcomeMessage()]
        }
    }

    function startNewChat() {
        const newId = crypto.randomUUID()
        sessionId.value = newId
        sessionStorage.setItem('chatSessionId', newId)
        messages.value = [welcomeMessage()]
    }

    async function sendMessage(userMsg, userProfile = {}) {
        messages.value.push(userMsg)
        aiThinking.value = true
        stopped = false
        cancelTyping = null
        let index = -1
        let current = ''
        const userId = sessionStorage.getItem('loginUser')
        const controller = new AbortController()

        try {
            const response = await streamChat({
                sessionId: sessionId.value,
                userId,
                message: userMsg.content,
                city: userProfile.city,
                transport: userProfile.transport,
                style: userProfile.style,
                note: userProfile.note
            }, controller.signal)

            if (!response.body) {
                throw new Error('No response body')
            }

            aiThinking.value = false
            aiTyping.value = true
            cancelTyping = () => controller.abort()

            const reader = response.body.getReader()
            const decoder = new TextDecoder()
            let buffer = ''
            let finished = false

            while (!finished) {
                const { done, value } = await reader.read()
                if (done) break

                buffer += decoder.decode(value, { stream: true })
                const eventBlocks = buffer.split('\n\n')

                for (let i = 0; i < eventBlocks.length - 1; i++) {
                    const { eventType, eventData } = parseSSEEvent(eventBlocks[i])

                    if (eventType === 'done' || eventData === '[DONE]') {
                        finished = true
                        break
                    }

                    if (eventData && !stopped) {
                        if (index === -1) {
                            index = messages.value.length
                            messages.value.push({
                                role: 'ai',
                                content: '',
                                time: getCurrentTime(),
                                sessionId: sessionId.value,
                                rawQuestion: userMsg.content,
                                isPlan: false
                            })
                        }
                        await appendTypewriter(eventData, value => {
                            current += value
                            messages.value[index].content = current
                            messages.value[index].isPlan = looksLikePlan(current)
                        })
                    }
                }

                buffer = eventBlocks[eventBlocks.length - 1]
            }
        } catch (error) {
            if (error.name !== 'AbortError') {
                const text = `AI 服务暂时不可用，请检查后端或云端大模型配置。${error.message || ''}`
                if (index === -1) {
                    messages.value.push({ role: 'ai', content: text, time: getCurrentTime(), isPlan: false })
                } else {
                    messages.value[index].content = text
                    messages.value[index].isPlan = false
                }
            }
        } finally {
            aiThinking.value = false
            aiTyping.value = false
            cancelTyping = null
        }
    }

    function looksLikePlan(content) {
        const text = String(content || '')
        if (text.length < 60) return false
        if (/失败|错误|异常|不可用|未完成配置|请检查后端|No response body/i.test(text)) return false
        return /方案|路线|行程|出行|交通|换乘|推荐|预算|耗时|目的地|起点|终点/.test(text)
    }

    function interrupt() {
        stopped = true
        if (cancelTyping) {
            cancelTyping()
        }
        aiThinking.value = false
        aiTyping.value = false
    }

    async function appendTypewriter(text, append) {
        for (const char of String(text)) {
            if (stopped) return
            append(char)
            await nextTick()
            scrollToBottom()
            await delay(10)
        }
    }

    function delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms))
    }

    function scrollToBottom() {
        if (aiChatRef?.value?.scrollToBottom) {
            aiChatRef.value.scrollToBottom()
        }
    }

    function init() {
        initSessionId()
        loadHistory()
    }

    function formatTime(value) {
        if (!value) return getCurrentTime()
        return String(value).replace('T', ' ').slice(0, 16)
    }

    return {
        messages,
        aiThinking,
        aiTyping,
        sessionId,
        sendMessage,
        interrupt,
        startNewChat,
        init
    }
}
