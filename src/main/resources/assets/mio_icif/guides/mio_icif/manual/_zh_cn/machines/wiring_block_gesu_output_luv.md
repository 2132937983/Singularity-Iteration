---
navigation:
  title: "大型储电GESU输出模块（LuV）"
  icon: mio_icif:wiring/block_gesu_output_luv
  parent: power.md
  position: 16
item_ids:
  - mio_icif:wiring/block_gesu_output_luv
---

# 大型储电GESU输出模块（LuV）

<Row>
  <BlockImage id="mio_icif:wiring/block_gesu_output_luv" scale="3" />
</Row>

## 功能

GESU输出模块（LuV）从GESU核心取出EU，以LuV输出到导线。
该模块是结构部件。模块仅在完整的GESU结构中工作。
每个输出模块都提高核心的输出速率。

## 电力数据

| 项目 | 数值 |
|---|---|
| 储能 | 65,536 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把模块直接放在GESU核心的一个面上。
2. 在核心的其余面上放满模块。
3. 把LuV导线接到输出模块的外侧面。
4. 在导线和低电压等级机器之间放置变压器。

## 注意


> **警告：** 输出为LuV。电压等级更低的机器因过压爆炸。


## 配方

<RecipesFor id="mio_icif:wiring/block_gesu_output_luv" fallbackText="-" />
