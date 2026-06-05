<script setup>
import { ref, onMounted } from 'vue'
import LandingPage from './components/landing/LandingPage.vue'
import LoginModal from './components/login/LoginModal.vue'
import ChatPage from './components/chat/ChatPage.vue'
import AdminPage from './components/admin/AdminPage.vue'
import { getCurrentUser, logoutUser } from './utils/api.js'

const isLoggedIn = ref(false)
const isAdmin = ref(false)
const showLanding = ref(true)
const showLoginModal = ref(false)
const loginTip = ref('登录后可继续使用 AI 出行规划服务。')
const loginUser = ref('')

onMounted(async () => {
  syncPageFromHash()
  window.addEventListener('hashchange', () => {
    syncPageFromHash()
  })
  loginUser.value = sessionStorage.getItem('loginUser') || ''
  isLoggedIn.value = !!loginUser.value
  try {
    const user = await getCurrentUser()
    saveLoginUser(user)
  } catch (e) {
    // 未登录时保持游客状态，用户真正操作时再弹出登录框。
  }
})

function syncPageFromHash() {
  const hash = window.location.hash || '#/'
  isAdmin.value = hash.startsWith('#/admin')
  showLanding.value = !hash.startsWith('#/app')
}

function startUse() {
  window.location.hash = '#/app'
  showLanding.value = false
}

function returnHome() {
  window.location.hash = '#/'
  showLanding.value = true
}

function requestLogin(message) {
  loginTip.value = message || '登录后可继续使用 AI 出行规划服务。'
  showLoginModal.value = true
}

function handleLoginSuccess(userInfo) {
  saveLoginUser(userInfo)
  showLoginModal.value = false
}

function saveLoginUser(userInfo) {
  const userId = String(userInfo?.id || userInfo?.userId || userInfo?.account || 'travel-user')
  sessionStorage.setItem('loginUser', userId)
  sessionStorage.setItem('loginUserId', userId)
  sessionStorage.setItem('loginEmail', userInfo?.email || userInfo?.name || userId)
  sessionStorage.setItem('authUser', JSON.stringify(userInfo || {}))
  if (userInfo?.token) {
    localStorage.setItem('token', userInfo.token)
  }
  loginUser.value = userId
  isLoggedIn.value = true
}

async function handleLogout() {
  try {
    await logoutUser()
  } catch (e) {
    // 即使后端会话已过期，也要清理本地登录态。
  }
  sessionStorage.removeItem('loginUser')
  sessionStorage.removeItem('loginUserId')
  sessionStorage.removeItem('loginEmail')
  sessionStorage.removeItem('authUser')
  sessionStorage.removeItem('chatSessionId')
  localStorage.removeItem('loginUser')
  localStorage.removeItem('token')
  loginUser.value = ''
  isLoggedIn.value = false
  showLoginModal.value = false
}
</script>

<template>
  <div class="app-wrapper" :class="{ 'landing-mode': showLanding && !isAdmin }">
    <AdminPage v-if="isAdmin" />
    <LandingPage
      v-else-if="showLanding"
      :is-logged-in="isLoggedIn"
      :login-user="loginUser"
      @start="startUse"
      @require-login="requestLogin"
    />
    <ChatPage
      v-else
      :is-logged-in="isLoggedIn"
      :login-user="loginUser"
      @logout="handleLogout"
      @require-login="requestLogin"
      @go-home="returnHome"
    />
    <LoginModal
      v-if="showLoginModal"
      :message="loginTip"
      @close="showLoginModal = false"
      @login-success="handleLoginSuccess"
    />
  </div>
</template>

<style>
/* 全局布局样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  margin: 0;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
    'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue',
    sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  height: 100%;
  overflow: hidden;
}

.app-wrapper {
  height: 100vh;
  overflow: hidden;
}

/* 首页有搜索结果、酒店详情和订单列表，需要允许在应用外壳内部滚动。
   主系统工作台仍保持 overflow hidden，避免聊天、地图、侧边栏布局被页面滚动破坏。 */
.app-wrapper.landing-mode {
  overflow-y: auto;
  overflow-x: hidden;
}
</style>
