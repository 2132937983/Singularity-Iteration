# 电网引擎与 IC2 的差异审计及修复方案

日期：2026-09-29。本文保存**修复前的审计基线**。当前实施结果见 [电网修复进度](energy_engine_fix_progress.md)。

## 1. 结论与范围

已在当前项目的真实 GameTest 服务器中复现 HV→EV、EV→IV 升压断续：**四个电源同时供电，接收端仍只收到一路电源的吞吐，升压后的平均输出仅为目标的 25%。** 问题位于电网分配器，不是电表显示，也不是变压器容量太小。

同时确认了降压多包分配错误、实际仅传输 1 EU 仍误触发过压爆炸、旧变压器 NBT 小数丢失。另发现路由计划反复失效、拓扑队列故障停网、附属多包接口未接入等问题或风险。

用户反馈的范围：HV→EV 与 EV→IV；储能充足；下游机器缓存实际增长不连续。尚未取得用户完整电路、输入设备数量和逐 tick 记录，因此本文证明了**足以产生该现象的引擎缺陷**，不声称用户存档中的每一次停顿都由同一个原因造成。

本次检查以本地 `source/IC2Code 1.12.2` 为基准。其中相关反编译文件尾部标明 `industrialcraft-2-2.8.221-ex112.jar`。当前项目部分注释引用 2.8.222 的历史测量；两者不可混作同一份证据。本次没有启动原版 1.12.2 服务器，表中的 IC2 预期来自所附源码；当前项目实测来自 NeoForge GameTest。

## 2. 当前实际执行链

```text
common 注册机器、线缆、电气参数
  → IndependentTransformerFactory 替换变压器实体、停用旧 ticker
  → PlatformTopology 收集放置、拆除、区块生命周期变化
  → IndependentSiEnergy 在世界 tick 边界发布拓扑、取得存储快照
  → ConductorRegistry / NativeRoutePlan 搜路和选择源、接收端窗口
  → FractionalDistributor → 整数快速路径 DomainDistributor
  → TransformerBatch / PacketDistributor 分配能量包
  → NetworkCell + CustomEUEnergyStorage + FeLedger 校验和提交
  → 电缆熔毁、触电、机器爆炸、NodeStats 统计
```

默认启用 `scex.independent.energy=true`。仅修改 `common/blockentity/transformer/mio_icif_transformer.java` 的旧 ticker，不会修好默认引擎。应优先修改 core 分配、调度和平台适配，common 继续承担注册与具体内容。

原版主要路径：`energy/grid/EnergyNetGlobal` 选择 `energy/leg/EnergyCalculatorLeg`，由 `EnergyNetLocal`、`GridUpdater` 调度；变压器用 `block/comp/Energy` 提供收发接口。

## 3. 升压实测

诊断代码：[EnergyEngineAuditGameTests.java](../src/addonExample/java/example/sicore/EnergyEngineAuditGameTests.java)。该 sourceSet 依赖独立 core JAR，通过注册 ID 放置 common 方块，没有引入 common 实现依赖。

条件：

- 两档变压器固定升压，高压面朝东；输入放在低压面。
- HV 输入使用 MFE，EV 输入使用 MFSU；每个电源预装 1,000,000 EU，不持续补电。
- 一路与四路供电分别测试。输出接空 EESU，EESU 输出朝外，足够容纳本轮全部输出。
- 直接贴面连接，无电缆损耗、无 FE、无红石切换；经过预热后连续记录 40 tick。
- 每 tick 验证电源总存量＋变压器存量＋负载存量不变；采集后清除诊断节点，避免影响后续测试。

| 场景 | 输入具备的吞吐 | 当前下游平均 | 40 tick 收电 | 无增长 tick | IC2 源码预期（稳定后、无损） |
|---|---:|---:|---:|---:|---|
| 1×MFE → HV→EV | 512 EU/t | 512 EU/t | 20,480 EU | 30 | 每四 tick 左右一个 2048 EU 包，合理 |
| 4×MFE → HV→EV | 2048 EU/t | **512 EU/t** | **20,480 EU** | **30** | 每 tick 一个 2048 EU 包 |
| 1×MFSU → EV→IV | 2048 EU/t | 2048 EU/t | 81,920 EU | 30 | 每四 tick 左右一个 8192 EU 包，合理 |
| 4×MFSU → EV→IV | 8192 EU/t | **2048 EU/t** | **81,920 EU** | **30** | 每 tick 一个 8192 EU 包 |

实际增量序列循环为 `0,0,0,2048` 或 `0,0,0,8192`，四路场景仅有相位差。统计窗口中变压器首尾存量相同，电源扣款等于负载到账，因此没有用初始存量变化冒充吞吐。

**储电量与输出功率须分开。** 原版变压器本就需要完整高压包才输出；只有一台 MFE/MFSU 时，库存再多也不能让它每 tick 交付四倍能量。修复目标是恢复多路、多包输入能力，同时保留供电不足时的正常攒包行为。

## 4. 差异与缺陷清单

证据等级：**实测确认**＝当前运行结果＋源码；**源码确认**＝明确分支或接口差异，尚未覆盖全部世界场景；**风险待测**＝已有可疑路径，需要专项压力或故障注入验证。

### E01 / P0：接收端被错误限制为每轮一包【实测确认】

位置：`core/runtime/energy/engine/DomainDistributor.java` 的 `served`、`accepting`；`FractionalDistributor.java` 的对应逻辑。

两个分配器收到任何正数电量后，都将接收端标记为本轮完成；此状态跨所有导体域共享。源码注释声称这是 IC2 规则，实际与本地 IC2 不符。

IC2 `EnergyCalculatorLeg.emit` 只在单次 `amount >= sinkDemand` 或发生拒收时移除接收端。有充足需求时，可以继续接收其他电源以及同一电源的后续包。项目当前即使四路供电，也只放行其中一路。

纯分配器复现同时覆盖整数和小数分支：四个 512 EU 源分别只到账 512，预期 2048；四个 2048 EU 源分别只到账 2048，预期 8192。小数测试用 `capacity - 0.5` 强制走精确算法，避免整数快速路径掩盖另一份错误。

**方案：**取消全局“一次正数到账即完成”。用接收端剩余空间、拒收状态和源的剩余包预算判断能否继续；整数和小数算法必须一起修改。变压器既是源又是接收端，仍需共享同一存储身份，并保留“本轮新收到的电量不凭空增加本轮已报价输出”的约束。

### E02 / P0：降压四包变成向四个接收端各发一包【实测确认】

位置：`TransformerBatch.allocateQuoted`；小数分配器内的多包分支。

`TransformerBatch` 只遍历一次接收端列表。只有一个负载时，即使缓冲充足、负载空余远大于四包，也只发一包。数值探针实测：`4×512` 预算只发 512；`4×2048` 预算只发 2048。

原版 `distributeMultiple` 会重复执行单包路由，同一个仍有需求的接收端能收到多包。本项目 `TransformerAccounting.advance` 自身也允许这种情况，说明简化模型和生产分配器已有语义分裂。

**方案：**建立明确的“每源包数 → 每包接收端遍历”过程，逐包扣除路损、记录轨迹。不能用放大单包额度替代，否则会把低压多包错误识别为高压一包。部分包跨多个接收端时，沿用剩余报价，防止重复扣款或超过包数预算。

### E03 / P0：额定电压被当成实际传输包进行破坏判定【实测确认】

位置：`IndependentSiEnergy.settlePlan` 的 `sourcePacket`、`deliveryVoltage`、`fusePlan`、`receiverPlan`；`portPacket` 的相关策略说明。

当前用源的额定包大小判断熔线与炸机，并用 `max(actualDebit, nominalPacket)` 计算绝缘、触电效果。原版由本次实际交付和路损算出 `effectiveAmount`，维护路径的最大实际包，再执行效果。

实测：MFSU（额定 2048）直接给仅缺 1 EU 的 MFE 供电，电源确实只扣了 **1 EU**，但 MFE 被摧毁；本地原版源码在这个无损场景不会触发过压。

**方案：**额定等级用于限制报价和生成包；实际每包扣款、到账和路损用于 IC2 效果判定。分别保存这些字段，禁止用总 EU/t 或名义等级替代实际包。原生 EU 和 FE 出口的效果路径一起检查。保持现有过压配置开关，修复不能靠关闭爆炸达成。

### E04 / P1：旧变压器 NBT 小数被截断【实测确认】

位置：`IndependentTransformerBlockEntity.loadAdditional`。

旧实现保存 `buffer_long` 和 double 类型的 `buffer_fraction`（范围 `[0,1)`），新实现却执行 `(long) tag.getDouble("buffer_fraction")`，再把它当成 52 位定点小数。测试输入 `100 + 0.75`，加载后实际为 **100.0 EU**。

**方案：**分别解码旧 double 小数和新 `scex_energy_fraction` 定点小数，使用 `EnergyAmount` 组合；保留模式数字和方块 ID。覆盖整数、小数、重复保存加载、非法小数、大整数边界。这会丢不足 1 EU 的余额，但不足以解释已复现的持续四分之一吞吐。

### E05 / P1：路由计划的跨 tick 恢复与缓存被代号更新破坏【源码确认；大网饥饿待测】

位置：`IndependentSiEnergy.settle`、`NativeRoutePlan`、`RoutingView`。

每次结算重建 ports 时无条件 `portsGeneration++`；紧接着只要代号不同就重建计划。`NativeRoutePlan.advance` 因预算不足返回时保留了 `originIndex`、候选和路径，但下次结算代号更新又把这些进度丢弃。完整单窗口计划也无法按注释保持跨 tick 热缓存。

导体最短路本身另有缓存，所以不能断言所有大网必定卡死；但计划层重复分配、重复候选扫描是确定的，超过单轮建表预算时能否完成需要专项测试。

**方案：**拆分拓扑版本与能量报价版本。只有节点身份、接触点、方向、连通性变化才失效路由；余额变化仅更新报价。保留有界搜索游标，提交前重新验证拓扑、实体身份、存储版本。不能直接删除版本校验，那会重新引入拆除、爆炸后使用旧节点的问题。

### E06 / P1：拓扑积压影响整个维度，队列溢出会停掉引擎【源码确认；已有历史故障日志】

位置：`PlatformTopology.enqueue / ready / pendingFor`；`IndependentSiEnergy.afterLevel / fail`。

`ready(level)` 以整维度待处理事件为门槛。电气修改发布、爆炸暂停也以 `WorldGrid` 为范围，可能影响远处不相连的电路。队列达到 65,536 后写入 failure；引擎随后关闭拓扑，没有局部重建恢复流程。

已有测试日志 `build/fluid-pipes/final.log:1630` 记录 `Topology event queue capacity exceeded`。这是大量测试结构更新造成的历史故障，不能直接当作用户这次小电路断续的证据。

原版 `EnergyNetLocal.onTickStart/onTickEnd` 也有拓扑更新暂停与随后 START/END 两次计算，因此**“看到双阶段或一次更新暂停”本身不算偏差**。需要区分原版时序与本项目额外的队列阻塞、永久失败。

**方案：**短期增加暂停原因和恢复状态；事件合并达到阈值时升级为受影响区块/连通分量重扫。重建期间撤销受影响路由，继续已验证独立网络；无法证明隔离时仍安全暂停。队列限额继续保留，禁止静默丢事件或无界扩容。局部隔离属于明确的工程优化，不宣称逐事件复制原版调度。

### E07 / P1：源/接收端窗口会影响吞吐与公平性【源码确认；规模边界待测】

每窗口最多 32 个源、128 个接收端，路径成本还会缩小接收端窗口。源访问一遍后即进入 `visited`，即使本窗口没有需求且其他窗口还有需求，也不再处理该源；窗口还会在不同报价时机重新看到共享变压器余额。

**方案：**把源的本次结算预算与窗口解耦：窗口推进时携带未消耗预算，以身份记录已用包数，而非简单“访问过就结束”。分别压测 31/32/33 个源、127/128/129 个负载，以及前一窗口已满、后一窗口为空。输入不足和预算不足必须有不同诊断原因。

### E08 / P1：附属的多包接口没有接入默认执行路径【源码确认】

`core/api/energy/grid/IEnergySource` 暴露 `getPacketCount()`，但默认引擎普通 Port 固定构造为 `packets=1`，只有独立变压器配置能提供四包。`DomainDistributor.Source` 还硬编码 `packetCount<=4`，并禁止残余包源同时是多包源。

原版 `IMultiEnergySource` 是通用供电接口，不能把“变压器最多四包”扩大成所有附属电源的契约。当前注册兼容入口也不等于任何第三方 IEnergyTile 都取得原生事务存储所有权。

**方案：**在 core 增加受支持的输出策略：额定包、可供额度、每次结算包数、是否允许残包；由拥有存储的机器组件声明并冻结为快照。遗留回调走边界适配，不在原子写阶段调用外部代码。通用包数采用有界工作预算，不能简单无限循环或把四包限制全部删掉。给附属示例加多包源和拒收接收端验收。

### E09 / P2：原版冻结需求与当前硬容量上限不同【源码确认；明确保留的差异】

IC2 `emit` 内没有逐包减少 `MutableDouble sinkDemand`；`Energy.injectEnergy` 又直接相加，某些多包场景可能越过最初容量。当前分配器、事务层更强调余额和容量约束。

**建议默认继续限制实际可用空间**，并记录为与原版的已知边界差异。本次恢复正常多包吞吐无需复制溢容量行为。不能只删除 `served`、继续使用冻结需求而不检查总到账。若未来要求严格复刻这项边界，再制定单独的存储策略和兼容验收。

### E10 / P2：电表数据源仍有路损统计口径差异【源码确认】

原版 `NodeStats` 的源和导体流量累计 `path.energySupplied`（净到账）；当前源和沿途导体累计 `sourceDebit`（包含路损），负载才累计净到账。当前电压统计则用实际发送量，和 E03 的破坏判定又采用了不同口径。

**方案：**内部保留 gross debit、net credit、loss、max packet 四项诊断；对外 IC2 兼容统计明确选择净流量。避免修改账本来迁就显示。最近电表采样和闲置零值修复应保留，但不能据此宣称电网所有统计都已对齐。

### E11 / P2：线缆损耗已接近原版，触电及绝缘机制仍有差异【源码确认；实体伤害待测】

锡/铜/金/铁/玻璃的基础损耗 `0.2/0.2/0.4/0.8/0.025` 和击穿阈值 `容量+1` 可在原版 `CableType / TileEntityCable` 对照。应保留已有这些参数。

但原版裸锡的绝缘吸收为 8 EU，裸铜/金/铁为 32 EU；绝缘金支持两层、铁支持三层。本项目 `CableTier` 默认裸线阈值分别为 32/128/256/1024，`AbstractWireBlockEntity` 只用布尔 `insulated` 选取参数，不能完整表达原版层数。

`mio_icif_wire.tick` 在独立引擎提前返回前仍执行旧触电检查，而引擎还有按包触电路径；旧检查会保持“曾通电”状态 20 tick，并包含水中传播。需要实测是否重复伤害、停电后是否仍伤人，并明确哪些水中行为是项目扩展。引擎在提交前调用 `conductor.onEnergyPass()` 也应移到成功提交后的效果阶段，避免被拒绝的交易仍把线缆点亮。

另外，原版损耗取整受 `roundEnetLoss` 配置控制；当前按毫 EU 建图、按整 EU 路损扣款，没有找到对应可切换配置。取整、染色隔离、多层绝缘、探测/分流线完整行为列入后续专项，不把仅有基础损耗一致写成“线缆全部对齐”。

## 5. 建议实施顺序

### 第一阶段：恢复实际吞吐（E01、E02）

1. 先把本次四路升压和单负载降压结果转成明确的兼容验收测试。
2. 统一整数、小数分配的包生命周期和接收端退出条件；保留定点电量和原子事务。
3. 维护源预算、共享缓冲、每包损耗和逐包轨迹；校验源扣款＝接收端到账＋路损。
4. 两档四路无损供电稳定后，应分别达到 2048、8192 EU/t；单路对照继续保持 512、2048 EU/t 的攒包输出。
5. 检查源正好一包、略少一包、负载只缺 1 EU/0.5 EU、输入输出同时发生、同一接收端跨导体域等边界。

不调整发电倍率、不放大缓存、不用 FE 绕过原生分配。首先修好当前玩家能直接遇到的错误，再优化性能。

### 第二阶段：修正破坏判定和存档迁移（E03、E04）

以实际包字段统一过压、熔线、绝缘、触电和仪表数据来源；把世界副作用放在成功提交之后。覆盖额定高压但实际小包、真正满包过压、损耗临界点、导体先熔毁、接收端拒收等情况。

单独修旧 NBT 的小数编码转换并验证二次读写。保存 ID、模式数字、方向和已有电量。原版默认红石自动模式与当前默认降压不同，若调整默认值只作用于新放置实例，不能重解释旧模式数字。

### 第三阶段：消除规模和拓扑引起的额外断续（E05、E06、E07）

先增加诊断，再改缓存和调度：

- 每维度、每连通分量记录拓扑版本、报价版本、路由命中/重建、搜索与路径访问预算。
- 给未输电原因分类：源不足整包、负载已满、方向不通、等待拓扑、路由预算耗尽、事务快照过期、FE 拒收、引擎故障。
- 路由计划保留进度，余额变化不重建拓扑；窗口继续时保留源预算，避免重复供电和无需求窗口占用整 tick。
- 队列满时合并重建；故障隔离到能证明边界的区域，提供可观察的恢复过程。
- 保持 Minecraft 世界访问与事务提交在服务端线程。若后续引入后台搜索，只允许不可变图快照，并在提交时重新验证。

现有源/接收端快照、定点算术、有界搜索、区块访问检查、FE 共用输出账本都有价值，优先局部修复，不建议本轮再替换整套引擎。

### 第四阶段：完成 API 和参数口径（E08—E11）

发布 core 的多包输出策略及附属范例；明确原生事务与外部回调接口边界。分别标注 IC2 一致行为、项目扩展、仍待实现项。增加电缆绝缘等级和统计口径测试，最后再做分配公平性、查询缓存与对象分配优化。

## 6. 验收矩阵

| 类别 | 必须覆盖 | 通过标准 |
|---|---|---|
| 升压 | 两档 × 单路/四路；直连及有线 | 稳定无损四路逐 tick 满高压包；单路平均不被放大 |
| 降压 | 一个与多个负载；缓冲一到四包 | 单负载可收四包；每个低压包独立计损和判压 |
| 守恒 | 整数、小数、共享变压器、多导体域 | 精确扣款＝到账＋损耗；余额不负、不超默认容量 |
| 过压 | 额定高压实际 1 EU；真正高压；临界电缆 | 小额无损场景不误炸；满包危险仍生效 |
| 生命周期 | 旋转、换模式、红石、卸载重载、拆线、爆炸 | 不使用失效报价；不重复扣款；重新连通后恢复 |
| 规模 | 31/32/33 源；127/128/129 负载；多独立网络 | 无窗口导致的长期饥饿，充足工作预算内稳定供电 |
| 预算 | 缩小测试预算，强制多 tick 构建 | 进度单调推进，静态网络能最终完成 |
| 故障 | 事件洪峰、队列上限、失效快照、异常附属 | 不增能；有原因与恢复途径；隔离有效网络 |
| FE | FE 与 EU 同 tick 消耗；1 FE 小数；拒收和重入 | 4 FE/EU 转换守恒，共享额度不能重复花费 |
| 存档 | 旧 100.75；新格式；大整数；连续二次加载 | 保留精确合法余额，异常值有明确处理 |
| 附属 | 多包发电机、方向策略、受控拒收、组件与继承两类接入 | 声明的策略进入实际引擎；独立 core JAR 可编译 |
| 性能 | 固定供需的静态大网与局部反复编辑 | 缓存有实际命中；进度可观察；报告 p50/p95/p99 tick 用时与分配量 |

性能优化先保存基线，再比较相同世界、相同供需与随机种子。不承诺未经测量的加速百分比。拓扑双阶段补算与 FE 每 tick 限额需单独测试，不能把原版合法的 START/END 行为误删。

## 7. 审计轮次的验证与限制（修复前）

最终执行：

```powershell
.\gradlew.bat runAddonGameTestServer checkCoreArchitecture checkCoreDistribution --offline --console=plain -q
```

结果：**34 项 GameTest 全通过，core 架构和分发边界检查通过。** 日志：`build/energy-engine-audit/final.log`，检索 `ENERGY_AUDIT` 可见每 tick 序列、整数/小数分配、降压、旧 NBT、1 EU 误炸记录。

新增两项属于审计探针：断言场景有效和守恒，输出“当前结果 vs IC2 源码预期”；**未把当前错误的 25% 吞吐写成必须保持的测试断言，也未宣称这些探针通过就代表已对齐。** 后续修复阶段应增加严格的目标行为断言。

中间运行 `verified.log` 曾有电表测试平均 14.4 而非 16 的失败；诊断电路当时在场景结束后继续留在同一测试世界。增加清理及等待拓扑发布后，最终全套通过。此记录保留，不把一次复测成功扩展成“规模与时序问题已经排除”；E05—E07 仍需专门复现。

审计轮次没有更改生产 Java、能源参数或默认配置，仅新增诊断 sourceSet 文件和此方案。后续修复另记于进度文档。没有启动客户端验证画面，也没有跑完整 IC2 1.12.2 游戏差分或大规模性能基准。

## 8. 源码定位索引

以下路径均从仓库根目录起，方法名为主要定位依据：

| 当前实现 | 检查位置 |
|---|---|
| `src/main/java/com/miophas/singularity_iteration/core/runtime/energy/engine/DomainDistributor.java` | `allocate`、`served`、`SharedStorage` |
| 同目录 `FractionalDistributor.java` | `allocateTraced`、`allocateExact` |
| 同目录 `TransformerBatch.java` / `TransformerAccounting.java` | `allocateQuoted` / `advance`、`Configuration` |
| `src/main/java/com/miophas/singularity_iteration/core/runtime/energy/IndependentSiEnergy.java` | `settle`、`settlePlan`、`ports`、`refreshQuotes`、`NativeRoutePlan`、`fail` |
| `src/main/java/com/miophas/singularity_iteration/core/platform/neoforge/energy/PlatformTopology.java` | `enqueue`、`ready`、`pendingFor` |
| 同目录 `IndependentTransformerBlockEntity.java` / `IndependentTransformerFactory.java` | `loadAdditional`、`snapshot` / 创建和 ticker 替换 |
| `src/main/java/com/miophas/singularity_iteration/core/runtime/energy/FeLedger.java` | `quoteNativeOutput`、`commitNativeOutput`、`push` |
| `src/main/java/com/miophas/singularity_iteration/core/api/energy/grid/IEnergySource.java` | `getPacketCount` |
| `src/main/java/com/miophas/singularity_iteration/core/api/energy/storage/CableTier.java` | 基础电压、损耗、绝缘阈值 |
| `src/main/java/com/miophas/singularity_iteration/core/prefab/blockentity/AbstractWireBlockEntity.java` | 参数选择、`checkElectricDamage` |
| `src/main/java/com/miophas/singularity_iteration/common/blockentity/mio_icif_wire.java` | `tick` 的触电调用顺序 |

| IC2 基准（均位于 `source/IC2Code 1.12.2/ic2/core/`） | 对照内容 |
|---|---|
| `energy/grid/EnergyNetGlobal.java` | 默认选择 `EnergyCalculatorLeg` |
| `energy/leg/EnergyCalculatorLeg.java` | `runCalculation`、`distributeMultiple`、`emit`、`applyCableEffects`、`getNodeStats` |
| `energy/grid/EnergyNetLocal.java` / `GridUpdater.java` | `onTickStart/onTickEnd`、`startTransferCalc` |
| `block/wiring/TileEntityTransformer.java` | 8 倍低压包容量，升压 1 包、降压 4 包，方向和模式 |
| `block/comp/Energy.java` | `fullEnergy`、`getPacketCount`、需求和注入 |
| `block/wiring/CableType.java` / `TileEntityCable.java` | 损耗、容量、多层绝缘、染色连通性 |
| `energy/grid/EnergyNetSettings.java` | `roundLossDown` 配置 |
