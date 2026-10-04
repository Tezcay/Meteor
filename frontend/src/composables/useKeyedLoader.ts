import { onBeforeUnmount, ref, shallowRef, toValue, watch } from 'vue'
import type { MaybeRefOrGetter } from 'vue'

export type LoadStatus = 'idle' | 'loading' | 'success' | 'error'

export function useKeyedLoader<TKey, TValue>(
  key: MaybeRefOrGetter<TKey>,
  loader: (key: TKey, signal: AbortSignal) => Promise<TValue>,
  fallbackMessage: string,
) {
  const data = shallowRef<TValue | null>(null)
  const status = ref<LoadStatus>('idle')
  const error = ref('')
  let controller: AbortController | null = null
  let sequence = 0

  async function load() {
    controller?.abort()
    const currentController = new AbortController()
    const currentSequence = ++sequence
    controller = currentController
    status.value = 'loading'
    error.value = ''

    try {
      const result = await loader(toValue(key), currentController.signal)
      if (currentController.signal.aborted || currentSequence !== sequence) return
      data.value = result
      status.value = 'success'
    } catch (reason) {
      if (currentController.signal.aborted || currentSequence !== sequence) return
      error.value = reason instanceof Error ? reason.message : fallbackMessage
      status.value = 'error'
    } finally {
      if (controller === currentController) controller = null
    }
  }

  function cancel() {
    sequence += 1
    controller?.abort()
    controller = null
  }

  watch(
    () => toValue(key),
    () => {
      data.value = null
      void load()
    },
    { immediate: true },
  )

  onBeforeUnmount(cancel)

  return { data, status, error, reload: load }
}
