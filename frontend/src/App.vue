<script setup lang="ts">
import { RouterView, useRouter } from 'vue-router'

const router = useRouter()

function navigateInternalLink(event: MouseEvent) {
  if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey)
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
