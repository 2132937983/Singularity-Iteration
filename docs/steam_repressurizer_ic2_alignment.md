# 蒸汽再加压机 IC2 对齐记录

核对日期：2026-10-06。基于当前 0.1.7.35.1 工作区；本次不调整项目版本号。

## 对照依据

- [IC2 TileEntitySteamRepressurizer](../参考代码/IC2Code%201.12.2/ic2/core/block/machine/tileentity/TileEntitySteamRepressurizer.java)：两罐、按需取热、批次转换、输出空间判定及热量保存。
- [IC2 GUI 定义](../参考代码/IC2Code%201.12.2/assets/ic2/guidef/steam_repressurizer.xml)：176×166、输入罐 `(15,19)`、输出罐 `(123,19)`、均为 38×47；玩家物品栏背景从 `(7,83)` 起。
- [IC2 默认配置](../参考代码/IC2Code%201.12.2/assets/ic2/config/general.ini)：`balance/steamRepressurizer/steamPerSteam=16`、`steamPerSuperSteam=32`。
- [当前方块实体](../src/main/java/com/miophas/singularity_iteration/common/blockentity/producer/mio_icif_steam_repressurizer.java)、[菜单](../src/main/java/com/miophas/singularity_iteration/common/menu/producer/SteamRepressurizerMenu.java)、[界面](../src/main/java/com/miophas/singularity_iteration/common/client/screen/mio_icif_gui_steam_repressurizer.java)。

## 机制与修正

| 项目 | 原实现问题 | 本次实现 |
| --- | --- | --- |
| 输入 | 蒸汽/过热蒸汽 | 保留，仅接受这两种流体 |
| 输入容量 | 10,000 mB | 10,000 mB |
| 输出容量 | 16,000 mB | 10,000 mB |
| 普通蒸汽 | 每 tick 仅从热存储消费 1 HU，形成处理瓶颈 | 每批 10 mB + 1 HU → 16 mB 普通蒸汽，同 tick 可处理多批 |
| 过热蒸汽 | 产物仍为过热蒸汽，普通/过热输入切换会因产物类型冲突停机 | 每批 10 mB + 1 HU → 32 mB 普通蒸汽 |
| 热源访问 | 排除机器正面，预先积累大量热量 | 按输入需求从六面查询相邻热源，查询的是热源面对机器的那一面 |
| 空机取热 | 没有蒸汽也持续取热 | 输入不足 10 mB 时不抽热 |
| 热量消费 | 两层缓冲、每 tick 只转入 1 HU | 单一持久化 HU 缓冲；消费实际完成的批次数 |
| 输出满/剩余空间不足 | 非完整批次容量判定、错误工作状态 | 无法放入完整产物批次时不消费输入和 HU；保留不足 10 mB 的余量 |
| 流体传输 | 内建前后面每次自动推送 1,000 mB | 六面输入只进、输出只出；外部管道/泵抽走产物，机器不自行推送 |
| 单元/升级槽 | 单元处理为空实现，GUI 仍摆出槽位 | 原版没有这些槽位，移除 GUI 槽位及升级效果，不允许插入新物品 |
| 配置 | 比例写死 | COMMON 配置 `[SteamRepressurizer]`：`steamPerSteam=16`、`steamPerSuperSteam=32`，两项范围均为 1～10,000 |

取热细节按 IC2 保留：`aim = inputAmount / 10`；仅当当前 HU 小于 aim 时取热，但一次申请的是完整 aim，不是 `aim - currentHeat`。原版可因此留有热量余额，不能每 tick 清零。正常输入满罐时 aim 最大 1,000 HU，正常缓冲最大 1,999 HU，存储上限取 2,000 HU。

IC2 在判断输出空间之前取热。因此输出堵塞时，机器可能先取一批热留在缓冲，之后达到目标余额便停止取热；这与“堵塞时不消费输入/处理 HU”并不矛盾。来源生成热量或燃烧燃料的策略仍由热源自己负责。

本机采用主动抽热，公开 HU 端口不接受热源的预先推送，避免空机囤热；正常本模组热源保留热缓冲，供机器通过正确朝向抽取。各来源的 HU 限额由来源能力执行。本机不直接使用 EU、不设置压力、不升高蒸汽发生器的压力。

## GUI

专用 GUI 背景位于 `assets/mio_icif/textures/gui/gui_steam_repressurizer.png`。2026-10-06 按项目统一 GUI 风格重新绘制，复用 `tools/si_gui2.py` 的浅灰面板、细边框、凹槽与物品槽配色；布局仍按 IC2 XML 对齐，不再直接使用 IC2 原始背景。可运行 `python tools/gui_steam_repressurizer.py` 重现该纹理。

- 面板 176×166；双罐尺寸与坐标按 XML 对齐，居中标题位于 y=6。
- 保留 36 格玩家物品栏，物品坐标 `(8,84)`，快捷栏 y=142；这是 XML 背景坐标加槽位边框偏移后的坐标。
- 没有单元槽、升级槽、HU 条或虚构的加工进度条。
- 同步输入/输出流体注册 ID 和数量；服务端更换蒸汽类型或清空罐后，客户端按新状态重建 FluidStack。
- 双罐使用真实流体纹理，鼠标提示显示流体名称、数量和容量，支持中英语言键。

## 与 IC2 环境相关的映射

原版 `STEAM = FluidRegistry.getFluid("steam")` 是外部注册的蒸汽，不存在时原版不工作，并显示缺少流体的提示。当前项目已有自身的普通蒸汽 `mio_icif:steam`，因此把原版外部蒸汽输出映射为本模组普通蒸汽，并直接使用本模组流体能力与管道。不会因未安装第三方蒸汽模组而禁用机器。

保留项目已有 `lit` 方块模型，仅在本 tick 真实完成转换时点亮。IC2 的该实体没有相同的工作状态更新；此为项目的状态反馈，GUI 与转换规则仍按上述原版实现。

## 旧存档

- 原来两个热量字段合并成一个余额，使用 `repressurizerVersion=1` 标识；新保存只写一次 `heat`，再次加载不会重复合并。
- 原先错误生成的过热蒸汽产物一次性转成等量普通蒸汽。保留原有体积；超过新容量的旧余额不会删除，在抽取到低于 10,000 mB 前不能继续加入产物。
- 原来 4 格物品仍保存于旧库存，但不显示在新 GUI 中、不能插入新物品、不产生升级作用。可通过漏斗/物品管道抽出，或破坏机器时取回。
- 保留方块、物品、方块实体与菜单的原注册 ID，不要求玩家拆除后重放。

## 验证

新增专用测试：`src/addonExample/java/example/sicore/SteamRepressurizerGameTests.java`。测试不打入正式模组，通过 `si_steam_audit` 命名空间在独立世界运行，不修改玩家配置。

```powershell
.\gradlew.bat runSteamRepressurizerAudit --init-script tools/steam-repressurizer-audit.init.gradle --offline --console=plain --max-workers=2
.\gradlew.bat build --offline --console=plain --max-workers=2
```

覆盖：同 tick 多批次和两种输入切换；完整批次输出空间与余量；六面取热及热源方向；空机不取热；流体输入/输出角色、模拟与抽取；无内建弹出；旧热量/超容量流体/库存迁移；36 槽 GUI 和流体 ID 同步；配置倍率实际生效。

2026-10-06 验证结果：

- 编译、完整 `build` 和 `checkCoreArchitecture` 通过。
- 七项专用 GameTest 全部通过，包括真实 10 线圈电力热源连续供热与模拟客户端菜单流体重建/类型切换/清空。
- GUI 已按模组统一风格重绘；检查了 256×256 纹理、176×166 面板、两处 38×47 流体区域、36 格物品位置及面板外透明区域。语言 JSON 和本文链接检查通过。
- 产物：`build/libs/mio_icif-0.1.7.35.1.jar`。运行日志：`build/steam-repressurizer-audit/final-validation.log`。

客户端布局依据 XML 与纹理静态核对；未进行实际客户端画面的人工操作验证，服务端测试不代替游戏内的视觉检查。
