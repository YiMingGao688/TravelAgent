import { requestForm } from './request.js'

export async function uploadChatImage({ file, text, userId, sessionId, model = 'zhipu-cloud' }) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('text', text)
    formData.append('userId', userId)
    formData.append('sessionId', sessionId)
    formData.append('model', model)
    return requestForm('/api/image/chat', formData)
}
