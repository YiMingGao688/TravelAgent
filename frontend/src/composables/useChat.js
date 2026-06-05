import { ref, nextTick } from 'vue'
import { streamChat, parseSSEEvent, getChatHistory, getTripContext } from '../utils/api.js'
import { getCurrentTime } from '../utils/time.js'

const welcomeMessage = () => ({
    role: 'ai',
    isWelcome: true,
    content: '您好，我是七维智行助手。您可以告诉我目的地、出行时间、预算和偏好，我会结合您的出行偏好生成可执行的出行方案。',
    time: getCurrentTime()
})

export function useChat(aiChatRef) {
    const messages = ref([])
    const aiThinking = ref(false)
    const aiTyping = ref(false)
    const sessionId = ref('')
    const currentTripContext = ref(null)
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
            await loadCurrentTripContext()
            attachContextToLatestAi()
        } catch (error) {
            console.error('加载聊天历史失败:', error)
            messages.value = [welcomeMessage()]
        }
    }

    function startNewChat() {
        const newId = crypto.randomUUID()
        sessionId.value = newId
        sessionStorage.setItem('chatSessionId', newId)
        currentTripContext.value = null
        messages.value = [welcomeMessage()]
    }

    async function sendMessage(userMsg, userProfile = {}) {
        messages.value.push(userMsg)
        aiThinking.value = true
        stopped = false
        cancelTyping = null
        let index = -1
        let current = ''
        let pendingTripContext = null
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

                    if (eventType === 'context' && eventData) {
                        try {
                            pendingTripContext = JSON.parse(eventData)
                            currentTripContext.value = pendingTripContext
                            console.log('后端返回 tripContext = ', pendingTripContext)
                            if (index !== -1) {
                                messages.value[index].tripContext = pendingTripContext
                                messages.value[index].structuredPlan = toStructuredPlan(pendingTripContext, userMsg.content)
                            }
                        } catch (error) {
                            console.warn('解析 tripContext 失败:', error)
                        }
                        continue
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
                                tripContext: pendingTripContext,
                                structuredPlan: pendingTripContext ? toStructuredPlan(pendingTripContext, userMsg.content) : null,
                                isPlan: false
                            })
                        }
                        await appendTypewriter(eventData, value => {
                            current += value
                            messages.value[index].content = current
                            messages.value[index].isPlan = looksLikePlan(current)
                            if (currentTripContext.value) {
                                messages.value[index].tripContext = currentTripContext.value
                                messages.value[index].structuredPlan = toStructuredPlan(currentTripContext.value, userMsg.content)
                            }
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
        if (text.length < 40) return false
        if (/失败|错误|异常|不可用|未完成配置|No response body/i.test(text)) return false
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

    async function loadCurrentTripContext() {
        if (!sessionId.value) return null
        currentTripContext.value = await getTripContext(sessionId.value)
        return currentTripContext.value
    }

    function attachContextToLatestAi() {
        if (!currentTripContext.value) return
        const latestAi = [...messages.value].reverse().find(item => item.role === 'ai' && !item.isWelcome)
        if (!latestAi) return
        latestAi.tripContext = currentTripContext.value
        latestAi.structuredPlan = toStructuredPlan(currentTripContext.value, latestAi.rawQuestion)
        console.log('当前展示 identifiedTravelInfo = ', latestAi.structuredPlan)
    }

    function syncTripContextFromProfile(savedProfile = {}, previousProfile = {}) {
        const next = { ...(currentTripContext.value || {}) }
        const oldCity = previousProfile.city || ''
        if (!next.startPlace || next.startPlace === oldCity) {
            next.startPlace = savedProfile.city || next.startPlace || ''
        }
        if (!next.transportMode || next.transportMode === previousProfile.transport) {
            next.transportMode = savedProfile.transport || next.transportMode || ''
        }
        if (!next.preference || next.preference === previousProfile.style) {
            next.preference = savedProfile.style || next.preference || ''
        }
        currentTripContext.value = next
        attachContextToLatestAi()
        console.log('当前展示 identifiedTravelInfo = ', toStructuredPlan(next))
    }

    function toStructuredPlan(context, rawQuestion = '') {
        if (!context) return null
        return {
            startPlace: context.startPlace || '',
            endPlace: context.endPlace || '',
            scenario: context.sceneType || '',
            days: context.days || null,
            preference: context.preference || '',
            budget: context.budget || '',
            companions: context.companionType || '',
            transportMode: context.transportMode || '',
            pacePreference: context.pacePreference || '',
            walkingPreference: context.walkingPreference || '',
            hotelPreference: context.hotelPreference || '',
            foodPreference: context.foodPreference || '',
            specialNeeds: context.specialNeeds || '',
            rawQuestion,
            hasUsefulFields: Boolean(context.startPlace || context.endPlace || context.days || context.budget || context.preference || context.companionType || context.sceneType)
        }
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
        currentTripContext,
        syncTripContextFromProfile,
        loadCurrentTripContext,
        sendMessage,
        interrupt,
        startNewChat,
        init
    }
}
