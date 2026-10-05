---
navigation:
  title: "储电盒"
  icon: mio_icif:wiring/block_bat_box
  parent: power.md
  position: 3
item_ids:
  - mio_icif:wiring/block_bat_box
---

# 储电盒

<Row>
  <BlockImage id="mio_icif:wiring/block_bat_box" scale="3" />
</Row>

## 功能

储电盒是LV储电方块。储电盒储存EU并向机器供电。
正面输出EU。其余五个面接收EU。
上方槽位为物品充电。下方槽位从电池取出EU。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | LV (32 EU) |
| 最大输入 | 32 EU/t |
| 储能 | 40,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 放置储电盒，使正面朝向你。
2. 把发电机导线接到五个输入面之一。
3. 用LV导线把正面接到机器。
4. 打开界面，选择红石模式以控制输出。

## 注意

- 红石模式可以停止输出，或在指定电量时发出信号。

## 配方

<RecipesFor id="mio_icif:wiring/block_bat_box" fallbackText="-" />
