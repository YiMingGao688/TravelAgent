const CHAT_API_BASE = '/api/chat'

async function request(url, options = {}) {
    const response = await fetch(url, {
        headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
        ...options
    })
    const result = await response.json()
    if (result.code && result.code !== 200) {
        throw new Error(result.message || result.msg || '请求失败')
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

export function streamChat(params, signal) {
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
    return request(`/api/trip/${id}`, { method: 'DELETE' })
}

export async function queryRoutes(payload) {
    const result = await request('/api/route/query', {
        method: 'POST',
        body: JSON.stringify(payload)
    })
    return result.data || { routes: [] }
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
    return request(`/api/admin/${path}/${id}`, { method: 'DELETE' })
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

export async function adminUpdateUser(id, payload) {
    return request(`/api/admin/users/${id}`, {
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

export async function deleteSmartService(id) {
    return request(`/api/smart-service/${id}`, { method: 'DELETE' })
}
