# 0.1.9.2 更新说明：紧急修复（打开背包即崩溃、建筑机指南无法打开）

基于 0.1.9.1（`pr-0.1.9.1-20261014`）。只含修复，没有新功能。构建环境为 Gradle 8.10.2、JDK 21、NeoForge 21.1.218，离线构建。

## 1. 打开背包或 AE2 无线合成终端时崩溃（`Not building!`）

* 现象：穿戴装有框架核心的头盔或胸甲时，打开背包（玩家预览）或 AE2 无线合成终端（`WCTScreen` 的玩家预览）立即崩溃，`CoreAttachmentLayer.quads` 抛出 `IllegalStateException: Not building!`（0.1.9.0 引入，0.1.9.1 相同）。
* 原因：`CoreAttachmentLayer.draw` 在逐段绘制之前取得一次装甲渲染类型（`armorCutoutNoCull`）的 `VertexConsumer` 并在循环中重复使用；循环中发光段又向同一个 `MultiBufferSource` 请求 `RenderType.eyes`。即时缓冲源（界面中的实体预览、没有批处理渲染器的世界渲染）所有非固定渲染类型共用一个缓冲区，切换类型会结束上一批次，之前取得的 consumer 失效，下一段写入时崩溃。
* 修复：改为两遍绘制。第 1 遍对每个外装取得装甲类型的 consumer 并画完它的全部段；第 2 遍对每个外装在第一个发光段处才取得 eyes 类型的 consumer 并画完全部发光段。任何 consumer 都在取得后立即使用，不再在另一次 `getBuffer` 之后使用。
* 同类排查：客户端渲染代码共有 55 处 `getBuffer`（36 个文件）；人工复核了同一方法内多次调用的 11 个文件，并用新检查自动扫描全部文件，没有其他跨 `getBuffer` 复用 consumer 的位置。新检查 `check_192.py` 会自动发现这种写法（对 0.1.9.1 运行时正确报出原问题）。
* 日志中随后的 `block_update` 解码失败是崩溃后客户端断开连接时产生的连带错误，不是独立问题。

## 2. 蓝图建筑机的指南页无法打开

* 现象：GuideME 打开建筑机页面时解析失败。
* 原因：第 41 行把界面上的文件选择按钮写成 `"<"` `">"`。GuideME 按 MDX 解析，`<` 后面不是标签名时报错。用户报告的是中文页，检查后英文页（`"<"`）与日文页（`「<」`）同样有误。
* 修复：三种语言都改为代码样式 `` `<` `` `` `>` ``（按钮）。新检查扫描全部指南页中不构成标签的 `<` 与标签外的 `{`：现在为 0 处。

## 3. 外部模组的日志错误与防护

* `TooltipNode.empty`（AdvancedLootInfo）与 `CCKJeiPlugin.registerCategories`（Create: Central Kitchen）的错误来自这两个模组自身（前者在客户端访问服务器端注册表，后者向 JEI 注册空的分类数组），与本模组无关。
* 本模组相关位置加了防护：
  * JEI 插件：配方分类逐个创建，某个失败时记录日志并跳过，不会向 JEI 注册空数组；15 种配方类型的注册逐步保护，一种失败不影响其余。
  * 提示框：武装库链接提示、Alt 冻结提示与提示尺寸测量捕获异常并记录（安静失败），不会打断其他模组的提示。

## 4. 验证

* `gradle build coreJar coreSourcesJar apiJar apiSourcesJar addonExampleJar checkCoreDistribution --offline` 通过。
* `verify-si192/check_192.py`：缓冲区生命周期（客户端渲染代码中跨 `getBuffer` 或在调用 `getBuffer` 的循环中复用 consumer）、全部指南页的 MDX 文本、JEI 防护 —— 0 个问题；对 0.1.9.1 运行时报出 6 个问题（原 `CoreAttachmentLayer`、三语建筑机页、JEI 未防护），说明检查有效。
* 0.1.9.1／0.1.9.0／0.1.8.6／0.1.8.5／0.1.8.4／0.1.8.3 的检查与三语注释检查仍为 0 个问题。
* **未在游戏中验证**：背包与 AE2 终端预览不再崩溃、核心外装与发光在界面与世界中的显示、建筑机指南页的显示。本机无法启动游戏客户端与 GameTest。
