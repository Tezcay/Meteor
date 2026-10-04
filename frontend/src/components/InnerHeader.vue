<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { getLocalRole } from '../data/localStore'
import BrandLogo from './BrandLogo.vue'
import SearchDialog from './SearchDialog.vue'

const props = defineProps<{ section?: string }>()
const menuOpen = ref(false)
const menuButton = ref<HTMLButtonElement | null>(null)
const searchDialog = ref<InstanceType<typeof SearchDialog> | null>(null)
const role = ref(getLocalRole())

const isWorkspace = computed(() => ['account', 'creator', 'admin'].includes(props.section ?? ''))
const workspaceTitle = computed(() =>
  props.section === 'admin' ? '管理后台' : props.section === 'creator' ? '创作中心' : '我的账户',
)
const workspaceDescription = computed(() =>
  props.section === 'admin'
    ? '审核与售后'
    : props.section === 'creator'
      ? '作品与订单'
      : '已购内容与订单',
)
const accountLabel = computed(() => (role.value === 'visitor' ? '登录' : '账户'))
const accountHref = computed(
  () =>
    ({
      visitor: '/login',
      user: '/library',
      creator: '/creator/works',
      admin: '/admin/reviews',
    })[role.value],
)

function syncRole() {
  role.value = getLocalRole()
}

function openSearch() {
  searchDialog.value?.open('all', true)
}

function closeMenu(returnFocus = false) {
  if (!menuOpen.value) return
  menuOpen.value = false
  if (returnFocus) nextTick(() => menuButton.value?.focus())
}

onMounted(async () => {
  window.addEventListener('meteor:role-change', syncRole)
  window.addEventListener('storage', syncRole)
  if (new URLSearchParams(window.location.search).get('focus') !== 'search') return
  await nextTick()
  openSearch()
})

onBeforeUnmount(() => {
  window.removeEventListener('meteor:role-change', syncRole)
  window.removeEventListener('storage', syncRole)
})
</script>

<template>
  <a class="skip-link" href="#page-main">跳到主要内容</a>
  <header
    class="inner-header"
    :class="{ 'inner-header-workspace': isWorkspace }"
    @keydown.esc="closeMenu(true)"
  >
    <div class="inner-header-row page-width">
      <BrandLogo />
      <div v-if="isWorkspace" class="workspace-context" aria-label="当前工作区">
        <strong>{{ workspaceTitle }}</strong>
        <small>{{ workspaceDescription }}</small>
      </div>
      <nav v-else class="inner-nav" aria-label="站点导航">
        <a href="/discover" :aria-current="section === 'discover' ? 'page' : undefined">发现</a>
        <a href="/library" :aria-current="section === 'library' ? 'page' : undefined">内容库</a>
        <a href="/creator/works">创作者</a>
      </nav>
      <div class="inner-actions">
        <template v-if="isWorkspace">
          <a class="workspace-return" href="/discover">浏览站点</a>
          <a class="inner-account" href="/login">切换账号</a>
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
        </template>
        <button
          ref="menuButton"
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
      @click="closeMenu()"
    >
      <template v-if="isWorkspace">
        <a href="/discover">浏览站点</a>
        <a href="/login">切换账号</a>
      </template>
      <template v-else>
        <a href="/discover">发现</a>
        <a href="/library">内容库</a>
        <a href="/creator/works">创作者</a>
        <a :href="accountHref">{{ accountLabel }}</a>
      </template>
    </nav>
  </header>
  <SearchDialog ref="searchDialog" />
</template>
