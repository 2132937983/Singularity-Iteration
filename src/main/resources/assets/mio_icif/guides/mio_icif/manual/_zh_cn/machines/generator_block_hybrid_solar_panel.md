---
navigation:
  title: "混合太阳能发电机"
  icon: mio_icif:generator/block_hybrid_solar_panel
  parent: generators.md
  position: 16
item_ids:
  - mio_icif:generator/block_hybrid_solar_panel
---

# 混合太阳能发电机

<Row>
  <BlockImage id="mio_icif:generator/block_hybrid_solar_panel" scale="3" />
</Row>

## 功能

机器白天利用阳光产生 EU，夜间以较低速率发电。
机器正上方必须能直接看到天空。
雨天时机器按夜间速率发电。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | MV (128 EU) |
| 输出 | 128 EU/t |
| 储能 | 100,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把面板放在有天空的维度。
2. 移除面板上方的所有不透明方块。
3. 把 MV 导线连接到面板。
4. 把最多四个电池放入充电槽位。

## 配方

<RecipesFor id="mio_icif:generator/block_hybrid_solar_panel" fallbackText="-" />
