---
navigation:
  title: "武装库"
  icon: mio_icif:producer/block_armory
  parent: machines.md
  position: 9
item_ids:
  - mio_icif:producer/block_armory
---

# 武装库

<Row>
  <BlockImage id="mio_icif:producer/block_armory" scale="3" />
</Row>

## 功能

武装库储存六套装备。每套有六件：头盔、胸甲、护腿、靴子、主手和副手。
武装库按指令把一套储存的装备飞送给主人。
武装库用EU修复储存的装备并为其充能。Botania装备使用武装库魔力罐中的魔力。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | HV (512 EU) |
| 最大输入 | 512 EU/t |
| 储能 | 1,000,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把HV导线连接到武装库。
2. 右键已通电的武装库，成为它的主人。
3. 在界面中把装备放入套装槽位。
4. 手持武装库遥控器潜行右键武装库，完成配对。
5. 把遥控器戴在护符或腰带饰品槽位。
6. 打开装备综合控制台的武装库页面，点击召唤。

## 注意

- 武装库储存足够EU后才接受主人。只有主人能打开、使用或拆除已绑定的武装库。
- 其他模组的装备需先用连接器套件连接，武装库才接受。
- 每次召唤消耗EU。距离越远或跨维度时，消耗越多。

## 配方

<RecipesFor id="mio_icif:producer/block_armory" fallbackText="-" />
