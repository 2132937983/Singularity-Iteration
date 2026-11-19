# Core 附属开发指南

目标环境：Minecraft 1.21.1 / NeoForge 21.1.218 / Java 21。当前开发包版本与主模组一致：`0.1.7.14`。

## 1. 依赖方式

玩家安装完整的 `mio_icif-0.1.7.14.jar`。附属编译时使用 `singularity-iteration-core-0.1.7.14.jar`；这个开发包不作为独立模组安装，也不要打进附属 JAR。

```groovy
dependencies {
    compileOnly files('libs/singularity-iteration-core-0.1.7.14.jar')
    localRuntime files('libs/mio_icif-0.1.7.14.jar')
}
```

运行 `./gradlew publishCorePublicationToMavenRepository` 会发布到本项目 `repo/`。在附属工程声明这个本地仓库后，也可用：

```groovy
compileOnly 'com.miophas.singularity_iteration:singularity-iteration-core:0.1.7.14'
```

这里没有配置公网发布仓库。附属的 `neoforge.mods.toml` 需要声明对 `mio_icif` 的运行依赖；完整例子见 [示例元数据](../src/addonExample/resources/META-INF/neoforge.mods.toml)。正式发布附属时，应将示例中的宽版本范围收窄到实际验证过的主模组版本。

## 2. 包边界

| 包 | 用途 |
| --- | --- |
| `com.miophas.singularity_iteration.core.api` | 能源、槽位、机器、升级、配方、物品与装备等公共契约和服务入口 |
| `core.prefab` | 可继承的方块实体、物品、库存、装备特性与喷气飞行底座，以及可组合的能源组件 |
| `core.platform.neoforge` | 能力注册等平台接入 |
| `core.runtime` | 核心内部实现，随开发包提供实现闭包；附属不要依赖其内部布局 |
| `common` | 主模组具体内容，附属不应导入这里的实现类 |

`MioIcifAPI.instance()` 由主模组装配。服务查询和扩展业务建议放到公共 setup 完成后的工作中，避免在附属类的静态初始化阶段查询尚未安装的服务。附属不要调用 `ApiServices.install` 或替换 `CommonCoreBootstrap` 的宿主配置。

本轮迁移了 Java 包与部分核心类名，旧包上的源码及二进制附属需要更新 import 后重新编译。游戏注册 ID 和资源命名空间仍为 `mio_icif`。

### 2.1 API 兼容与废弃策略（2026-09-29 起）

- 开发包版本继续与主模组一致；公开契约兼容版本独立为 **1.0**，通过 `MioIcifAPI.instance().getApiVersion()` 查询运行时提供者，通过 `MioIcifAPI.API_VERSION` 标识编译目标。
- 同一 API 主版本保持已发布公开契约的源码与二进制兼容；新增接口操作优先使用默认方法，旧提供者不支持时明确抛出 `UnsupportedOperationException`。改变签名、删除入口或改变核心语义需要升级 API 主版本并提供迁移说明。
- 稳定范围为未标记内部/实验性的 `core.api` 契约、本文列出的 `core.prefab` 扩展入口及 `CoreCapabilityRegistration`。`core.runtime`、其余平台内部设施和 `@ApiStatus.Internal` 成员不属于附属兼容承诺。
- 废弃入口必须注明 `@Deprecated(since=...)`、替代入口与迁移说明，至少保留两个后续正式模组版本，并且只能在下一 API 主版本移除。旧 Java 包迁移发生在本策略之前，不补充旧包兼容壳。
- 本版保留 `getEnergyStorageInternal()`、旧 `getEnergyStorageCapability(side)` 和流体缓冲 getter 的 JVM 签名，并标记内部兼容入口；不会为了整理 API 改写已有存档或删除这些方法。

机器自身工作使用 `getEnergyStorage().useEnergy/generateEnergy` 或受保护的 `apiUseEnergy/apiGenerateEnergy`；与外部机器传输使用 `euPort(side)` / FE 能力，它们遵守方向、生命周期与共享额度。不要强转返回对象、访问 `energyStorage` 字段或内部账本。

流体自动化组件通过 `pendingInput()` / `pendingOutput()` 提供防御性副本，通过 `save/load` 持久化。不要直接操作 `inputBuffer()` / `outputBuffer()`。

### 2.2 发布方式

当前可复现的发布目标是项目本地 Maven 仓库 `repo/`；本轮未发布到公网，也没有预设仓库地址。完整模组是运行依赖，core JAR 仅供编译，独立 core 前置模组仍为可选后续形态。

正式仓库接入已预留 `coreReleaseRepositoryUrl` Gradle 属性。维护者配置地址后可使用 `publishCorePublicationToCoreReleaseRepository`；认证由用户级 Gradle 属性 `coreReleaseUsername` / `coreReleasePassword` 或对应的 `ORG_GRADLE_PROJECT_*` 环境变量提供。凭据不要放进项目源码或命令行。未设置地址时，不创建远程发布目标。

## 3. 继承方式

可以继承以下基类：

- `AbstractEnergyBlockEntity`：能源实体基础。
- `AbstractProcessingMachineBlockEntity` / `AbstractStandardMachineBlockEntity`：生产机、标准离散加工周期。
- `AbstractGeneratorBlockEntity`：燃料、发电、充电槽骨架。
- `AbstractEnergyStorageBlockEntity`：储能、方向、红石、充放电槽骨架。
- `AbstractHeatBlockEntity` / `AbstractKineticBlockEntity`：HU / KU 基础。

最小例子见 [InheritedSource](../src/addonExample/java/example/sicore/InheritedSource.java) 和 [InheritedSink](../src/addonExample/java/example/sicore/InheritedSink.java)。方块及方块实体仍由附属自己的 `DeferredRegister` 注册。

在模组构造或 setup 阶段、第一次服务端启动前，注册自己的能源设备描述：

```java
EnergyNodeRegistry.register(
    ResourceLocation.fromNamespaceAndPath("your_addon", "generator"),
    EnergyNodeProfile.generator(32)
);
```

注册自己的能力：

```java
// RegisterCapabilitiesEvent，在附属自己的 mod event bus 上调用
CoreCapabilityRegistration.registerEntity(event, MY_ENTITY.get());
```

基础实体默认不会向所有设备发布 FE。自定义普通能源实体可以覆盖 `supportsFeCapability()` 返回 `true`；储能箱及生产机保持原有规则。EU、long EU 和 FE 能力都由同一实体提供。继承发电源时调用 `setAsPowerSource`，不要只注册 generator 描述却让实体维持消费端角色。

加工/发电逻辑需要由方块的服务端 ticker 调用相应基类 tick 方法。示例能源源只用于测试注入电量，不代表自动产电配方。菜单应由附属覆盖 `createMenu`，工作音效可以覆盖生产机的 `getWorkSound`。

## 3.1 多方块

通过公开入口创建多方块结构，回调随生命周期真实执行：

```java
IMultiblockStructure structure = IMultiblockStructure.builder()
    .validator(new MyValidator())
    .onFormed((level, pos) -> { ... })
    .onBroken((level, pos) -> { ... })
    .buildAndRegister(level, controllerPos);
```

结构成形后可用 `IMultiblockStructure.find(level, pos)` 按主控或成员位置查找；`structure.invalidate(level)` 拆除结构并触发 `onBroken`。若希望方块变化时自动重建结构，注册自己的控制方块：

```java
// 在附属公共 setup 阶段、首次启动世界前，通过已装配的服务注册
MioIcifAPI.instance().getMultiblockAPI()
    .registerController(MyControllerBlock.class, MyValidator::new, "MyStructure");
```

内置结构（GESU、大型制造机、流体反应堆）走同一注册表；结构发现不再对具体机器类做类型判断。完整的回调与查找验证见 `CoreGameTests.multiblockBuilderLifecycleCallbacks`。

注册按控制方块的精确 Java 类匹配；需要多个不同验证器时使用不同的控制方块子类。同类再次注册会替换旧描述，可用 `isControllerRegistered(MyControllerBlock.class)` 查询是否注册。

## 4. 组合方式

当已有方块实体不能改父类时，实现 `EnergyComponentHost` 并持有 `EnergyComponent`。完整例子见 [ComposedStorage](../src/addonExample/java/example/sicore/ComposedStorage.java)。

```java
private final EnergyComponent energy = new EnergyComponent(
    this, 1024, 32, 32, CableTier.LV,
    () -> new EnergyPortPolicy(inputMask, outputMask, false)
);

@Override
public EnergyComponent energyComponent() { return energy; }
```

六位方向掩码使用 `1 << Direction.ordinal()`。输入输出允许为 0；`null` 方向查询聚合端口。发电组件使用 `EnergyPortPolicy.source()`，同时注册 generator 设备描述；普通储能组件的 generator 标记为 `false`。

宿主必须转发生命周期：

| 方块实体方法 | 转发 |
| --- | --- |
| `setLevel` | `super.setLevel` 后调用 `energy.setLevel` |
| `onLoad` | `super.onLoad` 后调用 `energy.onLoad` |
| `setRemoved` | 调用 `energy.setRemoved`，再调用 `super.setRemoved` |
| `clearRemoved` | `super.clearRemoved` 后调用 `energy.clearRemoved` |
| `onChunkUnloaded` | 调用 `energy.onChunkUnloaded`，并保留父类调用 |
| `saveAdditional` | 保留父类调用，再调用 `energy.save` |
| `loadAdditional` | 保留父类调用，再调用 `energy.load` |

能力注册使用 `CoreCapabilityRegistration.registerComponent(event, MY_ENTITY.get())`。改变方向策略后调用 `energy.portsChanged()`，以失效能力缓存并通知拓扑。策略供应函数应是无副作用的查询；不要在传输回调中变更发电/储能角色。

机器内部产能、耗能使用 `generate` / `consume`；外部传输使用 `euPort` / `fePort`。不要调用标记为 `@ApiStatus.Internal` 的内部存储访问方法。

组件不会主动加载区块，只有当前服务端线程、仍归属于已加载且参与 tick 的区块实体才接受写入。EU/FE 外部端口共用输入限额；外部输出和原生电网输出共用输出账本。原生输入保留现有引擎的包电压规则，不把 `maxReceive` 重新解释成整个原生网络的总 EU/t 限额。

NBT 保留 `energy`、`scex_energy_fraction`、`scex_fe_uncertain_output`。组件使用当前默认的独立电网模式，不支持将启动参数 `scex.independent.energy` 关闭后继续作为旧电网组件使用。

## 4.1 原生 EU 多包输出

默认电网现在读取已有 `IEnergySource.getPacketCount()`。继承能源实体的附属可以覆盖这个方法，例如返回 `8`；同时需要有足够库存，并将存储的 `maxExtract` 配置为至少 `8 × 单包大小`。默认仍为一包，旧接口返回值限制到 0～64，0 表示停止原生输出。完整例子见 [InheritedMultiSource](../src/addonExample/java/example/sicore/InheritedMultiSource.java)。

组合方式或需要指定完整包/残包行为时，在拥有核心存储的实体上实现 `core.api.energy.IEnergyPacketSource`：

```java
private EnergyPacketPolicy packets = new EnergyPacketPolicy(32, 8, true);

@Override
public EnergyPacketPolicy getEnergyPacketPolicy() {
    return packets;
}
```

参数分别是单包 EU、每次结算的包数、是否允许初始不足一个完整包时发出残包。上例最多输出 8 个 32 EU 的低压包，不会变成一个 256 EU 的高压包。`partialPackets=false` 要求结算开始时凑齐包；已开始分配的包仍可按接收端需求拆分，剩余电量可继续送往后面的窗口。

显式策略优先于旧 `getPacketCount()` 和默认单包描述；单包大小决定输出包上限，接收端耐压仍由接收端自己的等级决定。策略 getter 必须是无副作用的查询，不能修改世界或存储；组合实体改变策略后调用 `energy.portsChanged()`。完整生命周期、策略保存和读取见 [ComposedPacketSource](../src/addonExample/java/example/sicore/ComposedPacketSource.java)。

这个接口不单独授予入网资格，仍需注册能源描述、能力并持有核心自有存储。原生 EU 和 FE 输出继续共用 `maxExtract` 的每 tick 额度；例如 `maxExtract=256`，外部先抽走 64 FE（16 EU），本 tick 原生 EU 最多再发 240 EU。声明 0 包只关闭原生 EU 报价；若还要关闭 FE 输出，应同时关闭输出端口。

64 包是当前引擎的工程上限，不是 IC2 原版参数。超大的网络可能因路径工作预算拆到多 tick；策略给出可供上限，不保证负载不足、线损或计算预算不足时的实际吞吐。结算窗口共享一份初始报价，切换窗口和本轮新输入都不会刷新它。沿用现有拓扑更新后的 START/END 双阶段行为，不能把“每次结算”误认为无条件等于“每 tick”。

## 4.2 导线颜色、损耗与恢复诊断

附属导线或端点可实现 `core.api.energy.IColoredEnergyTile`，按面返回 `DyeColor`；返回 `null` 表示通配色。同色或任一侧为通配时允许连接。内置导线把黑色映射为 `null`。改变颜色后，在服务器线程调用 `MioIcifAPI.instance().getEnergyNetAPI().conductorColorChanged(entity)`，使本节点及相邻节点的方向掩码重新采样；保存颜色、客户端更新和掉落数据仍由实体负责。脱离服务端世界或从其他线程调用会抛出 `IllegalStateException`。

查询 getter 必须无副作用。引擎会隔离查询抛异常的区块并每 20 tick 尝试重扫；故障区块内的其他节点也会暂时退出。队列溢出时会分批重建索引，实体余额保留。修复附属的异常后即可自动重新加入，无需重启整个服务器。

在服务器线程通过公开服务读取不可变快照：

```java
EnergyDiagnostics snapshot = MioIcifAPI.instance().getEnergyNetAPI().diagnostics(serverLevel);
```

该查询不会创建引擎、加载区块或刷新能量预算；`available()` 为 false 表示尚无引擎，此时计数为零。

- `routing()`：指定维度的拓扑代号、构建/续算次数和预算让出次数。
- `recovery()`：整个服务器的重建次数、隔离次数、正在恢复的维度数、隔离区块数及最近原因。
- `timing()`：指定维度最近最多 256 个已完成 tick 的结算耗时，p50/p95/p99/max 均为纳秒，START/END 合并；不代表整个服务器 tick 的耗时。

默认 `energynet.roundEnetLoss=true` 对完整路径损耗取整；关闭后使用毫 EU 路损的定点表示。FE 自动桥接则向上量化到整数 FE。自动桥接与原生 EU 共享初始包额度，0 包不执行自动输出，8/64 包不再受旧四次尝试限制，完整包必须凑齐；第三方通过暴露能力主动拉取 FE 仍按端口和每 tick FE 账本处理。

## 5. 可运行示例与验证

[CoreExampleMod](../src/addonExample/java/example/sicore/CoreExampleMod.java) 使用独立命名空间 `si_core_example`，编译 classpath 只有 core 开发 JAR 和 NeoForge 的平台依赖，没有 common 编译输出。

```powershell
.\gradlew.bat build addonExampleJar --offline
.\gradlew.bat runAddonGameTestServer --offline
.\gradlew.bat publishCorePublicationToMavenRepository --offline
.\gradlew.bat compileAddonExampleJava -PcoreExampleFromRepository --offline
```

`build` 会检查 core 源码边界、字节码依赖（含泛型、注解与反射字符串）、API 与 prefab 的公开/受保护签名、编译示例，以及完整模组和开发包的类一致性和 Mixin 路径。`testCoreArchitectureRules` 使用正反样例验证检查器；GameTest 单独运行，避免每次普通构建都启动游戏。

`PublicApiGameTests` 只使用公开入口，验证 EU/FE 共享预算、缓存端口失效、诊断只读与线程约束、控制器注册。其他 `*GameTests` 中有内部引擎回归测试，会有意访问 runtime，不应将这些测试中的故障注入代码复制进附属。实际附属示例与公开契约测试受到单独的禁止内部入口检查。

最后两条命令验证从本地 Maven 发布物重新编译，不使用 core/common 的工作区 class 目录。测试世界位于 `build/addon-gametest`。示例不会进入正常 `runClient` / `runServer`，也不会打进完整主模组；其独立 JAR 由 `addonExampleJar` 生成。

机器构建器的 `addFluidTank(capacity)` 现在会真正创建罐体：`GenericMachineBlockEntity` 按配置惰性建罐（0 罐时仍返回 null，不会引来流体自动化），并在 `RegisterCapabilitiesEvent` 里用 `CoreCapabilityRegistration.registerFluidHandler(event, MY_ENTITY.get())` 注册流体能力即可对外可用；多罐会合并为一个处理器（路由见 `FluidTankGroup`）。注意当前 `MachineConfiguration` 只保留“罐数 + 统一容量”，多罐异构容量尚未表达。

流体端口可只靠 core 声明：实现 `core.api.fluid.IFluidPorts`（提供输入/输出端口，可按方向路由），核心自动化即可像识别内置灌装机一样识别你的机器；多槽机器可用 `core.prefab.fluid.FluidTankGroup` 合并槽位：每个槽用 `FluidTankGroup.Tank(handler, Role)` 声明方向角色（只进 / 只出 / 双向 / 内部），填充只走可灌槽、抽取只走可抽槽，语义与 IC2 的 `Fluids` 组件一致；按面限制用 `core.prefab.fluid.SidedFluidHandler`。

自有方块实体也能复用流体自动化：实现 `FluidAutomationComponent.Host`（提供世界上下文、本机/相邻流体处理器、升级统计、脏标记），持有 `core.prefab.component.FluidAutomationComponent` 即可获得与内置机器一致的弹射/吸取行为与 `scex_fluid_*_pending` 缓冲持久化。

可垂直堆叠连通的储罐可继承 `AbstractTankBlockEntity`（core.prefab.blockentity）：子类只需提供容量与实体类型，连通组默认按具体子类判定（不同材质不串组），多罐合并读写、放置后均衡与 `FluidTank` NBT 由骨架提供；内置 5 种储罐即按此实现。

UU 报价与扫描值模型的内部实现位于 `core.runtime.uu`；这说明它们已经与内置内容解耦，不表示附属应直接依赖这些类。附属配方扩展使用 `MioIcifAPI.instance().getRecipeRegistrationAPI()` 注册、`syncToRecipeManager()` 同步、`getRecipeAPI().find*Recipe(...)` 查询；对应验证见 `CoreGameTests.addonCompressorRecipeExtension`。`uuQuoteBookStateLifecycle` 是内部生命周期回归测试。

平台接入依据：[NeoForge 1.21.1 能力说明](https://docs.neoforged.net/docs/1.21.1/inventories/capabilities/)、[GameTest 说明](https://docs.neoforged.net/docs/1.21.1/misc/gametest/)、[ModDevGradle source set 与运行配置](https://github.com/neoforged/ModDevGradle)。

## 6. IC2 作物周期与附属种子

希望使用 IC2 Experimental 规则的作物继承 `core.prefab.crop.Ic2PlantType`，通过 `MioIcifAPI.instance().getCropAPI().registerPlant(...)` 注册。此基类的 `usesIc2CropCycle()` 返回 true，宿主作物架每 256 游戏 tick 调用一次特殊 `tick(IPlanter)`，随后统一计算环境、生长点、阶段推进与资源衰减。`getGrowthTime` 的单位是生长点，默认 `tier * 200`；不要在特殊 tick 内重复推进整套生长引擎。

既有直接继承 `PlantType` 的作物默认 `usesIc2CropCycle() == false`，保留逐游戏 tick 回调和原有生长单位。新增加的动态 `getStageAfterHarvest(IPlanter)` 默认委托无参版本；`PlantStats.getDefensive/getColorful/getWeed` 为历史第三、第四、第五属性提供 IC2 语义别名，不移除旧 getter。

`IPlanter.makeSeeds(plant, count, growth, gain, resistance)` 的第二参数是种袋数量。宿主保留扫描等级；种袋种植保留属性，正常收获与拔株取种是两种不同操作。基础种子注册的 ItemStack 数量会用于种植成本（例如四个花卉），不能把数量仅当作展示信息。

自定义种子物品实现 `core.api.item.ICropSeedItem`，可覆盖 `cropModId/cropId`、`cropGrowth/cropGain/cropResistance`、`cropScanLevel/setCropScanLevel`。默认方法读取原 SI 的 CUSTOM_DATA 键，覆盖方法可以使用附属自己的存储格式。内置分析仪通过这些方法工作，不要求继承 common 的 `CropSeedItem`。该接口提供分析数据契约，种植行为仍由物品自身实现。

示例见 [ForeignCropSeed](../src/addonExample/java/example/sicore/ForeignCropSeed.java)；农业专项测试验证其自定义扫描数据可被内置分析仪逐级处理。玩法及存档迁移详见 [农业对齐实施记录](crop_ic2_alignment_progress.md)。

## 7. 物品与装备契约

物品契约位于 `core.api.item`（可直接实现），装备基类位于 `core.prefab.item`。

| 接口 | 用途 |
| --- | --- |
| `IBatteryItem` | 能量存储（`getMaxEnergy/getEnergy/addEnergy/extractEnergy/...`），基类 `AbstractBattery` |
| `IElectricToolItem` | 电动工具（`getEnergyPerUse/getToolTier/hasEnoughEnergy`），基类 `AbstractElectricTool` |
| `IElectricArmorItem` | 电力装甲（`getEnergyPerTick/getArmorTier/getFeatures/getEnergyPerDamage/getDamageAbsorptionRatio`），基类 `AbstractElectricArmor` |
| `IArmorModeFeatureItem` | 模式型装备特性切换（`cycleMode`） |
| `IJetpackItem` | 喷气背包（`getThrust/getEnergyPerTickFlying/getMode/setMode`），**已继承 `IArmorModeFeatureItem`** |
| `IBackSlotItem` | 外部背槽 tick 桥接（Curios `back` 等） |
| `IFluidCellItem` / `ICropSeedItem` | 流体单元 / 附属种子 |

### 7.1 装备特性（开关 / 模式）

- **声明**：重写 `IElectricArmorItem.getFeatures(ItemStack)` 返回 `ArmorFeatureInfo` 列表；`isMode=false` 为开关型，`isMode=true` 为模式型（需携带当前模式名）。
- **读写**：`IItemAPI.isArmorFeatureEnabled` / `setArmorFeatureEnabled` / `toggleArmorFeature` / `cycleArmorFeature` / `getArmorFeatureInfo`；或直接使用 `core.prefab.item.ArmorFeatureState`（存储，缺省**启用**）与 `ArmorFeatures`（分发）。
- **渲染**：`core.prefab.item.ArmorFeatureTooltip` 统一生成可点击行（`/mio_icif toggle <slot> <featureKey>`）；`AbstractElectricArmor.appendFeatureTooltip` 已默认接入，附属无需自写 Tooltip。
- **界面与同步**：Alt 特性管理界面、`/mio_icif toggle` 命令、`ArmorFeatureTogglePacket` / `ArmorFeatureCyclePacket` 全部由声明驱动，不识别具体物品类；附属无需自建界面或网络包。
- **槽位**：原版四格，以及可选的 Curios `back`（未安装 Curios 时自动退化）。

### 7.2 喷气盔甲

实现 `IJetpackItem`（需要背槽支持时同时实现 `IBackSlotItem`），在 tick 中调用：

```java
core.prefab.flight.JetpackFlightController.tick(
    player, stack, this, hoverCost, maxAscent, hoverAscent, hoverDescent, advanced);
```

即可复用推升 / 悬停 / 前向推进 / 限高与按键输入（`core.prefab.flight.JetpackKeyHandler` 由宿主为所有玩家同步）。模式切换无需自行实现：`IJetpackItem` 默认提供 `cycleMode`（悬停 ↔ 飞行），K 键、特性管理界面与 Tooltip 共用同一路径。

### 7.3 已废弃与迁移

- `common.item.armor.ArmorFeatureToggle`（含 `FeatureInfo`）已废弃，改用 `IItemAPI` 或 `core.prefab` 的 `ArmorFeatures` / `ArmorFeatureState` / `ArmorFeatureTooltip`。
- `JetpackFlightController` / `JetpackKeyHandler` 已由 `core.runtime.flight` 迁至 **`core.prefab.flight`**，附属可合法依赖。
- 开关型特性未显式写入时默认**启用**（`ArmorFeatureState.DEFAULT_ENABLED`）；模式型特性没有开关语义，只支持 `cycleArmorFeature` / `ArmorFeatures.cycleMode`。
- 完整示例、查询表与错误排查见 [附属模组开发文档 9.5 装备特性与喷气盔甲](addon_development_guide.md#95-装备特性与喷气盔甲)。
