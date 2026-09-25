<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import CategoryNavigation from '../components/CategoryNavigation.vue'
import CreatorSection from '../components/CreatorSection.vue'
import DiscoverySection from '../components/DiscoverySection.vue'
import HeroSection from '../components/HeroSection.vue'
import PageProgress from '../components/PageProgress.vue'
import SiteFooter from '../components/SiteFooter.vue'
import SiteHeader from '../components/SiteHeader.vue'
import type { DiscoveryCategory } from '../types/discovery'

const router = useRouter()
const activeCategory = ref<DiscoveryCategory>('all')
let revealObserver: IntersectionObserver | null = null

onMounted(() => {
  if (!('IntersectionObserver' in window) || window.matchMedia('(prefers-reduced-motion: reduce)').matches)
    return
  revealObserver = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue
        entry.target.classList.add('is-revealed')
        revealObserver?.unobserve(entry.target)
      }
    },
    { rootMargin: '0px 0px -20px 0px', threshold: 0.08 },
  )
  document.querySelectorAll('.inspiration-topic, .creator-image, .creator-copy').forEach((element) => {
    element.classList.add('can-reveal')
    revealObserver?.observe(element)
  })
})
onBeforeUnmount(() => revealObserver?.disconnect())

function explore(category: DiscoveryCategory = 'all', focusSearch = false, term = '') {
  const query: Record<string, string> = {}
  if (category !== 'all') query.category = category
  if (focusSearch) query.focus = 'search'
  if (term.trim()) query.q = term.trim()
  void router.push({ path: '/discover', query })
}

function selectHomeCategory(category: DiscoveryCategory) {
  activeCategory.value = category
}
</script>

<template>
  <a class="skip-link" href="#main">跳到主要内容</a>
  <SiteHeader @search="explore('all', false, $event)" @login="router.push('/login')" />
  <main id="main" tabindex="-1">
    <CategoryNavigation :active-category="activeCategory" @select="selectHomeCategory" />
    <HeroSection :active-category="activeCategory" />
    <DiscoverySection active-category="all" @browse="explore($event)" />
    <CreatorSection @publish="router.push('/creator/start')" />
  </main>
  <SiteFooter />
  <PageProgress />
</template>
