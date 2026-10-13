# 0.1.8.4 更新说明

基于 0.1.8.3（工作树 `pr-0.1.8.3-20261013`）。构建环境为 Gradle 8.10.2、JDK 21、NeoForge 21.1.218，离线构建。

## 1. 量子胸甲推进器音效：左右平移与转向时的卡顿

| 原因 | 修复 |
|---|---|
| 本地玩家的循环音是定位声源，放在脚下 0.8 格的 tick 位置。插值后的听者在 tick 之间越过声源，平移时声源左右来回，转向时方位摆动，左右声像跳变，听起来像断续的噪音 | 本地玩家的循环音与点火／熄火音改为相对听者播放（居中，与原版鞘翅风声 `ElytraOnPlayerSoundInstance` 相同）。其他玩家仍为定位声源 |
| 推力样本出现 1–3 tick 的空档时，循环音停止 → 熄火音 → 点火音 → 新循环音，与正在播放的声音叠在一起 | 短于 8 tick 的空档只把音量降到怠速（55%）；淡出中的循环音恢复而不是新建；点火音需要 20 tick 静默，同一飞行者两次提示音至少间隔 16 tick |
| 音量与音高按目标值跳变 | 音量每 tick 趋近 15%，音高每 tick 最多变化 0.015（不颤音），淡出 6 tick |

另外修复：背包中放一件备用的高级量子胸甲时，飞行与治疗每 tick 执行两次（双倍推力与耗电）。现在只有穿着的那件运行（`stack == 穿着的物品堆`）。

## 2. 僚机（Wingman）战斗 AI

* **武器分类**：拔刀剑（SlashBlade: Resharped）、远程武器（弓、弩、三叉戟、所有投射物武器、本模组能量枪）、近战武器。
* **拔刀剑**（`SlashBladeCombat`，反射，无编译依赖，只认 Resharped 分支）：用 javap 确认 Resharped 2.1.9 的机制——连段由 `ISlashBladeState#progressCombo(LivingEntity)` 推进，输入来自实体附件 `input_state`；连段的攻击动作在 `ItemSlashBlade#inventoryTick` 中通过 `ComboState#tickAction` 对任何主手持刀的 `LivingEntity` 执行；生物不会收到物品栏 tick，所以以前僚机无法攻击。现在僚机每 tick 调用该 tick；3.5–9 格时以 FORWARD + SPRINT 输入发动拔刀突进斩并直线冲刺，近身每 5 tick 输入一次连段，每 10 秒在 7 格内释放一次 SA（`doChargeAction`，目标通过 `setTargetEntityId` 指定）。战斗结束后连段自然结束并收刀。桥接失败时按普通剑攻击。
* **远程**：在交战距离带内（弓 8–16 格、弩 9–16 格，能量枪由 `IWeaponItem#engagementRange` 决定）高于目标 3.5 格环绕飞行；按目标速度与弹速做提前量；视线被挡或主人在射线上时不开火，持续被挡则爬升并换环绕方向；爆炸武器在目标距主人 5.5 格内时不开火。
* **近战**：按预测位置接近，攻击后有 1/3 概率侧向拉开再回来（打了就走）。
* **通用**：平滑转向（每 tick 最多 20–45°）、僚机之间相互避让（2.2 格）、主人新攻击的目标立即成为焦点（2 秒内）、护卫模式拦截正在逼近主人的敌对生物（6 格）、主人生命低于 40% 时编队收紧。
* **新 API**：`IWeaponItem#fireFrom(stack, shooter, direction, mainHand)`（非玩家射手开火，由武器自身电量支付）、`engagementRange()`、`projectileSpeed()`。已实现：电磁步枪、高级电磁步枪、战术激光步枪、电浆枪、速子干扰器、纳米弓、火箭发射器。僚机发射的爆炸不破坏方块（能量弹与火箭新增 `setTerrainDamage(false)`）。

## 3. 蓝图建筑机与复制机的自动联动（UU 供料）

* 材料接口中没有某种材料时，建筑机向供料范围内（默认 16 格）拥有该模板（存储水晶或相邻模板存储器）的**空闲**复制机下单（停止状态、无进行中的付费任务）。
* 复制机用自身的 UU 物质与能量按通常价格复制，订单按 LOOP 模式运行，完成后自动停止；订单覆盖蓝图中该材料的剩余需求（最多 64 个）。无人机从复制机输出槽取件；复制机来源的剩余货物送往最近的材料接口。
* 订单单独保存（`scex_build_order`），不改动复制机严格校验的付费记录结构。玩家正在使用的复制机不接单。模板被移除时订单取消。
* 建筑机界面新增 “UU 复制” 行，显示正在复制的材料与数量。
* 通用配置 `BlueprintBuilder.replicatorSupply`（默认开）、`replicatorRange`（4–64）。

## 4. 各发电机发电量配置

通用配置新节 `GeneratorOutput`：28 种 EU 发电机各一个倍率（1.0 = 原始，0 = 停止，最高 100）。燃料燃烧速度不变，所以倍率也是每单位燃料的 EU。

| 实现 | 说明 |
|---|---|
| 作用位置 | 各发电机决定输出的位置（喂给能量计划器的速率），燃料记账与存储空间检查保持一致：固定速率乘倍率并跨 tick 保留小数；燃料计划器（`ConsumableGeneration`／`FluidFuelGeneration`／`StoredFuelGeneration`）的单位能量与速率乘倍率；电网控制路径的精确量（`EnergyAmount`）乘倍率 |
| 核心钩子 | `AbstractGeneratorBlockEntity.OUTPUT_SCALE`（默认 1），供使用基类发电路径的发电机（火力发电机等）；附属模组不受影响 |
| 斯特林发电机 | HU→EU 比例统一为 0.5 × 倍率，所有房间／预算换算一致；非 1 倍率时允许最后一个 EU 小数的舍入 |
| 核反应堆 | 只缩放 EU（不影响热量） |
| 倍率 0 | 跳过发电与燃料消耗（计划器不接受 0 的单位能量） |

动能（KU）与热能（HU）源本身不产 EU，未列出；它们的 EU 由动能／涡轮动能／斯特林发电机产出，可在这些条目中调整。

## 5. 验证

* `gradle build coreJar coreSourcesJar apiJar apiSourcesJar addonExampleJar checkCoreDistribution --offline` 通过（含 `checkCoreArchitecture`）。
* 离线检查 `verify-si184/check_184.py`：28 个配置 ID 均为已注册方块实体、三语键齐全、7 种武器实现 `fireFrom` —— 0 个问题；0.1.8.3 的资源检查仍为 0 个问题。
* 新增 GameTests `Round184GameTests`（4 项：配置覆盖、能量步枪为非玩家开火、拔刀剑桥接对普通剑为否、无模板的复制机拒绝订单）。本机无法运行 GameTest，未运行。
* **未在游戏中验证**：推进器音效、僚机全部战斗行为（尤其 SlashBlade: Resharped 实机连段与 SA）、建筑机与复制机联动、各发电机倍率的实际输出、多人。
