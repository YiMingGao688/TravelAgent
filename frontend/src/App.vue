<script setup>
import { ref, onMounted } from 'vue'
import LandingPage from './components/landing/LandingPage.vue'
import LoginModal from './components/login/LoginModal.vue'
import ChatPage from './components/chat/ChatPage.vue'
import AdminPage from './components/admin/AdminPage.vue'

const isLoggedIn = ref(false)
const isAdmin = ref(false)
const showWorkspace = ref(false)
const showLoginModal = ref(false)
const loginTip = ref('登录后可继续使用 AI 出行规划服务。')
const loginUser = ref('')

onMounted(() => {
  syncPageFromHash()
  window.addEventListener('hashchange', () => {
    syncPageFromHash()
  })
  loginUser.value = sessionStorage.getItem('loginUser') || ''
  isLoggedIn.value = !!loginUser.value
})

function syncPageFromHash() {
  const hash = window.location.hash || '#/'
  isAdmin.value = hash.startsWith('#/admin')
  showWorkspace.value = hash.startsWith('#/app')
}

function startUse() {
  window.location.hash = '#/app'
  showWorkspace.value = true
}

function returnHome() {
  window.location.hash = '#/'
  showWorkspace.value = false
}

function requestLogin(message) {
  loginTip.value = message || '登录后可继续使用 AI 出行规划服务。'
  showLoginModal.value = true
}

function handleLoginSuccess(userInfo) {
  const account = userInfo?.account || 'travel-user'
  sessionStorage.setItem('loginUser', account)
  loginUser.value = account
  isLoggedIn.value = true
  showLoginModal.value = false
}

function handleLogout() {
  sessionStorage.removeItem('loginUser')
  sessionStorage.removeItem('chatSessionId')
  loginUser.value = ''
  isLoggedIn.value = false
}
</script>

<template>
  <div class="app-wrapper">
    <AdminPage v-if="isAdmin" />
    <LandingPage v-else-if="!showWorkspace" @start="startUse" />
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
</style>
