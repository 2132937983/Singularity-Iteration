# mio_icif Core 附属模组开发文档

> 目标环境：Minecraft 1.21.1 / NeoForge 21.1.x / Java 21
> 主模组 ID：`mio_icif`　根包：`com.miophas.singularity_iteration`
> API 兼容版本：`MioIcifAPI.API_VERSION = "1.0"`（可用 `MioIcifAPI.instance().getApiVersion()` 在运行时查询）
> 本次核对：2026-10-05，主模组 / core 开发包 `0.1.7.35`；以当前源码的实际行为为准。
>
> 本文档面向**附属（Addon）模组开发者**，介绍如何仅依赖 `core` 开发包，接入 mio_icif 的能源、机器、配方、流体、作物、升级、多方块等系统。
> 与 [core 接入与迁移指南](core_addon_guide.md)互补：本文侧重**从零搭建到各系统集成的完整开发流程**。代码片段省略常规 import、模型和语言资源；标为“片段”的内容需要接入附属自己的注册器和生命周期。

---

## 目录

1. [核心概念与包边界](#1-核心概念与包边界)
2. [工程接入（依赖与元数据）](#2-工程接入依赖与元数据)
3. [API 入口与基本约定](#3-api-入口与基本约定)
4. [最小可运行附属模组](#4-最小可运行附属模组)
5. [能源系统](#5-能源系统)
6. [机器系统（构建器 API）](#6-机器系统构建器-api)
7. [配方扩展](#7-配方扩展)
8. [流体系统](#8-流体系统)
9. [物品与装备](#9-物品与装备)
10. [橡胶树体系（橡胶木与树脂采集）](#10-橡胶树体系橡胶木与树脂采集)
11. [作物系统](#11-作物系统)
12. [多方块结构](#12-多方块结构)
13. [升级系统](#13-升级系统)
14. [事件系统](#14-事件系统)
15. [组合式组件（EnergyComponent / FluidAutomationComponent）](#15-组合式组件)
16. [平台能力注册](#16-平台能力注册)
17. [API 速查表](#17-api-速查表)
18. [线程、生命周期与缓存约束](#18-线程生命周期与缓存约束)
19. [常见错误与迁移](#19-常见错误与迁移)
20. [示例工程与验证命令](#20-示例工程与验证命令)
21. [核反应堆、HU / KU 与核爆边界](#21-核反应堆hu--ku-与核爆边界)
22. [主模组内容查询与配置](#22-主模组内容查询与配置)
23. [发布与升级核对](#23-发布与升级核对)

---

## 1. 核心概念与包边界

`mio_icif` 被拆分为两层：

| 包 | 角色 | 附属能否依赖 |
| --- | --- | --- |
| `com.miophas.singularity_iteration.core.api` | 公共契约（能源、机器、流体、作物、升级、配方、事件…） | ✅ 本文核对的公开契约；仍需验证宿主版本与实现支持 |
| `com.miophas.singularity_iteration.core.prefab` | 可继承的方块实体/物品/库存、装备特性与喷气飞行底座、可组合组件 | ✅ 在本文列出的范围内兼容 |
| `com.miophas.singularity_iteration.core.platform.neoforge` | 平台接入（能力注册、数据组件、音效） | ✅ 本文列出的接入入口 |
| `com.miophas.singularity_iteration.core.runtime` | 核心内部实现 | ❌ 不保证兼容，不要依赖 |
| `com.miophas.singularity_iteration.common` | 主模组具体内容实现 | ❌ 不要 import |

**铁律**：

- 只通过**接口**访问 API，不要强转或依赖 `*Impl` 实现类。
- 不要调用 `ApiServices.install(...)`、不要替换宿主的 bootstrap 配置。
- `@ApiStatus.Internal` 成员不属于兼容承诺。
- 包位于 `core`、方法为 `public`、类出现在开发 JAR 中，都不等于该实现是附属扩展点。源码明确写为内部实现的类同样应避开。
- `API_VERSION` 表示公开契约的兼容系列，不是模组版本，也不是所有新增方法、默认实现和游戏规则都已在旧宿主中存在的保证。

`checkCoreArchitecture` 拒绝 core 源码引用 `common`、旧包、客户端类及所列可选模组；附属示例不得引用 `common`，实际接入示例和 `PublicApiGameTests` 还不得引用 `core.runtime` 或内部桥接。宿主的部分 `*GameTests` 为引擎回归检查而允许访问内部实现，不能据此推导附属可用范围。

附属的客户端渲染、染色器、菜单画面和输入处理应放在自己的客户端入口。core 不含 Minecraft 客户端类；客户端业务与服务端接口分开加载。

---

## 2. 工程接入（依赖与元数据）

### 2.1 思路

玩家安装**完整模组** `mio_icif-<version>.jar`；附属**编译时**只需 core 开发包 `singularity-iteration-core-<version>.jar`。core 包不作为独立模组安装，**不要打进附属 JAR**。

### 2.2 依赖配置

本地文件方式：

```groovy
dependencies {
    // 编译期：仅 core 契约
    compileOnly files('libs/singularity-iteration-core-0.1.7.35.jar')
    // 开发运行期：完整模组（提供运行时实现）
    localRuntime files('libs/mio_icif-0.1.7.35.jar')
}
```

或走本地 Maven 仓库（主工程执行 `gradlew publishCorePublicationToMavenRepository` 后发布到 `repo/`）：

```groovy
repositories {
    maven {
        name = 'localCoreDevelopment'
        url = uri('repo')
        content { includeModule 'com.miophas.singularity_iteration', 'singularity-iteration-core' }
    }
}

dependencies {
    compileOnly 'com.miophas.singularity_iteration:singularity-iteration-core:0.1.7.35'
    localRuntime files('libs/mio_icif-0.1.7.35.jar')
}
```

> `localRuntime` 是 NeoForge ModDevGradle 的约定：仅运行时存在，不会传递给依赖你的模组。

上面两个版本应保持一致；升级宿主后同时替换开发包。`repo/` 路径相对于附属工程，需要指向主工程实际发布的仓库目录。开发包并非不依赖 Minecraft 的普通 Java 库：附属仍需要配置 Minecraft 1.21.1、NeoForge 与 Java 21 的开发环境。

### 2.3 `neoforge.mods.toml`

附属必须声明对 `mio_icif` 的**运行依赖**（`AFTER`）。下面以本次核对版本为最低依赖；其他元数据沿用示例工程：

```toml
modLoader="javafml"
loaderVersion="[4,)"
license="All Rights Reserved"
[[mods]]
modId="si_core_example"
version="1.0.0"
displayName="SI Core Development Example"

[[dependencies.si_core_example]]
modId="mio_icif"
type="required"
versionRange="[0.1.7.35,)"
ordering="AFTER"
side="BOTH"

[[dependencies.si_core_example]]
modId="neoforge"
type="required"
versionRange="[21.1,)"
ordering="NONE"
side="BOTH"

[[dependencies.si_core_example]]
modId="minecraft"
type="required"
versionRange="[1.21.1,1.21.2)"
ordering="NONE"
side="BOTH"
```

正式发布时应把 `versionRange` 收窄到实际验证过的主模组版本。

`MioIcifAPI.API_VERSION` 是编译期常量，可能被 Java 内联；要识别安装的提供者，应使用 `api.getApiVersion()`。`api.getVersion()` 返回宿主模组版本，`api.getModId()` 返回 `mio_icif`。不要仅比较 API 系列就跳过模组依赖检查。

### 2.4 发布通道（维护者）

- 本地可复现发布目标：项目内 `repo/`。
- 公网发布预留在 Gradle 属性 `coreReleaseRepositoryUrl`；认证用 `coreReleaseUsername` / `coreReleasePassword`（或 `ORG_GRADLE_PROJECT_*` 环境变量）。凭据不得写入源码或命令行。

---

## 3. API 入口与基本约定

所有功能的统一入口：

```java
MioIcifAPI api = MioIcifAPI.instance();
```

`instance()` 返回宿主装配的单例；**未初始化时抛 `IllegalStateException`**。因此不要在附属类的静态初始化阶段查询服务，应放到公共 setup / `ServerStartedEvent` 等宿主已装配之后。

可用子 API（`MioIcifAPI`）：

| 方法 | 返回 | 用途 |
| --- | --- | --- |
| `getEnergyNetAPI()` | `IEnergyNetAPI` | EU 电网查询、诊断、颜色通知 |
| `getFECompatAPI()` | `IFECompatAPI` | FE↔EU 适配（1 EU = 4 FE） |
| `getMachineAPI()` | `IMachineAPI` | 机器查询/控制/槽位/红石/多方块 |
| `getMachineBuilderAPI()` | `IMachineBuilderAPI` | 机器构建器（**推荐入口**） |
| `getItemAPI()` | `IItemAPI` | 模组物品、电池/工具/装甲，以及装备特性读写 |
| `getRecipeAPI()` | `IRecipeAPI` | 配方查询 |
| `getRecipeRegistrationAPI()` | `IRecipeRegistrationAPI` | 运行时注册配方 |
| `getFluidAPI()` | `IFluidAPI` | 流体访问 |
| `getFluidHandlerAPI()` | `IFluidHandlerAPI` | 机器流体存储查询/操作 |
| `getCropAPI()` | `ICropAPI` | 作物/种子注册 |
| `getBlockAPI()` | `IBlockAPI` | 方块访问 |
| `getRegistries()` | `IMioIcifRegistries` | 主模组内容查询 |
| `getHeatAPI()` | `IHeatAPI` | HU 热能 |
| `getKineticAPI()` | `IKineticAPI` | KU 动能 |
| `getReactorAPI()` | `IReactorAPI` | 反应堆 |
| `getWindAPI()` | `IWindAPI` | 全局风场 |
| `getRubberTreeAPI()` | `IRubberTreeAPI` | 橡胶树体系（橡胶木 / 树脂采集） |
| `getUpgradeAPI()` | `IUpgradeAPI` | 升级插件 |
| `getCapabilities()` | `IMioIcifCapabilities` | EU/HU/KU 能力工厂 |
| `getMultiblockAPI()` | `IMultiblockAccess` | 多方块发现/构建 |
| `getGeneratorAPI()` | `IGeneratorAPI` | ⚠️ 已废弃，用机器 API 替代 |

**废弃迁移**：`getGeneratorAPI()` 标记 `@Deprecated(forRemoval=true)`。发电机已并入机器体系——

- 新建 → `getMachineBuilderAPI().createGeneratorBuilder()`
- 查询 → `getMachineAPI().getMachineInfo(world, pos)`
- 启停 → `getMachineAPI().forceStart/forceStop(world, pos)`

### 3.1 API 子包索引

下表的包均相对于 `com.miophas.singularity_iteration.core.api`，用于选择接入契约；完整定义以链接源码为准。

| 子包 | 常用入口 / 类型 | 本文位置 |
| --- | --- | --- |
| `energy`、`energy.storage`、`energy.grid`、`energy.tile` | 节点描述、端口、多包、颜色、原生 EU / FE、远程开关与过压回调 | 第 5、16 节 |
| `machine`、`machine.builder` | `IMachineAPI`、机器定义、槽位与多方块 | 第 6、12、13 节 |
| `recipe` | `IRecipeAPI`、`IRecipeRegistrationAPI`、`IRecipeProcessingInfo` | 第 7 节 |
| `fluid`、`transport` | 流体端口、查询、管道材质和尺寸参数 | 第 8 节 |
| `item`、`item.electric` | 电池、装备特性、自动充电、省电与电动物品桥接 | 第 9 节 |
| `tool` | 工具模式、扳手交互、钻头判定 | 第 9.9 节 |
| `block`、`world` | 方块行为、橡胶木、采集器、风场 | 第 10、21 节 |
| `crop` | `PlantType`、`PlantStats`、`IPlanter`、作物注册 | 第 11 节 |
| `upgrade`、`upgrade.tile` | 升级效果与机器适用性 | 第 13 节 |
| `event` | 事件定义与实际发布范围 | 第 14 节 |
| `capability`、`heat`、`kinetic` | 能力常量、工厂、HU / KU 存储与输出 | 第 16、21 节 |
| `reactor` | 反应堆监控与元件属性 | 第 21 节 |
| `registry` | 宿主内容 ID 查询 | 第 22 节 |
| `network`、`util`、`armor` | 网络监听、HUD / 工具箱 / 防护声明；部分只有契约，需核对消费路径 | 第 9.8、18.2 节 |

尤其注意同名类型：`heat.IHeatStorage` 与 `IMioIcifCapabilities.IHeatStorage` 是不同接口；`upgrade.tile.IUpgradeItem`、`item.IUpgradeItem` 与能力工厂的嵌套升级接口也不能互换。优先使用完整限定名或显式 import，不用多个 `*` 导入来猜测类型。

---

## 4. 最小可运行附属模组

一个最小附属需要：注册自己的 Block / BlockEntityType，注册能力，注册能源节点描述。下面综合了 `src/addonExample/java/example/sicore/CoreExampleMod.java` 与 `InheritedSource` 的关键片段。

以下新增服务端 ticker，使示例每 tick 真正生成能量；`setAsPowerSource` 只声明输出，不会凭空补充库存。方块模型等资源由附属提供，片段可用 `/setblock si_core_example:source` 放置；如需创造栏物品，还要注册自己的 `BlockItem` 与创造栏条目。

### 4.1 方块实体（继承能源基类）

```java
public final class InheritedSource extends AbstractEnergyBlockEntity {
    public InheritedSource(BlockPos pos, BlockState state) {
        super(pos, state, CoreExampleMod.SOURCE_ENTITY.get(), 1024, 0, 32, CableTier.LV);
        setAsPowerSource(32); // 声明为电源，输出 32 EU/t
    }
    @Override protected boolean supportsFeCapability() { return true; } // 允许对 FE 设备输出
    public long generate(long amount) { return apiGenerateEnergy(amount, false); }
    public long stored()               { return apiGetStoredEnergy(); }
    public void serverTick() {
        if (level != null && !level.isClientSide) generate(32);
    }
}
```

### 4.2 注册器与模组构造

```java
@Mod(CoreExampleMod.ID)
public final class CoreExampleMod {
    public static final String ID = "si_core_example";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);

    public static final DeferredBlock<Block> SOURCE =
        BLOCKS.register("source", () -> new ExampleBlock(InheritedSource::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InheritedSource>> SOURCE_ENTITY =
        ENTITIES.register("source", () -> BlockEntityType.Builder
            .of(InheritedSource::new, SOURCE.get()).build(null));

    public CoreExampleMod(IEventBus bus) {
        // 1) 注册能源节点描述（必须在服务器首次启动前）
        EnergyNodeRegistry.register(id("source"), EnergyNodeProfile.generator(32));

        BLOCKS.register(bus);
        ENTITIES.register(bus);

        // 2) 注册能力
        bus.addListener(CoreExampleMod::capabilities);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        CoreCapabilityRegistration.registerEntity(event, SOURCE_ENTITY.get());
    }

    /** 附属自己实现的 EntityBlock。 */
    private static final class ExampleBlock extends Block implements EntityBlock {
        private final BiFunction<BlockPos, BlockState, BlockEntity> factory;
        private ExampleBlock(BiFunction<BlockPos, BlockState, BlockEntity> factory) {
            super(BlockBehaviour.Properties.of().strength(2));
            this.factory = factory;
        }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return factory.apply(pos, state);
        }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide || type != SOURCE_ENTITY.get()) return null;
            return (world, pos, currentState, entity) -> ((InheritedSource) entity).serverTick();
        }
    }
}
```

**要点**：
- `BlockEntityType` 用延迟注册器注册；能源描述在模组构造或 setup 注册，能力在附属的 `RegisterCapabilitiesEvent` 注册。不要在静态字段初始化时调用 `.get()` 解析尚未完成注册的 holder。
- 方块与方块实体**必须由附属自己注册**；core 不会替你注册 NeoForge 内容。
- 更多完整示例见 `src/addonExample/java/example/sicore/`（`InheritedSink`、`ComposedStorage`、`ComposedPacketSource`、`FluidMachine`、`ForeignCropSeed` 等）。

---

## 5. 能源系统

### 5.1 两条集成路径：继承 vs 组合

**路径 A：继承**（推荐）

可继承的基类（`core.prefab.blockentity`）：

| 基类 | 用途 |
| --- | --- |
| `AbstractEnergyBlockEntity` | 通用能源实体基类 |
| `AbstractGeneratorBlockEntity` | 燃料发电机骨架 |
| `AbstractEnergyStorageBlockEntity` | 储能箱骨架（电池/充放电/红石） |
| `AbstractProcessingMachineBlockEntity` | 加工机器骨架 |
| `AbstractStandardMachineBlockEntity` | 标准离散加工周期 |
| `AbstractHeatBlockEntity` / `AbstractHeatGeneratorBlockEntity` | HU 热能 |
| `AbstractKineticBlockEntity` / `AbstractKineticGeneratorBlockEntity` | KU 动能 |
| `AbstractTankBlockEntity` | 可垂直堆叠连通的储罐 |
| `AbstractWireBlockEntity` | 导线（需实现 `isWaterlogged`） |

**路径 B：组合**（无法改父类时）

实现 `EnergyComponentHost` 并持有 `EnergyComponent`：

```java
public final class ComposedStorage extends BlockEntity implements EnergyComponentHost {
    private EnergyPortPolicy ports =
        new EnergyPortPolicy(63 ^ (1 << Direction.EAST.ordinal()), 1 << Direction.EAST.ordinal(), false);
    private final EnergyComponent energy =
        new EnergyComponent(this, 1024, 32, 32, CableTier.LV, () -> ports);

    public ComposedStorage(BlockPos pos, BlockState state) {
        super(CoreExampleMod.STORAGE_ENTITY.get(), pos, state);
    }
    @Override public EnergyComponent energyComponent() { return energy; }
    public void closePorts() { ports = new EnergyPortPolicy(0, 0, false); energy.portsChanged(); }
}
```

组合方式**必须转发生命周期**（顺序很重要，见下表）：

| 方块实体方法 | 转发 |
| --- | --- |
| `setLevel` | `super.setLevel` 后 `energy.setLevel` |
| `onLoad` | `super.onLoad` 后 `energy.onLoad` |
| `setRemoved` | 先 `energy.setRemoved`，再 `super.setRemoved` |
| `clearRemoved` | `super.clearRemoved` 后 `energy.clearRemoved` |
| `onChunkUnloaded` | 先 `energy.onChunkUnloaded`，保留父类调用 |
| `saveAdditional` | 保留父类，再 `energy.save` |
| `loadAdditional` | 保留父类，再 `energy.load` |

能力注册用 `CoreCapabilityRegistration.registerComponent(event, MY_ENTITY.get())`。

### 5.2 注册能源节点描述

**必须在服务器首次启动前**（模组构造/setup）通过 `EnergyNodeRegistry` 注册：

```java
EnergyNodeRegistry.register(
    ResourceLocation.fromNamespaceAndPath("your_addon", "generator"),
    EnergyNodeProfile.generator(32)
);
```

`EnergyNodeProfile` 关键工厂（`core.api.energy`）：

- `EnergyNodeProfile.consumer()` — 纯消费者
- `EnergyNodeProfile.generator(packet)` / `generator(packet, dynamicTier)`
- `EnergyNodeProfile.storage(packet)`

约束：
- `register` 在注册表 `freeze()` 后会抛 `IllegalStateException`；重复 ID 抛 `IllegalArgumentException`。
- `CONDUCTOR` 角色必须提供损耗；`dynamicTier` 仅允许 `GENERATOR`。
- `EnergyComponent.onLoad()` 要求对应方块已注册 profile，否则抛 `IllegalStateException`。

### 5.3 方向端口与能量额度

`EnergyPortPolicy(inputs, outputs, generator)` 使用 **6 位方向掩码**，位序 = `Direction.ordinal()`：

```java
int allFaces  = 63;
int eastOnly  = 1 << Direction.EAST.ordinal();
new EnergyPortPolicy(eastOnly, 0, false);      // 只从东面输入
EnergyPortPolicy.consumer();                    // 六面输入
EnergyPortPolicy.source();                      // 六面输出，标记为电源
```

- 输入/输出允许为 0；`null` 方向表示聚合端口。
- `accepts(side)` / `emits(side)` 为判定方法。
- 方向策略变更后调用 `energy.portsChanged()` 以失效能力缓存并通知拓扑。
- 策略供应函数应**无副作用**；不要在传输回调里改变发电/储能角色。

**能量语义**（重要）：

- 机器**内部做功/发电**：`useEnergy` / `generateEnergy`（不受 `maxReceive`/`maxExtract` 限制）。
- **对外传输**：`euPort(side)` / `fePort(side)`（遵守方向、生命周期、共享额度）。
- 不要强转返回对象、访问 `energyStorage` 字段或内部账本。

**电压、吞吐量与共享预算要分开理解**：原生 EU 输入的 `maxReceive` 用于单包电压约束，不能直接解释为整 tick 的总 EU 输入上限。多个合规包可以在同一 tick 输入；FE 等外部端口仍有各自的额度约束。输出侧原生 EU 与 FE 提取共享 `maxExtract` 账本，不能分别设置预算后叠加输出。

### 5.4 原生 EU 多包输出

两种方式：

1. 继承能源实体时覆盖旧接口 `getPacketCount()`（0~64，0 = 停止原生输出），并保证库存充足、`maxExtract ≥ 包数 × 单包`。
2. 更精确：实现 `IEnergyPacketSource`：

```java
private EnergyPacketPolicy packets = new EnergyPacketPolicy(32, 8, true);
@Override public EnergyPacketPolicy getEnergyPacketPolicy() { return packets; }
```

`EnergyPacketPolicy(packetSize, packetCount, partialPackets)`：单包 EU、每轮包数、是否允许残包。上限 `EnergyPacketPolicy.MAX_PACKETS_PER_ROUND = 64`。

约束：
- 显式策略优先于 `getPacketCount()` 和默认单包描述。
- 策略 getter 必须无副作用、不得修改世界或存储；变更后调用 `energy.portsChanged()`。
- 原生 EU 与 FE 输出共享存储的输出额度；改变策略、拓扑或再查询能力不会重置已使用额度。
- `partialPackets=false` 要求开始一个包时有完整单包库存；已接受的包仍可分配给多个负载，剩余部分可在后续结算窗口处理。这不要求一个接收者一次吞下整个包。
- 64 包上限是当前引擎的实现限制，不是 IC2 的通用物理规则。包数是结算轮的策略参数，不能把每次 START / END 阶段调用都当成新一轮，再重复输出。
- 路由按工作预算分步完成，大网络可能跨 tick；同一轮后续窗口沿用初始报价，途中新增库存不能重复计入这一轮。附属不应调用内部引擎来强行刷新预算。

### 5.5 导线颜色与恢复诊断

- 导线/端点可实现 `IColoredEnergyTile`：`DyeColor getEnergyColor(Direction side)`，返回 `null` 表示通配色；同色或任一侧通配才允许连接。
- 主模组把未染色及黑色导线映射为通配色。附属要复用此规则，应在 getter 中把相应状态返回为 `null`；直接返回 `DyeColor.BLACK` 只是普通颜色值，接口不会替你映射。
- 改变颜色后，在**服务器线程**调用：

```java
MioIcifAPI.instance().getEnergyNetAPI().conductorColorChanged(entity);
```

此通知只处理电气拓扑。附属仍需保存颜色（`setChanged()`）、发送状态/方块实体更新，并使自己的模型与物品染色器读取相同颜色来源。能力方向同时改变时也要失效能力缓存。不要等待电源拆除才同步方块外观。

- 在服务器线程读取只读诊断快照：

```java
EnergyDiagnostics snapshot = MioIcifAPI.instance().getEnergyNetAPI().diagnostics(serverLevel);
```

`EnergyDiagnostics` 包含 `routing()`（拓扑/构建次数）、`recovery()`（重建/隔离/隔离区块/最近原因）、`timing()`（p50/p95/p99/max，纳秒）。`available() == false` 表示尚无引擎，此时计数为零。该查询**不会**创建引擎、加载区块或刷新能量预算。

### 5.6 电表与监控设备的采样口径

```java
NodeStats sample = MioIcifAPI.instance().getEnergyNetAPI().getNodeStats(level, pos);
if (sample != null) {
    double inputEu = sample.energyIn();
    double outputEu = sample.energyOut();
    double packetVoltage = sample.voltage();
    // 保存本次样本，由自己的菜单/网络消息同步到客户端。
}
```

`NodeStats` 是引擎的 tick 测量样本：`energyIn/energyOut` 是该样本的实际 EU 流量，`voltage` 是样本内最大原生 EU 包电压；FE 输入没有原生包电压，电压可为 0。引擎、节点或样本不可用时可返回 `null`，不要把它混同为确认停机的 0 流量。

连续采样时固定服务端采样阶段，用自己累计的有效 tick 样本数求平均；重复读取同一 tick 不能算多个样本。吞吐量不是缓存增长量，正在做功的机器可能同时收能与耗能。不要直接以 `energyIn / voltage` 推导展示电流，也不要访问内部的“当前 tick”采样方法。

### 5.7 EU / HU / KU 能力工厂

通过 `IMioIcifCapabilities` 创建符合主模组风格的能力实现：

```java
IMioIcifCapabilities caps = MioIcifAPI.instance().getCapabilities();

IMioIcifCapabilities.IEUStorage eu = caps.createEUStorage(10000, 32, 32, CableTier.LV);
IMioIcifCapabilities.IHeatStorage heat = caps.createHeatStorage(10000, 100, 100, 20, 1000, 0.01f);
IMioIcifCapabilities.IKineticStorage ku = caps.createKineticStorage(10000, 100, 100, 10000, 0.005f);
IMioIcifCapabilities.ICapabilityElectricItem item = caps.createElectricItem(100000, 512, 3, true);
```

一并可创建：`createTransformer`、`createPipe`、`createEnergyConverter`、`createWeapon`、`createSolarHelmet`、`createJetpack`、`createFluidCell`、`createUpgradeItem`，以及 `adaptHeatStorage` / `adaptKineticStorage`（包装方块实体内部存储，不支持时返回 `null`）。

### 5.8 导体端点、远程开关与过压回调

| 接口 | 当前契约与接入条件 |
| --- | --- |
| `ICableEnergyNode` | 继承 `energy.grid.IEnergyConductor`，通过 `isDirectionBlocked(Direction)` 描述单面断开；方向变化还需通知拓扑 |
| `IEnergyConductiveEndpoint` | 声明同一实体既是可穿过的导体又是原生能源端点；对应方块 ID 还必须注册为 CONDUCTOR，标记本身不会提供存储或注册节点 |
| `IEnergyEmissionSource` | `electricalContactPositions()` 声明一个权威电源的多个电气接触位置；不能把同一库存当作多个独立电源重复报价 |
| `IRemoteSwitchable` | `isRemotelyDisabled/setRemotelyDisabled` 用于能源终端远程开关；禁用保留库存，切断网络收发，机器工作逻辑也需遵守状态 |
| `energy.tile.IOverloadHandler` | `onOverload(int tier)` 在原生电网过压处理前调用，返回 true 可阻止该默认爆炸；参数为等级索引，不是 EU 数量 |
| `energy.tile.IExplosionPowerOverride` | `shouldExplode/getExplosionPower(tier, basePower)` 控制相关能源过压爆炸；不是第 21.5 节的核爆触发器 |

`AbstractEnergyBlockEntity` 已实现 `IRemoteSwitchable` 的状态保存与电气更新，加工基类会检查停用状态。使用自有工作引擎或组合组件时，附属自行停止做功、保存开关并更新端口。导体端点与多接触位置属于更复杂的接入形式，应同时验证导体描述、端口、颜色、唯一余额和卸载行为，不仅验证某次能力查询成功。

---

## 6. 机器系统（构建器 API）

### 6.1 推荐流程

NeoForge 要求 `BlockEntityType` 在初始化阶段注册，因此流程是：
**延迟注册自己的 Block/BlockEntityType → setup 中用构建器生成配置与定义 → 自己的实体读取配置并实现工作逻辑**。

声明机器定义的 setup 片段（`FLUID_MACHINE_ENTITY` 是附属已注册的类型，`configuration` 是附属保存的配置字段）：

```java
var builder = MioIcifAPI.instance().getMachineBuilderAPI()
    .createElectricMachineBuilder()
    .setName("fluid_machine")
    .setEnergyCapacity(10000)
    .setMaxReceive(32)
    .setMaxExtract(0)
    .setCableTier(CableTier.LV)
    .setEnergyPerTick(16)
    .setProcessTime(200)
    .useStandardLayout(1, 1, false, 0)
    .addFluidTank(8000)
    .addFluidTank(8000)
    .withEntityType(FLUID_MACHINE_ENTITY.get());

configuration = builder.getConfiguration();
builder.buildAndRegister("your_addon");
```

自己的实体可继承 `AbstractProcessingMachineBlockEntity`，并实现 `IMachineConfigurable.getConfiguration()` 暴露这份配置；构造器、库存、流体罐、工作 tick、NBT 和能力由附属实现。构建器不会给任意自定义实体自动创建罐体、执行配方或驱动回调。注册定义应在 setup 做一次，不要在每个实体构造时重复注册。

> `GenericMachineBlockEntity` / `GenericEnergyContainerBlockEntity` 是供构建器使用的内部通用实现，不应作为正式附属的继承底座。仓库 `FluidMachine` 仍直接继承通用实现，用于验证宿主构建器罐体行为；它不代表推荐的对外扩展路径。

### 6.2 构建器类型

`IMachineBuilderAPI` 提供 5 个工厂：

| 方法 | 构建器 | 用途 |
| --- | --- | --- |
| `createElectricMachineBuilder()` | `IElectricMachineBuilder` | 电力加工机器 |
| `createGeneratorBuilder()` | `IGeneratorBuilder` | 发电机 |
| `createHeatMachineBuilder()` | `IHeatMachineBuilder` | HU 热能机器 |
| `createKineticMachineBuilder()` | `IKineticMachineBuilder` | KU 动能机器 |
| `createEnergyContainerBuilder()` | `IEnergyContainerBuilder` | 储能方块 |

辅助方法与布局：
- `createStandardLayout(inputSlots, outputSlots, hasBatterySlot, upgradeSlots)`
- `createLayoutWithSpecialSlots(..., ISlotLayout.SlotType[] specialSlots)`
- `createFluidLayout(input, output, battery, upgrade, fluidTanks, tankCapacity)`
- `getDefaultsForTier(ICableTier)` → `IMachineDefaults`

`IElectricMachineBuilder` 链式配置：`setName` / `setTranslationKey` / `setBlockProperties` / `setEnergyCapacity` / `setMaxReceive` / `setMaxExtract` / `setEnergyPerTick` / `setCableTier` / `useDefaultsForTier` / `setProcessTime` / `setRecipeType` / `setSupportsUpgrades` / `setSupportsFluids` / `addFluidTank` / `setMachineType` / `addInputSlot` / `addOutputSlot` / `addBatterySlot` / `addUpgradeSlots` / `useStandardLayout` / `withEntityType` / `build` / `buildAndRegister` / `getConfiguration`，回调：`onWorkStart` / `onWorkComplete` / `onTick`。

### 6.3 机器定义与注册

```java
BlockEntityType<?> registerMachine(String modId, IMachineDefinition machine);
BlockEntityType<?> registerMachine(String modId, IMachineDefinition machine, BlockEntityType<?> type);
IMachineDefinition getMachineDefinition(String modId, String machineId);
boolean isMachineRegistered(String modId, String machineId);
```

`MachineConfiguration`（record）携带 `name`、`energyCapacity`、`maxReceive`、`maxExtract`、`energyPerTick`、`cableTier`、`processTime`、`recipeType`、`supportsUpgrades`、`supportsFluids`、`inputSlots`、`outputSlots`、`hasBatterySlot`、`upgradeSlots`、`fluidTanks`、`tankCapacity`、`machineType` 等。

**注意**：`registerMachine` 只把定义存入内部注册表，**不会**注册到 NeoForge；Block/BlockEntity/BlockEntityType 仍由附属自行注册。

当前电力构建器在 `build()` / `buildAndRegister()` 时要求已经通过 `withEntityType` 设置类型，否则抛 `IllegalStateException`。至少需要一个物品槽；容量与加工时间必须大于 0。不要按接口中残留的“类型可以为空”描述组织初始化顺序。`getConfiguration()` 可先取得配置，但定义构建必须等 holder 可解析后执行。

`onWorkStart/onWorkComplete/onTick` 会登记供通用机器实现使用的回调，不是覆盖所有机器的 NeoForge 事件。附属自己实现的工作引擎需要自行驱动相应行为。

### 6.4 自定义机器类型

`IMachineAPI.MachineType` 是固定枚举，无法运行时新增。需要带名字的自定义类型时：

```java
IMachineAPI.MachineTypeRegistry.register(new IMachineAPI.MachineTypeInfo(
    "mymod:quantum_assembler", "量子组装机", IMachineAPI.MachineType.PROCESSOR));
```

`IMachineBuilderAPI.registerCustomMachineType(modId, name)` 始终返回 `CUSTOM`，并用 `getRegisteredCustomMachineTypeName` 取回登记名。

### 6.5 直接继承加工机器基类

若不想用构建器，可继承 `AbstractProcessingMachineBlockEntity`（构造器 `(pos, state, type, capacity, maxReceive, maxExtract, maxProgress, slotLayout, energyPerTick, cableTier)`），**必须覆写**：

```java
protected abstract int[] getSlotsForDirection(Direction side);
protected abstract boolean canWork();
protected abstract void doWork();
```

加工/发电逻辑需由方块的**服务端 ticker** 调用相应基类 tick 方法：

```java
@Override
public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
    if (level.isClientSide || type != MY_MACHINE_ENTITY.get()) return null;
    return (world, pos, st, entity) -> ((MyMachine) entity).serverTick();
}
```

菜单由附属覆盖 `createMenu`，工作音效可覆盖生产机的 `getWorkSound`。

### 6.6 机器聚合 API 的查询与控制

`IMachineAPI` 同时继承多个子接口，可将同一个实例赋给对应子接口使用：

| 子接口 | 常用方法 |
| --- | --- |
| `IMachineQueryAPI` | `isMachine/getMachineInfo/getMachinesByType/getAllMachines/getCableTier` |
| `IMachineControlAPI` | `startMachine/stopMachine/forceStart/forceStop/isWorking/getMachineType/getMachineConfiguration` |
| `IMachineSlotAPI` | `getMachineSlotLayout/getMachineSlotItem/setMachineSlotItem/getMachineSlotCount` |
| `IMachineUpgradeOpsAPI` | `supportsUpgrades/getInstalledUpgrades/installUpgrade/removeUpgrade` |
| `IUpgradeStatsAPI` | 组合升级后的倍率、容量、等级及自动化统计 |
| `IGeneratorQueryAPI` | 燃料与燃烧进度查询 |
| `IMultiblockReactorAPI` | 已支持反应堆结构的查找与管理 |
| `IRedstoneAPI` | `getRedstoneModeEnum/setRedstoneModeEnum/isRedstoneAllowed/getRedstoneSignalStrength` |

`getMachineInfo/getMachineType/getMachineConfiguration/getMachineSlotLayout` 返回 `Optional`，不要照旧 Javadoc 示例直接当普通对象使用。启停与槽位写入可能返回失败；机器不支持某项控制时不能靠调用方法名强行实现。槽位索引来自实际 `ISlotLayout`，不是固定“0 输入、1 输出”。常规自动化优先使用对方的 sided item capability；`setMachineSlotItem` 是设置操作，不是带余量结算的插入交易。

---

## 7. 配方扩展

### 7.1 运行时注册

```java
IRecipeRegistrationAPI reg = MioIcifAPI.instance().getRecipeRegistrationAPI();

reg.registerCompressorRecipe(
    ResourceLocation.fromNamespaceAndPath("mymod", "compress_iron"),
    Ingredient.of(Items.IRON_INGOT),
    new ItemStack(ModItems.COMPRESSED_IRON.get()),
    200);
```

为一类机器提供一个 `registerXxxRecipe(...)`，签名规律为 `(ResourceLocation id, Ingredient input, ItemStack output, int processTime)`；多输出/特殊版本包括：

- `registerCentrifugeRecipe(id, input, ItemStack[] outputs, processTime)`
- `registerRecyclerRecipe(id, input, ItemStack[] outputs, float[] chances, processTime)`
- `registerOreWasherRecipe` / `registerElectrolyzerRecipe(id, input, ItemStack[] outputs, processTime)`
- `registerThermalCentrifugeRecipe(..., int heatRequired)`
- `registerElectricFurnaceRecipe(..., float experience)`
- `registerFusionReactorRecipe(id, Ingredient in1, Ingredient in2, output, long energyRequired, processTime)`
- `registerFluidSolidRecipe(id, Ingredient itemInput, String fluidInput /* "fluid_id:amount" */, output, processTime)`
- `registerWelderRecipe` / `registerPrecisionAssemblerRecipe`（有 `Ingredient[]` 重载，但当前实现仅接受长度 1；长度不是 1 时返回 `false`）
- `registerFluidRefiningRecipe(id, ingredient, FluidStack inFluid, FluidStack outFluid, processTime, energyPerTick)`

这些注册方法返回 `boolean`，应检查返回值并记录失败；重复 ID 会覆盖运行时条目，使用附属自己的命名空间。当前流体精炼注册会保留输入流体类型，但不保留 `inFluid` 的数量参数；需要精确规定输入 mB 时，不能只依赖这个快捷注册方法，应使用自己核对过的配方定义与 serializer。

### 7.2 关键生命周期

运行时注册的配方不会自动进入 `RecipeManager`：

```java
// 建议在 ServerStartedEvent 中调用
MioIcifAPI.instance().getRecipeRegistrationAPI().syncToRecipeManager();
```

查询端缓存在运行时注册/重载变化后需手动失效：`IRecipeAPI.invalidateCache()`。

当前 `syncToRecipeManager()` 在服务器存在时合并条目并自动失效宿主查询缓存；没有服务器时会跳过。数据包重载会替换 `RecipeManager` 内容，运行时配方需要在重载完成后按需再次同步。仅失效缓存不会把配方插入管理器；仅注册也不保证客户端配方查看器立即刷新。请在服务端线程完成同步，并单独处理附属需要的客户端展示。

### 7.3 查询

```java
IRecipeAPI recipes = MioIcifAPI.instance().getRecipeAPI();
recipes.getCompressorRecipeType();                 // RecipeType
recipes.findCompressorRecipe(input, level);        // Optional<RecipeHolder<T>>
```

- 多输入配方（焊接机/精密组装机/聚变）请用 `findRecipeMultiInput(RecipeType, ItemStack[], Level)`；旧的 `findRecipe(...)` 只校验第一个 Ingredient，多输入会误判（已废弃）。
- 自定义配方可实现 `IRecipeProcessingInfo` 暴露 `getProcessingTime()` / `getEnergyPerTick()`，避免依赖反射。

---

## 8. 流体系统

### 8.1 端口契约

机器只需实现 `IFluidPorts`（或 `ISeparateFluidPorts`），核心自动化即可像识别内置灌装机一样识别它：

```java
public interface IFluidPorts {
    IFluidHandler getInputFluidTank();
    IFluidHandler getOutputFluidTank();
    default IFluidHandler fluidPort(boolean input, @Nullable Direction side) { ... }
    default boolean routesBySide() { return false; } // 端口随方向变化时返回 true
    default int portCount() { return 2; }
}
```

此接口负责描述端口，不会自动注册 NeoForge 能力或 tick 自动化。附属仍需暴露 `Capabilities.FluidHandler.BLOCK`，并在自己的服务端 tick 中驱动所用组件；按面返回的 handler 必须与接口路由一致。

### 8.2 储罐合并与按面限制

- `FluidTankGroup`（`core.prefab.fluid`）：把多个槽合并为一个处理器，每个槽声明方向角色：

```java
new FluidTankGroup(List.of(
    new FluidTankGroup.Tank(inputHandler, FluidTankGroup.Role.INPUT_ONLY),
    new FluidTankGroup.Tank(outputHandler, FluidTankGroup.Role.OUTPUT_ONLY)
));
```

`Role`：`INPUT_ONLY` / `OUTPUT_ONLY` / `IO` / `INTERNAL`。**结构性保证**：输入槽不可被抽、输出槽不可被灌；`null` 方向表示任意面。

- `SidedFluidHandler(delegate, canFill, canDrain)`：只在能力边界限制，不改被包装处理器的余额。

- `FluidContainerInteraction.interact(player, hand, tankHandler)`：对齐 IC2 的流体单元交互（先容器→储罐，再储罐→容器，支持整叠）。

### 8.3 复用流体自动化

自有方块实体实现 `FluidAutomationComponent.Host` 并持有 `FluidAutomationComponent`，即可获得与内置机器一致的弹射/吸取行为与缓冲持久化：

```java
FluidAutomationComponent.Host host = new FluidAutomationComponent.Host() {
    public Level level() { ... }
    public BlockPos worldPosition() { ... }
    public IFluidHandler ownFluidHandler() { ... }
    public IFluidHandler adjacentFluidHandler(BlockPos pos, Direction side) { ... }
    public IMachineUpgradeStats upgradeStats() { ... }
    public void markUnsaved() { owner.setChanged(); }
    public void markNeighborUnsaved(BlockPos pos) {
        FluidAutomationComponent.markNeighborUnsaved(level(), pos);
    }
};
```

这是 Host 方法示意，`owner` 是自己的方块实体。默认脏标记回调是空操作，必须转发自己的脏标记，才能在传输后可靠保存；自己的服务端 ticker 每次调用一次 `runAutomation()`。流体弹出/吸取升级额度按邻接面计算，使用 `IMachineUpgradeStats.getFluidEjectorTransferLimit(side)` / `getFluidPullingTransferLimit(side)`；不要套用管道的“所有外部方向共用输出额度”规则。`fluidTransferPerStack(count)` 的当前值从单个升级的 50 mB/t 开始，后续按四倍递增并在计算档位上限处封顶。

`pendingInput()` / `pendingOutput()` 返回**防御性副本**；NBT 键 `scex_fluid_input_pending` / `scex_fluid_output_pending`。不要直接操作 `inputBuffer()` / `outputBuffer()`（已废弃/内部）。

缓冲属于组件的持久化状态，附属要转发其保存/加载方法，不能只保存自己的罐体。转移应按 `SIMULATE → EXECUTE → 核对实际接受量` 处理；模拟成功不代表执行必定接受相同数量。不要让自己的主动传输与升级组件各自重复抽取同一份流体。邻接查询使用对方朝向自己的面（`side.getOpposite()`），区块未加载时等待，不主动加载。

### 8.4 储罐基类

`AbstractTankBlockEntity<T>`：子类只需提供容量与实体类型；连通组默认按**具体子类**判定（不同材质不串组），多罐合并读写、放置均衡与 `FluidTank` NBT 由骨架处理。

### 8.5 流体查询

`IFluidHandlerAPI`：`hasFluidHandler`、`getFluidTankInfo(Level,pos,tank)`、`getAllFluidTanks`、`getTankCount`、`fillFluid`、`drainFluid`。

> ⚠️ `fillFluid` 的 `tank` 参数**仅用于验证兼容性**，NeoForge 的 `IFluidHandler.fill()` 会路由到第一个可接受槽。需要精确控制槽位时请直接使用 NeoForge capability。

### 8.6 气体流体

`GasFluidSource` / `GasFluidFlowing`（`core.prefab.fluid`）实现向上飘升、限高消失、水平扩散、禁止向下。约束：Source 与 Flowing 必须使用**相同的 `Properties` 与相同的 `maxHeight`**。

### 8.7 机器流体能力注册

```java
CoreCapabilityRegistration.registerFluidHandler(event, MY_ENTITY.get());
```

不支持流体时处理器返回 `null`，能力查询即视为不存在，机器不会被流体自动化顺带接管。

该便捷注册器当前只识别 `AbstractProcessingMachineBlockEntity` 和 `AbstractHeatBlockEntity`。如果是自有 `BlockEntity` 仅实现 `IFluidPorts`，需在自己的 `RegisterCapabilitiesEvent` 用 `event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, provider)` 注册按面 provider；仅调用便捷注册器会得到 `null`。

### 8.8 青铜 / 钢流体管道参数

`core.api.transport.PipeGrade` 与 `PipeSize` 是公开参数枚举，可用于附属自己的流体设备：

```java
int capacityMb = PipeGrade.STEEL.capacity(PipeSize.MEDIUM);           // 4800 mB
int outputMbPerTick = PipeGrade.STEEL.transferPerTick(PipeSize.MEDIUM); // 240 mB/t
int argb = PipeGrade.STEEL.color();                                  // 0xFF808080
int diameter = PipeSize.MEDIUM.diameterPixels();                     // 8 / 16 格
```

| 材质 | TINY 容量 / 速率 | SMALL 容量 / 速率 | MEDIUM 容量 / 速率 | LARGE 容量 / 速率 |
| --- | --- | --- | --- | --- |
| `BRONZE` | 400 / 20 | 800 / 40 | 2400 / 120 | 4800 / 240 |
| `STEEL` | 800 / 40 | 1600 / 80 | 4800 / 240 | 9600 / 480 |

表中单位分别为 mB 与 mB/t；容量倍率为 `1/6、1/3、1、2`，直径为 `4、6、8、10` 像素。材质基础容量青铜为 2400、钢为 4800；颜色分别为 RGB `(174,81,17)`、`(128,128,128)`。`transferPerTick` 描述面向非管道邻居的合计输出额度，不是每个方向独立获得一份额度。

主模组方块 ID 是 `mio_icif:pipe/fluid_pipe_bronze`、`mio_icif:pipe/fluid_pipe_steel`，四种尺寸使用状态属性 `size`。旧 `pipe/block_pipe_water` 保留用于存档兼容，不能当成新版青铜管道的别名。`IPipeBlock` 提供管道状态与抽取配置契约；仅实现它或使用参数枚举不会自动加入主模组内部管网。与具体管道交换流体请使用 NeoForge 流体能力。

---

## 9. 物品与装备

### 9.1 电池

`AbstractBattery extends Item`：

```java
protected AbstractBattery(Properties props, long maxEnergy, long initialEnergy, long chargeRate);
```

统一使用数据组件 `mio_icif:battery_energy`（`BatteryEnergy`）存储，实现 `IBatteryItem`（`getMaxEnergy/getEnergy/setEnergy/addEnergy/extractEnergy/isFull/isEmpty/getChargeRate`）。基类提供能量条和耐久处理，旧存储格式按基类迁移。**`addEnergy` / `extractEnergy` 仅对 `count == 1` 的堆生效。**

右键自动充电是额外的 `IEnergyDistributable` 契约：仅继承 `AbstractBattery` 并不会自动成为便携供电源。`0.1.7.35` 起右键切换自动模式，不再默认一次性分配电量，见第 9.10 节。

### 9.2 电动工具 / 护甲

- `AbstractElectricTool(Properties, maxEnergy, initialEnergy, chargeRate, energyPerUse, toolTier)`：实现 `IElectricToolItem`；方法 `hasEnoughEnergy` / `consumeEnergy` / `getRemainingUses` / `canUse`；内置手持物品栏（`HandHeldItems`）。
- `AbstractElectricArmor(Holder<ArmorMaterial>, Type, Properties, maxEnergy, initialEnergy, chargeRate, energyPerTick, armorTier, texturePrefix)`：实现 `IElectricArmorItem`；`texturePrefix` 形如 `namespace/path` → `namespace:textures/armor/path_1.png`（护腿为 `_2`）；可覆写 `getFeatures` / `getEnergyPerDamage` / `getDamageAbsorptionRatio`。

### 9.3 IC2 电动物品互操作

`BatteryElectricAdapter` 提供 SI ↔ IC2 桥接：

```java
BatteryElectricAdapter.asElectricItem(IBatteryItem);
BatteryElectricAdapter.asBatteryItem(IElectricItem, IElectricItemManager);
```

**严格约束**：查询期间 provider **不得修改物品**（否则抛 `IllegalStateException`）；禁止重入；仅对 `count == 1` 生效。

### 9.4 流体单元

`IFluidCellItem`（IC2 风格）：固定单格容量、单流体（不同流体拒绝填充）、单位 mB、`craftRemainder` 返还空单元。可用 `IMioIcifCapabilities.createFluidCell(int capacity)` 创建能力版本。

### 9.5 装备特性与喷气盔甲

电力装甲的"特性"（夜视、飞行、模式切换等）由物品**声明式**提供：工具提示、特性管理界面（Alt）、`/mio_icif toggle` 命令与网络包共用同一套公开契约，**不再识别具体物品类或硬编码特性键**。

**契约（`core.api.item`）**

| 类型 | 作用 |
| --- | --- |
| `IElectricArmorItem.getFeatures(ItemStack)` | 声明本物品的特性列表 |
| `ArmorFeatureInfo` | 特性声明 record：`slot` / `featureKey` / `featureNameKey` / `isMode` / `currentModeName` |
| `IArmorModeFeatureItem` | 模式型特性的切换入口（`cycleMode`） |
| `IJetpackItem` | 喷气背包契约；**已继承 `IArmorModeFeatureItem`** 并提供默认悬停↔飞行切换 |
| `IFeatureToolItem.getFeatures(ItemStack)` | 手持工具/武器的特性列表，与护甲共用分发和状态存储 |
| `ArmorFeatures.Extension` | 给物品类以外的来源追加特性，例如安装单元 |

**读写与切换（`IItemAPI`）**

```java
IItemAPI items = MioIcifAPI.instance().getItemAPI();

boolean on = items.isArmorFeatureEnabled(stack, "night_vision");    // 读取开关状态（默认 true）
items.setArmorFeatureEnabled(stack, "night_vision", false);         // 直接设置
items.toggleArmorFeature(stack, "night_vision");                    // 开关型取反，返回新状态
items.cycleArmorFeature(stack, "jetpack_mode");                     // 模式型切到下一模式
ArmorFeatureInfo info = items.getArmorFeatureInfo(stack, "flight"); // 查询特性声明
```

也可直接使用 `core.prefab` 的帮助类（适合在物品自身逻辑中调用）：

- `ArmorFeatureState.isEnabled(stack, key)` — 读取开关状态（CustomData 键 `feat_<key>`，未指定默认值的键默认启用）；
- `ArmorFeatures` — 声明查询、`isEnabled` / `toggle` / `cycleMode` / `toggleOrCycle`；
- `ArmorFeatureTooltip.appendAll(tooltip, stack, features)` — 渲染**可点击**（`/mio_icif toggle`）的特性行。

**最小示例：带开关特性的自定义胸甲**

```java
public final class MyBackpackArmor extends AbstractElectricArmor {
    public MyBackpackArmor(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties, 1_000_000, 0, 512, 0, 3, "mymod/my_backpack");
    }

    @Override
    public List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return List.of(
            new ArmorFeatureInfo(EquipmentSlot.CHEST, "my_feature", "tooltip.mymod.my_feature")
        );
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide && entity instanceof Player player
                && player.getItemBySlot(EquipmentSlot.CHEST) == stack
                && ArmorFeatureState.isEnabled(stack, "my_feature")) {
            // 特性逻辑
        }
    }
}
```

- 继承 `AbstractElectricArmor` 并实现 `getFeatures` 后，Tooltip 会自动带上可点击的开关/模式行，特性管理界面也会自动收录——无需任何 GUI 或网络代码。
- 开发**喷气盔甲**：物品额外实现 `IJetpackItem`（背槽还需 `IBackSlotItem`），在 tick 中调用
  `JetpackFlightController.tick(player, stack, this, hoverCost, maxAscent, hoverAscent, hoverDescent, advanced)`
  即可复用与主模组一致的飞行物理与按键输入（两者均位于 `core.prefab.flight`）。
- **槽位覆盖**：装备控制台支持四格护甲、主/副手，以及可选的 Curios 背槽和其他饰品槽。宿主解析 `head/chest/legs/feet/mainhand/offhand/back`；饰品 ID 使用内部槽位映射，附属不依赖该映射类。未安装 Curios 时保留原版装备与手持物功能。

**语义约定**

- 开关型特性未显式写入时按 `ArmorFeatureState.defaultFor(key)` 读取；未注册的键默认启用（`DEFAULT_ENABLED = true`）。可在初始化时调用 `ArmorFeatureState.registerDefault("your_addon:careful_mode", false)` 注册默认关闭的键；这不会覆盖已经保存的玩家选择。
- 模式型特性没有开关状态（`isArmorFeatureEnabled` 恒为 `true`），只能用 `cycleArmorFeature` / `ArmorFeatures.cycleMode` 切换。
- 默认值注册表按特性键全局共享，应使用附属命名空间，注册一次，避免覆盖宿主或其他附属的同名键。重命名特性键需要附属自己的存档迁移；宿主 `viltrum_flight → special_maneuver` 的更名没有自动迁移旧值。
- `ArmorFeatures.registerExtension(extension)` 追加特性声明；默认值、实际行为和模式切换仍由附属实现。返回空列表表示不适用，避免同一物品重复声明相同 featureKey；扩展不会自动赋予宿主套装单元或 FCS 模拟能力。

### 9.6 电动物品扣费与外部装备槽

`core.api.item.BatteryTransfer` 可复用严格的能量结算：

- `consume(stack, battery, amount)`：检查完整扣费，成功返回 `true`；会应用 `EnergySaving`，调用者不要预先再乘一次节能倍率。
- `move(source, from, target, to, amount)`：暂存源与目标物品变化，核对余额后提交，返回实际转移 EU；相同物品栈或非单件堆不转移。
- `transfer(stack, battery, amount, charging, simulate)`：验证返回值与实际余额变化一致；调用者仍需自行制定充放电速率政策。
- `canProvideExternally(stack)`：外部供电权限与工具自耗电不同，不能因为工具有电就把它当成电池输出源。

额外装备槽可通过 `BatteryTargetProviders.register(provider)` 把玩家实际持有的目标物品追加到列表，不必让电池实现依赖可选槽位模组。provider 不应追加复制品、重复目标或 `excluded` 源物品，也不应在查询时修改物品；注册一次即可。

### 9.7 工具特性与可选择的模式

工具/武器实现 `IFeatureToolItem`，在 `getFeatures` 中声明 `EquipmentSlot.MAINHAND` 特性；宿主装备控制台会按实际主/副手物品解析，不要求复制一套 OFFHAND 声明。开关读写仍使用 `ArmorFeatures` / `ArmorFeatureState`。工具基类不会自动追加特性 Tooltip，需要附属调用帮助类：

```java
public final class AddonFeatureTool extends AbstractElectricTool
        implements IFeatureToolItem, IEquipmentHudProvider {
    public static final String CAREFUL = "your_addon:careful_mode";

    public AddonFeatureTool() {
        super(new Properties(), 100_000, 0, 128, 32, 2);
    }

    @Override public List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return List.of(new ArmorFeatureInfo(EquipmentSlot.MAINHAND, CAREFUL,
            "tooltip.your_addon.feature.careful_mode"));
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ArmorFeatureTooltip.appendAll(tooltip, stack, ArmorFeatures.features(stack));
    }

    @Override public Component getEquipmentHudText(ItemStack stack) {
        boolean careful = ArmorFeatureState.isEnabled(stack, CAREFUL);
        return Component.translatable(careful ? "hud.your_addon.careful" : "hud.your_addon.normal",
            getEnergy(stack));
    }
}
```

该例只展示特性声明、提示与 HUD。附属的实际使用行为仍要检查 CAREFUL 状态；这些方法不会自动改变挖掘/攻击规则。翻译和物品资源由附属提供，若要求 CAREFUL 默认关闭，在模组初始化时注册默认值。

模式选择与布尔特性是两种契约：

- `IArmorModeFeatureItem.cycleMode`：装备特性列表中的模式项。
- `tool.IToolModeProvider`：工具操作模式列表，继承 `ToggleableElectricTool`。实现 `toolModes/toolModeIndex/toggleActive`，可选实现 `toolModeDetails/selectToolMode`。
- `ToggleableElectricTool` 的 `toggleActive` 接入宿主 G 键切换路径；`IToolModeProvider` 还提供装备控制台的直接选择。默认 `selectToolMode` 最多循环模式数次，附属可覆写为一次直接切换，服务端校验索引并同步状态。

模式列表的顺序、当前索引与名称需一致，getter 不做切换或扣费。两套模式可由同一物品实现，但不会自动共享存储，需由附属明确关联。

### 9.8 装备 HUD、信息接口与防护声明

`IEquipmentHudProvider.getEquipmentHudText(stack)` 返回一行 `Component`；返回 `null` 表示隐藏。`keepEquipmentHudVisible(stack, player)` 默认 false，返回 true 只让该装备自己的行常显。

当前宿主只轮询主手、副手、头、胸、腿、脚六槽。每行文字变化后显示 60 客户端 tick；只改变数字不会重开显示窗口，多行有效时每 40 tick 轮换。当前没有对任意 Curios 槽的 HUD 自动轮询；装备控制台支持饰品槽不等于 actionbar HUD 也支持。

HUD 回调由客户端执行，应读取已有同步状态，不发送每帧请求、不修改服务端余额，也不自行每 tick 抢占 actionbar。接口在公共包不意味着物品公共类能无条件引用 Minecraft 客户端类；额外客户端渲染代码放在客户端入口。

| 其他契约 | 说明与当前限制 |
| --- | --- |
| `util.IItemHudInfo` | `getHudInfo(stack, extended)` 返回信息行，嵌套条形接口返回 0～100；不同于 actionbar 提供者，不自动接入上述轮询 |
| `IEnhancedOverlayProvider` | 声明某方块/面是否提供增强覆盖层；当前未找到通用渲染消费路径，不能只实现它就承诺出现覆盖层 |
| `armor.IHazmatLike` | 单件物品的 `providesHazmatProtection(entity, slot, stack)` 防护声明；不等于主模组所有辐射路径都自动认可自定义装备 |
| `armor.IMetalArmor` | `isMetalArmor(stack, entity)` 查询金属护甲属性；不是护甲伤害吸收或辐射屏蔽算法 |
| `item.IMetalArmor` | 另一份金属护甲契约，参数是 `Player`；与 `armor.IMetalArmor` 不是同一个类型 |

当前食人作物针对玩家的护甲同时识别上述两份金属护甲接口，以及 `mio_icif:metal_armor` 物品标签，用于判断是否施加附加负面效果。不要仅凭接口注释推断自动获得电击、充电效率或辐射防护加成。

当前 `IHazmatLike.hasCompleteHazmat(entity)` 不拒绝未实现接口的空槽/普通护甲，不能作为严格“四件装备齐全”的判断。附属需要完整套装条件时，应逐一检查四格非空、均实现该接口且各件返回 true。这里记录现有实现限制，没有改动宿主防护代码。

### 9.9 扳手、工具标记与工具箱

`tool.IWrenchable` 可以由方块或方块实体实现，提供 `getFacing/setFacing/canSetFacing/wrenchCanRemove/getWrenchDrops`。默认朝向实现识别原版 `FACING/HORIZONTAL_FACING`，水平属性不能设为上下方向；没有对应属性时不具备默认旋转行为。方向变化后，附属还要更新自己的能力与电网端口。

`item.IWrenchItem`、`ICutterItem`、`IEUMeterItem` 等标记用于类型识别；自定义物品的右键、耐久、扣费和模型仍需自己实现。`util.IBoxable.canBeStoredInToolbox(stack)` 声明工具箱适用性，`util.IDebuggable` 声明调试信息，不代替具体工具的交互实现。不要从空标记接口推导自动获得主模组工具的整套行为。

扳手默认掉落使用方块 loot；库存保存与完整机器拾取不是 `getWrenchDrops` 的自动保证。附属自定义机器应核对掉落、库存、数据组件、拆除权限与服务端操作，避免既封装库存又在方块移除时重复散落物品。

### 9.10 电池自动充电与充电优先级（0.1.7.35）

继承基类的最小便携电池：

```java
public final class AddonBattery extends AbstractBattery implements IEnergyDistributable {
    public AddonBattery() {
        super(new Properties(), 100_000, 0, 128);
    }
}
```

基类已转发 `use` 到 `handleDistributeUse`，并在 `inventoryTick` 调用 `BatteryAutoCharge.tick`；不要再次手动调用同一 tick 的充电。自有电池直接实现 `IBatteryItem` 与 `IEnergyDistributable` 时，还需自行转发 `use`，每个服务端 tick 调用一次 `BatteryAutoCharge.tick(player, stack, this, getChargeRate(), slotId)`。模式默认关闭，只有 count=1 生效；`supportsAutoCharge=false` 的工具类保留自己的右键。

自动模式保存于 CustomData `SiAutoCharge`，通过 `isOn/toggle` 使用；当前按键的存在判定开启状态，关闭时删除键，不要写入 `false` 以为关闭。每 10 tick 按槽位错开运行一次，预算为电池充电速率 ×10 与余额中的较小值。目标是实际玩家库存/护甲中的 `IElectricToolItem` 或 `IElectricArmorItem` 单件物品，不会给任意 FE 物品自动充电。

`BatteryAutoCharge.isChargeSource` 排除自动模式物品、支持自动模式的电池和 `IEnergyPackItem` 能量背包，防止来源互相充电；太阳能头盔也跳过这些来源。当前自动充电只在安装 Curios 时追加 `BatteryTargetProviders` 的饰品目标，其他可选槽位集成需单独核对调用路径。四种宿主能量背包使用潜行+右键切换，普通右键仍负责穿戴；空的 `IEnergyPackItem` 标记不会给自定义护甲自动实现这一交互。

`IEnergyDistributable.distributeEnergyToItems(...)` 的手动分配方法仍存在，但不再是当前默认右键策略，也不能代替自动模式的防循环规则。

`ChargePriority` 为 `OFF/LOW/NORMAL/HIGH`，通过 `of(stack)` 和 `set(stack, priority)` 读写，默认 NORMAL；宿主充电站按高优先级先充，OFF 跳过。它不是全部充电路径的强制政策：当前 `BatteryAutoCharge.tick` 不读取该优先级。附属自己的共享充电器要自行决定是否遵守并排序。

### 9.11 “省电”附魔与扣费（0.1.7.35）

公开帮助类为 `core.api.item.EnergySaving`，附魔 ID `mio_icif:energy_saving`，支持 I～V：每级降低 5%，系数依次为 0.95、0.90、0.85、0.80、0.75。

| 方法 | 用途 |
| --- | --- |
| `level(stack)` | 读取等级，无附魔为 0，最高按 V 处理 |
| `factor(level)` | 确定性的展示/估算系数 |
| `apply(stack, amount)` / `apply(level, amount)` | 本次动作的实际整数费用，小数 EU 按概率补一个 EU，长期平均保持比例 |
| `rate(stack, rate)` | 每 tick / 每伤害点费率的四舍五入版本；正费率不低于 1，与 apply 的概率语义不同 |

`AbstractElectricTool.consumeEnergy`、`AbstractElectricArmor.consumeEnergy` 与 `BatteryTransfer.consume` 已应用省电，传入原始费用即可；充电与物品间转移不打折。直接 `extractEnergy` 是余额交易，不自动代表付费动作，附属自己的功能要选择正确扣费入口。

`apply` 有随机舍入，不能在模拟、提示、预检查和执行阶段反复调用来假定同一整数费用。显示预期值用 `factor`，实际服务端扣费调用一次并根据成功结果执行动作。原有 `hasEnoughEnergy` 仍可能按原始费用检查，不能把它当成省电后的最终授权；要以实际扣费返回值为准。

自定义电动工具/护甲需要加入附魔适用物品标签 `mio_icif:enchantable/electric`（附属数据包在该标签下追加物品，`replace=false`）；仅继承电动基类不会自动进入宿主标签。充电背包只供电，不应因此获得耗电折扣。宿主的量子套装单元、特殊机动和 FCS 实现在 `common`，其内容使用了省电帮助类，但没有因此成为通用套装/飞行 API。

---

## 10. 橡胶树体系（橡胶木与树脂采集）

橡胶树体系由**橡胶木方块**、**树液采集器物品**与**采集/再生规则**组成。核心算法位于 `core.runtime.world.RubberTreeSystem`，并通过 `IRubberTreeAPI` 暴露；方块与物品仅通过接口参与，因此你可以：

- 用现成采集器采集任意实现了 `IRubberWood` 的原木；
- 让自定义采集器实现 `ITreeTapItem` 后即可用于采集橡胶木；
- 在手写交互中调用 `IRubberTreeAPI.tryHarvest(...)` 复用统一的采集流程。

### 10.1 接口一览

| 类型 | 位置 | 作用 |
| --- | --- | --- |
| `IRubberTreeAPI` | `core.api.world` | 体系入口：判定 + 采集 + 再生 |
| `RubberHarvestResult` | `core.api.world` | 采集结果 record（`harvested` / `resinCount`） |
| `IRubberWood` | `core.api.block` | 橡胶木方块接口（含树脂 / 采集口 / 状态修改） |
| `ITreeTapItem` | `core.api.item` | 树液采集器标记接口（含掉落区间与耐久损耗默认值） |

### 10.2 IRubberWood（自定义橡胶木）

实现 `IRubberWood` 即可让自定义原木被采集器识别：

```java
public final class MyResinLog extends Block implements IRubberWood {
    public static final BooleanProperty RESIN = BooleanProperty.create("resin");

    @Override public boolean hasResin(BlockState s)  { return s.getValue(RESIN); }
    @Override public boolean isTappable(BlockState s) { return true; }
    @Override public BlockState withResin(BlockState s, boolean has) { return s.setValue(RESIN, has); }
}
```

- `hasResin`：当前是否含有可采集的树脂。
- `isTappable`：结构条件（例如是否已形成采集口）；只有 `isTappable && hasResin` 才可被采集。
- `withResin`：返回修改树脂后的状态，采集与再生都会调用它。
- `canRegrowResin` 默认实现为 `isTappable && !hasResin`，可按需覆写。

### 10.3 ITreeTapItem（自定义采集器）

实现 `ITreeTapItem` 即被认定为树液采集器：

```java
public final class MyTap extends Item implements ITreeTapItem {
    @Override public int getDurabilityCost() { return 1; } // 0 表示不耗耐久
    @Override public int getResinDropMin()   { return 2; } // 含
    @Override public int getResinDropMax()   { return 5; } // 含
}
```

默认值对应普通木质采集器（1 点耐久、1~3 个树脂）。若物品**同时**实现 `IElectricToolItem`（例如继承 `AbstractElectricTool`），`IRubberTreeAPI` 与橡胶木会把它当作**电动采集器**：改为消耗 `getEnergyPerUse()` 电能、能量不足时拒绝采集，此时应把 `getDurabilityCost()` 返回 0。

### 10.4 判定与采集

```java
IRubberTreeAPI rubber = MioIcifAPI.instance().getRubberTreeAPI();

// 纯查询（不修改世界）
boolean isTap  = rubber.isTreeTap(stack);        // 是否树液采集器
boolean isElec = rubber.isElectricTreeTap(stack); // 是否电动树液采集器
boolean isWood = rubber.isRubberWood(state);      // 是否橡胶木

if (rubber.canUseTapOn(state, stack)) {           // 可采集 + 工具可用（电动需有电）
    RubberHarvestResult r = rubber.tryHarvest(level, pos, state, player, hand);
    if (r.harvested()) { /* r.resinCount() 为掉落数量 */ }
}
```

- `tryHarvest` **仅在服务端**执行：消耗耐久/电能、掉落树脂并把方块置为无树脂；客户端或条件不满足时返回 `RubberHarvestResult.NONE`。
- `rollResinCount(tool, random)` 只掷数量、不改世界；`dropResin(level, pos, player, count)` 按默认规则在方块与玩家连线中点掉落树脂。
- `getResinItem()` / `createResinStack(count)` 获取树脂物品（`resource/item_harz`）。

### 10.5 再生

橡胶木的随机刻再生由 `IRubberWood.canRegrowResin` 与 `IRubberTreeAPI.getResinRegrowChance()`（默认 0.05）控制。自定义方块可在自己的 `randomTick` 中复用公开入口：

```java
@Override
protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    MioIcifAPI.instance().getRubberTreeAPI().tryRegrowResin(level, pos, state, random);
}
```

### 10.6 行为参数与迁移

| 采集器 | 耐久/次 | 电能/次 | 掉落 |
| --- | ---: | ---: | --- |
| 木质 `item_tool_wooden_treetap` | 1 | — | 1~3 |
| 电动 `item_tool_treetap_elc` | 0 | 10 EU | 2~4 |

- **迁移提示**：橡胶木的采集判定已从「物品名字符串包含 `treetap`」改为 `ITreeTapItem` 接口判定。曾依赖命名约定的第三方物品不再生效，请改为实现 `ITreeTapItem`。
- `ITreeTapItem` 由空标记接口扩展出三个默认方法，既有实现类无需修改即可继续编译。

### 10.7 自动树脂收集器与自定义原木

主模组树脂收集器 ID 为 `mio_icif:producer/block_resin_collector`：背面对准湿润橡胶木的采集正面，每次清除湿润状态并存入 **1 个**树脂；一个仅接受树脂的 EXTRA 槽，满槽时不消耗采集点，无需 EU。可通过其 NeoForge 物品能力进行自动化，不需要 import `common` 的实体或菜单类。

`IRubberWood` 本身没有“采集正面”方法。当前主模组收集器除接口判定外，还要求原木具有相同的 `BlockStateProperties.HORIZONTAL_FACING` 属性，并与收集器的朝向匹配。自定义原木只有 `IRubberWood` 而没有该属性时，手动采集 API 可以工作，内置自动收集器不会采集它。

附属自己的无人采集器不要构造假玩家调用 `tryHarvest`：该方法是带玩家/工具消耗与地面掉落的交互。可通过 `IRubberWood` 判定、`withResin(state, false)` 和 `IRubberTreeAPI.createResinStack(1)` 实现自己的服务端操作；先检查目标库存，再清除树脂状态并核对插入结果，失败时恢复，避免重复收取。保存和客户端同步也由附属负责。此组合没有统一的公共事务/方向 API。

---

## 11. 作物系统

### 11.1 注册植物

```java
MioIcifAPI.instance().getCropAPI().registerPlant(new MyPlant());
```

- 使用 IC2 Experimental 规则的作物继承 `Ic2PlantType`（`core.prefab.crop`）：`usesIc2CropCycle()` 返回 `true`，宿主作物架每 256 游戏 tick 调用一次特殊 `tick(IPlanter)`，随后统一计算环境/生长点/阶段推进/资源衰减。
- `getGrowthTime` 的单位是**生长点**，默认 `tier * 200`；不要在特殊 tick 内重复推进整套生长引擎。
- 直接继承 `PlantType` 的作物默认 `usesIc2CropCycle() == false`，保留逐游戏 tick 回调与原有生长单位。

### 11.2 种子物品

自定义种子实现 `ICropSeedItem`（默认方法读取 SI 的 CUSTOM_DATA 键，可覆写使用自己的存储格式）：

```java
public final class ForeignCropSeed extends Item implements ICropSeedItem {
    @Override public String cropModId(ItemStack stack) { return "mio_icif"; }
    @Override public String cropId(ItemStack stack) { return "wheat"; }
    @Override public int cropScanLevel(ItemStack stack) { return cropData(stack).getInt("AddonScan"); }
    @Override public void setCropScanLevel(ItemStack stack, int level) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("AddonScan", level));
    }
}
```

内置分析仪通过这些方法工作，**不要求**继承 common 的 `CropSeedItem`。

### 11.3 基础种子

`ICropAPI.registerBaseSeed(seed, plantType[, growthRate, gain, resistance, weedEx])` 注册基础种子。基础种子注册的 `ItemStack` **数量**会用于种植成本（例如四个花卉），不能仅当作展示信息。

基础种子匹配比较物品与组件，不要求玩家手持数量恰好等于注册数量；实际种植仍需满足消耗数。简化重载从阶段 1、三属性 0 开始；`weedEx` 当前保留而未参与规则。植物 ID 为 `getModId() + ":" + getTypeId()`；同 ID 植物会覆盖旧条目，基础种子则按列表追加，附属应使用自己的 ID，并只注册一次。

### 11.4 最小 IC2 周期作物

```java
public final class MyCrop extends Ic2PlantType {
    @Override public String getModId() { return "your_addon"; }
    @Override public String getTypeId() { return "example_crop"; }
    @Override public String[] getTraits() { return new String[] {"Green", "Food"}; }
    @Override public PlantStats getStats() { return new PlantStats(1, 0, 1, 0, 1, 0); }
    @Override public int getMaxGrowthStage() { return 4; }
    @Override public ItemStack[] getHarvest(IPlanter planter) {
        return new ItemStack[] {new ItemStack(Items.WHEAT)};
    }
    @Override public String getTexture(int stage) {
        int visibleStage = Math.clamp(stage, 1, 4);
        return "your_addon:block/crop/example_crop_" + visibleStage;
    }
}

// 公共 setup 的串行工作中执行；种子物品须已注册。
ICropAPI crops = MioIcifAPI.instance().getCropAPI();
MyCrop crop = new MyCrop();
crops.registerPlant(crop);
crops.registerBaseSeed(new ItemStack(MY_BASE_SEED.get()), crop, 0, 0, 0, 0);
```

`getTexture` 返回方块图集中的精灵 ID（如 `your_addon:block/crop/example_crop_1`），不含 `textures/` 或 `.png`。附属提供 `assets/your_addon/textures/block/crop/example_crop_1.png` 等纹理，并确保精灵加入 `minecraft:blocks` 图集（通过方块模型引用或图集 source 声明）；仅提供 PNG 文件不保证精灵可用。`getTraits` 使用稳定的类别名参与杂交相似度计算，不应返回按语言变化的显示名。

### 11.5 属性、回调与收获约定

| 数据 | 含义与范围 |
| --- | --- |
| `PlantStats` 第一参数 / `getLevel()` | 物种 tier，与种袋三属性不同 |
| `PlantStats` 后五参数 | IC2 顺序：chemical、food、defensive、colorful、weed；旧构造参数名为 chemistry、nutrition、color、medicinal、danger |
| `getDefensive/getColorful/getWeed` | 对应旧字段的新语义别名；不要按旧 getter 名字误解位置 |
| `IPlanter.getGrowthSpeed/getYield/getResilience` | 个体的生长 / 产量 / 抗性，0～31 |
| `IPlanter.getProgress` | IC2 周期作物的累计生长点，不能当作已运行游戏 tick |
| `cropScanLevel` / `getScanLevel` | 分析等级 0～4，与种袋数量无关 |
| `IPlanter.makeSeeds(type, count, growth, gain, resistance)` | 第二参数是种袋堆叠数量；分析等级来自当前作物架 |

常用物种扩展方法：`canGrow`、`getGrowthTime`、`weightInfluences` 控制成长；`canHybridize` 控制亲本资格；`getGains/dropGainChance/calculateDropCount` 控制产出；`getStageAfterHarvest(IPlanter)` 控制收获后阶段；`onInteract/onLeftClick/onCollision`、`getRedstoneSignal/getLightEmission` 控制特殊行为。使用 `IPlanter` 读写，不直接访问宿主作物实体字段；改变需展示的状态后调用 `updateState()`。

IC2 周期下 `tick(IPlanter)` 是每 256 tick 的特殊行为回调；默认不自行推进生长。直接继承 `PlantType` 时默认 tick 已包含旧式生长逻辑，自定义实现不要又调用一次宿主周期成长。种袋的 `cropData(stack)` 返回副本，修改它不会保存；使用 `CustomData.update` 或自己的存储 setter，并同时实现 `cropModId/cropId/cropGrowth/cropGain/cropResistance/cropScanLevel/setCropScanLevel` 的一致读写。

当前没有公共“指定杂交配方表”的注册入口：物种与 traits、stats、阶段资格参与宿主规则。分析仪能够识别 `ICropSeedItem` 不代表该种子自然注册成基础种子；物种注册、基础种子注册与种袋数据接口是三件独立工作。

---

## 12. 多方块结构

### 12.1 构建与查找

```java
IMultiblockStructure structure = IMultiblockStructure.builder()
    .validator(new MyValidator())
    .onFormed((level, pos) -> { /* 成形 */ })
    .onBroken((level, pos) -> { /* 拆除 */ })
    .buildAndRegister(level, controllerPos);

// 按主控或成员位置查找
IMultiblockStructure found = IMultiblockStructure.find(level, pos);

// 拆除并触发 onBroken
found.invalidate(level);
```

- `IMultiblockValidator.validate(Level, BlockPos)` 返回 `IMultiblockValidationResult`（可用 `success(...)` / `failure(msg)` / `builder()`）。
- `IMultiblockBuilder`：`validator` / `onFormed` / `onBroken` / `buildAndRegister`。

### 12.2 自动重建与控制器注册

希望方块变化时自动重建结构，需在**公共 setup 阶段、首次启动世界前**注册控制方块：

```java
MioIcifAPI.instance().getMultiblockAPI()
    .registerController(MyControllerBlock.class, MyValidator::new, "MyStructure");
```

- 按控制方块的**精确 Java 类**匹配；同类再次注册会替换旧描述。
- 需要多个不同验证器时，使用不同的控制方块子类。
- `isControllerRegistered(MyControllerBlock.class)` 可查询是否已注册。

### 12.3 多方块能源核心/部件基类

- `MultiblockEnergyCore`（抽象）：必须覆写 `onMultiblockFormed(IMultiblockStructure)` 与 `onMultiblockBroken()`。
- `MultiblockEnergyPart`（抽象）：`setCorePosition` / `getCorePosition` / `setStructureCompleted` 等。

---

## 13. 升级系统

`IUpgradeAPI`：

```java
boolean supportsUpgrades(Level world, BlockPos pos);
int     getMaxUpgradeSlots(Level world, BlockPos pos);
Collection<ItemStack> getInstalledUpgrades(Level world, BlockPos pos);
UpgradeType getUpgradeType(ItemStack upgrade);
IUpgradeEffect getUpgradeEffect(ItemStack upgrade);
IUpgradeEffect calculateCombinedEffect(Collection<ItemStack> upgrades);
boolean installUpgrade(Level world, BlockPos pos, ItemStack upgrade);
ItemStack removeUpgrade(Level world, BlockPos pos, int slot);
boolean canInstallUpgrade(Level world, BlockPos pos, ItemStack upgrade);
int getUpgradeLimit(UpgradeType type);
```

`UpgradeType`：`OVERCLOCKER` / `TRANSFORMER` / `ENERGY_STORAGE` / `EJECTOR` / `IMPORT` / `FLUID_EJECTOR` / `FLUID_IMPORT` / `REDSTONE_SIGNAL` / `ADVANCED_CIRCUIT` / `MOLECULAR_TRANSFORM` / `QUANTUM_CORE` / `CUSTOM`。

自定义升级物品实现 `IUpgradeItem`（`core.api.upgrade.tile`）：

- `isSuitableFor(ItemStack, Set<UpgradableProperty>)`
- `onTick(ItemStack, IUpgradableBlock)` — 返回 `true` 表示本 tick 被消耗
- `onProcessEnd(ItemStack, IUpgradableBlock, Collection<ItemStack> outputs)`
- 效果方法（default）：`getSpeedBonus()`（**值越小越快**）、`getEnergyBonus()`、`getExtraCapacity()`、`hasAutoEject()` 等。

`UpgradableProperty` 用于声明机器适用的升级类别：`PROCESSING` / `AUGMENTABLE` / `REDSTONE_SENSITIVE` / `TRANSFORMER` / `ENERGY_STORAGE` / `ITEM_CONSUMING` / `ITEM_PRODUCING` / `FLUID_CONSUMING` / `FLUID_PRODUCING` / `REMOTELY_ACCESSIBLE`。

机器侧 `IMachineUpgradeStats` 提供组合后的有效倍率（`getProcessTimeMultiplier`、`getEnergyUsageMultiplier`、`getEffectiveCableTier(base)`、`getEjectorDirections` 等）。

---

## 14. 事件系统

`core.api.event` 中有事件类型，但**类型存在不等于宿主已发布**。当前源码核查结果：

| 事件 | 当前宿主是否发布 | 说明 |
| --- | --- | --- |
| `MultiblockFormedEvent` | 是 | 结构成形时发布，不可取消；`getLevel/getControllerPos/getStructure/getStructureName/getStructureBlocks` |
| `MultiblockBrokenEvent` | 是 | 结构失效时发布，不可取消；`getLevel/getControllerPos/getStructureName/getFormerStructureBlocks` |
| `EnergyGenerationEvent` / `EnergyTransferEvent` | 未找到发布路径 | 类型可取消、可改 amount，但不能据此拦截当前宿主电网 |
| `GeneratorStartEvent` / `GeneratorStopEvent` | 未找到发布路径 | 不保证宿主发电机启停时触发 |
| `MachineWorkStartEvent` / `MachineWorkCompleteEvent` | 未找到发布路径 | 不保证宿主加工或完成时触发 |
| `MachineUpgradeInsertEvent` / `MachineUpgradeRemoveEvent` | 未找到发布路径 | 不保证宿主升级槽操作时触发 |

多方块事件发布到 **`NeoForge.EVENT_BUS`（游戏事件总线）**，不要注册到模组的 `IEventBus`。`RegisterCapabilitiesEvent`、`FMLCommonSetupEvent` 等生命周期事件则属于模组事件总线，见第 18 节。

示例（在附属构造器中调用一次，使用游戏事件总线监听成形）：

```java
NeoForge.EVENT_BUS.addListener((MultiblockFormedEvent event) -> {
    if (event.getLevel().isClientSide) return;
    BlockPos controller = event.getControllerPos();
    // 按自己的主控/结构筛选，执行附属逻辑。
});
```

要监控当前能源行为，请使用 `NodeStats` / `EnergyDiagnostics`；要改变自己机器的工作流程，使用自己的工作方法和配置。附属可以发布自己的事件，但取消能否阻止行为取决于发布者实际检查取消结果。不要把尚未接入的宿主事件写成必触发钩子。

另：`core.api.energy.event.EnergyTileEvent` 当前只是 `IEnergyTileMarker` 的命名空间，不是可订阅的节点加载/卸载事件。能源节点接入用基类或组件的生命周期。

---

## 15. 组合式组件

### 15.1 EnergyComponent

见 [5.1](#51-两条集成路径继承-vs-组合)。补充：

- 组件不会主动加载区块；只有**当前服务端线程**、仍归属已加载且参与 tick 的区块实体才接受写入。
- 外部 EU/FE 端口共享其输入额度；原生 EU 的单包电压规则与整 tick 吞吐量仍需区分。外部输出与原生电网输出共用输出账本。
- 使用组件 `save/load` 保存完整状态，不只写一个整数余额；内部小数精度及未结算 FE 状态由组件负责，不建议依赖其 NBT 字段自行修改余额。
- 使用当前默认的独立电网模式，不支持关闭 `scex.independent.energy` 后继续作为旧电网组件使用。

### 15.2 FluidAutomationComponent

见 [8.3](#83-复用流体自动化)。

---

## 16. 平台能力注册

`core.platform.neoforge.CoreCapabilityRegistration`（在**拥有者模组自己的 `RegisterCapabilitiesEvent`** 中调用）：

```java
// 继承 AbstractEnergyBlockEntity 的实体：注册 sided EU、long EU、FE
CoreCapabilityRegistration.registerEntity(event, MY_ENTITY.get());

// 组合 EnergyComponent 的实体：注册组件端口
CoreCapabilityRegistration.registerComponent(event, MY_COMPONENT_ENTITY.get());

// 流体：机器/热能实体按 side 暴露流体处理器
CoreCapabilityRegistration.registerFluidHandler(event, MY_FLUID_ENTITY.get());
```

另：
- `CoreDataComponents`：`BATTERY_ENERGY` 数据组件类型。
- `CoreSounds`：`MACHINE_WORK` / `CABLE_BREAK`；`CoreSounds.register(bus)`。

宿主已注册自己的数据组件和音效，附属只消费这些 holder，不要再次注册宿主注册器。附属自己的组件/音效使用自己的命名空间及注册器。

### 16.1 EU / FE 能力的类型与单位

| 常量 | 类型 / 单位 | 接入说明 |
| --- | --- | --- |
| `EUApi.SIDED` | `IEUEnergyStorage`，long 方法使用 EU | 原生 sided EU 存储能力，含导线等级匹配 |
| `ILongEnergyStorage.BLOCK` | `ILongEnergyStorage`，long 方法使用 EU | 基类/组件便捷注册器同时注册这一端口 |
| `Capabilities.EnergyStorage.BLOCK` | NeoForge `IEnergyStorage`，int 方法使用 FE | 1 EU = 4 FE；使用已有 FE 端口，不自行重复乘/除 |
| `IMioIcifCapabilities.EU_STORAGE_BLOCK` | 嵌套 `IEUStorage`，EU | 另一套查询能力，不是 `EUApi.SIDED` 的同名别名；便捷注册器不自动为附属注册它 |
| `IMioIcifCapabilities.ELECTRIC_ITEM` | 嵌套 `ICapabilityElectricItem` | 电动物品能力；与 `item.electric.IElectricItem` 接口区分 |

`ILongEnergyStorage` 的 `receive/extract/getAmount/getCapacity` 使用 EU，而继承的 `receiveEnergy/extractEnergy/getEnergyStored/getMaxEnergyStored` 使用 FE，后者还受 int 范围限制。不要把 int getter 的值当 EU 存回数据组件，也不要直接覆写接口标记为不可扩展的默认换算方法。

注册自己的公开端口，不注册 `internalStorage` 等绕过生命周期/限额的对象。仅调用 `createEUStorage/createElectricItem` 得到一个对象并不会把它自动挂到实体、物品或电网上；能力、profile、生命周期和持久化仍需分别接入。

### 16.2 HU / KU 两套能力不能混用

当前宿主机器流转常用的是：

| 当前机器端口 | Java 类型 | ID |
| --- | --- | --- |
| `IMioIcifCapabilities.HEAT_STORAGE_BLOCK` | `IMioIcifCapabilities.IHeatStorage` | `mio_icif:api_heat_storage` |
| `IMioIcifCapabilities.KINETIC_STORAGE_BLOCK` | `IMioIcifCapabilities.IKineticStorage` | `mio_icif:api_kinetic_storage` |

例如蒸汽发生器在邻接面查询上表的 HU 能力；附属仅暴露下面另一套 HU 能力时，不会因此自动被它接收。注册片段（`heatPort(side)` 是附属提供的按面方法，返回上表接口或 null）：

```java
event.registerBlockEntity(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, MY_HEAT_ENTITY.get(),
    (entity, side) -> entity.heatPort(side));
```

现有 HU/KU 端口查询与模拟：

```java
IMioIcifCapabilities.IHeatStorage port = level.getCapability(
    IMioIcifCapabilities.HEAT_STORAGE_BLOCK, pos, Direction.WEST);
if (port != null && port.canExtractHeat()) {
    long offeredHu = port.extractHeat(100, true); // 模拟，不修改余额
}
```

另外，`HUCapabilities.HeatStorage.BLOCK` 的 ID 是 `mio_icif:heat_storage`，类型为 `core.api.heat.IHeatStorage`；`KUCapabilities.KineticStorage.BLOCK` 的 ID 是 `mio_icif:kinetic_storage`，类型为 `core.api.kinetic.IKineticStorage`。这些契约另有 source/conductor 能力，可以在附属自己的 `RegisterCapabilitiesEvent` 中通过 `HURegisterCapabilitiesEvent.registerHeatStorage/registerHeatSource/registerHeatConductor` 或 KU 对应静态帮助方法注册。

两套没有自动等价/强转关系；要同时服务两种消费者，需要为自己的权威存储提供两种正确的适配视图，不能建立互不相干的两份余额。名称含 Event 的 HU/KU 帮助类也不意味着必须等待另一个宿主自动发布的事件，当前可直接传入 NeoForge 的 `RegisterCapabilitiesEvent` 注册。

---

## 17. API 速查表

### 17.1 查询

| 需求 | 调用 |
| --- | --- |
| 某位置是否机器 | `api.getMachineAPI().isMachine(world, pos)` |
| 机器信息 | `api.getMachineAPI().getMachineInfo(world, pos): Optional<IMachineInfo>` |
| 查询配方 | `api.getRecipeAPI().findCompressorRecipe(stack, level)` |
| 多输入配方 | `api.getRecipeAPI().findRecipeMultiInput(type, ItemStack[], level)` |
| 主模组方块/物品/流体 | `api.getRegistries().getBlock(id)` / `getItem(id)` / `getFluid(id)` |
| 电网节点状态 | `api.getEnergyNetAPI().getNodeStats(level, pos): NodeStats` |
| FE↔EU 适配 | `api.getFECompatAPI().getStorage(level, pos, inputSide)` |
| 风场 | `api.getWindAPI().getWindAt(level, height)`（仅服务端） |
| 装备特性 | `api.getItemAPI().isArmorFeatureEnabled(stack, key)` / `getArmorFeatures(stack)` / `cycleArmorFeature(stack, key)` |
| 树脂采集 | `api.getRubberTreeAPI().canHarvest(state)` / `tryHarvest(level,pos,state,player,hand)` |
| HU 存储 | `api.getHeatAPI().getHeatStorage(level, pos): Optional<IHeatStorageAccess>` |
| KU 存储 | `api.getKineticAPI().getKineticStorage(level, pos)` |
| 反应堆状态 | `api.getReactorAPI().getReactorStructureStatusDetailed(level, pos)` / `getReactorCurrentHeat(level, pos)` |
| 反应堆元件 | `api.getReactorAPI().isReactorComponent(stack)` / `getFuelRodCells(stack)` |
| 管道参数 | `PipeGrade.STEEL.capacity(PipeSize.MEDIUM)` / `transferPerTick(size)` |

### 17.2 常用枚举

- `CableTier`（`core.api.energy.storage`）：`LV(32)` … `MAX`；`getPowerRating()`、`getTier()`、`getElectricDamage()` 等。
- `IMachineAPI.MachineType`、`IMachineAPI.RedstoneMode`、`IMachineAPI.SlotType`。
- `EnergyNodeProfile.Role`：`CONSUMER/GENERATOR/STORAGE/CONDUCTOR`。
- `FluidTankGroup.Role`：`INPUT_ONLY/OUTPUT_ONLY/IO/INTERNAL`。

### 17.3 常用基类速查

| 场景 | 基类 |
| --- | --- |
| 通用能源方块实体 | `AbstractEnergyBlockEntity` |
| 燃料发电机 | `AbstractGeneratorBlockEntity` |
| 储能箱 | `AbstractEnergyStorageBlockEntity` |
| 加工机器 | `AbstractProcessingMachineBlockEntity` / `AbstractStandardMachineBlockEntity` |
| HU / KU | `AbstractHeatBlockEntity` / `AbstractKineticBlockEntity` |
| 储罐 | `AbstractTankBlockEntity<T>` |
| 导线 | `AbstractWireBlockEntity` |
| 电池 / 电动工具 / 护甲 | `AbstractBattery` / `AbstractElectricTool` / `AbstractElectricArmor` |
| 气体流体 | `GasFluidSource` / `GasFluidFlowing` |

---

## 18. 线程、生命周期与缓存约束

1. **服务端线程约束**：能量对外的写入、`conductorColorChanged`、`diagnostics`、`EnergyComponent.consume/generate` 等仅在服务端线程有效；线程/世界不符时抛 `IllegalStateException`。
2. **能源描述注册**：必须在服务器首次启动前，`freeze()` 之后不可再注册。
3. **能力缓存失效**：改变方向策略 → `energy.portsChanged()`；改变导线颜色 → `conductorColorChanged`。
4. **配方缓存**：运行时注册/重载后调用 `IRecipeAPI.invalidateCache()`；配方同步在 `ServerStartedEvent` 调 `syncToRecipeManager()`。
5. **查询 getter 必须无副作用**：能源包策略、端口策略、颜色查询都可能被引擎多次采样，不得修改世界或存储。
6. **内部做功 vs 对外传输**：内部用 `useEnergy`/`generateEnergy`（不占 maxReceive/maxExtract）；对外用 `receiveEnergy`/`extractEnergy` 或 `euPort`/`fePort`。
7. **组合方式必须转发生命周期**，顺序见 [5.1](#51-两条集成路径继承-vs-组合)。
8. **不要依赖** `core.runtime`、`*Impl`、`@ApiStatus.Internal` 成员（如 `internalStorage()` / `internalLedger()` / `getEnergyStorageInternal`）。
9. **橡胶树采集/再生仅在服务端生效**：`IRubberTreeAPI.tryHarvest` / `tryRegrowResin` / `dropResin` 在客户端为无操作，判定类方法可在两端调用。
10. **装备特性状态以服务端为准**：开关/模式写入物品 CustomData。宿主界面与 `/mio_icif toggle` 已负责交互；附属自己做界面时，通过自己的受校验消息在服务端调用公开 `IItemAPI`，检查玩家实际装备、特性键及权限。宿主的具体网络包类不是附属公共入口。

### 18.1 推荐初始化顺序

| 阶段 | 事件总线 / 所在线程 | 建议操作 |
| --- | --- | --- |
| 模组构造 | 模组加载阶段 | 创建并挂接 DeferredRegister；注册能源 profile；挂接事件监听；不解析未注册 holder，不静态获取服务 |
| 能力注册 | 模组总线 `RegisterCapabilitiesEvent` | 为附属自己的实体/物品注册能力 |
| 公共 setup | 模组总线 `FMLCommonSetupEvent`，共享修改用 `enqueueWork` | 查询宿主服务；关联机器定义与已注册类型；注册物种、基础种子、多方块主控及一次性的运行时配方 |
| 世界启动 | 游戏总线 `ServerStartedEvent`，服务器线程 | 同步运行时配方到 `RecipeManager`；建立当前服务器的监控状态 |
| 服务端 tick / 玩家交互 | 服务器线程 | 实际能量、物品和流体交易；状态保存与同步 |
| 数据包重载完成 | 游戏重载流程，最终切回服务器线程 | 恢复需要保留的运行时配方并失效自己的缓存 |
| 世界停止 | 游戏总线服务器停止事件 | 清理附属自己的 Level / BlockEntity / 玩家引用、统计及能力缓存 |

`IMioIcifRegistries`、机器定义等查询属于注册完成后的工作。不要缓存跨世界的 `Level` 或位置对应的 capability 实例；实体卸载、方向修改后重新查询。公共 API 不代替附属自己的区块可用性检查，尤其不要为了批量监控强制加载尚未加载的区块。

### 18.2 网络监听契约与附属自己的同步

`core.api.network` 提供 `INetworkDataProvider`、`INetworkUpdateListener`、`INetworkItemEventListener`、`INetworkTileEntityEventListener` 与 `INetworkClientTileEntityEventListener`。当前宿主的特定接收包会识别其中的监听接口，但 `MioIcifAPI` 没有公开网络发送服务；发送工具 `NetworkHelper` 与具体包位于 `common.network`。

因此仅实现 `getNetworkedFields()` 不会使任意附属实体自动同步，监听接口也不负责为附属注册消息通道。不要根据残留 Javadoc 调用不存在于公共包的 `NetworkHelper`，或 import `common.network` 来取得发送入口。

附属的方块实体同步可用标准 `getUpdateTag/getUpdatePacket`，GUI 数值用自己的菜单/同步载荷；C2S 操作由自己的 payload 校验玩家、位置、装备槽、交互距离和参数，然后切回服务器线程。每种字段选择合适的序列化类型：历史字段监听回调使用 `double`，大于 `2^53` 的 long 不能精确往返，UUID、枚举、ItemStack、FluidStack 也不能靠把数字强转来完整同步。

`0.1.7.32` 起宿主网络通道版本包含模组版本，服务端与客户端宿主不同版本会在连接时拒绝，不应使用旧客户端验证新增菜单。附属依赖范围及 API 系列检查不代替宿主两端版本一致；附属自己的通道还要自行管理协议兼容。

---

## 19. 常见错误与迁移

| 症状 | 原因 | 处理 |
| --- | --- | --- |
| `IllegalStateException: Singularity core services are not initialized yet` | 在 API 装配前查询 `MioIcifAPI.instance()` | 延迟到 setup / `ServerStartedEvent` 之后再查询 |
| `Energy profiles are frozen after server startup` | 服务器启动后才注册 `EnergyNodeRegistry` | 移到模组构造或 setup |
| `Duplicate energy profile` | 同一方块 ID 重复注册 | 每个方块 ID 只注册一次 |
| `EnergyComponent.onLoad` 抛异常 | 方块未注册 profile | 先 `EnergyNodeRegistry.register` |
| 机器不接收 EU | 未注册描述 / 未注册能力 / 端口掩码为 0 | 检查三处 |
| 机器被流体自动化接管但未声明流体 | `registerFluidHandler` 返回了非 null | 不支持时返回 `null` |
| 多输入配方匹配错误 | 用了 `findRecipe`（仅校验首个 Ingredient） | 改用 `findRecipeMultiInput` |
| 升级效果反了 | `getSpeedBonus()` 值越小越快 | 检查语义 |
| 自定义种子分析仪不识别 | 未实现 `ICropSeedItem` 对应方法 | 实现 `cropScanLevel/setCropScanLevel` 等 |
| 自定义装甲特性不显示 / 点不动 | 未实现 `IElectricArmorItem.getFeatures`（或未继承 `AbstractElectricArmor`） | 在 `getFeatures` 中声明 `ArmorFeatureInfo` |
| 模式型特性无法切换 | 物品未实现 `IArmorModeFeatureItem` | 实现该接口（喷气背包用 `IJetpackItem` 已内置默认切换） |
| 附属装甲特性状态与界面不一致 | 自行存储状态，未走统一存储 | 统一使用 `ArmorFeatureState` / `IItemAPI.isArmorFeatureEnabled` |
| 喷气盔甲飞行逻辑要自己写 | 旧的飞行控制器位于 `core.runtime`（不可依赖） | 现位于 `core.prefab.flight.JetpackFlightController`，可直接调用 |
| 自定义采集器无法采集橡胶木 | 未实现 `ITreeTapItem`（旧版靠物品名判定） | 实现 `ITreeTapItem`；需耗电则同时实现 `IElectricToolItem` |
| 自定义原木无法被采集 | 未实现 `IRubberWood` | 让方块实现 `IRubberWood` |
| 声明了发电但一直没电 | `setAsPowerSource` 不生成库存，或缺 ticker | 服务端主动生成；ticker 校验实体类型 |
| 高频改方向后能量凭空增加 | 把每个结算窗口误当新预算 | 保持同 tick / 同轮共享额度，走公开端口 |
| 导线染色后外观不变 | 只通知电网，没有同步模型状态 | 保存颜色、发送客户端更新，并通知电气拓扑 |
| 黑色导线意外隔离 | 附属 getter 返回 BLACK 而非通配 null | 按所需规则把黑色映射为 null |
| 电表读数偏小或波动 | 混用样本、缓存增量、电压与流量 | 固定服务端采样阶段，按有效 tick 取平均 |
| 注册加工事件却没有回调 | 类型存在但宿主未发布 | 按第 14 节核对实际覆盖；自己的机器自己驱动逻辑 |
| 电力构建器 build 时报错 | 没有关联已注册类型，或无槽/容量/时间非法 | setup 后使用 withEntityType，检查配置 |
| 配方在 `/reload` 后消失 | 只做了一次运行时同步 | 重载后再次合并；检查注册返回值与 ID |
| 自定义元件能被 API 识别但不能装入核电 | 属性接口不是宿主模拟注册表 | 见第 21 节；不要宣称仅实现接口即可扩展宿主燃料 |
| 自定义原木无法被自动收集器采集 | 只有树脂接口，没有匹配方向属性 | 见第 10.7 节，检查 HORIZONTAL_FACING 与朝向 |
| 工具特性在控制台出现但提示没有 | 工具基类未自动追加特性行 | 调用 ArmorFeatureTooltip.appendAll，见第 9.7 节 |
| 新物品的某特性意外默认关闭 | 同键注册了专用默认值 | 用命名空间键，并核对 ArmorFeatureState.defaultFor |
| 电池右键不再立即给工具充电 | 当前改为切换自动模式 | 模式开启后由 inventoryTick 每 10 tick 分批充电 |
| 自动充电忽略 OFF 优先级 | 该路径不读取 ChargePriority | 区分充电站政策与便携自动充电政策 |
| 省电倍率重复生效 | 原始费用先 apply，再交给 consume | 使用已有 consume 时传原始费用 |
| 装备能量数字变化但 HUD 没重现 | 0.1.7.35 数字变化不重启显示窗口 | 需要持续展示时，仅为自己的行设置 keepEquipmentHudVisible |
| 热交换机提供 HU 却被蒸汽机忽略 | 注册了不同 ID/类型的另一套 HU 能力 | 检查实际消费者的 IMioIcifCapabilities.HEAT_STORAGE_BLOCK |
| 网络字段声明了但客户端不更新 | 接口声明没有自动发送入口 | 使用自己的实体更新/菜单/payload，不调用 common 工具 |

**废弃迁移**：`getGeneratorAPI()` → `getMachineAPI()` / `getMachineBuilderAPI()`；`IGeneratorAPI` / `IGeneratorInfo` 整体废弃。Java 包迁移后的旧包无兼容壳，需更新 import 后重新编译；游戏注册 ID 与资源命名空间仍为 `mio_icif`。

---

## 20. 示例工程与验证命令

参考工程：`src/addonExample/java/example/sicore/`，使用独立命名空间 `si_core_example`，编译 classpath 只有 core 开发 JAR 与 NeoForge，无 `common` 输出。

示例类速览：

| 类 | 演示 |
| --- | --- |
| `CoreExampleMod` | 模组入口、注册器、能力注册 |
| `InheritedSource` / `InheritedSink` | 继承方式最小示例 |
| `ComposedStorage` | 组合 `EnergyComponent` 与生命周期转发 |
| `ComposedPacketSource` | 显式多包策略 + 组合 |
| `InheritedMultiSource` | 覆盖 `getPacketCount()` 多包输出 |
| `FluidMachine` | 验证构建器的通用实现罐体行为；当前直接继承内部通用机器，不作为正式附属模板 |
| `ForeignCropSeed` | 自定义扫描数据格式的种子 |
| `ForeignBattery` | 直接注册 NeoForge FE 能力 |
| `PublicApiGameTests` | 只用公开入口的消费契约验证 |
| 其他 `*GameTests` | 宿主回归验证，部分检查内部引擎或以反射访问主模组内容，不作为附属扩展范例 |

常用命令：

```powershell
.\gradlew.bat build addonExampleJar --offline
.\gradlew.bat runAddonGameTestServer --offline
.\gradlew.bat publishCorePublicationToMavenRepository --offline
.\gradlew.bat compileAddonExampleJava -PcoreExampleFromRepository --offline
```

- `build` 执行源码边界、示例编译及完整模组与开发包的一致性/Mixin 校验；本工作区有 `gradle/core-architecture.gradle` 时，还执行字节码依赖、公开/受保护签名检查和正反例。架构通过不代表每个 API 声明都已有实际调用路径。
- GameTest 测试世界位于 `build/addon-gametest`。示例不会进入正常 `runClient` / `runServer`，也不会打进完整主模组。
- `coreExampleFromRepository` 模式验证从本地 Maven 发布物重新编译，不使用工作区 class 目录。

## 21. 核反应堆、HU / KU 与核爆边界

### 21.1 反应堆监控与元件查询

反应堆查询使用 `core.api.reactor.IReactorAPI`，无需引用具体主控实体：

```java
IReactorAPI reactors = MioIcifAPI.instance().getReactorAPI();
var structure = reactors.getReactorStructureStatusDetailed(level, controllerPos);
boolean running = reactors.isReactorRunning(level, controllerPos);
long hullHeat = reactors.getReactorCurrentHeat(level, controllerPos);
long maxHullHeat = reactors.getReactorMaxHeat(level, controllerPos);
double temperature = reactors.getReactorCurrentTemperature(level, controllerPos);
long euPerTick = reactors.getReactorCurrentEnergyGeneration(level, controllerPos);
var mode = reactors.getReactorMode(level, controllerPos);

if (reactors.isFuelRod(stack)) {
    int cells = reactors.getFuelRodCells(stack); // 1 / 2 / 4
    boolean mox = reactors.isMoxFuel(stack);
    ItemStack depleted = reactors.getFuelRodDepletedItem(stack);
}
```

查询参数是**已加载的主控位置**。连接仓或外壳位置不自动等同主控；没有主控时多个 getter 返回 0 / false / 默认值。`getReactorMode` 的默认 GENERATOR 不能证明存在反应堆，应同时检查结构状态或自己的目标校验。

`getReactorCurrentHeat/getReactorMaxHeat` 是堆壳热量与容量，不是某个冷却液罐的 mB；温度是宿主展示用摄氏度数值。FLUID 模式不能仅用 EU 输出判断实际热能产量，冷却液循环与热交换机需要单独观测流体/HU 能力。

元件属性接口是 `IBaseReactorComponent`：声明类型、单元数、脉冲/热量/容热/传热参数、MOX、耗尽判定和耗尽物品。`getHeatOutput/getFuelRodHeatOutput` 等返回物品声明属性，**不会**根据摆放邻接、脉冲、堆温与运行模式重新模拟整台反应堆的实际输出。查询所有元件 ID 会扫描实现该接口的已注册物品，不只主模组物品；非元件的 `getComponentType` 当前返回 `OTHER`，虽接口注释残留“返回 null”，调用者应先用 `isReactorComponent` 判定。

### 21.2 自定义核燃料与反应堆的现有限制

**实现 `IBaseReactorComponent` 只保证属性查询识别，不保证能进入主模组反应堆。** 当前主模组元件槽和模拟仍依赖自己的具体元件体系，API 没有公开的自定义燃料、散热算法、反射器或核爆贡献注册入口。不要把自定义物品标为 `FUEL_ROD` 后便宣称其会按这些属性发电、产生热量或增加爆炸当量。

附属自建反应堆可以实现 `IReactorController` 参与查询，自己负责模拟、库存、流体、同步与保护规则。基础 `IReactor` 中出现的 `setHeat/addHeat/setMaxHeat/addEmitHeat/getItemAt/setItemAt/explode` 等方法，不代表主模组支持附属任意写入；`IReactorController` 对其中多项提供抛 `UnsupportedOperationException` 的默认实现，具体提供者可能覆写。正式互操作使用公开查询及对方实际暴露的物品/流体能力，不依赖内部热量存储或模拟上下文。

`getItemHandler/getFluidHandler` 不赋予绕过槽位验证或端口限制的权限；避免将返回对象强转为可任意改写的内部实现。元件操作仍应遵守对方库存规则和服务端线程要求。

### 21.3 热能、动能与冷却液循环

```java
IHeatAPI heat = MioIcifAPI.instance().getHeatAPI();
heat.getHeatStorage(level, pos).ifPresent(storage -> {
    long storedHu = storage.getHeatStored();
    int displayedTemperature = storage.getTemperature();
});

IKineticAPI kinetic = MioIcifAPI.instance().getKineticAPI();
kinetic.getKineticStorage(level, pos).ifPresent(storage -> {
    long storedKu = storage.getKineticStored();
    int rpm = storage.getRPM();
});
```

KU 查询认两种方块：方块实体实现 `core.api.kinetic.IKineticStorage/IKineticSource/IKineticConductor` 的，以及在某个面暴露 `IMioIcifCapabilities.KINETIC_STORAGE_BLOCK` 能力的。SI 自己的动能方块（各种动能发生机和三种动能发电机）属于后一种，`0.1.11.86` 起才能被 `hasKineticTile`、`getKineticStorage`、`getKineticConnections`、`insertKinetic/extractKinetic` 识别。这类方块按面区别对待（动能发电机只从正面收 KU，风力动能发生机只从背面给 KU）：存储查询把 KU 送进或取出第一个肯收或肯给的面；要走指定的面，直接按该面查询能力。

HU 查询同理：认实现 `core.api.heat.IHeatStorage/IHeatSource/IHeatConductor` 的方块，以及在某个面暴露 `IMioIcifCapabilities.HEAT_STORAGE_BLOCK` 的方块。SI 自己的热能方块（各种加热机、斯特林发电机、蒸汽机、高炉、反应堆等）属于后一种，`0.1.11.87` 起才能被 `hasHeatTile`、`getHeatStorage`、`getHeatConnections`、`insertHeat/extractHeat` 识别，按面的规则与 KU 相同。

两者存储查询都返回 `Optional`。服务端注入/提取可用 `insertHeat/extractHeat`、`insertKinetic/extractKinetic`，参数为非负量，`simulate=true` 不修改余额；必须使用返回的实际转移量结算源端。`setHeatStored/setKineticStored` 是通过接收/提取实现的目标值调整，对方拒收/拒抽时可能无法达到目标，需读取实际余额确认。

HU、KU、EU、mB 是不同单位，没有对所有机器成立的万能换算。自定义热交换机应分别处理热需求、热冷却液消耗、冷却液回流与输出槽容量；下游不索取热量时不能假定仍按满载冷却。定向能源互操作需核对 `IHeatSource/IKineticSource` 或对方按面暴露的能力，不能靠聚合存储查询推导某面一定可接收。

### 21.4 风场

`IWindAPI.getWindAt(level, height)` 返回经过高度、天气与全局修正的瞬时 MCU；`getBaseWindStrength` 返回基础强度，`getWindDirection` 返回 0～359 度。客户端查询返回 0，监控界面需由服务端采样并同步。

风场不等于已扣除机器周围遮挡的最终发电量；附属风机自行计算阻挡、转子适用范围、耐久和 KU/EU 转换。不要以客户端返回 0 判定真实无风。

### 21.5 核爆与辐射转化不是当前公共 API

近期加入的蘑菇云、闪光、冲击波、分步地形毁伤、持久化圆形辐射转化和冰雪融化，属于宿主的具体机制。当前 `MioIcifAPI` **没有** `getNuclearAPI/getRadiationAPI`，也没有公开的爆炸调度、污染区注册、转化配方或按材质回调接口。

不要 import `common.reactor` 的任务/管理器、`core.runtime.radiation` 的存档状态，或通过反射调用它们。它们的队列、扫描游标、区块同步与任务预算不属于附属契约。附属自己的爆炸系统应使用自己的实现；如需正式接入宿主的核爆流程，需要另外设计并发布公共契约。

宿主行为对整合兼容的影响：

- 辐射转化按水平圆形逐列判断，未加载区块留待加载后处理，进度持久化，不为转化主动加载整片区域；到访不代表所有列已经在同一 tick 完成。
- 既有污染、辐射区不会因关闭新核爆而自动撤销；附属不能从“核爆关闭”推导已污染区域安全。
- 热效应会清理雪层，雪块/细雪和普通、霜冰、浮冰、蓝冰可融为水，超热维度不留水。标准 ICE / SNOW 标签也参与宿主冷方块识别；标签只描述识别，并非承诺给任意附属材质开放可定制的转化算法。

这些是 `0.1.7.35` 的兼容注意事项，不是 API 系列 1.0 对所有游戏规则的永久数值承诺。反应堆校准过程见 [核电对齐进度](reactor_ic2_alignment_progress.md)。

---

## 22. 主模组内容查询与配置

### 22.1 内容 ID 与查询返回值

通过 `IMioIcifRegistries` 获取对象，不 import `common.registry`：

```java
IMioIcifRegistries registries = MioIcifAPI.instance().getRegistries();
Block collector = registries.getBlock("producer/block_resin_collector");
Block bronzePipe = registries.getBlock("pipe/fluid_pipe_bronze");
ItemStack resin = registries.getItemStack("resource/item_harz", 1);
if (collector == null || bronzePipe == null || resin.isEmpty()) {
    // 禁用依赖这些内容的附属功能，并说明当前宿主缺少所需内容。
}
```

字符串重载使用 `mio_icif` 命名空间，路径中的 `producer/`、`pipe/`、`resource/` 不能省略。缺失方块/物品/流体/实体类型查询返回 `null`，`getItemStack` 返回 `ItemStack.EMPTY`。不要用翻译名称或 Java 类名推导注册 ID。

`get*ByCategory` 是基于 ID 关键字的便捷分类，不是带严格类型保证的独立注册表。查找某种行为应核对公开接口或 capability；获取自己的附属内容使用自己的注册器或 Minecraft 注册表。主模组保留旧内容 ID 不等于附属可以依赖旧 Java 包。

### 22.2 COMMON 配置与附属配置责任

宿主配置是 NeoForge `ModConfig.Type.COMMON`，默认文件为实例 `config/mio_icif-common.toml`。它是宿主内容的策略，不是公开的配置服务；附属不要访问 `Singularity_Iteration_Config` 或内部 `CoreConfig`。附属通过自己的 `ModContainer` 注册自己的 ConfigSpec，并自行实现必要的服务端校验与客户端同步。

COMMON 不自动提供服务器到客户端的值同步。服务器使用自己的事故/地形配置，客户端视觉使用本地配置；不要以客户端文件决定服务器毁伤权限，也不要以本地视觉开关推断服务器核爆机制是否启用。

目前相关宿主键（供整合者理解行为，不是 Java 配置 API）：

| `NuclearExplosion` 下的键 | 默认 / 范围 | 实际作用 |
| --- | --- | --- |
| `enableNuclearExplosion` | `true` | 核弹及反应堆的大范围核爆开关；关闭后新事故仅局部移除装置，不毁周围地形；已排队压力/转化暂停，重开可续跑，已有污染不回滚 |
| `nukeExplosionMultiplier` | `1.0` / 0～16 | 独立核弹当量倍率，不影响反应堆或采矿镭射；0 不执行核弹的大范围核爆 |
| `reactorExplosionMultiplier` | `1.0` / 0～16 | 反应堆事故倍率；0 暂停事故与高温环境效果 |
| `reactorExplosionPowerLimit` | `45` / 0～1000 | 反应堆最终威力上限，水平半径上限为其两倍；0 暂停事故与高温效果 |
| `nuclearExplosionFlash` | `true` | 本地瞬时闪光 |
| `nuclearExplosionCameraShake` | `true` | 本地冲击波镜头震动 |
| `mushroomCloudParticleMultiplier` | `1.0` / 0～10 | 本地云密度；0 关闭云，实际数量另有上限 |

核弹当量和半径仍受宿主上限约束；反应堆还根据有效燃料单元及隔板计算。已提交反应堆事故保存当时的威力快照，修改非零倍率不会重新计算那次事故。不同配置可能在构造、tick 或缓存建立时读取，不要假定所有配置都能立即热重载，也不要把存在于配置文件中的遗留键当作新的公共扩展入口。

---

## 23. 发布与升级核对

发布附属前至少核对：

1. core 开发包与实际测试的完整宿主版本一致；附属 JAR 不包含 core、宿主、示例工程的类。
2. 正式源码只使用本文列出的公共 API/底座；不复制内部任务，不以反射绕过边界，不依赖 `Generic*` 内部通用机器作为继承实现。
3. Block / Item / BlockEntityType / 菜单 / 配方类型等由附属自己的命名空间注册；能源 profile、能力、ticker 与生命周期完整接入。
4. 在独立服务器也能加载，公共类不静态触发客户端类；状态读写在服务器完成，GUI/模型变化正确同步。
5. 按所集成系统验证实际行为：能量守恒与高压多包、EU/FE 共享预算、方向/颜色变更、流体满槽与回流、作物分析/收获、区块卸载重载、存档读写和数据包重载。
6. 未覆盖的事件、燃料模拟、核爆及辐射扩展保持明确限制，不以同名接口或开发包里的内部类代替能力证明。

主工程 `runAddonGameTestServer` 是宿主与示例的行为回归入口，不由普通 `build` 自动运行；不能把“构建通过”写成“所有功能已在游戏内验证”。正式附属需对自己实际用到的入口与受支持版本做验证。

维护本指南时以实现与调用点共同核对，不只照抄接口 Javadoc。当前已发现的文档/实现差异包括：部分事件未发布、电力构建器要求非空实体类型、数组配方重载只支持单输入、流体精炼输入数量未保留、核元件查询识别与模拟可安装范围不同。这里只如实说明当前行为，没有在更新文档时修改这些代码。

相关资料：

- [core 接入与迁移指南](core_addon_guide.md)
- [重构验收检查表](core_refactor_acceptance_checklist.md)
- [作物 IC2 对齐进度](crop_ic2_alignment_progress.md)
- [管道复原结果](fluid_pipe_restoration_result.md)
- [公开 API 入口源码](../src/main/java/com/miophas/singularity_iteration/core/api/MioIcifAPI.java)
- [工具特性契约](../src/main/java/com/miophas/singularity_iteration/core/api/item/IFeatureToolItem.java)
- [工具模式契约](../src/main/java/com/miophas/singularity_iteration/core/api/tool/IToolModeProvider.java)
- [装备 HUD 契约](../src/main/java/com/miophas/singularity_iteration/core/api/item/IEquipmentHudProvider.java)
- [自动充电契约与规则](../src/main/java/com/miophas/singularity_iteration/core/api/item/BatteryAutoCharge.java)
- [省电计算帮助类](../src/main/java/com/miophas/singularity_iteration/core/api/item/EnergySaving.java)
- [能力 ID 与嵌套类型](../src/main/java/com/miophas/singularity_iteration/core/api/capability/IMioIcifCapabilities.java)
- [独立附属示例入口](../src/addonExample/java/example/sicore/CoreExampleMod.java)

### 参考链接

- [NeoForge 1.21.1 能力说明](https://docs.neoforged.net/docs/1.21.1/inventories/capabilities/)
- [NeoForge GameTest](https://docs.neoforged.net/docs/1.21.1/misc/gametest/)
- [ModDevGradle（source set 与运行配置）](https://github.com/neoforged/ModDevGradle)
