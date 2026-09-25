<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { categoryLabel } from '../data/categories'
import { collectionSlugFor, showcaseItems } from '../data/showcase'
import type { DiscoveryCategory } from '../types/discovery'

const props = defineProps<{ activeCategory: DiscoveryCategory }>()
const featuredItems = computed(() => {
  const items =
    props.activeCategory === 'all'
      ? showcaseItems.filter((item) => item.art === 'poster')
      : showcaseItems.filter((item) => item.category === props.activeCategory)
  return items.slice(0, 6)
})
const featureTitle = computed(() =>
  props.activeCategory === 'all' ? '精选' : categoryLabel(props.activeCategory),
)

const currentPoster = ref(0)
const router = useRouter()

watch(featuredItems, () => {
  currentPoster.value = 0
})

function selectPoster(index: number) {
  const count = featuredItems.value.length
  if (!count) return
  currentPoster.value = (index + count) % count
}

function posterPosition(index: number) {
  if (index === currentPoster.value) return 'is-current'
  const count = featuredItems.value.length
  if (index === (currentPoster.value - 1 + count) % count) return 'is-previous'
  if (index === (currentPoster.value + 1) % count) return 'is-next'
  if (index === (currentPoster.value - 2 + count) % count)
    return 'is-far-previous'
  if (index === (currentPoster.value + 2) % count) return 'is-far-next'
  return 'is-hidden'
}

function usePoster(index: number) {
  if (index === currentPoster.value) {
    const item = featuredItems.value[index]
    const collectionSlug = item ? collectionSlugFor(item) : ''
    if (collectionSlug) void router.push(`/collections/${collectionSlug}`)
    else if (props.activeCategory !== 'all')
      void router.push({ path: '/discover', query: { category: props.activeCategory } })
    return
  }
  selectPoster(index)
}
</script>

<template>
  <section id="discover" class="featured content-width" aria-labelledby="featured-title">
    <header class="featured-heading">
      <h1 id="featured-title">{{ featureTitle }}</h1>
      <div class="poster-controls" aria-label="精选海报切换">
        <span aria-live="polite">
          {{ String(currentPoster + 1).padStart(2, '0') }} / {{ String(featuredItems.length).padStart(2, '0') }}
        </span>
        <button type="button" aria-label="上一张" @click="selectPoster(currentPoster - 1)">←</button>
        <button type="button" aria-label="下一张" @click="selectPoster(currentPoster + 1)">→</button>
      </div>
    </header>

    <div
      class="poster-carousel"
      tabindex="0"
      aria-label="精选海报，使用左右方向键切换"
      @keydown.left.prevent="selectPoster(currentPoster - 1)"
      @keydown.right.prevent="selectPoster(currentPoster + 1)"
    >
      <button
        v-for="(item, index) in featuredItems"
        :key="item.mark"
        type="button"
        class="poster-card"
        :class="posterPosition(index)"
        :data-poster="index"
        :aria-label="currentPoster === index ? `查看${item.title}` : `切换到${item.title}`"
        @click="usePoster(index)"
      >
        <img
          v-if="item.image"
          :src="item.image"
          width="900"
          height="1200"
          :alt="item.description"
          :fetchpriority="index === 0 ? 'high' : undefined"
          :loading="index === 0 ? 'eager' : 'lazy'"
          decoding="async"
          draggable="false"
        />
        <span v-else class="poster-demo" aria-hidden="true"><small>{{ item.mark }}</small><strong>{{ item.title }}</strong></span>
      </button>
    </div>
  </section>
</template>
