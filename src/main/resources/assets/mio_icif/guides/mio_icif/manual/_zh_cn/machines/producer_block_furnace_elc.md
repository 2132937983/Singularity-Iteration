---
navigation:
  title: "电炉"
  icon: mio_icif:producer/block_furnace_elc
  parent: machines.md
  position: 20
item_ids:
  - mio_icif:producer/block_furnace_elc
---

# 电炉

<Row>
  <BlockImage id="mio_icif:producer/block_furnace_elc" scale="3" />
</Row>

## 功能

电炉消耗EU，按熔炉配方冶炼物品。
机器有1个输入槽位、1个输出槽位、1个电池槽位和4个升级槽位。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | LV (32 EU) |
| 最大输入 | 32 EU/t |
| 储能 | 300 EU |
| 工作耗电 | 3 EU/t |
| 单次工作时间 | 100 tick (5 s) |
| 单次耗电 | 300 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将LV导线连接到机器。
2. 把要冶炼的物品放入输入槽位。
3. 安装超频升级以加快加工。
4. 从输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于LV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_furnace_elc" fallbackText="-" />
