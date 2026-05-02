/**
 * 获取当前时间（格式：HH:mm）
 * @returns {string} 格式化的时间字符串
 */
export function getCurrentTime() {
    const now = new Date()
    return now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

/**
 * 格式化日期时间
 * @param {Date} date - 日期对象
 * @returns {string} 格式化的时间字符串
 */
export function formatDateTime(date) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
