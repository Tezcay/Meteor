<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AccountSidebar from '../components/AccountSidebar.vue'
import InnerHeader from '../components/InnerHeader.vue'
import { getProductDetail, getPublishedProducts } from '../api/products'
import { categoryOptions, parseCategory, subcategoryLabel } from '../data/categories'
import { getLocalPurchases, getLocalRole, saveLocalPurchase, saveLocalRole } from '../data/localStore'
import type { LocalRole } from '../data/localStore'
import {
  artworkCollections,
  illustrationTopics,
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
const topic = ref(String(route.query.topic ?? ''))
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
const catalogSort = ref<'featured' | 'newest' | 'rating'>('featured')
const catalogPage = ref(1)
const catalogPageSize = 12
const signInNotice = ref(false)
function roleForNextPath(next: string): Exclude<LocalRole, 'visitor'> {
  if (next.startsWith('/admin')) return 'admin'
  if (next.startsWith('/creator')) return 'creator'
  return 'user'
}
const loginRole = ref<Exclude<LocalRole, 'visitor'>>(
  roleForNextPath(String(route.query.next ?? '')),
)
const activeImageIndex = ref(0)
const localRating = ref(0)
const ratingNotice = ref('')
const collectionSaved = ref(false)
const collectionViewer = ref<HTMLDialogElement | null>(null)
const localPurchases = ref(getLocalPurchases())
const previewPurchase = {
  id: 'preview-order',
  productId: 1001,
  title: '求职材料模板包',
  shopName: 'Northstar Design',
  packageName: 'Basic',
  purchasedAt: '2026-09-20T09:00:00.000Z',
}
const libraryEntries = computed(() => localPurchases.value.length ? localPurchases.value : showLibraryPreview.value ? [previewPurchase] : [])
const orderEntries = computed(() => localPurchases.value.length ? localPurchases.value : showOrderPreview.value ? [previewPurchase] : [])
const activeOrderId = computed(() => {
  const requested = String(route.query.order ?? '')
  return orderEntries.value.some((purchase) => purchase.id === requested)
    ? requested
    : (orderEntries.value[0]?.id ?? '')
})
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
const relatedTopics = computed(() =>
  collection.value?.category === 'illustration'
    ? illustrationTopics.filter((item) => item.slug !== collection.value?.slug)
    : [],
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
type ProductPreview = { label: string; eyebrow: string; title: string; subtitle: string; footer: string }
const productPreviewOptions = computed<ProductPreview[]>(() => {
  if (displayProduct.value.id !== 1001) {
    return [
      { label: '封面', eyebrow: displayProduct.value.shopName + ' / DIGITAL WORKS', title: displayProduct.value.title, subtitle: '精选预览', footer: 'PREVIEW · 01' },
      { label: '内容', eyebrow: 'CONTENTS / 02', title: '内容结构', subtitle: '清晰可见', footer: 'DETAILS · 02' },
      { label: '授权', eyebrow: 'LICENSE / 03', title: '交付说明', subtitle: '购买后查看', footer: 'DELIVERY · 03' },
    ]
  }
  return [
    { label: '封面', eyebrow: 'NORTHSTAR DESIGN / DIGITAL WORKS', title: '把经历', subtitle: '排成故事', footer: 'RESUME · LETTER · PORTFOLIO' },
    { label: '版式示意', eyebrow: 'GRID / TYPE / 02', title: '清晰层级', subtitle: '让信息先被看见', footer: 'LAYOUT STUDY · 02' },
    { label: '内容说明', eyebrow: 'DELIVERY / 03', title: '一套完整', subtitle: '从简历到作品集', footer: 'EDITABLE FILES · 03' },
  ]
})
const activeProductPreview = computed(() => productPreviewOptions.value[activeImageIndex.value] ?? productPreviewOptions.value[0]!)
const productRating = computed(() => displayProduct.value.id === 1001 ? { score: '4.8', count: 24 } : { score: '4.6', count: 8 })
const packageOption = computed(
  () =>
    displayProduct.value.packages.find((item) => item.id === selectedPackage.value) ??
    displayProduct.value.packages[0]!,
)
const selectedPackagePresentation = computed(() => {
  const option = packageOption.value
  if (!option) {
    return {
      items: ['交付文件与使用说明', '授权范围以正式发布信息为准'],
      note: '作品信息会在购买前再次确认',
    }
  }
  if (displayProduct.value.id === 1001 && option.name === 'Pro') {
    return {
      items: ['简历、求职信与完整作品集版式', '可编辑文本与图片替换页', '示例内容与使用说明'],
      note: '适合需要一次整理完整求职材料的场景',
    }
  }
  if (displayProduct.value.id === 1001) {
    return {
      items: ['简历与求职信基础版式', '可编辑文本与基础样式', '使用说明'],
      note: '适合先从一套清晰的基础材料开始',
    }
  }
  return {
    items: ['作品文件与使用说明', '购买后可在内容库查看', '授权范围以正式发布信息为准'],
    note: '具体内容以作品发布信息为准',
  }
})
const checkoutRequiresSignIn = computed(() => getLocalRole() === 'visitor')
const isDemoProduct = computed(() => !product.value)
const featuredIllustrationTopics = computed(() => category.value === 'illustration' ? illustrationTopics : [])
const featuredCollections = computed(() =>
  artworkCollections.filter(
    (item) => category.value === 'all' || item.category === category.value,
  ),
)
const catalogWorks = computed(() => {
  const keyword = query.value.trim().toLocaleLowerCase()
  const items = showcaseItems.filter((work) => {
    if (category.value !== 'all' && work.category !== category.value) return false
    if (topic.value && work.topic !== topic.value) return false
    if (!keyword) return true
    return [work.title, work.maker, work.description, work.tags.join(' ')]
      .join(' ')
      .toLocaleLowerCase()
      .includes(keyword)
  })

  return [...items].sort((left, right) => {
    if (catalogSort.value === 'newest') return right.variant - left.variant
    if (catalogSort.value === 'rating') return Number(right.featured) - Number(left.featured)
    return Number(right.featured) - Number(left.featured) || left.variant - right.variant
  })
})
const visibleCatalogWorks = computed(() => catalogWorks.value.slice(0, catalogPage.value * catalogPageSize))
const hasMoreCatalogWorks = computed(() => visibleCatalogWorks.value.length < catalogWorks.value.length)
const loadedCatalogProducts = computed(() =>
  category.value === 'all' || category.value === 'templates' ? catalog.value : [],
)
watch(
  () => route.query.next,
  (next) => {
    if (props.path === '/login' && !signInNotice.value) {
      loginRole.value = roleForNextPath(String(next ?? ''))
    }
  },
)

const section = computed(() =>
  props.path === '/library' || props.path.startsWith('/orders') ? 'account' : 'discover',
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

watch([query, category, topic], () => {
  if (props.path !== '/discover') return
  if (searchTimer) clearTimeout(searchTimer)
  catalogRequest?.abort()
  catalog.value = []
  catalogLoading.value = true
  catalogError.value = false
  catalogPage.value = 1
  searchTimer = setTimeout(() => void loadCatalog(), 250)
})

onMounted(async () => {
  window.addEventListener('meteor:purchases-change', refreshLocalPurchases)
  if (props.path === '/discover') void loadCatalog()
  if (props.path === '/shops/northstar' || isProductPath.value || props.path === '/checkout') {
    loadLocalRating()
    void loadProduct()
  }
})
onBeforeUnmount(() => {
  window.removeEventListener('meteor:purchases-change', refreshLocalPurchases)
  request?.abort()
  catalogRequest?.abort()
  if (searchTimer) clearTimeout(searchTimer)
})

function refreshLocalPurchases() {
  localPurchases.value = getLocalPurchases()
}

function setCategory(value: DiscoveryCategory) {
  category.value = value
  const nextQuery = { ...route.query }
  if (value === 'all') delete nextQuery.category
  else nextQuery.category = value
  if (value !== 'illustration') delete nextQuery.topic
  void router.replace({ path: '/discover', query: nextQuery })
}

watch(
  () => route.fullPath,
  () => {
    category.value = parseCategory(String(route.query.category ?? ''))
    query.value = String(route.query.q ?? '')
    topic.value = String(route.query.topic ?? '')
    selectedPackage.value = Number(route.query.package ?? 2001)
    activeImageIndex.value = 0
    collectionSaved.value = false
    localPurchases.value = getLocalPurchases()
    if (props.path === '/checkout/result' && route.query.demo === 'ready') resultState.value = 'ready'
    product.value = null
    productError.value = false
    if (props.path === '/discover') void loadCatalog()
    if (props.path === '/shops/northstar' || isProductPath.value || props.path === '/checkout') {
      loadLocalRating()
      void loadProduct()
    }
  },
)

function packageOptionDescription(name: string) {
  if (displayProduct.value.id !== 1001) return '查看交付内容与授权范围'
  return name === 'Pro' ? '包含完整作品集版式' : '简历与求职信基础版式'
}

function selectProductPreview(index: number) {
  const count = productPreviewOptions.value.length
  if (!count) return
  activeImageIndex.value = (index + count) % count
}
function loadLocalRating() {
  try {
    localRating.value = Number(localStorage.getItem('meteor.product.rating.' + displayProduct.value.id) ?? 0)
  } catch {
    localRating.value = 0
  }
}
function setLocalRating(value: number) {
  localRating.value = value
  ratingNotice.value = '已记录 ' + value + ' 星评分，仅保存在当前浏览器'
  try {
    localStorage.setItem('meteor.product.rating.' + displayProduct.value.id, String(value))
  } catch {
    // Local preview still works when storage is unavailable.
  }
}

function selectCollectionImage(index: number) {
  const imageCount = collection.value?.images.length ?? 0
  if (!imageCount) return
  activeImageIndex.value = (index + imageCount) % imageCount
}

function openCollectionViewer() {
  if (!activeCollectionImage.value || collectionViewer.value?.open) return
  collectionViewer.value?.showModal()
}

function closeCollectionViewer() {
  collectionViewer.value?.close()
}

function closeCollectionViewerFromBackdrop(event: MouseEvent) {
  if (event.target === collectionViewer.value) closeCollectionViewer()
}

function collectionLink(item: { category: string }) {
  const slug = collectionSlugFor(item as (typeof showcaseItems)[number])
  return slug ? `/collections/${slug}` : undefined
}

function clearFilters() {
  query.value = ''
  setCategory('all')
}

function loadMoreCatalogWorks() {
  catalogPage.value += 1
}

function completeDemoPurchase() {
  if (!packageOption.value) return
  if (checkoutRequiresSignIn.value) {
    void router.push({ path: '/login', query: { next: route.fullPath } })
    return
  }
  saveLocalPurchase({
    productId: displayProduct.value.id,
    title: displayProduct.value.title,
    shopName: displayProduct.value.shopName,
    packageName: packageOption.value.name,
  })
  localPurchases.value = getLocalPurchases()
  resultState.value = 'ready'
  void router.push({ path: '/checkout/result', query: { demo: 'ready' } })
}

function formatPurchaseDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }).format(new Date(value))
}

function signInWithDemoRole() {
  const requested = String(route.query.next ?? '')
  const next = requested.startsWith('/') && !requested.startsWith('//') ? requested : ''
  const targetRole = roleForNextPath(next)
  saveLocalRole(loginRole.value)
  signInNotice.value = true
  const canContinue = Boolean(next) && targetRole === loginRole.value
  const destination = canContinue
    ? next
    : { user: '/library', creator: '/creator/works', admin: '/admin/reviews' }[loginRole.value]
  void router.replace(destination)
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
        <section
          v-if="!query.trim() && (featuredIllustrationTopics.length || featuredCollections.length)"
          class="collection-index"
          aria-labelledby="collection-index-title"
        >
          <div class="collection-index-heading">
            <div>
              <h1 id="collection-index-title">{{ category === 'illustration' ? '插画脉络' : '专题' }}</h1>
              <p>{{ category === 'illustration' ? '从一个主题进入，再浏览全部作品' : '从一组完整的作品开始浏览' }}</p>
            </div>
          </div>
          <div v-if="category === 'illustration'" class="illustration-topic-grid" aria-label="插画主题">
            <a
              v-for="item in featuredIllustrationTopics"
              :key="item.slug"
              class="illustration-topic-card"
              :class="{ 'is-active': topic === item.slug }"
              :href="`/collections/${item.slug}`"
              :aria-current="topic === item.slug ? 'page' : undefined"
            >
              <div class="illustration-topic-image">
                <img :src="item.cover.src" :alt="item.cover.alt" loading="lazy" />
                <span>{{ item.count }} 件</span>
              </div>
              <div class="illustration-topic-copy">
                <h2>{{ item.title }}</h2>
                <p>{{ item.description }}</p>
                <span>浏览此主题 <i aria-hidden="true">→</i></span>
              </div>
            </a>
          </div>
          <div v-else class="collection-index-grid">
            <a
              v-for="item in featuredCollections"
              :key="item.slug"
              class="collection-index-card"
              :href="`/collections/${item.slug}`"
            >
              <div class="collection-index-image">
                <img :src="item.images[0]?.src" :alt="item.images[0]?.alt" loading="lazy" />
                <span>{{ item.images.length }} 件作品</span>
              </div>
              <div class="collection-index-copy">
                <h2>{{ item.title }}</h2>
                <p>{{ item.summary }}</p>
                <span>{{ item.maker }} <i aria-hidden="true">→</i></span>
              </div>
            </a>
          </div>
        </section>
        <section class="catalog-section" aria-labelledby="catalog-title">
          <div class="catalog-heading">
            <div>
              <h1 id="catalog-title">全部作品</h1>
              <p>{{ catalogWorks.length + loadedCatalogProducts.length }} 件可浏览内容</p>
            </div>
            <label class="catalog-sort">
              <span>排序</span>
              <select v-model="catalogSort" aria-label="作品排序">
                <option value="featured">编辑精选</option>
                <option value="newest">最新加入</option>
                <option value="rating">评分优先</option>
              </select>
            </label>
          </div>
          <div v-if="visibleCatalogWorks.length || loadedCatalogProducts.length" class="catalog-grid">
            <article v-for="item in visibleCatalogWorks" :key="`${item.category}-${item.title}`" class="catalog-work">
              <component
                :is="collectionLink(item) ? 'a' : 'div'"
                :href="collectionLink(item) || undefined"
                class="catalog-work-art"
                :class="[`art-${item.art}`, `variant-${item.variant}`, { 'is-static': !collectionLink(item) }]"
                :aria-label="collectionLink(item) ? `查看系列：${item.title}` : undefined"
              >
                <img v-if="item.image" :src="item.image" :alt="item.description" loading="lazy" />
                <div v-else class="demo-piece" :class="[`demo-${item.category}`, `variant-${item.variant}`]" aria-hidden="true">
                  <small>{{ item.mark }}</small>
                  <strong>{{ item.title }}</strong>
                  <div class="demo-shapes"><i></i><i></i><i></i><i></i></div>
                </div>
              </component>
              <div class="catalog-work-copy">
                <span>{{ subcategoryLabel(item.category, item.subcategory) }}</span>
                <h2>{{ item.title }}</h2>
                <p>{{ item.maker }}</p>
              </div>
            </article>
            <a v-for="item in loadedCatalogProducts" :key="`api-${item.id}`" class="catalog-work" :href="`/products/${item.id}`">
              <div class="catalog-work-art catalog-api-piece">
                <span>{{ item.shopName }}</span>
                <strong>{{ item.title }}</strong>
                <small>已发布作品</small>
              </div>
              <div class="catalog-work-copy">
                <span>数字作品</span>
                <h2>{{ item.title }}</h2>
                <p>{{ item.summary.replace(/[。！？]+$/, '') }}</p>
              </div>
            </a>
          </div>
          <div v-else class="catalog-empty">
            <h2>这里还没有匹配的作品</h2>
            <button class="button button-secondary" type="button" @click="clearFilters">清除筛选</button>
          </div>
          <div v-if="hasMoreCatalogWorks" class="catalog-more">
            <button class="button button-secondary" type="button" @click="loadMoreCatalogWorks">加载更多作品</button>
          </div>
        </section>      </section>
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
            :aria-label="`放大查看图像：${activeCollectionImage?.alt}`"
            @click="openCollectionViewer"
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
              <button type="button" aria-label="下一张图像" @click="openCollectionViewer">→</button>
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
              :aria-label="`放大查看第 ${index + 1} 张：${image.alt}`"
              @click="selectCollectionImage(index); openCollectionViewer()"
            >
              <img :src="image.src" :alt="image.alt" loading="lazy" />
              <span aria-hidden="true">{{ String(index + 1).padStart(2, '0') }}</span>
            </button>
          </figure>
        </div>
      </section>
      <section class="page-width collection-continue" aria-labelledby="collection-continue-title">
        <div>
          <h2 id="collection-continue-title">继续浏览</h2>
          <p>换一个主题，继续看完整图集。</p>
        </div>
        <nav class="collection-continue-links" aria-label="相关主题">
          <a v-for="item in relatedTopics" :key="item.slug" :href="`/collections/${item.slug}`">{{ item.title }} <span aria-hidden="true">→</span></a>
          <a class="underlined-link" :href="`/discover?category=${collection.category}`">更多{{ collectionCategoryLabel }}作品</a>
        </nav>
      </section>
      <dialog
        ref="collectionViewer"
        class="collection-viewer"
        aria-labelledby="collection-viewer-title"
        @click="closeCollectionViewerFromBackdrop"
      >
        <div class="collection-viewer-shell">
          <header>
            <p id="collection-viewer-title">{{ collection.title }} · {{ activeImageIndex + 1 }} / {{ collection.images.length }}</p>
            <button type="button" aria-label="关闭图像查看器" @click="closeCollectionViewer">×</button>
          </header>
          <img v-if="activeCollectionImage" :src="activeCollectionImage.src" :alt="activeCollectionImage.alt" />
          <footer>
            <button type="button" aria-label="上一张图像" @click="selectCollectionImage(activeImageIndex - 1)">←</button>
            <span>{{ activeCollectionImage?.alt }}</span>
            <button type="button" aria-label="下一张图像" @click="selectCollectionImage(activeImageIndex + 1)">→</button>
          </footer>
        </div>
      </dialog>
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
            <div class="product-cover" :class="'product-cover-variant-' + activeImageIndex">
              <span>{{ activeProductPreview.eyebrow }}</span>
              <strong>{{ activeProductPreview.title }}<br /><i>{{ activeProductPreview.subtitle }}</i></strong>
              <div class="product-cover-rule"></div>
              <small>{{ activeProductPreview.footer }}</small>
            </div>
            <div class="product-thumbs" aria-label="作品预览切换">
              <button
                v-for="(preview, index) in productPreviewOptions"
                :key="preview.label"
                type="button"
                :aria-pressed="activeImageIndex === index"
                @click="selectProductPreview(index)"
              >
                <span>{{ String(index + 1).padStart(2, '0') }}</span>
                <strong>{{ preview.label }}</strong>
                <small>{{ preview.title }}</small>
              </button>
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
            <section class="product-rating" aria-label="作品评分">
              <div class="product-rating-summary">
                <span class="rating-stars" aria-hidden="true">★★★★★</span>
                <strong>{{ productRating.score }}</strong>
                <span>{{ productRating.count }} 人评分</span>
              </div>
              <div class="rating-input" role="group" aria-label="给作品评分">
                <span>为它打分</span>
                <button
                  v-for="score in 5"
                  :key="score"
                  type="button"
                  :class="{ active: score <= localRating }"
                  :aria-label="score + ' 星'"
                  :aria-pressed="localRating === score"
                  @click="setLocalRating(score)"
                >★</button>
              </div>
              <p v-if="ratingNotice" role="status">{{ ratingNotice }}</p>
            </section>
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
                <span><strong>{{ option.name }}</strong><small>{{ packageOptionDescription(option.name) }}</small></span
                ><b>{{ yuan(option.priceCents) }}</b>
              </button>
            </div>
            <section v-if="displayProduct.packages.length" class="package-delivery" aria-live="polite">
              <h3>{{ packageOption.name }} 包含</h3>
              <ul><li v-for="item in selectedPackagePresentation.items" :key="item">{{ item }}</li></ul>
              <p>{{ selectedPackagePresentation.note }}</p>
            </section>
            <p class="product-terms">文件格式、授权与交付内容以正式发布信息为准</p>
            <a
              v-if="displayProduct.packages.length"
              class="button button-primary product-action"
              :href="`/checkout?product=${displayProduct.id}&package=${selectedPackage}`"
              >继续购买</a
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
            <p v-if="checkoutRequiresSignIn" class="checkout-login-hint">登录后才能保存购买记录与访问授权。</p>
            <button class="button button-primary" type="button" @click="completeDemoPurchase">{{ checkoutRequiresSignIn ? '登录后继续' : '完成演示购买' }}</button
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
      <div class="page-width workspace-layout account-layout">
        <AccountSidebar active="library" />
        <div class="workspace-content library-page">
          <div class="workspace-heading">
            <h1>内容库</h1>
            <p>已购作品与版本更新</p>
          </div>
          <div v-if="!localPurchases.length" class="preview-ribbon">
            <span>账户与授权接口未接入，以下为状态示例</span
            ><button type="button" @click="showLibraryPreview = !showLibraryPreview">
              {{ showLibraryPreview ? '查看真实空状态' : '预览已购状态' }}
            </button>
          </div>
          <div v-if="!libraryEntries.length" class="empty-state library-empty">
            <div class="empty-mark">✳</div>
            <h2>暂无已购作品</h2>
            <p>购买后可在这里查看作品</p>
            <a class="button button-primary" href="/discover">去发现作品</a>
          </div>
          <div v-else class="library-list">
            <article v-for="purchase in libraryEntries" :key="purchase.id" class="library-preview">
              <div class="library-item-art">
                <span>{{ purchase.shopName }}</span><strong>{{ purchase.title }}</strong>
              </div>
              <div class="library-item-copy">
                <span class="demo-badge">{{ purchase.id === 'preview-order' ? '状态示例 · 非真实购买' : '本机演示记录' }}</span>
                <h2>{{ purchase.title }}</h2>
                <p>{{ purchase.packageName }} 套餐 · 获得于 {{ formatPurchaseDate(purchase.purchasedAt) }}</p>
                <div class="library-status">
                  <span>已获得访问权限</span><small>版本更新将在这里显示</small>
                </div>
                <p class="muted">下载与文件访问需验证真实授权，当前不可用</p>
                <button class="button button-secondary" type="button" disabled>等待内容库接口接入</button>
                <a :href="`/orders/demo?order=${purchase.id}`">查看订单详情</a>
              </div>
            </article>
          </div>
        </div>
      </div>
    </template>

    <template v-else-if="path === '/orders/demo'">
      <div class="page-width workspace-layout account-layout">
        <AccountSidebar active="orders" />
        <div class="workspace-content order-page">
          <div class="workspace-heading">
            <h1>订单记录</h1>
            <p>购买记录与授权状态</p>
          </div>
          <div v-if="!localPurchases.length" class="preview-ribbon">
            <span>订单接口未接入，以下为状态示例</span
            ><button type="button" @click="showOrderPreview = !showOrderPreview">
              {{ showOrderPreview ? '查看真实空状态' : '预览订单示例' }}
            </button>
          </div>
          <div v-if="!orderEntries.length" class="empty-state">
            <h2>暂无订单</h2>
            <p>购买后可在这里查看状态</p>
            <a class="button button-secondary" href="/discover">继续探索</a>
          </div>
          <section v-else class="order-list" aria-label="购买订单列表">
            <article
              v-for="purchase in orderEntries"
              :key="purchase.id"
              class="order-sheet"
              :class="{ 'is-current': purchase.id === activeOrderId }"
              :aria-current="purchase.id === activeOrderId ? 'true' : undefined"
            >
              <div><span>订单编号</span><strong>#{{ purchase.id.slice(-6) }}</strong></div>
              <div><span>作品</span><strong>{{ purchase.title }}</strong></div>
              <div><span>套餐</span><strong>{{ purchase.packageName }}</strong></div>
              <div><span>购买时间</span><strong>{{ formatPurchaseDate(purchase.purchasedAt) }}</strong></div>
              <div><span>访问状态</span><strong>已获得授权</strong></div>
              <p>{{ purchase.id === 'preview-order' ? '这是演示状态，不会创建真实订单或访问权限。' : '下载权限与退款状态将在接入真实订单和授权接口后更新。' }}</p>
            </article>
          </section>
        </div>
      </div>
    </template>

    <template v-else-if="path === '/login'">      <div class="page-width login-layout">
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
          <p class="login-intent">
            当前演示身份：<strong>{{ loginRole === 'creator' ? '创作者 / 店家' : loginRole === 'admin' ? '管理员' : '普通用户' }}</strong>
            · 登录后会进入对应工作区
          </p>
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
