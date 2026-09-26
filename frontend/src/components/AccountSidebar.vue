<script setup lang="ts">
defineProps<{ active: 'library' | 'orders' }>()
</script>

<template>
  <aside class="workspace-sidebar account-sidebar">
    <div class="workspace-sidebar-title">
      <span>我的账户</span>
      <small>已购内容与订单</small>
    </div>
    <nav aria-label="账户导航">
      <a href="/library" :aria-current="active === 'library' ? 'page' : undefined">内容库</a>
      <a href="/orders/demo" :aria-current="active === 'orders' ? 'page' : undefined">订单记录</a>
    </nav>
    <p>当前展示本地演示数据，接入后端后将替换为真实账户与授权状态。</p>
  </aside>
</template>
