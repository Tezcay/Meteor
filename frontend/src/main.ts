import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles.css'
import './page-styles.css'

createApp(App).use(router).mount('#app')

if (import.meta.env.DEV && new URLSearchParams(window.location.search).has('a11y')) {
  void router.isReady().then(async () => {
    const { runDevA11yAudit } = await import('./utils/devA11y')
    await runDevA11yAudit()
  })
}
