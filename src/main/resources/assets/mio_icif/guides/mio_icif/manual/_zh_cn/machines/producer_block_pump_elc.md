---
navigation:
  title: "泵"
  icon: mio_icif:producer/block_pump_elc
  parent: machines.md
  position: 53
item_ids:
  - mio_icif:producer/block_pump_elc
---

# 泵

<Row>
  <BlockImage id="mio_icif:producer/block_pump_elc" scale="3" />
</Row>

## 功能

泵抽取机器前方的流体源方块，并把流体存入储罐。
抽取范围高1层，长8格，两侧各8格。
放在采矿机旁时，泵也会抽走采矿机遇到的流体。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | LV (32 EU) |
| 最大输入 | 32 EU/t |
| 储能 | 20 EU |
| 工作耗电 | 1 EU/t |
| 单次工作时间 | 20 tick (1 s) |
| 单次耗电 | 20 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 放置泵，使其正面朝向流体。
2. 将LV导线连接到机器。
3. 把空桶或空单元放入容器槽位，或连接管道。
4. 从输出槽位取出装满的容器。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于LV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_pump_elc" fallbackText="-" />
