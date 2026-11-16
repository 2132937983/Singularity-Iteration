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

机器利用照到身上的阳光产生 EU。早晚阳光弱、正午最强；辐照达到 700 W/m² 时机器达到满功率，再高也保持满功率。
下雨约剩 70% 的光，雷暴约剩 37%。板子越热发电越少（25 °C 以上每升 1 °C 少 0.4%），所以沙漠阳光足但温度高。
机器正上方需要露天；被遮住时只有散射光，完全黑暗时不发电。
界面显示今日记录：照到板上的阳光、晴天的对比曲线和输出。
夜间露天时，夜间核心保持较低的输出。
机器可在四个槽位中为物品充电。

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
