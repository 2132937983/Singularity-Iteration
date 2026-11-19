# 核电与 IC2 1.12.2 对齐审计

> 2026-09-29 更新：本文保留修复前的审计证据和数值，不代表最新实现。A1–A6、A8、A11、A13 已进入本轮修复；实施范围、回归结果及剩余项见 [核电对齐进度](reactor_ic2_alignment_progress.md)。

日期：2026-09-28。对象：当前工作区 0.1.7.14，包含上一轮冷却液分配修复。

**结论：基础参数大部分一致，完整运行行为尚未对齐。尤其是分热分配、MOX、冷媒切换、冷凝器周边和事故执行，不能据此保证原版教程布局得到相同结果。**

本次只新增审计文档及独立数值探针，没有修改生产实现。此前 18 项 GameTest 通过证明的是其覆盖的功能，不代表核电全部等价。

## 1. 基准、方法和范围

- 主基准：工作区 `source/IC2Code 1.12.2`，文件尾标注版本 `industrialcraft-2-2.8.221-ex112`。默认以 Experimental 的默认参数为目标，不混用 Classic 或 IC2 Classic。
- 当前权威路径：`common/blockentity/generator/mio_icif_nuclear_reactor_generator.java` → `common/reactor/ReactorInventory.java` → `core/runtime/reactor/ReactorCycle.java` / `FluidReactorCycle.java`。物品旧 `operate()` 方法及其注释不能代替实际世界循环。
- 静态追踪：上述路径、元件注册、流体端口、多方块、注入器、核材料/乏燃料配方、辐射及事故调用链。
- 数值验证：[探针源码](../tools/reactor-audit/ReactorAlignmentProbe.java)，运行 `./tools/reactor-audit/run.ps1`。直接编译当前核心类，不需要 Minecraft 启动或 Gradle。输出在 `build/reactor-alignment-audit/results.txt`。
- 探针 22 个观测点：13 个一致、9 个不同；不是 9 个独立缺陷，也不是全系统通过率。参考值按原版代码的最小布局逐步推导，没有在本次运行 IC2 原版游戏。
- 本地反编译源码存在数值强转丢失，例如 MOX 显示 `int / int`。另用工作区 `source/MoreElectricTools-master/libs/industrialcraft-2-2.8.195-ex112-dev.jar` 的 `javap -c` 交叉确认 MOX 的 `i2f / fdiv`，没有把反编译文本误判成整数阶跃。该字节码版本不同，不能替代 2.8.221 的整包实机验收。

下文 SI Java 路径相对 `src/main/java/com/miophas/singularity_iteration/`；IC2 Java 路径相对 `source/IC2Code 1.12.2/ic2/core/`。

## 2. 已确认的运行差异

### A1：燃料分热不是原版的逐接收方、逐燃料单元结算（高优先级，探针复现）

SI：`core/runtime/reactor/ReactorCycle.java:88,108–132`。
IC2：`item/reactor/ItemReactorUranium.java:60–98`。

原版对双联/四联中的每个燃料单元分别计算热量，以左、右、上、下收集接收元件，逐个执行 `剩余热量 / 剩余接收方数量`，每次立即处理返回余量。SI 先累加所有燃料单元热量，再按上、左、右、下分配，余数优先给前面的元件，并可能重新扫描接收方。

最小复现：单铀棒位于 `(1,1)`，仅左、上、下放置空冷却单元，本轮产热 4。

| 接收方 | IC2 | 当前 SI |
|---|---:|---:|
| 左 | 1 | 1 |
| 上 | 1 | 2 |
| 下 | 2 | 1 |

这会影响贴近散热极限的布局。溢出时也不同：左单元已有 10000/10000 热，右单元为空，单铀棒居中；原版右单元得到 1，SI 得到 2。两者都移除了左单元，但返回余量参与后续结算的方式不同。

### A2：MOX 的取值时机与隔板生效时机错误（高优先级，探针复现）

SI：`ReactorCycle.java:84–86,123,179`。
IC2：`block/reactor/tileentity/TileEntityNuclearReactorElectric.java:260–264,502–516`、`item/reactor/ItemReactorMOX.java:25–33,55–59`、`ItemReactorPlating.java:25–27`。

原版先 `pass 0 / heatrun=true` 处理热，再 `pass 1 / heatrun=false` 处理输出和燃料消耗。SI 的主要执行顺序也是先热后电，但是 `outputFactor()` 使用循环开始的 `outputHull`，分母固定 10000；相关注释把原版两阶段语义写反了。

此外，原版隔板在热阶段扫描到自身时才提高上限，SI 在循环前一次性加完所有隔板。

| 最小场景 | IC2 推导 | 当前 SI |
|---|---:|---:|
| 单 MOX，初始堆温 5000，无相邻元件 | 热阶段后 5004，输出约 15.008 EU/t | 15 EU/t |
| 同上，末尾另放 +2000 隔板 | 输出约 13.34 EU/t | 15 EU/t |
| 热核，初始堆温 5500，MOX 在前、+2000 隔板在末尾 | MOX 扫描时上限仍为 10000，本轮产热 8 | 预先按 12000 上限，本轮产热 4 |

第三种差异会直接改变热核产液需求，不能靠增加冷却液修复。

### A3：冷媒和已有热产物之间缺少配对检查（高优先级，静态确认）

SI：`common/blockentity/generator/mio_icif_fluid_reactor_handler.java:104–119` 及反应堆 `operate()`。
IC2：`TileEntityNuclearReactorElectric.java:279–298`。

`validTanks()` 只检查输入属于允许冷媒、输出属于允许热媒，没有检查两者是否配对。`commitCycle()` 优先沿用输出罐现有流体种类。

因此，在输入冷却液、输出残留热水的状态下，会按冷却液的 20 HU/mB 扣料，却增加热水数量；反向切换也存在问题。原版通过 `outputTank.canFillFluidType(add)` 拒绝这种转换，未换走的热量返回堆温。这里不仅影响原版对齐，也影响流体/热量账目。未启动游戏构造该混流场景，本项证据为调用链静态分析。

### A4：散热片熔毁当轮的排热不同（边界行为，探针复现）

SI：`ReactorCycle.java:146–151`。
IC2：`ItemReactorVent.java:25–37`、`ItemReactorHeatStorage.java:49–70`。

超频散热片已有 990 热、堆温 100：原版吸入 36 后移除槽位物品，但继续在原物品对象上执行自冷，本轮仍排出 20；SI 在 `parts[i] == null` 后跳过自冷，本轮排热 0。普通未熔毁的 36 吸热 / 20 自冷参数相同。

### A5：增殖完成后，产物替换延迟到整轮结束（边界行为，探针复现）

SI：`ReactorCycle.java:101–103,119`、`common/reactor/ReactorInventory.java` 的 `write()`。
IC2：`ItemReactorLithiumCell.java:24–34`、`ItemReactorDepletedUranium.java:25–35`。

原版达到增殖阈值就立即替换物品，之后的燃料单元不会再向原增殖元件发送有效脉冲。SI 只把进度夹到阈值，直至整轮提交才替换，期间仍把它当增殖元件。

复现：双铀棒邻接进度 9999/10000 的锂单元，堆温 3000。原版本轮产热 `24+12=36`，SI 为 `24+24=48`。

### A6：换热器读取冷凝器热量的语义不同（原版特殊行为，探针复现）

SI：`ReactorCycle.java:209–220`。
IC2：`ItemReactorCondensator.java`、`AbstractReactorComponent.java:36–37`、`ItemReactorHeatSwitch.java:59–82`。

原版冷凝器内部有私有 `getCurrentHeat(stack)`，但没有覆写供换热器调用的四参数公共方法，后者继承返回 0。SI 直接读取 `stored()`。

复现：基础换热器储热 250，相邻红石冷凝器储热 10000，堆温 0。原版冷凝器本轮升至 10012，SI 保持 10000。若目标是严格复刻原版布局，需要明确是否保留这个原版特性，不能仅按“百分比换热公式相同”认定等价。

### A7：冷凝器注入器的吞吐、库存及接触面不同（静态确认）

SI：`common/blockentity/producer/mio_icif_redstone_reactor_coolant_injector.java`、`mio_icif_lapis_reactor_coolant_injector.java`。
IC2：`block/reactor/tileentity/TileEntityAbstractRCI.java:42,197–198,216–229,246–255,280–295`。

| 项目 | IC2 | 当前 SI |
|---|---|---|
| 修复触发 | 储热严格大于 85% | 一致；红石实现等价于剩余小于 3000 |
| 消耗及效果 | 1000 EU + 一个对应方块，热量清零 | 一致 |
| 同 tick 修复 | 有资源时继续遍历多个元件 | 每 tick 最多一个 |
| 储能 | 48000 EU | 10000 EU |
| 库存 | 9 个材料槽、4 个升级槽，另有电机放电槽 | 53 个材料槽、1 个电池槽，没有升级槽 |
| 查找反应堆 | `facing.getOpposite()` | `facing` |
| EU 接收面 | 排除相邻堆芯/反应室所在面 | 当前注入器没有对应排除逻辑 |

方向字段与贴图含义在移植中可能不同，因此“前/后面接反”的玩家体验还需实机确认；吞吐、容量和升级槽差异则是明确的。这些差异会影响使用冷凝器的自动核电教程。

### A8：冷凝器工作台修复参数与配方接入有问题（静态确认）

SI：`common/item/reactor/mio_icif_lapis_condensator.java:24`、`common/recipe/mio_icif_CondensatorRepairRecipe.java:26–100`。
IC2：`assets/ic2/config/shapeless_recipes.ini:168–172`。

- 红石冷凝器：每红石修复 10000，一致。
- 青金石冷凝器：IC2 每红石修复 **5000**，当前是 **10000**；每青金石 40000 一致。
- 当前 `matches()` 允许红石冷凝器配青金石，即使实际修复量为 0。
- 当前按每个格子整叠 `stack.getCount()` 计算修复量。标准工作台完成一次配方只扣每格一个物品，若接入该配方，会出现修复量与消耗不匹配。
- 仓库内找到 serializer 注册，但未找到引用 `mio_icif:condensator_repair` 的内置配方 JSON 或该配方对象的运行时注入。仅注册 serializer/type 不会自动产生工作台配方。因此上面两个匹配/消耗问题目前属于实现风险；默认工作台是否可修复，必须同时补查配方实际加载情况，不能宣称功能已完成。外部数据包可能改变此结论。

### A9：热核成型规则不等价（静态确认）

SI：`common/multiblock/mio_icif_fluid_reactor_validator.java:21–40`。
IC2：`TileEntityNuclearReactorElectric.java:884–963`。

六个反应室、半径 2 的完整 5×5×5 外壳基本规则一致，但：

- SI 要求至少一个功能端口；原版 `hasFluidChamber()` 没有此数量限制。
- 原版会检查附近范围 4 内其他完整流体堆造成的冲突；当前验证器没有对应检查。

因此纯容器壳和相邻多堆等边界布局不能认为已对齐。

### A10：爆炸与辐射只能算移植实现，不能算原版等价（静态确认）

SI：`mio_icif_nuclear_reactor_generator.java:207–231,252–320`、`core/runtime/reactor/ReactorAccidentLatch.java:29`。
IC2：`TileEntityNuclearReactorElectric.java:408–475,682–720`、`ExplosionIC2.java`。

- 已有 40% 引燃、50% 蒸发、70% 生物伤害、85% 火/岩浆、100% 熔毁及默认爆炸上限 45；关闭上限时不执行高温效应的开关也有对应实现。
- 实际爆炸走 Minecraft `server.explode()`，不是 IC2 的 `ExplosionIC2.Type.Nuclear`。范围、射线/抗爆计算、掉落及辐射后果不能视为等价；工作区存在其他核爆调度类，不代表堆芯调用了它们。
- 当前用本轮开始的元件 `parts` 计算爆炸加成；原版在完成两阶段后遍历现存物品。燃料耗尽、反射板烧毁等当轮会不同。
- 隔板乘法先按千分之一整数逐次截断，原版用浮点累计；当前最终威力另有至少 1 的夹取。
- 当前大爆炸要等待整个爆炸窗口区块已加载；可能长期保留故障堆芯，原版流程不同。
- 高温生物伤害使用 `magic()`，原版使用 IC2 radiation 伤害源；可燃/可熔材质在新版本用标签近似。

核物品辐射另走 `common/event/mio_icif_events.java:49–70`、`common/util/NuclearRadiationHandler.java:11–23` 与 `common/effect/RadiationEffect.java`：每 20 tick 按物品标签检查，施加 100 tick/等级 1 状态并追加直接伤害；原版燃料在物品 tick 中调用 `IC2Potion.radiation.applyTo(...,200,100)`。防护判定也扩展了本模组装甲。确认存在差异，未把两个系统的总伤害做完整等效测算。

### A11：四联乏燃料离心产物少一份铁粉（数据确认）

SI：`src/main/resources/data/mio_icif/recipe/centrifuge/{uranium_quad_depleted,mox_quad_depleted}.json` 的 `tertiaryCount`。
IC2：`assets/ic2/config/thermal_centrifuge.ini:40,50`。

两种四联当前均为 6 铁粉，原版均为 7。抽查六种乏燃料：铀/钚主产物及单联、双联铁粉数量一致；铀要求 4000 热、MOX 要求 5000 热一致。MOX JSON 没写 `minheat`，但 serializer 默认值就是 5000，不能因此判为缺失。

### A12：附属核元件与旧物品 API 不是原版式动态执行（扩展能力差异）

SI 的元件权威行为来自 `ReactorInventory.PROFILES` 按本模组 ID 查表。自定义物品即使实现 `IBaseReactorComponent`，也不会像原版 `IReactorComponent.processChamber()` 那样自动参与循环；未知元件可能被拒绝或使本轮报错。

另外，燃料物品旧 `operate()`、`getActualEnergyOutput()`、`getActualHeatOutput()` 有自己的公式/时钟，与当前世界循环不完全一致。此项不等于普通内置燃料实际发电错误，但会误导附属和基于物品 API 的显示/模拟器。

### A13：热核 GUI 把每秒热量标成每 tick 功率（显示问题，静态确认）

`mio_icif_fluid_reactor_handler.java:61` 返回本轮转换量×HU/mB，周期为 20 tick；`common/menu/generator/FluidReactorMenu.java:204` 原样返回，`assets/mio_icif/lang/{zh_cn,en_us}.json` 均标为 `HU/t`。例如每周期转换 200 mB 冷却液，显示数值 4000，实际平均只有 200 HU/t。

原版 GUI 同样读 `EmitHeat`，但本地英文 `assets/ic2/lang_ic2/en_us.properties:866` 标为 `HU/s`；原版中文 `zh_cn.properties:814` 自身也写成了 `HU/t`。因此这是需要修正的单位误导，不能解读成实际产热放大了 20 倍，也不能只根据中文标签判定已对齐。建议保留原数值改标 HU/s，或除以 20 后显示 HU/t。

## 3. 基础参数对照

**下表的散热/换热数字是每个反应堆周期（20 游戏 tick）的内部热量，不可直接读成 HU/t。** 原版热核把排热乘 40，再交给每 tick 工作的液体热交换机；完整链路要另作单位换算。表中“一致”仅指参数及注明的常规行为，不覆盖 A1–A13 的组合、次序和边界差异。

参数来源：SI `ReactorInventory.java:31–49`、`common/item/reactor/mio_icif_reactors.java`；IC2 `init/BlocksItems.java:343–374`。

| 项目 | IC2 默认 | 当前 SI | 判定 |
|---|---|---|---|
| 反应周期 | 20 tick | 20 tick | 一致；首次启动相位不同 |
| 基础/满配空间 | 3×6 / 9×6 | 3×6 / 9×6 | 常规一致 |
| 基础堆温上限 | 10000 | 10000 | 一致 |
| 单/双/四铀棒无邻接输出 | 5 / 20 / 60 EU/t | 5 / 20 / 60 | 探针一致 |
| 单/双/四铀棒无邻接产热 | 4 / 24 / 96 每周期 | 4 / 24 / 96 | 探针一致 |
| 铀寿命，三种棒相同 | 20000 周期，即 400000 tick | 20000 周期 | 注册值一致；探针确认每周期扣一次 |
| MOX 寿命，三种棒相同 | 10000 周期，即 200000 tick | 10000 周期 | 注册值一致 |
| MOX 温度增益 | `1 + 4×heat/maxHeat` | 同形式但取值不对 | A2 |
| 冷却单元容量 | 10000 / 30000 / 60000 | 相同 | 一致；不自行散热 |
| 基础散热片 | 容量1000，自冷6，堆吸热0 | 相同 | 参数一致 |
| 反应堆散热片 | 容量1000，自冷5，堆吸热5 | 相同 | 参数一致 |
| 高级散热片 | 容量1000，自冷12，堆吸热0 | 相同 | 参数一致 |
| 超频散热片 | 容量1000，自冷20，堆吸热36 | 相同 | 参数一致，熔毁边界见 A4 |
| 元件散热片 | 不储热，每个相邻可冷却元件4 | 相同 | 常规行为一致；不冷却冷凝器 |
| 基础换热器 | 容量2500，邻位12，堆交换4 | 相同 | 参数一致，组合见 A1/A2/A6 |
| 反应堆换热器 | 容量5000，邻位0，堆交换72 | 相同 | 参数一致 |
| 元件换热器 | 容量5000，邻位36，堆交换0 | 相同 | 参数一致 |
| 高级换热器 | 容量10000，邻位24，堆交换8 | 相同 | 参数一致 |
| 普通/厚反射板 | 30000 / 120000 脉冲耐久 | 相同 | 参数一致；按每个燃料单元受脉冲磨损 |
| 铱反射板 | 无限耐久 | 无限耐久 | 一致 |
| 普通/热容/防爆隔板 | +1000/+2000/+500；系数.95/.99/.9 | 相同 | 参数一致，时序/爆炸计算另见上文 |
| 红石/青金石冷凝器 | 20000 / 100000，吸热不散热 | 相同 | 常规储热一致；修复和换热见 A6–A8 |
| 锂单元增殖 | 阈值10000，每脉冲 `heat/3000` | 相同 | 完成时机不同，见 A5 |
| 红石关闭 | 停燃料，但继续散热 | 相同 | 数值探针确认 |
| 热核两罐 | 各10000 mB | 各10000 mB | 一致 |
| 冷却液 / 水热交换 | 20 / 1 HU/mB | 20 / 1 | 参数一致，混流见 A3 |
| 热核排热放大 | 默认40 | 固定40 | 默认一致，未等价提供原版倍率配置 |
| 缺冷媒 / 热罐满 | 未吸收热返回堆温 | 有对应公式 | 常规探针一致 |
| 液体热交换机 | 两罐各2000；10传导器×10 HU/t | 相同 | 上轮已做真实机器循环验证 |
| 流体升级 | 每叠、每面 `50×4^min(4,n−1)` | 已在上轮修复 | 见 coolant_loop_fix.md |
| 堆芯外壳热外部抽取 | 无 HU 输出接口 | 只读热能力，抽取返回0 | 能力边界一致 |
| 电源等级 | 5 | 5 | 数值一致；底层 EU 电网是移植实现 |

## 4. 本模组扩展及原版配置差异

- 铱散热片（容量4000，自冷36）及铱超频散热片（容量4000，自冷56，堆吸热72）为 SI 扩展，不应纳入“原版已有同名元件必须等价”的范围。
- 加热元件、近衰变同位素棒在本地原版类上带 `@NotExperimental`，而当前 SI 注册了它们，属于混入其他原版 profile 的内容。加热元件原版按堆叠数量放大；当前反应堆强制每槽一个且 Profile 没有数量，因此未复刻整叠行为。
- 当前反应堆能源生成固定使用模型默认倍率；没有对齐 IC2 `balance/energyfix/generator/nuclear`、`FluidReactor/outputModifier` 和热交换倍率的完整配置能力。不能把默认参数相同理解为任意配置下相同。
- 物品部分中文注释仍留有旧的散热值、10000 容量或错误 tick 单位，实际权威参数已改变，应清理以免再次误导维护。

## 5. 覆盖边界与建议修复顺序

本次完成主运行路径和上述元件/周边的源码审计、独立数值探针，以及六种乏燃料回收数据抽查。没有逐个穷举全部合成配方，也没有实测完整燃料寿命、所有旧存档迁移、跨维度/区块卸载、多堆共壳、爆炸场景、装甲辐射总伤害、完整蒸汽/动能发电链或第三方管道。

建议先修 A1/A2/A3：它们直接改变排布的热平衡、发电效率及流体守恒。然后处理冷凝器注入/修复、乏燃料配方和结构规则；再决定是否逐条复刻原版异常边界及爆炸行为。修复时应把最小反例转成断言原版期望的回归测试，补上实际反应堆 GameTest，最后复测用户教程布局。

本报告不对“全核电已对齐”给出通过结论。
