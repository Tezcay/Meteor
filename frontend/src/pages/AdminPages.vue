<script setup lang="ts">
import { computed, ref } from 'vue'
import InnerHeader from '../components/InnerHeader.vue'

const props = defineProps<{ path: string }>()
const active = computed(() =>
  props.path.includes('/refunds')
    ? 'refunds'
    : props.path.includes('/exceptions')
      ? 'exceptions'
      : props.path.includes('/audit')
        ? 'audit'
        : 'reviews',
)
const selectedReview = ref('求职材料模板包 · v1.3')
const filter = ref<'all' | 'waiting' | 'done'>('all')
const search = ref('')
const reviews = [
  {
    name: '求职材料模板包 · v1.3',
    shop: 'Northstar Design',
    status: '待审核',
    time: '09.18 10:24',
  },
  { name: '版式练习 · v1.0', shop: '展示小店', status: '待审核', time: '09.17 16:08' },
  { name: '读物封面 · v2.1', shop: '展示小店', status: '已处理', time: '09.16 14:32' },
]
const filteredReviews = computed(() =>
  reviews.filter(
    (row) =>
      (filter.value === 'all' ||
        (filter.value === 'waiting' ? row.status === '待审核' : row.status === '已处理')) &&
      [row.name, row.shop]
        .join(' ')
        .toLocaleLowerCase()
        .includes(search.value.trim().toLocaleLowerCase()),
  ),
)
</script>

<template>
  <InnerHeader />
  <main id="page-main" class="inner-main page-width workspace-layout admin-layout" tabindex="-1">
    <aside class="workspace-sidebar">
      <div class="workspace-sidebar-title"><span>运营工作台</span></div>
      <nav aria-label="管理导航">
        <a href="/admin/reviews" :aria-current="active === 'reviews' ? 'page' : undefined"
          >作品审核</a
        ><a href="/admin/refunds" :aria-current="active === 'refunds' ? 'page' : undefined"
          >退款处理</a
        ><a href="/admin/exceptions" :aria-current="active === 'exceptions' ? 'page' : undefined"
          >异常订单</a
        ><a href="/admin/audit" :aria-current="active === 'audit' ? 'page' : undefined">操作记录</a>
      </nav>
    </aside>
    <div class="workspace-content">
      <div v-if="active === 'reviews'" class="workspace-heading">
        <h1>作品审核</h1>
      </div>
      <div v-else-if="active === 'refunds'" class="workspace-heading">
        <h1>退款处理</h1>
      </div>
      <div v-else-if="active === 'exceptions'" class="workspace-heading">
        <h1>异常订单</h1>
      </div>
      <div v-else class="workspace-heading">
        <h1>操作记录</h1>
      </div>
      <div class="preview-ribbon">
        <span>演示数据 · 管理接口未接入</span><a href="/discover">返回站点 ↗</a>
      </div>

      <template v-if="active === 'reviews'"
        ><div class="admin-toolbar">
          <div class="state-switch" role="group" aria-label="筛选审核事项">
            <button type="button" :aria-pressed="filter === 'all'" @click="filter = 'all'">
              全部</button
            ><button type="button" :aria-pressed="filter === 'waiting'" @click="filter = 'waiting'">
              待审核</button
            ><button type="button" :aria-pressed="filter === 'done'" @click="filter = 'done'">
              已处理
            </button>
          </div>
          <label class="admin-search"
            ><span class="sr-only">搜索作品</span
            ><input v-model="search" type="search" placeholder="搜索作品或小店"
          /></label>
        </div>
        <div class="admin-review-layout">
          <div class="review-list">
            <button
              v-for="row in filteredReviews"
              :key="row.name"
              type="button"
              :aria-pressed="selectedReview === row.name"
              @click="selectedReview = row.name"
            >
              <span
                ><strong>{{ row.name }}</strong
                ><small>{{ row.shop }} · {{ row.time }}</small></span
              ><em>{{ row.status }}</em>
            </button>
            <p v-if="!filteredReviews.length" class="muted">没有符合筛选条件的示例记录</p>
          </div>
          <section class="review-inspector">
            <h2>{{ selectedReview }}</h2>
            <div class="inspection-row"><span>公开预览</span><strong>展示图 3 张</strong></div>
            <div class="inspection-row">
              <span>套餐与版本</span><strong>Basic / Pro · 候选版本</strong>
            </div>
            <div class="inspection-row">
              <span>交付文件</span><strong>需登录后台并通过授权访问</strong>
            </div>
            <div class="inspection-row"><span>授权说明</span><strong>待核对</strong></div>
            <div class="inspection-actions">
              <button class="button button-secondary" type="button" disabled>退回修改</button
              ><button class="button button-primary" type="button" disabled>通过审核</button>
            </div>
          </section>
        </div></template
      >

      <template v-else-if="active === 'refunds'"
        ><div class="admin-case">
          <div class="case-header">
            <div>
              <h2>求职材料模板包 · Pro</h2>
              <p>Northstar Design · 单次购买订单</p>
            </div>
            <strong>待核对</strong>
          </div>
          <div class="case-grid">
            <div><span>支付结果</span><strong>已完成</strong></div>
            <div><span>授权来源</span><strong>订单获取</strong></div>
            <div><span>处理前检查</span><strong>其他来源的授权不受影响</strong></div>
          </div>
          <div class="case-note">
            <h3>处理说明</h3>
            <p>先核对支付与授权记录，退款成功后仅撤销该订单产生的权限</p>
          </div>
          <div class="inspection-actions">
            <button class="button button-secondary" type="button" disabled>驳回申请</button
            ><button class="button button-primary" type="button" disabled>确认退款</button>
          </div>
        </div></template
      >

      <template v-else-if="active === 'exceptions'"
        ><div class="exception-list">
          <article>
            <div class="exception-icon">!</div>
            <div>
              <h2>支付确认延迟</h2>
              <p>核对支付回调，避免重复授权</p>
            </div>
            <button type="button" disabled>等待接口</button>
          </article>
          <article>
            <div class="exception-icon">↺</div>
            <div>
              <h2>退款与权限不一致</h2>
              <p>核对权限来源与撤销结果</p>
            </div>
            <button type="button" disabled>等待接口</button>
          </article>
        </div></template
      >

      <template v-else
        ><div class="table-wrap">
          <table class="calm-table">
            <thead>
              <tr>
                <th>操作时间</th>
                <th>操作类型</th>
                <th>对象</th>
                <th>记录说明</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>09.18 10:24</td>
                <td>提交审核</td>
                <td>求职材料模板包 v1.3</td>
                <td>候选版本进入审核队列</td>
              </tr>
              <tr>
                <td>09.17 16:08</td>
                <td>审核退回</td>
                <td>版式练习 v1.0</td>
                <td>需要补充授权说明</td>
              </tr>
              <tr>
                <td>09.16 14:32</td>
                <td>退款处理</td>
                <td>示例订单</td>
                <td>记录处理理由与授权变更</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p class="muted table-footnote">真实操作日志由服务端生成</p></template
      >
    </div>
  </main>
  <footer class="inner-footer page-width">
    <span>METEOR · 运营工作台</span>
    <nav><a href="/">首页</a></nav>
  </footer>
</template>
