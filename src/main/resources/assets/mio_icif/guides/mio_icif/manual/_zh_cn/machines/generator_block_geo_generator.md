---
navigation:
  title: "地热发电机"
  icon: mio_icif:generator/block_geo_generator
  parent: generators.md
  position: 14
item_ids:
  - mio_icif:generator/block_geo_generator
---

# 地热发电机

<Row>
  <BlockImage id="mio_icif:generator/block_geo_generator" scale="3" />
</Row>

## 功能

机器把岩浆转换为 EU。
机器把岩浆桶和岩浆单元中的岩浆移入内部储罐。
每个相邻的岩浆方块都使发电量增加。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | LV (32 EU) |
| 输出 | 20 EU/t |
| 储能 | 2,400 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把 LV 导线连接到发电机。
2. 把岩浆桶放入输入槽位。
3. 从输出槽位取出空桶。
4. 把电池放入电池槽位来充电。

## 注意

- 输出槽位已满时，机器不会倒空桶。

## 配方

<RecipesFor id="mio_icif:generator/block_geo_generator" fallbackText="-" />
