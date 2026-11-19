# Core / common 重构实施记录

首次迁移：2026-09-25；收尾更新：2026-09-29。对应 [重构方案](core_common_refactor_plan.md)，附属用法与兼容策略见 [开发指南](core_addon_guide.md)。当前状态：代码分层与自动化验收完成，实机回归矩阵尚未全部签收。

## 1. 本轮落地范围

原有 1,453 个 Java 文件已全部迁入 `com.miophas.singularity_iteration`，按职责拆为 core 与 common。不是只搬接口：能源存储、数值引擎、交易账本、基础机器、通用物品、库存/槽位及升级实现均随 core 开发包提供。

当前共 1,508 个主源码 Java 文件：core 388 个、common 1,120 个；另有 23 个示例/测试 Java 文件。工作开始前工作区已存在大量未提交改动，本轮以磁盘文件为基线，没有恢复 Git HEAD。这里统计 Java 源文件，不包含备份目录，也不同于编译后的 class 数量。

```text
com.miophas.singularity_iteration
├── core
│   ├── api                  公共契约、服务装配与能源设备描述
│   ├── prefab               方块实体、能源组件、物品、库存、配方基类
│   ├── runtime              EU/HU/KU、账本、处理、反应堆等通用实现
│   └── platform.neoforge    能力、数据组件、世界与网络适配
└── common
    ├── bootstrap            主模组装配
    ├── registry             主要方块/实体/物品/菜单/声音/数据组件注册
    ├── block / blockentity / item
    ├── energy               内置能源参数、特殊设备工厂
    ├── service              依赖内置内容的 API 服务实现
    ├── integration          可选模组兼容
    └── client               屏幕、渲染、输入及客户端兼容
```

一些领域注册器仍留在所属 common 子包，例如流体、世界生成和细分物品注册器；它们不进入 core。

## 2. 核心入口映射

以下新包均以 `com.miophas.singularity_iteration` 为前缀：

| 原入口 | 新入口 |
| --- | --- |
| `Blocks.entity.mio_icif_Energy_Block` | `core.prefab.blockentity.AbstractEnergyBlockEntity` |
| `mio_icif_Energy_Container` | `core.prefab.blockentity.AbstractEnergyStorageBlockEntity` |
| `mio_icif_Energy_Generator` | `core.prefab.blockentity.AbstractGeneratorBlockEntity` |
| `mio_icif_producer` | `core.prefab.blockentity.AbstractProcessingMachineBlockEntity` |
| `mio_icif_standard_producer` | `core.prefab.blockentity.AbstractStandardMachineBlockEntity` |
| `IGeneratorBlockEntity` | `core.api.machine.IGeneratorBlockEntity` |
| `api.MioIcifAPI` | `core.api.MioIcifAPI` |
| `api.MioIcifAPIHolder` | `common.bootstrap.CommonApiServices`，经 `core.api.ApiServices` 安装 |
| `api.internal.*` | 通用服务在 `core.runtime`，内容服务在 `common.service`，Generic 实体在 `core.prefab.blockentity` |
| `energy.EnergyUnit.*` | `core.api.energy.storage.*` |
| `energy.IndependentSiEnergy` | `core.runtime.energy.IndependentSiEnergy` |
| `energy.energyfix.*` | `core.runtime.energy.engine.*`，平台适配在 `core.platform.neoforge.energy` |
| `energy.grid` | 接口在 `core.api.energy.grid`，实现及兼容门面在 `core.runtime.energy.grid` |
| `Blocks.entity.slot` | `core.prefab.inventory` |
| `api.item.AbstractBattery` 等 | `core.prefab.item` |
| `Blocks.mio_icif_capacities` | `common.registry.mio_icif_capacities` |
| `Blocks.entity.mio_icif_block_entities` | `common.registry.mio_icif_block_entities` |
| `Blocks.mio_icif_blocks` | `common.registry.mio_icif_blocks` |

## 3. 实际解耦

- `ApiServices` 由宿主安装公共服务，核心接口不再主动创建 common Holder。多方块查询/构建通过 `IMultiblockAccess` 转发。
- `CommonCoreBootstrap` 装配内容服务、物品槽位默认规则、菜单/声音和兼容实现。core 不引用或反射加载 common。
- 原生电网通过 `EnergyNodeRegistry` 获取内置/附属设备描述。内置发电包大小、电缆损耗、熔断参数及四类基础加工机规则移到 `BuiltinEnergyProfiles`。注册在第一次服务器启动时冻结。
- 电缆端口、反应堆额外接触面、装罐机双流体端口分别使用通用接口，核心不再检测相应具体机器类。
- MI/GT/AE2 能源适配和 Curios 电池目标查询通过宿主接口装配，具体第三方类留在 common。core 的编译配置没有这些可选 API 依赖。
- `EnergyComponent` 提供组合接入，不要求继承核心 BE；已接入原生网络的所有权检查、报价提交与输出配额。继承路径继续使用现有存储和账本，二者复用同一组数值/结算实现，但没有在本轮强制把所有旧基类字段替换成组件。
- 增加 `CoreCapabilityRegistration`，附属为自己的 BE 类型注册 EU/long EU/FE 能力。能力视图受方向、模拟、实体身份和世界状态约束。
- `battery_energy` 的定义和 codec 移到 core，common 引用同一个 holder；运行时只注册一次。
- 客户端屏幕注册移到仅在客户端构造的入口，船只渲染器也移入 `common.client`。两份 Mixin 配置及字符串反射类名已更新。
- 多方块结构生命周期下沉为 `core.runtime.multiblock` 的通用实现：`ActiveMultiblocks` 统一活动结构存储，`GenericMultiblockBuilder` 构建的附属结构会真实执行 `onFormed`/`onBroken` 回调并发布事件（旧实现保存回调但从不执行）。控制方块到验证器的映射改为注册式（`MultiblockControllers`），GESU、大型制造机、流体反应堆由 `common.multiblock.BuiltinMultiblockControllers` 在启动装配时注册，通用结构发现不再对具体机器类做 `instanceof` 判断。
- UU 报价与值索引下沉为 `core.runtime.uu`（`UuQuoteBook`、`IndependentUuValueIndex`、`UuScanResult`）。扫描结果使用 core 值模型，内置扫描机的 `ScanResult` 只是兼容外壳；`StoredPattern`、模式存储与复制路径不再反向依赖内置扫描机类型。
- 旧 Java 根包 `com.singularity_iteration` 已按方案 9.2 清除：目录内已无 Java 代码，仅剩过时 README 与 `.bak` 备份。REI 服务声明同步指向新 `REIPlugin` 类名；`build.gradle` 的旧包残留检查保留。
- 机器组件细化（方案 §6.2）：`IMachineUpgradeStats` 补上 HU 容量推导，`AbstractHeatBlockEntity` 的字段与 getter 改为接口类型，与 EU/标准机一致；新增 `core.prefab.item.EmergencyChargePolicy`，把放电槽“红石应急充能”（物品、800 EU/份、堆叠上限）从 `MachineItemHandler`/生产机/`MachineItemDischarging` 抽成可由宿主装配的策略，行为不变。
- 新增 `core.prefab.blockentity.AbstractTankBlockEntity`：可垂直堆叠连通的储罐骨架（容量参数化、连通组按具体子类判定、多罐合并读写、放置后均衡、NBT 键 `FluidTank` 不变）。5 个内置储罐实体（青铜 16000 / 铁 32000 / 钛 64000 / 精炼铁 128000 / 铱 1024000）由原先各 258 行同构代码改为继承该骨架，只保留容量与实体类型。
- 新增 `core.prefab.component.FluidAutomationComponent`：流体弹射/吸取升级的通用实现（预算、方向、先扣后交付缓冲、脏标记与邻居通知）。原先三份重复实现（`AbstractProcessingMachineBlockEntity`、`AbstractHeatBlockEntity`、`common` 的太阳能蒸馏机）改为持有该组件；两个核心基类仍保留可覆盖的 `ejectFluids`/`pullFluids`/`handleFluidUpgrades`，灌装机 measured 路径保持不变，NBT 键 `scex_fluid_output_pending`/`scex_fluid_input_pending` 不变。
- 构建器声明的流体罐真正生效（候选 D）：`GenericMachineBlockEntity` 按 `MachineConfiguration` 的 `fluidTanks`/`tankCapacity` 惰性创建罐体（配置在构造后注入，故惰性），并在 0 罐时仍返回 null，因此既有的 5 台构建器机器不会被流体自动化顺带接管；自行覆写 `getFluidHandlerCapability` 的子类（如炼油机）不受影响。罐体 NBT 用独立键 `scex_generic_tank_N`，不占用洗矿机 `fluid`、炼油机 `InputTank`/`OutputTank` 的既有键。新增 `CoreCapabilityRegistration.registerFluidHandler(...)` 与两个基类的公开访问器 `fluidHandler(side)`，附属可为自己的机器注册流体能力。示例附属新增 `FluidMachine` 端到端验证该路径。
- 流体端口路由下沉：新增 `core.api.fluid.IFluidPorts`（可按方向路由、可声明端口数，默认保持“输入罐/输出罐”语义），`ISeparateFluidPorts` 改为继承它，核心自动化改为识别端口契约；新增 `core.prefab.fluid.FluidTankGroup`（多槽合并，按槽有效性依次填充、默认逆序抽取）与 `core.prefab.fluid.SidedFluidHandler`（按面限制填充/抽取）。已迁移 4 处：太阳能蒸馏机的双罐合并处理器，以及 3 个流体分配器/调节器的 `SidedFluidHandler` 副本（各 45 行）。
- 对照 IC2 1.12.2 的 `ic2/core/block/comp/Fluids.java` 复核双流体槽语义后，`FluidTankGroup` 改为与 IC2 同构的槽位模型：每个槽带方向角色（`INPUT_ONLY`/`OUTPUT_ONLY`/`IO`/`INTERNAL`）+ 自身 accepts 谓词，`fill` 跨可灌槽累加、`drain(FluidStack)` 按类型跨可抽槽累加、`drain(int)` 由首个非空可抽槽定类型，null 面视为任意面（IC2 在 null 面全槽不可访问）。据此批量回迁剩余 8 份 `CombinedFluidHandler`（蒸汽机、再压机、炼油机、发酵机、热源机、灌装机、斯特林动能机、蒸汽动能机，约 350 行），并统一了斯特林机在 `mio_icif_capacities` 中语义不同的匿名处理器。行为变化：不再从输入罐回落抽取原料（5 台，与 IC2 一致），蒸汽动能机的蒸馏水罐改为可灌入（IC2 `SteamKineticGenerator` 的 `Access.IO`）。

## 4. 构建与产物

[build.gradle](../build.gradle) 新增独立 `core` source set。`compileCoreJava` 不依赖 main 输出；main 单向依赖 core。示例 source set 编译时直接消费开发 JAR，且不继承主模组的可选模组依赖。

| 产物 | 内容 |
| --- | --- |
| `build/libs/mio_icif-0.1.7.14.jar` | core + common + 游戏资源 |
| `build/libs/singularity-iteration-core-0.1.7.14.jar` | 只有 core 类及其内部实现闭包 |
| `build/libs/singularity-iteration-core-0.1.7.14-sources.jar` | core 源码 |
| `build/libs/singularity-iteration-core-example-0.1.7.14.jar` | 单独的开发示例，不进入主模组产物 |

以下检查均随 `build` 执行：

- `checkCoreArchitecture`：core 源码不得依赖 common、客户端或可选模组；实际附属示例与公开契约测试不得调用内部能源入口。
- `checkCoreBytecode`：检查实际 class 中的依赖、泛型、注解及反射字符串；core.api 不得依赖 runtime/prefab/platform；prefab 的公开/受保护签名不得外露 runtime 类型，显式 `@ApiStatus.Internal` 兼容桥除外。
- `testCoreArchitectureRules`：7 个正反样例验证上述规则，包含泛型泄漏、客户端注解、反射字符串与内部兼容桥。
- `checkCoreDistribution`：开发包内容、完整 JAR 内的同名 core 类字节一致、旧包/示例类残留及 Mixin 目标。

字节码检查不加载游戏类。Minecraft 开发依赖本身仍包含平台客户端类；约束的是 core 对客户端的引用，不重制平台 JAR。静态检查也不替代真实客户端/第三方集成加载验证。

开发包 Maven 坐标：`com.miophas.singularity_iteration:singularity-iteration-core:0.1.7.14`。本轮已发布到项目 `repo/` 并验证消费。远程发布目标通过 `coreReleaseRepositoryUrl` 配置，凭据走用户级 Gradle 属性；未配置地址、未向公网发布。完整模组版本与 API 兼容版本分开：当前 API 为 `1.0`。

## 5. 已做与待做验证

迁移前 `compileJava --offline` 通过。迁移后独立 core 与完整模组编译、开发包及示例打包、源码边界和产物检查通过；另从本地 Maven 发布物重新编译示例确认开发包内含新的 `core.runtime.multiblock` 与 `core.runtime.uu` 类。最终命令和测试结果在本节末记录。

[CoreGameTests](../src/addonExample/java/example/sicore/CoreGameTests.java) 的 14 个基础测试覆盖如下；它们是当前全部 72 项 GameTest 的一部分：

1. 不同命名空间的继承发电源 → 组合储能。
2. 组合储能 → 继承消费端。
3. EU/FE 多面混合输入、simulate、tick 限额、1 FE 小数电量保存/恢复、移除与替换后的旧端口。
4. EU/FE 共用输出限额、方向变更与缓存端口。
5. 显式启用 FE 的继承发电源端口及移除失效。
6. 附属 → 内置电缆 → 内置变压器 → 附属。
7. 内置火力发电机 → BatBox → 电炉，实际燃煤发电并将铁矿石加工成铁锭。
8. 多方块构建器：成形/拆除回调真实执行，结构可通过 `IMultiblockStructure.find` 发现，`invalidate` 后不再可查。
9. 配方扩展：附属命名空间的压缩机配方经公开注册 API 注册并 `syncToRecipeManager` 后可被 `IRecipeAPI.findCompressorRecipe` 查到。
10. UU 状态生命周期：报价世代安装/可观测、有限/拒绝/未知分类、扫描结果值模型校验与 MB 换算、服务器移除后状态释放。
11. 内置储罐垂直连通：两罐成组后容量翻倍、填充跨罐分布、成组抽取返回全部流体。
12. 流体自动化组件：单个流体弹射升级按 IC2 的每相邻面 50 mB/t 独立额度传输、源端正确扣减、缓冲存档键保留。2026-09-28 修正原先六面共用 1000 mB 的行为，见 [热核冷却液循环修复](coolant_loop_fix.md)。
13. 流体端口路由：多槽合并按槽有效性路由（水只进水槽）、逆序抽取、按面限制填充/抽取。
14. 构建器流体罐：附属只用公开入口声明 2 个罐后能拿到真实的流体能力（容量、填充、抽取正确），未声明罐的机器仍不暴露流体能力。

第 5 项最初发现 FE 输出仅允许储能基类的问题，修正为支持显式启用 FE 的附属发电源，并保留内置设备的能力发布范围。后续还加入能源结算、升压、多包、导线颜色、电表、HU、反应堆冷却循环等专项测试。

本轮新增 [PublicApiGameTests](../src/addonExample/java/example/sicore/PublicApiGameTests.java) 的 3 项公开入口测试：EU/FE 共享预算与移除失效；不可变诊断、版本查询与线程约束；控制器注册与无效参数保护。2026-09-29 实跑 **72 项 GameTest 全部通过**，服务端正常启动和关闭。自动化可模拟玩家使用染料，但不验证客户端像素、GUI 或用户真实旧存档。

已通过的完整命令：

```powershell
.\gradlew.bat build addonExampleJar publishCorePublicationToMavenRepository --offline --console=plain --max-workers=2
.\gradlew.bat compileAddonExampleJava -PcoreExampleFromRepository runAddonGameTestServer --offline --console=plain --max-workers=2
```

第二条命令实际重新编译了示例，仅解析本地 Maven 发布的 core JAR 与平台依赖，检查确认 classpath 没有 core/common 工作区 class 目录。

当前 core 字节码检查覆盖 688 个 class；产物检查确认完整模组与开发包中的 core 类一致，没有旧 Java 包或示例类混入主模组。构建记录：`build/core-closure-build.log`；从 Maven 编译与游戏测试记录：`build/core-closure-gametest.log`，结果为 `All 72 required tests passed :)`。这些文件是本机生成证据，不随源码发布；可用以上命令重新生成。

对迁移前 1,453 份源码逐一核对，未发现遗失实现；字符串审计中的变化集中在反射类名更新、内置参数表迁往 common、数据组件注册位置调整和通用 `active` 状态属性访问。注册名、能力 ID、NBT key、网络 payload ID、资源命名空间没有随 Java 包更名。

未验证的范围：用户旧存档载入、客户端 GUI 实机、完整 HU/KU/反应堆/升级矩阵、可选模组逐项启用、区块卸载/维度切换/多次开关不同世界。GameTest 的 NBT 保存恢复与移除测试不等于这些场景全部通过。构建仍有原有的弃用 API 等警告。可勾选的执行步骤见 [实机回归清单](core_refactor_acceptance_checklist.md)。

## 6. 后续阶段

本轮已完成的收尾：

- `IEnergyNetAPI` 增加导线改色通知与值对象诊断；附属不必直接调用 `IndependentSiEnergy`。
- `IMultiblockAccess` 增加控制器注册与查询，主模组服务转发到原有注册表。
- `AbstractEnergyBlockEntity.euPort(side)` 返回接口；旧存储方法/字段、账本与流体缓冲入口标记内部用途，历史 JVM 签名保留。流体缓冲新增防御性快照查询。
- API 兼容版本、废弃周期与发布方式写入开发指南；源码/字节码/产物边界检查进入常规构建。

尚需外部环境验收：用户重构前存档、客户端 GUI/渲染、完整 HU/KU/升级组合、真实区块/世界生命周期及可选集成矩阵，按 [实机清单](core_refactor_acceptance_checklist.md) 记录；没有测试证据的项目不勾选。远程发布需要实际仓库地址和凭据，本轮仅完成配置入口及本地发布验证。

后续可选维护：继续细化大型机器的库存、升级与流体组件。`RecipeFactoryBridge`/`RecipeTypeBridge` 的内置类型绑定及 `UuReferenceCatalog`/`UuProcessingRecipes` 的内置数据装配留在 common 符合职责划分，不作为未完成迁移。独立 core 前置模组仍是可选后续形态。
