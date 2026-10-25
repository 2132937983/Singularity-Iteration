# 0.1.10.18 更新说明：传送机粒子

基于 0.1.10.17。共 1 项。构建环境为 Gradle 8.10.2、JDK 21、NeoForge 21.1.218，离线构建。

## 现象

传送机从不显示粒子。设计中，接通红石并已链接的传送机应在上方显示粒子：可以传送时为蓝色，冷却中（刚有实体传送到这台传送机）为绿色。

## 原因

* 粒子由客户端的 `tickClient()` 生成，它读取 `isActive` 与 `cooldown`。
* 这两个值只写入存档（`saveAdditional`），不在更新标签中；传送机也没有 `getUpdatePacket`。因此客户端的方块实体始终是 `isActive = false`，`tickClient()` 立即返回。
* 0.1.10.17 的同步调查中发现（`docs/releases/0.1.10.17/UPDATE.md` “未修改的发现”）。
* 颜色：`spawnParticles` 使用固定为红色的 `DustParticleOptions.REDSTONE`，并把预期的颜色（r, g, b）作为粒子的速度传入。因此即使客户端收到状态，粒子也只会是向上（冷却中）或侧向（就绪）漂移的红色。

## 修复

* 更新标签（区块加载时发送）在父类的客户端数据之外带有 `isActive` 与 `cooldown`。
* `getUpdatePacket()`：方块实体数据包只含这两个值（`particleTag()`，约 30 字节）。
* 发送时机：激活状态改变时（`setActive`），以及冷却开始时（`onTeleportFrom`，目标传送机），经 `ClientSyncThrottle`（每 5 tick 最多一次，最终状态一定送达）。服务器每 tick 递减冷却时间，但不为此发包。
* 客户端在 `tickClient()` 中自行递减冷却时间，因此冷却结束时颜色自动变回蓝色，无需再发包。
* `onDataPacket`：只应用这两个值。原版默认实现会对数据包中的标签执行完整的 `loadAdditional`，重置标签中没有的客户端字段（例如链接目标）。
* 粒子颜色：两个预先创建的 `DustParticleOptions`（冷却中绿色、就绪蓝色，大小 1.0），速度为 0。

## 存档与兼容

* 存档格式不变；没有新增或删除的注册项；语言文件与 API 不变。
* 只修改了 `mio_icif_teleporter_elc`。

## 验证

* Gradle 离线完整构建；检查 `check_1118.py`；回归检查。
* 已编写并编译、本机无法运行：`Round1118GameTests`（2 项：更新标签与数据包带有粒子状态且数据包只含这两个值；客户端副本从数据包取得激活状态与冷却时间，链接目标与物品不变，并自行倒数冷却时间）。
* 未在游戏内确认：粒子的实际显示。
