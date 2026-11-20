---
navigation:
  title: "回收机"
  icon: mio_icif:producer/block_recycler_elc
  parent: machines.md
  position: 56
item_ids:
  - mio_icif:producer/block_recycler_elc
---

# 回收机

<Row>
  <BlockImage id="mio_icif:producer/block_recycler_elc" scale="3" />
</Row>

## 功能

回收机消耗EU，把物品撕碎并分选出废料。机器每次加工都会消耗一个输入物品。

**原理：** 双轴撕碎。两根反向转动的刀轴把物品撕碎，从碎料里分选出可用的部分。

**产出按比例累计：** 每回收一件，把产出率（废料 1/8，回收配方可以写自己的产物和比例）加进累计值，满 1 出一个产物。所以正好每 8 件出 1 个废料，没有随机。换成另一种产物时，累计值从 0 开始。界面显示产出率、当前累计值和还差几件。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | LV (32 EU) |
| 最大输入 | 32 EU/t |
| 储能 | 45 EU |
| 工作耗电 | 1 EU/t |
| 单次工作时间 | 45 tick |
| 单次耗电 | 45 EU |
| 产出 | 每 8 件 1 个废料（按比例累计） |

表中的工时和耗电是没有超频时的值。每个超频升级让工时变为 0.7 倍、耗电变为 1.6 倍。界面和 JEI 显示当前输入的实际数值。

## 使用步骤

1. 将LV导线连接到机器。
2. 把不需要的物品放入输入槽位。
3. 安装超频升级以加快加工。
4. 从输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于LV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_recycler_elc" fallbackText="-" />
