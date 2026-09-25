<script setup lang="ts">
import { computed, ref } from 'vue'

const emit = defineEmits<{ explore: [] }>()
const dialog = ref<HTMLDialogElement | null>(null)
const feature = ref<'login' | 'creator'>('login')
const title = computed(() =>
  feature.value === 'login' ? '很快，在这里相遇' : '给你的作品一个新去处',
)
const message = computed(() =>
  feature.value === 'login'
    ? '账号登录功能正在准备中，你可以先浏览作品'
    : '创作者入驻和作品发布正在准备中，期待与你相遇',
)

function open(value: 'login' | 'creator') {
  feature.value = value
  if (!dialog.value?.open) dialog.value?.showModal()
}

function closeFromBackdrop(event: MouseEvent) {
  if (event.target === dialog.value) dialog.value?.close()
}

function explore() {
  dialog.value?.close()
  emit('explore')
}

defineExpose({ open })
</script>

<template>
  <dialog
    ref="dialog"
    class="site-dialog feature-dialog"
    aria-labelledby="feature-title"
    aria-describedby="feature-description"
    @click="closeFromBackdrop"
  >
    <div class="dialog-shell">
      <header class="dialog-heading">
        <h2 id="feature-title">{{ title }}</h2>
        <button class="close-button" type="button" aria-label="关闭提示" @click="dialog?.close()">
          ×
        </button>
      </header>
      <p id="feature-description">{{ message }}</p>
      <button class="button button-primary" type="button" @click="explore">先逛逛作品</button>
    </div>
  </dialog>
</template>
