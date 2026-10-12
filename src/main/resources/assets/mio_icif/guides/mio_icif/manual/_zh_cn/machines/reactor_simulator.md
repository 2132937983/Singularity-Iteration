---
navigation:
  title: "核反应堆模拟器"
  icon: mio_icif:reactor/item_reactor_uranium_quad
  parent: generators.md
  position: 22
---

# 核反应堆模拟器

## 功能

在建造之前计算反应堆设计。
它使用与[核反应堆发电机](generator_block_nuclear_reactor_generator.md)相同的计算。
它从冷态反应堆、全新组件和满燃料棒开始，计算 100 秒（100 个反应堆周期）。

## 步骤

1. 点击下方两行中的组件，将其选为当前组件。
2. 点击反应堆槽位，当前组件即放入该槽位。
3. 要移除组件，右键点击该槽位。
4. 点击"-"或"+"设置反应仓数量（0 至 6）。
5. 查看右侧的数值。

## 数值

| 项目 | 含义 |
|---|---|
| 输出 | 每 tick 的平均 EU 输出 |
| 发热 | 燃料棒在一个周期（1 秒）内产生的热量 |
| 散热 | 散热片在一个周期内移除的热量 |
| 外壳 | 计算结束时的外壳热量 / 最大外壳热量 |
| 趋势 | 每个周期外壳热量的变化（最近 10 个周期的平均值） |
| 余量 | 100% 减去最高外壳热量占最大值的百分比 |

同时满足以下三个条件的设计为 **安全**：

* 没有组件熔毁。
* 外壳热量不上升。
* 没有发生堆芯熔毁。

## 规则

* 燃料棒只把热量传给相邻的可储热组件。如果没有这样的组件，热量进入外壳。
* 每个相邻的燃料棒或中子反射板增加一个脉冲。脉冲越多 EU 越多，而热量增加得更快。
* 散热片移除自身的热量。部分散热片还会从外壳吸取热量。
* 组件散热片移除相邻组件的热量。
* 隔板提高最大外壳热量。
* 组件受到超过容量的热量会熔毁，其热量转入外壳。

## 教程 1：第一座反应堆（5 EU/t）

单燃料棒每个周期产生 4 热量。四个基础散热片每个最多移除 6 热量。

<ReactorSimulator preset="_,vent,_/vent,uranium_simple,vent/_,vent,_" />

## 教程 2：双联燃料棒（20 EU/t）

双联燃料棒每个单元有 2 个脉冲，每个周期产生 24 热量。四个散热片各接收 6 热量，这是基础散热片的上限。

<ReactorSimulator preset="_,vent,_/vent,uranium_dual,vent/_,vent,_" />

## 教程 3：高输出（300 EU/t，4 个反应仓）

五根四联燃料棒每根每个周期产生 96 热量。每根燃料棒有自己的铱散热片，没有散热片同时接触两根燃料棒。每个散热片接收 24 至 32 热量并移除 36。

<ReactorSimulator preset="iridium_vent,uranium_quad,iridium_vent,_,iridium_vent,uranium_quad,iridium_vent/_,iridium_vent,_,iridium_vent,_,iridium_vent,_/iridium_vent,iridium_vent,iridium_vent,uranium_quad,iridium_vent,iridium_vent,iridium_vent/iridium_vent,uranium_quad,iridium_vent,iridium_vent,iridium_vent,uranium_quad,iridium_vent/_,iridium_vent,_,_,_,iridium_vent,_/_,_,_,_,_,_,_" />

## 失败示例

<ReactorSimulator preset="vent,uranium_quad,vent/_,vent,_/_,_,_" />

四联燃料棒每个周期产生 96 热量，三个基础散热片只能移除 18。散热片熔毁后，全部热量进入外壳。外壳热量每个周期上升 96，约 143 秒后发生堆芯熔毁。

**警告：** 堆芯熔毁会引发爆炸，摧毁反应堆附近的方块。放入燃料棒之前，务必先用模拟器检查设计。

## 自由设计

<ReactorSimulator chambers="6" />
