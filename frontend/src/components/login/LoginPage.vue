<script setup>
/**
 * 登录页面组件
 * 功能：用户登录/注册
 * 包含左右分栏布局，左侧品牌展示，右侧登录表单
 */
import { ref, computed } from 'vue'
import logoUrl from '../../assets/logo.png'

/** 向父组件发送登录成功事件 */
const emit = defineEmits(['loginSuccess'])

/** 是否为登录模式（false为注册模式） */
const isLoginMode = ref(true)

/** 提交按钮加载状态 */
const isLoading = ref(false)

/** 表单数据：手机号和密码 */
const formData = ref({
    phone: '',
    password: ''
})

/**
 * 表单验证计算属性
 * 验证规则：11 位手机号 且 密码长度 >= 6位
 */
const isFormValid = computed(() => {
    return /^1\d{10}$/.test(formData.value.phone) && formData.value.password.length >= 6
})

/**
 * 切换登录/注册模式
 * 切换时清空密码
 */
function toggleMode() {
    isLoginMode.value = !isLoginMode.value
    formData.value.password = ''
}

/**
 * 处理表单提交
 * 调用后端API进行登录或注册
 */
async function handleSubmit() {
    // 表单验证失败则不提交
    if (!isFormValid.value) return

    isLoading.value = true

    try {
        // 根据模式选择API地址：登录或注册
        const url = isLoginMode.value ? '/api/auth/login' : '/api/auth/register'

        // 发送POST请求
        const res = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                account: formData.value.phone,
                phone: formData.value.phone,
                password: formData.value.password
            })
        }).then(r => r.json())

        // 请求成功：保存用户信息并通知父组件
        if (res.code === 200) {
            sessionStorage.setItem('loginUser', formData.value.phone)
            emit('loginSuccess')
        } else {
            // 显示后端返回的错误信息
            alert(res.msg || '操作失败')
        }
    } catch (err) {
        // 网络错误或服务器未启动
        alert('请求失败：服务器未启动或网络异常')
    } finally {
        // 无论成功失败都要关闭加载状态
        isLoading.value = false
    }
}
</script>

<template>
    <div id="app">
        <!-- 左侧品牌展示区 -->
        <div class="brand-section">
            <div class="logo-container">
                <img class="logo-icon" :src="logoUrl" alt="七维智行">
                <div class="logo-text">七维智行</div>
            </div>

            <div class="hero-content">
                <h1 class="hero-title">探索世界，<br>从这里开始。</h1>
                <p class="hero-subtitle">由先进AI驱动的旅行规划与出行服务智能体。</p>
            </div>
        </div>

        <!-- 右侧登录表单区 -->
        <div class="auth-section">
            <div class="auth-container">
                <!-- 标题区域 -->
                <div class="auth-header">
                    <h2>{{ isLoginMode ? '欢迎回来' : '创建账号' }}</h2>
                    <p>{{ isLoginMode ? '登录以继续您的智能出行体验' : '注册以开启您的智能出行体验' }}</p>
                </div>

                <!-- 登录/注册表单 -->
                <form @submit.prevent="handleSubmit">
                    <!-- 手机号输入框 -->
                    <div class="input-group">
                        <label>手机号</label>
                        <div class="input-wrapper">
                            <i class="fa-solid fa-phone"></i>
                            <input
                                type="tel"
                                class="input-field"
                                placeholder="请输入手机号"
                                v-model="formData.phone"
                                inputmode="numeric"
                                maxlength="11"
                                required
                            >
                        </div>
                    </div>

                    <!-- 密码输入框 -->
                    <div class="input-group">
                        <label>密码</label>
                        
                        <div class="input-wrapper">
                            <i class="fa-solid fa-lock"></i>
                            <input
                                type="password"
                                class="input-field"
                                placeholder="输入您的密码"
                                v-model="formData.password"
                                required
                            >
                        </div>
                    </div>

                    <!-- 提交按钮 -->
                    <button
                        type="submit"
                        class="submit-btn"
                        :disabled="isLoading || !isFormValid"
                    >
                        <!-- 加载中显示旋转动画 -->
                        <span v-if="isLoading" class="spinner"></span>
                        <span v-else>{{ isLoginMode ? '登录' : '注册并继续' }}</span>
                    </button>
                </form>

                <!-- 切换登录/注册模式 -->
                <div class="toggle-mode">
                    {{ isLoginMode ? '还没有账号？' : '已有账号？' }}
                    <a @click="toggleMode">{{ isLoginMode ? '立即注册' : '登录现有账号' }}</a>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
@import './login.css';
</style>
