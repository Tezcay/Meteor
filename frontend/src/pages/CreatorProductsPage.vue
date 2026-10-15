<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import InnerHeader from '../components/InnerHeader.vue'
import {
  createProductDraft,
  getCreatorProducts,
  getDeliveryAssets,
  publishProduct,
  type CreatorProduct,
  type DeliveryAsset,
} from '../api/creator'

const products = ref<CreatorProduct[]>([])
const assets = ref<DeliveryAsset[]>([])
const loading = ref(true)
const busy = ref(false)
const loadError = ref('')
const operationError = ref('')
const notice = ref('')
const title = ref('')
const summary = ref('')
const selectedProductId = ref<number | null>(null)
const selectedFileId = ref<number | null>(null)
const packageName = ref('Basic')
const price = ref('19.00')
const publishedProductId = ref<number | null>(null)
const productSelect = ref<HTMLSelectElement | null>(null)
const publishedLink = ref<HTMLAnchorElement | null>(null)
const drafts = computed(() =>
  products.value.filter((product) => product.revisionStatus === 'DRAFT'),
)
const selectedDraft = computed(() =>
  drafts.value.find((product) => product.productId === selectedProductId.value),
)
const disabled = computed(() => loading.value || busy.value || Boolean(loadError.value))

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const [ownProducts, ownAssets] = await Promise.all([getCreatorProducts(), getDeliveryAssets()])
    products.value = ownProducts
    assets.value = ownAssets
    if (!drafts.value.some((product) => product.productId === selectedProductId.value)) {
      selectedProductId.value = drafts.value[0]?.productId ?? null
    }
    if (!assets.value.some((asset) => asset.id === selectedFileId.value)) {
      selectedFileId.value = assets.value[0]?.id ?? null
    }
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '读取失败，请重试。'
  } finally {
    loading.value = false
  }
}

async function createDraft() {
  busy.value = true
  operationError.value = ''
  notice.value = ''
  try {
    const draft = await createProductDraft(title.value, summary.value)
    selectedProductId.value = draft.productId
    title.value = ''
    summary.value = ''
    await load()
    notice.value = '草稿已保存。配置一个套餐和交付素材后即可发布。'
    busy.value = false
    await nextTick()
    productSelect.value?.focus()
  } catch (error) {
    operationError.value =
      error instanceof Error ? error.message : '保存未完成，请刷新作品列表后核对。'
  } finally {
    busy.value = false
  }
}

function priceInCents(value: string) {
  // 按整数拆分小数，避免浮点乘法把 19.99 元变成非整数分。
  if (!/^\d{1,7}(\.\d{1,2})?$/.test(value.trim())) throw new Error('请输入最多两位小数的售价。')
  const [whole, decimal = ''] = value.trim().split('.')
  const cents = Number(whole) * 100 + Number(decimal.padEnd(2, '0'))
  if (cents < 1 || cents > 100_000_000) throw new Error('售价范围为 0.01～1,000,000 元。')
  return cents
}

async function publish() {
  operationError.value = ''
  notice.value = ''
  if (selectedProductId.value === null || selectedFileId.value === null) {
    operationError.value = '请选择草稿与可用素材。'
    return
  }
  busy.value = true
  try {
    const result = await publishProduct(
      selectedProductId.value,
      packageName.value,
      priceInCents(price.value),
      selectedFileId.value,
    )
    publishedProductId.value = result.productId
    await load()
    notice.value = '作品已发布，买家现在可以购买并下载。'
    busy.value = false
    await nextTick()
    publishedLink.value?.focus()
  } catch (error) {
    operationError.value =
      error instanceof Error ? error.message : '发布未完成，请刷新列表核对后重试。'
  } finally {
    busy.value = false
  }
}
onMounted(load)
</script>

<template>
  <InnerHeader section="creator" />
  <main id="page-main" class="inner-main page-width workspace-layout" tabindex="-1">
    <aside class="workspace-sidebar">
      <nav aria-label="创作者导航">
        <a href="/creator/works" aria-current="page">我的作品</a>
        <a href="/discover">查看公开目录</a>
        <a href="/library">我的已购内容</a>
      </nav>
    </aside>
    <div class="workspace-content">
      <div class="workspace-heading">
        <h1>我的作品</h1>
        <p>先保存草稿，再配置一个套餐与交付素材。发布后可直接购买下载。</p>
      </div>
      <p v-if="busy && !loading" role="status">正在处理，请稍候…</p>
      <p v-if="loading" role="status">正在读取作品与素材…</p>
      <div v-if="loadError" role="alert">
        <p>读取失败：{{ loadError }}</p>
        <button class="button" type="button" :disabled="loading" @click="load">重新读取</button>
      </div>
      <p v-if="operationError" role="alert">操作未完成：{{ operationError }}</p>
      <p v-if="notice" role="status">{{ notice }}</p>
      <a
        v-if="publishedProductId"
        ref="publishedLink"
        class="published-link"
        :href="`/products/${publishedProductId}`"
      >
        查看刚发布的作品
      </a>

      <div class="publication-columns">
        <section aria-labelledby="draft-heading">
          <h2 id="draft-heading">创建草稿</h2>
          <form class="editor-form" @submit.prevent="createDraft">
            <label for="product-title">作品标题</label>
            <input
              id="product-title"
              v-model="title"
              maxlength="160"
              required
              :disabled="disabled"
            />
            <label for="product-summary">作品简介</label>
            <textarea
              id="product-summary"
              v-model="summary"
              maxlength="500"
              required
              :disabled="disabled"
            />
            <button class="button" type="submit" :disabled="disabled">保存草稿</button>
          </form>
        </section>
        <section aria-labelledby="publish-heading">
          <h2 id="publish-heading">配置并发布</h2>
          <p class="field-note">使用自己店铺正式清单中的已有素材；当前不提供文件上传。</p>
          <form class="editor-form" @submit.prevent="publish">
            <label for="publish-draft">待发布草稿</label>
            <select
              id="publish-draft"
              ref="productSelect"
              v-model="selectedProductId"
              required
              :disabled="disabled || !drafts.length"
            >
              <option :value="null" disabled>请先保存草稿</option>
              <option v-for="draft in drafts" :key="draft.productId" :value="draft.productId">
                {{ draft.title }}
              </option>
            </select>
            <p v-if="selectedDraft" class="field-note">{{ selectedDraft.summary }}</p>
            <label for="package-name">套餐名称</label>
            <input
              id="package-name"
              v-model="packageName"
              maxlength="80"
              required
              :disabled="disabled"
            />
            <label for="package-price">售价（元）</label>
            <input
              id="package-price"
              v-model="price"
              inputmode="decimal"
              required
              :disabled="disabled"
              aria-describedby="price-note"
            />
            <p id="price-note" class="field-note">人民币，最多两位小数；0.01～1,000,000 元。</p>
            <label for="delivery-asset">交付素材</label>
            <select
              id="delivery-asset"
              v-model="selectedFileId"
              required
              :disabled="disabled || !assets.length"
            >
              <option :value="null" disabled>请选择素材</option>
              <option v-for="asset in assets" :key="asset.id" :value="asset.id">
                {{ asset.filename }} · {{ asset.sizeBytes }} B
              </option>
            </select>
            <p v-if="!loading && !assets.length" class="field-note">
              店铺暂无可复用素材，当前无法发布。请使用已有店铺演示账号。
            </p>
            <button
              class="button"
              type="submit"
              :disabled="disabled || !drafts.length || !assets.length"
            >
              发布作品
            </button>
          </form>
        </section>
      </div>
      <section class="owned-products" aria-labelledby="products-heading">
        <div class="products-heading">
          <h2 id="products-heading">已保存作品</h2>
          <button class="button secondary" type="button" :disabled="loading || busy" @click="load">
            刷新列表
          </button>
        </div>
        <p v-if="!loading && !loadError && !products.length">还没有作品，先创建一份草稿。</p>
        <ul class="product-list">
          <li v-for="product in products" :key="product.productId">
            <div>
              <h3>{{ product.title }}</h3>
              <p>{{ product.summary }}</p>
            </div>
            <span>{{
              product.revisionStatus === 'PUBLISHED' && product.saleStatus === 'ON_SALE'
                ? '已发布'
                : '草稿'
            }}</span>
            <a
              v-if="product.revisionStatus === 'PUBLISHED' && product.saleStatus === 'ON_SALE'"
              :href="`/products/${product.productId}`"
              >查看作品</a
            >
          </li>
        </ul>
      </section>
    </div>
  </main>
</template>

<style scoped>
.publication-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 40px;
  margin-top: 32px;
}
.publication-columns section {
  min-width: 0;
}
h2 {
  font-size: 21px;
  margin: 0 0 18px;
}
.field-note {
  color: var(--muted);
  font-size: 14px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}
.published-link {
  display: inline-flex;
  align-items: center;
  min-height: 44px;
  color: var(--accent);
  text-decoration: underline;
  text-underline-offset: 4px;
}
.owned-products {
  margin-top: 48px;
}
.products-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.product-list {
  padding: 0;
  list-style: none;
}
.product-list li {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  padding: 20px 0;
  border-top: 1px solid var(--line);
}
.product-list li div {
  flex: 1 1 200px;
  min-width: 0;
}
.product-list h3 {
  font-size: 17px;
  margin: 0 0 8px;
  overflow-wrap: anywhere;
}
.product-list p {
  margin: 0;
  color: var(--muted);
  line-height: 1.6;
  overflow-wrap: anywhere;
}
.product-list a {
  display: inline-flex;
  align-items: center;
  min-height: 44px;
  color: var(--accent);
  text-decoration: underline;
  text-underline-offset: 4px;
}
.editor-form input,
.editor-form select,
.editor-form button {
  min-height: 44px;
  max-width: 100%;
}
.editor-form textarea {
  max-width: 100%;
}
@media (max-width: 900px) {
  .publication-columns {
    grid-template-columns: minmax(0, 1fr);
    gap: 32px;
  }
}
</style>
