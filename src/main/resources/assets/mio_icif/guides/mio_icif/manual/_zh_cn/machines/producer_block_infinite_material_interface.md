---
navigation:
  title: "无限军械库资材接口"
  icon: mio_icif:producer/block_infinite_material_interface
  parent: machines.md
  position: 11
item_ids:
  - mio_icif:producer/block_infinite_material_interface
---

# 无限军械库资材接口

<Row>
  <BlockImage id="mio_icif:producer/block_infinite_material_interface" scale="3" />
</Row>

## 功能

创造模式方块，和无限发电机一样没有合成表。为[蓝图建造机](producer_block_blueprint_builder.md)的无人机无限供应所有建材。

## 规则

* 放在建造机 16 格内即可，不用放任何东西进去：它没有界面，也没有槽位。
* 蓝图需要什么方块就给什么，不会消耗。
* 无人机送回的剩余建材直接消失。
* 漏斗和管道不会连接它。
* 如果离建造机更近的地方有普通的[军械库资材接口](producer_block_material_interface.md)，先用普通接口；普通接口没有的建材再由无限接口提供。
