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
机器正面有流动的水时，不用水桶也能产生 EU：按流速最多 4 EU/t（河流、潮流、流动水或瀑布，规则与水力动能发生机相同）。河流和海洋以外的静水不发电。
界面显示前方的水与流速、水箱和两路输出。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | LV (32 EU) |
| 输出 | 最多 8 EU/t |
| 储能 | 64,000 EU |
| 水箱中的水 | 1 mB/t → 4 EU/t (4 EU/mB) |
| 正面的水 | 平均水流 2 EU/t，最多 4 EU/t |

这些数值由本版本的方块实体实测得出。

发电倍率（配置 GeneratorOutput）不为 1.0 时，EU 数值会改变。燃料消耗不变。

## 使用步骤

1. 放置发电机，使其正面对着水。
2. 把水桶放入水桶槽位。
3. 从水桶槽位取出空桶。
4. 把 LV 导线连接到发电机。

## 配方

<RecipesFor id="mio_icif:generator/block_water_generator" fallbackText="-" />
