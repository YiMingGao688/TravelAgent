<script setup>
import { reactive, ref } from 'vue'

defineProps({
    message: { type: String, default: '登录后可继续使用 AI 出行规划服务。' }
})

const emit = defineEmits(['close', 'login-success'])

const form = reactive({
    account: '',
    password: ''
})
const mode = ref('login')
const error = ref('')

function submit() {
    error.value = ''
    if (!form.account.trim()) {
        error.value = '请输入手机号或邮箱'
        return
    }
    if (!form.password.trim()) {
        error.value = '请输入密码'
        return
    }
    emit('login-success', {
        account: form.account.trim(),
        name: form.account.trim()
    })
}
</script>

<template>
    <div class="modal-mask" @click.self="emit('close')">
        <section class="login-modal">
            <button class="close-btn" type="button" @click="emit('close')" title="关闭">
                <i class="fa-solid fa-xmark"></i>
            </button>
            <div class="modal-head">
                <h2>登录后继续使用</h2>
                <p>{{ message }}</p>
            </div>

            <label>
                手机号/邮箱
                <input v-model="form.account" placeholder="请输入手机号或邮箱" autocomplete="username" />
            </label>
            <label>
                密码
                <input v-model="form.password" type="password" placeholder="请输入密码" autocomplete="current-password" @keydown.enter="submit" />
            </label>

            <p v-if="error" class="error">{{ error }}</p>

            <button class="submit-btn" type="button" @click="submit">
                {{ mode === 'login' ? '登录' : '注册并登录' }}
            </button>

            <p class="switch-mode">
                {{ mode === 'login' ? '还没有账号？' : '已有账号？' }}
                <button type="button" @click="mode = mode === 'login' ? 'register' : 'login'">
                    {{ mode === 'login' ? '立即注册' : '返回登录' }}
                </button>
            </p>
        </section>
    </div>
</template>

<style scoped>
.modal-mask {
    position: fixed;
    inset: 0;
    z-index: 3000;
    display: grid;
    place-items: center;
    padding: 20px;
    background: rgba(17, 24, 39, 0.36);
}

.login-modal {
    position: relative;
    width: min(420px, 100%);
    padding: 28px;
    border-radius: 20px;
    background: #fff;
    color: #111827;
    box-shadow: 0 24px 70px rgba(15, 23, 42, 0.18);
}

.close-btn {
    position: absolute;
    top: 14px;
    right: 14px;
    width: 34px;
    height: 34px;
    border: 0;
    border-radius: 50%;
    background: #f3f4f6;
    color: #4b5563;
    cursor: pointer;
}

.modal-head h2 {
    margin: 0;
    font-size: 24px;
}

.modal-head p {
    margin: 10px 0 22px;
    color: #6b7280;
    line-height: 1.7;
}

label {
    display: block;
    margin-top: 14px;
    color: #374151;
    font-size: 14px;
    font-weight: 700;
}

input {
    width: 100%;
    height: 44px;
    margin-top: 8px;
    padding: 0 13px;
    border: 1px solid #d1d5db;
    border-radius: 12px;
    outline: none;
    font: inherit;
}

input:focus {
    border-color: #2563eb;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.error {
    margin: 12px 0 0;
    color: #dc2626;
    font-size: 13px;
}

.submit-btn {
    width: 100%;
    height: 46px;
    margin-top: 20px;
    border: 0;
    border-radius: 999px;
    background: #111827;
    color: #fff;
    font: inherit;
    font-weight: 700;
    cursor: pointer;
}

.switch-mode {
    margin: 18px 0 0;
    text-align: center;
    color: #6b7280;
    font-size: 14px;
}

.switch-mode button {
    border: 0;
    background: transparent;
    color: #2563eb;
    cursor: pointer;
    font: inherit;
    font-weight: 700;
}
</style>
