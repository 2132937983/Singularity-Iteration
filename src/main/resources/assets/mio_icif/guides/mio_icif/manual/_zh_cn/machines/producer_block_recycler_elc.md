---
navigation:
  title: "回收机"
  icon: mio_icif:producer/block_recycler_elc
  parent: machines.md
  position: 56
item_ids:
  - mio_icif:producer/block_recycler_elc
---

# 回收机

<Row>
  <BlockImage id="mio_icif:producer/block_recycler_elc" scale="3" />
</Row>

## 功能

回收机消耗EU把物品变为废料。每次加工只有一定几率产出废料。
机器每次加工都会消耗输入物品。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | LV (32 EU) |
| 最大输入 | 32 EU/t |
| 储能 | 45 EU |
| 工作耗电 | 1 EU/t |
| 单次工作时间 | 45 tick (2.2 s) |
| 单次耗电 | 45 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将LV导线连接到机器。
2. 把不需要的物品放入输入槽位。
3. 安装超频升级以加快加工。
4. 从输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于LV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_recycler_elc" fallbackText="-" />
