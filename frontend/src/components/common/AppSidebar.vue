<script setup>
import { ref } from 'vue'
import logoUrl from '../../assets/logo.png'

defineProps({
    userName: { type: String, default: '' },
    showSettings: { type: Boolean, default: false },
    activeMenu: { type: String, default: 'ai' }
})

const emit = defineEmits(['selectMenu', 'toggleSettings', 'openProfile', 'toggleTheme', 'loginAccount', 'logout', 'sidebar-toggle'])

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
    <div :class="['sidebar-host', { collapsed: isCollapsed }]">
        <aside class="sidebar-shell">
            <div class="sidebar-inner">
                <div class="sidebar-logo" title="七维智行">
                    <img class="logo-icon" :src="logoUrl" alt="七维智行" />
                    <div class="logo-text">
                        <div>七维智行</div>
                        <span>AI 智慧出行全过程服务平台</span>
                    </div>
                </div>

                <nav class="sidebar-menu" aria-label="主导航">
                    <button
                        v-for="menu in menus"
                        :key="menu.id"
                        :class="['menu-item', { active: activeMenu === menu.id }]"
                        @click="handleMenuClick(menu.id)"
                        :title="menu.name"
                        type="button"
                    >
                        <i :class="['menu-icon', 'fa-solid', menu.icon]" aria-hidden="true"></i>
                        <span class="menu-label">{{ menu.name }}</span>
                    </button>
                </nav>

                <div class="sidebar-user">
                    <div class="settings-menu" v-if="showSettings && !isCollapsed">
                        <button type="button" @click="emit('openProfile')">
                            <i class="fa-regular fa-id-card"></i>
                            <span>出行偏好</span>
                        </button>
                        <button type="button" @click="emit('toggleTheme')">
                            <i class="fa-solid fa-moon"></i>
                            <span>切换主题</span>
                        </button>
                        <button type="button" @click="emit('loginAccount')">
                            <i class="fa-regular fa-user-circle"></i>
                            <span>登录账号</span>
                        </button>
                        <button type="button" @click="emit('logout')">
                            <i class="fa-solid fa-right-from-bracket"></i>
                            <span>退出登录</span>
                        </button>
                    </div>

                    <button class="user-profile" type="button" @click.stop="emit('toggleSettings')" :title="userName || '用户中心'">
                        <div class="user-avatar"><i class="fa-solid fa-user"></i></div>
                        <div class="user-info">{{ userName || '出行用户' }}</div>
                        <i class="user-more fa-solid fa-ellipsis"></i>
                    </button>
                </div>
            </div>
        </aside>

        <button class="collapse-btn" type="button" @click="toggleSidebar" :title="isCollapsed ? '展开侧边栏' : '收起侧边栏'">
            <i :class="['fa-solid', isCollapsed ? 'fa-chevron-right' : 'fa-chevron-left']"></i>
        </button>
    </div>
</template>

<style scoped>
.sidebar-host {
    --sidebar-expanded: 260px;
    --sidebar-collapsed: 52px;
    --sidebar-padding-x: 12px;
    --menu-icon-size: 22px;
    --brand-icon-size: 34px;
    --user-avatar-size: 34px;
    --menu-item-height: 48px;
    width: var(--sidebar-expanded);
    height: 100vh;
    flex: 0 0 var(--sidebar-expanded);
    position: relative;
    z-index: 1000;
    transition: width 0.22s ease, flex-basis 0.22s ease;
}

.sidebar-host.collapsed {
    width: var(--sidebar-collapsed);
    flex-basis: var(--sidebar-collapsed);
}

.sidebar-shell {
    width: 100%;
    height: 100%;
    overflow: hidden;
    background: var(--sidebar-bg);
    border-right: 1px solid var(--border-color);
}

.sidebar-inner {
    width: var(--sidebar-expanded);
    min-width: var(--sidebar-expanded);
    height: 100%;
    display: flex;
    flex-direction: column;
    box-sizing: border-box;
    padding: 14px var(--sidebar-padding-x);
    transition: transform 0.22s ease;
}

.sidebar-host.collapsed .sidebar-inner {
    transform: translateX(-10px);
}

button {
    font: inherit;
}

.sidebar-logo {
    height: 56px;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 0 8px;
    margin-bottom: 10px;
    box-sizing: border-box;
    white-space: nowrap;
    overflow: hidden;
}

.logo-icon {
    width: var(--brand-icon-size);
    height: var(--brand-icon-size);
    flex: 0 0 var(--brand-icon-size);
    border-radius: 10px;
    display: block;
    object-fit: cover;
    box-shadow: 0 8px 18px rgba(37, 99, 235, 0.16);
    transform: translateY(-4px);
}

.logo-text {
    flex-shrink: 0;
    white-space: nowrap;
    color: var(--text-main);
    transition: opacity 0.18s ease;
}

.logo-text div {
    font-size: 16px;
    font-weight: 800;
    line-height: 1.1;
}

.logo-text span {
    display: block;
    margin-top: 2px;
    color: var(--text-secondary);
    font-size: 11px;
    line-height: 1.1;
}

.sidebar-menu {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 3px;
}

.menu-item {
    position: relative;
    width: calc(var(--sidebar-expanded) - var(--sidebar-padding-x) * 2);
    height: var(--menu-item-height);
    border: 0;
    border-radius: 14px;
    background: transparent;
    color: var(--text-secondary);
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 0 12px;
    box-sizing: border-box;
    cursor: pointer;
    text-align: left;
    font-weight: 650;
    white-space: nowrap;
    overflow: hidden;
    transition: background-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.menu-item::before {
    content: "";
    position: absolute;
    inset: 3px 4px;
    border-radius: 12px;
    background: transparent;
    opacity: 0;
    transition: opacity 0.18s ease, background-color 0.18s ease, box-shadow 0.18s ease;
}

.menu-icon {
    position: relative;
    z-index: 1;
    width: var(--menu-icon-size);
    height: var(--menu-icon-size);
    min-width: var(--menu-icon-size);
    flex: 0 0 var(--menu-icon-size);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    line-height: 1;
}

.menu-label {
    position: relative;
    z-index: 1;
    white-space: nowrap;
    flex-shrink: 0;
    transition: opacity 0.18s ease;
}

.menu-item:hover {
    color: var(--primary-color);
}

.menu-item:hover::before {
    background: transparent;
    opacity: 0;
}

.menu-item.active {
    background: transparent;
    color: #fff;
    box-shadow: none;
}

.menu-item.active::before {
    background: var(--primary-color);
    opacity: 1;
    box-shadow: 0 6px 14px rgba(37, 99, 235, 0.16);
}

.sidebar-host.collapsed .menu-item.active::before {
    left: 9px;
    right: auto;
    top: 10px;
    bottom: auto;
    width: 28px;
    height: 28px;
    border-radius: 9px;
}

.sidebar-user {
    position: relative;
    margin-top: auto;
    padding-top: 10px;
}

.user-profile {
    width: calc(var(--sidebar-expanded) - var(--sidebar-padding-x) * 2);
    height: 56px;
    border: 0;
    border-radius: 14px;
    background: transparent;
    color: var(--text-main);
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 0 6px;
    box-sizing: border-box;
    cursor: pointer;
    overflow: hidden;
    white-space: nowrap;
    transition: background 0.15s ease;
}

.user-profile:hover {
    background: var(--border-color);
}

.user-avatar {
    width: var(--user-avatar-size);
    height: var(--user-avatar-size);
    flex: 0 0 var(--user-avatar-size);
    border-radius: 50%;
    background: var(--primary-color);
    color: #fff;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    line-height: 1;
}

.user-info,
.user-more {
    flex-shrink: 0;
    white-space: nowrap;
    transition: opacity 0.18s ease;
}

.user-info {
    overflow: hidden;
    text-overflow: ellipsis;
    text-align: left;
}

.sidebar-host.collapsed .menu-label,
.sidebar-host.collapsed .logo-text,
.sidebar-host.collapsed .user-info,
.sidebar-host.collapsed .user-more {
    opacity: 0;
    pointer-events: none;
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

.settings-menu i,
.collapse-btn i {
    width: var(--menu-icon-size);
    height: var(--menu-icon-size);
    min-width: var(--menu-icon-size);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
}

.settings-menu button:hover {
    background: var(--primary-light);
    color: var(--primary-color);
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
    .sidebar-host {
        width: var(--sidebar-collapsed);
        flex-basis: var(--sidebar-collapsed);
    }

    .menu-label,
    .logo-text,
    .user-info,
    .user-more {
        opacity: 0;
        pointer-events: none;
    }
}
</style>
