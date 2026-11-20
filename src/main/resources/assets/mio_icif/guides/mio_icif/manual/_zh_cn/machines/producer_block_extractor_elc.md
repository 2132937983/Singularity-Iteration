---
navigation:
  title: "提取机"
  icon: mio_icif:producer/block_extractor_elc
  parent: machines.md
  position: 25
item_ids:
  - mio_icif:producer/block_extractor_elc
---

# 提取机

<Row>
  <BlockImage id="mio_icif:producer/block_extractor_elc" scale="3" />
</Row>

## 功能

提取机消耗EU，从物品中提取材料。粘性树脂变为橡胶。
机器有1个输入槽位、1个输出槽位、1个电池槽位和4个升级槽位。

**原理：** 螺旋压榨。螺杆把物料推向出口，越往前螺槽越浅，把汁液和可用的部分挤出来。

**工时和功率：** 每条配方按自己写的工时和功率运行：多数配方 100 tick × 10 EU/t（1,000 EU）；砂砾、火药、燧石粉、下界岩等 150 tick × 12 EU/t；锡罐倒空 50 tick × 5 EU/t。没写数值的配方（粘性树脂、橡胶树苗、水单元）300 tick × 2 EU/t。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | LV (32 EU) |
| 最大输入 | 32 EU/t |
| 储能 | 1,200 EU |
| 工作耗电 | 按配方（默认 2 EU/t） |
| 单次工作时间 | 按配方（默认 300 tick） |
| 单次耗电 | 按配方（默认 600 EU） |

表中的工时和耗电是没有超频时的值。每个超频升级让工时变为 0.7 倍、耗电变为 1.6 倍。界面和 JEI 显示当前输入的实际数值。

## 使用步骤

1. 将LV导线连接到机器。
2. 把物品放入输入槽位。
3. 安装超频升级以加快加工。
4. 从输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于LV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_extractor_elc" fallbackText="-" />
