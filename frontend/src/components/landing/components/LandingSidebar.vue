<script setup>
defineProps({
  entries: { type: Array, default: () => [] },
  isEntryActive: { type: Function, required: true }
})

defineEmits(['select'])
</script>

<template>
<aside class="scenario-sidebar" aria-label="出行服务导航">
      <div class="scenario-sidebar-inner">
        <nav class="sidebar-menu">
          <template v-for="item in entries" :key="item.id">
            <div v-if="item.divider" class="sidebar-divider" :class="{ 'push-bottom': item.id === 'orders-divider' }"></div>
            <button
              v-else
              type="button"
              class="sidebar-item"
              :class="{ active: isEntryActive(item) }"
              @click="$emit('select', item)"
            >
              <i class="fa-solid" :class="item.icon"></i>
              <span>{{ item.name }}</span>
            </button>
          </template>
        </nav>
      </div>
    </aside>
</template>
