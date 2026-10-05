---
navigation:
  title: "大型储电GESU输入模块"
  icon: mio_icif:wiring/block_gesu_input_iv
  parent: power.md
  position: 14
item_ids:
  - mio_icif:wiring/block_gesu_input_iv
---

# 大型储电GESU输入模块

<Row>
  <BlockImage id="mio_icif:wiring/block_gesu_input_iv" scale="3" />
</Row>

## 功能

GESU输入模块从导线接收EU，并把EU送入GESU核心。
该模块是结构部件。模块仅在完整的GESU结构中工作。
每个输入模块都提高核心的输入速率。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MAX (2,147,483,648 EU) |
| 最大输入 | 2,147,483,647 EU/t |
| 储能 | 4,294,967,294 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把模块直接放在GESU核心的一个面上。
2. 在核心的其余面上放满模块。
3. 把发电机导线接到输入模块的外侧面。
4. 增加输入模块以提高输入速率。

## 配方

<RecipesFor id="mio_icif:wiring/block_gesu_input_iv" fallbackText="-" />
