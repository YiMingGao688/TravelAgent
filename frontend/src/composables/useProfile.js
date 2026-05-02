import { ref } from 'vue'
import { getProfile, saveProfile as saveProfileApi } from '../utils/api'

const defaultProfile = {
    city: '南京',
    transport: '不限',
    style: '舒适优先',
    note: ''
}

export function useProfile() {
    const profile = ref({ ...defaultProfile })
    const profileLoaded = ref(false)

    async function loadProfile(userId) {
        if (!userId) return

        try {
            const result = await getProfile(userId)
            if (result.code === 200 && result.data) {
                profile.value = {
                    city: result.data.city || defaultProfile.city,
                    transport: result.data.transport || defaultProfile.transport,
                    style: result.data.style || defaultProfile.style,
                    note: result.data.note || defaultProfile.note
                }
            }
        } catch (error) {
            console.error('加载出行偏好失败:', error)
        } finally {
            profileLoaded.value = true
        }
    }

    async function saveProfile() {
        const userId = sessionStorage.getItem('loginUser')
        if (!userId) {
            throw new Error('用户未登录')
        }

        const result = await saveProfileApi({
            userId,
            city: profile.value.city,
            transport: profile.value.transport,
            style: profile.value.style,
            note: profile.value.note
        })

        if (result.code !== 200) {
            throw new Error(result.msg || result.message || '保存失败')
        }

        return result
    }

    function resetProfile() {
        profile.value = { ...defaultProfile }
    }

    return {
        profile,
        profileLoaded,
        loadProfile,
        saveProfile,
        resetProfile
    }
}
