# 0.1.10.16 更新说明：服务器负载、潜在缺陷、堆叠上限的限定、现代化与日语翻译补全

基于 0.1.10.15（GitHub 分支 `0.1.10.15-beta`，提交 579cfd4）。共 13 项，按优先级排列：服务器负载与潜在缺陷 → 安全化与现代化 → 便利性与翻译。构建环境为 Gradle 8.10.2、JDK 21、NeoForge 21.1.218，离线构建。Minecraft 版本仍为 1.21.1。

本版的方针：

* 不重命名现有类（`mio_icif_*` 等），只有新代码遵循命名约定。
* 保留所有迁移代码，旧存档照常读取。
* 配方查看器集中支持 JEI 与 EMI，不再维护 REI。
* 不新增 CI，继续本地构建。

## 1. 服务器负载与潜在缺陷

### 1.1 电网每 tick 的记账表（E-3）

* 独立 SI 电网（`IndependentSiEnergy` 的 `WorldGrid`）每 tick 维护 6 张按方块位置记账的表：数据包扣费 `packetSpent`、变压器接收 `transformerReceived`、本 tick 接收 `receivedThisTick`、中继供电 `relayFeeds`、输出失败 `failedOutputs`、本 tick 导出 `exportedThisTick`。
* 此前以 `BlockPos` 为键的 `HashMap`／`HashSet`：每个位置每 tick 分配一个节点对象，并计算 `BlockPos` 的哈希。
* 现在改为以 `BlockPos.asLong()` 为键的 fastutil 开放寻址表（`Long2ObjectOpenHashMap`、`LongOpenHashSet`，随 Minecraft 自带），不再分配节点。14 处调用全部改为 long 键。
* 这些表只做查找与合并、从不遍历，因此结算顺序与结果不变。

### 1.2 菜单数据槽超过 16 位（石油钻机面板）

* 原版的容器数据槽按有符号 16 位同步，超过 32,767 的值在客户端回绕。
* 基础菜单 `mio_icif_base_menu` 早已把每个值拆成两个 16 位数据槽。本版把这段逻辑提取为 `common.menu.base.WideDataSlots`，基础菜单改用它，行为不变。
* 石油钻机面板 `OilRigPanelMenu` 不继承基础菜单，此前按原版同步钻探坐标：钻机距原点超过 32,767 格时，面板上的 X／Z 坐标显示错误。现在同样使用 `WideDataSlots`。

### 1.3 机器物品中的冷却液注入器储备

* 用扳手拆下的机器把库存保存在物品的方块实体数据中。把机器物品用作合成材料时，`MachineContents` 退还其中的物品；只保留设置的副本（`MachineItemData`）删除同样的键。
* 0.1.10.14 的反应堆冷却液注入器把放不下的旧库存存为储备 `rci_reserve`。`MachineItemData` 会删除这个键，`MachineContents` 却不读取它，因此用作合成材料时储备物品丢失。现在 `MachineContents` 也读取 `rci_reserve`（物品堆列表）并退还。

## 2. 安全化与现代化

### 2.1 堆叠上限只作用于奇点迭代

此前（至 0.1.10.15）有 3 个全局 Mixin：

| Mixin | 作用 | 本版 |
|---|---|---|
| `ContainerMaxStackMixin` | 所有默认上限为 99 的容器报告 9,999。任何模组中 `max_stack_size` 超过 99 的物品（数据包、命令或其他模组设置）都能在所有容器中堆到该数量 | 删除 |
| `DataComponentMaxStackMixin` | `max_stack_size` 组件的存档编解码范围放宽到 0～9,999（原版 1～99） | 保留，只用于存取 |
| `ItemStackMaxStackMixin` | 物品数量的存档编解码范围放宽到 0～9,999（原版 1～99） | 保留，只用于存取 |

新的规则：

* 新接口 `common.item.LargeStackItem`（`largeStackLimit()`）与工具类 `common.item.LargeStacks`。目前只有工业货币实现它（9,999）。
* 两个限定 Mixin：`LargeStackContainerMixin`（`Container#getMaxStackSize(ItemStack)`）与 `LargeStackSlotMixin`（`Slot#getMaxStackSize(ItemStack)`）。只有同时满足以下条件时才返回 9,999：物品是大堆叠物品；容器或槽位使用原版默认上限 99；容器能保存大堆叠。有自身上限的容器与槽位保持其上限。其他所有物品恢复原版规则。
* “能保存大堆叠”的容器（`LargeStacks.keepsLargeStacks`）：原版容器（玩家背包、箱子、木桶、潜影盒、末影箱、漏斗等）与实现新标记接口 `common.item.LargeStackContainer` 的奇点迭代容器（目前为储物箱）。对 NeoForge 21.1.218 中原版类的字节码检查：声明 `setItem` 的 16 个容器类中，没有一个按无参上限 `getMaxStackSize()` 截断（5 个按单个物品的上限截断，11 个不截断）。
* 其他模组的容器与本模组的机器中，货币每格最多 99 个。原因：这些容器的 `setItem` 可能按无参上限（现在为 99）截断，若槽位允许放入 9,999 个，超出 99 的部分会被删除。0.1.10.15 时存入的大堆叠照常读取，也可以照常取出。
* 储物箱的 `setItem` 此前按无参上限截断。删除全局 Mixin 后，这会把放入的 9,999 个货币截成 99 个、删除其余部分。现在改按单个物品的上限截断。
* 货币在玩家背包、储物箱、箱子等容器中照常堆到 9,999，期货机的交易（在玩家背包中进行）不受影响。
* 两个编解码范围保留，只用于存取：任何容器中 9,999 个货币的堆叠照常保存与读取；旧存档中 `max_stack_size` 超过 99 的物品照常读取（没有这个范围，原版编解码器会丢弃整个物品堆）。它们不会让任何物品在容器中堆得更多。
* Mixin 设为可选（`require = 0`），目标变化时不会阻止游戏启动。启动时自检：货币在普通容器与槽位中应得到 9,999、石头保持 64；不符时在日志中记录错误。
* 变化：漏斗、管道等经由原版物品处理器包装（`InvWrapper`，读取容器的无参上限 99）放入时，货币每格最多 99 个（此前 9,999）。手动放入不受影响。

### 2.2 JEI 子类型解释器

* JEI 19.21 把 `IIngredientSubtypeInterpreter` 标记为将被移除。充电物品（电池、能量水晶、电动工具与护甲、带电的储电方块）的 5 个子类型解释器改用 `ISubtypeInterpreter`（`getSubtypeData` 与 `getLegacyStringSubtypeInfo`）。
* 子类型文本与此前相同（空文本时返回 null），JEI 中的显示与配方的缺料判定不变。

### 2.3 移除 REI 插件

* 删除 `common.client.integration.rei`（27 个文件）与服务声明 `META-INF/services/me.shedaniel.rei.api.client.plugins.REIClientPlugin`。
* `build.gradle` 删除 Shedaniel 与 Architectury 仓库及 REI、Architectury 的编译依赖；`gradle.properties` 删除 `rei_version`；`neoforge.mods.toml` 删除对 `roughlyenoughitems` 的可选依赖。
* JEI 插件与 EMI 原生插件（13 个配方类别）保留。

### 2.4 事件订阅

* 16 个类的 `@EventBusSubscriber` 去掉已弃用的 `bus = EventBusSubscriber.Bus.MOD` 参数。FML 4 按事件类型（`IModBusEvent`）自动选择总线，并忽略该参数，因此行为不变。

### 2.5 日志

* 能力注册（`mio_icif_capacities`）的 122 条 INFO 日志改为 DEBUG，注册结束时输出一行汇总。
* 客户端初始化（`Singularity_Iteration_Client`）的 38 条 INFO 日志改为 DEBUG；删除记录玩家名称的一行。

### 2.6 不再吞掉的异常

以下位置此前用空的 `catch` 吞掉异常，问题无从排查。现改用 `core.runtime.diagnostics.QuietFailures`：首次带堆栈记录警告，之后同一类失败每分钟最多一条（附累计次数），功能照常继续。

* AE2 直连桥接 `AE2DirectBridge`（5 处）、AE2 能量存储查询 `AE2Compat`。
* 斯特林发电机从相邻方块取热。
* JEI 的 Curios 子类型注册、创造模式物品栏中的 Curios 物品。

## 3. 便利性与翻译

### 3.1 日语翻译补全

* 补上 1,852 条日语文本：机器、发电机、电缆、变压器、原子炉、物品与材料、界面、提示、消息、配置、Jade、JEI／EMI 类别、字幕、死亡消息等。`ja_jp.json` 现有 3,567 条，与英语一致，无缺失、无失效键，占位符（`%s`、`%1$s`、`%d`、`%%`）全部与英语一致。用语与现有日语译文（原子炉事故调查、配置、模块等）保持一致。
* 英语补上 33 个名称（此前英语界面显示翻译键）：钛与铌的矿石、原矿块、金属块、锭、板、致密板、外壳、粉、小撮粉、粉碎矿石、纯净的粉碎矿石，钛制储物箱（方块与界面标题），含树脂的橡胶木、墙上的橡胶木告示牌与悬挂告示牌、过热蒸汽（旧键）、未知作物。
* 中文补 6 条（上述 5 个非金属名称与变压器界面的“模式”）。俄语补 7 条（同 5 个名称，以及两条模块提示：此前按键写死为 “V/B”，现在显示玩家设置的按键），删除俄语 1 条失效键（`item.mio_icif.lathing_tool.tooltip.damage`）。俄语仍有 479 条未翻译，显示英语。
* 新工具 `tools/lang_1116.py`：应用 `tools/lang_data/lang_1116.json`，然后对每种语言检查缺失键、失效键与占位符（按参数序号比较，`%2$s … %1$s` 与 `%s … %s` 视为一致）。日语缺键或本工具写入的文本占位符不一致时返回错误。保持各文件原有的换行符。

### 3.2 日语手册的页面标题

* 机器页面由 `gen_guide.py` 生成，标题取自语言文件；日语名称缺失时退回英语。日语手册 197 页中有 156 页的标题仍是英语（正文为日语）。
* 新工具 `tools/guide_1116.py`：日语页面的英语标题（导航标题与一级标题）改为日语名称，共 150 页。其余 6 页的名称本身就是英文缩写或专有名称（CESU、EESU、LESU、MFE、MFSU、FCS HUD）。
* 英语手册中超导合金导线页面的标题此前显示为 `block_superconducting_cable`（生成时英语名称缺失），现为 “Superconducting Cable”。

### 3.3 完整构建

* 0.1.10.14／0.1.10.15 新增的 5 个 GameTest 文件（`MachineApiRecipesGameTests`、`MachineConversionGameTests`、`OilRigAndFabricatorGameTests`、`PlayerFeedbackGameTests`、`PowerUnitRecipeGameTests`）以字面量写出 `com.miophas.singularity_iteration.common.` 包名。`checkCoreArchitecture` 禁止示例代码依赖 common 包，因此 0.1.10.15 的 `build` 任务失败（`compileJava` 与 jar 任务本身不受影响）。改为拼接字符串，与其他 GameTest 相同。

## 存档与兼容

* 没有新增或删除的注册项；数据组件不变；公开 API（`mio_icif-api`、`singularity-iteration-core`）不变。
* 所有迁移代码保留。货币堆叠与 `max_stack_size` 超过 99 的旧物品照常读取。
* 新增类：`common.item.LargeStackItem`、`common.item.LargeStackContainer`、`common.item.LargeStacks`、`common.menu.base.WideDataSlots`、`common.mixin.LargeStackContainerMixin`、`common.mixin.LargeStackSlotMixin`（都不属于 API）。
* 不再安装 REI 插件。

## 未包含（后续）

* 机器 tick 调度器（按需唤醒，E-1）。
* 方块实体同步瘦身（约 17 个方块实体的 `getUpdateTag` 发送完整存档数据，E-2）。
* 空值注解（JSpecify）、内部已弃用 API 的迁移、数据生成（datagen）。
* 俄语剩余 479 条、日语手册正文中英语物品名称的链接文字。

## 验证

* Gradle 离线完整构建：结果见交付包 `test-reports/build.txt`。
* 检查 `check_1116.py`（API 与存档、电网 long 键、宽数据槽、堆叠 Mixin、JEI、REI 已移除、事件订阅、日志、静默失败、语言文件、手册、换行符、文档、版本、GameTest 字面量）；离线测试 `Harness1116`（宽数据槽的取值往返）；`tools/lang_1116.py --check`。
* 回归检查 check_1100～check_1113 等。
* 已编写并编译、本机无法运行：`Round1116GameTests`（6 项：货币与其他物品的堆叠上限（含其他模组风格的容器）、储物箱中放入 9,999 个货币、冷却液注入器储备的退还、货币与旧物品的存取、宽数据槽、石油钻机面板）。
* 未在游戏内确认：Mixin 的实际生效（启动日志中的自检）、JEI 与 EMI 的显示、日语界面的实际排版。
