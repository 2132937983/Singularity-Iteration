---
navigation:
  title: "压缩机"
  icon: mio_icif:producer/block_compressor_elc
  parent: machines.md
  position: 17
item_ids:
  - mio_icif:producer/block_compressor_elc
---

# 压缩机

<Row>
  <BlockImage id="mio_icif:producer/block_compressor_elc" scale="3" />
</Row>

## 功能

压缩机消耗EU压缩物品。粉末变为板，锭变为块，空单元变为压缩空气单元。

**原理：** 液压机的压实功 W = ∫P dV。越硬的物料要越高的压力和越长的行程，液压机的功率等于压力乘流量，所以每条配方有自己的工时和功率。

| 配方类型 | 工时 × 功率 | 每次耗电 |
|---|---|---|
| 软材料（雪块等） | 100 tick × 5 EU/t | 500 EU |
| 致密板 | 400 tick × 20 EU/t | 8,000 EU |
| 工业钻石 | 600 tick × 30 EU/t | 18,000 EU |
| 没写数值的配方 | 300 tick × 2 EU/t | 600 EU |

界面画出液压机的侧视图，压头随进度压下和回程。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | LV (32 EU) |
| 最大输入 | 32 EU/t |
| 储能 | 18,600 EU |
| 工作耗电 | 按配方（默认 2 EU/t） |
| 单次工作时间 | 按配方（默认 300 tick） |
| 单次耗电 | 按配方（默认 600 EU） |

表中的工时和耗电是没有超频时的值。每个超频升级让工时变为 0.7 倍、耗电变为 1.6 倍。界面和 JEI 显示当前输入的实际数值。

## 使用步骤

1. 将LV导线连接到机器。
2. 把要压缩的物品放入输入槽位。
3. 安装超频升级以加快加工。
4. 从输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于LV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_compressor_elc" fallbackText="-" />
