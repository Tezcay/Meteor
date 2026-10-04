<script setup lang="ts">
import { ref } from 'vue'
import type { LibraryItemResponse } from '../../api/library'
import { formatDateTime } from '../../utils/formatters'
import ReleaseExplorer from './ReleaseExplorer.vue'

const props = defineProps<{ item: LibraryItemResponse }>()

const expanded = ref(false)
const hasOpened = ref(false)
const releaseRegionId = `release-list-${props.item.grantId}`

function toggleReleases() {
  expanded.value = !expanded.value
  if (expanded.value) hasOpened.value = true
}
</script>

<template>
  <article class="library-preview">
    <div class="library-item-art">
      <span>{{ item.shopName }}</span
      ><strong>{{ item.productTitle }}</strong>
    </div>
    <div class="library-item-copy">
      <span class="entitlement-badge">访问权益已生效</span>
      <h2>{{ item.productTitle }}</h2>
      <p>{{ item.packageName }} 套餐 · {{ formatDateTime(item.grantedAt) }}</p>
      <div class="library-status">
        <span>包含对应套餐的全部发布版本</span>
        <small>权益来源：订单 #{{ item.orderId }}</small>
      </div>
      <p class="muted">版本、冻结文件清单和一次性下载凭证已经接入；展开版本即可下载。</p>
      <button
        class="button button-secondary"
        type="button"
        :aria-expanded="expanded"
        :aria-controls="releaseRegionId"
        @click="toggleReleases"
      >
        {{ expanded ? '收起版本记录' : '查看可用版本' }}
      </button>
      <ReleaseExplorer
        v-if="hasOpened"
        v-show="expanded"
        :grant-id="item.grantId"
        :region-id="releaseRegionId"
      />
      <a class="underlined-link" :href="`/orders/${item.orderId}`">查看购买详情</a>
    </div>
  </article>
</template>
