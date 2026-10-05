---
navigation:
  title: "MFE充电座"
  icon: mio_icif:wiring/block_mfe_charger
  parent: power.md
  position: 27
item_ids:
  - mio_icif:wiring/block_mfe_charger
---

# MFE充电座

<Row>
  <BlockImage id="mio_icif:wiring/block_mfe_charger" scale="3" />
</Row>

## 功能

MFE充电座是顶部带充电板的HV储电方块。
充电板为顶部玩家的盔甲、手持物品和背包物品充电。
正面输出EU。其余五个面接收EU。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | HV (512 EU) |
| 最大输入 | 512 EU/t |
| 储能 | 4,000,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 放置MFE充电座，使正面朝向你。
2. 把HV发电机导线接到五个输入面之一。
3. 等待MFE充电座储存EU。
4. 站在MFE充电座顶部，为装备充电。

## 注意

- 仅在红石模式允许输出时，充电板才充电。

> **警告：** 输出为HV。电压等级更低的机器因过压爆炸。


## 配方

<RecipesFor id="mio_icif:wiring/block_mfe_charger" fallbackText="-" />
