---
navigation:
  title: "军械库资材接口"
  icon: mio_icif:producer/block_material_interface
  parent: machines.md
  position: 11
item_ids:
  - mio_icif:producer/block_material_interface
---

# 军械库资材接口

<Row>
  <BlockImage id="mio_icif:producer/block_material_interface" scale="3" />
</Row>

## 功能

为[蓝图建造机](producer_block_blueprint_builder.md)的无人机提供建材。
它有 27 个槽位，并读取六个相邻面上的存储。

## 规则

* 任何带物品处理器的存储都可用：箱子、木桶、抽屉、AE2 接口、精致存储接口等。
* 无人机先从自身槽位取用，再从相邻存储取用。
* 未用完的建材会被送回接口。
* 漏斗和管道可以向 27 个槽位补充物品。RTS Building 可以把它链接为存储。
* 建造机使用 16 格内的接口。

## 配方

<RecipesFor id="mio_icif:producer/block_material_interface" fallbackText="-" />
