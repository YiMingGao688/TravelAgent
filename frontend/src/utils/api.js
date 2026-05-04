import axios from 'axios'

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

function handleAxiosResult(response) {
    const result = response.data
    console.log('删除接口返回：', result)
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

export async function createTripPlan(plan) {
    return request('/api/trip', {
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

export async function getTripPlan(id) {
    const result = await request(`/api/trip/${id}`)
    return result.data
}

export async function updateTripPlan(id, payload) {
    const result = await request(`/api/trip/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
    })
    return result.data
}

export async function queryRoutes(payload) {
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
    return handleAxiosResult(await axios.delete(`/api/smart-service/${id}`))
}

export async function saveServiceReport(payload) {
    const result = await request('/api/service-report/save', {
        method: 'POST',
        body: JSON.stringify(payload)
    })
    return result.data
}

export async function createServiceReport(payload) {
    const result = await request('/api/service-report', {
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

export async function updateServiceReport(id, payload) {
    const result = await request(`/api/service-report/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
    })
    return result.data
}
