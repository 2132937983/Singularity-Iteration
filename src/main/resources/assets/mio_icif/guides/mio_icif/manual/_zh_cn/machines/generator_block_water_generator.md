---
navigation:
  title: "水力发电机"
  icon: mio_icif:generator/block_water_generator
  parent: generators.md
  position: 37
item_ids:
  - mio_icif:generator/block_water_generator
---

# 水力发电机

<Row>
  <BlockImage id="mio_icif:generator/block_water_generator" scale="3" />
</Row>

## 功能

机器把水桶中的水转换为 EU。
机器正面有水时，不用水桶也能产生 EU。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | LV (32 EU) |
| 输出 | 32 EU/t |
| 储能 | 64,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 放置发电机，使其正面对着水。
2. 把水桶放入水桶槽位。
3. 从水桶槽位取出空桶。
4. 把 LV 导线连接到发电机。

## 配方

<RecipesFor id="mio_icif:generator/block_water_generator" fallbackText="-" />
