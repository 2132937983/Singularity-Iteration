# mio_icif API 文档

## 概述

这是 Industrial Craft In Future (mio_icif) 的公共 API，供其他模组（附属模组）与 mio_icif 交互。

## 快速开始

```java
// 获取 API 实例
MioIcifAPI api = MioIcifAPI.instance();

// 使用能源 API
IEnergyNetAPI energyApi = api.getEnergyNetAPI();
NodeStats stats = energyApi.getNodeStats(world, pos);

// 使用物品 API
IItemAPI itemApi = api.getItemAPI();
Item battery = itemApi.getItem(ResourceLocation.fromNamespaceAndPath("mio_icif", "battery"));

// 使用配方 API
IRecipeAPI recipeApi = api.getRecipeAPI();
RecipeType<?> powderType = recipeApi.getPowderRecipeType();

// 使用流体 API
IFluidAPI fluidApi = api.getFluidAPI();
Fluid biogas = fluidApi.getBiogas();

// 使用作物 API
ICropAPI cropApi = api.getCropAPI();
cropApi.registerPlant(new MyCustomPlant());

// 使用方块 API
IBlockAPI blockApi = api.getBlockAPI();
Block hull = blockApi.getMachineHullBasic();

// 使用注册表 API
IMioIcifRegistries registries = api.getRegistries();
Collection<Item> batteries = registries.getItemsByCategory(ItemCategory.BATTERY);
```

## API 模块

### 1. 能源网络 API (`IEnergyNetAPI`)

提供 EU 电网的查询功能：
- `getNodeStats(world, pos)` - 获取节点统计信息
- `hasEnergyTile(world, pos)` - 检查位置是否有电网节点
- `getEnergyTile(world, pos)` - 获取能源方块访问接口
- `getPowerFromTier(tier)` - 等级转功率
- `getTierFromPower(power)` - 功率转等级

### 2. 物品 API (`IItemAPI`)

提供物品查询功能：
- `getItem(id)` - 根据 ID 获取物品
- `isElectricTool(stack)` - 检查是否是电动工具
- `isBattery(stack)` - 检查是否是电池
- `isLatheItem(stack)` - 检查是否是车床加工件
- `hasCustomDamage(stack)` - 检查是否有自定义耐久

### 3. 配方 API (`IRecipeAPI`)

提供配方类型访问：
- `getPowderRecipeType()` - 打粉配方
- `getCompressorRecipeType()` - 压缩机配方
- `getExtractorRecipeType()` - 提取机配方
- `getWasherRecipeType()` - 洗矿配方
- `getCentrifugeRecipeType()` - 热能离心机配方
- `getMetalFormerRecipeType()` - 金属成型机配方
- `getBlastFurnaceRecipeType()` - 高炉配方
- `getCannerRecipeType()` - 装罐机配方

### 4. 配方注册 API (`IRecipeRegistrationAPI`)

允许附属模组注册新配方：
- `registerCompressorRecipe(...)` - 注册压缩机配方
- `registerExtractorRecipe(...)` - 注册提取机配方
- `registerMaceratorRecipe(...)` - 注册打粉机配方
- `registerElectricFurnaceRecipe(...)` - 注册电炉配方
- 等等...

### 5. 流体 API (`IFluidAPI`)

提供流体访问：
- `getFluid(id)` - 根据 ID 获取流体
- `getBiogas()` - 生物气体
- `getHotWater()` - 热水
- `getBiomass()` - 生物质
- `getConstructionFoam()` - 建筑泡沫
- `getCoolant()` - 冷却液

### 6. 作物 API (`ICropAPI`)

提供作物系统访问：
- `registerPlant(plantType)` - 注册自定义植物
- `getPlant(modId, typeId)` - 获取植物类型
- `getAllPlants()` - 获取所有植物
- `isBaseSeed(stack)` - 检查是否是基础种子
- `registerBaseSeed(seed, plantType)` - 注册基础种子

### 7. 方块 API (`IBlockAPI`)

提供方块访问：
- `getBlock(id)` - 根据 ID 获取方块
- `getMachineHullBasic()` - 基础机器外壳
- `getMachineHullAdvanced()` - 高级机器外壳
- `getAllBlockIds()` - 获取所有方块 ID

### 8. 注册表 API (`IMioIcifRegistries`)

提供完整的注册表访问：
- `getBlock(id)` / `getItem(id)` / `getFluid(id)` - 根据 ID 获取内容
- `getBlocksByPrefix(prefix)` / `getItemsByPrefix(prefix)` - 根据前缀获取
- `getBlocksByCategory(category)` / `getItemsByCategory(category)` - 根据分类获取
- `getAllBlocks()` / `getAllItems()` / `getAllFluids()` - 获取所有内容

### 9. 机器构建 API (`IMachineBuilderAPI`) ⭐ 新增

**这是最重要的新增 API**，让附属模组可以方便地创建符合 mio_icif 风格的机器。

#### 电力机器构建器

```java
IMachineBuilderAPI builder = api.getMachineBuilderAPI();

// 创建电力机器
IElectricMachineBuilder electricBuilder = builder.createElectricMachineBuilder();
IMachineDefinition machineDef = electricBuilder
    .setName("advanced_compressor")
    .setEnergyCapacity(50000)
    .setEnergyPerTick(128)
    .setProcessTime(100)
    .addInputSlot(0, 64)
    .addOutputSlot(1, 64)
    .addBatterySlot(2)
    .addUpgradeSlots(3, 4)
    .setCableTier(CableTier.MV)
    .setRecipeType(api.getRecipeAPI().getCompressorRecipeType())
    .useDefaultsForTier(CableTier.MV)  // 自动配置能量参数
    .buildAndRegister("mymod");
```

#### 发电机构建器

```java
IGeneratorBuilder genBuilder = builder.createGeneratorBuilder();
var generator = genBuilder
    .setName("advanced_solar")
    .setGenerationRate(128)
    .setEnergyCapacity(100000)
    .setCableTier(CableTier.MV)
    .setGeneratorType(GeneratorType.SOLAR)
    .setRequiresSky(true)
    .setDayOnly(true)
    .addBatterySlot(0)
    .buildAndRegister("mymod");
```

#### 热能机器构建器

```java
IHeatMachineBuilder heatBuilder = builder.createHeatMachineBuilder();
var heatMachine = heatBuilder
    .setName("advanced_heat_exchanger")
    .setHeatCapacity(10000)
    .setHeatOutput(100)
    .setHeatTransferRate(50)
    .addInputSlot(0, 64)
    .addOutputSlot(1, 64)
    .buildAndRegister("mymod");
```

#### 动能机器构建器

```java
IKineticMachineBuilder kineticBuilder = builder.createKineticMachineBuilder();
var kineticMachine = kineticBuilder
    .setName("advanced_kinetic_generator")
    .setKineticCapacity(10000)
    .setKineticOutput(100)
    .setRequiresRotor(true)
    .setRotorType(RotorType.WIND)
    .addRotorSlot(0)
    .addBatterySlot(1)
    .buildAndRegister("mymod");
```

#### 槽位布局辅助

```java
// 创建标准布局
ISlotLayout layout = builder.createStandardLayout(2, 2, true, 4);
// 2输入, 2输出, 有电池槽, 4个升级槽

// 创建流体布局
ISlotLayout fluidLayout = builder.createFluidLayout(1, 1, true, 4, 2, 8000);
// 1输入, 1输出, 有电池槽, 4个升级槽, 2个流体槽, 每个8000mB
```

#### 等级默认模板

```java
// 根据等级获取默认参数
IMachineDefaults defaults = builder.getDefaultsForTier(CableTier.MV);
long capacity = defaults.getDefaultCapacity();      // 50000
long energyPerTick = defaults.getDefaultEnergyPerTick(); // 50
int processTime = defaults.getDefaultProcessTime();      // 300
```

### 10. 机器 API (`IMachineAPI`)

提供机器查询和控制：
- `isMachine(world, pos)` - 检查是否是机器
- `getMachineInfo(world, pos)` - 获取机器信息
- `getMachinesByType(world, type)` - 获取指定类型的机器
- `forceStart(world, pos)` / `forceStop(world, pos)` - 强制启停
- `getSpeedMultiplier(world, pos)` - 获取速度倍率
- `supportsUpgrades(world, pos)` - 是否支持升级

### 11. 发电机 API (`IGeneratorAPI`) ⚠️ 已废弃

> **迁移通知**：发电机已统一为机器的一种类型，此API已标记为 `@Deprecated(forRemoval = true)`。

#### 迁移指南

| 旧方式 (已废弃) | 新方式 |
|----------------|--------|
| `api.getGeneratorAPI()` | `api.getMachineAPI()` |
| `IGeneratorAPI.getGeneratorInfo(world, pos)` | `IMachineAPI.getMachineInfo(world, pos)` |
| `IGeneratorAPI.forceStart/Stop(world, pos)` | `IMachineAPI.forceStart/Stop(world, pos)` |
| `api.getGeneratorAPI().registerGeneratorType(...)` | `api.getMachineBuilderAPI().createGeneratorBuilder()` |

#### 创建新发电机

```java
// 使用发电机构建器创建自定义发电机
IMachineBuilderAPI builder = api.getMachineBuilderAPI();

var generator = builder.createGeneratorBuilder()
    .setName("advanced_solar")
    .setGenerationRate(128)
    .setEnergyCapacity(100000)
    .setCableTier(CableTier.MV)
    .setGeneratorType(IGeneratorBuilder.GeneratorType.SOLAR)
    .setRequiresSky(true)
    .setDayOnly(true)
    .addBatterySlot(0)
    .withEntityType(MY_GENERATOR_BLOCK_ENTITY_TYPE.get())
    .buildAndRegister("mymod");
```

#### 查询发电机状态

```java
// 使用 MachineAPI 查询
IMachineAPI machineApi = api.getMachineAPI();
IMachineInfo info = machineApi.getMachineInfo(world, pos);

// 或使用 Capability
var genCapability = world.getCapability(
    IMioIcifCapabilities.GENERATOR_BLOCK, pos, null
);
if (genCapability != null) {
    boolean generating = genCapability.isGenerating();
    long rate = genCapability.getGenerationRate();
}
```

> 此API仅保留用于向后兼容，将在未来版本中移除。

### 12. 热能 API (`IHeatAPI`)

提供 HU 热能系统访问：
- `getHeatStorage(world, pos)` - 获取热能存储
- `hasHeatTile(world, pos)` - 检查是否有热能方块
- `getHeatConnections(world, pos)` - 获取热能连接

### 13. 动能 API (`IKineticAPI`)

提供 KU 动能系统访问：
- `getKineticStorage(world, pos)` - 获取动能存储
- `hasKineticTile(world, pos)` - 检查是否有动能方块
- `getKineticConnections(world, pos)` - 获取动能连接

### 14. 核反应堆 API (`IReactorAPI`)

提供反应堆元件访问：
- `isReactorComponent(stack)` - 检查是否是反应堆元件
- `getComponentType(stack)` - 获取元件类型
- `getNeutronPulseOutput(stack)` - 获取中子脉冲
- `getHeatOutput(stack)` - 获取热量输出

### 15. 升级插件 API (`IUpgradeAPI`)

提供升级系统访问：
- `supportsUpgrades(world, pos)` - 检查是否支持升级
- `getInstalledUpgrades(world, pos)` - 获取已安装升级
- `installUpgrade(world, pos, upgrade)` - 安装升级
- `getUpgradeType(upgrade)` - 获取升级类型
- `calculateCombinedEffect(upgrades)` - 计算组合效果

## 能力系统 (Capabilities)

mio_icif 通过 NeoForge 的 Capability 系统暴露以下能力：

### 方块能力
- `EU_STORAGE_BLOCK` - EU 能量存储
- `MACHINE_BLOCK` - 机器信息
- `GENERATOR_BLOCK` - 发电机信息
- `HEAT_STORAGE_BLOCK` - 热能存储
- `KINETIC_STORAGE_BLOCK` - 动能存储

### 物品能力
- `EU_STORAGE_ITEM` - 物品 EU 存储
- `ELECTRIC_ITEM` - 电动物品
- `UPGRADE_ITEM` - 升级物品
- `REACTOR_COMPONENT` - 反应堆组件

## 事件系统

mio_icif 提供以下事件供附属模组监听：

- `EnergyGenerationEvent` - 能量生成事件
- `EnergyTransferEvent` - 能量传输事件
- `GeneratorStartEvent` - 发电机启动事件
- `GeneratorStopEvent` - 发电机停止事件

## 注意事项

1. 所有 API 方法都是线程安全的（在 Minecraft 主线程调用）
2. 返回 null 表示请求的内容不存在
3. 机器构建 API 目前为框架实现，需要进一步完善内部逻辑
4. 配方注册 API 需要在合适的生命周期阶段调用（如 `FMLCommonSetupEvent`）

## 版本历史

### v1.1.0 (当前)
- 新增机器构建 API (`IMachineBuilderAPI`)
- 新增注册表 API (`IMioIcifRegistries`)
- 新增能力系统定义 (`IMioIcifCapabilities`)
- 完善 API 文档

### v1.0.0
- 初始 API 版本
- 包含能源、物品、配方、流体、作物、方块、热能、动能、反应堆、升级等 API