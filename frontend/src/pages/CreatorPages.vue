<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import InnerHeader from '../components/InnerHeader.vue'
import { categoryTaxonomy, categories } from '../data/categories'
import { getLocalProfile, getLocalWorks, saveLocalProfile, saveLocalWork, type LocalWork } from '../data/localStore'
import type { DiscoveryCategory } from '../types/discovery'

const props = defineProps<{ path: string }>()
const active = computed(() =>
  props.path.includes('/releases')
    ? 'release'
    : props.path.includes('/review')
      ? 'review'
      : props.path.includes('/orders')
        ? 'orders'
        : props.path.includes('/start')
          ? 'start'
          : 'works',
)
const savedProfile = getLocalProfile()
const onboardingSubmitted = ref(false)
const displayName = ref(savedProfile?.displayName ?? '')
const introduction = ref(savedProfile?.introduction ?? '')
const editorTitle = ref('')
const editorSummary = ref('')
const versionLabel = ref('v1.0')
const packageName = ref('Basic')
const workCategory = ref<Exclude<DiscoveryCategory, 'all'>>('templates')
const workSubcategory = ref('work')
const tagInput = ref('')
const localFileNames = ref<string[]>([])
const editorNotice = ref('')
const reviewTab = ref<'draft' | 'review' | 'published' | 'rejected'>('review')
const localWorks = ref<LocalWork[]>(getLocalWorks())

watch(workCategory, (category) => {
  workSubcategory.value = categoryTaxonomy[category][0]?.value ?? ''
})

const workTags = computed(() =>
  tagInput.value
    .split(/[,，\n]/)
    .map((tag) => tag.trim())
    .filter(Boolean)
    .slice(0, 8),
)

function addLocalFiles(event: Event) {
  const input = event.target as HTMLInputElement
  localFileNames.value = Array.from(input.files ?? []).map((file) => file.name)
  editorNotice.value = localFileNames.value.length ? '文件仅显示在当前浏览器中，尚未上传' : ''
}
function savePreview() {
  if (!editorTitle.value.trim()) {
    editorNotice.value = '先填写作品名称，再保存草稿'
    return
  }
  saveLocalWork({
    title: editorTitle.value.trim(),
    summary: editorSummary.value.trim(),
    version: versionLabel.value.trim() || 'v1.0',
    packageName: packageName.value.trim() || 'Basic',
    files: localFileNames.value,
    category: workCategory.value,
    subcategory: workSubcategory.value,
    tags: workTags.value,
  })
  localWorks.value = getLocalWorks()
  editorNotice.value = '草稿已保存在当前浏览器'
}

function submitForReview() {
  if (!editorTitle.value.trim()) {
    editorNotice.value = '先填写作品名称，再提交审核'
    return
  }
  saveLocalWork(
    {
      title: editorTitle.value.trim(),
      summary: editorSummary.value.trim(),
      version: versionLabel.value.trim() || 'v1.0',
      packageName: packageName.value.trim() || 'Basic',
      files: localFileNames.value,
      category: workCategory.value,
      subcategory: workSubcategory.value,
      tags: workTags.value,
    },
    'review',
  )
  localWorks.value = getLocalWorks()
  editorNotice.value = '已提交到本地审核队列'
}

function saveOnboarding() {
  saveLocalProfile({ displayName: displayName.value.trim(), introduction: introduction.value.trim() })
  onboardingSubmitted.value = true
}
</script>

<template>
  <InnerHeader section="creator" />
  <main id="page-main" class="inner-main page-width workspace-layout" tabindex="-1">
    <aside class="workspace-sidebar">
      <div class="workspace-sidebar-title"><span>创作者空间</span></div>
      <nav aria-label="创作者导航">
        <a href="/creator/start" :aria-current="active === 'start' ? 'page' : undefined">入驻资料</a
        ><a href="/creator/works" :aria-current="active === 'works' ? 'page' : undefined"
          >我的作品</a
        ><a href="/creator/releases/new" :aria-current="active === 'release' ? 'page' : undefined"
          >新建发布</a
        ><a href="/creator/review" :aria-current="active === 'review' ? 'page' : undefined"
          >审核状态</a
        ><a href="/creator/orders" :aria-current="active === 'orders' ? 'page' : undefined"
          >小店订单</a
        >
      </nav>
    </aside>
    <div class="workspace-content">
      <template v-if="active === 'start'"
        ><div class="workspace-heading">
          <h1>创作者入驻</h1>
          <p>填写小店信息，仅供页面预览</p>
        </div>
        <div class="creator-onboard">
          <div class="onboard-art">
            <img src="/images/creator-studio-1280.webp" alt="创作桌面上的画稿与色卡" /><span
              >每件用心完成的作品<br />都值得被好好展示</span
            >
          </div>
          <form class="editor-form" @submit.prevent="saveOnboarding">
            <label for="creator-name">小店名称</label
            ><input
              id="creator-name"
              v-model="displayName"
              maxlength="40"
              required
              placeholder="给小店起个名字"
            /><label for="creator-intro">介绍一下你的创作</label
            ><textarea
              id="creator-intro"
              v-model="introduction"
              maxlength="300"
              required
              rows="5"
              placeholder="你喜欢做什么，作品适合谁"
            />
            <div class="field-note">这会成为访客认识你的第一段话</div>
            <button class="button button-primary" type="submit">预览入驻信息</button>
            <p v-if="onboardingSubmitted" class="inline-note" role="status">
              {{ displayName }} 的资料已保存在当前浏览器
            </p>
          </form>
        </div></template
      >

      <template v-else-if="active === 'works'"
        ><div class="workspace-heading split-heading">
          <div>
            <h1>我的作品</h1>
          </div>
          <a class="button button-primary" href="/creator/releases/new">新建发布</a>
        </div>
        <div class="preview-ribbon">
          <span>{{ localWorks.length ? `当前浏览器已保存 ${localWorks.length} 件作品` : '还没有本地作品' }}</span
          ><a href="/creator/start">编辑入驻资料 ↗</a>
        </div>
        <div class="works-feature">
          <div class="works-feature-art">
            <span>Northstar Design</span><strong>把经历<br />排成故事</strong>
          </div>
          <div class="works-feature-copy">
            <span class="demo-badge">作品示例</span>
            <h2>{{ localWorks[0]?.title ?? '求职材料模板包' }}</h2>
            <p>{{ localWorks[0] ? `${localWorks[0].version} · ${localWorks[0].status === 'review' ? '审核中' : '草稿已保存'}` : '界面示例' }}</p>
            <div class="work-statuses"><span>已上架</span><span>候选版本审核中</span></div>
            <a class="underlined-link" href="/creator/review">查看审核状态 ↗</a>
          </div>
        </div>
        <div class="works-next">
          <div>
            <h2>新建作品</h2>
          </div>
          <a href="/creator/releases/new">开始整理 ↗</a>
        </div></template
      >

      <template v-else-if="active === 'release'"
        ><div class="workspace-heading">
          <h1>发布作品</h1>
          <p>草稿保存在当前浏览器，不会上传</p>
        </div>
        <div class="release-layout">
          <div class="release-form">
            <section class="editor-section">
              <div class="editor-section-title">
                <span>1</span>
                <div>
                  <h2>作品介绍</h2>
                </div>
              </div>
              <div class="editor-form">
                <label for="work-title">作品名称</label
                ><input
                  id="work-title"
                  v-model="editorTitle"
                  maxlength="80"
                  placeholder="例如：求职材料模板包"
                /><label for="work-summary">一句话介绍</label
                ><textarea
                  id="work-summary"
                  v-model="editorSummary"
                  maxlength="300"
                  rows="4"
                  placeholder="说说作品里有什么"
                />
              </div>
            </section>
            <section class="editor-section">
              <div class="editor-section-title">
                <span>2</span>
                <div>
                  <h2>分类与标签</h2>
                  <p>大类与小类使用平台词表，标签可帮助访客继续筛选</p>
                </div>
              </div>
              <div class="editor-form editor-two-columns">
                <div>
                  <label for="work-category">大类</label>
                  <select id="work-category" v-model="workCategory">
                    <option v-for="item in categories" :key="item.value" :value="item.value">{{ item.label }}</option>
                  </select>
                </div>
                <div>
                  <label for="work-subcategory">小类</label>
                  <select id="work-subcategory" v-model="workSubcategory">
                    <option v-for="item in categoryTaxonomy[workCategory]" :key="item.value" :value="item.value">{{ item.label }}</option>
                  </select>
                </div>
              </div>
              <div class="editor-form">
                <label for="work-tags">标签</label>
                <input id="work-tags" v-model="tagInput" maxlength="120" placeholder="例如：建筑光影，极简，自然光" />
              </div>
            </section>
            <section class="editor-section">
              <div class="editor-section-title">
                <span>3</span>
                <div>
                  <h2>版本与套餐</h2>
                  <p>同一套餐的新版本会保留原有购买关系</p>
                </div>
              </div>
              <div class="editor-form editor-two-columns">
                <div>
                  <label for="version-label">版本标识</label
                  ><input id="version-label" v-model="versionLabel" placeholder="v1.0" />
                </div>
                <div>
                  <label for="package-name">套餐名称</label
                  ><input id="package-name" v-model="packageName" placeholder="Basic" />
                </div>
              </div>
            </section>
            <section class="editor-section">
              <div class="editor-section-title">
                <span>4</span>
                <div>
                  <h2>交付文件</h2>
                  <p>交付文件与公开预览分开管理</p>
                </div>
              </div>
              <label class="file-drop" for="release-file"
                ><strong>选择本地文件，预览列表样式</strong
                ><span>当前仅显示文件名，不会上传或保存</span
                ><input id="release-file" type="file" multiple @change="addLocalFiles"
              /></label>
              <ul v-if="localFileNames.length" class="local-files">
                <li v-for="name in localFileNames" :key="name">{{ name }} <span>本地预览</span></li>
              </ul>
            </section>
            <div class="editor-actions">
              <button class="button button-secondary" type="button" @click="savePreview">
                检查草稿状态</button
              ><button class="button button-primary" type="button" @click="submitForReview">提交审核</button>
            </div>
            <p v-if="editorNotice" class="inline-note" role="status">{{ editorNotice }}</p>
          </div>
          <aside class="release-aside">
            <h2>发布前检查</h2>
            <ul class="check-list">
              <li>公开预览中不包含交付文件地址</li>
              <li>每个套餐都有对应的版本与文件清单</li>
              <li>授权、格式、适用工具写得足够清楚</li>
              <li>候选版本审核时，旧版本继续可用</li>
            </ul>
            <a href="/creator/review">预览审核状态 ↗</a>
          </aside>
        </div></template
      >

      <template v-else-if="active === 'review'"
        ><div class="workspace-heading">
          <h1>审核状态</h1>
          <p>状态示例，非真实审核结果</p>
        </div>
        <div class="state-switch" role="group" aria-label="预览发布状态">
          <button
            v-for="item in [
              { value: 'draft', label: '草稿' },
              { value: 'review', label: '审核中' },
              { value: 'published', label: '已上架' },
              { value: 'rejected', label: '需修改' },
            ]"
            :key="item.value"
            type="button"
            :aria-pressed="reviewTab === item.value"
            @click="reviewTab = item.value as 'draft' | 'review' | 'published' | 'rejected'"
          >
            {{ item.label }}
          </button>
        </div>
        <div class="review-stage">
          <span class="demo-badge">状态示例</span>
          <div class="review-stage-top">
            <div>
              <span>求职材料模板包 · 候选版本 v1.3</span>
              <h2>
                {{
                  {
                    draft: '草稿',
                    review: '审核中',
                    published: '已上架',
                    rejected: '审核退回',
                  }[reviewTab]
                }}
              </h2>
              <p>
                {{
                  {
                    draft: '介绍、预览和交付文件都可以继续编辑',
                    review: '旧版本仍可被已购买的用户使用，新版本审核完成后再发布',
                    published: '购买此套餐的用户可在内容库查看更新',
                    rejected: '根据审核意见修改后，可以重新提交',
                  }[reviewTab]
                }}
              </p>
            </div>
            <span class="review-stage-symbol">{{
              { draft: '✳', review: '◌', published: '✓', rejected: '↺' }[reviewTab]
            }}</span>
          </div>
          <div class="review-timeline">
            <div class="done">
              <span></span>
              <p>保存草稿</p>
            </div>
            <div :class="{ done: reviewTab !== 'draft' }">
              <span></span>
              <p>提交审核</p>
            </div>
            <div :class="{ done: reviewTab === 'published' }">
              <span></span>
              <p>正式上架</p>
            </div>
          </div>
          <a class="underlined-link" href="/creator/releases/new">返回发布编辑 ↗</a>
        </div></template
      >

      <template v-else
        ><div class="workspace-heading">
          <h1>小店订单</h1>
        </div>
        <div class="preview-ribbon">
          <span>订单接口尚未接入，下列记录仅作展示</span><a href="/creator/works">返回我的作品 ↗</a>
        </div>
        <div class="orders-overview">
          <div><span>订单示例</span><strong>12</strong></div>
          <div><span>待处理售后示例</span><strong>1</strong></div>
          <div><span>已发布作品示例</span><strong>1</strong></div>
        </div>
        <div class="table-wrap">
          <table class="calm-table">
            <thead>
              <tr>
                <th>作品与套餐</th>
                <th>订单状态</th>
                <th>售后</th>
                <th>金额</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>求职材料模板包 <small>Basic</small></td>
                <td>已完成</td>
                <td>无</td>
                <td>¥19.00</td>
              </tr>
              <tr>
                <td>求职材料模板包 <small>Pro</small></td>
                <td>已完成</td>
                <td>退款审核中</td>
                <td>¥39.00</td>
              </tr>
              <tr>
                <td>求职材料模板包 <small>Basic</small></td>
                <td>已退款</td>
                <td>已结束</td>
                <td>¥19.00</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p class="muted table-footnote">
          示例金额来自项目种子套餐；订单记录与状态并非真实数据
        </p></template
      >
    </div>
  </main>
  <footer class="inner-footer page-width">
    <span>METEOR · 创作者空间</span>
    <nav><a href="/discover">发现作品</a><a href="/">首页</a></nav>
  </footer>
</template>
