---
navigation:
  title: "太阳能发电机"
  icon: mio_icif:generator/block_solar_generator
  parent: generators.md
  position: 28
item_ids:
  - mio_icif:generator/block_solar_generator
---

# 太阳能发电机

<Row>
  <BlockImage id="mio_icif:generator/block_solar_generator" scale="3" />
</Row>

## 功能

机器在白天利用阳光产生 EU。
机器正上方必须能直接看到天空。
夜间和雨天时机器停止发电。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | LV (32 EU) |
| 输出 | 1 EU/t |
| 储能 | 2 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把发电机放在有天空的维度，例如主世界。
2. 移除发电机上方的所有不透明方块。
3. 把 LV 导线连接到发电机。
4. 把电池放入电池槽位来充电。

## 配方

<RecipesFor id="mio_icif:generator/block_solar_generator" fallbackText="-" />
