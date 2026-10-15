import { createRouter, createWebHistory } from 'vue-router'
import AdminPages from './pages/AdminPages.vue'
import AccountPages from './pages/AccountPages.vue'
import BuyerPages from './pages/BuyerPages.vue'
import CreatorPages from './pages/CreatorPages.vue'
import CreatorProductsPage from './pages/CreatorProductsPage.vue'
import HomePage from './pages/HomePage.vue'
import { getCurrentUser, roleFromCurrentUser } from './api/auth'
import { getLocalRole } from './data/localStore'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, behavior: 'smooth' }
    if (to.path !== from.path) return { top: 0 }
    return false
  },
  routes: [
    { path: '/', component: HomePage },
    { path: '/library', component: AccountPages, props: (route) => ({ path: route.path }) },
    { path: '/orders', component: AccountPages, props: (route) => ({ path: route.path }) },
    {
      path: '/orders/:orderId(\\d+)',
      component: AccountPages,
      props: (route) => ({ path: route.path }),
    },
    {
      path: '/admin/:pathMatch(.*)*',
      component: AdminPages,
      props: (route) => ({ path: route.path }),
    },
    { path: '/creator/works', component: CreatorProductsPage },
    { path: '/creator/releases/new', component: CreatorProductsPage },
    {
      path: '/creator/:pathMatch(.*)*',
      component: CreatorPages,
      props: (route) => ({ path: route.path }),
    },
    { path: '/:pathMatch(.*)*', component: BuyerPages, props: (route) => ({ path: route.path }) },
  ],
})

router.beforeEach(async (to) => {
  const requiresCreator = to.path.startsWith('/creator')
  const requiresAdmin = to.path.startsWith('/admin')
  const requiresAccount = to.path === '/library' || to.path.startsWith('/orders')
  if (!requiresCreator && !requiresAdmin && !requiresAccount) return true

  let role = getLocalRole()
  try {
    const user = await getCurrentUser()
    role = user ? roleFromCurrentUser(user) : 'visitor'
  } catch {
    // Local demo pages may stay browsable during Vite development; production fails closed.
    if (!import.meta.env.DEV) role = 'visitor'
  }
  const allowed =
    (!requiresCreator && !requiresAdmin && (!requiresAccount || role !== 'visitor')) ||
    (requiresCreator && role === 'creator') ||
    (requiresAdmin && role === 'admin')
  if (allowed) return true
  return { path: '/login', query: { next: to.fullPath } }
})

const pageTitles: Record<string, string> = {
  '/': 'Meteor · 数字作品',
  '/discover': '发现作品 · Meteor',
  '/shops/northstar': 'Northstar Design · Meteor',
  '/checkout': '确认购买 · Meteor',
  '/checkout/result': '购买状态 · Meteor',
  '/library': '内容库 · Meteor',
  '/orders': '订单记录 · Meteor',
  '/login': '登录 · Meteor',
  '/creator/start': '成为创作者 · Meteor',
  '/creator/works': '我的作品 · Meteor',
  '/creator/releases/new': '新建发布 · Meteor',
  '/creator/review': '审核状态 · Meteor',
  '/creator/orders': '小店订单 · Meteor',
  '/admin/reviews': '作品审核 · Meteor',
  '/admin/refunds': '退款处理 · Meteor',
  '/admin/exceptions': '异常订单 · Meteor',
  '/admin/audit': '操作记录 · Meteor',
}

router.afterEach((to) => {
  document.title =
    pageTitles[to.path] ??
    (/^\/orders\/[1-9]\d*$/.test(to.path)
      ? '订单详情 · Meteor'
      : /^\/(products\/[1-9]\d*|collections\/[^/]+)$/.test(to.path)
        ? '作品详情 · Meteor'
        : '页面未找到 · Meteor')
})

export default router
