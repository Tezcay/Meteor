# Meteor 前端

独立的 Vue 3 + TypeScript + Vite 应用，Spring Boot 通过 `/api` 提供数据

## 本地启动

```powershell
npm install
npm run dev
```

默认访问 http://localhost:5173/，端口占用时以终端打印的地址为准
首页的展示图、字体和布局均使用本地资源，不需要启动后端即可浏览

“探索作品”向下滚动到灵感展示区，顶部“创作者”与兴趣分类分别定位到对应区块
搜索及展示区的分类箭头会打开作品浏览弹窗，并按需请求 `GET /api/products/1001`
需要真实数据时，另按 [后端说明](../backend/README.md) 启动 Spring Boot
Vite 将 `/api/*` 代理到 http://localhost:8080，无需为本地开发放开跨域
接口失败时弹窗提供重试，不使用虚构商品补充结果

## 首页设计

- 首页采用内容优先结构：品牌、搜索、精选海报、分类、创作者入口
- 删除营销口号、灵感签、英文装饰语和重复说明，只保留导航与操作文字
- 精选区使用用户提供的竖版海报，不显示价格、销售标签或虚构商品信息
- 海报手动循环切换，不自动播放；分类卡片保留克制的悬停反馈和滚动渐显
- 分类在桌面端四列、手机端单列；展示图不代表已上架商品
- 导航、按钮、分类和弹窗均为独立 HTML/Vue 元素，可键盘操作
- 尊重系统减少动态效果设置，前后端继续保持独立

## 当前功能边界

后端 M1 提供 `GET /api/products?q=` 公开商品列表与搜索，以及 `GET /api/products/{id}` 详情接口
发现页的商品搜索读取后端真实数据，并可按已发布 Revision 关联的 taxonomy slug 过滤；本地数据库目前只预置一件“实用模板”商品
其他分类仍是灵感陈列，不能购买，也不会混入真实搜索结果
后端未连接时，默认浏览状态保留一张明确标为“设计示例”的卡片；带关键词的搜索不会用示例商品冒充结果
详情支持动态商品 ID；登录后结账页会提交后端 `POST /api/orders` 创建待支付订单。结果页只显示真实后端订单，本地测试支付需要后端显式设置 `METEOR_TEST_PAYMENT_ENABLED=true`，且尚未接入真实收款。支付成功会产生真实购买权益，`/library`、`/orders` 和 `/orders/{id}` 分别显示当前用户的内容库、订单列表和详情，不再使用浏览器中的假购买记录。

内容库可按 Grant 展开对应套餐的正式 Release，再按 Release 展开冻结文件清单；数据来自 Release 和 Manifest 两个 GET 接口。权益卡、版本浏览和清单面板已拆成独立组件，并通过 keyed loader 隔离请求、取消过期请求和拒绝迟到响应。点击“下载文件”会先 POST 申请五分钟一次性凭证，再 GET 受控文件响应；界面提供忙碌、成功和失败播报，不接收或暴露 `storage_key`。

登录页已接入后端 Session：本地设置 `METEOR_DEMO_ACCOUNTS_ENABLED=true` 后，可使用 `buyer@meteor.local` 或 `creator@meteor.local`，密码均为 `password`。生产构建不显示演示身份快捷选择，后端故障时路由也不会信任本地角色。统一 API Client 负责错误解析、请求编号、CSRF token 缓存和单次 403 刷新重试；后端返回 `requestId` 时，页面错误会附上可用于查日志的请求编号。创作者入驻、作品发布、真实支付和生产对象存储仍未开放；当前下载只服务本地 Classpath 学习素材。

## 验证

```powershell
npm run format:check
npm run build
```

`format:check` 只检查格式，不改文件；`build` 依次执行 `vue-tsc --noEmit` 和 Vite 生产构建。全仓交付前应回到仓库根目录运行 `./scripts/verify.ps1`。

人工检查：首页无价格、标题无句号；桌面与手机无横向溢出；导航键盘可用
检查“探索作品”下滑、四个分类定位、创作者定位与回到顶部，不应打开弹窗
检查灵感签切换后文案和链接同步更新、阅读进度随滚动变化、减少动态效果时仍可正常浏览
检查搜索打开和关闭、Escape 返回、分类空状态、接口失败后的重试与作品简介
生产预览使用 `npm run preview`；与后端整合部署时仍需由网关转发 `/api`

### 开发环境无障碍检查

任一路由追加 `?a11y=1` 会按需加载 axe-core，并把 WCAG A/AA 结果写入 `#meteor-a11y-result`：

```text
http://localhost:5173/?a11y=1
http://localhost:5173/products/1001?a11y=1
```

这只在 Vite 开发模式且显式带查询参数时运行，不应进入生产运行路径。自动扫描通过后仍要人工检查 Tab 顺序、可见焦点、Escape 与焦点恢复、SPA 路由焦点、320px 溢出和减少动画。当前审查证据与未覆盖范围见 [无障碍审查报告](../docs/18-无障碍审查与修复报告-2026-10-03.md)。

## 代码位置

- `src/components/HeroSection.vue`：标题、主操作、响应式展台图片
- `src/components/SiteHeader.vue`：桌面导航及手机菜单
- `src/components/CategoryNavigation.vue`：分类入口
- `src/components/DiscoverySection.vue`：四类灵感视觉、灵感签与分类浏览入口
- `src/components/CreatorSection.vue`：浏览提示与创作者场景介绍
- `src/components/SiteFooter.vue`：页尾导航
- `src/components/PageProgress.vue`：阅读进度与浮动返回顶部
- `src/components/SearchDialog.vue`：按需读取现有商品接口、查询和分类过滤
- `src/components/FeatureDialog.vue`：登录与发布的功能状态提示
- `src/data/categories.ts`：分类文案与标识
- `src/api/products.ts`、`src/types/product.ts`：商品列表、详情 API 与 DTO
- `src/api/auth.ts`、`src/types/auth.ts`：CSRF、登录、当前用户和退出 API
- `src/api/orders.ts`：结账页创建订单的 CSRF、Session 和幂等请求
- `src/api/payments.ts`：结果页触发本地测试支付事件，不代表真实收款
- `src/api/http.ts`：统一 GET／POST、API 错误和 CSRF token 生命周期
- `src/api/library.ts`：读取有效权益、Release 与冻结清单，并申请／消费一次性下载凭证
- `src/pages/AccountPages.vue`：账户页路由、内容库／订单首屏状态和订单视图
- `src/components/library/LibraryItemCard.vue`：单个有效 Grant 的内容库卡片和版本入口
- `src/components/library/ReleaseExplorer.vue`：对应套餐的 Release 加载、错误恢复与展开状态
- `src/components/library/DeliveryManifestPanel.vue`：冻结文件清单、下载状态和浏览器 Blob 保存
- `src/composables/useKeyedLoader.ts`：按响应式键加载、取消旧请求并阻止迟到响应覆盖
- `src/utils/formatters.ts`：人民币、中文日期时间与文件大小格式化
- `src/utils/devA11y.ts`：仅开发环境按需运行的 axe WCAG A/AA 审查器
- `src/styles.css`：颜色、字体、间距、响应式和弹窗样式
- `public/images/`：优化后的装饰图片，见 [素材说明](public/images/README.md)
- `public/fonts/`：本地标题字体及 OFL 许可证

更换标题时需要重新生成字体子集，或使用覆盖新字符的完整字体
旧的蓝色分屏、商品卡片与演示商品数据已从首页代码中移除
