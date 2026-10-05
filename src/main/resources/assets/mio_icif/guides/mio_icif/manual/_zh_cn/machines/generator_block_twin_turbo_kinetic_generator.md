---
navigation:
  title: "双涡轮增压动能发电机"
  icon: mio_icif:generator/block_twin_turbo_kinetic_generator
  parent: generators.md
  position: 34
item_ids:
  - mio_icif:generator/block_twin_turbo_kinetic_generator
---

# 双涡轮增压动能发电机

<Row>
  <BlockImage id="mio_icif:generator/block_twin_turbo_kinetic_generator" scale="3" />
</Row>

## 功能

机器把正面方块提供的动能（KU）转换为 EU。
来自风力或水力动能发生机的 KU 按全效率转换。
来自其他来源的 KU 按低效率转换。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | EV (2,048 EU) |
| 输出 | 8,192 EU/t |
| 储能 | 400,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 放置发电机，使其正面对着风力动能发生机背面。
2. 把 EV 导线连接到发电机。

## 注意

- 机器的 KU 转 EU 比率高于涡轮增压动能发电机。

## 配方

<RecipesFor id="mio_icif:generator/block_twin_turbo_kinetic_generator" fallbackText="-" />
