/**
 * 主题Composable
 * 管理深色/浅色模式切换
 */
import { ref } from 'vue'

export function useTheme() {
    const isDarkMode = ref(false)

    function initTheme() {
        const savedTheme = localStorage.getItem('theme')
        if (savedTheme === 'dark') {
            isDarkMode.value = true
            document.documentElement.setAttribute('data-theme', 'dark')
        }
    }

    function toggleTheme() {
        isDarkMode.value = !isDarkMode.value

        if (isDarkMode.value) {
            document.documentElement.setAttribute('data-theme', 'dark')
        } else {
            document.documentElement.removeAttribute('data-theme')
        }

        localStorage.setItem('theme', isDarkMode.value ? 'dark' : 'light')
    }

    return {
        isDarkMode,
        initTheme,
        toggleTheme
    }
}
