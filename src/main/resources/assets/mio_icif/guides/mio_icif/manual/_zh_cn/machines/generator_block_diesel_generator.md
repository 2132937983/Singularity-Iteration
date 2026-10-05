---
navigation:
  title: "柴油发电机"
  icon: mio_icif:generator/block_diesel_generator
  parent: generators.md
  position: 5
item_ids:
  - mio_icif:generator/block_diesel_generator
---

# 柴油发电机

<Row>
  <BlockImage id="mio_icif:generator/block_diesel_generator" scale="3" />
</Row>

## 功能

机器燃烧柴油并产生 EU。
机器只接受柴油桶和柴油单元。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | EV (2,048 EU) |
| 输出 | 120 EU/t |
| 储能 | 1,000,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把 HV 导线连接到发电机。
2. 把柴油桶放入输入槽位。
3. 从输出槽位取出空容器。
4. 把电池放入电池槽位来充电。

## 配方

<RecipesFor id="mio_icif:generator/block_diesel_generator" fallbackText="-" />
