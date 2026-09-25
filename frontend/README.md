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
发现页的商品搜索读取后端真实数据；本地数据库目前只预置一件“实用模板”商品，分类字段尚未进入后端模型
其他分类仍是灵感陈列，不能购买，也不会混入真实搜索结果
后端未连接时，默认浏览状态保留一张明确标为“设计示例”的卡片；带关键词的搜索不会用示例商品冒充结果
详情支持动态商品 ID；结账页仍只预览套餐和金额，不创建订单或收款

登录、创作者入驻及发布入口显示明确的未开放提示，不采集账号或密码

## 验证

```powershell
npm run typecheck
npm run build
```

人工检查：首页无价格、标题无句号；桌面与手机无横向溢出；导航键盘可用
检查“探索作品”下滑、四个分类定位、创作者定位与回到顶部，不应打开弹窗
检查灵感签切换后文案和链接同步更新、阅读进度随滚动变化、减少动态效果时仍可正常浏览
检查搜索打开和关闭、Escape 返回、分类空状态、接口失败后的重试与作品简介
生产预览使用 `npm run preview`；与后端整合部署时仍需由网关转发 `/api`

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
- `src/styles.css`：颜色、字体、间距、响应式和弹窗样式
- `public/images/`：优化后的装饰图片，见 [素材说明](public/images/README.md)
- `public/fonts/`：本地标题字体及 OFL 许可证

更换标题时需要重新生成字体子集，或使用覆盖新字符的完整字体
旧的蓝色分屏、商品卡片与演示商品数据已从首页代码中移除
