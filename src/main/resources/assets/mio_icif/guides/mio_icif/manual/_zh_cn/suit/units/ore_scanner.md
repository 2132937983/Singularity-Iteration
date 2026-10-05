---
navigation:
  title: "地震矿物扫描仪"
  icon: mio_icif:module/item_module_ore_scanner
  parent: suit/index.md
  position: 10
item_ids:
  - mio_icif:module/item_module_ore_scanner
---

# 地震矿物扫描仪

<ItemImage id="mio_icif:module/item_module_ore_scanner" scale="3" />

## 功能

打开单元时，单元向半径16格的地层发出声呐脉冲。
脉冲以光环形式从穿戴者向外扩散。面罩上出现短暂的故障特效。
回波到达矿石方块时矿石亮起。矿石显示为矿物颜色的发光格，可透过岩石看到。
脉冲结束后，小标签显示每种矿物最近的一处及其距离。

## 电力数据

| 项目 | 数值 |
|---|---|
| 适用部位 | 靴子 / 护腿 |
| 功耗 | 24 EU/t（开启时） |
| 显示 | 需要量子头盔面罩 |

## 使用步骤

1. 把单元装入量子靴子或护腿。
2. 穿戴量子头盔。
3. 走到采矿区域。
4. 打开单元并等待回波。
5. 朝亮起的矿石挖掘。

## 注意

- 第一次脉冲后，单元每50 tick再次扫描，不显示脉冲特效。
- 关闭再打开单元可发出新的脉冲。

## 配方

<RecipesFor id="mio_icif:module/item_module_ore_scanner" fallbackText="-" />
