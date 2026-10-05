---
navigation:
  title: "半流质发电机"
  icon: mio_icif:generator/block_semifluid_generator
  parent: generators.md
  position: 27
item_ids:
  - mio_icif:generator/block_semifluid_generator
---

# 半流质发电机

<Row>
  <BlockImage id="mio_icif:generator/block_semifluid_generator" scale="3" />
</Row>

## 功能

机器燃烧半流质燃料并产生 EU。
机器接受沼气、生物质、原油和柴油。
每种燃料的发电量不同。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | LV (32 EU) |
| 输出 | 16 EU/t |
| 储能 | 32,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把 LV 导线连接到发电机。
2. 把燃料桶或燃料单元放入输入槽位。
3. 从输出槽位取出空容器。
4. 把电池放入电池槽位来充电。

## 配方

<RecipesFor id="mio_icif:generator/block_semifluid_generator" fallbackText="-" />
