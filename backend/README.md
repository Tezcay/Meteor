# Meteor Backend：M1 公开商品读取

当前实现公开商品列表、关键词搜索和详情读取。Spring Boot 通过 MyBatis 从 MySQL 查询已发布商品与套餐。登录、交易、Redis 和文件交付尚未实现。

## 代码地图

```text
Vue 前端 frontend/（Vite 开发服务器）
  → GET /api/products?q=关键词
  → GET /api/products/1001
  → ProductController
  → ProductQueryService
  → ProductMapper + ProductMapper.xml
  → MySQL products / shops / packages
```

顶层 `product` 是商品业务模块。以后会并列增加 `order`、`payment`、`entitlement`、`delivery`；每个模块内部再使用熟悉的 `controller`、`service`、`mapper`、`dto`。这样既能按业务聚合，又不会把整个项目的所有 Controller、Service、Mapper 混在巨大的同名目录中。

必须看懂的文件：

1. `ProductController.java`：HTTP 请求从哪里进入。
2. `ProductQueryService.java`：参数检查、未找到处理和查询结果到 API DTO 的转换。
3. `ProductMapper.xml`：真正执行的两条 SQL。
4. `schema.sql`：主键、外键、唯一约束与索引。
5. `../frontend/src/api/products.ts`：前端如何调用接口并处理失败。

`ProductDetailResponse` 是返回给浏览器的 DTO。`ProductSummaryProjection`、`PackageOptionProjection` 是 SQL 查询列的投影，因为它们来自 JOIN 或部分字段，并不是数据库表的完整 Entity。三者使用 Java record 表达不可变数据载体；record 会自动生成构造器、字段访问方法、`equals`、`hashCode` 和 `toString`。

列表接口最多返回 24 件已发布商品，按创建时间与 ID 倒序排列。`q` 可搜索商品标题、简介和店铺名称，最多 100 个字符；`%`、`_`、`!` 作为普通字符匹配。分类尚未进入商品表，前端目前只把种子商品归为“模板”，其他分类继续展示不参与购买的灵感素材。

## 为什么暂时使用 schema.sql

M1 先用 Spring Boot SQL 初始化器建立最小结构，减少环境搭建阻塞。进入持续演进前会迁移到 Flyway，并停止修改已经应用的迁移文件。当前做法是阶段选择，不代表生产项目应在每次启动执行任意 DDL。

## 本地建库

MySQL root 密码只在本机输入，不要发到对话中。可以在 MySQL Workbench 打开并执行：

```text
E:\VibeCoding\Meteor\scripts\mysql\bootstrap-local.sql
```

它只创建本地 `meteor` 数据库和学习账号 `meteor_app`。账号的固定密码是可公开的本地开发密码，不得用于部署环境。

## 编译与测试

当前机器的 IntelliJ Maven 默认错误地使用 Java 8，而项目编译目标为 Java 21。Codex 沙箱与当前 Windows 用户还会选择不同的 Maven 本地缓存。仓库内脚本会为当前进程指定已安装的 JDK 26、当前用户的 Maven 缓存，再调用 IntelliJ Maven：

```powershell
.\mvn-local.ps1 test
```

项目编译目标为 Java 21，当前 JDK 26 可以生成 Java 21 字节码。这个脚本只适用于当前电脑；后续补 Maven Wrapper，避免依赖固定的安装路径。

## 启动

完成建库后：

```powershell
.\mvn-local.ps1 spring-boot:run
```

后端只提供 API；页面由独立前端在 `http://localhost:5173/` 提供。启动方式见 [前端说明](../frontend/README.md)。可检查：

- `http://localhost:8080/api/products/1001`：应返回商品 JSON。
- `http://localhost:8080/api/products`：应返回公开商品列表。
- `http://localhost:8080/api/products?q=求职`：应返回匹配的商品。
- `http://localhost:8080/api/products/9999`：应返回 404 和 `PRODUCT_NOT_FOUND`。
- `http://localhost:8080/api/products/0`：应返回 400 和 `INVALID_ARGUMENT`。

## 学习检查

先只回答三个问题：

1. 为什么 Controller 不直接调用 Mapper？
2. SQL 为什么同时检查 `p.status = 'PUBLISHED'` 和 `sale_status = 'ON_SALE'`？
3. `idx_package_product_sale (product_id, sale_status, display_order)` 对第二条 SQL 有什么帮助？不确定第三题时先看 EXPLAIN，不猜结论。
