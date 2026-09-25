<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

const progress = ref(0)
const backToTopVisible = ref(false)
let frame = 0

function update() {
  cancelAnimationFrame(frame)
  frame = requestAnimationFrame(() => {
    const pageHeight = document.documentElement.scrollHeight - window.innerHeight
    progress.value = pageHeight > 0 ? Math.min(1, Math.max(0, window.scrollY / pageHeight)) : 0
    backToTopVisible.value = window.scrollY > window.innerHeight * 1.2
  })
}

onMounted(() => {
  update()
  window.addEventListener('scroll', update, { passive: true })
  window.addEventListener('resize', update)
})
onUnmounted(() => {
  cancelAnimationFrame(frame)
  window.removeEventListener('scroll', update)
  window.removeEventListener('resize', update)
})
</script>

<template>
  <div class="page-progress" aria-hidden="true" :style="{ transform: `scaleX(${progress})` }"></div>
  <a v-if="backToTopVisible" class="back-to-top" href="#top" aria-label="回到顶部" title="回到顶部"
    >↑</a
  >
</template>
