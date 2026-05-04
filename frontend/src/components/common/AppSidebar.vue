<script setup>
import { ref } from 'vue'

defineProps({
    userName: { type: String, default: '' },
    showSettings: { type: Boolean, default: false },
    activeMenu: { type: String, default: 'ai' }
})

const emit = defineEmits(['selectMenu', 'toggleSettings', 'openProfile', 'toggleTheme', 'logout', 'sidebar-toggle'])

const menus = [
    { id: 'ai', name: '智能 AI 咨询', icon: 'fa-wand-magic-sparkles' },
    { id: 'transit', name: '智能交通规划', icon: 'fa-map-location-dot' },
    { id: 'custom', name: '行程定制', icon: 'fa-route' },
    { id: 'smart', name: '一站式出行服务', icon: 'fa-clipboard-check' },
    { id: 'history', name: '历史方案', icon: 'fa-clock-rotate-left' },
    { id: 'reports', name: '历史出行服务报告', icon: 'fa-file-lines' }
]

const isCollapsed = ref(false)

function handleMenuClick(menuId) {
    emit('selectMenu', menuId)
}

function toggleSidebar() {
    isCollapsed.value = !isCollapsed.value
    emit('sidebar-toggle', isCollapsed.value)
}
</script>

<template>
    <aside :class="['sidebar', { collapsed: isCollapsed }]">
        <div class="logo-container" title="智能出行助手">
            <div class="logo-icon"><i class="fa-solid fa-plane-departure"></i></div>
            <div class="logo-text">智能出行</div>
        </div>

        <nav class="nav-menu" aria-label="主导航">
            <button
                v-for="menu in menus"
                :key="menu.id"
                :class="['nav-item', { active: activeMenu === menu.id }]"
                @click="handleMenuClick(menu.id)"
                :title="menu.name"
                type="button"
            >
                <i :class="['fa-solid', menu.icon]" aria-hidden="true"></i>
                <span>{{ menu.name }}</span>
            </button>
        </nav>

        <div class="user-container">
            <div class="settings-menu" v-if="showSettings && !isCollapsed">
                <button type="button" @click="emit('openProfile')">
                    <i class="fa-regular fa-id-card"></i>
                    <span>出行偏好</span>
                </button>
                <button type="button" @click="emit('toggleTheme')">
                    <i class="fa-solid fa-moon"></i>
                    <span>切换主题</span>
                </button>
                <button type="button" @click="emit('logout')">
                    <i class="fa-solid fa-right-from-bracket"></i>
                    <span>退出登录</span>
                </button>
            </div>

            <button class="user-profile" type="button" @click.stop="emit('toggleSettings')" :title="userName || '用户中心'">
                <div class="user-avatar"><i class="fa-solid fa-user"></i></div>
                <div class="user-info">{{ userName || '出行用户' }}</div>
                <i class="more fa-solid fa-ellipsis"></i>
            </button>
        </div>

        <button class="collapse-btn" type="button" @click="toggleSidebar" :title="isCollapsed ? '展开侧边栏' : '收起侧边栏'">
            <i :class="['fa-solid', isCollapsed ? 'fa-chevron-right' : 'fa-chevron-left']"></i>
        </button>
    </aside>
</template>

<style scoped>
.sidebar {
    width: 248px;
    height: 100vh;
    background: var(--sidebar-bg);
    border-right: 1px solid var(--border-color);
    display: flex;
    flex-direction: column;
    padding: 14px 10px;
    position: relative;
    z-index: 1000;
    flex: 0 0 248px;
    transition: width 200ms ease, flex-basis 200ms ease, padding 200ms ease;
    will-change: width, flex-basis;
}

.sidebar.collapsed {
    width: 68px;
    flex-basis: 68px;
    padding-inline: 8px;
}

button {
    font: inherit;
}

.logo-container {
    height: 46px;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 0 8px;
    margin-bottom: 10px;
    overflow: hidden;
}

.sidebar.collapsed .logo-container {
    justify-content: center;
    padding: 0;
}

.logo-icon,
.user-avatar {
    width: 36px;
    height: 36px;
    display: grid;
    place-items: center;
    flex: 0 0 36px;
    color: #fff;
}

.logo-icon {
    border-radius: 11px;
    background: linear-gradient(135deg, #1d4ed8, #06b6d4);
    box-shadow: 0 10px 22px rgba(37, 99, 235, 0.22);
}

.logo-text {
    font-size: 17px;
    font-weight: 800;
    white-space: nowrap;
    color: var(--text-main);
}

.nav-menu {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.nav-item {
    height: 42px;
    width: 100%;
    border: 0;
    border-radius: 10px;
    background: transparent;
    color: var(--text-secondary);
    display: grid;
    grid-template-columns: 28px 1fr;
    align-items: center;
    gap: 10px;
    padding: 0 10px;
    cursor: pointer;
    text-align: left;
    font-weight: 650;
    overflow: hidden;
    transition: background 150ms ease, color 150ms ease, box-shadow 150ms ease;
}

.sidebar.collapsed .nav-item {
    grid-template-columns: 1fr;
    place-items: center;
    padding: 0;
}

.nav-item i {
    width: 28px;
    min-width: 28px;
    height: 28px;
    display: grid;
    place-items: center;
    font-size: 16px;
    line-height: 1;
}

.nav-item span,
.logo-text,
.user-info,
.more {
    opacity: 1;
    transform: translateX(0);
    transition: opacity 120ms ease, transform 120ms ease;
}

.sidebar.collapsed .nav-item span,
.sidebar.collapsed .logo-text,
.sidebar.collapsed .user-info,
.sidebar.collapsed .more {
    opacity: 0;
    width: 0;
    max-width: 0;
    transform: translateX(-4px);
    pointer-events: none;
}

.sidebar.collapsed .nav-item span,
.sidebar.collapsed .logo-text,
.sidebar.collapsed .user-info,
.sidebar.collapsed .more {
    display: none;
}

.nav-item:hover {
    background: var(--primary-light);
    color: var(--primary-color);
}

.nav-item.active {
    background: var(--primary-color);
    color: #fff;
    box-shadow: 0 8px 18px rgba(37, 99, 235, 0.18);
}

.user-container {
    position: relative;
    padding-top: 10px;
}

.settings-menu {
    position: absolute;
    bottom: calc(100% + 8px);
    left: 0;
    right: 0;
    background: var(--card-bg);
    border: 1px solid var(--border-color);
    border-radius: 10px;
    box-shadow: var(--shadow-md);
    padding: 8px;
}

.settings-menu button {
    width: 100%;
    height: 36px;
    border: 0;
    border-radius: 8px;
    background: transparent;
    color: var(--text-main);
    display: flex;
    align-items: center;
    gap: 9px;
    padding: 0 10px;
    cursor: pointer;
    text-align: left;
}

.settings-menu button:hover {
    background: var(--primary-light);
    color: var(--primary-color);
}

.user-profile {
    width: 100%;
    height: 44px;
    border: 0;
    border-radius: 10px;
    background: transparent;
    color: var(--text-main);
    display: grid;
    grid-template-columns: 36px 1fr 20px;
    align-items: center;
    gap: 8px;
    padding: 0 8px;
    cursor: pointer;
    overflow: hidden;
}

.sidebar.collapsed .user-profile {
    grid-template-columns: 1fr;
    place-items: center;
    padding: 0;
}

.user-profile:hover {
    background: var(--border-color);
}

.user-avatar {
    border-radius: 50%;
    background: var(--primary-color);
    font-size: 14px;
}

.user-info {
    min-width: 0;
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
    border: 1px solid var(--border-color);
    border-left: 0;
    border-radius: 0 12px 12px 0;
    background: var(--sidebar-bg);
    color: var(--text-secondary);
    display: grid;
    place-items: center;
    cursor: pointer;
    box-shadow: 6px 0 12px rgba(15, 23, 42, 0.04);
}

.collapse-btn:hover {
    color: var(--primary-color);
}

@media (max-width: 760px) {
    .sidebar {
        width: 68px;
        flex-basis: 68px;
        padding-inline: 8px;
    }

    .sidebar .logo-container,
    .sidebar .nav-item,
    .sidebar .user-profile {
        justify-content: center;
    }

    .sidebar .logo-text,
    .sidebar .nav-item span,
    .sidebar .user-info,
    .sidebar .more {
        display: none;
    }
}
</style>
