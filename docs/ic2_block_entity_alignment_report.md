# IC2 方块实体对齐差异报告

> 对比对象：本模组（mio_icif，NeoForge 1.21.1）的 6 台机器方块实体 vs 原版 IC2 1.12.2（build 2.8.221）参考实现
> 机器清单：物品缓冲机、电动分拣机、高级物品分配机、流体分配机、高级流体分配机、流体流量调节机
> 生成日期：2026-07-17

## 目录

- [总体架构差异](#总体架构差异)
- [机器 1：物品缓冲机 ItemBuffer](#机器-1物品缓冲机-itembuffer)
- [机器 2：电动分拣机 SortingMachine](#机器-2电动分拣机-sortingmachine)
- [机器 3：高级物品分配机 WeightedItemDistributor](#机器-3高级物品分配机-weighteditemdistributor)
- [机器 4：流体分配机 FluidDistributor](#机器-4流体分配机-fluiddistributor)
- [机器 5：高级流体分配机 WeightedFluidDistributor](#机器-5高级流体分配机-weightedfluiddistributor)
- [机器 6：流体流量调节机 FluidRegulator](#机器-6流体流量调节机-fluidregulator)
- [修改优先级建议](#修改优先级建议)

---

## 总体架构差异

本模组的 6 台机器分成两种架构：

| 架构 | 机器 | 基类 | IC2 原版对应基类 |
|---|---|---|---|
| A — 自包含（直接继承 BlockEntity） | item_buffer_elc、item_distributor_elc | `BlockEntity` + `ItemStackHandler` | `TileEntityInventory` |
| B — 框架化（继承 mio_icif_producer） | sorter_elc、fluid_distributor_elc、weighted_fluid_distributor_elc、fluid_regulator_elc | `mio_icif_producer` → `mio_icif_Energy_Block` | `TileEntityElectricMachine` / `TileEntityInventory` |

### 基础架构层的关键差异（影响所有 6 台机器）

1. **升级处理方式不同**
   - IC2：缓冲机用 `tick` 标志**交替**处理两个升级槽（每 tick 只跑一个）；分拣机/调节机用 `InvSlotUpgrade.tick()`（每 tick 跑全部）。
   - 本模组：`mio_icif_producer.tick` 里 `recalculateUpgradeStats()` **每 tick 都重算**（无论升降级有无变化），且对 buffer/distributor 类无升级槽的机器不跑升级逻辑。物品缓冲机的交替逻辑已用 `tick` 标志正确复刻。

2. **侧向路由实现差异**
   - IC2：`getSlotsForFace` 返回**所有**槽位索引，再由 `canInsertItem`/`canExtractItem` 按 `Access`+`InvSide` 优先级筛选。
   - 本模组：`getSlotsForDirection(side)` 返回**已筛选**的槽位数组，直接交给 `WorldlyContainer`/capability。语义接近但实现更直接，不会出现 IC2 那种"优先槽满则回退到次选槽"的隐式行为。

3. **markDirty 触发的级联不同**
   - IC2：`markDirty()` → 每个槽位 `onChanged()`（如 `InvSlotUpgrade.onChanged` 重算超频）。
   - 本模组：`setChanged()` 不级联到槽位 `onChanged`，重算靠 `recalculateUpgradeStats()` 在 tick 开头主动调用。功能等价但触发时机不同。

4. **容器物品处理（流体相关机器）**
   - IC2：用 `InvSlotConsumableLiquidByTank` + `processIntoTank`/`processFromTank` 高层封装，自动处理 simulate→execute→移容器到输出槽。
   - 本模组：各流体机器**手写** simulate→fill→移容器的逻辑，重复且容易出 bug（见流体分配机差异）。

---

## 机器 1：物品缓冲机 ItemBuffer

**对比文件**
- 原版：`ic2/core/block/machine/tileentity/TileEntityItemBuffer.java`（159 行）
- 本模组：`mio_icif_item_buffer_elc.java`（236 行）

### 差异表

| 方面 | IC2 原版 | 本模组 | 差异性质 |
|---|---|---|---|
| 基类 | `TileEntityInventory`（无能量） | `BlockEntity` | 架构不同，行为等价 |
| 缓冲槽 | 24（右，SIDE）+ 24（左，NOTSIDE） | 24（右，N/S/E/W）+ 24（左，UP/DOWN） | ⚠️ **侧语义对调** |
| 升级槽 | 2 槽，`Access.NONE` | 2 槽（索引 48/49） | 一致 |
| 升级 tick | 两个都在时**交替**（`tick` 标志）；单个时每 tick 跑 | 完全一致（已用 `tick` 标志复刻） | ✅ 一致 |
| 升级属性 | `ItemProducing` | 无 `getUpgradableProperties` 等价物 | ⚠️ 升级适用性过滤缺失 |
| 能量 | 无能量组件；`getEnergy()` 返回 40.0 桩值；`useEnergy` 恒 true | 无能量 | ✅ 一致 |
| 侧路由 | `rightcontent`=SIDE，`leftcontent`=NOTSIDE | 右=N/S/E/W，左=UP/DOWN | ⚠️ **NOTSIDE 语义未复刻** |
| 客户端同步 | 依赖默认 + setChanged | 不重写 `getUpdateTag` | ⚠️ 升级交替的 `tick` 标志不持久化（与原版一致），但槽位内容同步可能滞后 |
| NBT 键 | `InvSlots`→`rightcontent`/`leftcontent`/`upgrade` | `items`（整个 handler） | ⚠️ 键名不同（不影响功能） |
| GUI | 硬编码 `ContainerItemBuffer`/`GuiItemBuffer` | `ItemBufferMenu` | ✅ 等价 |

### 关键问题 1：左右槽的侧语义对调

**IC2 原版**：
- `rightcontent` → `InvSide.SIDE` = {NORTH, SOUTH, WEST, EAST}（水平四面）
- `leftcontent` → `InvSide.NOTSIDE` = **空集**（不匹配任何面）

配合 IC2 的 `canInsertItem` 优先级算法，这意味着：
- 从水平面插入 → 走 `rightcontent`（因为 `leftcontent` 的 `NOTSIDE` 不匹配，且 `rightcontent` 匹配，`leftcontent` 被"抢占"阻塞）
- 从 UP/DOWN 插入 → 两个槽都不匹配优先级，都允许（回退到默认 true）

**本模组**：
- 右槽 → N/S/E/W（水平面）
- 左槽 → UP/DOWN

本模组的实现是"右槽=水平面、左槽=顶底"，而原版是"右槽=水平面、左槽=任意回退（含顶底，但被右槽抢占时不接）"。**实际效果**：本模组左右槽物理隔离更彻底（顶底只走左槽），原版是水平面只走右槽、顶底两槽都能走。

**影响**：自动化（漏斗）插入行为不同。原版从顶/底插入会同时填左右两排；本模组从顶/底插入只填左排。

**建议对齐**：将本模组 `isSide` 判定改为：右槽=水平面（保持），左槽=`InvSide.NOTSIDE` 等价语义（即所有面都允许插入左槽，但水平面插入时左槽被右槽抢占阻塞）。实现上需要改 `canPlaceItemThroughFace` 的优先级逻辑，而不是简单的侧数组划分。

### 关键问题 2：升级适用性过滤缺失

IC2 的 `getUpgradableProperties()` 返回 `{ItemProducing}`，`InvSlotUpgrade.accepts()` 据此过滤哪些升级可放。本模组 `mio_icif_item_buffer_elc` 没有等价的升级白名单校验，玩家可能放入不适用升级。

**建议**：在升级槽的 `isItemValid` 加上 `ItemProducing` 适用性校验（参考 IC2 升级项的 `isSuitableFor`）。

### 关键问题 3：`tick` 标志未持久化

本模组与原版一致——`tick` 布尔不存 NBT，加载后重置为 `true`。这是**正确**的（原版就是这样）。无需修改。

---

## 机器 2：电动分拣机 SortingMachine

**对比文件**
- 原版：`TileEntitySortingMachine.java`（368 行）
- 本模组：`mio_icif_sorter_elc.java`（297 行）

### 差异表

| 方面 | IC2 原版 | 本模组 | 差异性质 |
|---|---|---|---|
| 基类 | `TileEntityElectricMachine` | `mio_icif_producer` | 架构不同 |
| 能量容量 | 15000 EU | 15000 | ✅ 一致 |
| 能量 tier | 2（MV），变压器升级可提升 | LV（基类默认） | ⚠️ **tier 不一致** |
| 每物品能耗 | 20 EU/物品 | 20 EU/物品 | ✅ 一致 |
| 每 tick 门槛 | `energy ≥ 20` 才工作 | `canWork` 检查 `energy ≥ ENERGY_PER_ITEM` | ✅ 等价 |
| 放电槽 | `InvSlotDischarge` tier 2，`Access.NONE` | BATTERY 槽 | ✅ 等价 |
| 升级槽 | 3 槽，仅 `Transformer` 属性 | 3 槽，过滤 `isTransformerUpgrade` | ✅ 一致 |
| 缓冲槽 | 11 槽，`Access.IO`，`InvSide.ANY` | 11 槽（4..14） | ✅ 一致 |
| 过滤槽 | 6 方向 × 7 = 42 个全息槽 | 6 方向 × 7 = 42 个（独立数组） | ✅ 一致 |
| 默认输出方向 | `defaultRoute`，默认 DOWN，GUI 设置 | `defaultOutputDirection`，默认 DOWN | ✅ 一致 |
| 路由优先级 | `getAdjacentInventories` 排序：个人方块优先，再按容量降序 | 按固定 6 方向顺序（FILTER_DIRECTIONS） | ⚠️ **邻居遍历顺序不同** |
| 过滤匹配 | 同 item+nbt（工具看耐久，普通物品忽略耐久，NBT 忽略部分键） | `isSameItemSameComponents`（严格组件匹配） | ⚠️ **匹配严格度不同** |
| 过滤分支 simulate | 必须整批 `filterSize` 全部塞下才执行 | simulate 后若 >0 即执行 | ⚠️ **塞不下时行为不同** |
| 默认路由分支 | 不 simulate，直接塞 1 个 | simulate 后塞 1 个 | ⚠️ **execute 前是否 simulate 不同** |
| 默认路由分支 | 只塞 1 个就 break | 塞 1 个后 return true（等价 break） | ✅ 等价 |
| 能耗扣除时机 | 成功移动后 `useEnergy(amount*20)` | 成功移动后 `consumeEnergyInternal` | ✅ 等价 |
| active/LIT | **从不** setActive（恒 false） | 无 LIT | ✅ 一致 |
| 网络 | `defaultRoute` 是 networked field；`onNetworkEvent(event 0..5)` 设置方向 | 用 menu/packet 设置方向 | ✅ 等价 |
| NBT | `filters`（list of {index, item}）、`defaultroute`(byte) | `filterStacks`（slot+i 键）、`defaultOutputDir`(byte) | ⚠️ 键名不同 |

### 关键问题 1：tier 不一致

**IC2 原版**：tier 2（MV），且 `setUpgradableBlock()` 把 `energy.setSinkTier(upgradeSlot.getTier(2))`——变压器升级提升输入 tier。
**本模组**：构造时传 `CableTier.LV`，没有随变压器升级动态调整 sink tier 的逻辑。

**影响**：本模组的分拣机只能接 LV 电缆（32 EU/t），原版能接 MV（128 EU/t）且变压器升级后能接 HV+。原版容量 15000 但 LV 输入 32/t 要 469 tick 才能充满——明显设计是给 MV 的。

**建议对齐**：构造 tier 改为 MV（`CableTier.MV`），并在 `recalculateUpgradeStats` 或 `getEffectiveCableTier` 里根据变压器升级数提升 tier（参考 `mio_icif_producer` 已有的 `getEffectiveCableTier` 逻辑）。

### 关键问题 2：邻居遍历顺序不同

**IC2 原版**：`StackUtil.getAdjacentInventories(this)` 返回排序后的列表——**个人方块（personal blocks）优先，再按库存容量降序**。这影响"先试哪个邻居的过滤"。
**本模组**：按 `FILTER_DIRECTIONS = {DOWN, UP, NORTH, SOUTH, WEST, EAST}` 固定顺序遍历。

**影响**：当多个邻居都有匹配过滤时，原版优先填个人机器/大容量容器，本模组按方向固定顺序。**玩家感知**：相同配置下，物品流向的邻居可能不同。

**建议对齐**：实现邻居排序——优先级：① IC2 个人方块（本模组若有等价概念）② 按 `IItemHandler` 可用槽位数降序。至少做到按容量降序，更接近原版体验。

### 关键问题 3：过滤匹配严格度不同

**IC2 原版** `StackUtil.checkItemEquality(filter, stack)`：
- 同 `Item`
- 若 `isDamageable()`：耐久必须**完全相等**
- 否则（普通物品）：**忽略耐久**
- NBT：`checkNbtEquality` 忽略部分键（如 repair cost）

**本模组** `ItemStack.isSameItemSameComponents`：组件必须完全一致（含耐久、附魔、所有 data component）。

**影响**：原版一个过滤槽（如"铁剑"）能匹配任何耐久的铁剑；本模组只能匹配耐久完全相同的铁剑。普通物品（不 damageable）原版忽略耐久，本模组严格匹配。

**建议对齐**：自实现匹配函数：同 item；damageable 物品要求耐久相等（或按配置忽略）；普通物品忽略耐久；NBT 忽略 repair cost 等 IC2 忽略的键。

### 关键问题 4：过滤分支塞不下时的行为

**IC2 原版**：simulate 后**必须 `amount == filterSize`**（整批全塞下）才执行。塞不下则 `break`（跳过该邻居，不再试该邻居的其他过滤）。
**本模组**：simulate 后若 `>0`（塞下任意数量）即执行塞入的实际数量。

**影响**：原版严格要求"过滤指定的数量必须一次性全塞下"——这保证了"按过滤批量路由"的语义。本模组部分塞入会打乱批量路由的预期。

**建议对齐**：过滤分支改为"simulate 整批，仅当全部塞下才 execute 整批"。

### 关键问题 5：默认路由分支是否 simulate

**IC2 原版**：默认路由分支**不 simulate**，直接 `putInInventory(..., false)` 塞 1 个（这是原版代码的写法，可能是个小 bug 但属于"原版行为"）。
**本模组**：先 simulate 再 execute。

**影响**：极小。simulate 更安全。**建议**：可保留本模组的 simulate（更稳健），或严格对齐原版不 simulate。倾向保留 simulate（无害且更安全）。

---

## 机器 3：高级物品分配机 WeightedItemDistributor

**对比文件**
- 原版：`TileEntityWeightedItemDistributor.java`（171 行）
- 本模组：`mio_icif_item_distributor_elc.java`（212 行）

### 差异表

| 方面 | IC2 原版 | 本模组 | 差异性质 |
|---|---|---|---|
| 基类 | `TileEntityInventory`（无能量） | `BlockEntity` | 架构不同 |
| 接口 | `IWeightedDistributor`（含 `getPriority`/`updatePriority`） | 自实现 priority + `ContainerData` | ✅ 等价 |
| 能量 | **无** | 无 | ✅ 一致 |
| 缓冲槽 | 9 槽，`Access.I`（仅输入，不可外部抽取） | 9 槽 | ✅ 一致 |
| 外部抽取 | **禁止**（`Access.I` → `canOutput()=false`） | `DistributorSidedItemHandler.extractItem` 恒返回 EMPTY | ✅ 一致 |
| 升级槽 | **无** | 无 | ✅ 一致 |
| priority | `ArrayList<EnumFacing>(5)`，`@ClientModifiable` | `List<Direction>` + `ContainerData` 同步 | ✅ 等价 |
| facing 排除 | `updateConnectivity` 从 priority 移除自身 facing | `setPriority` 移除 facing | ✅ 一致 |
| 分配语义 | **严格优先级**：饱和 priority[0] 再 [1]… | **严格优先级**：每 tick 只服务一个方向 | ⚠️ **饱和策略不同** |
| 每 tick 移动量 | 一个方向内**遍历全部 9 槽**，每槽塞整堆 | 一个方向内遍历 9 槽，每槽塞 1 个 | ⚠️ **移动量不同** |
| break 时机 | `hasChanged && empty`（该方向拿走东西且缓冲全空）才 break | 该方向拿走任意东西就 break | ⚠️ **break 时机不同** |
| markDirty | 移动后 `func_70296_d()` | `setChanged` + `sendBlockUpdated` | ✅ 等价 |
| NBT | `priority` int[]（非空才写） | `priority` int[] + `inventory` | ✅ 等价 |
| active/LIT | 从不 setActive | 无 LIT | ✅ 一致 |

### 关键问题 1：分配饱和策略不同（重要）

**IC2 原版** `updateEntityServer`：
```
for (facing in priority):           // 按优先级顺序
    for (slot in 0..8):             // 遍历全部 9 槽
        if 非空:
            simulate 塞整堆 → 若 >0:
                execute 塞整堆
                缩减缓冲
    if (hasChanged && buffer全空): break   // 该方向吃饱且缓冲空才换下个方向
```
语义：**优先级 [0] 先吃饱（吃掉它能吃的全部），吃不完的才轮到 [1]**。一个方向内每槽都尝试塞**整堆**。

**本模组** `tick`：
```
for (dir in priority):
    for (slot in 0..8):
        if 非空:
            simulate 塞 1 个 → 若 EMPTY remainder:
                execute 塞 1 个
    if 该方向移动了任何东西: break   // 该方向拿走 1 个就换下个方向（下一 tick 再来）
```
语义：**每 tick 每个方向最多拿走每槽 1 个**，且一个方向拿走东西就停，不再服务其他方向。这是"轮询每 tick 推一点"而非"饱和优先"。

**影响**：吞吐量差异巨大。原版 9 槽每槽 64 个 = 576 物品可在一个 tick 内全塞给 priority[0]；本模组每 tick 最多推 1 个。玩家体验：本模组分发极慢。

**建议对齐**：改为"每槽塞整堆 + `hasChanged && buffer空` 才 break"。

### 关键问题 2：每槽移动量不同

原版每槽塞**整堆**（copy 整个 stack，simulate 整堆，execute 整堆）。本模组每槽塞 **1 个**。

**建议对齐**：每槽 simulate+execute 整堆（`copyWithSize(stack.count)`），与原版一致。

### 关键问题 3：break 时机不同

原版：`if (hasChanged && empty) break;`——只有当**该方向拿走东西且整个缓冲全空**才停止遍历后续 priority。否则继续试下一个 priority 方向（继续往别的方向塞）。
本模组：该方向拿走任意东西就 break。

**影响**：原版在缓冲未空时会**继续向后续优先级方向塞**（直到缓冲空或 priority 耗尽）；本模组拿走 1 个就停。

**建议对齐**：改为 `if (hasChanged && buffer.isEmpty()) break;`，让未空缓冲继续服务后续 priority 方向。

---

## 机器 4：流体分配机 FluidDistributor

**对比文件**
- 原版：`TileEntityFluidDistributor.java`（198 行）
- 本模组：`mio_icif_fluid_distributor_elc.java`（294 行）

### 差异表

| 方面 | IC2 原版 | 本模组 | 差异性质 |
|---|---|---|---|
| 基类 | `TileEntityInventory`（无能量） | `mio_icif_producer`（有能量框架但容量 0） | ⚠️ 架构冗余 |
| 能量 | **无** | 容量 0/maxReceive 0/maxExtract 0 | ✅ 等价（实际无能量） |
| 液罐 | 1000 mB | 1000 mB | ✅ 一致 |
| 输入槽 | `InvSlotConsumableLiquidByTank` OpType.**Fill**（罐→物品），InvSide.BOTTOM | FLUID_INPUT 槽 | ⚠️ **OpType 语义不同** |
| 输出槽 | `InvSlotOutput` 1 槽 | FLUID_OUTPUT 槽 1 | ✅ 一致 |
| 容器处理 | `processFromTank`（罐→物品，每 tick） | `processInputSlot` 手写双向 | ⚠️ **方向不同** |
| 模式 | `getActive()` 切换：true=单边推 facing；false=公平分配 | `active` 切换：true=单边；false=分配 | ✅ 一致 |
| 输入侧 | active: 全部非facing；inactive: 仅facing | 一致（`isInputSide`） | ✅ 一致 |
| 公平分配算法 | 多轮：每轮 `min(acceptedVolume, tankAmount)/numAcceptors`，饱和的邻居移除 | 一致（多轮 fair-share） | ✅ 一致 |
| 单边推 | 直接 `fillTile(facing, false)` 不 simulate | simulate 后 execute | ⚠️ simulate 与否不同 |
| active/LIT | `setActive` 切换 + 重算连通性 | `toggleMode` 切换 + 更新 LIT blockstate | ✅ 等价 |
| 连通性重算 | `setActive`/`setFacing` 调 `updateConnectivity` | `toggleMode`/tick 同步 LIT | ✅ 等价 |
| onNetworkEvent | `setActive(!getActive())`（单一事件） | menu/packet | ✅ 等价 |
| NBT | 无自定义键 | `active` + tank | ⚠️ 本模组多存 active（原版 active 走 blockstate） |

### 关键问题 1：输入槽 OpType 方向不同（重要）

**IC2 原版**：`inputSlot` = `OpType.Fill`，配合 `processFromTank`——**从罐里把流体装进容器**（空桶→满桶，输出到 OutputSlot）。罐由管道填充，容器是"取流体的工具"。

**本模组** `processInputSlot`：**双向**——罐有流体时装容器（tank→item）；罐空时从容器抽流体进罐（item→tank）。

**影响**：原版流体分配机的"输入槽"实际是**流体输出口**（用容器取流体），罐由管道注入。本模组让它双向，意味着玩家可以放满桶进去填罐——这是原版没有的行为。

**建议对齐**：明确输入槽语义为 `OpType.Fill`（仅罐→物品）。若要保留双向，需确认这是设计意图而非 bug。倾向对齐原版：仅 `processFromTank`。

### 关键问题 2：单边推送是否 simulate

**IC2 原版**：单边模式直接 `fillTile(target, side, fluid, false)`（不 simulate，直接塞，返回实际塞入量）。
**本模组**：先 simulate 再 execute。

**影响**：极小。simulate 更安全。**建议**：可保留 simulate（无害），或严格对齐。倾向保留。

### 关键问题 3：基类冗余

本模组 `mio_icif_fluid_distributor_elc` 继承 `mio_icif_producer`（带完整能量/升级框架），但容量/maxReceive/maxExtract 全 0，且无升级槽、无电池槽。框架的 `handleBatterySlot`/`handleAutomationUpgrades`/`recalculateUpgradeStats` 每 tick 空跑。

**影响**：性能浪费（每 tick 跑无用逻辑），且 `canWorkRedstone` 红石门控可能意外阻止工作。

**建议**：考虑让流体分配机不继承 `mio_icif_producer`，改用更轻的基类（类似 item_distributor_elc 的自包含方式）。或保持现状但确保红石门控不误锁。**优先级低**。

---

## 机器 5：高级流体分配机 WeightedFluidDistributor

**对比文件**
- 原版：`TileEntityWeightedFluidDistributor.java`（146 行）
- 本模组：`mio_icif_weighted_fluid_distributor_elc.java`（252 行）

### 差异表

| 方面 | IC2 原版 | 本模组 | 差异性质 |
|---|---|---|---|
| 基类 | `TileEntityFluidDistributor` | `mio_icif_producer` | ⚠️ 架构不同 |
| 能量 | 无 | 容量 0 | ✅ 等价 |
| 液罐 | 1000 mB（继承） | **8000 mB** | ⚠️ **容量不同** |
| 输入槽 | 继承 `OpType.Fill`/BOTTOM | INPUT 槽（非 FLUID_INPUT） | ⚠️ 槽类型不同 |
| 输出槽 | 继承 | OUTPUT 槽 | ⚠️ 槽类型不同 |
| priority | `List<EnumFacing>(5)`，`@ClientModifiable` | `List<Direction>` | ✅ 等价 |
| facing 排除 | `updateConnectivity` 移除 facing | `setPriority` 移除 facing | ✅ 一致 |
| 连通性 | 输入=仅facing，输出=空集（固定，忽略 active） | facing 输入，priority 输出 | ✅ 等价 |
| 分配语义 | **严格优先级**：priority[0] 吃饱再 [1]… | **严格优先级**：顺序遍历，塞满一个再下一个 | ✅ 一致 |
| 容器处理 | 继承 `processFromTank`（罐→物品） | `processFluidContainer` 双向 | ⚠️ **方向不同** |
| 移动量 | `fillTile(target, side, tank.getFluid(), false)`（整罐推） | simulate 整罐后 execute | ⚠️ simulate 与否不同 |
| break | `tankAmount <= 0` 才 break | `didWork` 后更新 LIT，break on tank 空 | ✅ 等价 |
| active/LIT | 不被 GUI 切换（事件编辑 priority）；连通性忽略 active | LIT 反映 didWork | ⚠️ **LIT 语义不同** |
| onNetworkEvent | `position=event/10, facing=byIndex(event%10 & 6)` 编辑 priority | menu/packet | ✅ 等价（编码不同） |
| NBT | `priority` int[] | tank + priority | ⚠️ 本模组多存 tank（原版 tank 走 Fluids 组件） |

### 关键问题 1：液罐容量不同（重要）

**IC2 原版**：继承父类 `TileEntityFluidDistributor` 的 **1000 mB** 罐。
**本模组**：**8000 mB**（8 倍）。

**影响**：本模组高级流体分配机能存 8 倍流体，缓冲能力远超原版。

**建议对齐**：改为 1000 mB。若设计意图是"高级版更大容量"则需明确这是有意偏离原版。倾向对齐原版 1000 mB。

### 关键问题 2：LIT/active 语义不同

**IC2 原版**：`active` 字段**不被** WeightedFluidDistributor 的 GUI 事件切换（事件只编辑 priority），且 `updateConnectivity` **忽略** `getActive()`（连通性固定=facing 输入）。`active` 实际是父类遗留的死状态。
**本模组**：`didWork`（本 tick 是否推过流体）驱动 LIT blockstate。

**影响**：本模组的 LIT 是"工作指示灯"（推流体时亮），原版无此指示。这是**本模组的增强**，偏离原版但更友好。

**建议**：保留本模组的 LIT 工作指示（增强体验），但**确保不因此触发连通性重算**（原版连通性固定）。当前实现已满足。无需改。

### 关键问题 3：容器处理方向不同

同机器 4。原版仅 `processFromTank`（罐→物品）；本模组双向。**建议对齐**为单向 `processFromTank`。

### 关键问题 4：槽类型不同

原版用 `InvSlotConsumableLiquidByTank`（FLUID_INPUT 语义）+ `InvSlotOutput`。本模组 weighted 版用 `INPUT`/`OUTPUT` 类型槽（非 `FLUID_INPUT`/`FLUID_OUTPUT`），靠 `isItemValidForSlot` 的 capability 检查过滤流体容器。

**影响**：功能等价，但槽类型不一致可能导致自动化（漏斗）行为细微差异（`INPUT` 槽默认接受非流体物品被 validator 拒绝；`FLUID_INPUT` 槽在 `MachineItemHandler` 层就限制）。

**建议对齐**：改用 `FLUID_INPUT`/`FLUID_OUTPUT` 槽类型，与普通流体分配机一致。

---

## 机器 6：流体流量调节机 FluidRegulator

**对比文件**
- 原版：`TileEntityFluidRegulator.java`（187 行）
- 本模组：`mio_icif_fluid_regulator_elc.java`（237 行）

### 差异表

| 方面 | IC2 原版 | 本模组 | 差异性质 |
|---|---|---|---|
| 基类 | `TileEntityElectricMachine` | `mio_icif_producer` | 架构不同 |
| 能量容量 | 10000 EU | 10000 | ✅ 一致 |
| 能量 tier | **4（IV）** | **EV** | ⚠️ **tier 命名/映射不同** |
| 每操作能耗 | 10 EU/操作 | 10 EU/操作 | ✅ 一致 |
| 液罐 | 10000 mB，`Access.NONE` | 10000 mB | ✅ 一致 |
| 输入槽 | `InvSlotConsumableLiquidByTank` OpType.**Drain**（物品→罐），InvSide.TOP | FLUID_INPUT 槽，drain 方向 | ✅ 一致 |
| 输出槽 | `InvSlotOutput` 1 槽 | FLUID_OUTPUT 槽 | ✅ 一致 |
| 容器处理 | `processIntoTank`（物品→罐，每 tick） | `processInputSlot`（drain 方向） | ✅ 一致 |
| 流量字段 | `outputmb`，GUI 增量调整，钳制 [0,1000] | `outputmb`，`onNetworkEvent` 增量调整，钳制 [0,1000] | ✅ 一致 |
| 模式 | mode 0=每秒(20 tick)，mode 1=每 tick | 一致 | ✅ 一致 |
| tick 门槛 | `updateTicker++ % 20 != 0 && mode==0` 才跳过 | 一致 | ✅ 一致 |
| 随机起始 | `IC2.random.nextInt(20)` | `(int)(Math.random()*20)` | ✅ 等价 |
| work() | 检查 outputmb/能量/罐；simulate drain→fill；execute drain(outputmb)+useEnergy(10) | 一致 | ✅ 一致 |
| ⚠️ drain 量 | `drainInternal(outputmb, true)`（按配置量 drain，**不管实际塞入量**） | `drain` 实际塞入量 | ⚠️ **drain 量不同** |
| active/LIT | `setActive(newActive)`（work 结果） | 更新 LIT 为 newActive | ✅ 一致 |
| 输入侧 | 全部非facing | 全部非facing | ✅ 一致 |
| 输出侧 | facing（仅 work 推送） | facing（仅 work 推送） | ✅ 一致 |
| onNetworkEvent | 1001→tick 模式，1002→sec 模式，else `outputmb+=event` | 一致（事件编码相同） | ✅ 一致 |
| NBT | `outputmb`(int)、`mode`(int) | `outputmb`、`mode`、tank | ⚠️ 本模组多存 tank |
| 网络 | `fluidTank` @GuiSynced；mode/outputmb 走 NBT 同步 | getUpdateTag 同步全部 | ✅ 等价 |

### 关键问题 1：drain 量不同（重要 bug）

**IC2 原版** `work()`：
```java
int amount = fillTile(te, side, fluidTank.drainInternal(outputmb, false), false);  // simulate drain + real fill
if (amount > 0) {
    fluidTank.drainInternal(outputmb, true);  // ⚠️ REAL drain 用 outputmb（配置量），不是 amount（实际塞入量）
    energy.useEnergy(10.0);
    return true;
}
```
原版**按配置量 outputmb 从罐里抽**，即使目标只接受了 `amount < outputmb`——**差额被虚空销毁**。这是原版的已知 bug/quirk。

**本模组**：按实际塞入量 `amount` drain（`drain(amount)` 语义）。

**影响**：本模组**不销毁流体**（更正确），原版**销毁差额**。两者行为不同。

**建议**：这是个判断题——
- 严格对齐原版：改为 `drain(outputmb, true)`（会销毁流体，复制原版 bug）
- 修正 bug：保留本模组的 `drain(amount)`（更合理）

**倾向保留本模组修正版**（不销毁流体），但在报告中标注这是与原版的偏差。需用户决定。

### 关键问题 2：tier 命名/映射

**IC2 原版**：`super(10000, 4)`——tier 4。IC2 的 tier 4 = IV（极高电压）。
**本模组**：`CableTier.EV`。

需确认本模组的 `CableTier` 枚举映射：若 `EV` 对应 IC2 的 tier 4（IV），则一致；若 `EV` 是 tier 3 而 `IV` 是 tier 4，则不一致。

**建议**：核对 `CableTier` 枚举与 IC2 tier 的映射，确保调节机用 tier 4（IV）对应值。

### 关键问题 3：NBT 多存 tank

原版 tank 走 `Fluids` 组件的 NBT（`@GuiSynced`）；本模组直接在 `saveAdditional` 存 tank。功能等价，键名不同。无需改。

---

## 修改优先级建议

按"对玩家体验影响 × 对齐原版重要性"排序：

### 高优先级（行为明显偏离原版，影响游戏性）

1. **高级物品分配机 — 饱和策略**（机器 3 关键问题 1/2/3）
   - 改为每槽塞整堆 + `hasChanged && buffer空` 才 break
   - 影响：吞吐量从"每 tick 1 个"提升到"每 tick 整堆"，与原版一致

2. **分拣机 — tier 不一致**（机器 2 关键问题 1）
   - 改为 MV（tier 2）+ 变压器升级动态提升
   - 影响：能量输入能力对齐原版

3. **高级流体分配机 — 液罐容量**（机器 5 关键问题 1）
   - 8000 mB → 1000 mB
   - 影响：缓冲能力对齐原版（除非有意增强）

### 中优先级（行为偏离，影响自动化细节）

4. **物品缓冲机 — 左右槽侧语义**（机器 1 关键问题 1）
   - 复刻 NOTSIDE 语义（左槽所有面可入但被右槽抢占）
   - 影响：漏斗插入行为对齐

5. **分拣机 — 过滤匹配严格度**（机器 2 关键问题 3）
   - 自实现 IC2 风格匹配（damageable 看耐久、普通忽略耐久、NBT 忽略部分键）
   - 影响：过滤匹配范围对齐

6. **分拣机 — 过滤分支塞不下行为**（机器 2 关键问题 4）
   - 改为整批全塞下才执行
   - 影响：批量路由语义对齐

7. **分拣机 — 邻居遍历顺序**（机器 2 关键问题 2）
   - 按容量降序排序邻居
   - 影响：多邻居时流向对齐

### 低优先级（细节差异或本模组增强）

8. **流体分配机 — 输入槽 OpType 方向**（机器 4 关键问题 1）
   - 改为单向 `processFromTank`（需确认设计意图）

9. **高级流体分配机 — 槽类型**（机器 5 关键问题 4）
   - INPUT/OUTPUT → FLUID_INPUT/FLUID_OUTPUT

10. **高级流体分配机 — 容器处理方向**（机器 5 关键问题 3）
    - 改为单向（同上）

11. **物品缓冲机 — 升级适用性过滤**（机器 1 关键问题 2）
    - 加 ItemProducing 适用性校验

### 需用户决策（原版 bug，对齐与否）

12. **流体流量调节机 — drain 量**（机器 6 关键问题 1）
    - 原版销毁差额（bug）；本模组修正不销毁
    - 建议：保留修正版（不销毁），但需用户确认是否严格对齐原版 bug

13. **分拣机 — 默认路由 simulate 与否**（机器 2 关键问题 5）
    - 原版不 simulate；本模组 simulate
    - 建议：保留本模组 simulate（更安全）

14. **流体分配机 — 单边推送 simulate 与否**（机器 4 关键问题 2）
    - 同上，保留 simulate

---

## 附：架构层面可选优化（非对齐项，供参考）

- **流体分配机基类冗余**：`mio_icif_fluid_distributor_elc` 继承 `mio_icif_producer` 但不用能量/升级框架，可考虑轻量化基类。
- **容器处理统一**：4 台流体相关机器各自手写 simulate→fill→移容器逻辑，可抽取公共方法（类似 IC2 的 `processIntoTank`/`processFromTank`）。
- **侧路由统一**：本模组 `getSlotsForDirection` 已筛选 vs IC2 `getSlotsForFace` 全返回+canInsert 筛选，功能等价但实现不同，无需强制统一。
