# 杂交农业与 IC2 1.12.2 对齐审计

> 后续状态：本文件保留实施前的审计记录，以下“当前”均指修复前。2026-09-29 已实施对齐；修复范围、兼容策略和验证见 [实施记录](crop_ic2_alignment_progress.md)。

日期：2026-09-29。对象：当前重构后的主源码。参考：本地 `source/IC2Code 1.12.2`，反编译文件标识为 IC2 `2.8.221-ex112`；本报告采用 Experimental 路径，不把 Classic 作物管理器混入比较。

**结论：尚未对齐。差异覆盖交互入口、生长引擎、杂交概率、遗传、环境、杂草、种袋、产出及农业机器，不能通过只调整作物等级解决。** 架构重构完成不代表玩法已经复原；本审计也不判断每个差异是在重构时引入还是更早已存在。

本轮执行源码调用链核查和作物参数静态比对，未改变游戏实现，未启动客户端，也未把静态推断写成实机复现结果。这里的“缺失”限定为已检查的内置实现；附属可以自行调用接口或写入数据，不能据此证明原版玩家交互已存在。

## 1. 参数核查结果

实际注册入口为 [CropRegistryEvent](../src/main/java/com/miophas/singularity_iteration/common/event/CropRegistryEvent.java)，由主模组公共事件总线注册。现有目录有 54 种作物：对应 IC2 的 52 种，加项目扩展的钛、铀作物。

对 52 种对应作物逐项比较“等级 + 五项属性”的构造参数：**29 种不同、23 种数字一致**。后者包括 15 种 generic 作物、6 种树苗、西瓜和食人花。数字一致不代表生长、收获和特殊行为一致。

复查工具：[compare_catalog.py](../tools/crop-audit/compare_catalog.py)，只读取源码，在终端输出完整 52 行对照表：

```powershell
python tools/crop-audit/compare_catalog.py
```

部分等级差异如下：

| 作物 | 当前等级 | IC2 等级 |
| --- | ---: | ---: |
| 咖啡 | 2 | 7 |
| 啤酒花 | 2 | 5 |
| 红麦 | 3 | 6 |
| 地狱疣 / 地元疣 | 3 | 5 |
| 铁叶草 / 铜 / 锡 | 4 | 6 |
| 铅 / 金 / 银 | 5 | 6 |
| 胡萝卜 / 马铃薯 | 1 | 2 |
| 南瓜 | 2 | 1 |

属性名称也要注意：IC2 顺序是 `tier, chemistry, consumable, defensive, colorful, weed`，当前 `PlantStats` 是 `level, chemistry, nutrition, color, medicinal, danger`。本次只按参数位置比较，未把不同名字强行解释成同一种属性。修复时应保留已有 API 签名，明确原版语义与兼容映射。

## 2. 优先修复：玩法入口与阶段推进

### A01：普通交互没有创建杂交架的路径（最高优先级）

- 当前两个作物架的 `useItemOn` 仅转发基础种子种植；没有“拿作物架右键空架，设置 crossing base”的处理。
- 全主源码中 `setHybridBase` 的调用只发现设置 `false`，没有正常交互设置 `true`；构造默认也为 false。名为 upgraded 的独立方块同样创建默认作物实体，并不会自动进入杂交态。
- tick 只有 `plant == null && hybridBase` 时才调用杂交。结果是新放的架子按原版教程操作，无法从当前内置交互进入这条路径。已有 NBT 或附属手动设置该值属于另一路径。
- IC2：`TileEntityCrop.rightClick` 第 475–482 行消耗一根作物架并设置 crossingBase。

证据：[普通架](../src/main/java/com/miophas/singularity_iteration/common/block/crop/mio_icif_crop_stick.java)、[升级架](../src/main/java/com/miophas/singularity_iteration/common/block/crop/mio_icif_crop_stick_upgraded.java)、[作物实体](../src/main/java/com/miophas/singularity_iteration/common/blockentity/crop/mio_icif_crop_entity.java) 第 72–86 行；[原版实体](<../source/IC2Code 1.12.2/ic2/core/crop/TileEntityCrop.java>)。

### A02：地狱疣的覆盖 tick 丢失阶段推进（最高优先级）

`mio_icif_crop_entity.tick` 只调用 `plant.tick(this)`；通用阶段推进在 `PlantType.tick` 内。但 `PlantNetherWart.tick` 覆盖后只在直接下方是灵魂沙/灵魂土时随机加 100 progress，不调用父类，也不检查阈值或增加阶段。因此即便 progress 增长，当前正常实体 tick 路径仍不会让它升级生长阶段。

IC2 将 `crop.tick` 特殊行为与 `performGrowthTick`、阈值推进分开调用，覆盖作物 tick 不会取消正常生长。

证据：[PlantNetherWart](../src/main/java/com/miophas/singularity_iteration/common/crop/PlantNetherWart.java) 第 78 行；[PlantType](../src/main/java/com/miophas/singularity_iteration/core/api/crop/PlantType.java) 第 69 行；原版 `performTick` 第 271–285 行。

### A03：收获和取种交互不等价

- 当前左键 `attack` 调用 `doManualHarvest`，不是原版左键的 `pick`；右键空手没有相应 `useWithoutItem` 收获分发。`PlantType.onInteract` 在主源码中没有找到调用点。
- 当前 `pick()` 只重置作物，不生成种子；原版按成熟度、等级、G/G/R 计算 0–2 份种子后移除植株。
- 当前普通收获额外随机掉种，原版普通收获与 uproot/pick 的取种用途分开。原版基础作物只有三项属性均不超过 1 时才回落普通种子，否则可获得保留属性的种袋。
- 当前空种袋可从成熟作物复制属性，操作不消耗或退化母株；种植还返还空袋。这是额外玩法，严格对齐时应改为显式扩展规则，不能当作 IC2 原行为。
- `makeSeeds(plant, count, ...)` 没有使用 count；空袋收集 `fillOne` 把扫描等级设为 0，未继承植株扫描信息。

证据：[CropSeedItem](../src/main/java/com/miophas/singularity_iteration/common/item/crop/CropSeedItem.java) 第 85–154 行、[CropSeedData](../src/main/java/com/miophas/singularity_iteration/core/runtime/processing/CropSeedData.java)、作物实体第 146–177、248 行；原版 `pick` 第 820 行与 [CropVanilla](<../source/IC2Code 1.12.2/ic2/core/crop/CropVanilla.java>)。

## 3. 生长、环境和杂交引擎

| 项目 | 当前实现 | IC2 参考实现 | 影响 |
| --- | --- | --- | --- |
| 生长调度 | 每游戏 tick 执行 PlantType.tick；512 tick 只驱动杂交/同步 | 默认每 256 tick 执行一次作物周期 | 不能把 512 与 256 直接解释成整体生长速度差一倍 |
| 生长增量 | `max(1, 1 + growth)` | `3 + random(7) + growth`，再按环境质量修正 | 随机性、G/G/R 代价不同 |
| 生长阈值 | 普通等级 1–5 为 17391 / 36036 / 56074 / 77669 / 102000；部分作物另有覆盖 | 默认 tier × 200 **生长点**，特殊作物覆盖 | 两边数值单位与调度共同决定时长，不能只照抄阈值 |
| 环境供给 | 湿度固定 5；耕地养分固定 3；空气只按高度算 | 气候、温度、耕地湿度、储水、土层、储肥、邻域阻塞和天空共同影响 | 生物群系、堆土和种植环境优化失效或不同 |
| 环境需求 | `weightInfluences` 存在，但正常生长没有调用 | 最低质量 `(tier-1)*4 + growth + gain + resistance`；供给为加权环境 ×5 | 高属性/高级作物的环境压力与死亡风险未复原 |
| 低环境惩罚 | 无对应正常生长处理 | 不足时减速；严重不足按 resistance 随机死亡 | 31/31/31 等属性布局行为不同 |
| 资源消耗 | 实体没有按作物周期减少 nutrients / water | 每作物周期各减 1（大于 0 时） | 普通作物无需持续管理，管理器缺少正常补给需求 |
| 杂交入口概率 | 每 512 tick，1/4 尝试 | 每作物周期，1/3 尝试，再做亲本参与判定 | 成功速率不同 |
| 亲本选择 | 固定使用可杂交邻居列表前两株 | 四个水平邻居分别按成熟条件、目标环境及 G/R 概率参与；至少两株 | 第三、第四亲本无贡献；高 resistance 对繁殖抑制缺失 |
| 后代物种 | 同种直接返回同种；否则双方共有标签的候选均匀随机，空集回落亲本 | 对可在目标生长的候选，累计全部亲本的属性差、大小写无关标签、tier 权重；同种权重 500 | 同种亲本不再产生原版允许的变种，候选池和概率均不等价 |
| 属性遗传 | 两亲本平均向上取整，变异 -1..1 | 所有参与亲本均值向下取整，n 亲本变异 -n..n | 属性培育速度和分布不同；双方最后均夹到 0..31 |
| 单邻居扩散 | 少于两株直接失败 | 恰好一个邻接作物实体时，存在独立 spreading 路径 | 原版单株复制布局不可照搬 |

证据：[PlantHybridization](../src/main/java/com/miophas/singularity_iteration/core/runtime/crop/PlantHybridization.java)、PlantType、作物实体；原版实体 `performGrowthTick`、`updateTerrain*`、`attemptCrossing`、`attemptSpreading`、`calculateRatioFor`、`checkCrossingAvailability`。

原版亲本概率的 base 初始为 4，growth ≥16、≥30 各加 1，resistance ≥28 加 `27-resistance`，再与 `random(16)` 比较。这些不是直接的 0..31 属性边界检查，不能用当前 clamp 代替。

## 4. 杂草、肥料、补水与根系

### A04：杂草体系基本未接入作物实体周期

当前 `PlantWeed` 有物种、成长与收获定义，但实体没有空架 1% 自生、邻株侵占或 Weed-EX 消耗逻辑。`PlantType.isWeed` 默认恒 false，缺少原版普通作物 size ≥2 且 growth ≥24 时的杂草行为；Venomilia 的 size=5、growth ≥8 特例也缺失。

原版实体空架杂草、侵占/抗性判断及 Weed-EX 防护在第 255–292、353–411 行；[原版 CropCard](<../source/IC2Code 1.12.2/ic2/api/crops/CropCard.java>) 第 396 行定义普通作物的杂草阈值。不能据此宣称当前已有原版高 G 的风险与抗性收益。

### A05：补给上限和操作链不一致

- 当前新架 water/nutrients 默认都为 100，setter 均限制到 100；原版储水补给目标 200，初始储量为零。
- 原版储肥低于 100 时允许手动加 100、自动加 90，结果可以超过 100；当前管理器把 `原值 +90` 截断到 100。
- 原版 Weed-EX 手动补给目标 100、自动 150；当前管理器调用 `setWeedControl(150)` 会被 setter 截为 100，是直接的内部不一致。注释中的“150天”不是 IC2 的时间单位。
- 原版 Weed-EX 是预防邻株侵占的流体；当前管理器只在已有 weed 时消耗物品耐久并清除植株，未实现相同预防行为。
- 普通架右键仅转发种植；肥料物品自身也没有对应 `useOn`。当前未发现原版手动施肥/流体容器补水/Weed-EX 的完整交互链。

证据：作物实体第 43–45、100–104 行；[作物管理器](../src/main/java/com/miophas/singularity_iteration/common/blockentity/producer/mio_icif_matron_elc.java) 第 448–536 行；原版实体第 1343–1371 行。

### A06：矿物根系检查错误

当前 `isBlockBelow` 只看紧邻下方一格，字符串版本只比较具体方块 ID；没有按根深遍历或标签匹配。原版金属作物根长为 5，在下方 1–4 格内找矿物/金属块，遇空气中止，并支持矿辞条目。

因此“作物架 → 耕地 → 铁矿/铁块”的原版布局无法让当前铁叶草从 3 长到 4。当前作物架允许直接放在实心块上，会掩盖这个问题；原版相邻更新要求脚下为耕地。1.21 迁移应使用方块/物品标签表达矿辞兼容，并保留深层搜索规则。

证据：作物实体第 240–245 行；[PlantFerru](../src/main/java/com/miophas/singularity_iteration/common/crop/PlantFerru.java)；[原版金属作物](<../source/IC2Code 1.12.2/ic2/core/crop/cropcard/CropBaseMetalCommon.java>)；原版实体第 944–983、1335 行。

## 5. 收成与特殊作物

当前收成基础倍数是 `1 + random(3 + yield)/3`，至少 1 份；多个作物自己的 `getHarvest` 又按 yield 增加数量，形成两层产量加成。`dropGainChance` 虽然定义了，实际收获路径不使用它。

原版先算 `chance = dropGainChance() * 1.03^gain`，再取 `max(0, round(gaussian * chance * 0.6827 + chance))` 份，每份分别调用 `getGains`。默认 `dropGainChance = 0.95^tier`，金属/红麦/疣类等可覆盖；原版允许零收成。当前一次抽取随机产物后再放大数量，也改变了副产物分布。

| 作物/系列 | 已确认差异 |
| --- | --- |
| 小麦 | 当前阶段 6 可收、收后回 1；原版最大阶段 7 才收、收后回 2；标签多出 Metal，数值属性也不同 |
| 红麦 | 光照 5–10 已匹配；当前提前到阶段 6 收、随机红石/小麦；缺少外部供电决定红石产出、成熟红石强度 15 和发光 7；收后回 1 而非 2 |
| Venomilia | 光照/成长阶段条件部分匹配；缺少第五阶段 grin powder、副作用中毒及碰撞退回第四阶段、特殊杂草规则；收后回 1 而非 3 |
| 黏性甘蔗 | 当前必须阶段 4 收，因此前期甘蔗产出分支正常收获不可达；原版阶段 >1 即可收，阶段 4 收后随机回 1–3；环境权重、属性和生长时长也不同 |
| 地狱疣 / 地元疣 | 地狱疣阶段推进缺陷见 A02；原版深层基底加速以及两种疣的 1/300 基底触发转化缺失 |
| 食人花 | 数字属性一致，但当前只提供简化生长和仙人掌掉落；原版阶段 4/5 可收，生长依赖特定深层基底、光照和环境；吸引、攻击、状态效果、进食增速与腐肉掉落未实现 |
| 六种树苗 | 属性、最大阶段 5、收后回 4、光照 ≥9、阶段阈值 600/150 数字匹配；当前另加 25% 原木掉落，且统一生长周期与收成分布仍不同 |
| 15 种 generic 作物 | 等级、五属性及主要阶段/阈值设置有大量直接对应；实际注册 helper 给所有作物传入空 specialDrops，原版副产物表丢失 |
| 高价值 generic 产物 | 当前 diareed 直接产钻石，原版基础产物为小堆钻石粉、钻石是副产物；ender_blossom 当前产末影珍珠，原版基础为末影珍珠粉；milk_wart、oil_berries 被牛奶桶/黏液球替代，失去原版对应作物产物链 |

注意：项目的 `IC2GenericCrops` 中还有另一套副产物工厂，但主源码未找到调用，实际注册走 `CropRegistryEvent.configuredPlants`。不能只看未接入的工厂就判定这些副产物已复原。

证据：当前 `Plant*` 类、注册 helper 第 89–97 行；[原版 IC2Crops](<../source/IC2Code 1.12.2/ic2/core/crop/IC2Crops.java>) 第 219–380 行和各 cropcard；原版实体第 873–909 行。

## 6. 分析仪与自动农业机器

| 设备 | 已匹配 | 差异 |
| --- | --- | --- |
| 作物分析仪 | 容量 100000 EU；扫描 0→1→2→3→4 的单步费用 10/90/900/9000 EU，总计 10000 EU | 当前充电速率 100、等级 1；原版 128、等级 2。当前右键田间作物只打开 GUI，缺少原版田间诊断信息；菜单会自动回送输出继续扫描，原版输出未取走不继续；当前判断具体 CropSeedItem，原版接受 ICropSeed 接口 |
| 作物管理器 | 基础水平 ±4、垂直 ±1 范围概念；自动施肥有 +90 数字 | 当前容量 1000 EU、水箱 16000 mB；原版 10000 EU，水/Weed-EX 各 2000 mB。当前 20 tick 扫一片区域择项工作；原版每 10 tick 推进一个持久扫描位置，扫点 1 EU、每项补给再 10 EU。当前先整片润耕地、每块 10 mB；原版扫描到耕地时按湿度差消耗 1–7 mB |
| 作物收割机 | 容量 10000 EU、15 个产物槽、10 tick 调度、水平 ±4/垂直 ±1 范围 | 当前每轮从固定起点搜索整片区域，轴上位置还可能重复检查；原版每轮推进一个位置。当前成功收一株固定 20 EU；参考源中 20 EU 在返回掉落项循环内扣除，另有扫点 1 EU。当前还收普通原版作物，原版该机器仅处理 TileEntityCrop |

管理器自动补给还受 A04/A05 影响：即便机器本身在工作，也不意味着作物资源循环与原版相同。收割机最优阶段选择也应按原版 `optimal size` 或 `max size` 判断，而不是把所有“能收”的阶段都当成同一种策略。

证据：[CropAnalyzerItem](../src/main/java/com/miophas/singularity_iteration/common/item/tools/CropAnalyzerItem.java)、[CropAnalyzerMenu](../src/main/java/com/miophas/singularity_iteration/common/menu/tool/CropAnalyzerMenu.java)、[收割机](../src/main/java/com/miophas/singularity_iteration/common/blockentity/producer/mio_icif_harvest_elc.java)；原版 [ItemCropnalyzer](<../source/IC2Code 1.12.2/ic2/core/item/tool/ItemCropnalyzer.java>)、[HandHeldCropnalyzer](<../source/IC2Code 1.12.2/ic2/core/item/tool/HandHeldCropnalyzer.java>)、[TileEntityCropmatron](<../source/IC2Code 1.12.2/ic2/core/block/machine/tileentity/TileEntityCropmatron.java>)、[TileEntityCropHarvester](<../source/IC2Code 1.12.2/ic2/core/block/machine/tileentity/TileEntityCropHarvester.java>)。

## 7. 扩展内容与兼容决策

- 钛/铀作物、富集种子可保留为本项目扩展，不应伪称原版 IC2 存在相同参数。
- 升级作物架暴露 1.5 生长/1.25 产量倍率 getter，但正常作物生长/收获中未找到调用。它既不是原版杂交架状态，也不能按注释声称已有倍率效果。
- 空袋复制种子、树苗产原木、管理器用耐久物品除草、收割普通作物等应逐项决定保留为扩展还是按严格 IC2 模式关闭。
- 新旧作物 ID 不必为了对齐而直接重命名（如 netherwart/nether_wart、蘑菇大小写）；应保留旧存档解析和必要别名。
- 当前公开 API 已有兼容策略。调整生长点单位、tick 回调或统计字段语义前应设计兼容入口和迁移，避免用“参数修复”悄悄改变附属的公开契约。

## 8. 建议修复顺序与验收

1. **先恢复入口和生命周期**：创建/拆除杂交架、左右键分工、种袋落地、独立的特殊 tick 与正常阶段推进；加入真实交互 GameTest。
2. **统一作物周期与环境引擎**：256 tick 调度、生长点与阶段阈值、气候/土层/天空、储肥储水衰减、环境不足死亡；为旧 progress 和储量设计迁移。
3. **恢复杂交和扩散**：全部亲本参与概率、加权候选、目标环境过滤、遗传与突变、单邻居传播；用固定随机输入校验边界，再做分布对比，不能只测“能生成一株”。
4. **恢复杂草与根系**：普通高 G 作物及 Venomilia 的杂草规则、Weed-EX 预防消耗、矿物根深与标签；至少覆盖标准“耕地下面放矿”的布局。
5. **校正全目录数据与掉落**：修正 29 种参数向量、标签、阶段与重生长状态；恢复 generic 副产物，并测试高 gain 时不出现两层产量加成。
6. **特殊行为与机器**：红麦红石/发光、毒花、食人花、疣类转化；分析仪、管理器、收割机的能耗、扫描公平性与容量。
7. **回归与存档**：真实旧作物架和种袋读回，GUI/纹理/发光，离开区块后重载，第三方种子接口；单列扩展模式与严格 IC2 行为。

现有核心构建/能源回归通过不能证明农业已对齐。本轮未发现覆盖上述杂交分布、生长与种袋完整循环的专项 GameTest。静态审计已足以确认这些差异；具体视觉效果、实际存档迁移和第三方附属仍需要运行验证。
