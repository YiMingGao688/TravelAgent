<script setup>
import { ref } from 'vue'

defineProps({
    userName: { type: String, default: '' },
    showSettings: { type: Boolean, default: false }
})

const emit = defineEmits(['selectMenu', 'toggleSettings', 'openProfile', 'toggleTheme', 'logout', 'sidebar-toggle', 'newChat'])

const menus = [
    { id: 'ai', name: 'AI 咨询', icon: 'fa-wand-magic-sparkles' },
    { id: 'transit', name: '交通规划', icon: 'fa-map-location-dot' },
    { id: 'custom', name: '行程定制', icon: 'fa-route' },
    { id: 'smart', name: '出行服务', icon: 'fa-clipboard-check' },
    { id: 'history', name: '历史方案', icon: 'fa-clock-rotate-left' }
]

const activeMenu = ref('ai')
const isCollapsed = ref(true)

function handleMenuClick(menuId) {
    activeMenu.value = menuId
    emit('selectMenu', menuId)
}

function toggleSidebar() {
    isCollapsed.value = !isCollapsed.value
    emit('sidebar-toggle', isCollapsed.value)
}

function handleNewChat() {
    activeMenu.value = 'ai'
    emit('selectMenu', 'ai')
    emit('newChat')
}
</script>

<template>
    <aside :class="['sidebar', { collapsed: isCollapsed }]">
        <div class="logo-container">
            <div class="logo-icon"><i class="fa-solid fa-plane-departure"></i></div>
            <div v-show="!isCollapsed" class="logo-text">智能出行智能体</div>
        </div>

        <button class="new-chat-btn" v-show="!isCollapsed" @click="handleNewChat">
            <i class="fa-solid fa-plus"></i>
            <span>新对话</span>
        </button>

        <nav class="nav-menu">
            <button
                v-for="menu in menus"
                :key="menu.id"
                :class="['nav-item', { active: activeMenu === menu.id }]"
                @click="handleMenuClick(menu.id)"
                :title="menu.name"
            >
                <i :class="['fa-solid', menu.icon]"></i>
                <span v-show="!isCollapsed">{{ menu.name }}</span>
            </button>
        </nav>

        <div class="user-container">
            <div class="settings-menu" v-if="showSettings && !isCollapsed">
                <button @click="emit('openProfile')"><i class="fa-regular fa-id-card"></i> 出行偏好</button>
                <button @click="emit('toggleTheme')"><i class="fa-solid fa-moon"></i> 切换主题</button>
                <button @click="emit('logout')"><i class="fa-solid fa-right-from-bracket"></i> 退出登录</button>
            </div>

            <button class="user-profile" @click.stop="emit('toggleSettings')" :title="userName">
                <div class="user-avatar"><i class="fa-solid fa-user"></i></div>
                <div v-show="!isCollapsed" class="user-info">{{ userName }}</div>
                <i v-show="!isCollapsed" class="fa-solid fa-ellipsis"></i>
            </button>
        </div>

        <button class="collapse-btn" @click="toggleSidebar">
            <i :class="['fa-solid', isCollapsed ? 'fa-chevron-right' : 'fa-chevron-left']"></i>
        </button>
    </aside>
</template>

<style scoped>
.sidebar {
    width: 260px;
    height: 100vh;
    background-color: var(--sidebar-bg);
    border-right: 1px solid var(--border-color);
    display: flex;
    flex-direction: column;
    padding: 20px 0 16px;
    z-index: 1000;
    transition: width 0.3s ease;
    position: relative;
    flex-shrink: 0;
}

.sidebar.collapsed {
    width: 72px;
}

.logo-container {
    padding: 0 20px 26px;
    display: flex;
    align-items: center;
    gap: 12px;
}

.sidebar.collapsed .logo-container {
    justify-content: center;
}

.logo-icon {
    width: 36px;
    height: 36px;
    background: linear-gradient(135deg, #007aff, #00c6ff);
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    flex-shrink: 0;
}

.logo-text {
    font-size: 18px;
    font-weight: 700;
}

button {
    font: inherit;
}

.new-chat-btn {
    margin: 0 16px 18px;
    padding: 12px 14px;
    background: var(--primary-light);
    border: 1px solid var(--primary-color);
    border-radius: 8px;
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;
    color: var(--primary-color);
    font-weight: 600;
}

.nav-menu {
    flex: 1;
    padding: 0 12px;
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.nav-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 14px;
    border-radius: 8px;
    border: none;
    background: transparent;
    cursor: pointer;
    color: var(--text-secondary);
    font-weight: 600;
    text-align: left;
}

.sidebar.collapsed .nav-item {
    justify-content: center;
    padding: 12px;
}

.nav-item:hover {
    background: var(--primary-light);
    color: var(--primary-color);
}

.nav-item.active {
    background: var(--primary-color);
    color: white;
}

.nav-item i {
    width: 20px;
    text-align: center;
}

.user-container {
    padding: 12px;
    position: relative;
}

.settings-menu {
    position: absolute;
    bottom: calc(100% + 8px);
    left: 12px;
    right: 12px;
    background: var(--card-bg);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    box-shadow: var(--shadow-md);
    padding: 8px;
}

.settings-menu button {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 9px 10px;
    border: none;
    background: transparent;
    color: var(--text-main);
    border-radius: 6px;
    cursor: pointer;
    text-align: left;
}

.settings-menu button:hover {
    background: var(--primary-light);
    color: var(--primary-color);
}

.user-profile {
    width: 100%;
    border: none;
    background: transparent;
    color: var(--text-main);
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px;
    border-radius: 8px;
    cursor: pointer;
}

.sidebar.collapsed .user-profile {
    justify-content: center;
}

.user-profile:hover {
    background: var(--border-color);
}

.user-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: var(--primary-color);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.user-info {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    text-align: left;
}

.collapse-btn {
    position: absolute;
    right: -12px;
    top: 50%;
    transform: translateY(-50%);
    width: 24px;
    height: 48px;
    background: var(--sidebar-bg);
    border: 1px solid var(--border-color);
    border-left: none;
    border-radius: 0 12px 12px 0;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: var(--text-secondary);
}
</style>
