---
navigation:
  title: "电解机"
  icon: mio_icif:producer/block_electrolyzer_elc
  parent: machines.md
  position: 23
item_ids:
  - mio_icif:producer/block_electrolyzer_elc
---

# 电解机

<Row>
  <BlockImage id="mio_icif:producer/block_electrolyzer_elc" scale="3" />
</Row>

## 功能

电解机消耗EU分解水单元中的水，并把能量存储为化学能。
机器把储存的能量送回未满的相邻方块。输出槽位接收空单元。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | LV (32 EU) |
| 最大输入 | 32 EU/t |
| 储能 | 400 EU |
| 工作耗电 | 10 EU/t |
| 单次工作时间 | 20 tick (1 s) |
| 单次耗电 | 200 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将LV导线连接到机器。
2. 把水单元放入输入槽位。
3. 在电解机旁放置储电方块或机器，以取回储存的能量。
4. 从输出槽位取出空单元。

## 注意


> **警告：** 电压高于LV时，机器因过压爆炸。


## 配方

<RecipesFor id="mio_icif:producer/block_electrolyzer_elc" fallbackText="-" />
