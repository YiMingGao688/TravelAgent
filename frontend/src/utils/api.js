import axios from 'axios'

const CHAT_API_BASE = '/api/chat'

/**
 * 统一的 fetch 请求入口。
 * 后端接口约定返回 Result(code/message/data)，这里集中处理错误码，页面层只关心业务数据。
 */
async function request(url, options = {}) {
    const response = await fetch(url, {
        headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
        credentials: 'include',
        ...options
    })
    const result = await response.json()
    if (result.code && result.code !== 200) {
        throw new Error(result.message || result.msg || '请求失败')
    }
    return result
}

export async function loginUser(payload) {
    const result = await request('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(payload)
    })
    return result.data
}

export async function registerUser(payload) {
    const result = await request('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(payload)
    })
    return result.data
}

export async function getCurrentUser() {
    const result = await request('/api/auth/me')
    return result.data
}

export async function logoutUser() {
    return request('/api/auth/logout', { method: 'POST' })
}

/**
 * axios 删除/更新类请求的统一结果校验。
 * 保留 code=200、code=1、success=true 三种兼容判断，是为了兼容项目不同阶段已有接口返回格式。
 */
function handleAxiosResult(response) {
    const result = response.data
    if (result?.code && result.code !== 200 && result.code !== 1 && result.success !== true) {
        throw new Error(result.message || result.msg || '删除失败')
    }
    return result
}

export async function saveProfile(profile) {
    return request(`${CHAT_API_BASE}/profile/save`, {
        method: 'POST',
        body: JSON.stringify(profile)
    })
}

export async function getProfile(userId) {
    return request(`${CHAT_API_BASE}/profile/get?userId=${encodeURIComponent(userId)}`)
}

export async function getChatHistory(sessionId, userId) {
    const result = await request(`${CHAT_API_BASE}/history?sessionId=${encodeURIComponent(sessionId)}&userId=${encodeURIComponent(userId || '')}`)
    return result.data || []
}

export async function getTripContext(sessionId) {
    if (!sessionId) return null
    const result = await request(`/api/trip-context/current?sessionId=${encodeURIComponent(sessionId)}`)
    return result.data || null
}

export function streamChat(params, signal) {
    // AI 咨询的 SSE 流式接口，前端会逐段接收 message/context/done 事件实现打字机效果。
    return fetch(`${CHAT_API_BASE}/stream`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream'
        },
        body: JSON.stringify(params),
        signal
    })
}

export function parseSSEEvent(data) {
    // 将浏览器读到的 SSE 文本块解析成事件名和数据体，供 useChat 组合式函数分发处理。
    const lines = data.split('\n')
    let eventType = ''
    let eventData = ''

    for (const line of lines) {
        if (line.startsWith('event:')) {
            eventType = line.substring(6).trim()
        } else if (line.startsWith('data:')) {
            const dataContent = line.substring(5)
            eventData += eventData ? `\n${dataContent}` : dataContent
        }
    }

    return { eventType, eventData }
}

export async function streamAgentTripPlan(payload, handlers = {}, signal) {
    const response = await fetch('/api/agent/trip/plan/stream', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream'
        },
        credentials: 'include',
        body: JSON.stringify(payload),
        signal
    })
    if (!response.ok || !response.body) {
        throw new Error('智能体规划流启动失败')
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    while (true) {
        const { value, done } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const chunks = buffer.split(/\n\n/)
        buffer = chunks.pop() || ''
        for (const chunk of chunks) {
            const { eventType, eventData } = parseSSEEvent(chunk)
            if (!eventType || !eventData) continue
            let data = eventData
            try {
                data = JSON.parse(eventData)
            } catch (error) {
                // 非 JSON 事件保持原文本，便于兼容后端错误消息。
            }
            handlers.onEvent?.(eventType, data)
        }
    }
}

export async function saveTripPlan(plan) {
    return request('/api/trip/save', {
        method: 'POST',
        body: JSON.stringify(plan)
    })
}

export async function listTripPlans(userId) {
    const result = await request(`/api/trip/list?userId=${encodeURIComponent(userId)}`)
    return result.data || []
}

export async function deleteTripPlan(id) {
    return handleAxiosResult(await axios.delete(`/api/trip/${id}`))
}

export async function queryRoutes(payload) {
    // 交通规划真实调用后端：后端会对接高德地理编码、路线、天气，并返回路线列表。
    const result = await request('/api/route/query', {
        method: 'POST',
        body: JSON.stringify(payload)
    })
    return result.data || { routes: [] }
}

export async function generateItinerary(payload) {
    const result = await request('/api/itinerary/generate', {
        method: 'POST',
        body: JSON.stringify(payload)
    })
    return result.data
}

export async function adminLogin(payload) {
    return request('/api/admin/login', {
        method: 'POST',
        body: JSON.stringify(payload)
    })
}

export async function adminGet(path) {
    return request(`/api/admin/${path}`)
}

export async function adminDelete(path, id) {
    return handleAxiosResult(await axios.delete(`/api/admin/${path}/${id}`))
}

export async function adminCreate(path, payload) {
    return request(`/api/admin/${path}`, {
        method: 'POST',
        body: JSON.stringify(payload)
    })
}

export async function adminUpdate(path, id, payload) {
    return request(`/api/admin/${path}/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
    })
}

export async function generateSmartService(payload) {
    return request('/api/smart-service/generate', {
        method: 'POST',
        body: JSON.stringify(payload)
    })
}

export async function generateTravelReport(payload) {
    return request('/api/smart-service/report', {
        method: 'POST',
        body: JSON.stringify(payload)
    })
}

export async function listSmartServices(userId) {
    const result = await request(`/api/smart-service/list?userId=${encodeURIComponent(userId)}`)
    return result.data || []
}

export async function saveServiceReport(payload) {
    // 一站式服务报告保存到数据库，历史出行服务报告页面会从对应接口重新拉取。
    const result = await request('/api/service-report/save', {
        method: 'POST',
        body: JSON.stringify(payload)
    })
    return result.data
}

export async function listServiceReports(userId, keyword = '') {
    const result = await request(`/api/service-report/list?userId=${encodeURIComponent(userId)}&keyword=${encodeURIComponent(keyword)}`)
    return result.data || []
}

export async function getServiceReport(id) {
    const result = await request(`/api/service-report/${id}`)
    return result.data
}

export async function deleteServiceReport(id) {
    return handleAxiosResult(await axios.delete(`/api/service-report/${id}`))
}

export async function searchHotels(params = {}) {
    const normalizedParams = { ...params }
    if (!normalizedParams.city && normalizedParams.cityName) normalizedParams.city = normalizedParams.cityName
    if (!normalizedParams.keyword && normalizedParams.destinationKeyword) normalizedParams.keyword = normalizedParams.destinationKeyword
    delete normalizedParams.cityName
    delete normalizedParams.destinationKeyword

    const query = new URLSearchParams()
    Object.entries(normalizedParams).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') query.set(key, value)
    })
    const result = await request(`/api/booking-hotels/search?${query.toString()}`)
    return result.data || []
}

export async function getLocationTips(keyword) {
    if (!keyword) return []
    const result = await request(`/api/location/tips?keyword=${encodeURIComponent(keyword)}`)
    return result.data || []
}

/**
 * 带防抖和请求取消的地点提示查询。
 * - 500ms 防抖
 * - keyword < 2 不请求
 * - 相同 keyword 不重复请求
 * - 新请求自动取消上一次未完成的请求
 * - 30 秒简单内存缓存
 */
export function createDebouncedLocationTips() {
    let timer = null
    let controller = null
    let lastKeyword = ''
    const cache = new Map() // key -> { data, expireAt }

    return function debouncedGetTips(keyword) {
        return new Promise((resolve, reject) => {
            clearTimeout(timer)

            const kw = (keyword || '').trim()
            if (kw.length < 2) {
                resolve([])
                return
            }

            // 与上一次完全相同则不请求
            if (kw === lastKeyword) {
                // 但需要等缓存；下面走缓存路径
            }

            timer = setTimeout(async () => {
                // 取消上一次未完成的请求
                if (controller) {
                    controller.abort()
                    controller = null
                }

                lastKeyword = kw

                // 检查缓存（30 秒）
                const cached = cache.get(kw)
                if (cached && Date.now() < cached.expireAt) {
                    resolve(cached.data)
                    return
                }

                controller = new AbortController()
                const signal = controller.signal

                try {
                    const response = await fetch(
                        `/api/location/tips?keyword=${encodeURIComponent(kw)}`,
                        {
                            headers: { 'Content-Type': 'application/json' },
                            credentials: 'include',
                            signal
                        }
                    )
                    const result = await response.json()
                    if (result.code && result.code !== 200) {
                        throw new Error(result.message || '地点查询失败')
                    }
                    const data = result.data || []

                    // 缓存 30 秒
                    cache.set(kw, { data, expireAt: Date.now() + 30_000 })

                    resolve(data)
                } catch (err) {
                    if (err.name === 'AbortError') {
                        // 请求被取消，不 reject
                        resolve([])
                        return
                    }
                    // 如果是高德限流（后端返回的 message 中包含关键词）
                    reject(err)
                } finally {
                    if (controller && controller.signal === signal) {
                        controller = null
                    }
                }
            }, 500)
        })
    }
}

export async function getHotelDetail(id) {
    const result = await request(`/api/booking-hotels/${id}`)
    return result.data
}

export async function createHotelOrder(payload) {
    const result = await request('/api/hotels/orders', {
        method: 'POST',
        body: JSON.stringify(payload)
    })
    return result.data
}

export async function searchTraffic(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') query.set(key, value)
    })
    const result = await request(`/api/traffic/search?${query.toString()}`)
    return result.data || []
}

export async function createBookingOrder(payload) {
    const result = await request('/api/orders/create', {
        method: 'POST',
        body: JSON.stringify(payload)
    })
    return result.data
}

export async function listUserOrders(userId) {
    if (!userId) return []
    const result = await request(`/api/orders/user/${encodeURIComponent(userId)}`)
    return result.data || []
}

export async function payBookingOrder(id) {
    const result = await request(`/api/orders/${id}/pay`, { method: 'PUT' })
    return result.data
}

export async function cancelBookingOrder(id) {
    const result = await request(`/api/orders/${id}/cancel`, { method: 'PUT' })
    return result.data
}

export async function askAssistant(payload) {
    const result = await request('/api/assistant/ask', {
        method: 'POST',
        body: JSON.stringify(payload)
    })
    return result.data
}

export async function getWeather(city) {
    const result = await request(`/api/weather?city=${encodeURIComponent(city)}`)
    return result.data
}

export async function planTravelChain(payload) {
    const result = await request('/api/travel-chain/plan', {
        method: 'POST',
        body: JSON.stringify(payload)
    })
    return result.data
}

export async function startAgentPlan(payload) {
    const result = await request('/api/agent/plan/start', {
        method: 'POST',
        body: JSON.stringify(payload)
    })
    return result.data
}

export async function getAgentTripRun(runId) {
    const result = await request(`/api/agent/trip/run/${encodeURIComponent(runId)}`)
    return result.data
}

export async function getAgentTripEvents(runId) {
    const result = await request(`/api/agent/trip/run/${encodeURIComponent(runId)}/events`)
    return result.data || []
}

export async function getAgentTripCards(runId) {
    const result = await request(`/api/agent/trip/run/${encodeURIComponent(runId)}/cards`)
    return result.data || []
}

export async function getAgentTripFinalItinerary(runId) {
    const result = await request(`/api/agent/trip/run/${encodeURIComponent(runId)}/final-itinerary`)
    return result.data || []
}

export async function confirmTripPlanCard(cardId) {
    const result = await request(`/api/agent/trip/card/${encodeURIComponent(cardId)}/confirm`, {
        method: 'POST',
        body: JSON.stringify({})
    })
    return result.data
}

export async function getAgentPlan(sessionId) {
    const result = await request(`/api/agent/plan/${encodeURIComponent(sessionId)}`)
    return result.data
}

export async function supplementAgentPlan(sessionId, payload = {}) {
    const result = await request(`/api/agent/plan/${encodeURIComponent(sessionId)}/supplement`, {
        method: 'POST',
        body: JSON.stringify(payload)
    })
    return result.data
}

export async function confirmAgentCandidate(sessionId, candidateId, payload = {}) {
    const result = await request(`/api/agent/plan/${encodeURIComponent(sessionId)}/candidates/${encodeURIComponent(candidateId)}/confirm`, {
        method: 'POST',
        body: JSON.stringify(payload)
    })
    return result.data
}

export async function rejectAgentCandidate(sessionId, candidateId, payload = {}) {
    const result = await request(`/api/agent/plan/${encodeURIComponent(sessionId)}/candidates/${encodeURIComponent(candidateId)}/reject`, {
        method: 'POST',
        body: JSON.stringify(payload)
    })
    return result.data
}

export async function changeAgentCandidate(sessionId, candidateId, payload = {}) {
    const result = await request(`/api/agent/plan/${encodeURIComponent(sessionId)}/candidates/${encodeURIComponent(candidateId)}/change`, {
        method: 'POST',
        body: JSON.stringify(payload)
    })
    return result.data
}

export async function finalizeAgentPlan(sessionId) {
    const result = await request(`/api/agent/plan/${encodeURIComponent(sessionId)}/finalize`, {
        method: 'POST',
        body: JSON.stringify({})
    })
    return result.data
}

export async function createAfterSaleOrder(payload) {
    const result = await request('/api/after-sale/orders', {
        method: 'POST',
        body: JSON.stringify(payload)
    })
    return result.data
}
