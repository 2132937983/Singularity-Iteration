# 奇点迭代 0.1.7.36 更新记录

交付源码基于 0.1.7.35；本次 PR 将增量合并到上游 0.1.7.35.1，并保留上游电动锄、蒸汽再加压机、配方和核爆修复。面向长期维护与后续版本迁移的全面整理：复杂算法补充「为什么／契约／不变量」注释，核心 API 注释全部改为 ASD-STE100 英文；全面排查边界值、空值、线程与客户端／服务端边界问题并修复；新增数据包驱动的数值调参（`si_tuning`）；网络载荷增加计数上限校验与客户端→服务端限流。交付提交共修改 632 个文件（含注释、源码、资源与开发工具）。

### 注释与文档

- 电网（`IndependentSiEnergy`、`EnergyNetGlobal`、`PlatformTopology`、`NetworkWalker`、`FECompatTile`、`GridEventHandler`）、多方块（`common/multiblock/` 全部、`MultiblockEnergyCore/Part`）、同步与载荷（`ClientSyncThrottle`、`SuitPackets`、`SuitSensorData`、`MachineStatusTracker/Packet`、`EquipmentConsolePacket`、武装库载荷、`mio_icif_Network` 的 47 个载荷一览表）、FCS 与特殊机动（`BallisticSolver`、`SensorTracker`、`OreScanner`、`HoloMap`、`FcsProjection`、`ManeuverMode(Client)`）补充 Why／Contract／Invariants：单位、取值范围、空值规则、线程、调用时机与副作用。
- `core/`（API、prefab、runtime、platform）共 210 个文件的中文注释改为 ASD-STE100 英文；此前已处理 `common/` 中 371 个文件（含 163 个乱码文件的修复）。所有注释改动都用去注释后的代码 token 比对工具逐文件确认「代码零改动」。
- 删除 38 个无引用的死文件（挤压机残留、八叉树／波形爆炸调度器、旧配方构建器、`ClientKeyHelper`、`PipeFluidRenderer`、`NetworkHelper`、`CustomEnergyStorage`、空热存储、`AddonExample` 等），以及 76 个未使用的私有字段、49 个孤立的 `@SuppressWarnings("unused")`。
- 剩余：`common/` 下约 550 个文件仍有中文注释（以机器方块实体、注册表与界面为主），留待后续版本。

### 缺陷修复

- `IUpgradeItem.getUpgradeEffect()`：匿名类中 6 个方法调用的是自身而非外层接口方法，任意调用都会无限递归导致 `StackOverflowError`。改为 `IUpgradeItem.this.xxx()`。
- 动能／热能发电机消耗燃料时先 `shrink` 再取容器物品，空堆总是返回空，熔岩桶的空桶会丢失。改为先读取容器物品。
- 扫描仪（OV／OD）：建筑高度上限按包含处理（`y == maxBuildHeight` 也被读取），改为不含；扫描循环改用可变坐标，不再每格分配 `BlockPos`。
- `ArmoryActionPacket`：没有权限解绑时也会提示「已解绑」并关闭界面；`unbind` 现在返回结果，只有成功才提示。
- `PlatformTopology`：观察者回调中关闭拓扑后再隔离区块会因 `server == null` 抛出空指针，现已防护；重建扫描不再为每个区块新建临时集合。
- `getRedstonePorts()` 返回内部可变集合，改为只读视图。`FECompatTile` 删除重复 import，日志文本改为英文。
- 激光塔统计：超过 64 字符的实体自定义名称会让编码抛出异常导致同步失败，现截断后写入。
- `SensorTracker`：先截取 64 个再按距离排序，远处生物可能挤掉近处生物；改为先排序后截取，只为保留的目标计算路径与意图。空中预测的水平阻力由 0.98 改为原版的 0.91。
- `HoloMap`：+x／+z 方向的界外目标被画在盒子外一格，钳制上限修正为 `RADIUS + 0.5`。
- `ManeuverModeClient.VISUALS` 从不移除离开的玩家；新类 `ClientSessionCleanup` 每 100 tick 清理，并在退出世界时重置 `FcsProjection` 与特殊机动视觉状态。
- `ClientSyncThrottle`：非服务器线程调用时直接 `sendBlockUpdated`（区块映射非线程安全），改为投递到服务器线程。
- 变压器模式切换提示原为硬编码中文，改为翻译键 `message.mio_icif.transformer.mode_switched`（带英文回退），补充日文模式名称。`MachineAPIImpl.getRedstoneModeName` 与 `getRunningState` 的 API 返回值由中文改为英文纯文本。
- 全部 `toLowerCase()／toUpperCase()` 改为 `Locale.ROOT`（土耳其语等区域设置下标识符会出错）；`AbstractElectricTool.getDurabilityPercent` 容量为 0 时不再返回 NaN。
- 此前一轮已修复：6 个 JEI 分类的空配料 `get(0)`、`FcsHud` 空路径、`IndependentSiEnergy` 的 `printStackTrace` 改为日志、`sampleTick` 的 `MIN_VALUE` 防护、遮蔽器每 20 tick 运行、武装库远程强制加载区块冷却、能量转换器整数 EU 取整、新的 `BlockEditPermission`（喷涂器与扳手的方块编辑权限检查）。

### 网络载荷

- 新 `PacketLimits`：列表计数由编码端截断、解码端校验（负数或超限直接抛出 `DecoderException`）。此前解码端的 `Math.min` 截断会留下未读字节、让后续字段错位，负数会抛出 `NegativeArraySizeException`。应用于 `ArmoryConsoleSyncPacket`（64）、`MachineStatusPacket`（4096，发送端超出时自动分包）、`SuitPackets.Threats`（256）、激光塔统计与目标过滤器。
- 新 `PacketRateLimit`（令牌桶，按玩家，退出时清理）：`ManeuverInput`（突发 10，之后每 tick 1 个；此前每个包都会转发给 160 格内的所有玩家）、武装库控制台（突发 10，每秒 5 个）、装备控制台按钮（突发 20，每秒 10 个）。滑块调参保持服务端槽位、参数索引与范围校验，但不经过此令牌桶：正常拖动频率可超过上限，且客户端没有最终值重发机制，直接丢弃会让服务端停在较早档位。
- 确认 47 个载荷全部为 `CustomPacketPayload`，处理器运行在主线程（默认），无可选载荷。

### 数据驱动（数据包）

- 新 `SiTuning` 重载监听器：`data/<命名空间>/si_tuning/<名称>.json`，每个文件为扁平的数值对象。数据包可用同名文件覆盖；文件缺少的键使用代码默认值；超出范围的值被钳制并警告；未知键（多为拼写错误）警告。所有可调值集中声明在 `SiTuningValues`，默认值与内置 JSON 一致（由测试检查）。
- 首批：OV／OD 扫描仪的扫描半径、纵向半径与每次耗电；采矿机各钻头与扫描仪的耗电区间；传送机冷却；武装库展示柜的网络范围。客户端绘制所用的数值（如采矿机工作范围）暂不开放，因为客户端尚无同步。
- 后续候选：处理机器的基础耗电与工时、发电机输出、反应堆参数、特殊机动能耗、多方块（油井、聚变）结构参数。

### 现代版本准备

- 项目已基于 NeoForge 1.21.1：物品数据使用 Data Components（自定义数据统一走 `CustomData`／组件包装），网络全部为 `CustomPacketPayload`，附魔与标签为数据驱动。本版本确认无 `getOrCreateTag` 等旧 NBT 接口。已弃用的 `LevelReader.hasChunkAt` 31 处全部改为 `Level.isLoaded`。核心模块注释中提及内容模块包名会触发架构检查，已改写。

### 测试

- 新 GameTest 类 `Round36GameTests`（8 项）：调参默认值与内置文件一致、调参句柄读取已加载值、计数上下限、机器状态包超限编码后无残留字节、武装库同步包截断展示柜与超长标签、限流突发上限、升级效果不再递归、变压器提示翻译键齐全。

## 源码基线与验证说明

- PR 基于上游 `0.1.7.35-beta` 的 `823b9367e8b045735b2fe74bc1fc832e1b2e6d4e`（0.1.7.35.1），移入交付提交 `eb0f20f41115e5408a528b485c9c59b1c62ee923` 的增量。保留电动锄、蒸汽再加压机、加工配方、热交换机配方及核爆修复。
- 源码 ZIP 的 8,138 个文件与交付 Bundle 提交逐字节一致。ZIP SHA-256：`b6d9ef343ca3e00317a5df76011bf3a89353ee67c7bf58d4d9d470957c72b05d`。
- 本轮额外修正：装备控制台滑块每次鼠标移动都会发包，正常拖动可能超过新增的 20 突发／10 每秒限制，且没有最终值重发机制。因此滑块调参继续使用原有服务端值域校验，不经过令牌桶；按钮、武装库与特殊机动的限流保留。
- 本地使用 Java 21、Gradle 8.10.2、NeoForge 21.1.218 与已有依赖缓存进行离线 `build`，包含 core 架构／分发检查和 addonExample 编译。`test` 为 `NO-SOURCE`，不能等同于运行 GameTest。源码 JSON 4,005 份解析通过，手册 STE 检查 0 问题，Git 差异检查通过。
- 原交付日志记录基础服务端 258 项、兼容服务端 264 项必需测试通过。两组均包含 Round36 的 8 项新增测试；客户端只有 GTM 4 项通过的摘要。上述结果来自交付方，本轮未独立运行 Minecraft，且不包含本次合并与滑块修正。
- 兼容日志使用 AE2 19.2.17、MI 2.4.2、Grand Power 3.0.0；Botania 和 GuideME 是测试 stub，不代表正式 Botania／GuideME 整包兼容验收。
- 原交付报告未绑定被测 JAR 的哈希。交付 JAR SHA-256：`2d4fc5c88d71602bf3c8587552b2e53b9dd355d8a308739bea105db617e98d9d`。本 PR 保留额外上游改动并修正滑块限流，与原交付 JAR 的源码输入不同。
