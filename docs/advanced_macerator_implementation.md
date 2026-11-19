# 纯 API 开发机器指南

> 本文档记录如何使用 mio_icif 公开 API 实现一个新机器，**不引用任何内部类**。
> 以三个进阶机器（打粉机、压缩机、金属成型机）为完整示例。

---

## 1. 核心原则

### 1.1 什么算"纯 API 实现"

纯 API 实现的机器类中：
- **只 import** `com.singularity_iteration.mio_icif.api.*` 下的接口和类
- **不 import** 任何 `com.singularity_iteration.mio_icif.Blocks.entity.slot.*`（内部 SlotLayout/SlotType）
- **不 import** 任何 `com.singularity_iteration.mio_icif.Blocks.entity.CableTier`（内部电缆等级）
- **不硬编码** 槽位索引（如 `INPUT_SLOT = 0`），全部从 `ISlotLayout` 动态获取

### 1.2 为什么需要纯 API

- **附属开发可行性**：附属模组只能依赖 API 包，不能引用内部类
- **布局自适应**：槽位从 ISlotLayout 动态获取，布局规则变化时无需修改机器代码
- **类型安全**：通过 API 接口而非内部枚举，避免类型不匹配导致的运行时崩溃

---

## 2. API 入口总览

所有 API 通过 `MioIcifAPI.instance()` 单例获取：

| API | 获取方式 | 用途 |
|-----|---------|------|
| `IMachineBuilderAPI` | `getMachineBuilderAPI()` | 创建槽位布局、构建机器定义 |
| `IRecipeAPI` | `getRecipeAPI()` | 查找配方、获取输出/时间/能耗 |
| `IEnergyNetAPI` | `getEnergyNetAPI()` | 获取电缆等级（MV、HV 等） |
| `IItemAPI` | `getItemAPI()` | 判断物品类型（电池、升级卡） |

---

## 3. 机器参数对齐（METS 原版）

### 3.1 进阶旋风打粉机

METS 原版调用：`super(5, 150, 1, 1)`

| 参数 | 普通打粉机 (IC2) | 进阶打粉机 (METS) | 说明 |
|------|-----------------|-------------------|------|
| energyPerTick | 2 EU/t | **5 EU/t** | 每tick耗电量 |
| defaultOperationLength | 300 ticks | **150 ticks** | 基础工作时间 |
| inputSlots | 1 | **1** | 输入槽数量 |
| outputSlots | 1 | **1** | 输出槽数量（METS原版仅1个） |
| 电压等级 | LV | **MV** | 能量等级2 |
| 能量容量 | 600 | **750** | energyPerTick × workTime |
| 最大接收 | 32 EU/t | **128 EU/t** | MV 等级上限 |

### 3.2 进阶高压压缩机

METS 原版调用：`super(25, 70, 1, 1)`

| 参数 | 普通压缩机 (IC2) | 进阶压缩机 (METS) | 说明 |
|------|-----------------|-------------------|------|
| energyPerTick | 2 EU/t | **25 EU/t** | 每tick耗电量 |
| defaultOperationLength | 300 ticks | **70 ticks** | 基础工作时间 |
| inputSlots | 1 | **1** | 输入槽数量 |
| outputSlots | 1 | **1** | 输出槽数量 |
| 电压等级 | LV | **MV** | 能量等级2 |
| 能量容量 | 600 | **1750** | energyPerTick × workTime |
| 最大接收 | 32 EU/t | **128 EU/t** | MV 等级上限 |

### 3.3 进阶金属成型机

METS 原版调用：`super(50, 20, 1, 1)`

| 参数 | 普通金属成型机 (IC2) | 进阶金属成型机 (METS) | 说明 |
|------|---------------------|----------------------|------|
| energyPerTick | 2 EU/t | **50 EU/t** | 每tick耗电量 |
| defaultOperationLength | 300 ticks | **20 ticks** | 基础工作时间 |
| inputSlots | 1 | **1** | 输入槽数量 |
| outputSlots | 1 | **1** | 输出槽数量 |
| 电压等级 | LV | **MV** | 能量等级2 |
| 能量容量 | 600 | **1000** | energyPerTick × workTime |
| 最大接收 | 32 EU/t | **128 EU/t** | MV 等级上限 |

### 3.4 进阶机器的优势

| 优势 | 原因 |
|------|------|
| 处理速度大幅提升 | 工作时间缩短为原来的 1/2 ~ 1/15 |
| 支持升级卡 | 4个升级槽，可加速/超频/节能 |
| MV 电压等级 | 可直接连接 MV 电网，无需变压器 |
| 纯 API 实现 | 附属开发者可参照实现自己的机器 |

---

## 4. 槽位布局（ISlotLayout）

### 4.1 通过 API 创建布局

```java
public static ISlotLayout getOrCreateLayout() {
    if (CACHED_LAYOUT == null) {
        CACHED_LAYOUT = MioIcifAPI.instance().getMachineBuilderAPI()
            .createStandardLayout(1, 1, true, 4);
    }
    return CACHED_LAYOUT;
}
```

`createStandardLayout(inputCount, outputCount, hasBattery, upgradeCount)` 参数：

| 参数 | 值 | 说明 |
|------|---|------|
| inputCount | 1 | 1个输入槽 |
| outputCount | 1 | 1个输出槽 |
| hasBattery | true | 有1个电池槽 |
| upgradeCount | 4 | 4个升级槽 |

### 4.2 布局产生的槽位索引

`createStandardLayout(1, 1, true, 4)` 产生的布局：

| 索引 | 用途 | 通过 API 获取 |
|------|------|--------------|
| 0 | 输入 | `layout.getInputSlots()[0]` |
| 1 | 输出 | `layout.getOutputSlots()[0]` |
| 2 | 电池 | `layout.getBatterySlots()[0]` |
| 3-6 | 升级 | `layout.getUpgradeSlots()[0..3]` |

**总计 7 个槽位**，通过 `layout.getTotalCount()` 获取。

### 4.3 在构造函数中缓存槽位索引

```java
private final int inputSlot;
private final int batterySlot;
private final int outputSlot;

public mio_icif_powder_advanced_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
    super(pos, state, type,
        DEFAULT_CAPACITY,
        DEFAULT_MAX_RECEIVE,
        DEFAULT_MAX_EXTRACT,
        DEFAULT_WORK_TIME,
        getOrCreateLayout(),
        DEFAULT_ENERGY_PER_TICK,
        getOrCreateTier());
    ISlotLayout layout = getOrCreateLayout();
    this.inputSlot = layout.getInputSlots()[0];
    this.batterySlot = layout.getBatterySlots()[0];
    this.outputSlot = layout.getOutputSlots()[0];
    setConfiguration(getOrCreateConfiguration());
    setWorkHandler(this::handleWork);
}
```

**关键**：槽位索引从 `ISlotLayout` 动态获取并缓存为 `final` 字段，而非硬编码常量。
如果布局规则变化（如增加输入槽），索引自动适应。

---

## 5. 电压等级（ICableTier）

### 5.1 通过 API 获取

```java
public static ICableTier getOrCreateTier() {
    if (CACHED_TIER == null) {
        CACHED_TIER = MioIcifAPI.instance().getEnergyNetAPI().getCableTier("mv");
    }
    return CACHED_TIER;
}
```

可用等级名称：`"lv"`、`"mv"`、`"hv"`、`"ev"`、`"iv"` 等。

---

## 6. 机器定义（IMachineBuilderAPI）

### 6.1 构建并注册机器定义

```java
private static IMachineBuilderAPI.MachineConfiguration buildMachineDefinition() {
    IElectricMachineBuilder builder = MioIcifAPI.instance().getMachineBuilderAPI()
        .createElectricMachineBuilder()
        .setName("powder_advanced_elc")
        .setTranslationKey("container.mio_icif.powder_advanced_elc")
        .setEnergyCapacity(DEFAULT_CAPACITY)
        .setMaxReceive(DEFAULT_MAX_RECEIVE)
        .setMaxExtract(DEFAULT_MAX_EXTRACT)
        .setEnergyPerTick(DEFAULT_ENERGY_PER_TICK)
        .setProcessTime(DEFAULT_WORK_TIME)
        .setCableTier(getOrCreateTier())
        .useStandardLayout(1, 1, true, 4)
        .setSupportsUpgrades(true)
        .setMachineType(IMachineAPI.MachineType.MACERATOR)
        .withEntityType(mio_icif_block_entities.POWDER_ADVANCED_ELC_ENTITY_TYPE.get());

    builder.buildAndRegister("mio_icif");
    return builder.getConfiguration();
}
```

### 6.2 MachineType 枚举

| MachineType | 对应机器 |
|-------------|---------|
| `MACERATOR` | 打粉机 |
| `COMPRESSOR` | 压缩机 |
| `METAL_FORMER` | 金属成型机 |

### 6.3 MetalFormerMode 枚举（仅金属成型机使用）

```java
IMachineAPI.MetalFormerMode mode = IMachineAPI.MetalFormerMode.ROLLING;  // 轧制
IMachineAPI.MetalFormerMode mode = IMachineAPI.MetalFormerMode.CUTTING;  // 切割
IMachineAPI.MetalFormerMode mode = IMachineAPI.MetalFormerMode.EXTRUDING; // 挤压
```

---

## 7. 配方查找（IRecipeAPI）

### 7.1 可用的配方查找方法

| 方法 | 配方类型 | 适用机器 |
|------|---------|---------|
| `findPowderRecipe(input, level)` | 打粉 | 打粉机 |
| `findCompressorRecipe(input, level)` | 压缩 | 压缩机 |
| `findRollingRecipe(input, level)` | 轧制 | 金属成型机 |
| `findCuttingRecipe(input, level)` | 切割 | 金属成型机 |
| `findExtrudingRecipe(input, level)` | 挤压 | 金属成型机 |

### 7.2 配方结果获取

```java
IRecipeAPI recipeAPI = MioIcifAPI.instance().getRecipeAPI();
Optional<? extends RecipeHolder<?>> recipe = recipeAPI.findPowderRecipe(input, level);
if (recipe.isEmpty()) return;

ItemStack result = recipeAPI.getRecipeOutput(recipe.get());
int processTime = recipeAPI.getRecipeProcessTime(recipe.get());
long energyPerTick = recipeAPI.getRecipeEnergyPerTick(recipe.get());
int ingredientCount = recipeAPI.getRecipeIngredientCount(recipe.get());
```

---

## 8. 槽位验证（ISlotType）

### 8.1 使用 ISlotType 判断槽位类型

```java
@Override
public boolean isItemValidForSlot(int slot, ItemStack stack) {
    ISlotType type = getOrCreateLayout().getType(slot);
    if (type.isInput()) return isCrushable(stack);
    if (type.isBattery()) return isBattery(stack);
    if (type.isOutput()) return false;
    if (type.isUpgrade()) return getItemAPI().isUpgrade(stack);
    return false;
}
```

**关键**：不再使用 `slot == INPUT_SLOT` 等硬编码判断，而是通过 `ISlotType` 接口判断槽位语义。

---

## 9. 工作逻辑

### 9.1 工作处理器（setWorkHandler）

```java
private void handleWork(GenericMachineBlockEntity machine, ItemStack[] inputs) {
    if (level == null || level.isClientSide) return;
    if (inputs.length == 0 || inputs[0].isEmpty()) return;

    IRecipeAPI recipeAPI = MioIcifAPI.instance().getRecipeAPI();
    Optional<? extends RecipeHolder<?>> recipe = recipeAPI.findPowderRecipe(inputs[0], level);
    if (recipe.isEmpty()) return;

    ItemStack result = recipeAPI.getRecipeOutput(recipe.get());
    if (result.isEmpty()) return;

    int targetSlot = getTargetOutputSlot(result);
    if (targetSlot == -1) return;

    ItemStack currentOutput = itemHandler.getStackInSlot(targetSlot);
    if (currentOutput.isEmpty()) {
        itemHandler.setStackInSlot(targetSlot, result.copy());
    } else {
        currentOutput.grow(result.getCount());
    }

    int ingredientCount = recipeAPI.getRecipeIngredientCount(recipe.get());
    consumeInput(inputSlot, ingredientCount);
}
```

### 9.2 单输出槽的输出判断

```java
private boolean canFitOutput(ItemStack result) {
    ItemStack existing = itemHandler.getStackInSlot(outputSlot);
    return canFitInSlot(existing, result);
}

private int getTargetOutputSlot(ItemStack result) {
    ItemStack existing = itemHandler.getStackInSlot(outputSlot);
    if (canFitInSlot(existing, result)) return outputSlot;
    return -1;
}

private boolean canFitInSlot(ItemStack existing, ItemStack result) {
    if (existing.isEmpty()) return true;
    if (!ItemStack.isSameItem(existing, result)) return false;
    return existing.getCount() + result.getCount() <= existing.getMaxStackSize();
}
```

### 9.3 tick 方法 — 使用 IProducerBlock 接口

```java
public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_powder_advanced_elc blockEntity) {
    if (level.isClientSide()) return;
    if (blockEntity instanceof IProducerBlock producer) {
        producer.serverTick();
    }
    blockEntity.setLit(blockEntity.isWorking());
}
```

**关键**：通过 `IProducerBlock` 接口调用 `serverTick()`，而非直接引用基类方法。
`setLit()` 和 `isWorking()` 也是 `IProducerBlock` 接口方法。

---

## 10. 金属成型机模式切换

### 10.1 模式存储与 NBT

```java
private IMachineAPI.MetalFormerMode currentMode = IMachineAPI.MetalFormerMode.ROLLING;

@Override
public IMachineAPI.MetalFormerMode getMetalFormerMode() {
    return currentMode;
}

@Override
public void setMetalFormerMode(IMachineAPI.MetalFormerMode mode) {
    this.currentMode = mode;
    this.progress = 0;
    setLit(isWorking());
    setChanged();
}

@Override
protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    tag.putInt("Mode", currentMode.getId());
}

@Override
public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    int modeOrdinal = tag.getInt("Mode");
    this.currentMode = IMachineAPI.MetalFormerMode.fromId(modeOrdinal);
}
```

### 10.2 模式配方查找

```java
private Optional<? extends RecipeHolder<?>> findRecipeForMode() {
    if (level == null) return Optional.empty();
    ItemStack input = itemHandler.getStackInSlot(inputSlot);
    if (input.isEmpty()) return Optional.empty();

    IRecipeAPI recipeAPI = MioIcifAPI.instance().getRecipeAPI();
    return switch (currentMode) {
        case ROLLING -> recipeAPI.findRollingRecipe(input, level);
        case CUTTING -> recipeAPI.findCuttingRecipe(input, level);
        case EXTRUDING -> recipeAPI.findExtrudingRecipe(input, level);
    };
}
```

---

## 11. Menu 类适配

### 11.1 槽位数量

单输出槽机器的 `SLOT_COUNT`：

```java
public static final int SLOT_COUNT = 7;  // 1输入 + 1输出 + 1电池 + 4升级
```

### 11.2 添加机器槽位

```java
@Override
protected void addMachineSlots() {
    ISlotLayout layout = getSlotLayout();
    int inputSlot = layout.getInputSlots()[0];
    int batterySlot = layout.getBatterySlots()[0];
    int[] outputSlots = layout.getOutputSlots();
    int[] upgradeSlots = layout.getUpgradeSlots();

    addInputSlot(inputSlot, INPUT_X, INPUT_Y);
    addBatterySlot(batterySlot, BATTERY_X, BATTERY_Y);
    this.addSlot(new LargeSlot(itemHandler, outputSlots[0], OUTPUT_X, OUTPUT_Y, 26, 26));
    for (int i = 0; i < upgradeSlots.length; i++) {
        addUpgradeSlot(upgradeSlots[i], UPGRADE_X, UPGRADE_Y[i]);
    }
}
```

### 11.3 GUI 布局坐标

```
GUI 布局 (176×166):
┌─────────────────────────────────────────┐
│                                         │
│    [输入]  (56,17)     [输出] (116,35)  │
│              →→→                        │
│    [电池]  (56,53)     [升1]  (152,8)   │
│                         [升2]  (152,26)  │
│                         [升3]  (152,44)  │
│                         [升4]  (152,62)  │
│                                         │
│    [玩家物品栏]                          │
└─────────────────────────────────────────┘
```

输出槽居中于 y=35（原双输出槽 y=24 和 y=47 的中间位置）。

---

## 12. 完整 import 清单（纯 API 机器）

```java
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;  // 实体类型引用
import com.singularity_iteration.mio_icif.Menu.Producer.XxxMenu;                   // 菜单类
import com.singularity_iteration.mio_icif.api.MioIcifAPI;                          // API 入口
import com.singularity_iteration.mio_icif.api.energy.ICableTier;                   // 电缆等级接口
import com.singularity_iteration.mio_icif.api.internal.machine.GenericMachineBlockEntity;  // 基类
import com.singularity_iteration.mio_icif.api.machine.IProducerBlock;              // 生产者接口
import com.singularity_iteration.mio_icif.api.machine.ISlotLayout;                 // 槽位布局接口
import com.singularity_iteration.mio_icif.api.machine.ISlotType;                   // 槽位类型接口
import com.singularity_iteration.mio_icif.api.machine.builder.IElectricMachineBuilder;  // 机器构建器
import com.singularity_iteration.mio_icif.api.machine.builder.IMachineBuilderAPI;   // 机器构建API
import com.singularity_iteration.mio_icif.api.recipe.IRecipeAPI;                   // 配方API
// 金属成型机额外需要：
import com.singularity_iteration.mio_icif.api.machine.IMachineAPI;                 // MachineType/MetalFormerMode
```

**注意**：不 import 任何 `Blocks.entity.slot.*` 或 `Blocks.entity.CableTier`。

---

## 13. 内部类 → API 接口对照表

| 之前（内部类） | 之后（API 接口） |
|---------------|-----------------|
| `SlotLayout.builder().input(1).battery().output(1).upgrade(4).build()` | `MioIcifAPI.instance().getMachineBuilderAPI().createStandardLayout(1, 1, true, 4)` |
| `CableTier.MV` | `MioIcifAPI.instance().getEnergyNetAPI().getCableTier("mv")` |
| `mio_icif_producer.tick(level, pos, state, blockEntity)` | `((IProducerBlock) blockEntity).serverTick()` |
| `state.getValue(block.LIT)` + `level.setBlock(...)` | `blockEntity.setLit(blockEntity.isWorking())` |
| `mio_icif_block_metal_former_advanced.MetalFormerMode` | `IMachineAPI.MetalFormerMode` |
| `INPUT_SLOT = 0` (硬编码) | `layout.getInputSlots()[0]` (动态获取) |
| `BATTERY_SLOT = 1` (硬编码) | `layout.getBatterySlots()[0]` (动态获取) |
| `OUTPUT_SLOT = 2` (硬编码) | `layout.getOutputSlots()[0]` (动态获取) |
| `slot == INPUT_SLOT` (硬编码判断) | `layout.getType(slot).isInput()` (语义判断) |

---

## 14. 内部类桥接方法

为了让非 API 机器继续工作，内部类新增了 API 桥接：

| 内部类 | 桥接方法 | 说明 |
|--------|---------|------|
| `CableTier` | `fromICableTier(ICableTier)` | ICableTier → CableTier 转换 |
| `SlotLayout` | `fromISlotLayout(ISlotLayout)` | ISlotLayout → SlotLayout 转换 |
| `mio_icif_producer` | 构造函数接受 `ISlotLayout + ICableTier` | 兼容 API 创建的布局 |
| `GenericMachineBlockEntity` | 构造函数接受 `ISlotLayout + ICableTier` | 兼容 API 创建的布局 |

---

## 15. SlotLayout.fromISlotLayout 修复

### 15.1 崩溃原因

`SlotLayout.fromISlotLayout` 方法在将 `ISlotLayout` 转换为内部 `SlotLayout` 时，遍历的是内部 `SlotType.values()`，然后调用 `layout.getCount(type)`。

但 `SlotLayoutImpl.getCount(ISlotType type)` 检查 `type instanceof SlotType`，而这里的 `SlotType` 是 `IMachineBuilderAPI.ISlotLayout.SlotType`，不是内部 `SlotType`。所以 `instanceof` 总是返回 `false`，`getCount()` 总是返回 `0`。

结果：`SlotLayout.fromISlotLayout` 创建的 `SlotLayout` 没有任何槽位（`totalSlots = 0`），导致 `MachineItemHandler` 初始化为 0 个槽位，访问任何槽位都崩溃。

### 15.2 修复方案

将 `SlotLayout.fromISlotLayout` 改为使用 `ISlotLayout` 接口的标准方法获取槽位数量：

```java
public static SlotLayout fromISlotLayout(ISlotLayout layout) {
    if (layout instanceof SlotLayout sl) return sl;
    Builder builder = builder();
    int inputCount = layout.getInputCount();
    int outputCount = layout.getOutputCount();
    int batteryCount = layout.getBatteryCount();
    int upgradeCount = layout.getUpgradeCount();
    if (inputCount > 0) builder.input(inputCount);
    if (outputCount > 0) builder.output(outputCount);
    if (batteryCount > 0) builder.battery();
    if (upgradeCount > 0) builder.upgrade(upgradeCount);
    return builder.build();
}
```

---

## 16. 实现流程总结

按依赖顺序，使用纯 API 实现一个新机器的完整步骤：

```
步骤1: 方块实体类（纯 API）
  ├── 通过 API 创建 ISlotLayout（createStandardLayout）
  ├── 通过 API 获取 ICableTier（getCableTier）
  ├── 构造函数中从 ISlotLayout 缓存槽位索引
  ├── 通过 API 构建机器定义（IElectricMachineBuilder）
  ├── 实现 canWork() / doWork() / handleWork()
  ├── 使用 ISlotType 判断槽位类型（isItemValidForSlot）
  ├── 使用 IRecipeAPI 查找配方
  └── tick 方法通过 IProducerBlock 接口调用

步骤2: 方块类
  ├── 定义 BlockState 属性 (LIT)
  ├── 实现 newBlockEntity()
  ├── 实现 getTicker()
  └── 实现 useWithoutItem() / onRemove()

步骤3: 菜单类
  ├── 定义 SLOT_COUNT（从 ISlotLayout.getTotalCount() 获取）
  ├── 实现 addMachineSlots()（从 ISlotLayout 获取槽位索引）
  └── 实现 broadcastChanges() 数据同步

步骤4: 屏幕类
  ├── 指定 GUI_TEXTURE
  ├── 实现 renderBg() 背景渲染
  └── 实现 renderLabels() 悬停提示

步骤5: 注册 (4个文件)
  ├── mio_icif_blocks.java — DeferredRegister<Block>
  ├── mio_icif_block_entities.java — DeferredRegister<BlockEntityType>
  ├── mio_icif_menus.java — DeferredRegister<MenuType>
  └── mio_icif_screens.java — RegisterMenuScreensEvent

步骤6: 能力注册 — mio_icif_capacities.java
  ├── wireCapabilities 列表 (导线连接)
  ├── EUApi.SIDED (能量能力)
  └── Capabilities.ItemHandler.BLOCK (物品能力)

步骤7: 创造模式物品栏 — mio_icif_creativeTabs.java

步骤8: 数据文件
  ├── blockstate JSON
  ├── model JSON (关/开 + 物品)
  ├── loot_table JSON
  ├── recipe JSON
  ├── lang JSON (zh_cn + en_us)
  └── tag JSON (mineable/pickaxe + needs_stone_tool + machine)
```

---

## 17. 附属开发指南

如果附属模组要添加自己的机器，需要自行完成：

| 必须 | 内容 | 原因 |
|------|------|------|
| ✅ | DeferredRegister 注册 Block/BlockEntity/Menu | NeoForge 要求，需要附属自己的 MOD_ID |
| ✅ | RegisterCapabilitiesEvent 注册能力 | NeoForge 1.21 显式注册要求 |
| ✅ | CreativeModeTab 添加物品 | 需显式添加 |
| ✅ | 资源文件 (JSON) | 数据驱动，不属于代码API |

但基类方法让能力注册只需一行 lambda：
```java
event.registerBlockEntity(EUApi.SIDED, myEntityType, (be, dir) ->
    be instanceof mio_icif_Energy_Block e ? e.getEnergyStorageCapability(dir) : null);

event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, myEntityType, (be, dir) ->
    be instanceof mio_icif_producer p ? p.getItemHandler() : null);
```

**核心原则**：逻辑行为靠 API（基类 + 接口），注册声明靠手动（NeoForge 要求），槽位索引靠 ISlotLayout 动态获取（不硬编码）。