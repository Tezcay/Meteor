# Meteor

Meteor 是一个面向数字作品交易与交付的全栈学习项目。当前重点是把“登录、下单、测试支付、权益发放、版本、文件清单和受控下载”做成能够测试、演示并在面试中解释的真实业务链路，同时控制无目的的页面扩张。

## 当前完成度

```text
公开商品
  → 数据库 Session 登录
  → 服务端核价与幂等下单
  → 本地测试支付事件
  → 事务内发放 EntitlementGrant
  → 内容库与订单查询
  → Package Release
  → DeliveryManifest 与 FileObject 元数据
  → 五分钟一次性 DownloadTicket
  → 权益复核、文件校验与 DownloadAudit
```

已完成：

- Spring Security Session、BCrypt 和 CSRF；固定演示账号默认锁定。
- 服务端核价、订单快照、用户级幂等键和并发冲突恢复。
- 默认关闭的本地测试支付适配器，以及金额、币种、事件键和交易键校验。
- 支付事务内幂等发放购买权益，历史已支付订单回填。
- 基于当前用户和 ACTIVE Grant 的订单、Release、Manifest 资源隔离。
- Package 级不可变 Release，以及包含真实 Classpath 素材、大小和 SHA-256 的冻结交付清单。
- 五分钟一次性下载票据；只保存 token 哈希，消费前重新校验 Session 用户、ACTIVE Grant、PUBLISHED Release、ManifestItem 和 FileObject，流式核对文件大小与 SHA-256，再以 Spring `Resource` 写响应并记录响应准备审计。
- Vue 3 桌面与手机端真实业务页面；内容库下载按钮已接真实授权和文件响应。
- 核心页面的 SPA 焦点、键盘菜单、可见焦点、触摸目标和减少动画支持；11 个路由完成 axe WCAG A/AA 扫描。
- Testcontainers 临时 MySQL 从空库执行 Flyway V1～V12，并实跑购买、下载、同键双线程下单幂等及两类事务回滚；严格门禁要求容器测试不得跳过。

尚未完成：

- 真实支付商签名、退款、对账和异常恢复任务。
- 创作者发布与审核写接口。
- 生产对象存储、Range／断点续传、失败下载审计、限流和对象版本校验。
- 注册／找回密码、CI、部署与性能验证。

因此本项目目前可以描述为“本地测试支付到权益、冻结清单和一次性受控下载的闭环”。当前下载已经避免整文件进入 JVM 堆内存，但只面向按部署约束视为不可变的 Classpath 学习素材；没有生产对象存储、Range 和传输失败处理，不能描述为生产大文件下载系统。

## 技术栈

- 后端：Java 21、Spring Boot、Spring Security、MyBatis、Flyway
- 数据库：MySQL 8.0
- 前端：Vue 3、TypeScript、Vite
- 验证：JUnit 5、AssertJ、MockMvc、Testcontainers、Playwright、axe-core、真实 MySQL HTTP 验收

当前基线：Flyway V12；Maven 后端测试 95 项全部通过、0 跳过，其中 2 项 Testcontainers 使用真实 MySQL 8.0.36，并包含两个线程同时使用同一幂等键的数据库竞争场景；前端类型检查与生产构建通过。11 个核心路由及本轮下载状态 axe WCAG A/AA 为 0 条违规。订单、支付尝试和支付事件状态已经使用明确枚举。每个 HTTP 响应包含 `X-Request-Id`，错误 JSON 和请求完成日志使用同一编号。

## 目录

```text
backend/                  Spring Boot API、迁移和后端测试
frontend/                 Vue 3 页面与 API Client
docs/                     需求、数据库、学习和面试文档
scripts/mysql/            本地 MySQL 初始化脚本
GLOSSARY.md               项目统一领域语言
```

## 本地运行

准备 Java 21、Node.js/npm 和 MySQL 8.0。仓库已经包含 Maven Wrapper，首次运行会下载锁定的 Maven 3.9.16，不要求全局安装 Maven。

1. 使用 MySQL 管理员执行 [本地初始化脚本](scripts/mysql/bootstrap-local.sql)。
2. 启动后端：

```powershell
cd backend
$env:METEOR_DEMO_ACCOUNTS_ENABLED='true'
$env:METEOR_TEST_PAYMENT_ENABLED='true'
.\mvnw.cmd spring-boot:run
```

3. 启动前端：

```powershell
cd frontend
npm install
npm run dev
```

4. 访问 `http://localhost:5173/`。本地演示账号为 `buyer@meteor.local` 或 `creator@meteor.local`，密码均为 `password`。两个开关只用于本地学习，普通运行和部署时不要开启。

当前电脑仍可在 `backend/` 使用 `./mvn-local.ps1` 排查本机环境；常规运行和验证应优先使用 Maven Wrapper。

## 验证

在仓库根目录运行：

```powershell
.\scripts\verify.ps1
```

该命令使用 Maven Wrapper 执行后端测试，再执行前端 Prettier 检查、TypeScript／生产构建、Markdown 相对链接检查和 `git diff --check`。普通模式会明确报告被跳过的测试；发布证据使用 `./scripts/verify.ps1 -RequireContainers`，要求两项 Testcontainers 测试均实际执行。2026-10-04 本机严格门禁已完整通过。

后端测试覆盖认证、订单核价与幂等、支付事件、权益、订单查询、Release、Manifest 和一次性下载权限。真实 MySQL 与浏览器验收结果记录在 [学习执行日志](docs/08-学习执行日志.md)。

`PurchaseFlowMySqlIntegrationTest` 已实际启动临时 MySQL，执行 Flyway V1～V12，并验证真实下单支付、顺序重放、同键双线程竞争、一次性下载、重复 token，以及 Grant／DownloadAudit 写入失败时的事务回滚和重试。本机报告为 `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`；双线程场景连续运行 3 轮均通过。它不是压力测试或性能证明，运行方法与证据边界见[第七课](docs/25-第七课-Testcontainers真实MySQL集成测试-V1.0.md)。

开发环境可在任一路由追加 `?a11y=1` 运行 axe，例如 `http://localhost:5173/products/1001?a11y=1`。详细复测方法、人工检查和未覆盖边界见 [无障碍审查报告](docs/18-无障碍审查与修复报告-2026-10-03.md)。

## 文档入口

- [文档导航：9 号到校后的唯一入口、当前版本和技术债](docs/README.md)
- [零基础项目学习主路线：先执行第 0 节的 90 分钟启动流程](docs/17-零基础项目学习路线-V1.0.md)
- [简历与面试证据清单：学完主线后逐条验收](docs/27-简历与面试证据清单-V1.0.md)
- [最新阶段审查与文档治理报告](docs/26-阶段审查与文档治理报告-2026-10-04.md)
- [历史版本归档](docs/archive/README.md)

业务承诺以 V0.4.0 设计基线和已接受 ADR 为准，已实现范围以 [文档首页](docs/README.md) 的现状矩阵和测试证据为准。
