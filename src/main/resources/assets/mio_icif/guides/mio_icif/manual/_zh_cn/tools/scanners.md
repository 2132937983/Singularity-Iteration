---
navigation:
  title: "扫描仪与多功能仪表"
  icon: mio_icif:item_tool_od_scanner
  parent: tools.md
  position: 4
item_ids:
  - mio_icif:item_tool_od_scanner
  - mio_icif:item_tool_ov_scanner
  - mio_icif:item_tool_meter
  - mio_icif:item_tool_windmeter
  - mio_icif:item_geomagnetic_detector
  - mio_icif:item_tool_area_scanner
---

# 扫描仪与多功能仪表

## 矿物扫描仪

| 扫描仪 | 范围 | 每次耗电 | 容量 |
|---|---|---|---|
| <ItemLink id="mio_icif:item_tool_od_scanner" /> | 13×13×13 | 48 EU | 100,000 EU |
| <ItemLink id="mio_icif:item_tool_ov_scanner" /> | 25×13×25 | 96 EU | 1,000,000 EU |

- 右键扫描，界面按矿种列出数量，以及最近一块的方位和距离。
- 只统计 `c:ores` 里的方块，按 `c:ores/<种类>` 分组。
- OV 扫描仪由 OD 扫描仪合成，电量随之带过去。两者也是采矿机的两档扫描仪。

## 多功能仪表

<ItemLink id="mio_icif:item_tool_meter" /> 不耗电，有三个探头（G 键切换）：

- **电能。**右键电网设备打开数据界面；潜行右键切换读数（输入、输出、经过的电量等）。
- **风力。**右键风力动能发生机读它的有效风力；右键别处读脚下高度的风力。附带风向和天气系数。
- **地磁。**右键方块读那里的地磁强度和当前 Kp 指数。

<ItemLink id="mio_icif:item_tool_windmeter" /> 和 <ItemLink id="mio_icif:item_geomagnetic_detector" /> 已被取代：单独放进工作台换成多功能仪表。

## 区域扫描仪

<ItemLink id="mio_icif:item_tool_area_scanner" /> 右键机器，在世界里画出它的工作范围。潜行右键清除。
