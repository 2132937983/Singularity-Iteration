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

机器利用照到身上的阳光产生 EU。早晚阳光弱、正午最强；辐照达到 700 W/m² 时机器达到满功率，再高也保持满功率。
下雨约剩 70% 的光，雷暴约剩 37%。板子越热发电越少（25 °C 以上每升 1 °C 少 0.4%），所以沙漠阳光足但温度高。
机器正上方需要露天；被遮住时只有散射光，完全黑暗时不发电。
界面显示今日记录：照到板上的阳光、晴天的对比曲线和输出。

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
