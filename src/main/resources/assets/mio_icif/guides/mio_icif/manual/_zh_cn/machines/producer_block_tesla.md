---
navigation:
  title: "特斯拉线圈"
  icon: mio_icif:producer/block_tesla
  parent: machines.md
  position: 66
item_ids:
  - mio_icif:producer/block_tesla
---

# 特斯拉线圈

<Row>
  <BlockImage id="mio_icif:producer/block_tesla" scale="3" />
</Row>

## 功能

特斯拉线圈消耗EU，伤害周围9格内的所有生物。
收到红石信号且储能足够时，线圈按固定间隔攻击。
对目标的首次攻击伤害更高。每次攻击也会损坏护甲。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MV (128 EU) |
| 最大输入 | 128 EU/t |
| 储能 | 10,000 EU |
| 工作耗电 | 500 EU/t |
| 单次工作时间 | 100 tick (5 s) |
| 单次耗电 | 50,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把特斯拉线圈放在需要防护的区域。
2. 将MV导线连接到机器。
3. 等待线圈储存足够的EU。
4. 施加红石信号以启动线圈。

## 注意


> **警告：** 线圈也会攻击范围内的玩家和驯服的动物。


> **警告：** 电压高于MV时，机器因过压爆炸。


## 配方

<RecipesFor id="mio_icif:producer/block_tesla" fallbackText="-" />
