---
navigation:
  title: "风力动能发生机"
  icon: mio_icif:kugenerator/block_wind_kinetic_generator
  parent: generators.md
  position: 40
item_ids:
  - mio_icif:kugenerator/block_wind_kinetic_generator
---

# 风力动能发生机

<Row>
  <BlockImage id="mio_icif:kugenerator/block_wind_kinetic_generator" scale="3" />
</Row>

## 功能

机器通过转子把风力转换为动能（KU）。
风力随高度和恶劣天气增加。转子前方的方块使风力降低。
机器只从背面输出 KU。

## 电力数据

该方块不使用EU。

## 使用步骤

1. 把转子放入转子槽位。
2. 移除转子前方区域的所有方块。
3. 放置动能发电机，使其正面对着机器背面。
4. 用风力计点击机器，读取有效风力。

## 注意


> **警告：** 风力超过转子上限时，转子损耗速度变为四倍。

- 转子在运行中持续损耗。在转子损坏前更换它。

## 配方

<RecipesFor id="mio_icif:kugenerator/block_wind_kinetic_generator" fallbackText="-" />
