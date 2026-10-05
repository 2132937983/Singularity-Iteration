---
navigation:
  title: "动能发电机"
  icon: mio_icif:generator/block_kinetic_generator
  parent: generators.md
  position: 18
item_ids:
  - mio_icif:generator/block_kinetic_generator
---

# 动能发电机

<Row>
  <BlockImage id="mio_icif:generator/block_kinetic_generator" scale="3" />
</Row>

## 功能

机器把动能（KU）转换为 EU。
机器从正面相邻的方块接收 KU。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | MV (128 EU) |
| 输出 | 4,096 EU/t |
| 储能 | 100,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 放置发电机，使其正面对着风力动能发生机背面。
2. 把 MV 导线连接到发电机。

## 配方

<RecipesFor id="mio_icif:generator/block_kinetic_generator" fallbackText="-" />
