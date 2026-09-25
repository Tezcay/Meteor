<script setup lang="ts">
import { computed } from 'vue'
import { categoryLabel } from '../data/categories'
import { showcaseItems } from '../data/showcase'
import type { DiscoveryCategory } from '../types/discovery'

const props = defineProps<{ activeCategory: DiscoveryCategory }>()
defineEmits<{ browse: [category: DiscoveryCategory] }>()

const selectedItems = computed(() =>
  props.activeCategory === 'all'
    ? []
    : showcaseItems.filter((item) => item.category === props.activeCategory).slice(0, 6),
)
</script>

<template>
  <section id="category-preview" class="discovery-section content-width" aria-labelledby="discover-title">
    <template v-if="activeCategory !== 'all'">
      <div class="section-heading home-category-heading">
        <h2 id="discover-title" tabindex="-1">{{ categoryLabel(activeCategory) }}</h2>
        <button class="home-category-open" type="button" @click="$emit('browse', activeCategory)">打开完整分类</button>
      </div>
      <div class="home-category-preview" :aria-label="`${categoryLabel(activeCategory)}精选预览`">
        <article v-for="item in selectedItems" :key="item.mark" class="home-category-item">
          <div class="home-category-art" :class="`art-${item.art}`">
            <img v-if="item.image" :src="item.image" :alt="item.description" loading="lazy" />
            <template v-else>
              <span>{{ item.mark }}</span>
              <strong>{{ item.title }}</strong>
            </template>
          </div>
          <h3>{{ item.title }}</h3>
        </article>
      </div>
    </template>

    <template v-else>
      <div class="section-heading">
        <h2 id="discover-title" tabindex="-1">精选分类</h2>
      </div>

      <div class="inspiration-grid">
      <article
        id="topic-design"
        class="inspiration-topic topic-design"
        aria-labelledby="design-title"
      >
        <div class="topic-art art-forms" aria-hidden="true">
          <div class="forms-poster">
            <div class="form-arch"></div>
            <div class="form-orb"></div>
          </div>
          <div class="colour-notes"><span></span><span></span><span></span></div>
        </div>
        <div class="topic-caption">
          <div>
            <h3 id="design-title">设计素材</h3>
          </div>
          <button
            class="topic-browse"
            type="button"
            aria-label="查看设计素材分类"
            @click="$emit('browse', 'design')"
          >
            <span aria-hidden="true">↗</span>
          </button>
        </div>
      </article>

      <article
        id="topic-photography"
        class="inspiration-topic topic-photography"
        aria-labelledby="photography-title"
      >
        <div class="topic-art art-photography">
          <img
            src="/images/courtyard-1280.webp"
            srcset="/images/courtyard-640.webp 640w, /images/courtyard-1280.webp 1280w"
            sizes="(max-width: 600px) 90vw, 42vw"
            width="1280"
            height="801"
            loading="lazy"
            decoding="async"
            alt="橄榄树的影子落在白色庭院与石阶上"
          />
        </div>
        <div class="topic-caption">
          <div>
            <h3 id="photography-title">摄影作品</h3>
          </div>
          <button
            class="topic-browse"
            type="button"
            aria-label="查看摄影作品分类"
            @click="$emit('browse', 'photography')"
          >
            <span aria-hidden="true">↗</span>
          </button>
        </div>
      </article>

      <article
        id="topic-publications"
        class="inspiration-topic topic-publications"
        aria-labelledby="publications-title"
      >
        <div class="topic-art art-reading" aria-hidden="true">
          <div class="reading-book">
            <svg class="book-botanical" viewBox="0 0 160 180" fill="none">
              <path
                d="M76 172c12-48 7-100-3-153M84 116C43 119 31 86 33 64c33 7 53 25 51 52ZM82 89c36-6 50-31 45-55-31 13-43 27-45 55ZM79 57C51 50 40 34 44 13c26 8 36 21 35 44ZM82 146c37-1 56-23 56-45-30 4-49 16-56 45Z"
              />
            </svg>
          </div>
          <div class="reading-slip" aria-hidden="true"></div>
        </div>
        <div class="topic-caption">
          <div>
            <h3 id="publications-title">数字读物</h3>
          </div>
          <button
            class="topic-browse"
            type="button"
            aria-label="查看数字读物分类"
            @click="$emit('browse', 'publications')"
          >
            <span aria-hidden="true">↗</span>
          </button>
        </div>
      </article>

      <article
        id="topic-templates"
        class="inspiration-topic topic-templates"
        aria-labelledby="templates-title"
      >
        <div class="topic-art art-planning" aria-hidden="true">
          <div class="planner-sheet">
            <div class="planner-heading">
              <span class="planner-shape"></span><span class="planner-star">✳</span>
            </div>
            <div class="planner-rule"></div>
            <div class="planner-columns">
              <div><span>THIS WEEK</span><i></i><i></i><i></i></div>
              <div><span>IDEAS & NOTES</span><i></i><i></i><i></i></div>
            </div>
          </div>
          <div class="planner-note" aria-hidden="true"></div>
        </div>
        <div class="topic-caption">
          <div>
            <h3 id="templates-title">实用模板</h3>
          </div>
          <button
            class="topic-browse"
            type="button"
            aria-label="查看实用模板分类"
            @click="$emit('browse', 'templates')"
          >
            <span aria-hidden="true">↗</span>
          </button>
        </div>
      </article>
      </div>
    </template>
  </section>
</template>
