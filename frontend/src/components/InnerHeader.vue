<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { getLocalRole } from '../data/localStore'
import BrandLogo from './BrandLogo.vue'
import SearchDialog from './SearchDialog.vue'

const props = defineProps<{ section?: string }>()
const menuOpen = ref(false)
const searchDialog = ref<InstanceType<typeof SearchDialog> | null>(null)
const role = getLocalRole()

const isWorkspace = computed(() => props.section === 'creator' || props.section === 'admin')
const workspaceTitle = computed(() => props.section === 'admin' ? '运营中心' : '创作中心')
const workspaceDescription = computed(() => props.section === 'admin' ? '平台管理工作台' : '作品与小店工作台')
const workspaceHref = computed(() => role === 'admin' ? '/admin/reviews' : '/creator/works')
const workspaceEntryLabel = computed(() => role === 'admin' ? '运营中心' : '创作中心')
const accountHref = computed(() => role === 'visitor' ? '/login' : '/library')
const accountLabel = computed(() => role === 'visitor' ? '登录' : '内容库')

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
  <header class="inner-header" :class="{ 'inner-header-workspace': isWorkspace }">
    <div class="inner-header-row page-width">
      <BrandLogo />
      <div v-if="isWorkspace" class="workspace-context" aria-label="当前工作区">
        <strong>{{ workspaceTitle }}</strong>
        <small>{{ workspaceDescription }}</small>
      </div>
      <nav v-else class="inner-nav" aria-label="站点导航">
        <a href="/discover" :aria-current="section === 'discover' ? 'page' : undefined">发现</a>
        <a href="/discover#collection-index-title">专题</a>
        <a href="/discover#catalog-title">全部作品</a>
      </nav>
      <div class="inner-actions">
        <template v-if="isWorkspace">
          <a class="workspace-return" href="/discover">浏览站点</a>
          <a class="inner-account" href="/login">切换身份</a>
        </template>
        <template v-else>
          <button class="inner-search" type="button" aria-label="搜索作品" @click="openSearch">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="10.8" cy="10.8" r="6.2" />
              <path d="m15.4 15.4 4.1 4.1" />
            </svg>
            <span>搜索</span>
          </button>
          <a class="inner-account" :href="accountHref">{{ accountLabel }}</a>
          <a class="workspace-entry" :href="workspaceHref">{{ workspaceEntryLabel }}</a>
        </template>
        <button
          class="inner-menu-button"
          type="button"
          :aria-expanded="menuOpen"
          aria-controls="inner-mobile-nav"
          :aria-label="menuOpen ? '关闭导航' : '打开导航'"
          @click="menuOpen = !menuOpen"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path v-if="!menuOpen" d="M4 7h16M4 12h16M4 17h16" />
            <path v-else d="m6 6 12 12M18 6 6 18" />
          </svg>
        </button>
      </div>
    </div>
    <nav
      v-if="menuOpen"
      id="inner-mobile-nav"
      class="inner-mobile-nav page-width"
      aria-label="移动端导航"
    >
      <template v-if="isWorkspace">
        <a href="/discover">浏览站点</a>
        <a href="/login">切换身份</a>
      </template>
      <template v-else>
        <a href="/discover">发现</a>
        <a href="/discover#collection-index-title">专题</a>
        <a href="/discover#catalog-title">全部作品</a>
        <a v-if="role !== 'visitor'" href="/library">内容库</a>
        <a :href="workspaceHref">{{ workspaceEntryLabel }}</a>
        <a v-if="role === 'visitor'" href="/login">登录</a>
        <a v-else href="/login">切换身份</a>
      </template>
    </nav>
  </header>
  <SearchDialog ref="searchDialog" />
</template>
