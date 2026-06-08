<script setup>
defineProps({
  logoUrl: { type: String, required: true },
  isLoggedIn: { type: Boolean, default: false },
  currentUserName: { type: String, default: '' }
})

defineEmits(['toggle-sidebar', 'search', 'login'])
</script>

<template>
<header class="home-header">
        <div class="home-nav">
          <button type="button" class="nav-sidebar-toggle" @click="$emit('toggle-sidebar')">
            <i class="fa-solid fa-bars"></i>
          </button>
          <a class="home-brand" href="#hero">
            <img :src="logoUrl" alt="七维智行" />
            <span>七维智行</span>
          </a>
          <div class="top-search">
            <input type="text" placeholder="搜索目的地、酒店、交通或出行服务" />
            <button type="button" @click="$emit('search')"><i class="fa-solid fa-magnifying-glass"></i></button>
          </div>
          <div class="top-actions">
            <button v-if="!isLoggedIn" type="button" class="login-btn" @click="$emit('login')">登录 / 注册</button>
            <button v-else type="button" class="user-avatar-btn" title="已登录">
              {{ currentUserName.slice(0, 1).toUpperCase() }}
            </button>
          </div>
        </div>
      </header>
</template>
