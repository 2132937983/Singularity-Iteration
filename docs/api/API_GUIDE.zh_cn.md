# 奇点迭代（Singularity Iteration）API 指南（0.1.8.0）

本指南面向附属模组作者。文字遵循 ASD-STE100 简明技术语言原则：短句、主动语态、每个步骤只有一个指令。

## 1. 制品

| 制品 | 内容 | 用途 |
|---|---|---|
| `mio_icif-api-<版本>.jar` | 仅 `com.miophas.singularity_iteration.core.api.**` | 编译只使用 API 的附属模组。 |
| `singularity-iteration-core-<版本>.jar` | API、运行时、预制类、平台辅助类 | 编译继承预制方块实体的附属模组。 |
| `singularity-iteration-<版本>.jar` | 完整模组 | 运行游戏。 |

API 包不引用任何运行时、预制或 `common` 类。构建会检查这一点（`checkCoreArchitecture`）。示例附属（`src/addonExample`）只依赖核心 JAR 编译。

### 1.1 步骤：添加依赖

1. 把 API JAR 作为仅编译（compileOnly）依赖加入构建。
2. 把完整模组 JAR 加入运行类路径。
3. 在 `neoforge.mods.toml` 中添加对 `mio_icif` 的依赖。附属离不开它时，设置 `type="required"`；只做联动时，设置 `type="optional"`。
4. 如果依赖是可选的，把每个 API 调用放进一个只在 `ModList.get().isLoaded("mio_icif")` 为真时才加载的类中。

## 2. 通用规则

* 除非某节另有规定，在服务器线程上调用 API。
* 在模组构造期间或初始化事件中注册。服务器启动后不要注册。
* API 中的注册表在两端都开放。请在两端都注册，因为客户端使用这些数据显示名称、颜色和界面。
* API 不改变世界存档格式。附属添加的数据保存在附属自己的数据组件或附件中。

**注意：** 不要保留 API 传给回调的 `ItemStack` 引用。回调之后该物品堆可能改变。

## 3. 量子套装单元（`core.api.suit`）

附属可以为量子套装添加升级单元。该单元与内置单元行为相同：量子改装台安装和拆卸它，奇点管理面板显示它的开关，它的能量消耗使用整套能量池。

| 类型 | 功能 |
|---|---|
| `SuitModuleSpec` | 单元数据：ID、部位、耗电（EU/t）、颜色、是否需要面罩、单元物品。 |
| `ISuitModuleItem` | 单元物品的接口，返回 spec。 |
| `SuitModuleBehavior` | 单元的工作。单元生效时，每个服务器 tick 运行一次。 |
| `SuitModuleRegistry` | 注册，以及查询 `isActive(player, id)`。 |

### 3.1 规则

* ID 必须带有你的模组命名空间，例如 `myaddon:sonar`。内置单元没有命名空间。
* 附属单元计入该部件的单元容量（头盔 4、胸甲 3、护腿 2、靴子 2）。
* "生效"指：安装在已穿戴的量子部件中、在面板中已开启、能量足够（至少一个 tick 的耗电），并且 `needsVisor` 为真时已穿戴量子头盔。
* 耗电每秒收取一次。部件的节能附魔适用。
* 抛出异常的行为会被记录日志，该 tick 内单元保持关闭。
* 模组被移除后，部件仍保留其单元 ID。该单元不起作用，也不占用容量。

### 3.2 步骤：添加单元

1. 编写继承 `Item` 并实现 `ISuitModuleItem` 的物品类。
2. 创建 `SuitModuleSpec`。示例：

   ```java
   public static final SuitModuleSpec SPEC = new SuitModuleSpec(
       ResourceLocation.fromNamespaceAndPath("myaddon", "sonar"),
       Set.of(EquipmentSlot.HEAD), 3, 0x33CCAA, true,
       ResourceLocation.fromNamespaceAndPath("myaddon", "sonar_unit"));
   ```

3. 在模组构造函数中注册 spec 和行为：

   ```java
   SuitModuleRegistry.register(SPEC, (player, piece) -> {
       if (player.tickCount % 40 == 0) { /* 单元的工作 */ }
   });
   ```

4. 添加翻译键 `module.myaddon.sonar`（名称）和 `module.myaddon.sonar.desc`（说明）。
5. 为单元物品添加配方。

面板开关键为 `fcs_<命名空间>_<路径>`（`SuitModuleSpec.featureKey()`）。

## 4. 弹道（`core.api.item`）

火控 HUD、智能弹道诱导单元和无人机使用同一个弹道模型。每一步为一个 tick：先移动，再阻力，再重力。

| 类型 | 功能 |
|---|---|
| `BallisticProfile` | 发射速度、重力、阻力、水中阻力、"就绪"标志。工厂方法：`arrow`、`thrown`、`straight`。 |
| `IBallisticWeapon` | 武器物品的接口，返回当前射击的参数。 |
| `BallisticProfiles` | 查询，以及为其他模组物品注册的提供器。 |

查询顺序：

1. 物品实现了 `IBallisticWeapon`。
2. 已注册的提供器（`BallisticProfiles.register`），按注册顺序。
3. 客户端从该物品的射击中学习到的参数（见 4.2）。
4. 内置原版参数（弓、弩、三叉戟、投掷物）。

### 4.1 步骤：描述武器

1. 在武器物品上实现 `IBallisticWeapon`。
2. 返回本次射击的参数。有蓄力时间的武器使用 `useTicks`。`BallisticProfile.drawPower(useTicks, fullDrawTicks)` 给出原版的蓄力曲线。
3. 武器无法射击时返回 `null`。

对于其他模组的物品，请注册提供器。提供器对不认识的物品必须返回 `null`。

### 4.2 学习到的参数

本地玩家用没有参数的武器射击时，客户端观察新弹射物一个 tick，测量发射速度、阻力和重力。每个物品最多保存 12 次射击，存放在 `config/mio_icif-ballistics.json`。在流体中的射击、带推力的射击和立即命中的射击不被采用。

## 5. 军械库（`core.api.armory`）

奇点迭代在服务器线程上把以下事件发布到 `NeoForge.EVENT_BUS`。

| 事件 | 时机 | 可取消 |
|---|---|---|
| `ArmoryEvent.Summon` | 套装飞向穿戴者（玩家或同伴）之前。 | 是。不消耗能量，不移动物品。 |
| `ArmoryEvent.PieceDocked` | 飞行部件装备完成之后。 | 否。 |
| `ArmoryEvent.WingmanDeploy` | 储存的套装作为僚机出动之前。 | 是。 |

### 5.1 数据标签

| 标签 | 类型 | 功能 |
|---|---|---|
| `mio_icif:armory/remote_equip` | 实体类型 | 可以从军械库接收套装的同伴。实体必须是 `OwnableEntity` 且归该玩家所有。默认：`touhou_little_maid:maid`（可选条目）。 |
| `mio_icif:armory/drawn_blade` | 物品 | 以拔刀姿态飞到手中的物品。拔刀剑：重锋（SlashBlade: Resharped）的刀无需此标签即可识别。 |
| `mio_icif:wingman/fulton_immune` | 实体类型 | 富尔顿回收不能带走的生物。首领（`c:bosses`）始终免疫。 |
| `mio_icif:resonance_immune` | 方块 | 力场共振解体不会破坏的方块。 |

## 6. 能量（`core.api.energy`）

0.1.8.0 没有改变能量 API 的约定。EU 网络默认使用公平分配（`config/core.ini` 中的 `distributionMode=fair`，或通用配置）。

1. 用 `EnergyNodeRegistry.register(id, profile)` 注册每个方块的角色。使用 `EnergyNodeProfile.generator(packet)`、`storage(packet)` 或 `consumer()`。
2. 提供方块能力 `EUApi.SIDED`。对于预制方块实体，调用 `CoreCapabilityRegistration.registerEntity(event, type)`。
3. 网络分四个阶段供电：先按额定输入向用电器供电，再填满用电器的剩余空间，然后按额定输入向储能设备供电，最后填满储能设备的剩余空间。每个阶段内，接收方按其剩余需求的比例获得能量。发电机先于储能设备供电。

## 7. 蓝图建造机与资材接口

* 军械库资材接口提供 `Capabilities.ItemHandler.BLOCK`（27 个槽位）。相邻的、提供同一能力的任何存储都可以为无人机供料。
* 无人机只使用普通物品堆：没有修改过的组件（内容物、名称、附魔）。
* 建造机读取 `schematics` 文件夹中的 `.nbt` 结构文件和 `.litematic` 文件，以及已部署的机械动力蓝图（数据组件 `create:schematic_*`）。
* 每次放置都以带有所有者档案的假玩家触发 `BlockEvent.EntityPlaceEvent`。保护类模组可以取消它。

## 8. 文档挂钩

* GuideME：手册是代码构建的指南（`mio_icif:manual`）。标签 `<ReactorSimulator chambers="N" preset="行/行/..."/>` 显示核反应堆模拟器。预设的每一行是以逗号分隔的组件键（`_` 表示空位）。
* 机械动力思索（Ponder）：军械库、核反应堆发电机、蓝图建造机和喷射管道的场景。文本键：`mio_icif.ponder.<场景>.header` 与 `.text_N`。

## 9. Mixin

0.1.8.0 移除了调用器 mixin `CropBiomeAccessor`，由访问转换器（`META-INF/accesstransformer.cfg`）替代。以下功能使用 NeoForge 事件或数据附件，不使用 mixin：电力护甲吸收（`LivingDamageEvent.Pre`）、套装能量池、军械库来源（玩家附件 `mio_icif:armory_origin`）、僚机所有者登记、智能诱导和共振力场（tick 事件）。

保留的 mixin 及原因：

| Mixin | 原因 |
|---|---|
| `ItemStackMaxStackMixin`、`DataComponentMaxStackMixin`、`ContainerMaxStackMixin` | 堆叠数超过 99。NeoForge 没有针对编解码器上限的挂钩。 |
| `LivingEquipEnergyMixin` | 只有储能不同的物品堆不能触发装备音效和游戏事件。没有可取消的 NeoForge 事件。 |
| `ArmorEquipSoundMixin` | 其他模组的空装备音效会使脚步声模组崩溃。该 mixin 提供回退值。 |
| `client.ItemInHandReequipMixin` | 对所有物品（包括附属物品）的纯能量变化不播放重新装备动画。`IItemExtension` 只覆盖本模组的物品。 |
| `client.AdvancementTabMiophaMixin`、`client.AdvancementWidgetHintMixin` | 进度界面布局。没有对应事件。 |
| `client.FcsGlowMixin`、`client.FcsTeamColorMixin` | 套装传感器的客户端描边发光。没有对应事件。 |
| `client.PlayerManeuverPoseMixin` | 飞行时的肢体姿态。`RenderPlayerEvent` 只能修改姿态矩阵，不能修改模型部件。 |
| `IndependentChunkMixin`（核心） | 能量网络拓扑时钟，以及电缆/变压器方块实体工厂。NeoForge 没有创建方块实体的挂钩。 |

## 10. 版本规则（建议方案）

以下规则是对 API 的建议，项目尚未正式采用。

* 版本迭代规则中，版本格式为 0.1.x.y
* 在该版本格式下，0 意为非正式版，1 代表奇点迭代第一系列版本，x代表该系列里第x个大更新内容，y代表在第x个更新内容的基础上进行的第y个增添或漏洞补正
* 小版本（0.1.8.x）保持 API 源码兼容。删除内容会提前一个版本公告。
* 标记为 `@Deprecated` 的 API 保留一个小版本。
* 运行时请检查 `MioIcifAPI.instance()`。不要调用内部类。
