<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import InnerHeader from '../components/InnerHeader.vue'
import { getCurrentUser, login as loginWithApi, roleFromCurrentUser } from '../api/auth'
import { createOrder, getOrder } from '../api/orders'
import type { OrderDetailResponse } from '../api/orders'
import { applyTestPayment } from '../api/payments'
import { getProductDetail, getPublishedProducts } from '../api/products'
import { categoryOptions, parseCategory, subcategoryLabel } from '../data/categories'
import { saveLocalRole } from '../data/localStore'
import type { LocalRole } from '../data/localStore'
import { ApiError } from '../types/auth'
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
const resultOrder = ref<OrderDetailResponse | null>(null)
const resultLoading = ref(false)
const resultError = ref('')
const product = ref<ProductDetailResponse | null>(null)
const productError = ref(false)
const productLoading = ref(false)
const catalog = ref<ProductListItemResponse[]>([])
const catalogLoading = ref(false)
const catalogError = ref(false)
const catalogSort = ref<'featured' | 'newest' | 'rating'>('featured')
const catalogPage = ref(1)
const catalogPageSize = 12
const signInNotice = ref('')
const orderLoading = ref(false)
const orderError = ref('')
const paymentLoading = ref(false)
const paymentError = ref('')
const paymentNotice = ref('')
const testPaymentEventId = ref('')
const testPaymentTransactionId = ref('')
const checkoutAuthenticated = ref<boolean | null>(null)
const demoAccountsVisible = import.meta.env.DEV
function roleForNextPath(next: string): Exclude<LocalRole, 'visitor'> {
  if (next.startsWith('/admin')) return 'admin'
  if (next.startsWith('/creator')) return 'creator'
  return 'user'
}
const loginRole = ref<Exclude<LocalRole, 'visitor'>>(
  roleForNextPath(String(route.query.next ?? '')),
)
const loginEmail = ref(
  demoAccountsVisible
    ? loginRole.value === 'creator'
      ? 'creator@meteor.local'
      : 'buyer@meteor.local'
    : '',
)
const loginPassword = ref(demoAccountsVisible ? 'password' : '')
const loginLoading = ref(false)
const loginError = ref('')
const activeImageIndex = ref(0)
const localRating = ref(0)
const ratingNotice = ref('')
const collectionSaved = ref(false)
const collectionViewer = ref<HTMLDialogElement | null>(null)

function selectLoginRole(role: Exclude<LocalRole, 'visitor'>) {
  loginRole.value = role
  if (!demoAccountsVisible) return
  if (role === 'user') loginEmail.value = 'buyer@meteor.local'
  if (role === 'creator') loginEmail.value = 'creator@meteor.local'
  if (!loginPassword.value) loginPassword.value = 'password'
}

function checkoutIdempotency(productId: number, packageId: number) {
  const storageKey = `meteor.checkout.idempotency.${productId}.${packageId}`
  const prefix = `checkout-${productId}-${packageId}-`
  const existing = sessionStorage.getItem(storageKey)
  if (existing?.startsWith(prefix) && existing.length <= 100) {
    return { key: existing, storageKey }
  }
  const key = `${prefix}${crypto.randomUUID()}`
  sessionStorage.setItem(storageKey, key)
  return { key, storageKey }
}
let request: AbortController | null = null
let catalogRequest: AbortController | null = null
let searchTimer: ReturnType<typeof setTimeout> | null = null
const isProductPath = computed(() => /^\/products\/[1-9]\d*$/.test(props.path))
const isCollectionPath = computed(() => props.path.startsWith('/collections/'))
const collection = computed(() =>
  isCollectionPath.value
    ? getArtworkCollection(props.path.slice('/collections/'.length))
    : undefined,
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
type ProductPreview = {
  label: string
  eyebrow: string
  title: string
  subtitle: string
  footer: string
}
const productPreviewOptions = computed<ProductPreview[]>(() => {
  if (displayProduct.value.id !== 1001) {
    return [
      {
        label: '封面',
        eyebrow: displayProduct.value.shopName + ' / DIGITAL WORKS',
        title: displayProduct.value.title,
        subtitle: '精选预览',
        footer: 'PREVIEW · 01',
      },
      {
        label: '内容',
        eyebrow: 'CONTENTS / 02',
        title: '内容结构',
        subtitle: '清晰可见',
        footer: 'DETAILS · 02',
      },
      {
        label: '授权',
        eyebrow: 'LICENSE / 03',
        title: '交付说明',
        subtitle: '购买后查看',
        footer: 'DELIVERY · 03',
      },
    ]
  }
  return [
    {
      label: '封面',
      eyebrow: 'NORTHSTAR DESIGN / DIGITAL WORKS',
      title: '把经历',
      subtitle: '排成故事',
      footer: 'RESUME · LETTER · PORTFOLIO',
    },
    {
      label: '版式示意',
      eyebrow: 'GRID / TYPE / 02',
      title: '清晰层级',
      subtitle: '让信息先被看见',
      footer: 'LAYOUT STUDY · 02',
    },
    {
      label: '内容说明',
      eyebrow: 'DELIVERY / 03',
      title: '一套完整',
      subtitle: '从简历到作品集',
      footer: 'EDITABLE FILES · 03',
    },
  ]
})
const activeProductPreview = computed(
  () => productPreviewOptions.value[activeImageIndex.value] ?? productPreviewOptions.value[0]!,
)
const productRating = computed(() =>
  displayProduct.value.id === 1001 ? { score: '4.8', count: 24 } : { score: '4.6', count: 8 },
)
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
const checkoutRequiresSignIn = computed(() => checkoutAuthenticated.value === false)
const resultOrderId = computed(() => Number(route.query.orderId))
const isServerOrderResult = computed(
  () => Number.isSafeInteger(resultOrderId.value) && resultOrderId.value > 0,
)
const resultHasActiveEntitlement = computed(
  () => resultOrder.value?.status === 'PAID' && resultOrder.value.entitlementStatus === 'ACTIVE',
)
const resultHasUnavailableEntitlement = computed(
  () => resultOrder.value?.status === 'PAID' && resultOrder.value.entitlementStatus !== 'ACTIVE',
)
const isDemoProduct = computed(() => !product.value)
const featuredIllustrationTopics = computed(() =>
  category.value === 'illustration' ? illustrationTopics : [],
)
const featuredCollections = computed(() =>
  artworkCollections.filter((item) => category.value === 'all' || item.category === category.value),
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
const visibleCatalogWorks = computed(() =>
  catalogWorks.value.slice(0, catalogPage.value * catalogPageSize),
)
const hasMoreCatalogWorks = computed(
  () => visibleCatalogWorks.value.length < catalogWorks.value.length,
)
const loadedCatalogProducts = computed(() =>
  category.value === 'all' || category.value === 'templates' ? catalog.value : [],
)
watch(
  () => route.query.next,
  (next) => {
    if (props.path === '/login' && !signInNotice.value) {
      selectLoginRole(roleForNextPath(String(next ?? '')))
    }
  },
)

const section = computed(() => 'discover')

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
  if (props.path === '/discover') void loadCatalog()
  if (props.path === '/shops/northstar' || isProductPath.value || props.path === '/checkout') {
    loadLocalRating()
    void loadProduct()
  }
  if (props.path === '/checkout/result') void loadResultOrder()
  if (props.path === '/checkout') void loadCheckoutIdentity()
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
    product.value = null
    productError.value = false
    if (props.path === '/discover') void loadCatalog()
    if (props.path === '/shops/northstar' || isProductPath.value || props.path === '/checkout') {
      loadLocalRating()
      void loadProduct()
    }
    if (props.path === '/checkout/result') void loadResultOrder()
    if (props.path === '/checkout') void loadCheckoutIdentity()
  },
)

async function loadCheckoutIdentity() {
  try {
    checkoutAuthenticated.value = Boolean(await getCurrentUser())
  } catch {
    checkoutAuthenticated.value = false
  }
}

async function loadResultOrder() {
  if (!isServerOrderResult.value) {
    resultOrder.value = null
    resultError.value = '订单地址不完整，请从订单记录或结账页进入。'
    return
  }
  resultLoading.value = true
  resultError.value = ''
  try {
    resultOrder.value = await getOrder(resultOrderId.value)
    resultState.value = resultOrder.value.status === 'PAID' ? 'ready' : 'waiting'
  } catch (error) {
    resultOrder.value = null
    resultError.value = error instanceof Error ? error.message : '订单状态读取失败，请稍后重试。'
  } finally {
    resultLoading.value = false
  }
}

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
    localRating.value = Number(
      localStorage.getItem('meteor.product.rating.' + displayProduct.value.id) ?? 0,
    )
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

function openCollectionImage(index: number) {
  selectCollectionImage(index)
  openCollectionViewer()
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

async function completeDemoPurchase() {
  if (!packageOption.value) return
  if (checkoutAuthenticated.value === null) await loadCheckoutIdentity()
  if (checkoutRequiresSignIn.value) {
    void router.push({ path: '/login', query: { next: route.fullPath } })
    return
  }
  orderLoading.value = true
  orderError.value = ''
  try {
    if (!product.value) {
      throw new Error('当前无法连接交易后端，订单没有创建。请恢复服务后重试。')
    }
    const idempotency = checkoutIdempotency(product.value.id, packageOption.value.id)
    const order = await createOrder(product.value.id, packageOption.value.id, idempotency.key)
    sessionStorage.removeItem(idempotency.storageKey)
    resultState.value = 'waiting'
    void router.push({
      path: '/checkout/result',
      query: { orderId: String(order.id) },
    })
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      checkoutAuthenticated.value = false
      void router.push({ path: '/login', query: { next: route.fullPath } })
      return
    }
    orderError.value = error instanceof Error ? error.message : '订单创建失败，请稍后重试。'
  } finally {
    orderLoading.value = false
  }
}

async function completeLocalTestPayment() {
  const order = resultOrder.value
  if (!order) {
    paymentError.value = '订单状态尚未读取完成，请重新读取后再试。'
    return
  }
  paymentLoading.value = true
  paymentError.value = ''
  paymentNotice.value = ''
  testPaymentEventId.value ||= `evt-ui-${order.id}-${crypto.randomUUID()}`
  testPaymentTransactionId.value ||= `txn-ui-${order.id}-${crypto.randomUUID()}`
  try {
    const payment = await applyTestPayment(
      order.id,
      testPaymentEventId.value,
      testPaymentTransactionId.value,
      order.totalAmountCents,
      order.currency,
    )
    resultState.value = 'ready'
    paymentNotice.value = `订单已完成，访问权益 #${payment.entitlementGrantId ?? ''} 已生效。`
    await loadResultOrder()
  } catch (error) {
    paymentError.value = error instanceof Error ? error.message : '测试支付失败，请稍后重试。'
  } finally {
    paymentLoading.value = false
  }
}

async function signInWithDemoRole() {
  const requested = String(route.query.next ?? '')
  const next = requested.startsWith('/') && !requested.startsWith('//') ? requested : ''
  loginLoading.value = true
  loginError.value = ''
  signInNotice.value = ''
  if (loginRole.value === 'admin') {
    loginError.value = '管理员账号将在管理员后端接口完成后开放。当前可先使用用户或创作者账号。'
    loginLoading.value = false
    return
  }
  try {
    const user = await loginWithApi(loginEmail.value, loginPassword.value)
    const actualRole = roleFromCurrentUser(user)
    saveLocalRole(actualRole)
    const targetRole = roleForNextPath(next)
    signInNotice.value = `已连接后端会话：${user.displayName}`
    const canContinue = Boolean(next) && targetRole === actualRole
    const destination = canContinue
      ? next
      : { user: '/library', creator: '/creator/works', admin: '/admin/reviews' }[actualRole]
    void router.replace(destination)
  } catch (error) {
    loginError.value =
      error instanceof Error ? error.message : '登录失败，请检查后端是否启动和账号密码。'
  } finally {
    loginLoading.value = false
  }
}
</script>

<template>
  <InnerHeader :section="section" />
  <main id="page-main" class="inner-main" tabindex="-1">
    <template v-if="path === '/discover'">
      <section class="page-width discover-body" aria-label="浏览作品">
        <h1 class="sr-only">发现作品</h1>
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
              <h2 id="collection-index-title">
                {{ category === 'illustration' ? '插画脉络' : '专题' }}
              </h2>
              <p>
                {{
                  category === 'illustration'
                    ? '从一个主题进入，再浏览全部作品'
                    : '从一组完整的作品开始浏览'
                }}
              </p>
            </div>
          </div>
          <div
            v-if="category === 'illustration'"
            class="illustration-topic-grid"
            aria-label="插画主题"
          >
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
              <h2 id="catalog-title">全部作品</h2>
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
          <div
            v-if="visibleCatalogWorks.length || loadedCatalogProducts.length"
            class="catalog-grid"
          >
            <article
              v-for="item in visibleCatalogWorks"
              :key="`${item.category}-${item.title}`"
              class="catalog-work"
            >
              <component
                :is="collectionLink(item) ? 'a' : 'div'"
                :href="collectionLink(item) || undefined"
                class="catalog-work-art"
                :class="[
                  `art-${item.art}`,
                  `variant-${item.variant}`,
                  { 'is-static': !collectionLink(item) },
                ]"
                :aria-label="collectionLink(item) ? `查看系列：${item.title}` : undefined"
              >
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
              </component>
              <div class="catalog-work-copy">
                <span>{{ subcategoryLabel(item.category, item.subcategory) }}</span>
                <h2>{{ item.title }}</h2>
                <p>{{ item.maker }}</p>
              </div>
            </article>
            <a
              v-for="item in loadedCatalogProducts"
              :key="`api-${item.id}`"
              class="catalog-work"
              :href="`/products/${item.id}`"
            >
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
            <button class="button button-secondary" type="button" @click="clearFilters">
              清除筛选
            </button>
          </div>
          <div v-if="hasMoreCatalogWorks" class="catalog-more">
            <button class="button button-secondary" type="button" @click="loadMoreCatalogWorks">
              加载更多作品
            </button>
          </div>
        </section>
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
        <a href="/discover">发现</a><span>/</span
        ><a :href="`/discover?category=${collection.category}`">{{
          collection.category === 'photography' ? '摄影' : '插画'
        }}</a
        ><span>/</span><span>{{ collection.title }}</span>
      </div>
      <section class="page-width collection-layout" aria-labelledby="collection-title">
        <div class="collection-stage">
          <button
            class="collection-main-image"
            type="button"
            :aria-label="`放大查看图像：${activeCollectionImage?.alt}`"
            @click="openCollectionViewer"
          >
            <img
              v-if="activeCollectionImage"
              :src="activeCollectionImage.src"
              :alt="activeCollectionImage.alt"
            />
            <span class="collection-expand" aria-hidden="true">↗</span>
          </button>
          <div class="collection-thumbs" role="group" aria-label="图集预览">
            <button
              v-for="(image, index) in collection.images"
              :key="image.src"
              type="button"
              :aria-pressed="index === activeImageIndex"
              :aria-label="`查看第 ${index + 1} 张：${image.alt}`"
              @click="selectCollectionImage(index)"
              @keydown.left.prevent="selectCollectionImage(index - 1)"
              @keydown.right.prevent="selectCollectionImage(index + 1)"
            >
              <img :src="image.src" alt="" />
            </button>
          </div>
        </div>
        <aside class="collection-info">
          <div>
            <span class="quiet-label">{{
              collection.category === 'photography' ? '摄影作品' : '插画作品'
            }}</span>
            <h1 id="collection-title">{{ collection.title }}</h1>
            <a class="collection-maker" href="/discover">{{ collection.maker }}</a>
          </div>
          <div class="collection-controls">
            <p>
              {{ collection.images.length }} 张图像
              <span>{{ activeImageIndex + 1 }} / {{ collection.images.length }}</span>
            </p>
            <div>
              <button
                type="button"
                aria-label="上一张图像"
                @click="selectCollectionImage(activeImageIndex - 1)"
              >
                ←
              </button>
              <button type="button" aria-label="下一张图像" @click="openCollectionViewer">→</button>
            </div>
          </div>
          <button
            class="collection-save"
            type="button"
            :aria-pressed="collectionSaved"
            @click="collectionSaved = !collectionSaved"
          >
            {{ collectionSaved ? '已收藏' : '收藏作品' }}
          </button>
          <p class="collection-summary">{{ collection.summary }}</p>
          <a class="underlined-link" href="#collection-images">浏览全部图像</a>
        </aside>
      </section>
      <section id="collection-note" class="page-width collection-note">
        <h2>作品说明</h2>
        <p>
          这是一组手动编排的展示图像。作品集归属由创作者或编辑配置，不依据题材或视觉相似度自动合并。
        </p>
      </section>
      <section
        id="collection-images"
        class="page-width collection-sequence"
        aria-labelledby="collection-images-title"
      >
        <header class="collection-sequence-head">
          <h2 id="collection-images-title">完整图集</h2>
          <p>{{ collection.images.length }} 张展示图</p>
        </header>
        <div class="collection-sequence-grid" :class="`collection-sequence-${collection.category}`">
          <figure
            v-for="(image, index) in collection.images"
            :key="`sequence-${image.src}`"
            class="collection-sequence-item"
          >
            <button
              type="button"
              :aria-label="`放大查看第 ${index + 1} 张：${image.alt}`"
              @click="openCollectionImage(index)"
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
          <a v-for="item in relatedTopics" :key="item.slug" :href="`/collections/${item.slug}`"
            >{{ item.title }} <span aria-hidden="true">→</span></a
          >
          <a class="underlined-link" :href="`/discover?category=${collection.category}`"
            >更多{{ collectionCategoryLabel }}作品</a
          >
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
            <p id="collection-viewer-title">
              {{ collection.title }} · {{ activeImageIndex + 1 }} / {{ collection.images.length }}
            </p>
            <button type="button" aria-label="关闭图像查看器" @click="closeCollectionViewer">
              ×
            </button>
          </header>
          <img
            v-if="activeCollectionImage"
            :src="activeCollectionImage.src"
            :alt="activeCollectionImage.alt"
          />
          <footer>
            <button
              type="button"
              aria-label="上一张图像"
              @click="selectCollectionImage(activeImageIndex - 1)"
            >
              ←
            </button>
            <span>{{ activeCollectionImage?.alt }}</span>
            <button
              type="button"
              aria-label="下一张图像"
              @click="selectCollectionImage(activeImageIndex + 1)"
            >
              →
            </button>
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
              <strong
                >{{ activeProductPreview.title }}<br /><i>{{
                  activeProductPreview.subtitle
                }}</i></strong
              >
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
                >
                  ★
                </button>
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
                <span
                  ><strong>{{ option.name }}</strong
                  ><small>{{ packageOptionDescription(option.name) }}</small></span
                ><b>{{ yuan(option.priceCents) }}</b>
              </button>
            </div>
            <section
              v-if="displayProduct.packages.length"
              class="package-delivery"
              aria-live="polite"
            >
              <h3>{{ packageOption.name }} 包含</h3>
              <ul>
                <li v-for="item in selectedPackagePresentation.items" :key="item">{{ item }}</li>
              </ul>
              <p>{{ selectedPackagePresentation.note }}</p>
            </section>
            <p class="product-terms">
              购买后获得所选套餐的版本、文件清单与下载权限；每次下载都会重新核对当前权益
            </p>
            <a
              v-if="displayProduct.packages.length"
              class="button button-primary product-action"
              :href="`/checkout?product=${displayProduct.id}&package=${selectedPackage}`"
              >购买 {{ packageOption.name }} · {{ yuan(packageOption.priceCents) }}</a
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
          <p>
            {{
              product
                ? '确认作品、套餐和金额，然后创建一笔可追踪的订单'
                : '交易服务暂时不可用，当前不会创建订单'
            }}
          </p>
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
                <li>购买成功后将获得 {{ packageOption.name }} 套餐的访问权益</li>
                <li>权益来源与这笔订单绑定，可在内容库和订单详情中核对</li>
                <li>当前为本地测试支付，不会产生真实扣款</li>
              </ul>
            </section>
          </div>
          <aside class="checkout-summary">
            <span>本次金额</span><strong>{{ yuan(packageOption.priceCents) }}</strong>
            <p v-if="checkoutRequiresSignIn" class="checkout-login-hint">
              登录后才能保存购买记录与访问授权。
            </p>
            <p v-else-if="demoAccountsVisible" class="checkout-login-hint">
              本地学习环境 · 不会产生真实扣款
            </p>
            <p v-if="orderError" class="form-error" role="alert">{{ orderError }}</p>
            <button
              class="button button-primary"
              type="button"
              :disabled="orderLoading || !product"
              @click="completeDemoPurchase"
            >
              {{
                checkoutRequiresSignIn
                  ? '登录后继续'
                  : orderLoading
                    ? '正在确认购买…'
                    : `确认购买 · ${yuan(packageOption.priceCents)}`
              }}
            </button>
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
        <h1>
          {{
            resultLoading
              ? '正在确认订单'
              : resultHasActiveEntitlement
                ? '购买完成'
                : resultOrder?.status === 'PAID'
                  ? '订单已支付'
                  : '订单已创建'
          }}
        </h1>
        <p v-if="resultOrder">
          订单 #{{ resultOrder.id }} · {{ resultOrder.productTitle }} ·
          {{ resultOrder.packageName }}
        </p>
        <p v-else-if="resultError">没有找到可展示的真实订单状态。</p>
        <div class="result-paper">
          <span
            class="result-symbol"
            :class="
              resultError || resultHasUnavailableEntitlement
                ? 'late'
                : resultHasActiveEntitlement
                  ? 'ready'
                  : 'waiting'
            "
            aria-hidden="true"
            >{{
              resultLoading
                ? '…'
                : resultError || resultHasUnavailableEntitlement
                  ? '!'
                  : resultHasActiveEntitlement
                    ? '✓'
                    : '·'
            }}</span
          >
          <h2 v-if="resultLoading">正在从后端读取订单</h2>
          <h2 v-else-if="resultError">暂时无法读取订单</h2>
          <h2 v-else-if="resultHasActiveEntitlement">访问权益已经生效</h2>
          <h2 v-else-if="resultOrder?.entitlementStatus === 'REVOKED'">访问权益已经撤销</h2>
          <h2 v-else-if="resultHasUnavailableEntitlement">订单已支付，权益正在恢复</h2>
          <h2 v-else>订单正在等待支付</h2>
          <p v-if="resultLoading">请稍候，这里只会显示后端确认过的真实状态。</p>
          <p v-else-if="resultError">{{ resultError }}</p>
          <p v-else-if="resultHasActiveEntitlement">
            作品已经加入你的内容库。每个订单项只会生成一份权益，重复支付通知不会重复发放。
          </p>
          <p v-else-if="resultOrder?.entitlementStatus === 'REVOKED'">
            这笔订单的来源权益当前不可用。内容库只展示有效权益，请从订单详情核对状态。
          </p>
          <p v-else-if="resultHasUnavailableEntitlement">
            系统没有确认到有效权益，因此不会把作品显示为可访问。请重新读取状态；仍未恢复时保留订单号排查。
          </p>
          <p v-else>
            订单已经安全保存。当前学习环境未接入真实收款渠道，可用下方测试按钮完成支付闭环。
          </p>
          <p v-if="paymentError" class="form-error" role="alert">{{ paymentError }}</p>
          <p v-if="paymentNotice" class="inline-note" role="status">{{ paymentNotice }}</p>
          <div class="result-actions">
            <button
              v-if="demoAccountsVisible && resultOrder?.status === 'PENDING_PAYMENT'"
              class="button button-primary"
              type="button"
              :disabled="paymentLoading"
              @click="completeLocalTestPayment"
            >
              {{ paymentLoading ? '正在确认测试支付…' : '完成本地测试购买' }}
            </button>
            <button
              v-if="resultError"
              class="button button-secondary"
              type="button"
              :disabled="resultLoading"
              @click="loadResultOrder"
            >
              重新读取订单
            </button>
            <button
              v-if="resultHasUnavailableEntitlement"
              class="button button-secondary"
              type="button"
              :disabled="resultLoading"
              @click="loadResultOrder"
            >
              重新核对权益
            </button>
            <a v-if="resultHasActiveEntitlement" class="button button-primary" href="/library"
              >查看已购内容</a
            >
            <a v-if="resultOrder" class="underlined-link" :href="`/orders/${resultOrder.id}`"
              >查看订单详情</a
            >
            <a v-else-if="!resultLoading" class="underlined-link" href="/discover">返回发现页</a>
          </div>
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
              v-model="loginEmail"
              autocomplete="email"
              placeholder="name@example.com"
            /><label for="login-password">密码</label
            ><input
              id="login-password"
              type="password"
              required
              v-model="loginPassword"
              autocomplete="current-password"
              placeholder="输入密码"
            />
            <div
              v-if="demoAccountsVisible"
              class="state-switch login-role-switch"
              role="group"
              aria-label="选择演示身份"
            >
              <button
                type="button"
                :aria-pressed="loginRole === 'user'"
                @click="selectLoginRole('user')"
              >
                用户
              </button>
              <button
                type="button"
                :aria-pressed="loginRole === 'creator'"
                @click="selectLoginRole('creator')"
              >
                店家
              </button>
              <button
                type="button"
                :aria-pressed="loginRole === 'admin'"
                @click="selectLoginRole('admin')"
              >
                管理员
              </button>
            </div>
            <button class="button button-primary" type="submit" :disabled="loginLoading">
              {{ loginLoading ? '正在登录…' : '登录' }}
            </button>
          </form>
          <p v-if="demoAccountsVisible" class="login-intent">
            当前演示身份：<strong>{{
              loginRole === 'creator'
                ? '创作者 / 店家'
                : loginRole === 'admin'
                  ? '管理员'
                  : '普通用户'
            }}</strong>
            · 本地演示账号密码均为 password
          </p>
          <p v-if="signInNotice" class="inline-note" role="status">
            {{ signInNotice }}
          </p>
          <p v-if="loginError" class="inline-note login-error" role="alert">
            {{ loginError }}
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
