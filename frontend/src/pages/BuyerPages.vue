<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EmptyCategoryScene from '../components/EmptyCategoryScene.vue'
import InnerHeader from '../components/InnerHeader.vue'
import { getProductDetail, getPublishedProducts } from '../api/products'
import { categories, categoryOptions, parseCategory, subcategoryLabel } from '../data/categories'
import { getLocalPurchase, saveLocalPurchase, saveLocalRole } from '../data/localStore'
import type { LocalRole } from '../data/localStore'
import {
  collectionSlugFor,
  getArtworkCollection,
  seededProduct,
  showcaseItems,
  yuan,
} from '../data/showcase'
import type { DiscoveryCategory } from '../types/discovery'
import type { ProductDetailResponse, ProductListItemResponse } from '../types/product'

const props = defineProps<{ path: string }>()
const route = useRoute()
const router = useRouter()
const category = ref<DiscoveryCategory>(parseCategory(String(route.query.category ?? '')))
const query = ref(String(route.query.q ?? ''))
const selectedPackage = ref(Number(route.query.package ?? 2001))
const resultState = ref<'waiting' | 'checking' | 'ready' | 'late'>('waiting')
const showLibraryPreview = ref(false)
const showOrderPreview = ref(false)
const product = ref<ProductDetailResponse | null>(null)
const productError = ref(false)
const productLoading = ref(false)
const catalog = ref<ProductListItemResponse[]>([])
const catalogLoading = ref(false)
const catalogError = ref(false)
const signInNotice = ref(false)
const loginRole = ref<Exclude<LocalRole, 'visitor'>>('user')
const activeImageIndex = ref(0)
const collectionSaved = ref(false)
const localPurchase = ref(getLocalPurchase())
let request: AbortController | null = null
let catalogRequest: AbortController | null = null
let searchTimer: ReturnType<typeof setTimeout> | null = null
const isProductPath = computed(() => /^\/products\/[1-9]\d*$/.test(props.path))
const isCollectionPath = computed(() => props.path.startsWith('/collections/'))
const collection = computed(() =>
  isCollectionPath.value ? getArtworkCollection(props.path.slice('/collections/'.length)) : undefined,
)
const activeCollectionImage = computed(() => collection.value?.images[activeImageIndex.value])
const collectionCategoryLabel = computed(() =>
  collection.value?.category === 'photography' ? '摄影' : '插画',
)
const requestedProductId = computed(() =>
  isProductPath.value
    ? Number(props.path.slice('/products/'.length))
    : props.path === '/checkout'
      ? Number(route.query.product ?? 1001)
      : 1001,
)

const displayProduct = computed(() => product.value ?? seededProduct)
const canShowProduct = computed(() => product.value !== null || requestedProductId.value === 1001)
const productSummary = computed(() => displayProduct.value.summary.replace(/[。！？]+$/, ''))
const packageOption = computed(
  () =>
    displayProduct.value.packages.find((item) => item.id === selectedPackage.value) ??
    displayProduct.value.packages[0]!,
)
const isDemoProduct = computed(() => !product.value)
const exhibitionShelves = computed(() => {
  if (category.value === 'all') {
    return categories.map((item) => ({
      value: item.value,
      category: item.value,
      label: item.label,
      items: showcaseItems.filter((work) => work.category === item.value),
    }))
  }

  const selectedCategory = category.value
  const groups = new Map<string, (typeof showcaseItems)[number][]>()
  showcaseItems
    .filter((work) => work.category === selectedCategory)
    .forEach((work) => groups.set(work.subcategory, [...(groups.get(work.subcategory) ?? []), work]))

  return [...groups.entries()].map(([subcategory, items]) => ({
    value: `${selectedCategory}-${subcategory}`,
    category: selectedCategory,
    label: subcategoryLabel(selectedCategory, subcategory),
    items,
  }))
})
const catalogItems = computed(() =>
  category.value === 'all'
    ? catalog.value
    : category.value === 'templates'
      ? catalog.value.filter((item) => item.id === 1001)
      : [],
)
const section = computed(() =>
  props.path === '/library' || props.path.startsWith('/orders') ? 'library' : 'discover',
)

async function loadProduct(id = requestedProductId.value) {
  request?.abort()
  const currentRequest = new AbortController()
  request = currentRequest
  productLoading.value = true
  productError.value = false
  try {
    product.value = await getProductDetail(id, currentRequest.signal)
    if (!product.value.packages.some((item) => item.id === selectedPackage.value)) {
      selectedPackage.value = product.value.packages[0]?.id ?? 0
    }
  } catch {
    if (!currentRequest.signal.aborted) productError.value = true
  } finally {
    if (!currentRequest.signal.aborted) productLoading.value = false
  }
}

async function loadCatalog() {
  catalogRequest?.abort()
  const currentRequest = new AbortController()
  catalogRequest = currentRequest
  catalogLoading.value = true
  catalogError.value = false
  catalog.value = []
  try {
    catalog.value = await getPublishedProducts(
      query.value,
      category.value === 'all' ? undefined : category.value,
      currentRequest.signal,
    )
  } catch {
    if (!currentRequest.signal.aborted) catalogError.value = true
  } finally {
    if (!currentRequest.signal.aborted) catalogLoading.value = false
  }
}

watch([query, category], () => {
  if (props.path !== '/discover') return
  if (searchTimer) clearTimeout(searchTimer)
  catalogRequest?.abort()
  catalog.value = []
  catalogLoading.value = true
  catalogError.value = false
  searchTimer = setTimeout(() => void loadCatalog(), 250)
})

onMounted(async () => {
  if (props.path === '/discover') void loadCatalog()
  if (props.path === '/shops/northstar' || isProductPath.value || props.path === '/checkout')
    void loadProduct()
})
onBeforeUnmount(() => {
  request?.abort()
  catalogRequest?.abort()
  if (searchTimer) clearTimeout(searchTimer)
})

function setCategory(value: DiscoveryCategory) {
  category.value = value
  const nextQuery = { ...route.query }
  if (value === 'all') delete nextQuery.category
  else nextQuery.category = value
  void router.replace({ path: '/discover', query: nextQuery })
}

watch(
  () => route.fullPath,
  () => {
    category.value = parseCategory(String(route.query.category ?? ''))
    query.value = String(route.query.q ?? '')
    selectedPackage.value = Number(route.query.package ?? 2001)
    activeImageIndex.value = 0
    collectionSaved.value = false
    localPurchase.value = getLocalPurchase()
    if (props.path === '/checkout/result' && route.query.demo === 'ready') resultState.value = 'ready'
    product.value = null
    productError.value = false
    if (props.path === '/discover') void loadCatalog()
    if (props.path === '/shops/northstar' || isProductPath.value || props.path === '/checkout')
      void loadProduct()
  },
)

function selectCollectionImage(index: number) {
  const imageCount = collection.value?.images.length ?? 0
  if (!imageCount) return
  activeImageIndex.value = (index + imageCount) % imageCount
}

function collectionLink(item: { category: string }) {
  const slug = collectionSlugFor(item as (typeof showcaseItems)[number])
  return slug ? `/collections/${slug}` : undefined
}

function clearFilters() {
  query.value = ''
  setCategory('all')
}

function completeDemoPurchase() {
  if (!packageOption.value) return
  localPurchase.value = saveLocalPurchase({
    productId: displayProduct.value.id,
    title: displayProduct.value.title,
    shopName: displayProduct.value.shopName,
    packageName: packageOption.value.name,
  })
  resultState.value = 'ready'
  void router.push({ path: '/checkout/result', query: { demo: 'ready' } })
}

function signInWithDemoRole() {
  saveLocalRole(loginRole.value)
  signInNotice.value = true
  const next = String(route.query.next ?? '')
  const canContinue =
    (loginRole.value === 'creator' && next.startsWith('/creator')) ||
    (loginRole.value === 'admin' && next.startsWith('/admin'))
  const destination = canContinue
    ? next
    : { user: '/library', creator: '/creator/works', admin: '/admin/reviews' }[loginRole.value]
  void router.replace(destination)
}

function scrollShelf(categoryValue: string, direction: -1 | 1) {
  const rail = document.getElementById(`rail-${categoryValue}`)
  if (!rail) return
  const behavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'
  rail.scrollBy({ left: direction * Math.round(rail.clientWidth * 0.78), behavior })
}

type RailDragState = { startX: number; startScrollLeft: number; dragging: boolean }
const railDrags = new WeakMap<HTMLElement, RailDragState>()

function startRailDrag(event: PointerEvent) {
  if (event.pointerType === 'touch' || event.button !== 0) return
  const rail = event.currentTarget as HTMLElement
  railDrags.set(rail, { startX: event.clientX, startScrollLeft: rail.scrollLeft, dragging: false })
  rail.setPointerCapture(event.pointerId)
}

function moveRailDrag(event: PointerEvent) {
  const rail = event.currentTarget as HTMLElement
  const state = railDrags.get(rail)
  if (!state) return
  const distance = event.clientX - state.startX
  if (Math.abs(distance) > 3) {
    state.dragging = true
    rail.classList.add('is-dragging')
  }
  rail.scrollLeft = state.startScrollLeft - distance
}

function endRailDrag(event: PointerEvent) {
  const rail = event.currentTarget as HTMLElement
  railDrags.delete(rail)
  rail.classList.remove('is-dragging')
  if (rail.hasPointerCapture(event.pointerId)) rail.releasePointerCapture(event.pointerId)
}

function wheelShelf(event: WheelEvent) {
  const rail = event.currentTarget as HTMLElement
  if (Math.abs(event.deltaY) < Math.abs(event.deltaX)) return
  const canScroll = rail.scrollWidth > rail.clientWidth
  if (!canScroll) return
  event.preventDefault()
  rail.scrollBy({ left: event.deltaY, behavior: 'auto' })
}

function tiltCard(event: PointerEvent) {
  if (event.pointerType !== 'mouse' || window.matchMedia('(prefers-reduced-motion: reduce)').matches)
    return
  const card = event.currentTarget as HTMLElement
  const bounds = card.getBoundingClientRect()
  const x = (event.clientX - bounds.left) / bounds.width - 0.5
  const y = (event.clientY - bounds.top) / bounds.height - 0.5
  card.style.setProperty('--tilt-x', `${(-y * 2.2).toFixed(2)}deg`)
  card.style.setProperty('--tilt-y', `${(x * 2.2).toFixed(2)}deg`)
  card.classList.add('is-tilting')
}

function resetCardTilt(event: PointerEvent) {
  const card = event.currentTarget as HTMLElement
  card.classList.remove('is-tilting')
  card.style.removeProperty('--tilt-x')
  card.style.removeProperty('--tilt-y')
}
</script>

<template>
  <InnerHeader :section="section" />
  <main id="page-main" class="inner-main" tabindex="-1">
    <template v-if="path === '/discover'">
      <section class="page-width discover-body" aria-label="浏览作品">
        <nav class="filter-row" aria-label="分类筛选">
          <button
            v-for="item in categoryOptions"
            :key="item.value"
            type="button"
            :aria-pressed="category === item.value"
            @click="setCategory(item.value)"
          >
            {{ item.label }}
          </button>
        </nav>
        <div v-if="!query.trim() && exhibitionShelves.length" class="curation-shelves">
          <section
            v-for="shelf in exhibitionShelves"
            :key="shelf.value"
            class="curation-shelf"
            :aria-labelledby="`shelf-${shelf.value}`"
          >
            <header class="curation-shelf-heading">
              <h2 :id="`shelf-${shelf.value}`">{{ shelf.label }}</h2>
              <div class="curation-shelf-actions">
                <a v-if="category === 'all'" :href="`/discover?category=${shelf.category}`">查看</a>
                <span v-else>{{ shelf.items.length }} 件</span>
                <div class="curation-shelf-controls" :aria-label="`浏览${shelf.label}`">
                  <button type="button" :aria-label="`向左浏览${shelf.label}`" @click="scrollShelf(shelf.value, -1)">←</button>
                  <button type="button" :aria-label="`向右浏览${shelf.label}`" @click="scrollShelf(shelf.value, 1)">→</button>
                </div>
              </div>
            </header>
            <div
              :id="`rail-${shelf.value}`"
              class="curation-rail"
              tabindex="0"
              :aria-label="`${shelf.label}作品列表，可拖动或使用滚轮浏览`"
              @pointerdown="startRailDrag"
              @pointermove="moveRailDrag"
              @pointerup="endRailDrag"
              @pointercancel="endRailDrag"
              @wheel="wheelShelf"
            >
              <article
                v-for="item in shelf.items"
                :key="item.title"
                class="curation-card"
                :class="[`curation-${item.art}`, `variant-${item.variant}`]"
                @pointermove="tiltCard"
                @pointerleave="resetCardTilt"
              >
                <component
                  :is="collectionLink(item) ? 'a' : 'div'"
                  :href="collectionLink(item)"
                  :aria-label="collectionLink(item) ? `查看图集：${item.title}` : undefined"
                >
                  <div class="curation-art" :class="[`art-${item.art}`, `variant-${item.variant}`]">
                    <img v-if="item.image" :src="item.image" :alt="item.description" loading="lazy" />
                    <div
                      v-else
                      class="demo-piece"
                      :class="[`demo-${item.category}`, `variant-${item.variant}`]"
                      aria-hidden="true"
                    >
                      <small>{{ item.mark }}</small>
                      <strong>{{ item.title }}</strong>
                      <div class="demo-shapes"><i></i><i></i><i></i><i></i></div>
                    </div>
                  </div>
                  <h3>{{ item.title }}</h3>
                </component>
              </article>
            </div>
          </section>
        </div>
        <EmptyCategoryScene
          v-if="
            !query.trim() &&
            category !== 'all' &&
            category !== 'templates' &&
            !exhibitionShelves.length
          "
          :category="category"
          @reset="clearFilters"
        />
        <section
          v-if="
            ['all', '模板', 'templates'].includes(category) &&
            (catalogItems.length || catalogLoading)
          "
          class="catalog-section"
          aria-labelledby="catalog-title"
        >
          <div class="catalog-heading">
            <div>
              <h2 id="catalog-title">作品</h2>
            </div>
            <span v-if="catalogLoading" class="quiet-note">正在读取作品</span>
          </div>
          <a
            v-for="item in catalogItems"
            :key="item.id"
            class="catalog-product"
            :href="`/products/${item.id}`"
            ><div class="catalog-product-art">
              <span>{{ item.shopName }}</span
              ><strong v-if="item.id === 1001">把经历<br />排成故事</strong
              ><strong v-else>{{ item.title }}</strong
              ><small>{{ item.id === 1001 ? '简历 / 求职信 / 作品集' : 'DIGITAL WORKS' }}</small>
            </div>
            <div class="catalog-product-copy">
              <span>{{ item.id === 1001 ? '实用模板' : '数字作品' }} · {{ item.shopName }}</span>
              <h3>{{ item.title }}</h3>
              <p>{{ item.summary.replace(/[。！？]+$/, '') }}</p>
              <span class="text-arrow">查看作品 ↗</span>
            </div></a
          >
        </section>
        <div v-else-if="query.trim() && !catalogLoading" class="empty-state">
          <h2>没有找到相关作品</h2>
          <p>试试其他关键词</p>
          <button class="button button-secondary" type="button" @click="clearFilters">
            清除筛选
          </button>
        </div>
      </section>
    </template>

    <template v-else-if="path === '/shops/northstar'">
      <section class="shop-cover">
        <div class="page-width shop-cover-inner">
          <span class="quiet-label">创作者小店</span>
          <h1>Northstar Design</h1>
          <p>数字作品与实用模板</p>
        </div>
      </section>
      <div class="page-width shop-content">
        <a class="catalog-product" href="/products/1001"
          ><div class="catalog-product-art">
            <span>Northstar Design</span><strong>把经历<br />排成故事</strong
            ><small>简历 / 求职信 / 作品集</small>
          </div>
          <div class="catalog-product-copy">
            <span>店内作品</span>
            <h3>{{ displayProduct.title }}</h3>
            <p>{{ productSummary }}</p>
            <small v-if="isDemoProduct">{{
              productError ? '后端暂未连接，当前显示设计示例' : '正在读取商品信息'
            }}</small
            ><span class="text-arrow">查看详情 ↗</span>
          </div></a
        >
      </div>
    </template>

    <template v-else-if="isCollectionPath && collection">
      <div class="page-width collection-breadcrumb">
        <a href="/discover">发现</a><span>/</span><a :href="`/discover?category=${collection.category}`">{{ collection.category === 'photography' ? '摄影' : '插画' }}</a><span>/</span><span>{{ collection.title }}</span>
      </div>
      <section class="page-width collection-layout" aria-labelledby="collection-title">
        <div class="collection-stage">
          <button
            class="collection-main-image"
            type="button"
            :aria-label="`切换至下一张图像：${activeCollectionImage?.alt}`"
            @click="selectCollectionImage(activeImageIndex + 1)"
          >
            <img v-if="activeCollectionImage" :src="activeCollectionImage.src" :alt="activeCollectionImage.alt" />
            <span class="collection-expand" aria-hidden="true">↗</span>
          </button>
          <div class="collection-thumbs" role="tablist" aria-label="图集预览">
            <button
              v-for="(image, index) in collection.images"
              :key="image.src"
              type="button"
              role="tab"
              :aria-selected="index === activeImageIndex"
              :aria-label="`查看第 ${index + 1} 张：${image.alt}`"
              @click="selectCollectionImage(index)"
              @keydown.left.prevent="selectCollectionImage(index - 1)"
              @keydown.right.prevent="selectCollectionImage(index + 1)"
            ><img :src="image.src" alt="" /></button>
          </div>
        </div>
        <aside class="collection-info">
          <div>
            <span class="quiet-label">{{ collection.category === 'photography' ? '摄影作品' : '插画作品' }}</span>
            <h1 id="collection-title">{{ collection.title }}</h1>
            <a class="collection-maker" href="/discover">{{ collection.maker }}</a>
          </div>
          <div class="collection-controls">
            <p>{{ collection.images.length }} 张图像 <span>{{ activeImageIndex + 1 }} / {{ collection.images.length }}</span></p>
            <div>
              <button type="button" aria-label="上一张图像" @click="selectCollectionImage(activeImageIndex - 1)">←</button>
              <button type="button" aria-label="下一张图像" @click="selectCollectionImage(activeImageIndex + 1)">→</button>
            </div>
          </div>
          <button class="collection-save" type="button" :aria-pressed="collectionSaved" @click="collectionSaved = !collectionSaved">
            {{ collectionSaved ? '已收藏' : '收藏作品' }}
          </button>
          <p class="collection-summary">{{ collection.summary }}</p>
          <a class="underlined-link" href="#collection-images">浏览全部图像</a>
        </aside>
      </section>
      <section id="collection-note" class="page-width collection-note">
        <h2>作品说明</h2>
        <p>这是一组手动编排的展示图像。作品集归属由创作者或编辑配置，不依据题材或视觉相似度自动合并。</p>
      </section>
      <section id="collection-images" class="page-width collection-sequence" aria-labelledby="collection-images-title">
        <header class="collection-sequence-head">
          <h2 id="collection-images-title">完整图集</h2>
          <p>{{ collection.images.length }} 张展示图</p>
        </header>
        <div class="collection-sequence-grid" :class="`collection-sequence-${collection.category}`">
          <figure v-for="(image, index) in collection.images" :key="`sequence-${image.src}`" class="collection-sequence-item">
            <button
              type="button"
              :aria-label="`在上方查看第 ${index + 1} 张：${image.alt}`"
              @click="selectCollectionImage(index)"
            >
              <img :src="image.src" :alt="image.alt" loading="lazy" />
              <span aria-hidden="true">{{ String(index + 1).padStart(2, '0') }}</span>
            </button>
          </figure>
        </div>
      </section>
      <section class="page-width collection-continue" aria-labelledby="collection-continue-title">
        <h2 id="collection-continue-title">继续浏览</h2>
        <a class="underlined-link" :href="`/discover?category=${collection.category}`">更多{{ collectionCategoryLabel }}作品</a>
      </section>
    </template>

    <template v-else-if="isProductPath">
      <template v-if="canShowProduct">
        <div class="page-width breadcrumb">
          <a href="/discover">发现</a><span>/</span
          ><a v-if="displayProduct.id === 1001" href="/shops/northstar">Northstar Design</a
          ><span v-else>{{ displayProduct.shopName }}</span
          ><span>/</span><span>{{ displayProduct.title }}</span>
        </div>
        <section class="page-width product-layout">
          <div class="product-gallery">
            <div class="product-cover">
              <span>{{ displayProduct.shopName }} / DIGITAL WORKS</span
              ><strong v-if="displayProduct.id === 1001">把经历<br /><i>排成故事</i></strong
              ><strong v-else>{{ displayProduct.title }}</strong>
              <div class="product-cover-rule"></div>
              <small>{{
                displayProduct.id === 1001 ? 'RESUME · LETTER · PORTFOLIO' : 'DIGITAL WORKS'
              }}</small>
            </div>
            <div class="product-thumbs">
              <div>预览图</div>
              <div>版式示意</div>
              <div>内容说明</div>
            </div>
            <p class="media-disclaimer">展示图仅供预览，不代表交付文件</p>
          </div>
          <div class="product-info">
            <span class="quiet-label"
              >{{ displayProduct.id === 1001 ? '实用模板' : '数字作品' }} /
              {{ displayProduct.shopName }}</span
            >
            <h1>{{ displayProduct.title }}</h1>
            <p class="product-summary">{{ productSummary }}</p>
            <div v-if="isDemoProduct" class="inline-note">
              {{
                productError
                  ? '后端未连接，以下套餐取自项目种子数据，仅供界面预览'
                  : '正在读取商品信息'
              }}
              <button v-if="productError" type="button" @click="loadProduct()">重试</button>
            </div>
            <div class="package-picker">
              <h2>选择套餐</h2>
              <button
                v-for="option in displayProduct.packages"
                :key="option.id"
                type="button"
                :aria-pressed="selectedPackage === option.id"
                @click="selectedPackage = option.id"
              >
                <strong>{{ option.name }}</strong
                ><b>{{ yuan(option.priceCents) }}</b>
              </button>
            </div>
            <p class="product-terms">文件格式、授权与交付内容以正式发布信息为准</p>
            <a
              v-if="displayProduct.packages.length"
              class="button button-primary product-action"
              :href="`/checkout?product=${displayProduct.id}&package=${selectedPackage}`"
              >查看购买信息 <span aria-hidden="true">↗</span></a
            >
            <p v-else class="inline-note">暂无可选套餐</p>
            <a v-if="displayProduct.id === 1001" class="shop-return" href="/shops/northstar"
              >查看 Northstar Design ↗</a
            >
          </div>
        </section>
      </template>
      <div v-else class="page-width empty-state not-found">
        <h1>{{ productLoading || !productError ? '正在读取作品' : '作品暂不可用' }}</h1>
        <button
          v-if="productError"
          class="button button-secondary"
          type="button"
          @click="loadProduct()"
        >
          重试
        </button>
        <a class="button button-primary" href="/discover">返回发现</a>
      </div>
    </template>

    <template v-else-if="path === '/checkout'">
      <div v-if="canShowProduct && packageOption" class="page-width narrow-page">
        <a class="back-link" :href="`/products/${displayProduct.id}`">← 返回作品详情</a>
        <div class="form-page-head">
          <h1>确认购买</h1>
          <p>演示购买仅保存在当前浏览器</p>
        </div>
        <div class="checkout-layout">
          <div class="checkout-main">
            <section class="quiet-panel">
              <h2>作品与套餐</h2>
              <div class="checkout-product">
                <div class="checkout-mini-art">
                  {{ displayProduct.shopName.charAt(0)
                  }}<span>{{ displayProduct.shopName.slice(1) }}</span>
                </div>
                <div>
                  <h3>{{ displayProduct.title }}</h3>
                  <p>{{ displayProduct.shopName }}</p>
                  <small>{{ packageOption.name }} 套餐</small>
                </div>
              </div>
              <div class="checkout-line">
                <span>套餐价格</span><strong>{{ yuan(packageOption.priceCents) }}</strong>
              </div>
            </section>
            <section class="quiet-panel">
              <h2>购买前确认</h2>
              <ul class="check-list">
                <li>文件内容与格式以作品发布信息为准</li>
                <li>付款完成后，购买记录将在内容库中查看</li>
                <li>退款和授权范围以正式规则为准</li>
              </ul>
            </section>
          </div>
          <aside class="checkout-summary">
            <span>本次金额</span><strong>{{ yuan(packageOption.priceCents) }}</strong>
            <button class="button button-primary" type="button" @click="completeDemoPurchase">完成演示购买</button
            ><a href="/checkout/result">查看其他支付状态 ↗</a>
          </aside>
        </div>
      </div>
      <div v-else class="page-width empty-state not-found">
        <h1>{{ productLoading || !productError ? '正在读取作品' : '购买信息暂不可用' }}</h1>
        <button
          v-if="productError"
          class="button button-secondary"
          type="button"
          @click="loadProduct()"
        >
          重试
        </button>
        <a class="button button-primary" href="/discover">返回发现</a>
      </div>
    </template>

    <template v-else-if="path === '/checkout/result'">
      <div class="page-width narrow-page result-page">
        <h1>支付状态预览</h1>
        <p>以下状态不对应真实订单</p>
        <div class="state-switch" role="group" aria-label="预览购买状态">
          <button
            v-for="item in [
              { value: 'waiting', label: '等待支付' },
              { value: 'checking', label: '正在确认' },
              { value: 'ready', label: '已可使用' },
              { value: 'late', label: '确认延迟' },
            ]"
            :key="item.value"
            type="button"
            :aria-pressed="resultState === item.value"
            @click="resultState = item.value as typeof resultState"
          >
            {{ item.label }}
          </button>
        </div>
        <div class="result-paper">
          <span class="result-symbol" :class="resultState">{{
            resultState === 'ready' ? '✓' : resultState === 'late' ? '!' : '↗'
          }}</span>
          <h2>
            {{
              {
                waiting: '等待完成支付',
                checking: '正在确认付款',
                ready: '作品已加入内容库',
                late: '确认延迟',
              }[resultState]
            }}
          </h2>
          <p>
            {{
              {
                waiting: '完成支付后，订单会自动更新',
                checking: '正在核对支付结果，请稍候再查看',
                ready: '你可以从内容库查看已获得的作品',
                late: '请勿重复付款，稍后查看订单状态或联系支持',
              }[resultState]
            }}
          </p>
          <div class="result-actions">
            <a class="button button-secondary" href="/library">查看内容库</a
            ><a class="underlined-link" href="/orders/demo">预览订单详情 ↗</a>
          </div>
        </div>
      </div>
    </template>

    <template v-else-if="path === '/library'">
      <div class="page-width library-page">
        <div class="inner-hero-copy">
          <h1>内容库</h1>
          <p>已购作品与版本更新</p>
        </div>
        <div v-if="!localPurchase" class="preview-ribbon">
          <span>账户与授权接口未接入，以下为状态示例</span
          ><button type="button" @click="showLibraryPreview = !showLibraryPreview">
            {{ showLibraryPreview ? '查看真实空状态' : '预览已购状态' }} ↗
          </button>
        </div>
        <div v-if="!showLibraryPreview && !localPurchase" class="empty-state library-empty">
          <div class="empty-mark">✳</div>
          <h2>暂无已购作品</h2>
          <p>购买后可在这里查看作品</p>
          <a class="button button-primary" href="/discover">去发现作品</a>
        </div>
        <div v-else class="library-preview">
          <div class="library-item-art">
            <span>Northstar Design</span><strong>把经历<br />排成故事</strong>
          </div>
          <div class="library-item-copy">
            <span class="demo-badge">{{ localPurchase ? '本机演示记录' : '状态示例 · 非真实购买' }}</span>
            <h2>{{ localPurchase?.title ?? '求职材料模板包' }}</h2>
            <p>{{ localPurchase?.packageName ?? 'Basic' }} 套餐 · 示例版本 v1.2</p>
            <div class="library-status">
              <span>有更新可查看</span><small>原有版本仍会保留</small>
            </div>
            <p class="muted">下载与文件访问需验证真实授权，当前不可用</p>
            <button class="button button-secondary" type="button" disabled>
              等待内容库接口接入</button
            ><a href="/orders/demo">预览订单详情 ↗</a>
          </div>
        </div>
      </div>
    </template>

    <template v-else-if="path === '/orders/demo'">
      <div class="page-width narrow-page order-page">
        <a class="back-link" href="/library">← 返回内容库</a>
        <div class="form-page-head">
          <h1>订单详情</h1>
        </div>
        <div class="preview-ribbon">
          <span>界面示例，未创建订单或文件权限</span
          ><button type="button" @click="showOrderPreview = !showOrderPreview">
            {{ showOrderPreview ? '收起示例' : '查看订单示例' }} ↗
          </button>
        </div>
        <div v-if="showOrderPreview" class="order-sheet">
          <div><span>示例订单</span><strong>已完成 · 可查看作品</strong></div>
          <div><span>作品</span><strong>求职材料模板包 / Basic</strong></div>
          <div><span>订单来源</span><strong>单次购买</strong></div>
          <div><span>退款</span><strong>申请与处理接口尚未接入</strong></div>
          <p>若正式订单退款成功，仅撤销该订单产生的访问权限，其他合法来源的权限不受影响</p>
        </div>
        <div v-else class="empty-state">
          <h2>暂无订单</h2>
          <p>购买后可在这里查看状态</p>
          <a class="button button-secondary" href="/discover">继续探索</a>
        </div>
      </div>
    </template>

    <template v-else-if="path === '/login'">
      <div class="page-width login-layout">
        <div class="login-art">
          <img src="/images/creator-studio-1280.webp" alt="阳光下摆着纸张与色卡的创作桌面" />
        </div>
        <div class="login-copy">
          <h1>登录</h1>
          <form @submit.prevent="signInWithDemoRole">
            <label for="login-email">邮箱</label
            ><input
              id="login-email"
              type="email"
              required
              autocomplete="email"
              placeholder="name@example.com"
            /><label for="login-password">密码</label
            ><input
              id="login-password"
              type="password"
              required
              autocomplete="current-password"
              placeholder="输入密码"
            />
            <div class="state-switch login-role-switch" role="group" aria-label="选择演示身份">
              <button type="button" :aria-pressed="loginRole === 'user'" @click="loginRole = 'user'">用户</button>
              <button type="button" :aria-pressed="loginRole === 'creator'" @click="loginRole = 'creator'">店家</button>
              <button type="button" :aria-pressed="loginRole === 'admin'" @click="loginRole = 'admin'">管理员</button>
            </div>
            <button class="button button-primary" type="submit">进入演示</button>
          </form>
          <p v-if="signInNotice" class="inline-note" role="status">
            登录接口尚未接入，当前不会提交账户信息
          </p>
          <a class="underlined-link" href="/discover">先看看作品 ↗</a>
        </div>
      </div>
    </template>

    <template v-else
      ><div class="page-width empty-state not-found">
        <h1>页面不存在</h1>
        <a class="button button-primary" href="/discover">返回发现</a>
      </div></template
    >
  </main>
  <footer class="inner-footer page-width">
    <nav aria-label="页脚导航">
      <a href="/discover">发现</a><a href="/creator/start">成为创作者</a><a href="/">首页</a>
    </nav>
  </footer>
</template>
