<script setup lang="ts">
import { categories } from '../data/categories'
import type { DiscoveryCategory } from '../types/discovery'

defineProps<{ activeCategory: DiscoveryCategory }>()
defineEmits<{ select: [category: DiscoveryCategory] }>()
</script>

<template>
  <nav id="interests" class="interest-navigation content-width" aria-labelledby="interest-label">
    <span id="interest-label" class="sr-only">作品分类</span>
    <div class="interest-links">
      <button
        class="interest-link"
        :class="{ 'is-active': activeCategory === 'all' }"
        type="button"
        :aria-pressed="activeCategory === 'all'"
        @click="$emit('select', 'all')"
      ><i class="interest-mark mark-all" aria-hidden="true"></i>全部</button>
      <button
        v-for="category in categories"
        :key="category.value"
        class="interest-link"
        :class="{ 'is-active': activeCategory === category.value }"
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
