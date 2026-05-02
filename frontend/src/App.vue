<script setup>
import { ref, onMounted } from 'vue'
import LoginPage from './components/login/LoginPage.vue'
import ChatPage from './components/chat/ChatPage.vue'
import AdminPage from './components/admin/AdminPage.vue'

const isLoggedIn = ref(false)
const isAdmin = ref(false)

onMounted(() => {
  isAdmin.value = window.location.hash.startsWith('#/admin')
  window.addEventListener('hashchange', () => {
    isAdmin.value = window.location.hash.startsWith('#/admin')
  })
  isLoggedIn.value = !!sessionStorage.getItem('loginUser')
})
</script>

<template>
  <div class="app-wrapper">
    <AdminPage v-if="isAdmin" />
    <LoginPage v-else-if="!isLoggedIn" @login-success="isLoggedIn = true" />
    <ChatPage v-else @logout="isLoggedIn = false" />
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
