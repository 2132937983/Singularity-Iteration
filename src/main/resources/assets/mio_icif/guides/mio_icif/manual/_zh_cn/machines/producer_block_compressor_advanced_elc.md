---
navigation:
  title: "进阶高压压缩机"
  icon: mio_icif:producer/block_compressor_advanced_elc
  parent: machines.md
  position: 2
item_ids:
  - mio_icif:producer/block_compressor_advanced_elc
---

# 进阶高压压缩机

<Row>
  <BlockImage id="mio_icif:producer/block_compressor_advanced_elc" scale="3" />
</Row>

## 功能

进阶高压压缩机在MV下使用压缩机配方。
机器完成每次加工的时间比压缩机短。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MV (128 EU) |
| 最大输入 | 128 EU/t |
| 储能 | 1,750 EU |
| 工作耗电 | 25 EU/t |
| 单次工作时间 | 70 tick (3.5 s) |
| 单次耗电 | 1,750 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将MV导线连接到机器。
2. 把要压缩的物品放入输入槽位。
3. 安装超频升级以加快加工。
4. 从输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于MV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_compressor_advanced_elc" fallbackText="-" />
