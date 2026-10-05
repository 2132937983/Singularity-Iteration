---
navigation:
  title: "风力发电机"
  icon: mio_icif:generator/block_wind_generator
  parent: generators.md
  position: 39
item_ids:
  - mio_icif:generator/block_wind_generator
---

# 风力发电机

<Row>
  <BlockImage id="mio_icif:generator/block_wind_generator" scale="3" />
</Row>

## 功能

机器利用风力产生 EU。
高度超过 Y 64 越多，输出越高。雨和雷暴也使输出增加。
机器周围 9x9x7 范围内的方块使输出降低。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | LV (32 EU) |
| 输出 | 32 EU/t |
| 储能 | 64,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把发电机放在远高于 Y 64 的位置。
2. 移除发电机周围的方块。
3. 把 LV 导线连接到发电机。
4. 把电池放入电池槽位来充电。

## 注意

- 低于 Y 64 时，机器不产生 EU。
- 风力强度随时间随机变化。

## 配方

<RecipesFor id="mio_icif:generator/block_wind_generator" fallbackText="-" />
