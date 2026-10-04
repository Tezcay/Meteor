# Meteor 发布版本 Release 学习手册 V1.0

日期：2026-10-03
适用代码：Flyway V9、`delivery` 模块、内容库版本展开区
目标：理解用户拥有某套餐权益之后，系统如何只向本人展示这个套餐的正式发布版本，同时不提前暴露文件或伪造下载能力。

## 1. 这一步完成了什么

本阶段把链路从“我拥有 Basic 权益”推进到“Basic 当前有哪些正式版本”：

```text
当前 Session 用户
  → 选择自己内容库中的 Grant
  → GET /api/library/{grantId}/releases
  → 校验 Grant 属于当前用户且状态为 ACTIVE
  → 读取 Grant 指向的 packageId
  → 查询该 Package 下状态为 PUBLISHED 的 Release
  → 按 releaseNumber 从新到旧返回
```

新增接口：

```http
GET /api/library/{grantId}/releases
```

当前 Basic 与 Pro 各有一个 `v1.0.0` 正式版本。内容库点击“查看可用版本”后读取真实接口，展示版本号、说明、发布时间和最新标记。

V9 阶段完成时还没有实现文件清单、下载按钮和下载凭证；后续 V10～V12 已补齐冻结文件元数据和 Classpath 小文件的一次性下载，详见第 10 节与[第六课](24-第六课-一次性下载票据与受控文件响应-V1.0.md)。生产私有对象存储、大文件流和 Range 仍未实现。

## 2. Package、Grant、Release 的关系

| 对象 | 回答的问题 |
| --- | --- |
| Package | 买家选择购买哪个内容档位？ |
| EntitlementGrant | 当前用户为什么有资格访问这个档位？ |
| Release | 这个档位正式发布过哪些不可变版本？ |

关系是：

```text
Package 1 ── N Release
EntitlementGrant N ── 1 Package
```

Grant 不直接指向某个文件，也不只指向购买当时的一个版本。当前访问策略是 `ALL_RELEASES`，所以有效 Grant 可以看到对应 Package 所有符合条件的历史和未来正式版本。

Basic 与 Pro 即使版本号都叫 `1.0.0`，也是两个不同 Package 下的两个 Release。唯一约束因此是：

```sql
UNIQUE (package_id, release_number)
UNIQUE (package_id, version_label)
```

而不是全平台 `version_label` 唯一。

## 3. V9 表结构为什么这样设计

`releases` 主要字段：

```sql
package_id      -- 归属哪个套餐
release_number  -- 套餐内部单调递增的排序号
version_label   -- 给用户看的版本文字，例如 1.0.0
summary         -- 本次发布说明
status          -- PUBLISHED 或 DISABLED
published_at    -- 正式发布时间
disabled_at     -- 紧急停用时间
```

### 为什么同时保存 number 和 label

`version_label` 是展示文本，以后可能出现 `2026.10`、`1.0.0-hotfix` 等形式，不适合直接做可靠排序。`release_number` 是套餐内部的正整数序号，数据库可以稳定按它倒序查询。

### 为什么没有 DRAFT

这一小步只建立“正式交付版本”的读取模型，没有实现创作者发布工作流。当前表里的 Release 一创建就是正式版本，状态只允许：

- `PUBLISHED`：可被有资格用户看到。
- `DISABLED`：因安全或合规原因停止新的交付访问。

以后实现草稿、审核时，应区分发布草稿和已经形成的不可变 Release，不能为了页面方便随便扩展状态含义。

### 为什么有 disabledAt

普通商品下架只阻止新购买，不应抹掉老用户历史权益。Release 紧急禁用则表示这个具体版本不能继续用于新的文件授权。因此 Release 状态和 Product 在售状态是两套不同边界。

检查约束保证：

```sql
PUBLISHED → disabled_at 必须为空
DISABLED  → disabled_at 必须有值
```

## 4. 为什么接口先查 Grant，再查 Release

错误做法是让前端直接调用：

```http
GET /api/packages/2001/releases
```

如果这是公开接口，任何人只要猜到 Package ID 就能看到私有交付版本。当前实现先执行：

```sql
SELECT ...
FROM entitlement_grants
WHERE id = #{grantId}
  AND user_id = #{userId}
  AND status = 'ACTIVE'
```

只有找到有效 Grant，服务才使用其中可信的 `packageId` 查询 Release。前端不能自己传一个别人的 packageId 来扩大访问范围。

不存在、已撤销或属于别人的 Grant 都返回相同的 404 `ENTITLEMENT_NOT_FOUND`，避免通过响应差异枚举别人的购买情况。

## 5. ReleaseService 代码怎么读

入口：

```java
public List<ReleaseResponse> getAccessibleReleases(long userId, long grantId)
```

执行顺序：

1. 用 `userId + grantId + ACTIVE` 找 Grant。
2. 找不到时抛 `EntitlementGrantNotFoundException`。
3. 从 Grant 读取 `packageId`。
4. 查询该 Package 的 PUBLISHED Release。
5. 因 SQL 已按版本倒序，把列表第一项标记成 `latest = true`。
6. 返回不可变列表副本。

这里没有从 URL 接收 packageId，也没有相信前端传来的权益状态。

### `for` 循环里的 index 有什么用

```java
for (int index = 0; index < releases.size(); index++) {
    boolean latest = index == 0;
}
```

`index` 从 0 开始。Mapper 已保证最新版本排在第一项，所以 `index == 0` 就是最新版本。这个结论依赖 SQL 排序；如果以后改查询排序，测试应立即暴露问题。

### 为什么返回 `List.copyOf(result)`

内部使用 `ArrayList` 逐项构造 DTO，结束后用 `List.copyOf` 返回不可修改列表，避免调用者意外增删服务层结果。它不让数据库行变得不可变；数据库不变量仍由迁移和写入规则负责。

## 6. 前端展开交互怎么工作

重构后，Release 状态归 `ReleaseExplorer.vue` 自己管理，而不是继续堆在账户页：

```ts
const { data: releases, status, error, reload } = useKeyedLoader(
  () => props.grantId,
  (grantId, signal) => getAccessibleReleases(grantId, signal),
  '版本记录读取失败，请稍后重试。',
)
```

以 grantId 作为请求键有三个作用：

- Grant 改变时会清空旧结果并开始新请求。
- 每张权益卡拥有独立组件实例，不会和其他 Grant 共用错误或加载状态。
- 请求返回前若键改变或组件卸载，旧请求会被取消，递增序号还会阻止迟到响应写回。

`LibraryItemCard` 第一次展开时才挂载 `ReleaseExplorer`；收起使用 `v-show`，所以已经加载的版本仍保留在组件实例里，再次展开不会重复请求。

页面状态明确分开：

- 正在读取。
- 请求失败并允许重试。
- 有有效 Grant，但当前没有 PUBLISHED Release。
- 存在版本，展示版本记录。

“没有版本”不是请求失败；“请求失败”也不能显示成空列表。

组件卸载时 `useKeyedLoader` 会调用 `AbortController.abort()`；取消请求与序号校验是两层保护，前者节省工作，后者保证旧结果不能覆盖当前界面。

## 7. 本阶段测试证明什么

新增 3 项 ReleaseService 单元测试：

1. 有效 Grant 只查询自己的 packageId，并按顺序标记唯一最新版本。
2. 有效 Grant 对应套餐暂时没有正式版本时返回空列表。
3. 不存在、已撤销或外部用户的 Grant 返回 404 路径，并且根本不执行 Release 查询。

真实 MySQL 验证：

- Flyway V9 在 MySQL 8.0 成功执行。
- Basic Grant 返回 Basic `v1.0.0`，没有混入 Pro 版本。
- 切换另一账号访问同一 grantId 返回 404。

真实浏览器验证：

- 内容库点击可展开真实版本。
- 桌面和 390px 手机宽度布局正常。
- 页面没有假下载按钮。
- 浏览器控制台 0 error。

后端自动化测试总数由 34 增至 37，全部通过；前端生产构建通过。

## 8. 面试追问

### 问：为什么 Release 属于 Package，不直接属于 Product？

答：买家购买的是具体套餐，不同套餐交付范围不同。若 Release 只属于 Product，Basic 和 Pro 的文件边界会混在一起，授权判断无法准确落到所购档位。

### 问：为什么有效 Grant 才能查看 Release？

答：Release 是私有交付模型的一部分，不是公开商品预览。接口从当前 Session 取得 userId，并按 Grant 归属和状态判断，不能只依赖前端隐藏按钮。

### 问：为什么禁用 Release 不删除它？

答：历史订单、授权和审计仍需知道当时存在过哪个版本。禁用停止新的访问，删除会破坏可追溯性。

### 问：现在能不能说已实现文件下载？

答：可以说明已完成本地 Classpath 小文件的一次性受控下载；V12 会在消费时重新检查撤权并核对文件摘要。仍不能描述为生产交付，因为对象存储、大文件流、Range、限流和完整失败审计没有实现。

## 9. 简历如何更新

可以在现有权益闭环后追加：

> 建立 Package 级不可变 Release 读取模型，使用当前 Session 与 ACTIVE EntitlementGrant 限定版本访问范围；通过复合唯一约束、状态一致性检查和用户归属查询隔离不同套餐与账号的交付版本。

不能写：

- 已完成文件上传与下载。
- 已接入 OSS、S3 或 MinIO。
- 已实现短时签名 URL。
- 已实现创作者完整发布审核流程。

## 10. 下一小步

这一步已由 V10 完成：

```text
Release
  → DeliveryManifest
  → ManifestItem
  → FileObject 元数据
```

系统现在能够准确回答“这个 Release 包含哪些文件”。下一步实现下载授权接口；它必须重新检查 Session、ACTIVE Grant、Release 状态、Manifest 归属和 ManifestItem，最终只签发短时凭证或返回受控文件流，不返回永久私有存储地址。详细说明见 [交付清单与文件对象学习手册](16-交付清单与文件对象学习手册-V1.0.md)。
