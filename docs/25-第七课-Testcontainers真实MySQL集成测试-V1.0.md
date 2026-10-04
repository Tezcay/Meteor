# 第七课：Testcontainers 真实 MySQL 集成测试 V1.0

> 对应代码：`backend/src/test/java/dev/meteor/integration/PurchaseFlowMySqlIntegrationTest.java`。
> 总学习时间：约 2 小时 15 分钟；每节 10～20 分钟，可以分开完成。
> 前置课：[第四课：测试支付与事务内权益发放](22-第四课-测试支付与事务内权益发放-V1.0.md)。
> 当前验证状态：测试代码已经编译并被 Maven 发现；2026-10-04 本机 Docker Desktop 启动崩溃，因此 2 项容器测试被跳过，尚不能写成“Testcontainers 已跑通”。

## 0. 学习安排

| 小节 | 时间 | 阅读目标 | 停下后从哪里继续 |
| --- | ---: | --- | --- |
| 1. 为什么补集成测试 | 15 分钟 | 区分 Stub、Web 切片和真实数据库测试 | 第 2 节依赖与容器 |
| 2. 容器怎样接入 Spring | 15 分钟 | 看懂三个依赖、三个注解和 `@ServiceConnection` | 第 3 节数据库启动 |
| 3. Flyway 与隔离数据库 | 15 分钟 | 理解每次从空库迁移以及为什么不污染本地库 | 第 4 节成功链 |
| 4. 下单支付成功链 | 20 分钟 | 把 Service、Mapper、事务和真实表串起来 | 第 5 节幂等证据 |
| 5. 幂等与归属证据 | 15 分钟 | 看懂数据库计数和跨用户 404 的断言 | 第 6 节回滚链 |
| 6. 强制失败与事务回滚 | 20 分钟 | 理解临时 Trigger 怎样证明原子性 | 第 7 节运行方法 |
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
    private static final MySQLContainer MYSQL =
            new MySQLContainer("mysql:8.0.36");
}
```

- `@Testcontainers`：启用 JUnit 5 扩展。
- `@Container`：这个字段由扩展负责启动和停止。
- `static`：同一个测试类中的两个方法共享一个容器，避免每个方法重新拉起 MySQL。
- `@ServiceConnection`：Spring Boot 从容器读取 JDBC URL、用户名和密码，覆盖 `application.yml` 中的本地数据库连接。
- `disabledWithoutDocker = true`：没有 Docker 时明确跳过，不让普通单元测试连接或破坏本地 MySQL。

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

## 4. 第一项测试：真实下单和支付

方法名：

```java
void runsAllMigrationsAndCompletesAnIdempotentPurchase()
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

Service 返回同一个 ID 证明应用分支正确，行数为 1 证明没有偷偷插入重复订单。

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

## 5. 测试里怎样证明幂等和资源归属

### 5.1 不只断言返回对象

成功响应可能掩盖数据库重复写入，所以测试同时查询：

```text
orders                1 行
payment_attempts      1 行
payment_events        1 行
entitlement_grants    1 行
```

这些断言与数据库唯一约束一起检查结果。唯一约束负责最终防线，测试负责证明当前代码确实遵守设计。

### 5.2 重新通过 Service 读取订单

```java
assertThat(orderService.getOrder(buyerId, order.id()).status())
        .isEqualTo(OrderStatus.PAID);
```

这会执行 `OrderMapper.findDetailByIdAndUserId` 的真实 JOIN，能发现订单详情投影和权益字段映射问题。

### 5.3 另一个用户看不到订单

```java
assertThatThrownBy(() -> orderService.getOrder(creatorId, order.id()))
        .isInstanceOf(OrderNotFoundException.class);
```

测试使用真实用户 ID 和同一个订单 ID，证明 Mapper 的 `o.user_id = #{userId}` 生效。它没有通过 HTTP，因此这里只证明 Service/Mapper 的资源归属；401、Session 和 HTTP 404 仍由 Web 层测试覆盖。

## 6. 第二项测试：用真实故障证明事务回滚

方法名：

```java
void rollsBackPaymentWhenEntitlementInsertFails()
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

## 7. 运行、报告和当前环境问题

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

### 7.4 2026-10-04 的实际证据

本轮已经确认：

- Maven 成功解析 Spring Boot 4.1.0 管理的 Testcontainers 2.0.5；
- 新测试使用 Java 21 目标成功编译；
- 全仓 Maven 发现 95 项测试；
- 93 项非容器测试全部通过，其中包括统一错误 JSON、请求编号、公开商品、订单、支付与交付资源 HTTP 契约、OrderMapper XML 映射契约和支付状态分支；
- 新增 2 项因 Docker 环境不可用而跳过；
- Docker Desktop 4.86.0 日志显示启动时无法访问遗留的 `dockerInference` socket，随后后端退出；
- 尝试临时关闭 Docker AI 后仍触发同一错误，设置已经恢复；
- 没有重置 Docker、删除镜像或改动本地业务数据库。

因此当前简历和面试只能说“已接入 Testcontainers 测试代码，等待 Docker 环境修复后完成运行验证”。修复并得到 `Skipped: 0` 后，才能改为“使用 Testcontainers 验证真实 MySQL 迁移和事务回滚”。

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

普通模式允许 Docker 不可用的电脑继续做其他开发，但会打印 `skipped=2` 警告。此时可以说明 93 项非容器测试通过，不能说明 Testcontainers 通过。

Docker 可用后运行严格模式：

```powershell
.\scripts\verify.ps1 -RequireContainers
```

严格模式单独读取 `PurchaseFlowMySqlIntegrationTest` 的报告，要求 `tests=2` 且 `skipped=0`。当前机器的实际结果是命令按设计失败并报告 `expected tests=2 and skipped=0, got tests=2 and skipped=2`。这项失败是正确的质量门槛，不是新的业务回归。

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

> 还不能。本机 Docker Desktop 当前启动崩溃，Maven 报告 93 项非容器测试通过、2 项容器测试跳过。测试代码已经编译并进入测试发现；必须在 Docker 可用环境得到 2 项执行、0 跳过后，才能把它列为已验证证据。

### 下一步 CI 应做什么

Docker 环境修复后先完成本机 2 项测试，再添加 CI：

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
- [ ] 能说明临时 Trigger 为什么在 Grant 插入处制造失败。
- [ ] 能用四项数据库结果证明事务回滚。
- [ ] 能区分 `Failures: 0` 与 `Skipped: 2`。
- [ ] 能解释普通门禁为何警告跳过、严格门禁为何直接失败。
- [ ] 能说出 Maven Wrapper 固定了什么、仍未提供什么。
- [ ] 当前不会把尚未运行的容器测试写成已经通过。

下一次从这里继续：先修复或升级 Docker Desktop，使 `docker version` 返回 Server；随后运行本课两项测试。通过后更新本课、[项目首页](README.md)和[学习执行日志](08-学习执行日志.md)中的验证状态，再进入 CI。
