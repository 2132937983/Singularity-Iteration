---
navigation:
  title: "粒子聚合发生器"
  icon: mio_icif:producer/block_neutron_polymerizer
  parent: machines.md
  position: 50
item_ids:
  - mio_icif:producer/block_neutron_polymerizer
---

# 粒子聚合发生器

<Row>
  <BlockImage id="mio_icif:producer/block_neutron_polymerizer" scale="3" />
</Row>

## 功能

粒子聚合发生器消耗大量EU，把物品转化为稀有物品。
煤变为钻石，钛变为铱，玻璃导线变为超导导线。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | EV (2,048 EU) |
| 最大输入 | 2,048 EU/t |
| 储能 | 2,304,000 EU |
| 工作耗电 | 1,536 EU/t |
| 单次工作时间 | 1,500 tick (75 s) |
| 单次耗电 | 2,304,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 通过变压器将EV电源连接到机器。
2. 把输入物品放入输入槽位。
3. 安装超频升级以加快加工。
4. 从输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于EV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_neutron_polymerizer" fallbackText="-" />
