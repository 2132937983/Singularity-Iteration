# EU 电表与 IC2 对齐

2026-09-29。修改对象为重构后的 `common/item/tools/mio_icif_eu_meter`、对应菜单和界面，统计及同步的通用部分保留在 core。

## 原版口径

依据仓库 `source/IC2Code 1.12.2/ic2/core/item/tool/ContainerMeter.java`、`GuiToolMeter.java`、`energy/grid/EnergyNetGlobal.java` 与 `energy/leg/EnergyCalculatorLeg.java`：

- 输入、输出分别是被测节点实际接收、送出的能量，单位 EU/t。
- 增益是输入减输出，可以为负值；电缆的输入输出通常相同，增益不代表电缆正在输出多少能量。
- 平均、最小、最大统计当前测量期间的逐 tick 数值；点击重置或切换模式开始新一轮，采样秒数为 tick 数除以 20。
- 原版 `EnergyNetGlobal` 使用 `EnergyCalculatorLeg`，其 `getNodeStats` 将最大能量包转换为电压等级。电表电压模式沿用这一口径：32 EU 包显示 1，128 EU 显示 2，512 EU 显示 3；GUI 的 V 单位是原版沿用的标签，并非现实电压单位。core `NodeStats.voltage` 仍表示能量包大小，转换仅发生在电表。
- 原版 `GuiToolMeter` 使用六位有效数字的 SI 格式，小于 1 的读数也可显示 m、µ 等前缀。

本地 2.8.221 的 `ContainerMeter` 反编译结果部分挤在单行、缺失统计分支，累计统计分支同时参照仓库 `source/IC2Code_Not_for_us_Version` 的清晰反编译版本核对，不照搬明显不完整的反编译文本。

## 修正

1. 原实现在 `broadcastChanges()` 每次调用时采样。玩家 tick、拾取物品和其他容器同步都会调用它，不能把调用次数视作电网 tick 数。改为 `ServerTickEvent.Post` 结算后一次采样，普通界面同步只发送已有读数。
2. 电网在拓扑变动后的补算可能跨本 tick 的 START/END 阶段。电表统一读取 END 完成后的数据，避免读取中间状态。
3. 旧电表遇到没有转移记录时直接返回，导致空闲时间不进入平均值、计时停止、界面保留上次读数。新增 `getCurrentTickNodeStats` 查询：有效的空闲节点返回 0，缺失/卸载/失效节点返回不可用；不会重放上一个 tick 的包。原来的稀疏 `getNodeStats` API 保持不变。
4. 同步前先更新统计值，切换模式和重置立即同步；被测方块拆除、替换或卸载后关闭电表，不继续显示旧对象数据。
5. 新 `MeterReadings` 将 double 分成四个 16 位字、计数分成两个 16 位字。负数、小数、大数和超过 65535 的计数均可通过原版容器数据包无损往返。
6. 界面由固定两位小数改成六位有效数字及 SI 前缀，避免微小流量直接四舍五入成零。电压模式按 IC2 等级转换，并增加物品提示说明。

NeoForge 21.1.218 正常连接会使用 `AdvancedContainerSetDataPayload` 传完整 32 位整数，因此“旧实现将 double 拆成两个 int”不是所有连接必然出错。本次 16 位编码是同时兼容原版回退通道的修正，不能未经实测就断言这是用户所见读数偏小的唯一原因。

没有提高发电量、改变输电限额或给读数乘补偿倍率。额定功率、实际供给、线路损耗及负载需求仍由现有电网决定。尚未获得用户所测方块、模式、实际及预期数字，不能声称已复现该特定现场的偏小幅度。

## 验证

`EnergyMeterGameTests` 包含真实 `ClientboundContainerSetDataPacket` 编解码，以及真实注册电表菜单、电网、服务端 tick 和客户端读数镜像。无客户端握手的 GameTest 模拟玩家通过菜单注册工厂创建菜单，不模拟打开界面的网络握手。

覆盖小数、负数、大数、70001 次计数、32 EU/t 持续输入、四种测量模式、额外 20 次界面同步不增加采样、立即重置、空载归零、交替供电的平均/最小/最大值，以及拆除节点后关闭界面。

实际结果：全部 32 项 GameTest 通过（含 2 项新增电表测试），持续输入 32 EU/t、交替输入平均 16 EU/t，逐 tick 最小 0 / 最大 32。测试日志为 `build/energy-meter/gametest-second.log`。

`gradlew build --offline --console=plain -q` 通过，退出码 0；包含 core 架构和分发边界检查。成品 `build/libs/mio_icif-0.1.7.14.jar` 已核对包含电表修正及中英文提示，不包含测试模组代码。构建日志：`build/energy-meter/build.log`。客户端画面未人工验看。
