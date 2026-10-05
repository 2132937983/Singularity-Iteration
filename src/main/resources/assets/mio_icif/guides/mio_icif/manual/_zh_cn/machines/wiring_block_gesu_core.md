---
navigation:
  title: "大型储电GESU核心"
  icon: mio_icif:wiring/block_gesu_core
  parent: power.md
  position: 13
item_ids:
  - mio_icif:wiring/block_gesu_core
---

# 大型储电GESU核心

<Row>
  <BlockImage id="mio_icif:wiring/block_gesu_core" scale="3" />
</Row>

## 功能

GESU核心是GESU多方块结构的中心，储存EU。
核心六个面都接触GESU输入或输出模块时，结构成形。
输入模块决定输入速率。输出模块决定输出速率。

## 电力数据

| 项目 | 数值 |
|---|---|
| 储能 | 2,147,483,647 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 放置GESU核心。
2. 在核心的一个面上放置至少一个GESU输入模块。
3. 在核心的其余面上放置GESU输出模块。
4. 把发电机导线接到输入模块。
5. 把机器导线接到输出模块。
6. 打开核心界面，检查模块数量。

## 注意

- 结构为七个方块组成的十字形：核心和六个模块。
- 拆除一个模块后，GESU停止工作，直到补回模块。
- 界面有一个槽位为物品充电，一个槽位从电池取出EU。

## 配方

<RecipesFor id="mio_icif:wiring/block_gesu_core" fallbackText="-" />
