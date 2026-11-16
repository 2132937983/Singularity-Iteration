---
navigation:
  title: "进阶掉落物发电机"
  icon: mio_icif:generator/block_advanced_drop_generator
  parent: generators.md
  position: 0
item_ids:
  - mio_icif:generator/block_advanced_drop_generator
---

# 进阶掉落物发电机

<Row>
  <BlockImage id="mio_icif:generator/block_advanced_drop_generator" scale="3" />
</Row>

## 功能

机器吸引掉落物，并销毁它们来产生 EU。
机器的范围和输出都大于掉落物发电机。
稀有物品比普通物品产生更多 EU。普通物品按熔炉燃烧时间计算。
机器是一台有炉膛温度的焚化炉。可燃物把炉膛烧向 1,000 °C；不可燃物（石头、泥土）不提供燃料，每件让炉膛降 25 °C。燃料转为 EU 的比例随炉温：300 °C 以下没有，850 °C 全额。冷机先用最初的燃料升温。
每件物品都在灰斗里留下灰（可燃物 1 个，其他 2 个）。灰满 64 个时机器不再吸取物品；用手、漏斗或管道把灰取出。
界面是记录仪：最近 12 秒的炉膛温度，每件物品一个记号，以及效率、输出和灰量。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | ULV (8 EU) |
| 输出 | 40 EU/t |
| 储能 | 100,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把发电机放在物品收集线路的出口。
2. 把 HV 导线连接到发电机。
3. 把不需要的物品丢在发电机附近。

## 注意


> **警告：** 机器销毁范围内的所有掉落物，包括你的物品。


## 配方

<RecipesFor id="mio_icif:generator/block_advanced_drop_generator" fallbackText="-" />
