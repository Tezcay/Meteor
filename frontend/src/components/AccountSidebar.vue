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
      <a href="/orders" :aria-current="active === 'orders' ? 'page' : undefined">订单记录</a>
    </nav>
    <p>订单与访问权益均从当前后端 Session 查询，不使用浏览器中的演示数据。</p>
  </aside>
</template>
