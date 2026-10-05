---
navigation:
  title: "高级采矿机"
  icon: mio_icif:producer/block_advanced_miner_elc
  parent: machines.md
  position: 6
item_ids:
  - mio_icif:producer/block_advanced_miner_elc
---

# 高级采矿机

<Row>
  <BlockImage id="mio_icif:producer/block_advanced_miner_elc" scale="3" />
</Row>

## 功能

高级采矿机无需采矿管道和钻头，开采机器下方的矿石。
OD扫描器的半径为16格。OV扫描器为32格。没有扫描器时，机器只向正下方开采。
机器把产物放入相邻容器。没有容器有空间时，产物掉落在机器上方。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | HV (512 EU) |
| 最大输入 | 512 EU/t |
| 储能 | 4,000,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将HV导线连接到机器。
2. 把OD扫描器或OV扫描器放入扫描器槽位。
3. 在机器旁放置箱子，收集矿石。
4. 在界面中设置过滤列表、白名单或黑名单模式和精准采集。

## 注意

- 重置按钮使开采位置从顶部重新开始。
- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于HV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_advanced_miner_elc" fallbackText="-" />
