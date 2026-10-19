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

<SubPages id="power.md" icons={true} alphabetical={true} />
