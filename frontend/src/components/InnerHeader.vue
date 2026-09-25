<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import BrandLogo from './BrandLogo.vue'
import SearchDialog from './SearchDialog.vue'

defineProps<{ section?: string }>()
const menuOpen = ref(false)
const searchDialog = ref<InstanceType<typeof SearchDialog> | null>(null)

function openSearch() {
  searchDialog.value?.open('all', true)
}

onMounted(async () => {
  if (new URLSearchParams(window.location.search).get('focus') !== 'search') return
  await nextTick()
  openSearch()
})
</script>

<template>
  <a class="skip-link" href="#page-main">跳到主要内容</a>
  <header class="inner-header">
    <div class="inner-header-row page-width">
      <BrandLogo />
      <nav class="inner-nav" aria-label="主导航">
        <a href="/discover" :aria-current="section === 'discover' ? 'page' : undefined">发现</a>
        <a href="/library" :aria-current="section === 'library' ? 'page' : undefined">内容库</a>
        <a href="/creator/works" :aria-current="section === 'creator' ? 'page' : undefined"
          >创作者</a
        >
      </nav>
      <div class="inner-actions">
        <button class="inner-search" type="button" aria-label="搜索作品" @click="openSearch"
          >⌕ <span>搜索</span></button
        >
        <a class="inner-account" href="/login">登录</a>
        <button
          class="inner-menu-button"
          type="button"
          :aria-expanded="menuOpen"
          aria-controls="inner-mobile-nav"
          :aria-label="menuOpen ? '关闭导航' : '打开导航'"
          @click="menuOpen = !menuOpen"
        >
          ☰
        </button>
      </div>
    </div>
    <nav
      v-if="menuOpen"
      id="inner-mobile-nav"
      class="inner-mobile-nav page-width"
      aria-label="移动端导航"
    >
      <a href="/discover">发现</a><a href="/library">内容库</a><a href="/creator/works">创作者</a
      ><a href="/login">登录</a>
    </nav>
  </header>
  <SearchDialog ref="searchDialog" />
</template>
