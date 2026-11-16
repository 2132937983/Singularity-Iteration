---
navigation:
  title: "放射性同位素温差发电机"
  icon: mio_icif:generator/block_rt_generator
  parent: generators.md
  position: 26
item_ids:
  - mio_icif:generator/block_rt_generator
---

# 放射性同位素温差发电机

<Row>
  <BlockImage id="mio_icif:generator/block_rt_generator" scale="3" />
</Row>

## 功能

机器利用放射性同位素燃料靶丸产生 EU。
每增加一个靶丸，发电量翻倍（温带空气中 1、2、4、8、16、32 EU/t）。靶丸不会耗尽。
它是温差发电机：冷端越凉，发电越多。六面中最好的一面决定冷端：冰、浮冰、蓝冰、雪块或细雪 ×1.25，水 ×1.15；否则为空气，炎热群系（35 °C 以上）×0.9，下界 ×0.7。任何一面贴着岩浆再 ×0.6。
界面是记录仪：热端与冷端两支温度计、最近 12 秒的输出与靶丸表，以及冷端系数。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | LV (32 EU) |
| 输出 | 32 EU/t |
| 储能 | 20,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把 LV 导线连接到发电机。
2. 把最多六个燃料靶丸放入靶丸槽位。

## 配方

<RecipesFor id="mio_icif:generator/block_rt_generator" fallbackText="-" />
