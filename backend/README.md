# Meteor Backend：公开商品读取与会话认证

当前实现公开商品读取、Spring Security Session 登录，以及“服务端核价下单 → 本地测试支付 → 购买权益 → 内容库／订单 → Package 发布版本 → 冻结交付清单 → 一次性受控下载”闭环。数据库由 Flyway 管理到 V12。下载最小切片会流式校验 Classpath 文件并以 Spring `Resource` 写响应，不会把整份文件放进 JVM 堆内存；它仍不等同于生产对象存储、Range／断点续传或真实支付。

## 代码地图

```text
Vue 前端 frontend/（Vite 开发服务器）
  → GET /api/products?q=关键词
  → GET /api/products/1001
  → ProductController
  → ProductQueryService
  → ProductMapper + ProductMapper.xml
  → MySQL products / product_revisions / shops / packages

登录后下单：
  → POST /api/orders
  → OrderController
  → OrderService（服务端核价、幂等检查、事务）
  → OrderMapper
  → MySQL orders / order_items

本地测试支付：
  → POST /api/orders/{orderId}/test-payment
  → PaymentController
  → PaymentService（校验金额、事件幂等、状态转移）
  → PaymentMapper
  → MySQL payment_attempts / payment_events

支付后权益与账户查询：
  → PaymentService → EntitlementMapper → entitlement_grants
  → GET /api/library → LibraryService
  → GET /api/orders 与 /api/orders/{id} → OrderService

有效权益下的发布版本：
  → GET /api/library/{grantId}/releases
  → ReleaseController → ReleaseService
  → ACTIVE Grant 归属检查 → ReleaseMapper → MySQL releases

发布版本的冻结交付清单：
  → GET /api/library/{grantId}/releases/{releaseId}/manifest
  → DeliveryManifestController → DeliveryManifestService
  → ACTIVE Grant → Package／Release 归属检查
  → DeliveryManifestMapper → delivery_manifests / manifest_items / file_objects

受控下载：
  → POST /api/library/{grantId}/releases/{releaseId}/manifest/items/{manifestItemId}/download-authorizations
  → DownloadService 重新校验当前用户、ACTIVE Grant、PUBLISHED Release 和条目归属
  → download_tickets 保存五分钟一次性 token 的 SHA-256，不保存原 token
  → GET /api/downloads/{token}
  → 再次校验权益并流式核对文件大小与 SHA-256，随后锁定并消费票据
  → Spring Resource 分块写入文件响应
  → download_audits 记录 FILE_RESPONSE_PREPARED
```

顶层业务模块现在包括 `product`、`auth`、`order`、`payment`、`entitlement`、`delivery`、`download`；每个模块内部再使用熟悉的 `controller`、`service`、`mapper`、`dto`。跨模块的 HTTP 错误响应集中在 `web.error`，请求编号和完成日志集中在 `web.request`。这样既能按业务聚合，又不会把整个项目的所有 Controller、Service、Mapper 混在巨大的同名目录中。

必须看懂的文件：

1. `ProductController.java`：HTTP 请求从哪里进入。
2. `ProductQueryService.java`：参数检查、未找到处理和查询结果到 API DTO 的转换。
3. `ProductMapper.xml`：真正执行的两条 SQL。
4. `db/migration/`：按版本演进的表结构、数据迁移、约束与索引。
5. `../frontend/src/api/products.ts`：前端如何调用接口并处理失败。

`ProductDetailResponse` 是返回给浏览器的 DTO。`ProductSummaryProjection`、`PackageOptionProjection` 是 SQL 查询列的投影，因为它们来自 JOIN 或部分字段，并不是数据库表的完整 Entity。三者使用 Java record 表达不可变数据载体；record 会自动生成构造器、字段访问方法、`equals`、`hashCode` 和 `toString`。

列表接口最多返回 24 件已发布商品，按创建时间与 ID 倒序排列。`q` 可搜索商品标题、简介和店铺名称，最多 100 个字符；`%`、`_`、`!` 作为普通字符匹配。分类通过 `product_revision_taxonomy_terms` 关联到已发布 Revision，并支持按 taxonomy slug 过滤；本地数据库目前只预置一件真实商品，其他前端分类卡片继续作为不参与购买的灵感素材。

## 会话认证

认证使用数据库账号、BCrypt 密码哈希、Spring Security Session 和 CSRF 防护。公开商品 GET 接口和 CSRF 获取接口允许匿名访问；`/api/me` 及后续业务接口默认要求登录。

V3 迁移保留两个本地演示账号，密码均为 `password`；认证默认拒绝这两个账号，只有显式设置 `METEOR_DEMO_ACCOUNTS_ENABLED=true` 才能登录：

- `buyer@meteor.local`：普通买家，角色 `USER`。
- `creator@meteor.local`：创作者，角色 `USER`、`CREATOR`。

这些凭据只用于本地学习环境，部署时不得开启演示账号开关。调用顺序：

1. `GET /api/auth/csrf`，保留响应 Cookie，并读取响应中的 `headerName` 和 `token`。
2. `POST /api/auth/login`，发送 JSON 邮箱与密码，并将 token 放入指定请求头。
3. `GET /api/me`，携带同一会话 Cookie 获取当前用户。
4. 退出前重新获取 CSRF token，再 `POST /api/auth/logout`；成功返回 204。

登录会规范化邮箱的首尾空格和大小写，但不会修改密码内容。认证失败统一返回 `AUTHENTICATION_FAILED`，不区分账号不存在或密码错误。

## 数据库迁移

数据库结构由 Flyway 管理到 V12。V6 创建购买权益，V7 回填历史已支付订单，V8 默认锁定固定演示凭据，V9 创建 Package 级 Release，V10 创建不可变交付清单与文件对象元数据，V11 统一订单状态，V12 创建一次性下载票据与响应准备审计。应用启动时仅在显式开启本地开关后恢复演示密码，否则会再次锁定。已经应用的迁移文件不得修改；后续变化必须新增更高版本迁移。

为了接管早期没有 Flyway 历史表的本地 `meteor` 数据库，当前开发配置启用 `baseline-on-migrate` 并将基线版本设为 1。部署到全新或正式环境时应显式设置 `METEOR_FLYWAY_BASELINE_ON_MIGRATE=false`，避免错误接管未知数据库。

## 本地建库

MySQL root 密码只在本机输入，不要发到对话中。可以在 MySQL Workbench 打开并执行：

```text
E:\VibeCoding\Meteor\scripts\mysql\bootstrap-local.sql
```

它只创建本地 `meteor` 数据库和学习账号 `meteor_app`。账号的固定密码是可公开的本地开发密码，不得用于部署环境。

## 编译与测试

仓库包含 Maven Wrapper，并锁定 Maven 3.9.16。安装 Java 21 后即可执行：

```powershell
.\mvnw.cmd test
```

首次运行会从 Maven Central 下载该版本 Maven。`mvn-local.ps1` 只用于当前电脑上排查 IntelliJ Maven／JDK 路径问题，不是跨机器构建入口。

现有 93 项非容器测试覆盖公开商品列表／详情的 HTTP 契约、数据库账号加载、订单核价与幂等、统一错误 JSON、请求编号校验／MDC 清理／下载 token 日志遮蔽、订单 HTTP 认证／CSRF／校验／错误契约、OrderMapper XML 映射契约、测试支付事件、状态转移、金额校验与 HTTP 开关／认证／CSRF 契约、权益发放、内容库／Release／Manifest 的当前用户与资源隐藏契约、下载票据签发与一次性消费、文件完整性、存储失败，以及登录、Session、附件文件名和媒体类型的 Web 行为。

`PurchaseFlowMySqlIntegrationTest` 另外定义 2 项 Testcontainers 测试：临时 MySQL 从空库执行 Flyway V1～V12，再使用真实 MyBatis Mapper 验证下单／支付／权益／一次性下载链，以及 Grant 或 DownloadAudit 插入失败时的事务回滚。下载审计失败后还会使用同一 token 重试，证明票据更新已回滚。Docker 可用时可单独执行：

```powershell
.\mvnw.cmd -Dtest=PurchaseFlowMySqlIntegrationTest test
```

真正通过时报告必须为 `Tests run: 2, Skipped: 0`。2026-10-04 本机 Docker Desktop 因遗留 `dockerInference` socket 启动崩溃；当前全量报告是 95 项被发现、93 项通过、2 项跳过。测试代码已编译，不等于容器测试已验证。修复方法和逐行解释见[第七课](../docs/25-第七课-Testcontainers真实MySQL集成测试-V1.0.md)。

项目编译目标为 Java 21。仓库根目录的 `scripts/verify.ps1` 会统一执行 Wrapper 后端测试、前端格式与构建、文档链接和 Git 空白检查；Docker 可用后追加 `-RequireContainers`，如果这两项集成测试被跳过，命令会失败。

## 启动

完成建库后：

```powershell
$env:METEOR_DEMO_ACCOUNTS_ENABLED='true'
$env:METEOR_TEST_PAYMENT_ENABLED='true'
.\mvnw.cmd spring-boot:run
```

演示账号认证和 `test-payment` 本地学习适配器都默认关闭。前者由 `METEOR_DEMO_ACCOUNTS_ENABLED` 控制，后者由 `METEOR_TEST_PAYMENT_ENABLED` 控制；部署或普通运行时不要开启。

后端只提供 API；页面由独立前端在 `http://localhost:5173/` 提供。启动方式见 [前端说明](../frontend/README.md)。可检查：

- `http://localhost:8080/api/products/1001`：应返回商品 JSON。
- `http://localhost:8080/api/products`：应返回公开商品列表。
- `http://localhost:8080/api/products?q=求职`：应返回匹配的商品。
- `http://localhost:8080/api/products/9999`：应返回 404 和 `PRODUCT_NOT_FOUND`。
- `http://localhost:8080/api/products/0`：应返回 400 和 `INVALID_ARGUMENT`。
- 登录后用 `POST /api/orders` 创建订单：服务端会从 `packages.price_cents` 取价，不能由前端传入金额。
- 用 `POST /api/orders/{orderId}/test-payment` 模拟本地支付成功；同一 `providerEventId` 重放不会重复应用。
- 登录后用 `GET /api/library` 查看 ACTIVE 权益；用 `GET /api/orders` 和 `GET /api/orders/{orderId}` 查看当前用户订单及权益状态。
- 用 `GET /api/library/{grantId}/releases` 查看有效权益对应套餐的正式版本；别人的、撤销的或不存在的 Grant 返回 404。
- 用 `GET /api/library/{grantId}/releases/{releaseId}/manifest` 查看该版本冻结的文件清单；跨套餐 Release 或其他用户的 Grant 返回 404，响应不会泄露内部 `storage_key`。
- 用下载授权 POST 为某个 ManifestItem 申请五分钟票据，再请求响应中的 `downloadUrl`；同一 URL 成功一次后再次请求返回 404。

## 学习检查

先只回答三个问题：

1. 为什么 Controller 不直接调用 Mapper？
2. SQL 为什么同时检查 `p.status = 'PUBLISHED'` 和 `sale_status = 'ON_SALE'`？
3. `idx_package_product_sale (product_id, sale_status, display_order)` 对第二条 SQL 有什么帮助？不确定第三题时先看 EXPLAIN，不猜结论。
