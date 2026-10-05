---
navigation:
  title: "MFSU"
  icon: mio_icif:wiring/block_mfsu
  parent: power.md
  position: 28
item_ids:
  - mio_icif:wiring/block_mfsu
---

# MFSU

<Row>
  <BlockImage id="mio_icif:wiring/block_mfsu" scale="3" />
</Row>

## 功能

MFSU是EV储电方块。MFSU储存EU并向机器供电。
正面输出EU。其余五个面接收EU。
上方槽位为物品充电。下方槽位从电池取出EU。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | EV (2,048 EU) |
| 最大输入 | 2,048 EU/t |
| 储能 | 40,000,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 放置MFSU，使正面朝向你。
2. 把发电机导线接到五个输入面之一。
3. 用EV导线把正面接到机器。
4. 打开界面，选择红石模式以控制输出。

## 注意

- 红石模式可以停止输出，或在指定电量时发出信号。

> **警告：** 输出为EV。电压等级更低的机器因过压爆炸。


## 配方

<RecipesFor id="mio_icif:wiring/block_mfsu" fallbackText="-" />
