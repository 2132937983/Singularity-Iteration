---
navigation:
  title: "采矿机"
  icon: mio_icif:producer/block_miner_elc
  parent: machines.md
  position: 44
item_ids:
  - mio_icif:producer/block_miner_elc
---

# 采矿机

<Row>
  <BlockImage id="mio_icif:producer/block_miner_elc" scale="3" />
</Row>

## 功能

采矿机把采矿管道向下推进，并开采每层的矿石。
机器需要钻头、采矿管道和OD扫描器或OV扫描器。
OD扫描器的半径为管道周围3格。OV扫描器为6格。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | LV (32 EU) |
| 最大输入 | 128 EU/t |
| 储能 | 10,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将LV导线连接到机器。
2. 把铁钻头、钻石钻头或铱钻头放入钻头槽位。
3. 把采矿管道放入管道槽位。
4. 把OD扫描器或OV扫描器放入扫描器槽位。
5. 从储存槽位取出矿石。

## 注意

- 在采矿机旁放置泵，以抽走竖井中的流体。
- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于LV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_miner_elc" fallbackText="-" />
