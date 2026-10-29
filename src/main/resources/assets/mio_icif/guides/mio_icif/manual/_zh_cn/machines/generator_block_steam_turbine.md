---
navigation:
  title: "蒸汽涡轮发电机"
  icon: mio_icif:generator/block_steam_turbine
  parent: generators.md
  position: 31
item_ids:
  - mio_icif:generator/block_steam_turbine
---

# 蒸汽涡轮发电机

<Row>
  <BlockImage id="mio_icif:generator/block_steam_turbine" scale="3" />
</Row>

## 功能

机器把蒸汽转换为 EU。
机器为电池槽中的电池充电，并把蒸汽单元倒入储罐。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | MV (128 EU) |
| 输出 | 64 EU/t |
| 储能 | 32,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把机器放在蒸汽反应堆旁，或把输送蒸汽的管道连接到机器。
2. 把 MV 导线连接到机器。

## 注意

- 只有储能未满时，机器才消耗蒸汽。

## 配方

<RecipesFor id="mio_icif:generator/block_steam_turbine" fallbackText="-" />
