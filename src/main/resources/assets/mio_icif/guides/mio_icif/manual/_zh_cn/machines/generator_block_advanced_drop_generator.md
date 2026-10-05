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
稀有物品比普通物品产生更多 EU。

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
