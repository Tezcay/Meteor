<script setup lang="ts">
import { categories } from '../data/categories'
import type { DiscoveryCategory } from '../types/discovery'

defineProps<{ activeCategory: DiscoveryCategory }>()
defineEmits<{ select: [category: DiscoveryCategory] }>()

const categoryTones: Record<DiscoveryCategory, string> = {
  all: '#303d37',
  design: '#9ca979',
  templates: '#a7c6ca',
  photography: '#6f8f7c',
  illustration: '#315b86',
  fonts: '#9e83bd',
  'three-d': '#c79f60',
  audio: '#515950',
  publications: '#8d6f59',
}

function toneFor(category: DiscoveryCategory) {
  return categoryTones[category]
}
</script>

<template>
  <nav id="interests" class="interest-navigation content-width" aria-labelledby="interest-label">
    <span id="interest-label" class="sr-only">作品分类</span>
    <div class="interest-links">
      <button
        class="interest-link"
        :class="{ 'is-active': activeCategory === 'all' }"
        :style="{ '--interest-tone': toneFor('all') }"
        type="button"
        :aria-pressed="activeCategory === 'all'"
        @click="$emit('select', 'all')"
      ><i class="interest-mark mark-all" aria-hidden="true"></i>全部</button>
      <button
        v-for="category in categories"
        :key="category.value"
        class="interest-link"
        :class="{ 'is-active': activeCategory === category.value }"
        :style="{ '--interest-tone': toneFor(category.value) }"
        type="button"
        :aria-pressed="activeCategory === category.value"
        :aria-label="'在首页查看' + category.label"
        @click="$emit('select', category.value)"
      >
        <i class="interest-mark" :class="`mark-${category.value}`" aria-hidden="true"></i>{{ category.label }}
      </button>
    </div>
  </nav>
</template>