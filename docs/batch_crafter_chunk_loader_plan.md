# 批量工作台 & 区块加载器 重构计划

> 基于 IC2 1.12.2 源码分析，重构到 1.21.1 NeoForge (mio_icif 项目)

---

## 一、批量工作台（Batch Crafter）

### 1.1 原版IC2行为总结

批量工作台是一台**电力自动合成台**：
- 玩家在 3×3 **全息合成网格**中摆放配方图案（不消耗物品）
- 机器从 9 个**原料输入槽**自动消耗对应材料
- 合成产物输出到**产物输出槽**，容器剩余物输出到**容器输出槽**
- 每次合成消耗 2 EU/t，工作 40 ticks 完成
- 支持超频/加速升级，可每tick完成多次合成

### 1.2 原版参数对齐

| 参数 | IC2 原版值 | 说明 |
|------|-----------|------|
| defaultTier | 1 (MV) | 电压等级 |
| defaultEnergyConsume | 2 EU/t | 每tick耗电量 |
| defaultOperationLength | 40 ticks | 基础工作时间 |
| defaultEnergyStorage | 20000 EU | 内部能量缓存 |
| operationsPerTick | 1 | 每tick可完成操作数（升级可提升） |
| 掉落类型 | AdvMachine | 进阶机器掉落 |
| 稀有度 | UNCOMMON | |

### 1.3 槽位结构

| 槽位组 | 数量 | 类型 | 说明 |
|--------|------|------|------|
| 全息合成网格 `craftingGrid[9]` | 9 | **虚拟槽（PhantomSlot）** | 仅存配方图案，不消耗物品 |
| 原料输入槽 `ingredientsRow[9]` | 9 | InvSlot (Access.I, size=1) | 实际原料，与全息网格同索引一一对应 |
| 产物输出槽 `craftingOutput` | 1 | InvSlotOutput | 合成产物 |
| 容器输出槽 `containerOutput` | 9 | InvSlotOutput | 容器剩余物（空桶等） |
| 升级槽 `upgradeSlot` | 4 | InvSlotUpgrade | 升级卡 |
| 电池槽（放电槽） | 1 | InvSlotDischarge | 放电槽 |

**总计：9(虚拟) + 9 + 1 + 9 + 4 + 1 = 33，其中 itemHandler 实际管理 24 个槽**

### 1.4 项目现有可复用组件

#### ✅ PhantomSlot（虚拟槽）— 已有现成实现
- 文件：`Menu/Slot/PhantomSlot.java`
- 功能：不可拾取、不可放入真实物品、支持点击设置/清除虚拟物品
- **已在分拣机（SorterElcMenu）和进阶采矿机（AdvancedMinerElcMenu）中使用**
- 使用模式：
  ```java
  // 在 BlockEntity 中用 ItemStack[] 存储虚拟槽数据
  private final ItemStack[] filterStacks = new ItemStack[FILTER_SLOT_COUNT];
  
  // 在 Menu 中创建 PhantomSlot 并重写 getItem/set 委托给 BlockEntity
  PhantomSlot phantomSlot = new PhantomSlot(index, x, y, stackSizeLimit) {
      @Override
      public ItemStack getItem() { return blockEntity.getFilterStack(filterIndex); }
      @Override
      public void set(ItemStack stack) { blockEntity.setFilterStack(filterIndex, stack); }
  };
  
  // 在 Menu.clicked() 中拦截 PhantomSlot 的点击
  if (slot instanceof PhantomSlot phantomSlot) {
      phantomSlot.onSlotClick(dragType, player);
      return;
  }
  ```

#### ✅ mio_icif_producer（生产者基类）— 已有
- 提供：progress、maxProgress、energyPerTick、SlotLayout、升级卡支持
- 子类只需实现 `handleWork()` 逻辑

#### ✅ SlotLayout 系统 — 已有
- `SlotLayout.builder().battery().upgrade(4).input(9).output(10).build()`
- 可灵活定义槽位布局

#### ✅ mio_icif_industrial_workbench — 已有类似实现
- 已实现 3×3 合成网格 + 配方查找
- 使用 `level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, ...)` 查找配方
- **可作为配方查找逻辑的参考**

### 1.5 核心工作逻辑（需实现）

```
服务端 tick:
  1. 平衡原料（shift-click 物品自动分配到对应原料槽）
  2. 检查是否可合成：
     - 配方存在（基于全息网格查找）
     - 每个有全息物品的槽，其原料槽的物品仍被配方接受
  3. 执行工作：
     if (canCraft && outputCanAdd && energy.useEnergy(energyConsume)):
       progress++
       if (progress >= operationLength):
         doCrafting()  // 完成一次合成
         progress = 0
     else:
       if (!hasRecipe()) progress = 0
       setActive(false)
  4. 升级卡 tick
```

### 1.6 合成执行逻辑（需实现）

```
doCrafting() — 循环 operationsPerTick 次:
  1. 通知升级卡 onProcessEnd()
  2. craft():
     a. craftingOutput.add(recipeOutput)  // 产出放入输出槽
     b. 获取容器剩余物 recipe.getRemainingItems(ingredients)
     c. 对每个 ingredientsRow[slot]:
        - 如果有物品且对应全息槽也有物品 → 消耗1个
        - 如果有容器剩余物 → 尝试放回原料槽，否则放 containerOutput，否则丢弃
     d. 处理多余的容器剩余物（索引 >= 9 的）
  3. 如果配方失效或输出槽满，停止
```

### 1.7 配方匹配机制（关键！）

- **全息网格**定义"配方形状"（哪些位置需要物品、什么物品）
- **原料槽**定义"实际材料"（真正被消耗的物品）
- 两者通过**同索引位置一一对应**
- 原料槽的 `accepts()` 过滤：放入物品时，临时替换全息网格对应位置，检查配方是否仍成立

### 1.8 1.21.1 API 适配

| IC2 1.12.2 | 1.21.1 NeoForge | 说明 |
|---|---|---|
| `CraftingManager.findMatchingRecipe(crafting, world)` | `level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingInput, level)` | 参考工业工作台实现 |
| `IRecipe.matches()` / `IRecipe.getCraftingResult()` | `CraftingRecipe.matches()` / `CraftingRecipe.assemble()` | 接口名变化 |
| `recipe.getRemainingItems(ingredients)` | `recipe.getRemainingItems(craftingInput)` | 容器剩余物 |
| `InventoryCrafting` | `CraftingInput` (1.21+) | 合成容器类变化 |
| `SlotHologramSlot` | **PhantomSlot**（项目已有） | 直接复用 |

### 1.9 标签

| 标签 | 必须 | 说明 |
|------|------|------|
| `minecraft:mineable/pickaxe` | ✅ | 镐子可挖掘 |
| `c:needs_iron_tool` | ✅ | 需铁镐以上（进阶机器） |
| `c:machine` | ✅ | 扳手拆卸逻辑 |
| `mio_icif:producer` | ✅ | 标识为加工机器 |

### 1.10 需要创建的文件

| 文件 | 路径 | 说明 |
|------|------|------|
| BlockEntity | `Blocks/entity/producer/mio_icif_batch_crafter.java` | 方块实体 |
| Menu | `Menu/Producer/BatchCrafterMenu.java` | 容器菜单 |
| Menu Type | `Menu/mio_icif_menus.java` 中注册 | 菜单类型 |
| BlockEntity Type | `Blocks/entity/mio_icif_block_entities.java` 中注册 | 实体类型 |
| Block | `Blocks/mio_icif_blocks.java` 中注册 | 方块注册 |
| Screen | `client/screen/producer/BatchCrafterScreen.java` | 客户端 GUI |
| 配方 | `resources/data/mio_icif/recipe/` | 合成配方 |

### 1.11 实现步骤

1. **创建 BlockEntity** (`mio_icif_batch_crafter.java`)
   - 继承 `mio_icif_producer`
   - 定义 `craftingGrid[9]`（ItemStack[]，虚拟槽数据，NBT持久化）
   - 定义 SlotLayout：battery(1) + upgrade(4) + input(9) + output(10) = 24
   - 实现 `findRecipe()`：基于全息网格查找 CraftingRecipe
   - 实现 `canCraft()`：配方存在且原料槽物品被配方接受
   - 实现 `craft()`：消耗原料、产出结果、处理容器剩余物
   - 实现 `handleWork()`：进度推进 + doCrafting
   - 实现 `setOverclockRates()`：超频升级参数调整
   - NBT 读写：progress + craftingGrid

2. **创建 Menu** (`BatchCrafterMenu.java`)
   - 继承 `mio_icif_machine_menu`
   - 9个 PhantomSlot 对应全息合成网格（委托给 blockEntity.craftingGrid）
   - 9个 SlotItemHandler 对应原料输入槽
   - 1个 SlotItemHandler 对应产物输出槽
   - 9个 SlotItemHandler 对应容器输出槽
   - 4个 SlotItemHandler 对应升级槽
   - 1个 SlotItemHandler 对应电池槽
   - 重写 `clicked()` 拦截 PhantomSlot 点击
   - 重写 `quickMoveStack()` 处理 shift-click

3. **创建 Screen** (`BatchCrafterScreen.java`)
   - 绘制 3×3 全息网格（PhantomSlot 区域）
   - 绘制原料输入行
   - 绘制产物输出 + 容器输出
   - 绘制升级槽 + 电池槽
   - 绘制进度箭头 + 能量条
   - 绘制配方产物预览图标

4. **注册**：BlockEntityType、MenuType、Block、标签

---

## 二、区块加载器（Chunk Loader）

### 2.1 原版IC2行为总结

区块加载器是一台**电力方块**，消耗EU强制加载周围区块：
- 放置时默认加载自身所在区块
- GUI 中显示 9×9 区块地图（半径4个区块）
- 点击区块格子切换加载/卸载状态
- 每tick消耗 `加载区块数 × euPerChunk` 的EU
- 能量不足时自动释放所有强制加载
- 自身所在区块不可卸载

### 2.2 原版参数对齐

| 参数 | IC2 原版值 | 说明 |
|------|-----------|------|
| defaultTier | 1 (MV) | 电压等级 |
| defaultEnergyStorage | 2500 EU | 内部能量缓存 |
| euPerChunk | 1.0 EU/t（可配置） | 每个加载区块每tick耗电量 |
| 加载范围 | 9×9 = 81 区块 | 以自身区块为中心，半径4 |
| 掉落类型 | Self | 掉落自身 |
| 稀有度 | UNCOMMON | |

### 2.3 槽位结构

| 槽位组 | 数量 | 类型 | 说明 |
|--------|------|------|------|
| 升级槽 `upgradeSlot` | 4 | InvSlotUpgrade | 升级卡 |
| 放电槽 `dischargeSlot` | 1 | InvSlotDischarge | 放电槽 |

**总计：5 个槽位**（非常简洁）

### 2.4 核心逻辑

```
服务端 tick:
  1. 计算耗电量 = loadedChunks.size() × euPerChunk
  2. 尝试消耗能量：active = energy.useEnergy(耗电量)
  3. 如果 active 状态变化：
     - 变为活跃 → 创建 Ticket，强制加载所有 loadedChunks
     - 变为非活跃 → 释放 Ticket，取消所有强制加载
  4. 升级卡 tick
```

### 2.5 区块加载范围

```
isChunkInRange(chunk):
  mainChunk = getChunkCoords(pos)  // 机器所在区块
  return |chunk.x - mainChunk.x| <= 4 && |chunk.z - mainChunk.z| <= 4
```

### 2.6 区块增删规则

- `addChunkToLoaded(chunk)`：范围检查 + 数量限制 + 添加到 Ticket
- `removeChunkFromLoaded(chunk)`：**不允许移除自身所在区块** + 从 Ticket 移除
- `onPlaced()`：默认加载自身所在区块
- `onBlockBreak()`：释放所有强制加载

### 2.7 支持的升级属性

```java
{EnergyStorage, ItemConsuming, ItemProducing, Transformer}
```
- **不支持 Processing 升级**（没有加速概念）

### 2.8 1.21.1 NeoForge 区块强制加载 API

**关键变化：1.21 不再使用 ForgeChunkManager.Ticket 机制！**

| IC2 1.12.2 | 1.21.1 NeoForge | 说明 |
|---|---|---|
| `ForgeChunkManager.Ticket` | **不需要** | 1.21 直接使用 ServerLevel API |
| `ForgeChunkManager.forceChunk(ticket, chunk)` | `serverLevel.setChunkForced(chunkPos.x, chunkPos.z, true)` | 强制加载区块 |
| `ForgeChunkManager.unforceChunk(ticket, chunk)` | `serverLevel.setChunkForced(chunkPos.x, chunkPos.z, false)` | 取消强制加载 |
| `ForgeChunkManager.requestTicket()` | **不需要** | 不需要 Ticket 概念 |
| `ForgeChunkManager.releaseTicket()` | **不需要** | 遍历 loadedChunks 逐个 setChunkForced(false) |
| `ForgeChunkManager.LoadingCallback` | **不需要** | NBT 持久化由 BlockEntity 自身处理 |
| `ForgeChunkManager.getMaxChunkDepthFor()` | 自行配置或使用默认值 | 最大区块数限制 |

**简化后的实现**：
- **不需要 ChunkLoaderLogic 全局管理器**
- BlockEntity 自身 NBT 存储 `loadedChunks` 列表
- `setActive(true)` 时遍历 `loadedChunks` 调用 `setChunkForced(true)`
- `setActive(false)` 时遍历调用 `setChunkForced(false)`
- `onLoad()` 时从 NBT 恢复并重新强制加载
- `setRemoved()` / `onBlockBreak()` 时释放所有强制加载

### 2.9 GUI 交互

- 客户端显示 9×9 区块地图网格
- 每个格子 16×16 像素，显示区块地形缩略图
- 已加载区块：半透明绿色覆盖
- 未加载区块：半透明灰色覆盖
- 点击格子 → 发送网络事件 → 服务端切换加载状态
- 显示 "已加载数 / 最大数" 文字

### 2.10 标签

| 标签 | 必须 | 说明 |
|------|------|------|
| `minecraft:mineable/pickaxe` | ✅ | 镐子可挖掘 |
| `c:needs_stone_tool` | ✅ | 需石镐以上 |
| `c:machine` | ✅ | 扳手拆卸逻辑 |
| `mio_icif:chunk_loader` | ✅ | 新增子分类标签 |

### 2.11 需要创建的文件

| 文件 | 路径 | 说明 |
|------|------|------|
| BlockEntity | `Blocks/entity/producer/mio_icif_chunk_loader.java` | 方块实体 |
| Menu | `Menu/Producer/ChunkLoaderMenu.java` | 容器菜单 |
| Menu Type | `Menu/mio_icif_menus.java` 中注册 | 菜单类型 |
| BlockEntity Type | `Blocks/entity/mio_icif_block_entities.java` 中注册 | 实体类型 |
| Block | `Blocks/mio_icif_blocks.java` 中注册 | 方块注册 |
| Screen | `client/screen/producer/ChunkLoaderScreen.java` | 客户端 GUI（含区块地图） |

### 2.12 实现步骤

1. **创建 BlockEntity** (`mio_icif_chunk_loader.java`)
   - 继承 `mio_icif_Energy_Container` 或 `mio_icif_Energy_Block`
   - 定义 `loadedChunks: Set<ChunkPos>`（NBT持久化）
   - 实现 `tickServer()`：耗电 = loadedChunks.size() × euPerChunk，管理活跃状态
   - 实现 `setActive(true/false)`：遍历 loadedChunks 调用 `serverLevel.setChunkForced()`
   - 实现 `addChunkToLoaded()` / `removeChunkFromLoaded()`
   - 实现 `isChunkInRange()`：9×9 范围检查
   - 实现 `onPlaced()`：默认加载自身区块
   - 实现 `onBlockBreak()` / `setRemoved()`：释放所有强制加载
   - 实现 `onLoad()`：从NBT恢复并重新强制加载
   - NBT 读写：loadedChunks 列表

2. **创建 Menu** (`ChunkLoaderMenu.java`)
   - 继承 `mio_icif_machine_menu`
   - 4个升级槽 + 1个放电槽
   - 同步 loadedChunks 数据到客户端

3. **创建 Screen** (`ChunkLoaderScreen.java`)
   - 绘制 9×9 区块地图网格
   - 绘制地形缩略图（取最高非空气方块的 MapColor）
   - 已加载/未加载区块颜色覆盖
   - 点击处理 → 发送网络事件
   - 显示 "已加载数 / 最大数" 文字
   - 能量条

4. **注册**：BlockEntityType、MenuType、Block、标签

5. **配置**：在模组配置中添加 `euPerChunk` 选项（默认 1.0）

---

## 三、两个机器的对比

| 特性 | 批量工作台 | 区块加载器 |
|------|-----------|-----------|
| 基类 | mio_icif_producer | mio_icif_Energy_Block / Container |
| 有工作进度 | ✅ | ❌ |
| 有配方处理 | ✅（原版合成配方） | ❌ |
| 虚拟槽 | ✅（9格全息网格） | ❌ |
| 区块强制加载 | ❌ | ✅ |
| 升级卡 | ✅（含Processing） | ✅（不含Processing） |
| GUI 复杂度 | 中等 | 高（区块地图） |
| 1.21 API 变化 | 配方查找API | 区块加载API（完全不同） |

---

## 四、风险与注意事项

1. **批量工作台 - CraftingInput 适配**：1.21 的 `CraftingInput` 与 1.12 的 `InventoryCrafting` 差异较大，需要仔细适配全息网格→CraftingInput 的转换
2. **批量工作台 - 原料平衡**：IC2 的 `StackUtil.balanceStacks()` 逻辑需要自行实现
3. **批量工作台 - 容器剩余物**：`recipe.getRemainingItems()` 在 1.21 中的行为需要验证
4. **区块加载器 - setChunkForced 持久化**：`ServerLevel.setChunkForced()` 在世界重新加载时**不会自动恢复**，必须在 BlockEntity.onLoad() 中重新调用
5. **区块加载器 - 最大区块数**：1.21 没有直接的 `getMaxChunkDepthFor()` 等效API，需要自行设定上限或读取 Forge 配置
6. **区块加载器 - 性能**：9×9=81 个区块的强制加载可能对服务器性能有影响，建议默认只加载自身区块