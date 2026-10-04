<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import AccountSidebar from '../components/AccountSidebar.vue'
import InnerHeader from '../components/InnerHeader.vue'
import LibraryItemCard from '../components/library/LibraryItemCard.vue'
import { getLibrary } from '../api/library'
import type { LibraryItemResponse } from '../api/library'
import { getOrder, getOrders } from '../api/orders'
import type { OrderDetailResponse } from '../api/orders'
import { formatCurrency, formatDateTime } from '../utils/formatters'

const props = defineProps<{ path: string }>()
const loading = ref(false)
const error = ref('')
const libraryItems = ref<LibraryItemResponse[]>([])
const orders = ref<OrderDetailResponse[]>([])
const order = ref<OrderDetailResponse | null>(null)
let request: AbortController | null = null

const isLibrary = computed(() => props.path === '/library')
const orderId = computed(() => {
  const match = props.path.match(/^\/orders\/([1-9]\d*)$/)
  return match ? Number(match[1]) : null
})

function orderStatusLabel(status: OrderDetailResponse['status']) {
  return {
    PENDING_PAYMENT: '等待支付',
    PAID: '已支付',
    CLOSED: '已关闭',
    REFUNDED: '已退款',
  }[status]
}

function entitlementStatusLabel(status: OrderDetailResponse['entitlementStatus']) {
  if (status === 'ACTIVE') return '访问权益已生效'
  if (status === 'REVOKED') return '访问权益已撤销'
  return '尚未产生访问权益'
}

async function load() {
  request?.abort()
  const currentRequest = new AbortController()
  request = currentRequest
  loading.value = true
  error.value = ''
  try {
    if (isLibrary.value) {
      libraryItems.value = await getLibrary(currentRequest.signal)
      return
    }
    if (orderId.value) {
      order.value = await getOrder(orderId.value, currentRequest.signal)
      return
    }
    orders.value = await getOrders(currentRequest.signal)
  } catch (reason) {
    if (!currentRequest.signal.aborted) {
      error.value = reason instanceof Error ? reason.message : '读取账户信息失败，请稍后重试。'
    }
  } finally {
    if (!currentRequest.signal.aborted) loading.value = false
  }
}

watch(
  () => props.path,
  () => void load(),
)
onMounted(() => void load())
onBeforeUnmount(() => {
  request?.abort()
})
</script>

<template>
  <InnerHeader section="account" />
  <main id="page-main" class="inner-main" tabindex="-1">
    <div class="page-width workspace-layout account-layout">
      <AccountSidebar :active="isLibrary ? 'library' : 'orders'" />
      <div class="workspace-content" :class="isLibrary ? 'library-page' : 'order-page'">
        <div class="workspace-heading">
          <h1>{{ isLibrary ? '内容库' : orderId ? '订单详情' : '订单记录' }}</h1>
          <p>
            {{
              isLibrary
                ? '你当前拥有访问资格的数字作品'
                : orderId
                  ? '购买快照与权益状态'
                  : '按时间查看购买状态'
            }}
          </p>
        </div>

        <div v-if="loading" class="account-state" role="status">
          <span class="account-state-mark" aria-hidden="true"></span>
          <h2>正在读取{{ isLibrary ? '内容库' : '订单' }}</h2>
          <p>正在从后端核对你的账号与访问资格。</p>
        </div>

        <div v-else-if="error" class="account-state is-error" role="alert">
          <h2>暂时无法读取</h2>
          <p>{{ error }}</p>
          <button class="button button-secondary" type="button" @click="load">重新读取</button>
        </div>

        <template v-else-if="isLibrary">
          <div v-if="!libraryItems.length" class="empty-state library-empty">
            <div class="empty-mark" aria-hidden="true"></div>
            <h2>内容库还是空的</h2>
            <p>完成一次购买后，生效的访问权益会显示在这里。</p>
            <a class="button button-primary" href="/discover">发现数字作品</a>
          </div>
          <div v-else class="library-list">
            <LibraryItemCard v-for="item in libraryItems" :key="item.grantId" :item="item" />
          </div>
        </template>

        <template v-else-if="orderId">
          <div v-if="!order" class="account-state is-error">
            <h2>没有找到这笔订单</h2>
            <a class="button button-secondary" href="/orders">返回订单记录</a>
          </div>
          <article v-else class="order-detail-sheet">
            <header>
              <div>
                <span>订单 #{{ order.id }}</span>
                <h2>{{ order.productTitle }}</h2>
                <p>{{ order.shopName }} · {{ order.packageName }} 套餐</p>
              </div>
              <strong class="order-status" :class="`is-${order.status.toLowerCase()}`">{{
                orderStatusLabel(order.status)
              }}</strong>
            </header>
            <dl class="order-detail-grid">
              <div>
                <dt>购买时间</dt>
                <dd>{{ formatDateTime(order.createdAt) }}</dd>
              </div>
              <div>
                <dt>实付金额</dt>
                <dd>{{ formatCurrency(order.totalAmountCents) }}</dd>
              </div>
              <div>
                <dt>访问策略</dt>
                <dd>对应套餐的全部发布版本</dd>
              </div>
              <div>
                <dt>权益状态</dt>
                <dd>
                  {{
                    order.entitlementStatus === 'ACTIVE'
                      ? '已生效'
                      : order.entitlementStatus === 'REVOKED'
                        ? '已撤销'
                        : '尚未发放'
                  }}
                </dd>
              </div>
            </dl>
            <div class="order-next-step">
              <template v-if="order.entitlementStatus === 'ACTIVE'">
                <div>
                  <strong>这笔购买已经产生访问权益</strong>
                  <p>你可以在内容库查看它；文件交付将在下一阶段开放。</p>
                </div>
                <a class="button button-primary" href="/library">前往内容库</a>
              </template>
              <template v-else>
                <div>
                  <strong>这笔订单尚未产生访问权益</strong>
                  <p>只有支付成功的订单才会进入内容库。</p>
                </div>
                <a class="button button-secondary" href="/discover">继续浏览</a>
              </template>
            </div>
          </article>
        </template>

        <template v-else>
          <div v-if="!orders.length" class="empty-state">
            <h2>还没有订单</h2>
            <p>你创建的购买订单会按时间显示在这里。</p>
            <a class="button button-secondary" href="/discover">继续探索</a>
          </div>
          <section v-else class="order-list" aria-label="购买订单列表">
            <a
              v-for="item in orders"
              :key="item.id"
              class="order-sheet order-sheet-link"
              :href="`/orders/${item.id}`"
            >
              <div>
                <span>订单编号</span><strong>#{{ item.id }}</strong>
              </div>
              <div>
                <span>作品</span><strong>{{ item.productTitle }}</strong>
              </div>
              <div>
                <span>套餐</span><strong>{{ item.packageName }}</strong>
              </div>
              <div>
                <span>金额</span><strong>{{ formatCurrency(item.totalAmountCents) }}</strong>
              </div>
              <div>
                <span>状态</span><strong>{{ orderStatusLabel(item.status) }}</strong>
              </div>
              <p>{{ entitlementStatusLabel(item.entitlementStatus) }} · 查看详情</p>
            </a>
          </section>
        </template>
      </div>
    </div>
  </main>
  <footer class="inner-footer page-width">
    <nav aria-label="页脚导航">
      <a href="/discover">发现</a><a href="/creator/start">成为创作者</a><a href="/">首页</a>
    </nav>
  </footer>
</template>
