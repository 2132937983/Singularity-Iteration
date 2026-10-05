---
navigation:
  title: "精炼机"
  icon: mio_icif:producer/block_oil_refinery_elc
  parent: machines.md
  position: 48
item_ids:
  - mio_icif:producer/block_oil_refinery_elc
---

# 精炼机

<Row>
  <BlockImage id="mio_icif:producer/block_oil_refinery_elc" scale="3" />
</Row>

## 功能

精炼机消耗EU精炼流体。原油变为柴油。
机器也把水或蒸汽变为蒸馏水。
每个超频升级都会增加每次循环的流体量和EU消耗。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MV (128 EU) |
| 最大输入 | 128 EU/t |
| 储能 | 2,000 EU |
| 工作耗电 | 8 EU/t |
| 单次工作时间 | 10 tick (0.5 s) |
| 单次耗电 | 80 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将MV导线连接到机器。
2. 把输入流体泵入机器，或把装满的单元放入输入槽位。
3. 把空单元放入输出单元槽位，或连接管道取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于MV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_oil_refinery_elc" fallbackText="-" />
