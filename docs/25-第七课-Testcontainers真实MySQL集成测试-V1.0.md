# 第七课：Testcontainers 真实 MySQL 集成测试 V1.0

> 对应代码：`backend/src/test/java/dev/meteor/integration/PurchaseFlowMySqlIntegrationTest.java`。
> 总学习时间：约 2 小时 30 分钟；每节 10～20 分钟，可以分开完成。
> 前置课：[第四课：测试支付与事务内权益发放](22-第四课-测试支付与事务内权益发放-V1.0.md)。
> 当前验证状态：2026-10-05 本机已实际启动 MySQL 8.0.36 容器；2 项容器测试通过、0 跳过，全量后端 95 项通过，严格门禁完整通过；同键双线程下单场景连续运行 3 轮均通过。

## 0. 学习安排

| 小节 | 时间 | 阅读目标 | 停下后从哪里继续 |
| --- | ---: | --- | --- |
| 1. 为什么补集成测试 | 15 分钟 | 区分 Stub、Web 切片和真实数据库测试 | 第 2 节依赖与容器 |
| 2. 容器怎样接入 Spring | 15 分钟 | 看懂三个依赖、三个注解和 `@ServiceConnection` | 第 3 节数据库启动 |
| 3. Flyway 与隔离数据库 | 15 分钟 | 理解每次从空库迁移以及为什么不污染本地库 | 第 4 节成功链 |
| 4. 下单支付下载成功链 | 20 分钟 | 把 Service、Mapper、事务、文件和真实表串起来 | 第 5 节幂等证据 |
| 5. 幂等、并发与归属证据 | 30 分钟 | 看懂数据库计数、双线程竞争和跨用户 404 的断言 | 第 6 节回滚链 |
| 6. 强制失败与事务回滚 | 20 分钟 | 理解两个临时 Trigger 怎样证明支付与下载原子性 | 第 7 节运行方法 |
| 7. 怎样运行和判读报告 | 15 分钟 | 分清通过、跳过和环境失败 | 第 8 节质量门禁 |
| 8. 一键质量门禁 | 15 分钟 | 看懂 Wrapper、普通模式和严格模式 | 第 9 节测试边界 |
| 9. 面试表达与后续 CI | 15 分钟 | 准确说明已经完成和仍待验证的部分 | 完成本课清单 |

本课没有新增业务接口或数据库迁移。它为已经存在的下单、支付和权益代码增加自动化证据。

## 1. 为什么现有 93 项非容器测试还不够

### 1.1 Stub 单元测试证明了什么

`OrderServiceTest` 和 `PaymentServiceTest` 自己实现了 Stub Mapper。例如：

```java
private final StubOrderMapper orderMapper = new StubOrderMapper();
private final OrderService orderService = new OrderService(productMapper, orderMapper);
```

Stub 是测试中手写的简化替身。它能快速验证 Service 的分支判断，例如：

- 同一个幂等键是否返回旧订单；
- 金额不一致是否拒绝支付；
- 相同支付事件是否走重放分支；
- Service 是否调用权益 Mapper。

它没有启动 Spring、MyBatis、Flyway 或 MySQL，所以不能证明：

- Mapper 注解中的 SQL 能被 MySQL 执行；
- SQL 列名能正确映射到 Java record；
- V1～V12 可以从空库连续迁移；
- `@Transactional` 能把多张表的写入一起回滚；
- MySQL 唯一约束、外键、检查约束和行锁按预期工作。

### 1.2 MockMvc Web 测试证明了什么

`AuthControllerWebTest` 和 `DownloadControllerWebTest` 主要检查 HTTP 层：状态码、CSRF、认证要求、JSON 和响应头。它们使用 Mock Bean 隔离业务依赖，不访问真实数据库。

### 1.3 新测试补哪一层

新增测试启动完整 Spring Boot 上下文，并让 Testcontainers 创建一个临时 MySQL：

```text
JUnit
  → Spring Boot ApplicationContext
  → OrderService / PaymentService
  → 真实 MyBatis Mapper
  → 临时 MySQL 8.0.36
  → Flyway V1～V12
```

Testcontainers 是在自动化测试期间启动临时容器的 Java 库。官方说明它适合用真实数据库替代兼容性不完整的内存数据库，并让每次测试从已知状态开始：[Testcontainers Database Containers](https://java.testcontainers.org/modules/databases/)。

## 2. Maven 依赖和容器注解

### 2.1 `pom.xml` 新增的三个测试依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-mysql</artifactId>
    <scope>test</scope>
</dependency>
```

三个依赖分别负责：

| 依赖 | 职责 |
| --- | --- |
| `spring-boot-testcontainers` | 把容器提供的连接信息交给 Spring Boot |
| `testcontainers-junit-jupiter` | 用 JUnit 5 管理容器的启动和停止 |
| `testcontainers-mysql` | 提供 MySQL 专用容器类型和 JDBC 支持 |

`scope=test` 表示这些库只进入测试编译和测试运行，不会打进生产应用。版本由 Spring Boot 4.1.0 的依赖管理统一为 Testcontainers 2.0.5，项目没有再写一份容易漂移的版本号。

### 2.2 完整 Spring 上下文

```java
@SpringBootTest(properties = {
        "spring.flyway.baseline-on-migrate=false",
        "meteor.demo-accounts.enabled=false",
        "meteor.test-payment.enabled=false"
})
```

`@SpringBootTest` 会寻找 `MeteorApplication`，创建生产代码中的 Service、Mapper、事务管理器、数据源和 Flyway。

三个属性的含义：

- 空测试库必须执行 V1，因此关闭 `baseline-on-migrate`；
- 演示账号恢复逻辑保持关闭，测试直接调用 Service，不用固定密码登录；
- 测试支付 HTTP Controller 保持关闭，测试直接验证 `PaymentService` 的核心事务。

直接调用 Service 不验证 Session 和 CSRF；这两部分由现有 Web 测试负责。分层测试各自回答不同问题。

### 2.3 JUnit 管理容器

```java
@Testcontainers(disabledWithoutDocker = true)
class PurchaseFlowMySqlIntegrationTest {

    @Container
    @ServiceConnection
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.36")
            .withCommand("--log-bin-trust-function-creators=1")
            .withDatabaseName("meteor_test")
            .withUsername("meteor_test")
            .withPassword("meteor_test");
}
```

- `@Testcontainers`：启用 JUnit 5 扩展。
- `@Container`：这个字段由扩展负责启动和停止。
- `static`：同一个测试类中的两个方法共享一个容器，避免每个方法重新拉起 MySQL。
- `@ServiceConnection`：Spring Boot 从容器读取 JDBC URL、用户名和密码，覆盖 `application.yml` 中的本地数据库连接。
- `disabledWithoutDocker = true`：没有 Docker 时明确跳过，不让普通单元测试连接或破坏本地 MySQL。

`--log-bin-trust-function-creators=1` 只传给这次测试容器。回滚测试需要由测试账号临时创建 Trigger；MySQL 开启 binary log 时，默认会拒绝没有 `SUPER` 权限的账号创建 Trigger。这个参数不在 `application.yml` 中，生产应用不会启用，也不会在运行时创建 Trigger。

官方 JUnit 文档说明静态容器会在该测试类的方法之间共享，并在类结束后停止：[Testcontainers JUnit 5](https://java.testcontainers.org/test_framework_integration/junit_5/)。

## 3. 从空数据库执行 Flyway

### 3.1 容器数据库不是本地 `meteor`

```java
new MySQLContainer("mysql:8.0.36")
        .withDatabaseName("meteor_test")
        .withUsername("meteor_test")
        .withPassword("meteor_test");
```

容器把 MySQL 端口随机映射到主机。测试代码不硬编码端口，`@ServiceConnection` 自动提供连接信息。

这意味着：

- 不需要知道本机 MySQL root 密码；
- 不会读取或清理本地 `meteor` 的订单；
- 容器停止后临时库整体消失；
- 每次重新创建容器都从空数据库开始。

### 3.2 应用启动时 Flyway 自动迁移

Spring Boot 创建数据源后，Flyway 按版本执行 `db/migration`：

```text
V1 商品目录
  → V3 演示账号数据
  → V4 订单
  → V5 支付
  → V6/V7 权益
  → V9 Release
  → V10 Manifest/FileObject
  → V11 订单状态
  → V12 下载票据与审计
```

测试使用真实查询确认成功迁移数量：

```java
SELECT COUNT(*)
FROM flyway_schema_history
WHERE success = 1
```

断言是 12。它能发现缺失迁移、MySQL 不兼容 SQL、执行顺序和约束创建错误。

### 3.3 为什么不用 H2

H2 是运行在 JVM 内的数据库，启动更快，但 MySQL 的以下细节不一定完全一致：

- `ON DUPLICATE KEY UPDATE`；
- `TIMESTAMP(6)` 和默认时间；
- `utf8mb4_0900_ai_ci`；
- `SELECT ... FOR UPDATE`；
- 检查约束和错误码；
- MySQL Trigger 与事务行为。

本项目已经使用多项 MySQL 特性，因此这条测试直接选择 MySQL 容器。

## 4. 第一项测试：真实下单、支付和一次性下载

方法名：

```java
void runsAllMigrationsAndCompletesIdempotentPurchaseAndDownload()
```

### 4.1 从迁移数据找到用户

```java
long buyerId = userId("buyer@meteor.local");
long creatorId = userId("creator@meteor.local");
```

`userId` 使用 `JdbcTemplate` 查询真实 `users` 表。这同时证明 V3 的种子数据存在。测试没有把用户主键猜成固定数字。

### 4.2 创建订单

```java
OrderResponse order = orderService.createOrder(
        buyerId,
        new CreateOrderRequest(1001L, 2001L, "it-purchase-success")
);
```

这一次调用实际经过：

```text
OrderService
  → ProductMapper 查询 PUBLISHED Product
  → ProductMapper 查询 ON_SALE Package
  → OrderMapper 插入 orders
  → OrderMapper 插入 order_items 成交快照
  → Spring 事务提交
```

它验证价格 `1900` 来自数据库，而不是测试请求；订单初始状态来自 Java `OrderStatus.PENDING_PAYMENT`。

### 4.3 使用同一个幂等键重放

相同用户、商品、套餐和幂等键再次调用 `createOrder`：

```java
assertThat(replayedOrder.id()).isEqualTo(order.id());
```

数据库计数还必须是 1：

```sql
SELECT COUNT(*)
FROM orders
WHERE idempotency_key = 'it-purchase-success';
```

Service 返回同一个 ID 证明应用分支正确，行数为 1 证明没有偷偷插入重复订单。这一段是先后调用；第 5.2 节再让两个线程同时进入。

### 4.4 应用支付事件

```java
TestPaymentEventRequest paymentEvent = new TestPaymentEventRequest(
        "it-event-success",
        "it-transaction-success",
        1900L,
        "CNY"
);

PaymentResponse payment =
        paymentService.applyTestPayment(buyerId, order.id(), paymentEvent);
```

真实事务按顺序执行：

```text
锁定当前用户订单
  → 插入 payment_attempts
  → 插入 payment_events
  → attempt 改为 SUCCEEDED
  → order 改为 PAID
  → INSERT ... SELECT 创建 entitlement_grants
  → 事务提交
```

随后用相同事件再次调用。事件 ID 和 Grant ID 必须保持不变，三张表的相关记录都只能有一条。

### 4.5 从真实 Grant 走到文件响应

支付成功返回真实 `entitlementGrantId` 后，测试继续调用：

```java
DownloadAuthorizationResponse authorization = downloadService.authorize(
        buyerId,
        grantId,
        3001L,
        6001L
);
String token = tokenFrom(authorization);
DownloadFilePayload file = downloadService.consume(buyerId, token);
```

这段代码连接 Flyway V6～V12 创建的 Grant、Release、ManifestItem、FileObject、DownloadTicket 和 DownloadAudit。断言会检查 634 B Classpath 文件可读、票据状态为 `CONSUMED`、审计恰好一条，并再次消费同一 token 得到 `DownloadNotFoundException`。

这仍是 Service／Mapper／真实 MySQL 集成测试，没有经过 Session、CSRF 或浏览器保存；HTTP 边界继续由 `DownloadControllerWebTest` 与真实浏览器验收负责。

## 5. 测试里怎样证明幂等、并发和资源归属

### 5.1 不只断言返回对象

成功响应可能掩盖数据库重复写入，所以测试同时查询：

```text
orders                1 行
payment_attempts      1 行
payment_events        1 行
entitlement_grants    1 行
download_tickets      1 行，状态 CONSUMED
download_audits       1 行
```

这些断言与数据库唯一约束一起检查结果。唯一约束负责最终防线，测试负责证明当前代码确实遵守设计。

### 5.2 两个线程怎样制造真实唯一键竞争

预计时间：15 分钟。读完后能解释 `CountDownLatch`、测试 Trigger 和四项断言各自负责什么。

顺序重放会在第二次调用开始前看到旧订单，无法进入“两个请求都先查不到”的竞争窗口。测试因此创建两个任务，让它们先在同一条起跑线上等待：

```java
CountDownLatch ready = new CountDownLatch(2);
CountDownLatch start = new CountDownLatch(1);
ExecutorService executor = Executors.newFixedThreadPool(2);

Future<OrderResponse> first = executor.submit(
        () -> createOrderAfterStartSignal(buyerId, request, ready, start)
);
Future<OrderResponse> second = executor.submit(
        () -> createOrderAfterStartSignal(buyerId, request, ready, start)
);

assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
start.countDown();
```

`CountDownLatch` 是一次性的线程协作计数器。两个任务各自执行 `ready.countDown()`，主测试确认两者都已就绪后调用 `start.countDown()`，两个任务才同时执行 `orderService.createOrder`。

只同时放行仍可能被操作系统迅速串行调度。为了稳定扩大两个事务的重叠时间，测试在临时库创建一个只延迟 `orders` 插入的 Trigger：

```sql
CREATE TRIGGER delay_integration_test_order
BEFORE INSERT ON orders
FOR EACH ROW
SET @integration_test_delay = SLEEP(1);
```

两个线程会先分别执行幂等查询；进入插入时各等待 1 秒，更容易让双方都已经读到“还没有订单”。随后唯一约束只允许一个事务写入，失败方捕获 `DuplicateKeyException`，通过 `SELECT ... FOR UPDATE` 当前读读取胜出的订单。

Trigger 只存在于 Testcontainers 的一次性数据库中，并在 `finally` 中删除。它不属于生产逻辑，也不会让真实用户下单等待 1 秒。

测试最后同时检查返回值和数据库状态：

```java
assertThat(responses.get(0).id()).isEqualTo(responses.get(1).id());
assertThat(count("orders", "idempotency_key", idempotencyKey)).isEqualTo(1);
assertThat(count("order_items", "order_id", responses.get(0).id())).isEqualTo(1);
```

这证明当前代码在一次两请求竞争中收敛到同一订单，并且没有重复订单项。本机连续运行 3 轮均通过。测试没有增加用户数、请求持续时间或并发级别，也没有统计吞吐量与延迟，因此不能称为压力测试、性能测试或生产容量验证。

### 5.3 重新通过 Service 读取订单

```java
assertThat(orderService.getOrder(buyerId, order.id()).status())
        .isEqualTo(OrderStatus.PAID);
```

这会执行 `OrderMapper.findDetailByIdAndUserId` 的真实 JOIN，能发现订单详情投影和权益字段映射问题。

### 5.4 另一个用户看不到订单

```java
assertThatThrownBy(() -> orderService.getOrder(creatorId, order.id()))
        .isInstanceOf(OrderNotFoundException.class);
```

测试使用真实用户 ID 和同一个订单 ID，证明 Mapper 的 `o.user_id = #{userId}` 生效。它没有通过 HTTP，因此这里只证明 Service/Mapper 的资源归属；401、Session 和 HTTP 404 仍由 Web 层测试覆盖。

## 6. 第二项测试：用真实故障证明事务回滚

方法名：

```java
void rollsBackBusinessTransactionsWhenDownstreamInsertFails()
```

### 6.1 为什么需要主动制造失败

正常链全部成功只能证明“能提交”。要证明 `@Transactional` 的原子性，必须让事务在已经写入支付数据、已经尝试把订单改为 PAID 之后失败。

如果只传错误金额，Service 会在任何写入前拒绝，无法证明中途失败能回滚。

### 6.2 临时 MySQL Trigger

测试在临时库创建：

```sql
CREATE TRIGGER reject_integration_test_grant
BEFORE INSERT ON entitlement_grants
FOR EACH ROW
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = 'forced entitlement failure for transaction test';
```

Trigger 是数据库在特定写操作发生时自动执行的规则。这里它只存在于一次性的容器数据库中，用于让 Grant 插入抛出真实 MySQL 错误。

### 6.3 失败发生的位置

调用支付前半段已经执行：

```text
insert payment_attempts
  → insert payment_events
  → update payment_attempts
  → update orders to PAID
  → insert entitlement_grants 时 Trigger 报错
```

MyBatis 把 JDBC 错误翻译成 Spring `DataAccessException`。运行时异常离开 `@Transactional` 方法后，Spring 标记事务回滚。

### 6.4 `finally` 保证清理 Trigger

```java
try {
    assertThatThrownBy(() -> paymentService.applyTestPayment(...))
            .isInstanceOf(DataAccessException.class);
} finally {
    jdbcTemplate.execute(
            "DROP TRIGGER IF EXISTS reject_integration_test_grant"
    );
}
```

`finally` 无论断言成功还是失败都会执行，避免 Trigger 影响同一容器里的下一项测试。

### 6.5 回滚后的四项证据

失败后必须同时成立：

| 数据 | 期望 |
| --- | --- |
| `orders.status` | 仍为 `PENDING_PAYMENT` |
| `payment_attempts` | 0 行 |
| `payment_events` | 0 行 |
| `entitlement_grants` | 0 行 |

这四项一起证明本地支付不会留下“订单已支付但无权益”或“事件存在但状态没改”的半成品。

### 6.6 审计写入失败时下载票据也必须回滚

同一个测试随后完成另一笔 Basic 购买并签发下载票据，再在临时库创建第二个 Trigger：

```sql
CREATE TRIGGER reject_integration_test_download_audit
BEFORE INSERT ON download_audits
FOR EACH ROW
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = 'forced download audit failure for transaction test';
```

`DownloadService.consume` 的事务顺序是：锁定票据、重新授权、校验文件、更新票据为 `CONSUMED`、插入审计。Trigger 在最后一步制造真实数据库错误；正确结果必须是前面的票据更新一起回滚：

| 数据 | 失败后的期望 |
| --- | --- |
| `download_tickets.status` | 仍为 `ISSUED` |
| `download_tickets.consumed_at` | 仍为 `NULL` |
| `download_audits` | 0 行 |

测试删除 Trigger 后使用同一 token 重试。重试应成功返回 634 B 文件，并把票据改为 `CONSUMED`、写入一条审计。这一步证明的不只是数据库快照，还证明回滚后用户仍保留一次有效下载机会。

## 7. 运行、报告和本机修复记录

### 7.1 先确认 Docker 引擎可用

```powershell
docker version
```

必须同时看到 Client 和 Server。只有 Client 代表安装了命令行，不能启动容器。

### 7.2 只运行这两个集成测试

项目使用 Maven Wrapper 3.9.16，不要求另装 Maven：

```powershell
cd backend
.\mvnw.cmd -Dtest=PurchaseFlowMySqlIntegrationTest test
```

macOS／Linux 安装 Java 21 后可使用对应脚本：

```powershell
./mvnw -Dtest=PurchaseFlowMySqlIntegrationTest test
```

第一次需要从容器镜像仓库拉取 `mysql:8.0.36`，耗时会明显长于普通单元测试。

### 7.3 怎样才算真正通过

报告必须出现：

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

下面这个结果不等于通过：

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 2
```

`Skipped: 2` 表示 JUnit 发现了测试，但因为 Docker 不可用没有执行测试方法。

### 7.4 2026-10-05 的实际证据

本轮得到的实际证据：

- Docker Desktop 4.86.0 最初被两个损坏的 Windows AF_UNIX socket 阻塞：`Docker/run/dockerInference` 和 `docker-secrets-engine/engine.sock`；停止进程后同时移动两个父目录并关闭 Docker AI，Linux 引擎恢复为 `Running`。旧目录保留为时间戳备份，没有恢复出厂设置，也没有删除镜像、容器或卷。
- 第一次真实运行暴露 MySQL 错误 1419：应用测试账号没有 `SUPER` 权限，无法创建故障注入 Trigger。测试容器增加 `--log-bin-trust-function-creators=1` 后，业务仍使用普通 `meteor_test` 账号，只有一次性测试服务器放宽 Trigger 创建限制。
- 单独运行 `PurchaseFlowMySqlIntegrationTest` 连续 3 轮均得到 `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`；每轮都执行同键双线程竞争。
- 临时 MySQL 从空库成功应用 Flyway V1～V12；第一项实跑购买成功、支付重放、逐层授权、一次性下载和重复 token；第二项实跑 Grant 与 DownloadAudit 两类失败回滚，以及同 token 重试。
- 全仓严格门禁得到后端 `Tests run: 95, Failures: 0, Errors: 0, Skipped: 0`；前端 Prettier、TypeScript 和 Vite 生产构建、51 份 Markdown 相对链接及 Git 空白检查同时通过。
- 测试连接的是临时 `meteor_test` 容器库，结束后由 Testcontainers 清理；没有连接或改动本机 `meteor` 业务数据库。

因此现在可以准确表述为“使用 Testcontainers 和真实 MySQL 8.0.36 验证 Flyway V1～V12、同键双线程下单、购买到下载成功链，以及支付／下载事务回滚”。不能扩大成真实支付、生产对象存储、部署、高并发或性能验证。

## 8. 一键质量门禁

预计时间：15 分钟。读完后能解释为什么“测试命令退出码为 0”仍可能没有执行容器测试。

仓库根目录的 `scripts/verify.ps1` 把当前交付检查集中为一条命令：

```powershell
.\scripts\verify.ps1
```

它依次执行：

1. `backend/mvnw.cmd test`，并读取 Surefire XML 汇总测试、失败、错误和跳过数；
2. `npm run format:check`，只检查前端格式；
3. `npm run build`，执行 TypeScript 检查和 Vite 生产构建；
4. 检查仓库 Markdown 的相对链接；
5. 执行 `git diff --check`，拦截本次差异中的空白错误。

普通模式允许 Docker 不可用的电脑继续做其他开发，但会打印容器测试跳过警告。出现该警告时，只能报告实际执行的非容器测试，不能沿用本机这次 `Skipped: 0` 证据说明当前环境也跑过容器。

Docker 可用后运行严格模式：

```powershell
.\scripts\verify.ps1 -RequireContainers
```

严格模式单独读取 `PurchaseFlowMySqlIntegrationTest` 的报告，要求 `tests=2` 且 `skipped=0`。2026-10-05 本机实际结果为 `tests=2, skipped=0`，随后前端和文档检查也全部完成，命令退出码为 0。

Maven Wrapper 固定构建工具版本，Java 21 仍需由电脑或 CI 提供。Wrapper 第一次运行要下载 Maven，不能把它理解为把 JDK 和所有依赖都存进仓库。

本轮格式基线还发现 `SiteHeader.vue` 的模板事件里写了两条语句；Prettier 换行后 Vue 编译器无法解析。代码已改为 `openMobileSearch()` 具名函数。这个例子说明格式检查和生产构建必须连续执行：格式正确不代表语法一定可编译。

## 9. 面试表达和下一步 CI

### 问：为什么同时保留单元测试和 Testcontainers？

答：

> Stub 单元测试启动快，适合覆盖金额不匹配、事件冲突和重放等大量业务分支；Testcontainers 数量少、运行慢，用真实 MySQL 验证 Flyway、MyBatis SQL、数据库约束和 Spring 事务。两层测试回答的问题不同，不能互相替代。

### 问：怎样证明支付和权益在一个事务里？

答：

> 集成测试在临时 MySQL 中创建一个只针对 Grant 插入的 Trigger，让支付流程在最后阶段抛出数据库异常。随后断言订单仍是 PENDING_PAYMENT，支付尝试、支付事件和 Grant 都是 0 行。如果 `@Transactional` 没有覆盖这些 Mapper 调用，前面的写入会残留。

### 问：测试为什么不会污染开发数据库？

答：

> MySQL 运行在 Testcontainers 创建的临时容器中，连接信息由 `@ServiceConnection` 注入。Flyway 从空库执行，测试结束后容器停止并删除，因此不需要按业务 ID 清理本地 `meteor` 数据。

### 问：目前可以说集成测试已经通过吗？

答：

> 可以，但要带上范围：本机已用 Testcontainers 启动 MySQL 8.0.36，2 项集成测试实际执行且 0 跳过，验证了 V1～V12、同键双线程下单、购买和下载链、Grant 与 DownloadAudit 失败回滚。双线程场景连续 3 轮通过，但这不是压力测试；它也没有经过真实支付商、生产对象存储或部署环境。

### 下一步 CI 应做什么

本机 2 项测试已经完成，下一步在确认实际代码托管平台后添加 CI：

1. 使用 Java 21；
2. 缓存 Maven 依赖；
3. 运行 `mvn test`；
4. 要求总报告没有失败、错误或跳过的容器测试；
5. 再运行前端 `npm ci`、类型检查、格式检查和生产构建；
6. 保留测试报告，失败时能看到具体测试类和容器日志。

本阶段没有创建 CI 配置，不能把“计划”描述成“提交会自动检查”。

## 10. 本课过关清单

- [ ] 能解释 Stub Mapper 为什么不能证明 SQL 正确。
- [ ] 能说出三个 Testcontainers 依赖各自负责什么。
- [ ] 能解释 `@Container`、`static` 和 `@ServiceConnection`。
- [ ] 能说明为什么测试库关闭 `baseline-on-migrate`。
- [ ] 能画出成功支付写入的四类数据。
- [ ] 能解释相同幂等键和相同支付事件怎样验证只写一次。
- [ ] 能解释两个 `CountDownLatch` 如何让两个下单任务从同一起跑线开始。
- [ ] 能说明延迟 Trigger 只负责扩大测试竞争窗口，不属于生产下单逻辑。
- [ ] 能用相同订单 ID、1 个订单头和 1 个订单项证明两请求收敛。
- [ ] 能说明临时 Trigger 为什么在 Grant 插入处制造失败。
- [ ] 能用四项数据库结果证明事务回滚。
- [ ] 能说明一次性下载成功时 Ticket 和 Audit 各自写入什么。
- [ ] 能说明 Audit 插入失败时 Ticket 为什么必须回到 ISSUED，并能用同一 token 重试。
- [ ] 能区分 `Failures: 0` 与 `Skipped: 2`。
- [ ] 能解释普通门禁为何警告跳过、严格门禁为何直接失败。
- [ ] 能说出 Maven Wrapper 固定了什么、仍未提供什么。
- [ ] 不会把“本机 2 项真实 MySQL 测试通过”扩大成生产数据库、真实支付、压力测试或高并发容量验证。

下一次从这里继续：先亲手运行一次 `scripts\verify.ps1 -RequireContainers` 并打开 Surefire 报告定位这 2 项测试；能够解释成功链和两类回滚后，再按实际代码托管平台接入 CI。
