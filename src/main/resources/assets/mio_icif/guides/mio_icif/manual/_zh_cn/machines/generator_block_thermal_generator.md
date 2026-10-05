---
navigation:
  title: "火力发电机"
  icon: mio_icif:generator/block_thermal_generator
  parent: generators.md
  position: 32
item_ids:
  - mio_icif:generator/block_thermal_generator
---

# 火力发电机

<Row>
  <BlockImage id="mio_icif:generator/block_thermal_generator" scale="3" />
</Row>

## 功能

机器燃烧熔炉燃料并产生 EU。
机器为电池槽位中的电池充电。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | LV (32 EU) |
| 输出 | 10 EU/t |
| 储能 | 4,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把 LV 导线连接到发电机。
2. 把煤炭放入燃料槽位。
3. 把电池放入电池槽位来充电。

## 配方

<RecipesFor id="mio_icif:generator/block_thermal_generator" fallbackText="-" />
