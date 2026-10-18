# 0.1.10.5 更新说明：修复补丁（9 项：僚机崩溃与复制、节能附魔、爆炸预警、热交换机、复制机、作物指南、铅屏蔽、配方）

基于 0.1.10.4（`pr-0.1.10.4-20261015`）。处理实机测试与玩家报告的 9 个问题，其中包括存档损坏（崩溃循环）与装备复制两个严重问题。构建环境为 Gradle 8.10.2、JDK 21、NeoForge 21.1.218，离线构建。

## 1. 僚机手持拔刀剑导致的崩溃（最严重）

**崩溃报告**（下载文件夹 `minecraft-exported-crash-info-2026-10-15T22-30-54.zip`，SI 0.1.10.3，拔刀剑：重锋 2.0.8）：

```
java.lang.IllegalArgumentException: Can't find attribute minecraft:player.sweeping_damage_ratio
  at AttributeSupplier.getAttributeInstance
  at LivingEntity.getAttributeValue
  at mods.flammpfeil.slashblade.util.AttackHelper.getSweepingBonus(AttackHelper.java:110)
  ...
  at mods.flammpfeil.slashblade.entity.EntitySlashEffect.tick(EntitySlashEffect.java:353)
Entity being ticked: slashblade:slash_effect
```

**原因**：僚机是 `Mob`，没有玩家专属属性。拔刀剑的斩击实体（所有者为僚机）在计算横扫加成时读取 `SWEEPING_DAMAGE_RATIO`（另外还读取 `ENTITY_INTERACTION_RANGE`），属性不存在时 `getAttributeValue` 抛出异常。斩击实体随存档保存，所以每次加载世界都在同一 tick 崩溃，存档无法进入。（扫描了拔刀剑 2.0.8 与 2.1.9 的字节码，两版读取的属性相同。）

**修复**：

* 僚机拥有玩家的全部属性：`LUCK`、`BLOCK_INTERACTION_RANGE`（4.5）、`ENTITY_INTERACTION_RANGE`（3.0）、`BLOCK_BREAK_SPEED`、`SUBMERGED_MINING_SPEED`、`SNEAKING_SPEED`、`MINING_EFFICIENCY`、`SWEEPING_DAMAGE_RATIO`。属性表在构造时由注册的属性生成，所以已保存的僚机也会获得这些属性：**用 0.1.10.5 打开出问题的存档即可正常加载。**
* 新 mixin `LevelEntityTickGuardMixin`（`Level#guardEntityTick`，MixinExtras `@WrapOperation`）与 `WingmanTickGuard`：
  * 僚机的 tick 抛出异常：服务器端套装归还武装库，僚机离开，主人收到聊天提示（`message.mio_icif.wingman.fault`）。
  * 僚机产生的实体（所有者链最多 4 层通向僚机，或带实体标签 `mio_icif.wingman_owned`——实体进入世界时打上，随实体保存）抛出异常：移除该实体；拔刀剑的实体还会关闭拔刀剑桥接（僚机改为把刀当普通剑使用）。
  * 其他实体不处理，原版崩溃报告与 NeoForge 的 `removeErroringEntities` 照常工作（不掩盖其他模组的错误）。
  * 首次出错记录完整堆栈，之后同类错误每分钟汇总一次，不刷屏。
* `SlashBladeCombat.tick`（僚机替刀执行 inventoryTick）加上异常捕获，出错时关闭桥接并写一条日志。
* 僚机的 `remove` 即使归还套装出错也会完成移除（否则每 tick 重复出错）；查找武装库与放下货物的异常不再中断归还。

## 2. 生物捕捉工具导致的僚机装备复制

**原因**：捕捉工具（灵魂瓶、生物囚禁工具、捕捉球等）先把实体连同装备保存进物品，再移除实体。僚机被移除时把套装送回武装库，武装库认为它已归还；再出动后，放出被捕捉的僚机就多出一整套装备。

**修复 A：阻止捕捉**

* 对僚机使用任何物品都无效：`mobInteract` 返回 CONSUME（不再调用物品的 `interactLivingEntity`），并以最高优先级取消 `PlayerInteractEvent.EntityInteract` 与 `EntityInteractSpecific`（不依赖 `mobInteract` 的工具也拦住）。主人空手潜行召回不受影响。
* 僚机加入以下实体类型标签：`c:capturing_not_supported`、`c:teleporting_not_supported`、`industrialforegoing:mob_imprisonment_tool_blacklist`、`ars_nouveau:jar_blacklist`、`enderio:soul_vial_blacklist`（覆盖发射器、投掷物等绕过交互的捕捉方式）。

**修复 B：出击令牌（最终保障）**

* 每次出动生成一个出击令牌（实体 NBT `Sortie`；世界数据 `data/mio_icif_wingman_sorties.dat`）。套装归还时令牌关闭（保留最近 4096 个已关闭令牌）。
* 僚机进入世界时（`EntityJoinLevelEvent`，最高优先级）检查令牌：令牌已关闭，或已有另一个已加载僚机持有同一令牌，即判定为副本——取消进入，清空装备且**不掉落**，记录警告并通知主人（`message.mio_icif.wingman.copy_removed`）。
* 捕捉工具以“不销毁”方式移除再放出（令牌仍有效）时，令牌改绑到新实体，正常继续。
* 没有令牌的僚机（0.1.10.4 及更早保存）会被收养；未知令牌也收养，从不因为账本丢失而吞掉真实装备。**限制**：0.1.10.5 之前已被捕捉、更新后才放出的僚机无法与正常僚机区分。

## 3. 节能附魔使智能诱导失效（及全面检查）

**原因**：`SuitEnergyPool.pay` 返回节能后实际扣除的 EU。智能诱导写成 `extract(...) < GUIDED_EU`：带节能附魔的头盔实际扣除少于名义费用，被判定为“电量不足”，诱导从不生效（预测诱导协同也因此失效）。同类问题：僚机维持费用按名义 160 EU/秒计算，节能使部件付得更少，差额由武装库重复支付；武装库未加载时僚机会因“低电量”返航。

**修复**：

* 新方法 `EnergySaving.maxCost(stack, amount)`（节能后费用向上取整，`apply` 永远不会更多）；套装侧 `SuitEnergyPool.cost / canPay / payAll / payPart`，`SuitModules.canPay`。
* 智能诱导：`payAll`（全有或全无）。
* 僚机维持：按名义费用计算各部件覆盖的份额（`payPart`），剩余部分才由武装库支付。
* 全面检查后改用节能后费用判断“够不够电”的位置：
  * 套装单元的启用判断（`SuitModules.isActive` 两处）、演算核心状态、共振力场、相干度回复、纳米储备回复、纳米自修复、引力核心与推进核心修复、推进核心电弧放电、偏导护盾（同样电量可抵消更多伤害）。
  * API 默认方法与预制类：`IWeaponItem#canShoot`、`IJetpackItem#canFlyTick`、`IElectricToolItem#hasEnoughEnergy`、`IElectricArmorItem#hasEnoughEnergy`、`AbstractElectricTool#hasEnoughEnergy`（两个重载）、`AbstractElectricArmor#hasEnoughEnergy`；以及迷彩喷涂器、电动鱼竿、电动护甲。
* 已确认无误：`BatteryTransfer.consume`、`consumeFlyTick`、喷气背包控制器（均为“节能后全有或全无”）；量子行动、协同、框架核心的其余费用只扣费不比较。

## 4. 爆炸预警：通用检测

此前只识别原版已点燃 TNT、TNT 矿车与膨胀的苦力怕。新 `BlastDetector`（客户端）：

| 类别 | 半径 | 计时 |
|---|---|---|
| 工业 TNT、核弹、炸药、粘性炸药（SI） | 8 / 40 / 3 / 2 | 同步的引信；遥控粘性炸药无计时 |
| `PrimedTnt` 及其子类（其他模组的 TNT）、TNT 矿车、苦力怕及其子类 | 4 / 4 / 3（闪电 6） | 引信 / 膨胀 |
| 爆炸类投射物：SI 火箭弹、恶魂火球、凋灵之首、末影龙火球、名称含爆炸词的投射物 | 5 / 2 / 2 / 3 / 3 | 到最近接近点的时间；只在朝穿戴者飞来且会进入半径时预警 |
| 其他模组：实体类型标签 `mio_icif:blast_warning`，或注册名含 tnt、nuke、bomb、dynamite、grenade、explosive、missile、warhead、landmine（及整词 c4、primed、rocket）的非生物实体 | 4（核类词 24） | 反射读取公开的 `getFuse`/`getFuseTime`/`getFuseTicks`/`getFuseTimer`，否则无计时 |

* 新数据映射 `mio_icif:blast_warning`（实体类型，同步到客户端）：`radius`、`fuse`、`label`，可以描述任何模组的爆炸物；SI 的爆炸物也写在默认数据映射中作为示例。
* 无计时的爆炸物 HUD 显示“--”，不闪红，蜂鸣以慢节奏响起。
* 大半径爆炸物（核弹）在 100 格内预警（每 10 tick 进行一次大范围查询）。
* 类信息按实体类型缓存，标签或数据映射更新时清空。

## 5. 热交换机的传热面

* 检查结果：代码、方块状态与模型已经一致——热量只从 `FACING` 面（铜质传热板，放置时朝向玩家）输出，能力面、兜底推送与 `ThermalOutput` 都使用同一面；与 IC2 原版（液体热交换机的 front 为铜板、`facingMatchesDirection`）一致。
* 实际问题：热交换机借用了流体加热机的贴图（背面画着两根蓝管），而 SI 自带、来自用户 `resources.zip` 的液体热交换机贴图 `block/producer/block_liquid_heat_ex_ec.png` 从未使用。现在改用该贴图：正面铜板、背面为纯铆钉面、两侧管道；工作时使用激活帧（红色传热板、蓝色管道），以前开关状态看起来相同。
* 指南（三种语言）新增“传热面”一节：传热板朝向、各种用热机器的受热面、扳手的转向方法。

## 6. 复制机：默认可复制物品扩充与数据包清单

* 新的复制清单机制：`data/<命名空间>/uu_replication/*.json`，条目为 `item` 或 `tag`、`uu`（每件 mB）、可选 `eu`（每件 EU）、`deny`、`required`。清单价格是固定价格，优先于 IC2 参照表与配方推导，由其合成的物品也会推导出价格。错误条目只警告并跳过，不影响整个价格表。数据包与 KubeJS（写入数据包文件）都可以使用。
* 每件 EU：复制机按 0.1 mB 一步工作，每步 EU = 每件 EU ÷ 步数（1～8,192 EU）；没有设置时仍为每步 512 EU（每 mB 5,120 EU）。
* 默认清单 `data/mio_icif/uu_replication/defaults.json`（95 条）：1.13 之后的矿石与粗矿（铜、深层矿石、远古残骸）、下界石英、紫水晶、新石材、生物掉落物（恶魂之泪、潜影壳、海晶碎片、幻翼膜、墨囊、鹦鹉螺壳、海洋之心、不死图腾、凋灵骷髅头颅、下界之星、生肉与鱼）、植物（藤蔓、下界疣、竹子、海带、浆果、紫颂果、新树苗）与 SI 的深层矿石、粗矿和粘性树脂。龙蛋明确禁止。所有 ID 已对照 1.21.1 原版注册表与 SI 注册表检查；IC2 参照表中已有有限价格的物品（例如海绵）不覆盖。
* 日志：`[UU] Replication lists: N entries from M files (...)`。
* 文档：复制机指南（三种语言）与 API 指南第 12.1 节。

## 7. 作物与杂交指南

GuideME 手册此前没有作物页面。新增“作物与杂交”（三种语言），内容取自代码：

* 周期（256 tick）、需求 = （等级−1）×4 + 生长 + 产量 + 抗性，品质 = 5 ×（湿度 + 营养 + 空气质量），惩罚与死亡判定，特殊权重（咖啡、可可、甘蔗、南瓜/西瓜），作物监护机的 +50。
* 湿度（生物群系数值表、湿润耕地、储水档位）、营养（生物群系加成、耕地下 3 格泥土、储存营养档位）、空气质量（高度、西/北/西北三格、天空）、各作物的光照条件、夜间光源距离。
* 杂交：杂交作物架、参与几率公式、后代选择、属性变异范围、单邻居复制；环境不改变杂交几率。
* 布置技巧。

## 8. 铅块辐射屏蔽

* 新类 `RadiationShielding`：从辐射源到生物的直线上，`mio_icif:radiation_shielding`（铅块、`#c:storage_blocks/lead`）每块只让 10 % 通过，`mio_icif:radiation_shielding_weak`（强化石头、黑曜石、哭泣的黑曜石、强化深板岩、铁块、下界合金块、钢块）每块 50 %；低于 5 % 时完全屏蔽。生物按“到眼睛”和“到身体中心”两条线中较开放的一条受辐射；只计算生物前 48 格，未加载区块视为空气（不加载区块）。
* 适用于：反应堆 70 % 堆温阶段（伤害乘以透过率）、核爆炸产生的放射性方块（效果时长乘以透过率，透过率低于 50 % 时不给虚弱/凋零）、核爆炸辐射区（需要朝向爆心的屏蔽和屋顶两者）。
* 反应堆指南（三种语言）新增“辐射屏蔽”一节。

## 9. 缺失的配方

* 铱中子反射板：0.1.10.4 已加入配方（6 加厚中子反射板 + 2 致密铜板 + 铱板）。报告来自 0.1.10.3（崩溃报告的版本），当时确实没有配方。本次确认所有材料与标签都能解析。
* 防辐射容纳盒：新增配方（8 个铅外壳围成一圈，`c:casings/lead`）。此前它只是一个没有功能的普通物品：现在是 18 格的铅盒，使用即可打开（原版双排箱子界面），内容保存在物品的 `minecraft:container` 组件中；盒子打开时不能移动；不能放入盒子或潜影盒；物品被烧毁时内容物掉出。盒内的放射性物品不辐射携带者。
* 全面检查反应堆与辐射相关物品后补充的配方：
  * MOX 核燃料（`resource/item_mox`）：此前无来源，MOX 燃料棒因此无法制作。新增 6 铀-238 + 3 钚（UUU/PPP/UUU，与铀核燃料的 UUU/CCC/UUU 同形）。
  * 锂单元：装罐机，空燃料棒 + 锂粉。
  * 近衰变同位素棒：装罐机，空燃料棒 + 铀-238（可增殖材料）。
  * 重新浓缩的铀 + 煤粉 → 铀核燃料（此前增殖产物无用途）。
  * 反应堆加热元件：4 铜板 + 4 红石 + 岩浆桶。
  * 以上为 SI 的设计（IC2 原版没有或不确定的配方），不是 IC2 原版配方。
  * 未改动：枯竭燃料棒（反应堆产出）、放射性方块、铀矿等。

## 新增与变更的文件（摘要）

* 代码：`WingmanSorties`、`WingmanTickGuard`、`LevelEntityTickGuardMixin`、`BlastDetector`、`BlastWarningData`、`UuReplicationOverrides`、`RadiationShielding`、`ContainmentBoxItem`、`ContainmentBoxMenu`；修改 `WingmanEntity`、`WingmanService`、`SlashBladeCombat`、`SuitEnergyPool`、`SuitModules`、`SmartGuidance`、`SensorTracker`、`FcsHud`、`FcsClient`、`UuPricingLifecycle`、`UuQuoteBook`、`mio_icif_replicator_elc`、反应堆、放射性方块、核辐射区等。
* 数据：上述实体与方块标签、数据映射、复制清单、6 个配方、热交换机模型。
* 文档：指南新页“作物与杂交”及反应堆、热交换机、复制机、爆炸预警、军械库各节（三种语言）；API 指南第 5.1、5.2、6.3、9、12 节（中英）。
* 工具：`tools/lang_1105.py`。

## 验证

* Gradle 离线完整构建（含 `checkCoreArchitecture` / `checkCoreDistribution`）：BUILD SUCCESSFUL。
* 新检查 `check_1105.py`：0 个问题。
  * 公共 API：与 0.1.10.4 的 javap 比较，api jar 341 个类、core jar 423 个类，丢失成员 0（新增 `EnergySaving.maxCost` 两个重载）。
  * 存档：9 个物品组件不变，724 个物品/方块 ID 全部保留。
  * 僚机：扫描拔刀剑：重锋 2.0.8 与 2.1.9 的字节码，其读取的全部属性僚机都有；mixin 注册与包装位置；守护范围；`remove` 一定调用父类。
  * 捕捉与令牌、节能（全代码无名义费用比较；`maxCost ≥ apply` 对 1～5 级、1～20,000 EU 穷举）、爆炸预警、热交换机贴图列、复制清单（ID 对照 1.21.1 源码与 SI 注册表、不覆盖 IC2 有限价格）、屏蔽标签、新配方的物品与标签、指南（585 页 MDX 规则、作物页与 `CropGrowthMath` 一致）、语言键。
* 回归：`check_1104`～`check_1100`、`check_192`～`check_183`、三语注释检查（0 错误，4 个已知长句警告）：全部通过。
* 已编写并编译、本机无法运行（GameTest 服务器无法启动）：`Round1105GameTests`（僚机属性、守护范围、捕捉与令牌、节能付费、爆炸数据映射、复制清单价格、铅屏蔽透过率、配方与防辐射容纳盒、热交换机模型）。
* 未在游戏内验证：拔刀剑僚机的实际战斗与出错时的归还、各模组捕捉工具、爆炸预警 HUD、热交换机外观、复制机清单的实际复制、作物指南的显示、铅屏蔽的实际伤害、容纳盒界面、多人游戏。