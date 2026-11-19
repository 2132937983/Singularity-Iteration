# 热核冷却液循环修复（2026-09-28）

> 2026-09-29：用户提供九台换热机的实际布局后，进一步复现并修复了热能力适配失效造成的半速换热，见 [896 HU/t 用户布局回流修复](user_reactor_coolant_loop_fix.md)。本文上轮测试使用直接抽热来模拟负载，没有覆盖蒸汽发生器查询热能力的路径；流体分配修复不能单独解决后者。

## 原因与复现

`FluidAutomationComponent` 原先给流体升级设置 `数量 × 1000 mB/t` 的共享额度，按固定方向顺序交付。热核每 20 tick 批量产生热冷却液，第一台热交换机可一次收走整批；后续机器没有原料，实际换热能力低于安装的总能力。多加冷却液会使先收到流体的 2000 mB 输入罐逐渐填满，表现为热流体积压。

已用旧实现复现：循环总液量 400 mB，反应堆每 20 tick 将 200 mB 冷液变成热液，两台 100 HU/t 热交换机各应处理 5 mB/t。旧实现到第 60 tick 开始缺液，首个相邻接收方在额度测试中一次得到 1000 mB，后续接收方得不到流体。

## IC2 依据与修改

依据工作区 `source/IC2Code 1.12.2`：

- `ic2/core/item/upgrade/ItemUpgradeModule.java` 的 `onTick`：每一叠流体升级的额度为 `50 × 4^min(4, 堆叠数量−1)`，对每个相邻流体处理器分别调用传输。
- `ic2/core/block/invslot/InvSlotUpgrade.java` 的 `tickNoMark`：逐升级槽执行，分别保留该槽的数量及方向。
- `ic2/core/block/machine/tileentity/TileEntityLiquidHeatExchanger.java`：热冷却液 20 HU/mB，10 个热传导器的上限为 100 HU/t，即 5 mB/t。

修复通用流体自动化组件的弹出和抽入：每面额度独立；每叠数量 1/2/3/4/≥5 对应 50/200/800/3200/12800 mB/t。升级统计保留各槽的方向和额度，避免北向升级给南向额外加速，也避免把两槽各 1 个误算成一叠 2 个。

`IMachineUpgradeStats` 新增默认额度方法，原有附属实现可继续编译。生产改动仅在 core 的流体升级统计和自动化路径；反应堆产热、换热比例、储罐容量、NBT 键和转移缓冲协议不变。所有使用该组件的机器采用修正后的 IC2 速率，第三方管道自己的分配规则不在此次修改范围内。

## 回归覆盖

`src/addonExample/java/example/sicore/CoolantLoopGameTests.java` 包含：

- 六面分配，每个邻居分别得到 50 mB。
- 堆叠倍率、不同槽位方向及流体抽入行为。
- 6000 步闭环模型：使用实际自动化组件和反应堆换算，每步检查总液量、每轮检查缺液，检查两台换热量一致。
- 两台实际液体热交换机运行 600 游戏 tick：真实流体能力、真实机器 tick、每台 10 个热传导器，模拟持续满负荷用热方，检查换热速度和冷却液守恒。

实际机器测试用反应堆公式及双罐模拟上游批量产液，不含完整核燃料、多方块、汽轮机或第三方管道布局；用户教程的具体摆法仍需实机核对。

验证结果：`gradlew.bat build` 通过（包含 core 架构及分发检查）；`gradlew.bat runAddonGameTestServer --offline --console=plain -q` 的全部 18 项测试通过，含上述 4 项新增回归。产物为 `build/libs/mio_icif-0.1.7.14.jar`。测试运行目录为 `build/addon-gametest`，未操作用户游戏存档。
