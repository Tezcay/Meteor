<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import { getProductDetail } from '../api/products'
import { categories } from '../data/categories'
import { collectionSlugFor, showcaseItems } from '../data/showcase'
import type { DiscoveryCategory } from '../types/discovery'
import type { ProductDetailResponse } from '../types/product'
import SearchIcon from './SearchIcon.vue'

interface DiscoveryResult {
  id: string
  title: string
  maker: string
  summary: string
  category: DiscoveryCategory
  href: string
  packages?: string[]
}

const dialog = ref<HTMLDialogElement | null>(null)
const input = ref<HTMLInputElement | null>(null)
const query = ref('')
const category = ref<DiscoveryCategory>('all')
const product = ref<ProductDetailResponse | null>(null)
const loading = ref(false)
const usingLocalShowcase = ref(false)
let request: AbortController | undefined

const matches = computed(() => {
  const term = query.value.trim().toLocaleLowerCase()
  const includesTerm = (result: DiscoveryResult) =>
    !term || `${result.title} ${result.maker} ${result.summary}`.toLocaleLowerCase().includes(term)

  if (product.value && !usingLocalShowcase.value) {
    const published: DiscoveryResult = {
      id: String(product.value.id),
      title: product.value.title,
      maker: product.value.shopName,
      summary: product.value.summary,
      category: 'templates',
      href: `/products/${product.value.id}`,
      packages: product.value.packages.map((option) => option.name),
    }
    return ['all', 'templates'].includes(category.value) && includesTerm(published) ? [published] : []
  }

  return showcaseItems
    .filter((item) => category.value === 'all' || item.category === category.value)
    .map<DiscoveryResult>((item, index) => ({
      id: `showcase-${item.category}-${index}`,
      title: item.title,
      maker: item.maker,
      summary: item.description,
      category: item.category,
      href: collectionSlugFor(item)
        ? `/collections/${collectionSlugFor(item)}`
        : `/discover?category=${item.category}`,
    }))
    .filter(includesTerm)
    .slice(0, 6)
})

async function load() {
  if (loading.value) return
  loading.value = true
  request = new AbortController()
  try {
    product.value = await getProductDetail(1001, request.signal)
    usingLocalShowcase.value = false
  } catch {
    // The creator workspace remains usable while the local API is offline.
    if (!request.signal.aborted) usingLocalShowcase.value = true
  } finally {
    loading.value = false
  }
}

async function open(selected: DiscoveryCategory = 'all', focusSearch = false) {
  category.value = selected
  query.value = ''
  if (!dialog.value?.open) dialog.value?.showModal()
  await nextTick()
  if (focusSearch) input.value?.focus()
  if (!product.value) void load()
}

function close() {
  dialog.value?.close()
}
function resetFilters() {
  category.value = 'all'
  query.value = ''
}
function closeFromBackdrop(event: MouseEvent) {
  if (event.target === dialog.value) close()
}

onBeforeUnmount(() => request?.abort())
defineExpose({ open })
</script>

<template>
  <dialog
    ref="dialog"
    class="site-dialog discovery-dialog"
    aria-labelledby="discovery-title"
    @click="closeFromBackdrop"
  >
    <div class="dialog-shell">
      <header class="dialog-heading">
        <h2 id="discovery-title">发现作品</h2>
        <button class="close-button" type="button" aria-label="关闭作品浏览" @click="close">
          ×
        </button>
      </header>
      <div class="discovery-search">
        <SearchIcon />
        <label class="sr-only" for="site-search">搜索作品、创作者</label>
        <input
          id="site-search"
          ref="input"
          v-model="query"
          type="search"
          placeholder="搜索作品、创作者"
          autocomplete="off"
        />
      </div>
      <div class="discovery-filters" role="group" aria-label="作品分类">
        <button type="button" :aria-pressed="category === 'all'" @click="category = 'all'">
          全部
        </button>
        <button
          v-for="item in categories"
          :key="item.value"
          type="button"
          :aria-pressed="category === item.value"
          @click="category = item.value"
        >
          {{ item.label }}
        </button>
      </div>
      <div class="discovery-results" aria-live="polite" :aria-busy="loading">
        <p v-if="loading" class="dialog-empty">正在寻找好作品…</p>
        <template v-else-if="matches.length">
          <details v-for="item in matches" :key="item.id" class="discovery-result">
            <summary>
              <span
                ><strong>{{ item.title }}</strong
                ><small>{{ item.maker }} / {{ categories.find((entry) => entry.value === item.category)?.label }}</small></span
              >
              <span class="result-action">查看简介 <span aria-hidden="true">＋</span></span>
            </summary>
            <div class="result-description">
              <p>{{ item.summary }}</p>
              <p v-if="item.packages?.length" class="package-names">
                包含套餐：{{ item.packages.join('、') }}
              </p>
              <a class="text-button" :href="item.href" @click="close">查看作品</a>
            </div>
          </details>
        </template>
        <div v-else class="dialog-empty">
          <p>{{ query.trim() ? '暂时没有找到匹配作品' : '这个分类的作品正在准备中' }}</p>
          <button class="text-button" type="button" @click="resetFilters">看看全部作品</button>
        </div>
      </div>
    </div>
  </dialog>
</template>
