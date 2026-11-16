---
navigation:
  title: "蒸汽涡轮发电机"
  icon: mio_icif:generator/block_steam_turbine
  parent: generators.md
  position: 31
item_ids:
  - mio_icif:generator/block_steam_turbine
---

# 蒸汽涡轮发电机

<Row>
  <BlockImage id="mio_icif:generator/block_steam_turbine" scale="3" />
</Row>

## 功能

机器把蒸汽转换为 EU。
机器为电池槽中的电池充电，并把蒸汽单元倒入储罐。
蒸汽在涡轮中从进汽压力（蒸汽：10 bar 饱和；过热蒸汽：40 bar、400 °C）膨胀到背压。排大气时背压为 1 bar。旁边有一台工作中的冷凝机时，排汽进入冷凝机，背压降到 0.1 bar：同样的蒸汽做功更多（蒸汽 0.8 EU/mB，原 0.5；过热蒸汽 1.3，原 1.0）。
界面是电厂模拟屏：进汽、涡轮、发电机、排汽去冷凝机或排空，并显示膨胀读数。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | MV (128 EU) |
| 输出 | 64 EU/t |
| 储能 | 32,000 EU |
| 蒸汽消耗 | 128 mB/t → 64 EU/t (每 EU 2 mB) |
| 过热蒸汽消耗 | 64 mB/t → 64 EU/t (每 EU 1 mB) |
| 配冷凝机 | 蒸汽 80 mB/t、过热蒸汽约 49 mB/t → 64 EU/t |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把机器放在蒸汽反应堆旁，或把输送蒸汽的管道连接到机器。
2. 把 MV 导线连接到机器。

## 注意

- 只有储能未满时，机器才消耗蒸汽。

## 配方

<RecipesFor id="mio_icif:generator/block_steam_turbine" fallbackText="-" />
