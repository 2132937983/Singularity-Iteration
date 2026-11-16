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
机器靠磁场的变化发电，所以输出跟着磁场扰动：30,720 EU/t 是扰动为 1 时的输出。平静的日子里，白天日照的天空扰动磁场（中午约 0.55，夜里 0.08）；每天有 12% 的概率发生一次磁暴（Kp 6～9，持续半天到一天半），扰动最高到 1.5。同一个世界里所有发电机看到的是同一场磁暴。平均约为以前恒定出力的 27%。
界面是磁强记录：最近三天的扰动、每三小时的 Kp、输出与场地系数。地磁探测器显示场地系数和当前 Kp。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | ZPM (131,072 EU) |
| 输出 | 扰动为 1 时 30,720 EU/t（平均约 8,300 EU/t） |
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
