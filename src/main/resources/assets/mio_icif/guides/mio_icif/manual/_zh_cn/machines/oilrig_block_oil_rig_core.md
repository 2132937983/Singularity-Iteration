---
navigation:
  title: "石油钻机核心"
  icon: mio_icif:oilrig/block_oil_rig_core
  parent: heavy.md
  position: 6
item_ids:
  - mio_icif:oilrig/block_oil_rig_core
---

# 石油钻机核心

<Row>
  <BlockImage id="mio_icif:oilrig/block_oil_rig_core" scale="3" />
</Row>

## 功能

石油钻机核心控制石油钻机多方块结构。钻机向下钻到基岩并产出原油。
钻头移除钻机下方的方块。每移除一个方块，都可能向储油模块提供原油。
钻头钻完核心周围的有限区域后，钻机停止。

## 电力数据

| 项目 | 数值 |
|---|---|
| 储能 | 50,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 放置石油钻机核心，并在同层四周放置八个模块。
2. 在核心上方叠放两个模块。
3. 在这两个模块上方叠放三个钛制钻机框架。
4. 在叠放模块的四个侧面各放两个框架。
5. 至少包含一个输入模块和一个储油模块。
6. 把HV导线连接到输入模块。

## 注意

- 右键石油钻机面板，查看钻机状态。核心没有界面。

> **警告：** 钻头摧毁钻探列中的方块，且不掉落物品。不要在基地上方建造钻机。


## 配方

<RecipesFor id="mio_icif:oilrig/block_oil_rig_core" fallbackText="-" />
