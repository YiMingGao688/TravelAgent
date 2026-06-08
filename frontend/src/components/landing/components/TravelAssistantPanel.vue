<script setup>
defineProps({
  weatherLoading: { type: Boolean, default: false }
})

defineEmits(['open-consultation', 'query-weather'])
</script>

<template>
<aside class="trip-panel travel-assistant-panel">
        <header class="trip-panel-head">
          <div>
            <span>出行助手</span>
            <h2>服务入口</h2>
          </div>
        </header>
        <div class="assistant-entry-list">
          <button type="button" @click="$emit('open-consultation')">
            <i class="fa-solid fa-comments"></i>
            <strong>行程问题咨询</strong>
            <span>结合当前行程询问路线、住宿、提醒等问题</span>
          </button>
          <button type="button" :disabled="weatherLoading" @click="$emit('query-weather')">
            <i class="fa-solid fa-cloud-sun"></i>
            <strong>天气查询</strong>
            <span>{{ weatherLoading ? '正在查询天气...' : '查询起点和终点天气' }}</span>
          </button>
        </div>
      </aside>
</template>
