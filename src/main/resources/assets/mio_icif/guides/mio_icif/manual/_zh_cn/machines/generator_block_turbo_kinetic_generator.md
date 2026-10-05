---
navigation:
  title: "涡轮增压动能发电机"
  icon: mio_icif:generator/block_turbo_kinetic_generator
  parent: generators.md
  position: 33
item_ids:
  - mio_icif:generator/block_turbo_kinetic_generator
---

# 涡轮增压动能发电机

<Row>
  <BlockImage id="mio_icif:generator/block_turbo_kinetic_generator" scale="3" />
</Row>

## 功能

机器把正面方块提供的动能（KU）转换为 EU。
来自风力或水力动能发生机的 KU 按全效率转换。
来自其他来源的 KU 按低效率转换。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | HV (512 EU) |
| 输出 | 2,048 EU/t |
| 储能 | 200,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 放置发电机，使其正面对着风力动能发生机背面。
2. 把 HV 导线连接到发电机。

## 配方

<RecipesFor id="mio_icif:generator/block_turbo_kinetic_generator" fallbackText="-" />
