# Day 2：Java 常用基础快速诊断

目标用时：30—45 分钟。这个练习不是背 API，而是确认后续业务代码所需的集合、对象相等和异常栈基础。

## 第一步：先预测，不运行

阅读 `JavaBasicsDiagnostic.java`，写下四个预测：

1. `list size`、`set size`、`map size` 分别是多少？
2. `filesById.get(101L)` 最终得到 `basic-guide.pdf` 还是 `renamed-guide.pdf`？
3. 为什么两个名称不同的 `ProductFile` 可能被 Set 当成同一个对象？
4. 异常栈中 `parsePositiveId`、`parseRequestedRelease`、`demonstrateExceptionStack` 的调用方向是什么？

## 第二步：运行

在本目录打开终端：

```powershell
java JavaBasicsDiagnostic.java
```

JDK 26 支持直接运行单文件源码，因此今天不需要 Maven。将实际输出与预测对照；预测错误比猜对更有学习价值，记录错误原因即可。

## 第三步：必须理解的四点

- List 保留插入顺序和重复元素，适合表达有顺序的交付清单，但不能自动保证业务唯一性。
- Set 根据 `equals`／`hashCode` 判断元素身份；这里把稳定 `id` 当作文件身份，名称变化不创造新身份。
- Map 用 key 定位值；相同 key 再次 `put` 会覆盖旧值。是否应该覆盖是业务问题，Map 不会替你做决定。
- 异常栈先显示异常发生位置，再沿调用链向外展开。包装异常时保留 cause，才能看到底层 `NumberFormatException`。

## 第四步：一个小变式

只做以下修改：把 `sameIdentity` 的 id 从 `101L` 改为 `303L`，文件名保持不变。不要改其他代码。

再次预测并运行，说明三个集合大小和 `file 101` 为什么发生或没有发生变化。完成后可以把你的解释发给助手，不需要粘贴整段运行输出。

## 过关自测

如果下面四题都能回答，就直接进入项目 M1：

1. 为什么数据库唯一约束不能由 Java Set 代替？
2. 为什么 `equals` 用可修改的文件名可能产生问题？
3. Map 相同 key 覆盖旧值，为什么不适合直接表达“重复支付事件已安全处理”？
4. 日志只打印 `exception.getMessage()` 会丢失哪些排错信息？
