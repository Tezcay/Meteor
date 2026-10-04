<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  createDownloadAuthorization,
  getAuthorizedDownload,
  getReleaseManifest,
} from '../../api/library'
import type { ManifestItemResponse } from '../../api/library'
import { useKeyedLoader } from '../../composables/useKeyedLoader'
import { formatFileSize } from '../../utils/formatters'

const props = defineProps<{
  grantId: number
  releaseId: number
  panelId: string
}>()

const downloadingFileId = ref<number | null>(null)
const downloadError = ref('')
const downloadNotice = ref('')

const requestKey = computed(() => `${props.grantId}:${props.releaseId}`)
const {
  data: manifest,
  status,
  error,
  reload,
} = useKeyedLoader(
  requestKey,
  (_key, signal) => getReleaseManifest(props.grantId, props.releaseId, signal),
  '文件清单读取失败，请稍后重试。',
)

function manifestRoleLabel(role: ManifestItemResponse['role']) {
  return {
    PRIMARY: '主要文件',
    GUIDE: '使用指南',
    BONUS: '附加内容',
  }[role]
}

async function downloadFile(file: ManifestItemResponse) {
  if (downloadingFileId.value !== null) return

  downloadingFileId.value = file.id
  downloadError.value = ''
  downloadNotice.value = ''
  try {
    const authorization = await createDownloadAuthorization(props.grantId, props.releaseId, file.id)
    const blob = await getAuthorizedDownload(authorization.downloadUrl)
    const objectUrl = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = objectUrl
    anchor.download = authorization.filename
    document.body.append(anchor)
    anchor.click()
    anchor.remove()
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 0)
    downloadNotice.value = `已开始下载“${file.displayName}”。本次下载凭证已经使用。`
  } catch (reason) {
    downloadError.value =
      reason instanceof Error ? reason.message : '下载失败，请重新获取下载凭证后再试。'
  } finally {
    downloadingFileId.value = null
  }
}
</script>

<template>
  <section :id="panelId" class="manifest-disclosure" aria-label="版本文件清单">
    <p v-if="status === 'loading'" role="status">正在核对冻结文件清单…</p>
    <div v-else-if="status === 'error'" class="release-disclosure-error" role="alert">
      <p>{{ error }}</p>
      <button type="button" @click="reload">重新读取文件清单</button>
    </div>
    <template v-else-if="manifest">
      <header class="manifest-summary">
        <div>
          <strong>{{ manifest.itemCount }} 个文件</strong>
          <small>共 {{ formatFileSize(manifest.totalBytes) }}</small>
        </div>
        <span>清单已冻结</span>
      </header>
      <p v-if="!manifest.items.length" class="manifest-empty">这个正式版本暂时没有可交付文件。</p>
      <div class="manifest-download-feedback">
        <p v-if="downloadError" class="manifest-download-error" role="alert">
          {{ downloadError }}
        </p>
        <p v-else-if="downloadNotice" class="manifest-download-notice" role="status">
          {{ downloadNotice }}
        </p>
      </div>
      <ul v-if="manifest.items.length" class="manifest-list">
        <li v-for="file in manifest.items" :key="file.id">
          <span class="manifest-position">{{ String(file.position).padStart(2, '0') }}</span>
          <div>
            <div class="manifest-file-heading">
              <strong>{{ file.displayName }}</strong>
              <button
                type="button"
                class="manifest-download-button"
                :disabled="downloadingFileId !== null"
                :aria-label="`下载${file.displayName}`"
                @click="downloadFile(file)"
              >
                {{ downloadingFileId === file.id ? '正在核验…' : '下载文件' }}
              </button>
            </div>
            <p>
              {{ file.filename }} · {{ file.mediaType }} · {{ formatFileSize(file.sizeBytes) }} ·
              {{ manifestRoleLabel(file.role) }}
            </p>
            <code :title="file.sha256">SHA-256 {{ file.sha256.slice(0, 16) }}…</code>
          </div>
        </li>
      </ul>
      <p class="manifest-boundary">
        每次下载使用五分钟一次性凭证；服务端会重新核对当前权益并校验文件大小与 SHA-256。
      </p>
    </template>
  </section>
</template>
