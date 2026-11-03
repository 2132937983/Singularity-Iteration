---
navigation:
  title: "输电与储电"
  icon: mio_icif:wiring/block_mfe
  parent: index.md
  position: 12
---

# 输电与储电

导线在方块之间传送EU。变压器改变电压等级。
储电方块储存EU并给物品充电。

## 电网的服务器负载

- 管理员可输入 /mio_icif perf 查看服务器 tick，以及每个维度电网每 tick 所用的时间（最近 256 tick 的中位数、95 百分位与最大值）。
- 经电缆的送电使用路径档案（熔断、绝缘与电击的最低限值）。低于全部限值的送电无需逐格检查电缆。熔断、绝缘烧毁与电击照常发生。
- 电缆不再 tick。最近 1 秒内有电流通过的电缆视为通电。
- /mio_icif perf 还会显示最近 100 tick 每 tick 的负载：会通知邻居的方块实体变化、客户端更新、状态灯检查、邻居能力查询及其中由缓存回答的次数，以及休眠省下的机器 tick。
- 大型电网按每批最多 32 个电源的窗口结算。在电缆、机器及其接口面不变时，各窗口的路由计划会被保留，不必每 tick 重新搜索。

## 其他模组的方块（FE）

SI 电网与导线旁其他模组的方块交换 Forge Energy（FE），双向均为 4 FE = 1 EU（无损，与 Mekanism 相同）。自 0.1.11.1 起：

- 电网在公平分配中与 SI 机器、储能一起结算这些方块。其他模组的机器与 SI 储能一样，分得 SI 负载用剩的电能；被请求时会给出 FE 的其他模组发电机，像 SI 发电机一样向电网供电。FE 以 0 电压进入电网：不会烧毁导线，也不需要变压器。
- 其他模组的电池（既收又放 FE）充电到 90 % 后，向 SI 负载供电直到降至 10 %，如此往复。它从不给 SI 储能充电，因此电能不会在它与 SI 储能之间来回往返。
- SI 导线不接收 FE。只会推送 FE（被请求时不给出 FE）的其他模组方块，必须贴着 SI 机器或并网接口。
- SI 导线不与其他模组的导线连接（方块标签 `mio_icif:foreign_energy_cables`，或方块 ID 含 cable、duct、conduit、transmitter、wire、energy_pipe）。要连接其他模组的电网，请使用并网接口（输出、输入或双向）。GregTech 与 Modern Industrialization 的导线照旧连接（EU，1 EU = 1 EU）。
- 以焦耳计量的机器接口（Mekanism）会把不足 1 EU 的部分留到下次传输，小电流也能完整送达。
- `/mio_icif perf` 显示每 tick 并网接口传输的 FE、向其他模组方块输出与从中输入的 FE，以及邻居探测次数。

<SubPages id="power.md" icons={true} alphabetical={true} />
