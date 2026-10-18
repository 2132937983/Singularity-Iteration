# 奇点迭代（Singularity Iteration）API 指南（0.1.10.2）

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
* `core.api` 主要类型（`docs/dev/trilingual_manifest.txt`）的注释包含英文（ASD-STE100）、中文与日文三部分（见 `docs/dev/COMMENT_STYLE.md`），以英文为准。
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

### 3.3 纳米套装单元与爆发（0.1.8.5）

自 0.1.8.5 起纳米套装也可以安装单元。纳米部件按晶格点数（LP）计：头盔 4、胸甲 6、护腿 4、靴子 4，每个致密剂 +2，最多 2 个。
两参数的 `register` 不变：这样注册的单元仅限量子。

| 类型 | 作用 |
|---|---|
| `SuitClass` | `QUANTUM` 或 `NANO`。 |
| `SuitModuleTraits` | 纳米部件上的 LP（1..6）、适用的套装类别与可选的爆发。`nano(lp, burst)`、`shared(lp)`、`QUANTUM_ONLY`。 |
| `SuitBurstSpec` | 爆发消耗：储备点、热量、冷却 tick、是否持续型。 |
| `SuitBurstBehavior` | 服务器上的 `fire(player, piece, input)`。爆发成功时返回真；只有此时才支付消耗。 |
| `SuitNanoCore` | 玩家储备、热量、热量上限与过热状态的只读视图。 |
| `ArmorCapacityBonus`（`core.api.item`） | 单件电力护甲由自身数据带来的额外 EU 容量。 |

步骤：带特性注册。

```java
SuitModuleRegistry.register(SPEC, SuitModuleBehavior.NONE,
    SuitModuleTraits.nano(2, new SuitBurstSpec(15, 10, 60, false)),
    (player, piece, input) -> { /* 爆发内容 */ return true; });
```

规则：

* 爆发单元必须同时提供 `traits.burst()` 与爆发行为。
* 穿戴者在奇点管理面板中把爆发分配给 V 或 B 键。
* 调用 `fire` 之前，奇点迭代会检查单元（装在已穿戴的纳米部件中、已开启、有电）、过热锁定、冷却与储备。
* 附属爆发为单次型；0.1.8.5 中附属单元的持续型标志会被忽略。

### 3.4 框架核心（0.1.9.0）

套装头盔或胸甲的核心插槽可装一个框架核心。插槽是数据组件 `mio_icif:suit_core`（字符串 ID：`solar_array_1`、`solar_array_2`、`solar_array_3`、`graviton`、`propulsion`）。可读取它来显示或响应核心；不要写入，请使用通用套装改装台。

| 钩子（`core.prefab.flight.JetpackFlightController`） | 作用 |
|---|---|
| `THRUST_SCALE` | 胸甲喷气背包物品的推力倍率（高级推进：1.15）。 |
| `NO_CEILING` | 胸甲喷气背包物品没有飞行高度上限时为 true。 |

奇点迭代在启动时设置这两个钩子。附属若要修改，必须保留原函数并与之组合。

太阳能头盔系列、高级量子胸甲与重型量子胸甲已退役。其 ID 保留为隐藏的旧式外壳，会转换为装有核心的标准部件。其类（`mio_icif_*_solar_helmet`、`mio_icif_chestplate_advanced_quantum`、`mio_icif_chestplate_heavy_quantum`）已删除；引用它们的附属应改为检查核心。

### 3.5 量子行动（0.1.10.0）

自 0.1.10.0 起，量子单元可以在行动键（V 与 B，与纳米爆发共用）上拥有主动能力。行动消耗量子相干度（0..100，每个相干电容器 +50，由 EU 回复）。相干度降到 0 时核心退相干数秒，拒绝一切行动。没有行动的单元不受影响：0.1.9.2 的所有类型与签名均未改变。

| 类型 | 作用 |
|---|---|
| `SuitActionSpec` | 相干度消耗、冷却 tick、是否持续型、持续型每秒消耗。`single(cost, cooldown)`、`sustained(start, perSecond, cooldown)`。 |
| `SuitActionBehavior` | 服务器上的 `fire(player, piece, input)`；成功时返回真（只有此时才支付消耗）。持续型行动每 tick 还会收到 `sustain`（返回假则结束）与最后的 `end`。 |
| `SuitQuantumActions` | `register(unitId, spec, behavior)`、`get(key)`、`all()`。 |
| `SuitQuantumCore` | 玩家相干度、相干度上限与退相干锁定的只读视图。 |

步骤：照旧注册单元，再注册其行动。

```java
SuitModuleRegistry.register(SPEC, SuitModuleBehavior.NONE);                // 量子单元
SuitQuantumActions.register(SPEC.id(), SuitActionSpec.single(15, 200),
    (player, piece, input) -> { /* 行动 */ return true; });
```

规则：

* 单元必须允许量子类别，并位于穿戴中的量子部件上；穿戴者在奇点管理面板（量子核心分区）把行动分配给 V 或 B。
* 奇点迭代在 `fire` 之前检查单元（已安装、开启、有能量）、退相干锁定、冷却与相干度。持续型行动需要多于启动消耗的相干度；再次按键、`sustain` 返回假、相干度耗尽（退相干）或单元停止时结束。
* ID 必须带附属命名空间；请在服务器启动前在两端注册。

### 3.6 纠缠协同与演算带宽（0.1.10.1）

自 0.1.10.1 起，穿戴者可以在量子胸甲上把两个量子单元链接起来（2 条链接，演算核心启用时 3 条）。链接的一对单元构成协同。
量子部件上每个生效的单元还会占用套装计算机（演算带宽，QB）。超出预算（20 QB，装有演算核心时 28 QB）时，传感器刷新减半，相干度回复速度减半。
现有附属不受影响：0.1.10.0 的所有类型与签名均未改变。

| 类型 | 功能 |
|---|---|
| `SuitSynergies` | `register(id, unitA, unitB)`、`get(key)`、`all()`、`active(player, id)`。这一对单元会显示在纠缠矩阵中；效果由附属在 `active` 为真处实现。名称：`Entry.nameKey()` 与 `nameKey + ".desc"`。 |
| `SuitBandwidth` | `register(unitId, cost)`（0..16 QB）、`cost(key)`、`load(player)`、`budget(player)`。 |

步骤：注册协同，然后在效果生效处检测。

```java
SuitSynergies.register(ResourceLocation.fromNamespaceAndPath(MODID, "deep_scan"), MODID + ":sonar_unit", "ore_scanner");
SuitBandwidth.register(ResourceLocation.fromNamespaceAndPath(MODID, "sonar_unit"), 2);

if (SuitSynergies.active(player, DEEP_SCAN)) { /* 协同效果 */ }
```

规则：

* 两个单元都必须安装在穿戴中的量子部件上并处于生效状态（已安装、开启、有能量）。
* 单元键为内置 ID（`"entity_esp"`）或已注册的附属 ID（`"addon:unit"`）。两个键必须不同。
* 未注册消耗的附属单元占用 0 QB（与 0.1.10.0 相同）。
* ID 必须带附属命名空间；请在服务器启动前在两端注册。指向已移除附属协同的链接会保留在胸甲上，但不起作用。

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

### 4.3 喷气背包推进器音效（0.1.8.1）

实现 `IJetpackItem` 并调用 `JetpackFlightController.tick` 的喷气背包物品，无需其他代码即可获得推进器音效。客户端播放点火音、随推力变化的循环音和熄火音。其他玩家通过服务器只在推力状态变化时发送的小数据包听到声音。

1. 覆盖 `getThrusterSound()` 选择音效档：`THRUSTER_ELECTRIC`（轻型涡轮，默认）、`THRUSTER_ADVANCED`（重型轰鸣）或 `THRUSTER_IONIC`（低沉离子嗡鸣）。
2. 不要为同一次飞行再播放自己的循环音，两种声音会重叠。

未知值使用 electric 档。
## 5. 军械库（`core.api.armory`）

奇点迭代在服务器线程上把以下事件发布到 `NeoForge.EVENT_BUS`。

| 事件 | 时机 | 可取消 |
|---|---|---|
| `ArmoryEvent.Summon` | 套装飞向穿戴者（玩家或同伴）之前。 | 是。不消耗能量，不移动物品。 |
| `ArmoryEvent.PieceDocked` | 飞行部件装备完成之后。 | 否。 |
| `ArmoryEvent.WingmanDeploy` | 储存的套装作为僚机出动之前。 | 是。 |
| `ArmoryEvent.WingmanCommand`（0.1.10.2） | 来自指挥轮盘或全息台的量子指挥命令执行之前：MOVE、HOLD、SUPPRESS、FOCUS、FORM_UP、MODE、SQUAD、RETURN、QUANTUM_RECALL。`getOrder()`、`getWingmen()`、`getPoint()`（定点命令）、`getTarget()`（FOCUS）。 | 是：该命令不改变任何东西。 |

### 5.1 数据标签

| 标签 | 类型 | 功能 |
|---|---|---|
| `mio_icif:armory/remote_equip` | 实体类型 | 可以从军械库接收套装的同伴。实体必须是 `OwnableEntity` 且归该玩家所有。默认：`touhou_little_maid:maid`（可选条目）。 |
| `mio_icif:armory/drawn_blade` | 物品 | 以拔刀姿态飞到手中的物品。拔刀剑：重锋（SlashBlade: Resharped）的刀无需此标签即可识别。 |
| `mio_icif:wingman/fulton_immune` | 实体类型 | 富尔顿回收不能带走的生物。首领（`c:bosses`）始终免疫。 |
| `mio_icif:resonance_immune` | 方块 | 力场共振解体不会破坏的方块。 |
| `mio_icif:radiation_shielding`（0.1.10.5） | 方块 | 强辐射屏蔽：直线上每个方块只让 10 % 通过。默认：铅块与 `#c:storage_blocks/lead`。 |
| `mio_icif:radiation_shielding_weak`（0.1.10.5） | 方块 | 弱辐射屏蔽：每个方块让 50 % 通过。默认：强化石头、黑曜石、哭泣的黑曜石、强化深板岩、铁块、下界合金块、`#c:storage_blocks/steel`。 |
| `mio_icif:blast_warning`（0.1.10.5） | 实体类型 | 爆炸预警单元把该实体视为爆炸物（半径 4）。半径、引信与标签用同名数据映射设置（见第 12 节）。 |
| `c:capturing_not_supported` 等（0.1.10.5） | 实体类型 | 僚机加入 `c:capturing_not_supported`、`c:teleporting_not_supported`、`industrialforegoing:mob_imprisonment_tool_blacklist`、`ars_nouveau:jar_blacklist`、`enderio:soul_vial_blacklist`。 |

### 5.2 僚机的出击令牌与崩溃保护（0.1.10.5）

* 每次出动生成一个出击令牌（实体 NBT `Sortie`，世界数据 `mio_icif_wingman_sorties`）。套装归还时令牌关闭。带着已关闭令牌、或与已加载僚机同令牌进入世界的僚机是副本：`EntityJoinLevelEvent` 被取消，其物品清空且不掉落。没有令牌的旧僚机会被收养。
* 对僚机使用任何物品都无效（`mobInteract` 返回 CONSUME，`PlayerInteractEvent.EntityInteract` 与 `EntityInteractSpecific` 被取消）。不要依赖“对僚机右键物品”的交互。
* `LevelEntityTickGuardMixin` 包住每个实体的 tick：僚机或其所有者链通向僚机的实体（或带实体标签 `mio_icif.wingman_owned`）抛出异常时，僚机归还套装并离开，衍生实体被移除；其他实体照常走原版崩溃报告。
* 僚机拥有玩家的全部属性（`ENTITY_INTERACTION_RANGE`、`SWEEPING_DAMAGE_RATIO` 等），给 `Mob` 读取玩家属性的武器模组不再抛出异常。

## 6. 能量（`core.api.energy`）

0.1.8.0 没有改变能量 API 的约定。EU 网络默认使用公平分配（`config/core.ini` 中的 `distributionMode=fair`，或通用配置）。

1. 用 `EnergyNodeRegistry.register(id, profile)` 注册每个方块的角色。使用 `EnergyNodeProfile.generator(packet)`、`storage(packet)` 或 `consumer()`。
2. 提供方块能力 `EUApi.SIDED`。对于预制方块实体，调用 `CoreCapabilityRegistration.registerEntity(event, type)`。
3. 网络分四个阶段供电：先按额定输入向用电器供电，再填满用电器的剩余空间，然后按额定输入向储能设备供电，最后填满储能设备的剩余空间。每个阶段内，接收方按其剩余需求的比例获得能量。发电机先于储能设备供电。

### 6.1 电池堆叠（`core.prefab.item.AbstractBattery`，0.1.10.3）

* 只有空电池可以堆叠。堆叠数大于 1 的电池在没有电量时可堆叠到该数量；一旦有电量，`getMaxStackSize(ItemStack)` 使该堆不再增长，因此两个有电的电池即使电量相同也不会合并。
* 新构造函数 `AbstractBattery(Properties, maxEnergy, initialEnergy, chargeRate, maxStackSize, emptyStackSize)`：`maxStackSize` 为 1 时保留不可堆叠的存储方式（默认组件保存 `initialEnergy`，旧存档电量不变），空电池仍可堆叠到 `emptyStackSize`。`getEmptyStackSize()` 返回该数量。
* 可堆叠的空电池不带 `mio_icif:battery_energy` 组件：新的、合成的与放空的电池是同一种堆。旧版本以显式 0 保存的堆在加载时规范化（`verifyComponentsAfterLoad`）。
* 多于一个的电池堆从不接收或输出电量：`addEnergy`、`extractEnergy`、FE 能力与物品 API 的转移都拒绝它；对可堆叠电池的这种堆，`setEnergy` 忽略正值。机器电池槽只放一个电池，所以把一堆放进充电槽时只留下一个。
* 依赖“同电量的有电电池可堆叠”的附属电池可以重写 `getMaxStackSize(ItemStack)`。

### 6.2 机器关闭的面（`IEnergyBlock#canConnect`，0.1.10.4）

* EU 网络建立端口时，会向每个基于 `AbstractEnergyBlockEntity` 的机器询问 `canConnect(side)`。返回 false 的面不接收也不输出 EU，直接相贴的发电机或多方块电源（核反应堆及其反应仓）也一样；该面的外部 EU 与 FE 能力同样拒绝。
* 基类开放全部六个面，因此现有机器的面不变。
* 结果会变化的机器（例如取决于相邻方块）调用 `IndependentSiEnergy.changed(blockEntity)`，让网络重建端口。反应堆冷却液注入器用它关闭接触反应堆的面。

### 6.3 节能附魔与费用检查（`core.api.item.EnergySaving`，0.1.10.5）

* 每次扣费都先经过 `EnergySaving.apply`，实际扣除的 EU 低于名义费用，而且小额费用可能为 0。**不要拿实际扣除量和名义费用比较。**
* 扣费前的电量检查使用新方法 `EnergySaving.maxCost(stack, amount)`（节能后的费用向上取整，`apply` 永远不会更多）。`IWeaponItem#canShoot`、`IJetpackItem#canFlyTick`、`IElectricToolItem#hasEnoughEnergy`、`IElectricArmorItem#hasEnoughEnergy` 与预制类 `AbstractElectricTool`、`AbstractElectricArmor` 的同名方法已改用它。
* 套装费用：`SuitEnergyPool.canPay` / `payAll`（全有或全无，返回是否付清）/ `payPart`（返回名义费用中被覆盖的部分）。旧版本中智能诱导把扣除量与名义费用比较，带节能头盔时从不生效。

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
| `LevelEntityTickGuardMixin`（0.1.10.5） | 包住 `Level#guardEntityTick` 中的实体 tick：僚机及其衍生实体出错时不再让世界崩溃。没有对应事件。 |
| `IndependentChunkMixin`（核心） | 能量网络拓扑时钟，以及电缆/变压器方块实体工厂。NeoForge 没有创建方块实体的挂钩。 |

## 10. 版本规则（建议方案）

以下规则是对 API 的建议，项目尚未正式采用。

* 版本迭代规则中，版本格式为 0.1.x.y
* 在该版本格式下，0 意为非正式版，1 代表奇点迭代第一系列版本，x代表该系列里第x个大更新内容，y代表在第x个更新内容的基础上进行的第y个增添或漏洞补正
* 小版本（0.1.8.x）保持 API 源码兼容。删除内容会提前一个版本公告。
* 标记为 `@Deprecated` 的 API 保留一个小版本。
* 运行时请检查 `MioIcifAPI.instance()`。不要调用内部类。

## 11. 作物与次世代作物扫描仪（0.1.8.2）

### 11.1 新增的默认方法

| 方法 | 作用 |
|---|---|
| `IPlanter#doManualHarvest(Player)` | 为玩家手动收获。掉落物放入物品栏，放不下的掉在脚下。不会产出物品的收获不执行（植物保持阶段）。默认实现调用 `doManualHarvest()`。 |
| `IPlanter#markTended(long)` / `isTended()` | 作物监护机照料。被照料的作物至少获得所需环境品质 + 50（无惩罚，生长至少 +50%）。默认实现不做任何事。 |
| `ICropSeedItem#setCropTraits(ItemStack, int, int, int)` | 写入生长／产量／抗性（各 0..31）。基因编辑使用它。种子物品有自己的存储格式时请覆盖。 |

### 11.2 数据

* 配方材料 `mio_icif:crop_seed`：指定植物与产量范围的种子袋。使用 `Ingredient#test` 检查配方的机器可以按产量调整产物。
  `{"type": "mio_icif:crop_seed", "plant": "mio_icif:wheat", "min_gain": 8, "max_gain": 15}`（`min_gain` 默认 0，`max_gain` 默认 31）。
* 物品标签 `mio_icif:crop_seed_bags`：种子袋。种子袋不在 `c:seeds` 中，因为多个模组在配方中消耗 `#c:seeds`（种子油、磨盘），会毁掉已分析的种子袋。
* 附属植物的学名（扫描仪 HUD 与菜单）：语言键 `<modId>.plant.<typeId>.scientific`。没有该键时显示 “<类型> sp.”。

### 11.3 规则

* 交配研究列出自然杂交的候选（`PlantHybridization#calculateRatio`）。自然杂交无法产生的植物也无法研究。
* 附属不要调用 `CropAnalyzerMenu`。它是内部类，菜单与槽位布局可能改变。

## 12. 数据包扩展（0.1.10.5）

### 12.1 复制清单（`data/<命名空间>/uu_replication/*.json`）

复制机的价格先来自 IC2 参照表与配方推导，复制清单在其上加入固定价格、能量费用或禁止项。清单条目是“固定价格”：优先于参照表和配方价格，由它们合成的物品也会推导出价格。

```json
{
  "entries": [
    { "item": "minecraft:netherite_scrap", "uu": 1500 },
    { "item": "minecraft:nether_star", "uu": 100000, "eu": 60000000 },
    { "tag": "c:ores/tin", "uu": 1.4 },
    { "item": "minecraft:dragon_egg", "deny": true },
    { "item": "othermod:gem", "uu": 40, "required": false }
  ]
}
```

* `item` 或 `tag`（二选一）；`uu`：每件 mB（大于 0）；`eu`：可选，每件 EU（默认每 mB 5,120 EU，复制机每 tick 最多 8,192 EU）；`deny: true`：不可扫描、不可复制；`required: false`：未知 ID 静默跳过。
* 文件按 ID（命名空间、路径）排序加载，同一物品以后者为准。错误条目只写警告并跳过，不影响其他价格。上限 4096 个物品，每个标签 512 个。
* 默认清单 `data/mio_icif/uu_replication/defaults.json`（95 条）；数据包在同一路径放文件即可替换。
* 运行时：`UuQuoteBook.energy(server, item)` 返回清单给出的每件 EU（无则 -1）。

### 12.2 爆炸预警数据映射（`mio_icif:blast_warning`）

实体类型数据映射，同步到客户端：`data/<命名空间>/data_maps/entity_type/blast_warning.json`。

```json
{ "values": { "othermod:c4": { "radius": 6, "fuse": 5, "label": "C4" } } }
```

* `radius`：危险半径（格，必填）；`fuse`：完整引信秒数（可选）；`label`：HUD 标签（可选，最多 9 个字符）。
* 计时来源顺序：实体的公开 `getFuse()`（或 `getFuseTime`、`getFuseTicks`、`getFuseTimer`，单位 tick）→ `fuse` 减去客户端上的实体年龄 → 无计时（HUD 显示“--”）。
* 没有数据映射时，实体类型标签 `mio_icif:blast_warning`、原版 `PrimedTnt` 子类、苦力怕子类、爆炸类投射物和名称含爆炸词的实体也会被识别（见指南“爆炸预警计时器”）。
