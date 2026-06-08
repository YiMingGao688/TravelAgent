export async function request(url, options = {}) {
    const response = await fetch(url, {
        headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
        credentials: 'include',
        ...options
    })
    const result = await response.json()
    if (result.code && result.code !== 200) {
        throw new Error(result.message || result.msg || 'Request failed')
    }
    return result
}

export async function requestForm(url, formData, options = {}) {
    const response = await fetch(url, {
        method: 'POST',
        credentials: 'include',
        ...options,
        body: formData
    })
    if (!response.ok) {
        throw new Error('Upload failed')
    }
    return response.json()
}
