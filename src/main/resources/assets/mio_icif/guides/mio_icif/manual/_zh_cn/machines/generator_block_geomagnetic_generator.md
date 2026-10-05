---
navigation:
  title: "地磁发电机"
  icon: mio_icif:generator/block_geomagnetic_generator
  parent: generators.md
  position: 12
item_ids:
  - mio_icif:generator/block_geomagnetic_generator
---

# 地磁发电机

<Row>
  <BlockImage id="mio_icif:generator/block_geomagnetic_generator" scale="3" />
</Row>

## 功能

结构完整时，机器利用地磁场产生 EU。
低于海平面或下方有空气和水时，输出降低。
下界生物群系和山地生物群系使输出增加。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | ZPM (131,072 EU) |
| 输出 | 30,720 EU/t |
| 储能 | 400,000,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把地磁发电机放在实心地面上。
2. 在发电机上方竖直叠放两个地磁发电机天线。
3. 在发电机下方方块的四周放置四个地磁发电机基座。
4. 把导线连接到发电机。
5. 把电池放入电池槽位来充电。

## 注意

- 机器检查下方最多 20 格。每个空气或水方块都使输出降低。

## 配方

<RecipesFor id="mio_icif:generator/block_geomagnetic_generator" fallbackText="-" />
