---
navigation:
  title: "电网遥测传感器"
  icon: mio_icif:module/item_module_grid_telemetry
  parent: suit/index.md
  position: 11
item_ids:
  - mio_icif:module/item_module_grid_telemetry
---

# 电网遥测传感器

<ItemImage id="mio_icif:module/item_module_grid_telemetry" scale="3" />

## 功能

该单元读取视线方块与穿戴者所在区块的EU电网。
面板显示视线方块的电压等级、电力包大小、EU流量与导线额定值。
面板显示区块内的发电机、用电机器、储电方块、导线、储存EU与过压风险。

## 电力数据

| 项目 | 数值 |
|---|---|
| 适用部位 | 护腿 / 头盔 |
| 功耗 | 4 EU/t（开启时） |
| 显示 | 需要量子头盔面罩 |

## 使用步骤

1. 把单元装入量子护腿或头盔。
2. 看向24格内的机器或导线。
3. 读取遥测面板的LOS行。
4. 比较电力包大小与导线额定值。
5. 面板显示OVER时更换导线。

## 注意

- OV计数显示区块内收到超过自身电压等级电力包的机器数量。

## 配方

<RecipesFor id="mio_icif:module/item_module_grid_telemetry" fallbackText="-" />
