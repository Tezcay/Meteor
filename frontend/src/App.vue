<script setup lang="ts">
import { nextTick, watch } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

watch(
  () => route.path,
  async (path, previousPath) => {
    if (path === previousPath) return
    await nextTick()
    document.querySelector<HTMLElement>('#page-main, #main')?.focus()
  },
)

function navigateInternalLink(event: MouseEvent) {
  if (
    event.defaultPrevented ||
    event.button !== 0 ||
    event.metaKey ||
    event.ctrlKey ||
    event.shiftKey
  )
    return
  const target = event.target as HTMLElement | null
  const link = target?.closest<HTMLAnchorElement>('a[href]')
  if (!link || link.target || link.hasAttribute('download')) return
  const url = new URL(link.href, window.location.href)
  if (url.origin !== window.location.origin || !url.pathname.startsWith('/')) return
  event.preventDefault()
  void router.push(`${url.pathname}${url.search}${url.hash}`)
}
</script>

<template>
  <div class="app-shell" @click.capture="navigateInternalLink">
    <RouterView />
  </div>
</template>
