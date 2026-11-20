---
navigation:
  title: "热能离心机"
  icon: mio_icif:producer/block_centrifuge_elc
  parent: machines.md
  position: 67
item_ids:
  - mio_icif:producer/block_centrifuge_elc
---

# 热能离心机

<Row>
  <BlockImage id="mio_icif:producer/block_centrifuge_elc" scale="3" />
</Row>

## 功能

热能离心机消耗EU升温，然后把物品分离为最多3种产物。

**加热：** 每条配方写了最低热量（圆石 100 HU，黏土 250 HU，粉碎矿石 500 到 4,000 HU，没写的 5,000 HU）。机器加热到配方所需就开工。只在有料、输出有空位、没被关闭时加热：每 tick +4 HU、耗 4 EU，最高 5,000 HU；否则每 tick −2 HU。

**转子：** 工作时转速每秒升高 10%，不工作时每秒降低 5%。低于满速时按转速比例运转。满速 12,000 rpm，离心力约 16,000 g。

界面画出俯视的加热环和转子，大字是转速。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MV (128 EU) |
| 最大输入 | 128 EU/t |
| 储能 | 24,000 EU |
| 工作耗电 | 48 EU/t |
| 单次工作时间 | 500 tick (25 s) |
| 单次耗电 | 24,000 EU |
| 加热 | 每 tick +4 HU、4 EU，最高 5,000 HU |

表中的工时和耗电是没有超频时的值。每个超频升级让工时变为 0.7 倍、耗电变为 1.6 倍。界面和 JEI 显示当前输入的实际数值。

## 使用步骤

1. 将MV导线连接到机器。
2. 把粉碎矿石放入输入槽位。
3. 等待机器达到工作热量。
4. 从3个输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于MV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_centrifuge_elc" fallbackText="-" />
