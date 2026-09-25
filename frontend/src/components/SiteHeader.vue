<script setup lang="ts">
import { nextTick, ref } from 'vue'
import BrandLogo from './BrandLogo.vue'
import SearchIcon from './SearchIcon.vue'

const emit = defineEmits<{ search: [query: string]; login: [] }>()
const menuOpen = ref(false)
const menuButton = ref<HTMLButtonElement | null>(null)
const searchInput = ref<HTMLInputElement | null>(null)
const searchOpen = ref(false)
const searchQuery = ref('')

function openSearch(focus = false) {
  searchOpen.value = true
  if (focus) nextTick(() => searchInput.value?.focus())
}

function closeSearch() {
  if (!searchQuery.value) searchOpen.value = false
}

function submitSearch() {
  if (!searchQuery.value.trim()) {
    openSearch(true)
    return
  }
  emit('search', searchQuery.value)
}

function login() {
  menuOpen.value = false
  emit('login')
}

function closeOnEscape() {
  if (!menuOpen.value) return
  menuOpen.value = false
  menuButton.value?.focus()
}
</script>

<template>
  <header id="top" class="site-header content-width" @keydown.esc="closeOnEscape">
    <BrandLogo />
    <form
      class="header-search"
      :class="{ 'is-open': searchOpen }"
      role="search"
      @mouseenter="openSearch()"
      @mouseleave="closeSearch"
      @submit.prevent="submitSearch"
    >
      <button type="button" aria-label="搜索作品" @click="openSearch(true)"><SearchIcon /></button>
      <label class="sr-only" for="home-search">搜索作品</label>
      <input
        id="home-search"
        ref="searchInput"
        v-model="searchQuery"
        type="search"
        placeholder="搜索作品"
        autocomplete="off"
        @focus="openSearch()"
        @blur="closeSearch"
      />
    </form>
    <div class="header-actions">
      <button class="login-trigger" type="button" @click="emit('login')">登录</button>
      <a class="publish-trigger" href="/creator/start">发布作品</a>
      <button
        ref="menuButton"
        class="menu-toggle"
        type="button"
        :aria-label="menuOpen ? '关闭导航' : '打开导航'"
        :aria-expanded="menuOpen"
        aria-controls="mobile-navigation"
        @click="menuOpen = !menuOpen"
      >
        <span></span><span></span>
      </button>
    </div>
    <nav id="mobile-navigation" class="mobile-nav" :hidden="!menuOpen" aria-label="移动端导航">
      <button type="button" @click="menuOpen = false; openSearch(true)">搜索作品</button>
      <a href="/discover" @click="menuOpen = false">浏览全部</a>
      <a href="/creator/start" @click="menuOpen = false">发布作品</a>
      <button type="button" @click="login">登录</button>
    </nav>
  </header>
</template>
