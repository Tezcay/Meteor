<script setup lang="ts">
import { ref } from 'vue'
import { getAccessibleReleases } from '../../api/library'
import { useKeyedLoader } from '../../composables/useKeyedLoader'
import { formatDateTime } from '../../utils/formatters'
import DeliveryManifestPanel from './DeliveryManifestPanel.vue'

const props = defineProps<{
  grantId: number
  regionId: string
}>()

const expandedReleaseId = ref<number | null>(null)
const openedReleaseIds = ref(new Set<number>())
const {
  data: releases,
  status,
  error,
  reload,
} = useKeyedLoader(
  () => props.grantId,
  (grantId, signal) => getAccessibleReleases(grantId, signal),
  '版本记录读取失败，请稍后重试。',
)

function manifestPanelId(releaseId: number) {
  return `manifest-${props.grantId}-${releaseId}`
}

function toggleManifest(releaseId: number) {
  if (expandedReleaseId.value === releaseId) {
    expandedReleaseId.value = null
    return
  }

  expandedReleaseId.value = releaseId
  openedReleaseIds.value = new Set(openedReleaseIds.value).add(releaseId)
}
</script>

<template>
  <section :id="regionId" class="release-disclosure" aria-label="可用发布版本">
    <p v-if="status === 'loading'" role="status">正在核对可用版本…</p>
    <div v-else-if="status === 'error'" class="release-disclosure-error" role="alert">
      <p>{{ error }}</p>
      <button type="button" @click="reload">重新读取版本记录</button>
    </div>
    <p v-else-if="!releases?.length">当前套餐还没有可用的正式版本。</p>
    <ol v-else class="release-list">
      <li v-for="release in releases" :key="release.id">
        <div class="release-title-row">
          <div>
            <strong>v{{ release.versionLabel }}</strong>
            <span v-if="release.latest">最新版本</span>
          </div>
          <button
            class="manifest-toggle"
            type="button"
            :aria-expanded="expandedReleaseId === release.id"
            :aria-controls="manifestPanelId(release.id)"
            @click="toggleManifest(release.id)"
          >
            {{ expandedReleaseId === release.id ? '收起文件' : '查看文件清单' }}
          </button>
        </div>
        <p>{{ release.summary }}</p>
        <small>发布于 {{ formatDateTime(release.publishedAt) }}</small>
        <DeliveryManifestPanel
          v-if="openedReleaseIds.has(release.id)"
          v-show="expandedReleaseId === release.id"
          :grant-id="grantId"
          :release-id="release.id"
          :panel-id="manifestPanelId(release.id)"
        />
      </li>
    </ol>
  </section>
</template>
